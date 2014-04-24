package id.stsn.stm9.activity;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;
import id.stsn.stm9.fragment.DialogFragmentWorkaround;
import id.stsn.stm9.fragment.FileDialogFragment;
import id.stsn.stm9.services.KeyIntentService;
import id.stsn.stm9.services.KeyIntentServiceHandler;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Messenger;
import android.os.Message;
import android.widget.Toast;
import android.os.Handler;
import android.app.ProgressDialog;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class ListKeyActivity extends SherlockFragmentActivity {

	protected String mExportFilename = Environment.getExternalStorageDirectory() + "/";
	protected String mImportData;
	protected boolean mDeleteAfterImport = false;
	protected int tipeKunci;

	    FileDialogFragment mFileDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		handleActions(getIntent());
	}

	protected void handleActions(Intent intent) {
		String action = intent.getAction();
		Bundle extras = intent.getExtras();

		if (extras == null) {
			extras = new Bundle();
		}

		/**
		 * Android Standard Actions
		 */
		String searchString = null;
		if (Intent.ACTION_SEARCH.equals(action)) {
			searchString = extras.getString(SearchManager.QUERY);
			if (searchString != null && searchString.trim().length() == 0) {
				searchString = null;
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case Id.request.filename: {
			if (resultCode == RESULT_OK && data != null) {
				try {
					String path = data.getData().getPath();
					Log.d("Stm9", "path=" + path);

				} catch (NullPointerException e) {
					Log.e("Stm9", "Nullpointer!", e);
				}
			}
			return;
		}

		default: {
			break;
		}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, Id.menu.opsi.export_keys, 2, R.string.menu_export_keys).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			/* back to MainMenuActivity, icon on top */
			Intent intent = new Intent(this, MainMenuActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;

		case Id.menu.opsi.export_keys: {
			showExportKeysDialog(-1);
			return true;
		}

		default: {
			return super.onOptionsItemSelected(item);
		}
		}
	}


	/**
	 * menampilkan dialog tujuan export kunci
	 */
	public void showExportKeysDialog(final long keyRingMasterKeyId) {
		/* dieksekusi ketika file dipilih */ 
		Handler returnHandler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				if (message.what == FileDialogFragment.MESSAGE_OKAY) {
					Bundle data = message.getData();
					mExportFilename = data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME);

					exportKeys(keyRingMasterKeyId);
				}
			}
		};

		/* Buat Messenger balik */ 
		final Messenger messenger = new Messenger(returnHandler);

		DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
			public void run() {
				String title = null;
				if (keyRingMasterKeyId != -1) {
					/*single key export*/ 
					title = getString(R.string.title_export_key);
				} else {
					title = getString(R.string.title_export_keys);
				}

				String message = null;
				if (tipeKunci == Id.tipe.public_key) {
					message = getString(R.string.specify_file_to_export_to);
				} else {
					message = getString(R.string.specify_file_to_export_secret_keys_to);
				}

				mFileDialog = FileDialogFragment.newInstance(messenger, title, message,
						mExportFilename, null, Id.request.filename);

				mFileDialog.show(getSupportFragmentManager(), "fileDialog");
			}
		});
	}

	/**
	 * Export kunci
	 * -1 export semua kunci
	 */
	public void exportKeys(long keyRingMasterKeyId) {
		Log.d("Stm-9", "exportKeys started");

		/* send informasi ke KeyService */
		Intent intent = new Intent(this, KeyIntentService.class);

		intent.setAction(KeyIntentService.ACTION_EXPORT_KEYRING);

		Bundle data = new Bundle();

		data.putString(KeyIntentService.EXPORT_FILENAME, mExportFilename);
		data.putInt(KeyIntentService.EXPORT_KEY_TYPE, tipeKunci);

		if (keyRingMasterKeyId == -1) {
			data.putBoolean(KeyIntentService.EXPORT_ALL, true);
		} else {
			data.putLong(KeyIntentService.EXPORT_KEY_RING_MASTER_KEY_ID, keyRingMasterKeyId);
		}

		intent.putExtra(KeyIntentService.EXTRA_DATA, data);

		/*message diterima ketika selesai APG*/ 
		KeyIntentServiceHandler exportHandler = new KeyIntentServiceHandler(this,
				R.string.proses_exporting, ProgressDialog.STYLE_HORIZONTAL) {
			public void handleMessage(Message message) {
				super.handleMessage(message);

				if (message.arg1 == KeyIntentServiceHandler.MESSAGE_OKAY) {
					// get returned data bundle
					Bundle returnData = message.getData();

					int exported = returnData.getInt(KeyIntentService.RESULT_EXPORT);
					String toastMessage;
					if (exported == 1) { // export 1 kunci
						toastMessage = getString(R.string.key_exported);
					} else if (exported > 0) { // export lebih dari 1 kunci
						toastMessage = getString(R.string.keys_exported, exported);
					} else { // export gagal!
						toastMessage = getString(R.string.no_keys_exported);
					} // menampilkan toast message
					Toast.makeText(ListKeyActivity.this, toastMessage, Toast.LENGTH_SHORT).show();

				}
			};
		};

		/*buat messenger balik*/
		Messenger messenger = new Messenger(exportHandler);
		intent.putExtra(KeyIntentService.EXTRA_MESSENGER, messenger);

		/* menampilkan proses dalam bentuk dialog */ 
		exportHandler.showProgressDialog(this);

		/* start service dg intent */ 
		startService(intent);
	}
}

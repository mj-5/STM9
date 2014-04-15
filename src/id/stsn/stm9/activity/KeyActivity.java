package id.stsn.stm9.activity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;
import id.stsn.stm9.fragment.SetPassphraseDialogFragment;
import id.stsn.stm9.pgp.PgpConvert;
import id.stsn.stm9.pgp.PgpGeneralException;
import id.stsn.stm9.services.ActionBarHelper;
import id.stsn.stm9.services.KeyIntentService;
import id.stsn.stm9.services.KeyIntentServiceHandler;
import id.stsn.stm9.widget.KeyEditor;
import id.stsn.stm9.widget.SectionView;
import id.stsn.stm9.widget.UserIdEditor;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class KeyActivity extends SherlockFragmentActivity {

	/* hanya untuk internal */ 
	public static final String ACTION_CREATE_KEY = "id.stsn.stm9" + ".action." + "CREATE_KEY";

	/* untuk pembuatan kunci dengan beberapa parameter */ 
	public static final String EXTRA_USER_IDS = "user_ids";
	public static final String EXTRA_GENERATE_DEFAULT_KEYS = "generate_default_keys";

	/* untuk menyimpan kunci yang telah dibuat */ 
	public static final String RESULT_EXTRA_MASTER_KEY_ID = "master_key_id";
	public static final String RESULT_EXTRA_USER_ID = "user_id";

	private ActionBar vActionBar;

	private SectionView vUserIdsView;
	private SectionView vKeysView;

	private String vPassphraseAwal = null;
	private String vPassphraseBaru = null;

	private Button vUbahPassphrase;

	private CheckBox vNoPassphrase;

	Vector<String> mUserIds;
	Vector<PGPSecretKey> mKeys;
	Vector<Integer> mKeysUsages;
	boolean masterCanSign = true;

	/* will be set to false to build layout later in handler */ 
	private boolean vBuildLayout = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* menampilkan menu simpan dan batal */
		final LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		final View customActionBarView = inflater.inflate(R.layout.bar_view_simpan_batal, null);

		((TextView) customActionBarView.findViewById(R.id.bar_simpan_text)).setText(R.string.tmbl_simpan);
		customActionBarView.findViewById(R.id.bar_simpan).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				/* eksekusi simpan */
				simpan();
			}
		});
		((TextView) customActionBarView.findViewById(R.id.bar_batal_text)).setText(R.string.tmbl_batal);
		customActionBarView.findViewById(R.id.bar_batal).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				/* eksekusi batal */
				batal();
			}
		});

		/* menyembunyikan action bar */
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		setContentView(R.layout.key_activity);

		vActionBar = getSupportActionBar();
		vActionBar.setDisplayShowTitleEnabled(true);

		/* find views */
		vUbahPassphrase = (Button) findViewById(R.id.key_tmbl_ubah_passphrase);
		vNoPassphrase = (CheckBox) findViewById(R.id.key_no_passphrase);

		mUserIds = new Vector<String>();
		mKeys = new Vector<PGPSecretKey>();
		mKeysUsages = new Vector<Integer>();

		ActionBarHelper.setBackButton(this);

		/* catch intens */
		Intent i = getIntent();
		String action = i.getAction();
		if (ACTION_CREATE_KEY.equals(action)) {
			handleActionCreateKey(i);
		} 


		vUbahPassphrase.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				tampilInputPassphraseDialog();
			}
		});
		if (vBuildLayout) {
			buildLayout();
		}
	}

	/**
	 * buat pasangan kunci baru
	 */
	private void handleActionCreateKey(Intent i) {
		Bundle extras = i.getExtras();

		vActionBar.setTitle(R.string.title_generate_kunci);

		vPassphraseAwal = ""; 

		if (extras != null) {
			/* jika diberikan userId */
			if (extras.containsKey(EXTRA_USER_IDS)) {
				Log.d("Stm-9", "UserIds are given!");
				mUserIds.add(extras.getString(EXTRA_USER_IDS));
			}



			/* generate pasangan kunci */
			if (extras.containsKey(EXTRA_GENERATE_DEFAULT_KEYS)) {
				boolean generateDefaultKeys = extras.getBoolean(EXTRA_GENERATE_DEFAULT_KEYS);
				if (generateDefaultKeys) {

					vBuildLayout = false;

					/* kirim parameter (info) untuk generate kunci */
					Intent serviceI = new Intent(this, KeyIntentService.class);
					serviceI.setAction(KeyIntentService.ACTION_GENERATE_DEFAULT_RSA_KEYS);

					Bundle data = new Bundle();
					data.putString(KeyIntentService.GENERATE_KEY_SYMMETRIC_PASSPHRASE, vPassphraseAwal);

					serviceI.putExtra(KeyIntentService.EXTRA_DATA, data);

					/* tampil pesan ketika generate */
					KeyIntentServiceHandler saveHandler = new KeyIntentServiceHandler(this, R.string.proses_generate, ProgressDialog.STYLE_SPINNER) {
						public void handleMessage(Message m) {
							/* handle m */
							super.handleMessage(m);

							if (m.arg1 == KeyIntentServiceHandler.MESSAGE_OKAY) {
								/* menerima kunci baru dari services */
								Bundle data = m.getData();
								PGPSecretKeyRing masterKeyRing = (PGPSecretKeyRing) PgpConvert.BytesToPGPKeyRing(data.getByteArray(KeyIntentService.RESULT_NEW_KEY));

								/* add master key */
								@SuppressWarnings("unchecked")
								Iterator<PGPSecretKey> masterIt = masterKeyRing.getSecretKeys();
								mKeys.add(masterIt.next());
								mKeysUsages.add(Id.pilihan.usage.sign_and_encrypt);


								buildLayout();
							}
						};
					};

					/* buat messenger baru untuk mengembalikan data */
					Messenger messenger = new Messenger(saveHandler);
					serviceI.putExtra(KeyIntentService.EXTRA_MESSENGER, messenger);

					saveHandler.showProgressDialog(this);

					/* mulai service dengan i */
					startService(serviceI);
				}
			}
		}
	}

	/**
	 * tampilkan dialog untuk memasukkan passphrase baru
	 */
	private void tampilInputPassphraseDialog() {
		/* pesan diterima setelah passphrase disimpan sementara */ 
		Handler returnHandler = new Handler() {
			@Override
			public void handleMessage(Message m) {
				if (m.what == SetPassphraseDialogFragment.MESSAGE_OKAY) {
					Bundle data = m.getData();

					/* buat passphrase baru */ 
					vPassphraseBaru = data.getString(SetPassphraseDialogFragment.MESSAGE_NEW_PASSPHRASE);

					gantiTombolPassphraseText();
				}
			}
		};

		/* buat messenger baru */ 
		Messenger messenger = new Messenger(returnHandler);

		/* set title based on setPassphrase() */ 
		int title = -1;
		if (setPassphrase()) {
			title = R.string.title_ganti_passphrase;
		} else {
			title = R.string.title_set_passphrase;
		}

		SetPassphraseDialogFragment setPassphraseDialog = SetPassphraseDialogFragment.nInstance(messenger, title);

		setPassphraseDialog.show(getSupportFragmentManager(), "setPassphraseDialog");
	}

	/**
	 * Build layout dari mUserId, mKeys and mKeysUsages Vectors. 
	 * buat setiap user Id dan kunci
	 */
	private void buildLayout() {
		/* bulid layout dari inputan userIds dan keys */
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		LinearLayout container = (LinearLayout) findViewById(R.id.key_container);

		/* user ID */
		vUserIdsView = (SectionView) inflater.inflate(R.layout.key_section, container, false);
		vUserIdsView.setType(Id.tipe.user_id);
		vUserIdsView.setCanEdit(masterCanSign);
		vUserIdsView.setUserIds(mUserIds);
		container.addView(vUserIdsView);

		/* master key */
		vKeysView = (SectionView) inflater.inflate(R.layout.key_section, container, false);
		vKeysView.setType(Id.tipe.key);
		vKeysView.setCanEdit(masterCanSign);
		vKeysView.setKeys(mKeys, mKeysUsages);
		container.addView(vKeysView);

		gantiTombolPassphraseText();
	}

	private long getMasterKeyId() {
		if (vKeysView.getEditors().getChildCount() == 0) {
			return 0;
		}
		return ((KeyEditor) vKeysView.getEditors().getChildAt(0)).getValue().getKeyID();
	}

	public boolean setPassphrase() {
		if (vNoPassphrase.isChecked()) {
			return true;
		} else if ((!vPassphraseAwal.equals(""))
				|| (vPassphraseBaru != null && !vPassphraseBaru.equals(""))) {
			return true;
		} else {
			return false;
		}
	}
	
	private void gantiTombolPassphraseText() {
		vUbahPassphrase.setText(setPassphrase() ? R.string.tmbl_ganti_passphrase
				: R.string.tmbl_set_passphrase);
	}


	/**
	 * Returns user ids dari SectionView
	 */
	private ArrayList<String> getUserIds(SectionView userIdsView) throws PgpGeneralException {
		ArrayList<String> userIds = new ArrayList<String>();

		ViewGroup userIdEditors = userIdsView.getEditors();

		boolean gotMainUserId = false;
		for (int i = 0; i < userIdEditors.getChildCount(); ++i) {
			UserIdEditor editor = (UserIdEditor) userIdEditors.getChildAt(i);
			String userId = null;
			try {
				userId = editor.getValue();
			} catch (UserIdEditor.NoNameException e) {
				throw new PgpGeneralException(this.getString(R.string.error_user_id_name));
			} catch (UserIdEditor.NoEmailException e) {
				throw new PgpGeneralException(
						this.getString(R.string.error_user_id_email));
			} catch (UserIdEditor.InvalidEmailException e) {
				throw new PgpGeneralException(e.getMessage());
			}

			if (userId.equals("")) {
				continue;
			}

			if (editor.isMainUserId()) {
				userIds.add(0, userId);
				gotMainUserId = true;
			} else {
				userIds.add(userId);
			}
		}

		if (userIds.size() == 0) {
			throw new PgpGeneralException(getString(R.string.error_key_needs_user_id));
		}

		if (!gotMainUserId) {
			throw new PgpGeneralException(getString(R.string.error_main_user_id_no_empty));
		}

		return userIds;
	}

	/**
	 * Returns keys dari SectionView
	 */
	private ArrayList<PGPSecretKey> getKeys(SectionView keysView) throws PgpGeneralException {
		ArrayList<PGPSecretKey> keys = new ArrayList<PGPSecretKey>();

		ViewGroup keyEditors = keysView.getEditors();

		if (keyEditors.getChildCount() == 0) {
			throw new PgpGeneralException(getString(R.string.error_key_needs_master_key));
		}

		for (int i = 0; i < keyEditors.getChildCount(); ++i) {
			KeyEditor editor = (KeyEditor) keyEditors.getChildAt(i);
			keys.add(editor.getValue());
		}

		return keys;
	}

	/**
	 * Returns usage selections of keys dari SectionView
	 */
	private ArrayList<Integer> getKeysUsages(SectionView keysView) throws PgpGeneralException {
		ArrayList<Integer> getKeysUsages = new ArrayList<Integer>();

		ViewGroup keyEditors = keysView.getEditors();

		if (keyEditors.getChildCount() == 0) {
			throw new PgpGeneralException(getString(R.string.error_key_needs_master_key));
		}

		for (int i = 0; i < keyEditors.getChildCount(); ++i) {
			KeyEditor editor = (KeyEditor) keyEditors.getChildAt(i);
			getKeysUsages.add(editor.getUsage());
		}

		return getKeysUsages;
	}

	private void simpan() {
		try {
			if (!setPassphrase()) {
				throw new PgpGeneralException(this.getString(R.string.set_a_passphrase));
			}

			/* kirim info ke services u' edit */
			Intent i = new Intent(this, KeyIntentService.class);

			i.setAction(KeyIntentService.ACTION_SAVE_KEYRING);

			/* fill values */
			Bundle data = new Bundle();
			data.putString(KeyIntentService.SAVE_KEYRING_CURRENT_PASSPHRASE, vPassphraseAwal);
			data.putString(KeyIntentService.SAVE_KEYRING_NEW_PASSPHRASE, vPassphraseBaru);
			data.putStringArrayList(KeyIntentService.SAVE_KEYRING_USER_IDS, getUserIds(vUserIdsView));
			ArrayList<PGPSecretKey> keys = getKeys(vKeysView);
			data.putByteArray(KeyIntentService.SAVE_KEYRING_KEYS, PgpConvert.PGPSecretKeyArrayListToBytes(keys));
			data.putIntegerArrayList(KeyIntentService.SAVE_KEYRING_KEYS_USAGES, getKeysUsages(vKeysView));
			data.putLong(KeyIntentService.SAVE_KEYRING_MASTER_KEY_ID, getMasterKeyId());
			data.putBoolean(KeyIntentService.SAVE_KEYRING_CAN_SIGN, masterCanSign);

			i.putExtra(KeyIntentService.EXTRA_DATA, data);

			/* pesan diterima setelah proses simpan selesai */
			KeyIntentServiceHandler saveHandler = new KeyIntentServiceHandler(this,
					R.string.proses_simpan, ProgressDialog.STYLE_HORIZONTAL) {
				public void handleMessage(Message message) {
					super.handleMessage(message);

					if (message.arg1 == KeyIntentServiceHandler.MESSAGE_OKAY) {
						Intent data = new Intent();
						data.putExtra(RESULT_EXTRA_MASTER_KEY_ID, getMasterKeyId());
						ArrayList<String> userIds = null;
						try {
							userIds = getUserIds(vUserIdsView);
						} catch (PgpGeneralException e) {
							Log.e("stm-9", "exception while getting user ids", e);
						}
						data.putExtra(RESULT_EXTRA_USER_ID, userIds.get(0));
						setResult(RESULT_OK, data);
						finish();
					}
				};
			};

			/* buat messenger baru */ 
			Messenger messenger = new Messenger(saveHandler);
			i.putExtra(KeyIntentService.EXTRA_MESSENGER, messenger);

			saveHandler.showProgressDialog(this);

			/* mulai service dengan i */ 
			startService(i);
		} catch (PgpGeneralException e) {
			Toast.makeText(this, getString(R.string.error_message, e.getMessage()),
					Toast.LENGTH_SHORT).show();
		}
	}

	private void batal() {
		setResult(RESULT_CANCELED);
		finish();
	}
}

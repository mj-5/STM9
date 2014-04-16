package id.stsn.stm9.activity;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MasterKeyActivity extends ListKeyActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		tipeKunci = Id.tipe.secret_key;

		setContentView(R.layout.master_key_activity);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(1, Id.menu.opsi.create, 1, R.string.menu_buat_kunci).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return true;
	}

	/* membuat pilihan untuk membuat kunci baru*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Id.menu.opsi.create: {
			buatKunci();
			return true;
		}

		default: {
			return super.onOptionsItemSelected(item);
		}
		}
	}

	/* buat kunci baru secara general */
	private void buatKunci() {
		Intent intent = new Intent(this, KeyActivity.class);
		intent.setAction(KeyActivity.ACTION_CREATE_KEY);
		intent.putExtra(KeyActivity.EXTRA_GENERATE_DEFAULT_KEYS, true);
		intent.putExtra(KeyActivity.EXTRA_USER_IDS, ""); 
		startActivityForResult(intent, 0);
	}
}

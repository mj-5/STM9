package id.stsn.stm9.ui;

import id.stsn.stm9.R;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.os.Bundle;

public class MainMenuActivity extends Activity {

	Button btnKeyList, btnMasterKey, btnEncSign, btnDecVer;

	// // klik-->menampilkan halaman keyList
	// public void keyListOnClick(View view){
	// startActivityForResult(new Intent(this, KeyListActivity.class), 0);
	// }
	//
	// // klik-->menampilkan halaman masterKey
	// public void masterKeyOnClick(View view){
	// startActivityForResult(new Intent(this, MasterKeyActivity.class), 0);
	// }
	//
	// // klik-->menampilkan halaman EncSignActivity
	// public void encSignOnClick(View view){
	// startActivityForResult(new Intent(this, EncSignActivity.class), 0);
	// }
	//
	// // klik-->menampilkan halaman decryptVerification
	// public void decVerOnClick(View view){
	// startActivityForResult(new Intent(this, DecVerActivity.class), 0);
	// }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_menu);

		btnKeyList = (Button) findViewById(R.id.key_list);
		btnMasterKey = (Button) findViewById(R.id.master_key);
		btnEncSign = (Button) findViewById(R.id.encrypt_sign);
		btnDecVer = (Button) findViewById(R.id.decrypt_verification);

		btnKeyList.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				keyList();
			}
		});

		btnMasterKey.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				masterKey();
			}
		});

		btnEncSign.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				encSign();
			}
		});

		btnDecVer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				decVer();
			}
		});
	}

	protected void keyList() {
		Intent i = new Intent(this, KeyListActivity.class);
		startActivity(i);
	}

	protected void masterKey() {
		Intent i = new Intent(this, MasterKeyActivity.class);
		startActivity(i);		
	}

	protected void encSign() {
		Intent i = new Intent(this, EncSignActivity.class);
		startActivity(i);		
	}

	protected void decVer() {
		Intent i = new Intent(this, DecVerActivity.class);
		startActivity(i);		
	}
}

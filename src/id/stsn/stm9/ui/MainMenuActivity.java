package id.stsn.stm9.ui;

import id.stsn.stm9.R;

import android.content.Intent;
import android.view.View;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;

public class MainMenuActivity extends SherlockActivity{
	
	// klik-->menampilkan halaman keyList
	public void keyListOnClick(View view){
		startActivityForResult(new Intent(this, KeyListActivity.class), 0);
		}

	// klik-->menampilkan halaman masterKey
	public void masterKeyOnClick(View view){
		startActivityForResult(new Intent(this, MasterKeyActivity.class), 0);
	}
	
	// klik-->menampilkan halaman EncSignActivity
	public void encSignOnClick(View view){
		startActivityForResult(new Intent(this, EncSignActivity.class), 0);
	}
	
	// klik-->menampilkan halaman decryptVerification
	public void decVerOnClick(View view){
		startActivityForResult(new Intent(this, DecVerActivity.class), 0);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_menu);
	}
}

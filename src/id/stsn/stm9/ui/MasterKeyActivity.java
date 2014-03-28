package id.stsn.stm9.ui;

import id.stsn.stm9.R;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;

public class MasterKeyActivity extends Activity {

	Button btnBuatKunci;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.master_key);

		btnBuatKunci = (Button) findViewById(R.id.menu_buat_kunci);

		btnBuatKunci.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				buatKunci();
			}
		});

	}

	private void buatKunci() {
		Intent intent = new Intent(this, KeyActivity.class);
		intent.setAction(KeyActivity.BUAT_KUNCI_ACTION);
		intent.putExtra(KeyActivity.EXTRA_GENERATE_DEFAULT_KEYS, true);
		intent.putExtra(KeyActivity.EXTRA_USER_IDS, "");
		startActivityForResult(intent, 0);

	}

}

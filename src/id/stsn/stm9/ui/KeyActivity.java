package id.stsn.stm9.ui;

import java.util.Vector;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import id.stsn.stm9.R;
import id.stsn.stm9.fragment.SetPassphraseDialogFragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class KeyActivity extends SherlockFragmentActivity {

	Button btnSimpanKunci, btnBatalKunci, btnIsiPasspharase;

	public static final String BUAT_KUNCI_ACTION = "id.stsn.stm9" + ".action."
			+ "BUAT_KUNCI";
    public static final String EDIT_KUNCI_ACTION = "id.nci.stm9" + ".action." + "EDIT_KUNCI";

	public static final String EXTRA_GENERATE_DEFAULT_KEYS = "generate_default_keys";
	public static final String EXTRA_USER_IDS = "user_ids";
	
	private String mPassphraseAwal = null;
	private String mPassphraseBaru = null;
	
	Vector<String> mUserIds;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
		setContentView(R.layout.edit_key);

		btnSimpanKunci = (Button) findViewById(R.id.btn_simpan_kunci);
		btnBatalKunci = (Button) findViewById(R.id.btn_batal_kunci);

		btnSimpanKunci.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clickSimpan();

			}
		});

		btnBatalKunci.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clickBatal();

			}
		});
		
		btnIsiPasspharase = (Button) findViewById(R.id.isi_passphrase);
        
		btnIsiPasspharase.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showPassphraseBox();
			}
		});
	}

	public void showPassphraseBox() {
		Handler returnHandler = new Handler(){
			@Override
			public void handleMessage(Message message){
				if (message.what == SetPassphraseDialogFragment.MESSAGE_OKAY){
					Bundle data = message.getData();
					
					mPassphraseBaru = data.getString(SetPassphraseDialogFragment.MESSAGE_NEW_PASSPHRASE);
					
					gantiPassphrase();
				}
			}
		};
		
		Messenger messenger = new Messenger(returnHandler);
		
		//set title based on gantiPassphraseBox
		int title = -1;
		if (gantiPassphraseBox()){
			title = R.string.ganti_passphrase;
		} else {
			title = R.string.isi_passphrase;
		}
		
		SetPassphraseDialogFragment setPassphraseDialog = SetPassphraseDialogFragment.newInstance(messenger, title);
		setPassphraseDialog.show(getSupportFragmentManager(),"setPassphraseDialog");
	}

	private void gantiPassphrase() {
		btnIsiPasspharase.setText(gantiPassphraseBox() ? R.string.ganti_passphrase : R.string.isi_passphrase);
	}

	public boolean gantiPassphraseBox() {
		if ((!mPassphraseAwal.equals(""))
                || (mPassphraseBaru!= null && !mPassphraseBaru.equals(""))) {
            return true;
        } 
		else {
            return false;
        }
	}

	private void clickSimpan() {
		// TODO Auto-generated method stub

	}

	private void clickBatal() {
		setResult(RESULT_CANCELED);
		finish();
	}
}

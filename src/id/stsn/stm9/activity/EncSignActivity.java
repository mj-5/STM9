package id.stsn.stm9.activity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Vector;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;
import id.stsn.stm9.fragment.PassphraseDialogFragment;
import id.stsn.stm9.pgp.PgpGeneralException;
import id.stsn.stm9.pgp.PgpKeyHelper;
import id.stsn.stm9.provider.ProviderHelper;
import id.stsn.stm9.services.ActionBarHelper;
import id.stsn.stm9.services.KeyIntentService;
import id.stsn.stm9.services.KeyIntentServiceHandler;
import id.stsn.stm9.services.PassphraseCacheService;
import id.stsn.stm9.services.Preferences;
import id.stsn.stm9.utility.QrCodeUtils;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class EncSignActivity extends SherlockFragmentActivity {

	/* Intents */
	public static final String ACTION_ENCRYPT = "id.stsn.stm9" + ".action." + "ENCRYPT";

	/* EXTRA keys for input */
	public static final String EXTRA_TEXT = "text";

	// preselect ids, for internal use
	public static final String EXTRA_SIGNATURE_KEY_ID = "signature_key_id";
	public static final String EXTRA_ENCRYPTION_KEY_IDS = "encryption_key_ids";

	private String mEncryptString = "";

	private EditText mMessage = null;
	private Button mSelectKeysButton = null;

	private CheckBox mSign = null;
	private TextView mMainUserId = null;
	private TextView mMainUserIdRest = null;

	private LinearLayout mSource = null;
	private TextView mSourceLabel = null;

	private LinearLayout mMode = null;
	private TextView mModeLabel = null;
	
    private long mEncryptionKeyIds[] = null;
    private long mSecretKeyId = Id.kunci.none;
    
    private boolean mEncryptEnabled = false;
    
    private int mEncryptTarget;

    private boolean mGenerateSignature = false;
    
	private Bitmap mImage;

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(1, Id.menu.opsi.encrypt, 1, mEncryptString).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return true;

	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainMenuActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			
			return true;

		case Id.menu.opsi.encrypt:
			encryptClicked();
			
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.enc_sign_activity);

		ActionBarHelper.setBackButton(this);

		showLayout();

		handleActions(getIntent());
	}

	/**
	 * Handle all actions with this Intent
	 * @param intent
	 */
	private void handleActions(Intent intent) {
		String action = intent.getAction();
		Bundle extras = intent.getExtras();
		String type = intent.getType();

		if (extras == null) {

			extras = new Bundle();
		}

		/*
		 * Device's action
		 */
		if (Intent.ACTION_SEND.equals(action) && type != null) {

			if("text/plain".equals(type)) {

				/* plaintext */
				String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
				if (sharedText != null) {

					extras.putString(EXTRA_TEXT, sharedText);
					action = ACTION_ENCRYPT;
				}
			}
		}

		String textData = extras.getString(EXTRA_TEXT);

		long signatureKeyId = extras.getLong(EXTRA_SIGNATURE_KEY_ID);
		long[] encryptionKeyIds = extras.getLongArray(EXTRA_ENCRYPTION_KEY_IDS);

		/* preselect keys given by intent */
		preselectKeys(signatureKeyId, encryptionKeyIds);

		/*
		 * Main Action
		 */
		if (ACTION_ENCRYPT.equals(action) && textData != null) {

			mMessage.setText(textData);

		}
		
		else {
			Log.e("stm9-enc", "Include the extra 'text' with setData() in your Intent!");
		}
	}

	private void preselectKeys(long preselectedSignatureKeyId, long[] preselectedEncryptionKeyIds) {

		if (preselectedSignatureKeyId != 0) {
			PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(this,
					preselectedSignatureKeyId);
			PGPSecretKey masterKey = null;
			if (keyRing != null) {
				masterKey = PgpKeyHelper.getMasterKey(keyRing);
				if (masterKey != null) {
					Vector<PGPSecretKey> signKeys = PgpKeyHelper.getUsableSigningKeys(keyRing);
					if (signKeys.size() > 0) {
						mSecretKeyId = masterKey.getKeyID();
					}
				}
			}
		}

		if (preselectedEncryptionKeyIds != null) {
			Vector<Long> goodIds = new Vector<Long>();
			for (int i = 0; i < preselectedEncryptionKeyIds.length; ++i) {
				PGPPublicKeyRing keyRing = ProviderHelper.getPGPPublicKeyRingByMasterKeyId(this,
						preselectedEncryptionKeyIds[i]);
				PGPPublicKey masterKey = null;
				if (keyRing == null) {
					continue;
				}
				masterKey = PgpKeyHelper.getMasterKey(keyRing);
				if (masterKey == null) {
					continue;
				}
				Vector<PGPPublicKey> encryptKeys = PgpKeyHelper.getUsableEncryptKeys(keyRing);
				if (encryptKeys.size() == 0) {
					continue;
				}
				goodIds.add(masterKey.getKeyID());
			}
			if (goodIds.size() > 0) {
				mEncryptionKeyIds = new long[goodIds.size()];
				for (int i = 0; i < goodIds.size(); ++i) {
					mEncryptionKeyIds[i] = goodIds.get(i);
				}
			}
		}
	}
    
	private void showLayout() {
		mSource = (LinearLayout) findViewById(R.id.source);
		mSourceLabel = (TextView) findViewById(R.id.sourceLabel);
		mMode = (LinearLayout) findViewById(R.id.mode);
		mModeLabel = (TextView) findViewById(R.id.modeLabel);
		mMessage = (EditText) findViewById(R.id.message);
		mSelectKeysButton = (Button) findViewById(R.id.btn_selectEncryptKeys);
		mSign = (CheckBox) findViewById(R.id.sign);
		mMainUserId = (TextView) findViewById(R.id.mainUserId);
		mMainUserIdRest = (TextView) findViewById(R.id.mainUserIdRest);

		mSelectKeysButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				selectPublicKeys();
			}
		});

		mSign.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				CheckBox checkBox = (CheckBox) v;
				if (checkBox.isChecked()) {
					selectSecretKey();
				} else {
					mSecretKeyId = Id.kunci.none;
					updateView();
				}
			}
		});
	}
	
    protected void updateView() {

        if (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0) {
            mSelectKeysButton.setText(R.string.no_keys_selected);
        } else if (mEncryptionKeyIds.length == 1) {
            mSelectKeysButton.setText(R.string.one_key_selected);
        } else {
            mSelectKeysButton.setText("" + mEncryptionKeyIds.length + " "
                    + getResources().getString(R.string.n_keys_selected));
        }

        if (mSecretKeyId == Id.kunci.none) {
            mSign.setChecked(false);
            mMainUserId.setText("");
            mMainUserIdRest.setText("");
        } else {
            String uid = getResources().getString(R.string.unknown_user_id);
            String uidExtra = "";
            PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(this,
                    mSecretKeyId);
            if (keyRing != null) {
                PGPSecretKey key = PgpKeyHelper.getMasterKey(keyRing);
                if (key != null) {
                    String userId = PgpKeyHelper.getMainUserIdSafe(this, key);
                    String chunks[] = userId.split(" <", 2);
                    uid = chunks[0];
                    if (chunks.length > 1) {
                        uidExtra = "<" + chunks[1];
                    }
                }
            }
            mMainUserId.setText(uid);
            mMainUserIdRest.setText(uidExtra);
            mSign.setChecked(true);
        }

        updateActionBarButtons();
    		
	}

	private void setActionbarButtons(boolean encryptEnabled, int encryptStringRes) {
        mEncryptEnabled = encryptEnabled;
        if (encryptEnabled) {
            mEncryptString = getString(encryptStringRes);
        }
        
        invalidateOptionsMenu();
    }
    
    private void updateActionBarButtons() {
    	if (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0) {
    		if (mSecretKeyId == 0) {
    			setActionbarButtons(false, 0);
    		} else {
    			setActionbarButtons(true, R.string.tmbl_sign_and_send);
    		}
    	} else {
    		setActionbarButtons(true, R.string.tmbl_encrypt_and_send);
    	}
    }
    
    private void encryptClicked() {
    	Log.d("stm9-enc", "encrypt button clicked!");

    	boolean encryptIt = (mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0);

    		if (!encryptIt && mSecretKeyId == 0) {
    			Toast.makeText(this, R.string.select_encryption_or_signature_key, Toast.LENGTH_SHORT)
    			.show();
    			return;
    		}

    		if (mSecretKeyId != 0
    				&& PassphraseCacheService.getCachedPassphrase(this, mSecretKeyId) == null) {
    			showPassphraseDialog();

    			return;
    		}
    	
    	encryptStart();
    }

    private void showPassphraseDialog() {
    	Handler returnHandler = new Handler() {

    		public void handleMessage(Message message) {
    			if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
    				encryptStart();
    			}
    		}
    	};
    	
    	// Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(
                    EncSignActivity.this, messenger, mSecretKeyId);

            passphraseDialog.show(getSupportFragmentManager(), "passphraseDialog");
        } catch (PgpGeneralException e) {
            Log.d("stm-9", "No passphrase for this secret key, encrypt directly!");
            // send message to handler to start encryption directly
            returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
        }
    }

    protected void encryptStart() {
    	// Send all info
    	Intent intent = new Intent(this, KeyIntentService.class);

    	Bundle data = new Bundle();

    	boolean useAsciiArmor = true;
    	long encryptionKeyIds[] = null;
    	int compressionId = 0;
    	boolean signOnly = false;
    	long mSecretKeyIdToPass = 0;

    		mSecretKeyIdToPass = mSecretKeyId;
    		encryptionKeyIds = mEncryptionKeyIds;
    		signOnly = (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0);

    	intent.setAction(KeyIntentService.ACTION_ENCRYPT_SIGN);

    	/* default target and data bundle */
    		useAsciiArmor = true;
    		compressionId = Preferences.getPreferences(this).getDefaultMessageCompression();

    		data.putInt(KeyIntentService.TARGET, KeyIntentService.TARGET_BYTES);

    		String message = mMessage.getText().toString();
    		if (signOnly) {
    			fixBadCharactersForGmail(message);
    		}
    		data.putByteArray(KeyIntentService.ENCRYPT_MESSAGE_BYTES, message.getBytes());

    	data.putLong(KeyIntentService.ENCRYPT_SECRET_KEY_ID, mSecretKeyIdToPass);
    	data.putBoolean(KeyIntentService.ENCRYPT_USE_ASCII_ARMOR, useAsciiArmor);
    	data.putLongArray(KeyIntentService.ENCRYPT_ENCRYPTION_KEYS_IDS, encryptionKeyIds);
    	data.putInt(KeyIntentService.ENCRYPT_COMPRESSION_ID, compressionId);
    	data.putBoolean(KeyIntentService.ENCRYPT_GENERATE_SIGNATURE, mGenerateSignature);
    	data.putBoolean(KeyIntentService.ENCRYPT_SIGN_ONLY, signOnly);

    	intent.putExtra(KeyIntentService.EXTRA_DATA, data);

    	// Message is received after encrypting is done in ApgService
    	KeyIntentServiceHandler saveHandler = new KeyIntentServiceHandler(this,
    			R.string.proses_encrypting, ProgressDialog.STYLE_HORIZONTAL) {
    		public void handleMessage(Message message) {
    			// handle messages by standard ApgHandler first
    			super.handleMessage(message);

    			if (message.arg1 == KeyIntentServiceHandler.MESSAGE_OKAY) {
    				// get returned data bundle
    				Bundle data = message.getData();

    				String output;

    				output = data.getString(KeyIntentService.RESULT_ENCRYPTED_STRING);
    				Log.d("stm-9", "output: " + output);

    				/* encode output to qr code */
    				mImage = QrCodeUtils.getQRCodeBitmap(output, 500 );

    				Intent sendIntent = new Intent(Intent.ACTION_SEND);

    				sendIntent.setType("image/png");

    				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    				mImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
    				File f = new File(Environment.getExternalStorageDirectory() + File.separator + "temporary_file.png");
    				try {
    					f.createNewFile();
    					FileOutputStream fo = new FileOutputStream(f);
    					fo.write(bytes.toByteArray());

    				} catch (Exception e) {
    					e.printStackTrace();
    				}

    				sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/temporary_file.png"));
    				startActivity(Intent.createChooser(sendIntent,
    						getString(R.string.title_send_email)));
    			}
    		};
    	};
    	// Create a new Messenger for the communication back
    	Messenger messenger = new Messenger(saveHandler);
    	intent.putExtra(KeyIntentService.EXTRA_MESSENGER, messenger);

    	// show progress dialog
    	saveHandler.showProgressDialog(this);

    	// start service with intent
    	startService(intent);
    }

	private String fixBadCharactersForGmail(String message) {
        message = message.replaceAll(" +\n", "\n");
        message = message.replaceAll("\n\n+", "\n\n");
        message = message.replaceFirst("^\n+", "");
        message = message.replaceFirst("\n*$", "\n");

        return message;

	}
	
	private void selectPublicKeys() {
        Intent intent = new Intent(this, SelectPublicKeyActivity.class);
        Vector<Long> keyIds = new Vector<Long>();
        if (mSecretKeyId != 0) {
            keyIds.add(mSecretKeyId);
        }
        if (mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0) {
            for (int i = 0; i < mEncryptionKeyIds.length; ++i) {
                keyIds.add(mEncryptionKeyIds[i]);
            }
        }
        long[] initialKeyIds = null;
        if (keyIds.size() > 0) {
            initialKeyIds = new long[keyIds.size()];
            for (int i = 0; i < keyIds.size(); ++i) {
                initialKeyIds[i] = keyIds.get(i);
            }
        }
        intent.putExtra(SelectPublicKeyActivity.EXTRA_SELECTED_MASTER_KEY_IDS, initialKeyIds);
        startActivityForResult(intent, Id.request.public_keys);
    }

    private void selectSecretKey() {
        Intent intent = new Intent(this, SelectSecretKeyActivity.class);
        startActivityForResult(intent, Id.request.secret_keys);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
		case Id.request.public_keys: {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                mEncryptionKeyIds = bundle
                        .getLongArray(SelectPublicKeyActivity.RESULT_EXTRA_MASTER_KEY_IDS);
            }
            updateView();
            break;
        }

        case Id.request.secret_keys: {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                mSecretKeyId = bundle.getLong(SelectSecretKeyActivity.RESULT_EXTRA_MASTER_KEY_ID);
            } else {
                mSecretKeyId = Id.kunci.none;
            }
            updateView();
            break;
        }
		default:
			break;
		}
    }
}

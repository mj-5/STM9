package id.stsn.stm9.activity;

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Vector;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;

import android.annotation.SuppressLint;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class EncSignActivity extends SherlockFragmentActivity {

	/* Intents */
	public static final String ACTION_ENCRYPT = "id.stsn.stm9" + ".action." + "ENCRYPT";

	/* EXTRA keys for input */
	public static final String EXTRA_TEXT = "text";

	// enables ASCII Armor for file encryption when uri is given
	public static final String EXTRA_ASCII_ARMOR = "ascii_armor";

	// preselect ids, for internal use
	public static final String EXTRA_SIGNATURE_KEY_ID = "signature_key_id";
	public static final String EXTRA_ENCRYPTION_KEY_IDS = "encryption_key_ids";

	private long mEncryptionKeyIds[] = null;

	private EditText mMessage = null;
	private Button mSelectKeysButton = null;

	private boolean mEncryptEnabled = false;
	private String mEncryptString = "";

	private CheckBox mSign = null;
	private TextView mMainUserId = null;
	private TextView mMainUserIdRest = null;

	private TextView mSourceLabel = null;

	private TextView mModeLabel = null;

	private int mEncryptTarget;

	private boolean mGenerateSignature = false;

	private long mSecretKeyId = Id.kunci.none;

	private Bitmap mImage;
	
  private boolean mAsciiArmorDemand = false;
  private boolean mOverrideAsciiArmor = false;

	/**
	 * ActionBar menu is created based on class variables to change it at runtime
	 * 
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (mEncryptEnabled) {
			menu.add(1, Id.menu.opsi.encrypt, 1, mEncryptString).setShowAsAction(
					MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			// app icon in Action Bar clicked; go home
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.enc_sign_activity);

		// set actionbar without home button if called from another app
		ActionBarHelper.setBackButton(this);

		initView();

		// Handle intent actions
		handleActions(getIntent());

		//        updateView();
		//        updateSource();
		//        updateMode();

		updateActionBarButtons();
	}

	/**
	 * Handles all actions with this intent
	 * 
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
		 * Android's Action
		 */
		if (Intent.ACTION_SEND.equals(action) && type != null) {
			// When sending to APG Encrypt via share menu
			if ("text/plain".equals(type)) {
				// Plain text
				String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
				if (sharedText != null) {
					// handle like normal text encryption, override action and extras to later
					// execute ACTION_ENCRYPT in main actions
					extras.putString(EXTRA_TEXT, sharedText);
					extras.putBoolean(EXTRA_ASCII_ARMOR, true);
					action = ACTION_ENCRYPT;
				}
			} 
		}

		String textData = extras.getString(EXTRA_TEXT);

		long signatureKeyId = extras.getLong(EXTRA_SIGNATURE_KEY_ID);
		long[] encryptionKeyIds = extras.getLongArray(EXTRA_ENCRYPTION_KEY_IDS);

		// preselect keys given by intent
		preselectKeys(signatureKeyId, encryptionKeyIds);

		/**
		 * Main Actions
		 */
		if (ACTION_ENCRYPT.equals(action) && textData != null) {
			// encrypt text based on given extra

			mMessage.setText(textData);
//			            mSource.setInAnimation(null);
//			            mSource.setOutAnimation(null);
//			            while (mSource.getCurrentView().getId() != R.id.sourceMessage) {
//			            	mSource.showNext();
			//            }
		} else {
			Log.e("stm-9",
					"Include the extra 'text' with setData() in your Intent!");
		}
	}

	/**
	 * If an Intent gives a signatureKeyId and/or encryptionKeyIds, preselect those!
	 * 
	 * @param preselectedSignatureKeyId
	 * @param preselectedEncryptionKeyIds
	 */
	private void preselectKeys(long preselectedSignatureKeyId, long[] preselectedEncryptionKeyIds) {
		if (preselectedSignatureKeyId != 0) {
			PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(this, preselectedSignatureKeyId);
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
				PGPPublicKeyRing keyRing = ProviderHelper.getPGPPublicKeyRingByMasterKeyId(this, preselectedEncryptionKeyIds[i]);
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

	//    private void updateSource() {
	//        switch (mSource.getCurrentView().getId()) {
	//        case R.id.sourceMessage: {
	//            mSourceLabel.setText(R.string.label_message);
	//            break;
	//        }
	//
	//        default: {
	//            break;
	//        }
	//        }
	//        updateActionBarButtons();
	//    }

	/**
	 * Set ActionBar buttons based on parameters
	 * 
	 * @param encryptEnabled
	 * @param encryptStringRes
	 * @param encryptToClipboardEnabled
	 * @param encryptToClipboardStringRes
	 */
	@SuppressLint("NewApi")
	private void setActionbarButtons(boolean encryptEnabled, int encryptStringRes) {
		mEncryptEnabled = encryptEnabled;
		if (encryptEnabled) {
			mEncryptString = getString(encryptStringRes);
		}

		invalidateOptionsMenu();
	}

	/**
	 * Update ActionBar buttons based on current selection in view
	 */
	private void updateActionBarButtons() {
		//        switch (mSource.getCurrentView().getId()) {
		//        case R.id.sourceMessage: {
		//            mSourceLabel.setText(R.string.label_message);

		if (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0) {
			if (mSecretKeyId == 0) {
				setActionbarButtons(false, 0);
			} else {
				setActionbarButtons(true, R.string.tmbl_sign_and_send);
			}
		} else {
			setActionbarButtons(true, R.string.tmbl_encrypt_and_send);
			//                }
		//            break;
			//        }

			//        default: {
			//        	break;
			//        }
		}
	}

	//    private void updateMode() {
	//        switch (mMode.getCurrentView().getId()) {
	//        case R.id.modeAsymmetric: {
	//            mModeLabel.setText(R.string.label_asymmetric);
	//            break;
	//        }
	//
	//        default: {
	//            break;
	//        }
	//        }
	//        updateActionBarButtons();
	//    }

	private void encryptClicked() {
		Log.d("stm-9", "encryptClicked invoked!");

		initiateEncryption();
	}

	private void initiateEncryption() {
		if (mEncryptTarget == Id.target.email){
		boolean encryptIt = (mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0);
		// for now require at least one form of encryption for files

		if (!encryptIt && mSecretKeyId == 0) {
			Toast.makeText(this, R.string.select_encryption_or_signature_key, Toast.LENGTH_SHORT).show();
			return;
		}

		if (mSecretKeyId != 0
				&& PassphraseCacheService.getCachedPassphrase(this, mSecretKeyId) == null) {
			showPassphraseDialog();

			return;
		}
		}
		encryptStart();
		
	}

	/**
	 * Shows passphrase dialog to cache a new passphrase the user enters for using it later for
	 * encryption
	 */
	private void showPassphraseDialog() {
		// Message is received after passphrase is cached
		Handler returnHandler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
					if (mEncryptTarget == Id.target.email){
					encryptStart();
					}
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

	@SuppressLint("SdCardPath")
	private void encryptStart() {
		// Send all information needed to service to edit key in other thread
		Intent intent = new Intent(this, KeyIntentService.class);

		// fill values for this action
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

		// choose default settings, target and data bundle by target
		if (mEncryptTarget == Id.target.email) {
		useAsciiArmor = true;
		compressionId = Preferences.getPreferences(this).getDefaultMessageCompression();

		data.putInt(KeyIntentService.TARGET, KeyIntentService.TARGET_BYTES);

		String message = mMessage.getText().toString();

		data.putByteArray(KeyIntentService.ENCRYPT_MESSAGE_BYTES, message.getBytes());
		}
		
		if (mOverrideAsciiArmor) {
			useAsciiArmor = mAsciiArmorDemand;
		}
		
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
					if (mEncryptTarget == Id.target.email) {

						output = data.getString(KeyIntentService.RESULT_ENCRYPTED_STRING);
						Log.d("stm-9", "output: " + output);

						/* encode output to qr code */
						mImage = QrCodeUtils.getQRCodeBitmap(output, 1000 );

						Intent sendIntent = new Intent(Intent.ACTION_SEND);

						// Type is set to text/plain so that encrypted messages can
						// be sent with Whatsapp, Hangouts, SMS etc...
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

	private void initView() {
		mSourceLabel = (TextView) findViewById(R.id.sourceLabel);

		mSourceLabel.setClickable(true);

		mModeLabel = (TextView) findViewById(R.id.modeLabel);

		mModeLabel.setClickable(true);

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

	private void updateView() {
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

	@Override
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

		default: {
			break;
		}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}
}

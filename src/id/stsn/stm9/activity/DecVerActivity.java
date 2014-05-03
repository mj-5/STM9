package id.stsn.stm9.activity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;

import org.spongycastle.openpgp.PGPPublicKeyRing;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;
import id.stsn.stm9.fragment.PassphraseDialogFragment;
import id.stsn.stm9.pgp.NoAsymmetricEncryptionException;
import id.stsn.stm9.pgp.PgpGeneralException;
import id.stsn.stm9.pgp.PgpHelper;
import id.stsn.stm9.pgp.PgpKeyHelper;
import id.stsn.stm9.provider.ProviderHelper;
import id.stsn.stm9.services.ActionBarHelper;
import id.stsn.stm9.services.FileHelper;
import id.stsn.stm9.services.KeyIntentService;
import id.stsn.stm9.services.KeyIntentServiceHandler;
import id.stsn.stm9.services.PassphraseCacheService;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

public class DecVerActivity extends SherlockFragmentActivity{
	
    public static final String ACTION_DECRYPT = "id.stsn.stm9_9" +".action." + "DECRYPT";
    public static final String EXTRA_TEXT = "text";

    private String mDecryptString = "";
    
    private LinearLayout mSource = null;
    private TextView mSourceLabel = null;
    
    private EditText mMessage = null;
    private LinearLayout mSignatureLayout = null;
    private ImageView mSignatureStatusImage = null;
    private TextView mUserId = null;
    private TextView mUserIdRest = null;
    
    private ImageView mImage = null;

    private long mSignatureKeyId = 0;
    
    private boolean mDecryptImmediately = false;
    
    private Uri mContentUri = null;
    
    private int mDecryptTarget;
    
    private boolean mSignedOnly = false;
    
    private long mSecretKeyId = Id.kunci.none;

    private boolean mReturnBinary = false;

    private boolean mReturnResult = false;

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(1, Id.menu.opsi.decrypt, 0, mDecryptString).setShowAsAction(
				MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, MainMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
           
            return true;

        case Id.menu.opsi.decrypt: 
        	decryptClicked();

        	return true;

        default:
        	return super.onOptionsItemSelected(item);
        }
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.dec_ver_activity);
		
		ActionBarHelper.setBackButton(this);
		
		showLayout();
		
		handleActions(getIntent());

        mSignatureLayout.setVisibility(View.GONE);
        mSignatureLayout.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if (mSignatureKeyId == 0) {
					return;
				}
				
				PGPPublicKeyRing key = ProviderHelper.getPGPPublicKeyRingByKeyId(DecVerActivity.this, mSignatureKeyId);
				
				if (key !=null) {
					Log.d("stm9-dec", "gak ada kunci di database!");
				}
			}
		});

        invalidateOptionsMenu();
        
        if (mDecryptImmediately || (mSource.getId() == R.id.sourceMessage && (mMessage.getText().length() > 0 || mContentUri != null))) {
        	decryptClicked();
        }
	}

	private void showLayout() {
        mSource = (LinearLayout) findViewById(R.id.source);
        mSourceLabel = (TextView) findViewById(R.id.sourceLabel);

        mMessage = (EditText) findViewById(R.id.message);
        mSignatureLayout = (LinearLayout) findViewById(R.id.signature);
        mSignatureStatusImage = (ImageView) findViewById(R.id.ic_signature_status);
        mUserId = (TextView) findViewById(R.id.mainUserId);
        mUserIdRest = (TextView) findViewById(R.id.mainUserIdRest);
        
        mImage = (ImageView) findViewById(R.id.imageQr);
	}

	private void handleActions(Intent intent) {
		
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        String type = intent.getType();
        Uri uri = intent.getData();

        if (extras == null) {
            extras = new Bundle();
        }
        
        /*
         * Device's Action
         */
        if (Intent.ACTION_SEND.equals(action) && type != null) {
        	
            if ("text/plain".equals(type)) {
            	String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            	if (sharedText != null) {

            		extras.putString(EXTRA_TEXT, sharedText);
            		action = ACTION_DECRYPT;
            	}
            } else {
            	uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            	
            	/*
            	 *  decode file qr code to text
            	 */
            	
            	try {
            		String path = FileHelper.getPath(this, uri);
                    Log.i("images path", path);
                    
                    Bitmap bMap = BitmapFactory.decodeFile(path);
                    mImage.setImageBitmap(bMap);
                    
                    LuminanceSource source = new id.stsn.stm9.utility.RGBLuminanceSources(bMap);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                    
                    Reader reader = new MultiFormatReader();
                    Result result;
                    
                    result = reader.decode(bitmap);
                    String text = result.getText();
                    Log.i("decode result", text);
                    
                    extras.putString(EXTRA_TEXT, text);
                    action = ACTION_DECRYPT;
					
				} catch (Exception e) {
					e.printStackTrace();
				}
            }            
        }
        
        String textData = extras.getString(EXTRA_TEXT);
        
        /*
         * Main actions
         */
        if (ACTION_DECRYPT.equals(action) && textData != null) {
        	Log.d("stm-9", "textData null, matching text ...");
        	Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(textData);
        	if (matcher.matches()) {
        		Log.d("stm-9", "PGP_MESSAGE matched");
        		textData = matcher.group(1);
        		// replace non breakable spaces
        		textData = textData.replaceAll("\\xa0", " ");
        		mMessage.setText(textData);
        	} else {
        		matcher = PgpHelper.PGP_SIGNED_MESSAGE.matcher(textData);
        		if (matcher.matches()) {
        			Log.d("stm-9", "PGP_SIGNED_MESSAGE matched");
        			textData = matcher.group(1);
        			// replace non breakable spaces
        			textData = textData.replaceAll("\\xa0", " ");
        			mMessage.setText(textData);

        			mDecryptString = getString(R.string.tmbl_verify);
        			// build new action bar
        			invalidateOptionsMenu();
        		} else {
        			Log.d("stm-9", "Nothing matched!");
        		}
        	}
        } else {
        	Log.e("stm9-dec", " include extra text with setData() in Intent! ");
        }
	}

	private void decryptClicked() {
		Log.d("stm9-dec", "decrypt clicked");
		
        mDecryptTarget = Id.target.message;
        Log.d("stm9-dec", "target message");

		if (mDecryptTarget == Id.target.message) {
            String messageData = mMessage.getText().toString();
            Matcher matcher = PgpHelper.PGP_SIGNED_MESSAGE.matcher(messageData);
            if (matcher.matches()) {
                mSignedOnly = true;
                decryptStart();
                return;
            }
		}
		
		mSignedOnly = false;
		
		getDecryptionKeyFromInputStream();

		if (PassphraseCacheService.getCachedPassphrase(this, mSecretKeyId) == null) {
			showPassphraseDialog();
		} else {		

			decryptStart();
		}
	}

	private void showPassphraseDialog() {
		Handler returnHandler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
						decryptStart();
				}
			}
		};

		// Create a new Messenger for the communication back
		Messenger messenger = new Messenger(returnHandler);

		try {
			PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(this,
					messenger, mSecretKeyId);

			passphraseDialog.show(getSupportFragmentManager(), "passphraseDialog");
		} catch (PgpGeneralException e) {
			Log.d("stm-9", "No passphrase for this secret key, encrypt directly!");
			// send message to handler to start encryption directly
			returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
		}		
	}
	
	private void getDecryptionKeyFromInputStream() {
		InputStream inStream = null;
		
		inStream = new ByteArrayInputStream(mMessage.getText().toString().getBytes());
		
		// get decrypt key for this instream
		
		try {
			try {
				if (inStream.markSupported()) {
					inStream.mark(200);
				}
				
				mSecretKeyId = PgpHelper.getDecryptionKeyId(this, inStream);
				if (mSecretKeyId == Id.kunci.none) {
					throw new PgpGeneralException(getString(R.string.error_no_secret_key_found));
				}
			} catch (NoAsymmetricEncryptionException e) {
				if (inStream.markSupported()) {
					inStream.reset();
				}
				
			}
		} catch (Exception e) {
			Toast.makeText(this, getString(R.string.error_message, e.getMessage()), Toast.LENGTH_SHORT).show();
		}
	}
	
	private void decryptStart() {
		Log.d("stm9-dec", "decrypt started!");
		
		Intent intent = new Intent(this, KeyIntentService.class);
		
		Bundle data = new Bundle();
		
		intent.setAction(KeyIntentService.ACTION_DECRYPT_VERIFY);
		
		data.putInt(KeyIntentService.TARGET, KeyIntentService.TARGET_BYTES);
		
		String message = mMessage.getText().toString();
		data.putByteArray(KeyIntentService.DECRYPT_CIPHERTEXT_BYTES, message.getBytes());
		
		data.putLong(KeyIntentService.ENCRYPT_SECRET_KEY_ID, mSecretKeyId);
		data.putBoolean(KeyIntentService.DECRYPT_SIGNED_ONLY, mSignedOnly);
        data.putBoolean(KeyIntentService.DECRYPT_RETURN_BYTES, mReturnBinary);

        intent.putExtra(KeyIntentService.EXTRA_DATA, data);
        
        KeyIntentServiceHandler saveHandler = new KeyIntentServiceHandler(this, R.string.proses_decrypting, ProgressDialog.STYLE_HORIZONTAL) {
        	public void handleMessage(Message message) {
        		super.handleMessage(message);
        		
        		if (message.arg1 == KeyIntentServiceHandler.MESSAGE_OKAY) {
        			Bundle returnData = message.getData();
        		
        		
                mSignatureKeyId = 0;
                mSignatureLayout.setVisibility(View.GONE);

                // show action bar
                invalidateOptionsMenu();

                Toast.makeText(DecVerActivity.this, R.string.decryption_successful, Toast.LENGTH_SHORT).show();

                if (mReturnResult) {
                	Intent intent = new Intent();
                	intent.putExtras(returnData);
                	setResult(RESULT_OK, intent);
                	finish();
                	
                	return;
                }
                
                // decrypt target
                String decryptedMessage = returnData.getString(KeyIntentService.RESULT_DECRYPTED_STRING);
                mMessage.setText(decryptedMessage);
                mMessage.setHorizontallyScrolling(false);
                
                // make new action bar
                invalidateOptionsMenu();
                
                /* ------------------------------------------------ */
                
                if (returnData.getBoolean(KeyIntentService.RESULT_SIGNATURE)) {
                    String userId = returnData.getString(KeyIntentService.RESULT_SIGNATURE_USER_ID);
                    mSignatureKeyId = returnData.getLong(KeyIntentService.RESULT_SIGNATURE_KEY_ID);
                    mUserIdRest.setText("id: " + PgpKeyHelper.convertKeyIdToHex(mSignatureKeyId));
                    if (userId == null) {
                    	userId = getResources().getString(R.string.unknown_user_id);
                    }
                    
                    String chunks[] = userId.split(" <", 2);
                    userId = chunks[0];
                    if (chunks.length > 1) {
                        mUserIdRest.setText("<" + chunks[1]);
                    }
                    mUserId.setText(userId);
                    
                    if (returnData.getBoolean(KeyIntentService.RESULT_SIGNATURE_SUCCESS)) {
                        mSignatureStatusImage.setImageResource(R.drawable.overlay_ok);
                    } else {
                    	mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
                    }
                    
                    mSignatureLayout.setVisibility(View.VISIBLE);
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
}

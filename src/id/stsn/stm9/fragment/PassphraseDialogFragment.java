package id.stsn.stm9.fragment;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;
import id.stsn.stm9.pgp.PgpGeneralException;
import id.stsn.stm9.pgp.PgpKeyHelper;
import id.stsn.stm9.provider.ProviderHelper;
import id.stsn.stm9.services.PassphraseCacheService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.util.Log;

public class PassphraseDialogFragment extends DialogFragment implements OnEditorActionListener {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_SECRET_KEY_ID = "secret_key_id";

    public static final int MESSAGE_OKAY = 1;
    public static final int MESSAGE_CANCEL = 2;

    private Messenger vMessenger;
    private EditText vPassphraseEditText;
    private boolean canKB;

    public static PassphraseDialogFragment newInstance(Context context, Messenger messenger,
            long secretKeyId) throws PgpGeneralException {
        // check if secret key has a passphrase
        if (!(secretKeyId == Id.kunci.symmetric || secretKeyId == Id.kunci.none)) {
            if (!PassphraseCacheService.hasPassphrase(context, secretKeyId)) {
                throw new PgpGeneralException("No passphrase! No passphrase dialog needed!");
            }
        }

        PassphraseDialogFragment frag = new PassphraseDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SECRET_KEY_ID, secretKeyId);
        args.putParcelable(ARG_MESSENGER, messenger);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        final long secretKeyId = getArguments().getLong(ARG_SECRET_KEY_ID);
        vMessenger = getArguments().getParcelable(ARG_MESSENGER);

        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setTitle(R.string.title_authentication);

        final PGPSecretKey secretKey;

        if (secretKeyId == Id.kunci.symmetric || secretKeyId == Id.kunci.none) {
          secretKey = null;
            alert.setMessage(R.string.passphrase_for_symmetric_encryption);
        } else {
            // TODO: by master key id???
            secretKey = PgpKeyHelper.getMasterKey(ProviderHelper.getPGPSecretKeyRingByKeyId(activity,
                    secretKeyId));
            // secretKey = PGPHelper.getMasterKey(PGPMain.getSecretKeyRing(secretKeyId));

            if (secretKey == null) {
                alert.setTitle(R.string.title_key_not_found);
                alert.setMessage(getString(R.string.key_not_found, secretKeyId));
                alert.setPositiveButton(android.R.string.ok, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });
                alert.setCancelable(false);
                canKB = false;
                return alert.create();
            }
            String userId = PgpKeyHelper.getMainUserIdSafe(activity, secretKey);

            Log.d("stm-9", "User id: '" + userId + "'");
            alert.setMessage(getString(R.string.passphrase_for, userId));
        }

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.passphrase, null);
        alert.setView(view);

        vPassphraseEditText = (EditText) view.findViewById(R.id.passphrase_once);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
                long curKeyIndex = 1;
                boolean keyOK = true;
                String passPhrase = vPassphraseEditText.getText().toString();
                long keyId;
                PGPSecretKey clickSecretKey = secretKey;

                if (clickSecretKey != null) {
                    while (keyOK == true) {
                        if (clickSecretKey != null) { // check again for loop
                            try {
                                PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder()
                                        .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(
                                                passPhrase.toCharArray());
                                PGPPrivateKey testKey = clickSecretKey
                                        .extractPrivateKey(keyDecryptor);
                                if (testKey == null) {
                                    if (!clickSecretKey.isMasterKey()) {
                                        Toast.makeText(activity,
                                                R.string.error_could_not_extract_private_key,
                                                Toast.LENGTH_SHORT).show();
                                        
                                        sendMessageToHandler(MESSAGE_CANCEL);
                                        return;
                                    } else {
                                        clickSecretKey = PgpKeyHelper.getKeyNum(ProviderHelper
                                                .getPGPSecretKeyRingByKeyId(activity, secretKeyId),
                                                curKeyIndex);
                                        curKeyIndex++; // does post-increment work like C?
                                        continue;
                                    }
                                } else {
                                    keyOK = false;
                                }
                            } catch (PGPException e) {
                                Toast.makeText(activity, R.string.wrong_passphrase,
                                        Toast.LENGTH_SHORT).show();
                                
                                sendMessageToHandler(MESSAGE_CANCEL);
                                return;
                            }
                        } else {
                            Toast.makeText(activity, R.string.error_could_not_extract_private_key,
                                    Toast.LENGTH_SHORT).show();
                            
                            sendMessageToHandler(MESSAGE_CANCEL);
                            return; // ran out of keys to try
                        }
                    }
                    keyId = secretKey.getKeyID();
                } else {
                    keyId = Id.kunci.symmetric;
                }

                // cache the new passphrase
                Log.d("stm-9", "Everything okay! Caching entered passphrase");
                PassphraseCacheService.addCachedPassphrase(activity, keyId, passPhrase);
                if (keyOK == false && clickSecretKey.getKeyID() != keyId) {
                    PassphraseCacheService.addCachedPassphrase(activity, clickSecretKey.getKeyID(),
                            passPhrase);
                }

                sendMessageToHandler(MESSAGE_OKAY);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        canKB = true;
        return alert.create();
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);
        if (canKB) {
            // request focus and open soft keyboard
            vPassphraseEditText.requestFocus();
            getDialog().getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            vPassphraseEditText.setOnEditorActionListener(this);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        dismiss();
        sendMessageToHandler(MESSAGE_CANCEL);
    }

    /**
     * Associate the "done" button on the soft keyboard with the okay button in the view
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            AlertDialog dialog = ((AlertDialog) getDialog());
            Button bt = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            bt.performClick();
            return true;
        }
        return false;
    }

    /**
     * Send message back to handler which is initialized in a activity
     * 
     * @param what
     *            Message integer you want to send
     */
    private void sendMessageToHandler(Integer what) {
        Message msg = Message.obtain();
        msg.what = what;

        try {
            vMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w("stm-9", "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w("stm-9", "Messenger is null!", e);
        }
    }

}

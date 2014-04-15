package id.stsn.stm9.fragment;

import id.stsn.stm9.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;
import android.util.Log;

public class SetPassphraseDialogFragment extends DialogFragment implements OnEditorActionListener {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_TITLE = "title";
    public static final String MESSAGE_NEW_PASSPHRASE = "new_passphrase";

    private Messenger mMessenger;
    private EditText mPassphraseEditText1;
    private EditText mPassphraseEditText2;
    
    public static final int MESSAGE_OKAY = 1;

    /**
     * Dialog fragment untuk insert passphrase
     */
    public static SetPassphraseDialogFragment nInstance(Messenger messenger, int title) {
        SetPassphraseDialogFragment frag = new SetPassphraseDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_TITLE, title);
        bundle.putParcelable(ARG_MESSENGER, messenger);

        frag.setArguments(bundle);

        return frag;
    }

    /**
     * membuat dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        int title = getArguments().getInt(ARG_TITLE);
        mMessenger = getArguments().getParcelable(ARG_MESSENGER);

        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setTitle(title);
        alert.setMessage(R.string.enter_passphrase_twice);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.passphrase_ulangi, null);
        alert.setView(view);

        mPassphraseEditText1 = (EditText) view.findViewById(R.id.passphrase_once);
        mPassphraseEditText2 = (EditText) view.findViewById(R.id.passphrase_twice);

        // OK button
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                String passphraseOnce = mPassphraseEditText1.getText().toString();
                String passphraseTwice = mPassphraseEditText2.getText().toString();
                
                // if passphrase not same when input
                if (!passphraseOnce.equals(passphraseTwice)) {
                    Toast.makeText(
                            activity,
                            getString(R.string.error_message,
                                    getString(R.string.passphrases_do_not_match)), Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                // if passphrase empty
                if (passphraseOnce.equals("")) {
                    Toast.makeText(
                            activity,
                            getString(R.string.error_message,
                                    getString(R.string.passphrase_must_not_be_empty)),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // return resulting data back to activity
                Bundle data = new Bundle();
                data.putString(MESSAGE_NEW_PASSPHRASE, passphraseOnce);

                sendMessageToHandler(MESSAGE_OKAY, data);
            }
        });

        // Cancel button
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            
        	// back to KeyActivity
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        return alert.create();
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);

        // request focus and open soft keyboard
        mPassphraseEditText1.requestFocus();
        getDialog().getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        mPassphraseEditText2.setOnEditorActionListener(this);
    }

    /**
     * Associate "done" button on the soft keyboard with the okay button in the view
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            AlertDialog dialog = ((AlertDialog) getDialog());
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            button.performClick();
            return true;
        }
        return false;
    }

    /**
     * Send message back to handler which is initialized in a activity
     */
    private void sendMessageToHandler(Integer what, Bundle data) {
        Message msg = Message.obtain();
        msg.what = what;
        if (data != null) {
            msg.setData(data);
        }

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w("stm-9", "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w("stm-9", "Messenger is null!", e);
        }
    }
}
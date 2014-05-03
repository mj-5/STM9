package id.stsn.stm9.fragment;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;
import id.stsn.stm9.activity.KeyServerQueryActivity;
import id.stsn.stm9.pgp.PgpKeyHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.util.Log;


public class LookupUnknownKeyDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_UNKNOWN_KEY_ID = "unknown_key_id";

    public static final int MESSAGE_OKAY = 1;
    public static final int MESSAGE_CANCEL = 2;

    private Messenger mMessenger;

    /**
     * Creates new instance of this dialog fragment
     * 
     * @param messenger
     * @param unknownKeyId
     * @return
     */
    public static LookupUnknownKeyDialogFragment newInstance(Messenger messenger, long unknownKeyId) {
        LookupUnknownKeyDialogFragment frag = new LookupUnknownKeyDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_UNKNOWN_KEY_ID, unknownKeyId);
        args.putParcelable(ARG_MESSENGER, messenger);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        final long unknownKeyId = getArguments().getLong(ARG_UNKNOWN_KEY_ID);
        mMessenger = getArguments().getParcelable(ARG_MESSENGER);

        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setIcon(android.R.drawable.ic_dialog_alert);
        alert.setTitle(R.string.title_unknown_signature_key);
        alert.setMessage(getString(R.string.lookup_unknown_key,
                PgpKeyHelper.convertKeyIdToHex(unknownKeyId)));

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                sendMessageToHandler(MESSAGE_OKAY);

                Intent intent = new Intent(activity, KeyServerQueryActivity.class);
                intent.setAction(KeyServerQueryActivity.ACTION_LOOK_UP_KEY_ID);
                intent.putExtra(KeyServerQueryActivity.EXTRA_KEY_ID, unknownKeyId);
                startActivityForResult(intent, Id.request.look_up_key_id);
            }
        });
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                sendMessageToHandler(MESSAGE_CANCEL);
            }
        });
        alert.setCancelable(true);
        alert.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                sendMessageToHandler(MESSAGE_CANCEL);
            }
        });

        return alert.create();
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
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w("stm-9", "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w("stm-9", "Messenger is null!", e);
        }
    }
}
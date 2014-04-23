package id.stsn.stm9.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;

public class ProgressDialogFragment extends DialogFragment {
    private static final String ARG_MESSAGE_ID = "message_id";
    private static final String ARG_STYLE = "style";

    /**
     * Creates new instance of this fragment
     * 
     * @param id
     * @return
     */
    public static ProgressDialogFragment newInstance(int messageId, int style) {
    	ProgressDialogFragment frag = new ProgressDialogFragment();
    	Bundle args = new Bundle();
    	args.putInt(ARG_MESSAGE_ID, messageId);
    	args.putInt(ARG_STYLE, style);

    	frag.setArguments(args);
    	return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();

        ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        int messageId = getArguments().getInt(ARG_MESSAGE_ID);
        int style = getArguments().getInt(ARG_STYLE);

        dialog.setMessage(getString(messageId));
        dialog.setProgressStyle(style);

        // Disable the back button
        OnKeyListener keyListener = new OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;
                }
                return false;
            }

        };
        dialog.setOnKeyListener(keyListener);

        return dialog;
    }
}
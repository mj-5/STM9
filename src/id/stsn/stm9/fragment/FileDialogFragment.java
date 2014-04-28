package id.stsn.stm9.fragment;

import id.stsn.stm9.R;
import id.stsn.stm9.services.FileHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.util.Log;

public class FileDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_DEFAULT_FILE = "default_file";
    private static final String ARG_CHECKBOX_TEXT = "checkbox_text";
    private static final String ARG_REQUEST_CODE = "request_code";

    public static final int MESSAGE_OKAY = 1;

    public static final String MESSAGE_DATA_FILENAME = "filename";
    public static final String MESSAGE_DATA_CHECKED = "checked";

    private Messenger mMessenger;

    /**
     * Creates new instance of this file dialog fragment
     * 
     * @param messenger
     * @param title
     * @param message
     * @param defaultFile
     * @param checkboxText
     * @param requestCode
     * @return
     */
    public static FileDialogFragment newInstance(Messenger messenger, String title, String message,
            String defaultFile, String checkboxText, int requestCode) {
        FileDialogFragment frag = new FileDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSENGER, messenger);

        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_DEFAULT_FILE, defaultFile);
        args.putString(ARG_CHECKBOX_TEXT, checkboxText);
        args.putInt(ARG_REQUEST_CODE, requestCode);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        mMessenger = getArguments().getParcelable(ARG_MESSENGER);

        String title = getArguments().getString(ARG_TITLE);
        String message = getArguments().getString(ARG_MESSAGE);
        String defaultFile = getArguments().getString(ARG_DEFAULT_FILE);
        String checkboxText = getArguments().getString(ARG_CHECKBOX_TEXT);
        final int requestCode = getArguments().getInt(ARG_REQUEST_CODE);

        final EditText mFilename;
        final ImageButton mBrowse;
        final CheckBox mCheckBox;

        LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setTitle(title);
        alert.setMessage(message);

        View view = inflater.inflate(R.layout.file_dialog, null);

        mFilename = (EditText) view.findViewById(R.id.input);
        mFilename.setText(defaultFile);
        mBrowse = (ImageButton) view.findViewById(R.id.btn_browse);
        mBrowse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // only .asc or .gpg files
                // setting it to text/plain prevents Cynaogenmod's file manager from selecting asc or gpg types!
                FileHelper.openFile(activity, mFilename.getText().toString(), "*/*",
                        requestCode);
            }
        });

        mCheckBox = (CheckBox) view.findViewById(R.id.checkbox);
        if (checkboxText == null) {
            mCheckBox.setEnabled(false);
            mCheckBox.setVisibility(View.GONE);
        } else {
            mCheckBox.setEnabled(true);
            mCheckBox.setVisibility(View.VISIBLE);
            mCheckBox.setText(checkboxText);
        }

        alert.setView(view);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                boolean checked = false;
                if (mCheckBox.isEnabled()) {
                    checked = mCheckBox.isChecked();
                }

                // return resulting data back to activity
                Bundle data = new Bundle();
                data.putString(MESSAGE_DATA_FILENAME, mFilename.getText().toString());
                data.putBoolean(MESSAGE_DATA_CHECKED, checked);

                sendMessageToHandler(MESSAGE_OKAY, data);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
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
    private void sendMessageToHandler(Integer what, Bundle data) {
        Message msg = Message.obtain();
        msg.what = what;
        if (data != null) {
            msg.setData(data);
        }

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w("Stm-9", "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w("Stm-9", "Messenger is null!", e);
        }
    }
    
    public void setFilename(String filename) {
        AlertDialog dialog = (AlertDialog) getDialog();
        EditText filenameEditText = (EditText) dialog.findViewById(R.id.input);

        if (filenameEditText != null) {
            filenameEditText.setText(filename);
        }
    }
}
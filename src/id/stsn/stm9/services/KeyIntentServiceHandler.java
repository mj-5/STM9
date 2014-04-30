package id.stsn.stm9.services;

import id.stsn.stm9.fragment.ProgressDialogFragment;
import id.stsn.stm9.R;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;
import android.os.Bundle;

public class KeyIntentServiceHandler extends Handler {

    // possible messages send from this service to handler on ui
    public static final int MESSAGE_OKAY = 1;
    public static final int MESSAGE_EXCEPTION = 2;
    public static final int MESSAGE_UPDATE_PROGRESS = 3;

  // possible data keys for messages
  public static final String DATA_ERROR = "error";
  public static final String DATA_PROGRESS = "progress";
  public static final String DATA_PROGRESS_MAX = "max";
  public static final String DATA_MESSAGE = "message";

    Activity mActivity;
    ProgressDialogFragment mProgressDialogFragment;
    
    public KeyIntentServiceHandler(Activity activity) {
        this.mActivity = activity;
    }

    public KeyIntentServiceHandler(Activity activity, int progressDialogMessageId, int progressDialogStyle) {
        this.mActivity = activity;
        this.mProgressDialogFragment = ProgressDialogFragment.newInstance(progressDialogMessageId,
                progressDialogStyle);
    }
    
    public KeyIntentServiceHandler(Activity activity, ProgressDialogFragment progressDialogFragment) {
    	this.mActivity = activity;
    	this.mProgressDialogFragment = progressDialogFragment;
    }

    public void showProgressDialog(FragmentActivity activity) {
        mProgressDialogFragment.show(activity.getSupportFragmentManager(), "progressDialog");
    }

    @Override
    public void handleMessage(Message message) {
    	Bundle data = message.getData();

        switch (message.arg1) {
        case MESSAGE_OKAY:
            mProgressDialogFragment.dismiss();

            break;
            
        case MESSAGE_EXCEPTION:
            mProgressDialogFragment.dismiss();

            // show error from service
            if (data.containsKey(DATA_ERROR)) {
                Toast.makeText(mActivity, mActivity.getString(R.string.error_message, data.getString(DATA_ERROR)), Toast.LENGTH_SHORT).show();
            }
            
            break;

        default:
            break;
        }
    }
}

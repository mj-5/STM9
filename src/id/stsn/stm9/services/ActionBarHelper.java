package id.stsn.stm9.services;

import id.stsn.stm9.R;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.util.Log;

public class ActionBarHelper {

    /**
     * Set actionbar without home button if called from another app
     */
    public static void setBackButton(SherlockFragmentActivity activity) {
        final ActionBar actionBar = activity.getSupportActionBar();
        Log.d("Stm-9", "calling package (only set when using startActivityForResult)=" + activity.getCallingPackage());
        if (activity.getCallingPackage() != null && activity.getCallingPackage().equals("id.stsn.stm9")) {actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        } else {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
        }
    }

    /**
     * Sets custom view on ActionBar for Save/Cancel activities
     */
    public static void setDoneCancelView(ActionBar actionBar, int doneText, OnClickListener doneOnClickListener, int cancelText, OnClickListener cancelOnClickListener) {

        // Inflate a "Save"/"Cancel" custom action bar view
        final LayoutInflater inflater = (LayoutInflater) actionBar.getThemedContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(R.layout.bar_view_simpan_batal, null);

        // save
        ((TextView) customActionBarView.findViewById(R.id.bar_simpan_text)).setText(doneText);
        customActionBarView.findViewById(R.id.bar_simpan).setOnClickListener(doneOnClickListener);
        
        // cancel
        ((TextView) customActionBarView.findViewById(R.id.bar_batal_text)).setText(cancelText);
        customActionBarView.findViewById(R.id.bar_batal).setOnClickListener(cancelOnClickListener);

        // Show the custom action bar view and hide the normal Home icon and title.
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM
                | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }
}

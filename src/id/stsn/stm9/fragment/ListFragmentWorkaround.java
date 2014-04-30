package id.stsn.stm9.fragment;

import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * Bug on Android >= 4.1
 * 
 * http://code.google.com/p/android/issues/detail?id=35885
 * 
 * Items are not checked in layout
 */
public class ListFragmentWorkaround extends SherlockListFragment {

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        l.setItemChecked(position, l.isItemChecked(position));
    }
}

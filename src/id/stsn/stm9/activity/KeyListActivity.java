package id.stsn.stm9.activity;

import id.stsn.stm9.Id;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import id.stsn.stm9.R;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;

public class KeyListActivity extends SherlockFragmentActivity{
	
    protected int mKeyType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        handleActions(getIntent());
    }
    
    protected void handleActions(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null) {
            extras = new Bundle();
        }
        
        /**
         * Android Standard Actions
         */
        String searchString = null;
        if (Intent.ACTION_SEARCH.equals(action)) {
            searchString = extras.getString(SearchManager.QUERY);
            if (searchString != null && searchString.trim().length() == 0) {
                searchString = null;
            }
        }
    }

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
        // TODO: reimplement!
        // menu.add(3, Id.menu.option.search, 0, R.string.menu_search)
        // .setIcon(R.drawable.ic_menu_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
//        menu.add(0, Id.menu.option.import_from_file, 5, R.string.menu_import_from_file)
//                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//        menu.add(0, Id.menu.option.export_keys, 6, R.string.menu_export_keys).setShowAsAction(
//                MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//
        return true;
	}
}

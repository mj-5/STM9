package id.stsn.stm9.activity;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;

/**
 * Display all public keys and can search public keys on key server
 * @author 1009100850
 *
 */
public class KeyListActivity extends ListKeyActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
        tipeKunci = Id.tipe.public_key;

        setContentView(R.layout.key_list_activity);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(1, Id.menu.opsi.key_server, 1, R.string.menu_key_server).setIcon(R.drawable.ic_menu_search_list)
              .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case Id.menu.opsi.key_server: {
            startActivityForResult(new Intent(this, KeyServerQueryActivity.class), 0);

            return true;
        }
        
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }
}

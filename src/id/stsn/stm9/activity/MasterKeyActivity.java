package id.stsn.stm9.activity;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MasterKeyActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.master_key_activity);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(1, Id.menu.opsi.create, 1, R.string.menu_buat_kunci).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }

    /* membuat pilihan untuk membuat kunci baru*/
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case Id.menu.opsi.create: {
            buatKunci();
            return true;
        }

        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    /* buat kunci baru secara general */
    private void buatKunci() {

    }
}

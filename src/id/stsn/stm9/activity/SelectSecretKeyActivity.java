package id.stsn.stm9.activity;

import id.stsn.stm9.R;
import id.stsn.stm9.fragment.SelectSecretKeyFragment;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;

public class SelectSecretKeyActivity extends SherlockFragmentActivity {

    // Actions for internal use only:
    public static final String ACTION_SELECT_SECRET_KEY = "id.stsn.stm9" + ".action."
            + "SELECT_SECRET_KEYRING";

    public static final String EXTRA_FILTER_CERTIFY = "filter_certify";

    public static final String RESULT_EXTRA_MASTER_KEY_ID = "master_key_id";
    public static final String RESULT_EXTRA_USER_ID = "user_id";

    private boolean mFilterCertify = false;
    private SelectSecretKeyFragment mSelectFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_seckey_activity);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        mFilterCertify = getIntent().getBooleanExtra(EXTRA_FILTER_CERTIFY, false);

        handleIntent(getIntent());

        if (findViewById(R.id.select_secret_key_fragment_container) != null) {

            if (savedInstanceState != null) {
                return;
            }

            // Create an instance of the fragment
            mSelectFragment = SelectSecretKeyFragment.newInstance(mFilterCertify);

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.select_secret_key_fragment_container, mSelectFragment).commit();
        }
    }

    /**
     * This is executed by SelectSecretKeyFragment after clicking on an item
     * 
     * @param masterKeyId
     * @param userId
     */
    public void afterListSelection(long masterKeyId, String userId) {
        Intent data = new Intent();
        data.putExtra(RESULT_EXTRA_MASTER_KEY_ID, masterKeyId);
        data.putExtra(RESULT_EXTRA_USER_ID, (String) userId);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /**
     * Menu Options
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, MainMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
}

package id.stsn.stm9.activity;

import id.stsn.stm9.R;
import id.stsn.stm9.fragment.SelectPublicKeyFragment;
import id.stsn.stm9.services.ActionBarHelper;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class SelectPublicKeyActivity extends SherlockFragmentActivity {

    // Actions for internal use only:
    public static final String ACTION_SELECT_PUBLIC_KEYS = "id.stsn.stm9" + ".action."
            + "SELECT_PUBLIC_KEYRINGS";

    public static final String EXTRA_SELECTED_MASTER_KEY_IDS = "master_key_ids";

    public static final String RESULT_EXTRA_MASTER_KEY_IDS = "master_key_ids";
    public static final String RESULT_EXTRA_USER_IDS = "user_ids";

    SelectPublicKeyFragment mSelectFragment;

    long selectedMasterKeyIds[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBarHelper.setDoneCancelView(getSupportActionBar(), R.string.tmbl_okay,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ok
                        okClicked();
                    }
                }, R.string.tmbl_do_not_save, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // cancel
                        cancelClicked();
                    }
                });

        setContentView(R.layout.select_pubkey_activity);

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        handleIntent(getIntent());

        if (findViewById(R.id.select_public_key_fragment_container) != null) {

            if (savedInstanceState != null) {
                return;
            }

            mSelectFragment = SelectPublicKeyFragment.newInstance(selectedMasterKeyIds);

            getSupportFragmentManager().beginTransaction().add(R.id.select_public_key_fragment_container, mSelectFragment).commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
    }

    private void cancelClicked() {
        setResult(RESULT_CANCELED, null);
        finish();
    }

    private void okClicked() {
        Intent data = new Intent();
        data.putExtra(RESULT_EXTRA_MASTER_KEY_IDS, mSelectFragment.getSelectedMasterKeyIds());
        data.putExtra(RESULT_EXTRA_USER_IDS, mSelectFragment.getSelectedUserIds());
        setResult(RESULT_OK, data);
        finish();
    }

}

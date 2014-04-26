package id.stsn.stm9.activity;

import java.util.ArrayList;
import java.util.List;

import id.stsn.stm9.fragment.DeleteFileDialogFragment;
import id.stsn.stm9.R;
import id.stsn.stm9.fragment.FileDialogFragment;
import id.stsn.stm9.fragment.ImportKeysListFragment;
import id.stsn.stm9.fragment.ImportKeysServerFragment;
import id.stsn.stm9.services.ActionBarHelper;
import id.stsn.stm9.services.ImportKeysListEntry;
import id.stsn.stm9.services.KeyIntentService;
import id.stsn.stm9.services.KeyIntentServiceHandler;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Toast;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class ImportKeysActivity extends SherlockFragmentActivity implements OnNavigationListener {
    public static final String ACTION_IMPORT_KEY = "id.stsn.stm9" + ".action." + "IMPORT_KEY";

    // only used by IMPORT
    public static final String EXTRA_KEY_BYTES = "key_bytes";

    protected boolean mDeleteAfterImport = false;

    FileDialogFragment mFileDialog;
    ImportKeysListFragment mListFragment;
    OnNavigationListener mOnNavigationListener;
    String[] mNavigationStrings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.import_key_activity);

        // set actionbar without home button if called from another app
        ActionBarHelper.setBackButton(this);

        handleActions(savedInstanceState, getIntent());
    }

    protected void handleActions(Bundle savedInstanceState, Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        /**
         * Android Standard Actions
         */
        if (Intent.ACTION_VIEW.equals(action)) {
        	
            action = ACTION_IMPORT_KEY;
        }

        /**
         * App Actions
         */
        if (ACTION_IMPORT_KEY.equals(action)) {
        	if (extras.containsKey(EXTRA_KEY_BYTES)) {
        		byte[] importData = intent.getByteArrayExtra(EXTRA_KEY_BYTES);

        		/* directly load data */ 
        		startListFragment(savedInstanceState, importData, null);
        	}
        }
    }

    private void startListFragment(Bundle savedInstanceState, byte[] bytes, String filename) {
        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.import_keys_list_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create an instance of the fragment
            mListFragment = ImportKeysListFragment.newInstance(bytes, filename);

            // Add the fragment to the 'fragment_container' FrameLayout
            // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.import_keys_list_container, mListFragment)
                    .commitAllowingStateLoss();
            // do it immediately!
            getSupportFragmentManager().executePendingTransactions();
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        // Create new fragment from our own Fragment class
        switch (itemPosition) {
        case 0:
            loadFragment(ImportKeysServerFragment.class, null, mNavigationStrings[itemPosition]);
            break;

        default:
            break;
        }
        return true;
    }

    private void loadFragment(Class<?> clss, Bundle args, String tag) {
        Fragment fragment = Fragment.instantiate(this, clss.getName(), args);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // Replace whatever is in the fragment container with this fragment
        // and give the fragment a tag name equal to the string at the position selected
        ft.replace(R.id.import_navigation_fragment, fragment, tag);
        // Apply changes
        ft.commit();
    }

    public void loadCallback(byte[] importData, String importFilename) {
        mListFragment.loadNew(importData, importFilename);
    }

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

    /**
     * Import keys with mImportData
     */
    public void importKeys() {
        if (mListFragment.getKeyBytes() != null || mListFragment.getImportFilename() != null) {
            Log.d("stm-9", "importKeys started");

            // Send all information needed to service to import key in other thread
            Intent intent = new Intent(this, KeyIntentService.class);

            intent.setAction(KeyIntentService.ACTION_IMPORT_KEYRING);

            // fill values for this action
            Bundle data = new Bundle();

            // get selected key ids
            List<ImportKeysListEntry> listEntries = mListFragment.getData();
            ArrayList<Long> selectedKeyIds = new ArrayList<Long>();
            for (ImportKeysListEntry entry : listEntries) {
                if (entry.isSelected()) {
                    selectedKeyIds.add(entry.keyId);
                }
            }

            data.putSerializable(KeyIntentService.IMPORT_KEY_LIST, selectedKeyIds);

            if (mListFragment.getKeyBytes() != null) {
                data.putInt(KeyIntentService.TARGET, KeyIntentService.TARGET_BYTES);
                data.putByteArray(KeyIntentService.IMPORT_BYTES, mListFragment.getKeyBytes());
            }

            intent.putExtra(KeyIntentService.EXTRA_DATA, data);

            // Message is received after importing is done in ApgService
            KeyIntentServiceHandler saveHandler = new KeyIntentServiceHandler(this,
                    R.string.proses_importing, ProgressDialog.STYLE_HORIZONTAL) {
                public void handleMessage(Message message) {
                    // handle messages by standard ApgHandler first
                    super.handleMessage(message);

                    if (message.arg1 == KeyIntentServiceHandler.MESSAGE_OKAY) {
                        // get returned data bundle
                        Bundle returnData = message.getData();

                        int added = returnData.getInt(KeyIntentService.RESULT_IMPORT_ADDED);
                        int updated = returnData
                                .getInt(KeyIntentService.RESULT_IMPORT_UPDATED);
                        int bad = returnData.getInt(KeyIntentService.RESULT_IMPORT_BAD);
                        String toastMessage;
                        if (added > 0 && updated > 0) {
                            toastMessage = getString(R.string.keys_added_and_updated, added, updated);
                        } else if (added > 0) {
                            toastMessage = getString(R.string.keys_added, added);
                        } else if (updated > 0) {
                            toastMessage = getString(R.string.keys_updated, updated);
                        } else {
                            toastMessage = getString(R.string.no_keys_added_or_updated);
                        }
                        Toast.makeText(ImportKeysActivity.this, toastMessage, Toast.LENGTH_SHORT)
                                .show();
                        if (bad > 0) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(
                                    ImportKeysActivity.this);

                            alert.setIcon(android.R.drawable.ic_dialog_alert);
                            alert.setTitle(R.string.warning);
                            alert.setMessage(ImportKeysActivity.this.getString(
                                    R.string.bad_keys_encountered, bad));

                            alert.setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                            alert.setCancelable(true);
                            alert.create().show();
                        } else if (mDeleteAfterImport) {
                            // everything went well, so now delete, if that was turned on
                            DeleteFileDialogFragment deleteFileDialog = DeleteFileDialogFragment
                                    .newInstance(mListFragment.getImportFilename());
                            deleteFileDialog.show(getSupportFragmentManager(), "deleteDialog");
                        }
                    }
                };
            };

            // Create a new Messenger for the communication back
            Messenger messenger = new Messenger(saveHandler);
            intent.putExtra(KeyIntentService.EXTRA_MESSENGER, messenger);

            // show progress dialog
            saveHandler.showProgressDialog(this);

            // start service with intent
            startService(intent);
        } else {
            Toast.makeText(this, R.string.error_nothing_import, Toast.LENGTH_LONG).show();
        }
    }

    public void importOnClick(View view) {
        importKeys();
    }
}

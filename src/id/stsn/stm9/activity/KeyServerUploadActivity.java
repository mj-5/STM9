package id.stsn.stm9.activity;

import id.stsn.stm9.R;
import id.stsn.stm9.services.KeyIntentService;
import id.stsn.stm9.services.KeyIntentServiceHandler;
import id.stsn.stm9.services.Preferences;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Sends the selected public key to a key server
 */
public class KeyServerUploadActivity extends SherlockFragmentActivity {

    // Not used in sourcode, but listed in AndroidManifest!
    public static final String ACTION_EXPORT_KEY_TO_SERVER = "id.stsn.stm9" + ".action." + "EXPORT_KEY_TO_SERVER";

    public static final String EXTRA_KEYRING_ROW_ID = "key_id";

    private Button export;
    private Spinner keyServer;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, KeyListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

        default:
            break;

        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.export_keyserver_activity);

        export = (Button) findViewById(R.id.btn_export_to_server);
        keyServer = (Spinner) findViewById(R.id.keyServer);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, 
        		Preferences.getPreferences(this).getKeyServers());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keyServer.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            keyServer.setSelection(0);
        } else {
            export.setEnabled(false);
        }

        export.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadKey();
            }
        });
    }

    private void uploadKey() {
        // Send all information needed to service to upload key in other thread
        Intent intent = new Intent(this, KeyIntentService.class);

        intent.setAction(KeyIntentService.ACTION_UPLOAD_KEYRING);

        // fill values for this action
        Bundle data = new Bundle();

        int keyRingId = getIntent().getIntExtra(EXTRA_KEYRING_ROW_ID, -1);
        data.putInt(KeyIntentService.UPLOAD_KEY_KEYRING_ROW_ID, keyRingId);

        String server = (String) keyServer.getSelectedItem();
        data.putString(KeyIntentService.UPLOAD_KEY_SERVER, server);

        intent.putExtra(KeyIntentService.EXTRA_DATA, data);

        // Message is received after uploading is done in ApgService
        KeyIntentServiceHandler saveHandler = new KeyIntentServiceHandler(this, R.string.proses_exporting,
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == KeyIntentServiceHandler.MESSAGE_OKAY) {

                    Toast.makeText(KeyServerUploadActivity.this, R.string.key_send_success,
                            Toast.LENGTH_SHORT).show();
                    finish();
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
    }
}

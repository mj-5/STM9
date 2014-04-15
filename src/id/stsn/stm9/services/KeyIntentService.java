package id.stsn.stm9.services;

import java.util.ArrayList;

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;

import id.stsn.stm9.Id;
import id.stsn.stm9.pgp.PgpConvert;
import id.stsn.stm9.pgp.PgpKeyOperation;
import id.stsn.stm9.provider.ProviderHelper;
import id.stsn.stm9.utility.ProgressDialogUpdater;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * Service ini mengandung semua operasi yang penting untuk APG.
 * Service ini menerima Intens data dari activities, antrian intens, mengeksekusi, dan berhenti setelah melakukannya.
 */
public abstract class KeyIntentService extends IntentService implements ProgressDialogUpdater {

    /* pemberian dari intents */
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_DATA = "data";

    /* possible actions */
    public static final String ACTION_SAVE_KEYRING = "id.stsn.stm9" + ".action." + "SAVE_KEYRING";
    public static final String ACTION_GENERATE_DEFAULT_RSA_KEYS = "id.stsn.stm9" + ".action." + "GENERATE_DEFAULT_RSA_KEYS";

    /* data bundle kunci2 */

    /* simpan keyring */ 
    public static final String SAVE_KEYRING_NEW_PASSPHRASE = "new_passphrase";
    public static final String SAVE_KEYRING_CURRENT_PASSPHRASE = "current_passphrase";
    public static final String SAVE_KEYRING_USER_IDS = "user_ids";
    public static final String SAVE_KEYRING_KEYS = "keys";
    public static final String SAVE_KEYRING_KEYS_USAGES = "keys_usages";
    public static final String SAVE_KEYRING_MASTER_KEY_ID = "master_key_id";
    public static final String SAVE_KEYRING_CAN_SIGN = "can_sign";

    /* generate kunci */ 
    public static final String GENERATE_KEY_SYMMETRIC_PASSPHRASE = "passphrase";
    
    /* keys */
    public static final String RESULT_NEW_KEY = "new_key";

    Messenger vMessenger;

    public KeyIntentService() {
        super("ApgService");
    }

    @Override
    protected void onHandleIntent(Intent i) {
        Bundle extras = i.getExtras();
        if (extras == null) {
            Log.e("stm-9", "Extras bundle is null!");
            return;
        }

        if (!(extras.containsKey(EXTRA_MESSENGER) || extras.containsKey(EXTRA_DATA) || (i
                .getAction() == null))) {
            Log.e("stm-9",
                    "Extra bundle must contain a messenger, a data bundle, and an action!");
            return;
        }

        vMessenger = (Messenger) extras.get(EXTRA_MESSENGER);
        Bundle data = extras.getBundle(EXTRA_DATA);

        OtherHelper.logDebugBundle(data, "EXTRA_DATA");

        String action = i.getAction();

        /* execute action dari extra bundle */
        if (ACTION_GENERATE_DEFAULT_RSA_KEYS.equals(action)) {
            /* generate RSA 1024 key untuk signing untuk encrypting */
            try {
                /* Input */
                String passphrase = data.getString(GENERATE_KEY_SYMMETRIC_PASSPHRASE);

                /* Operation */
                PgpKeyOperation keyOperations = new PgpKeyOperation(this, this);

                PGPSecretKeyRing masterKeyRing = keyOperations.createKey(Id.pilihan.algoritma.rsa, 1024, passphrase, null);

                /* Output */
                Bundle resultData = new Bundle();
                resultData.putByteArray(RESULT_NEW_KEY, PgpConvert.PGPSecretKeyRingToBytes(masterKeyRing));

                OtherHelper.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(KeyIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        } else if (ACTION_SAVE_KEYRING.equals(action)) {
            try {
                /* Input */
                String oldPassPhrase = data.getString(SAVE_KEYRING_CURRENT_PASSPHRASE);
                String newPassPhrase = data.getString(SAVE_KEYRING_NEW_PASSPHRASE);
                boolean canSign = true;

                if (data.containsKey(SAVE_KEYRING_CAN_SIGN)) {
                    canSign = data.getBoolean(SAVE_KEYRING_CAN_SIGN);
                }

                if (newPassPhrase == null) {
                    newPassPhrase = oldPassPhrase;
                }
                ArrayList<String> userIds = data.getStringArrayList(SAVE_KEYRING_USER_IDS);
                ArrayList<PGPSecretKey> keys = PgpConvert.BytesToPGPSecretKeyList(data.getByteArray(SAVE_KEYRING_KEYS));
                ArrayList<Integer> keysUsages = data.getIntegerArrayList(SAVE_KEYRING_KEYS_USAGES);
                long masterKeyId = data.getLong(SAVE_KEYRING_MASTER_KEY_ID);

                PgpKeyOperation keyOperations = new PgpKeyOperation(this, this);
                /* Operation */
                if (!canSign) {
                    keyOperations.changeSecretKeyPassphrase(ProviderHelper.getPGPSecretKeyRingByKeyId(this, masterKeyId), oldPassPhrase, newPassPhrase);
                } else {
                    keyOperations.buildSecretKey(userIds, keys, keysUsages, masterKeyId, oldPassPhrase, newPassPhrase);
                }
                PassphraseCacheService.addCachedPassphrase(this, masterKeyId, newPassPhrase);

                /* Output */
                sendMessageToHandler(KeyIntentServiceHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }            
        } 
    }

    private void sendErrorToHandler(Exception e) {
        Log.e("stm-9", "ApgService Exception: ", e);
        e.printStackTrace();

        Bundle data = new Bundle();
        data.putString(KeyIntentServiceHandler.DATA_ERROR, e.getMessage());
        sendMessageToHandler(KeyIntentServiceHandler.MESSAGE_EXCEPTION, null, data);
    }

    private void sendMessageToHandler(Integer arg1, Integer arg2, Bundle data) {
        Message msg = Message.obtain();
        msg.arg1 = arg1;
        if (arg2 != null) {
            msg.arg2 = arg2;
        }
        if (data != null) {
            msg.setData(data);
        }

        try {
            vMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w("stm-9", "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w("stm-9", "Messenger is null!", e);
        }
    }

    private void sendMessageToHandler(Integer arg1, Bundle data) {
        sendMessageToHandler(arg1, null, data);
    }

    private void sendMessageToHandler(Integer arg1) {
        sendMessageToHandler(arg1, null, null);
    }
}

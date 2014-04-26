package id.stsn.stm9.services;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;
//import id.stsn.stm9.fragment.ImportKeysListFragment.InputData;
import id.stsn.stm9.pgp.PgpConvert; //v
import id.stsn.stm9.pgp.PgpGeneralException; //v
import id.stsn.stm9.pgp.PgpImportExport; //v
import id.stsn.stm9.pgp.PgpKeyOperation; //v
import id.stsn.stm9.provider.ProviderHelper;
import id.stsn.stm9.utility.HkpKeyServer;
import id.stsn.stm9.utility.KeyServer.KeyInfo;
import id.stsn.stm9.utility.ProgressDialogUpdater; //v
import id.stsn.stm9.utility.InputData;

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
	public static final String ACTION_EXPORT_KEYRING = "id.stsn.stm9" + ".action." + "EXPORT_KEYRING";
	public static final String ACTION_GENERATE_KEY = "id.stsn.stm9" + ".action." + "GENERATE_KEY";
	public static final String ACTION_QUERY_KEYRING = "id.stsn.stm9" + ".action." + "QUERY_KEYRING";
	public static final String ACTION_IMPORT_KEYRING = "id.stsn.stm9" + ".action." + "IMPORT_KEYRING";

	/* data bundle kunci2 */

	/* encrypt, decrypt, import export */
	public static final String TARGET = "target";
	  
	/* target */ 
	public static final int TARGET_BYTES = 1;
	public static final int TARGET_STREAM = 3;

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
	public static final String GENERATE_KEY_ALGORITHM = "algorithm";
	public static final String GENERATE_KEY_KEY_SIZE = "key_size";
	public static final String GENERATE_KEY_MASTER_KEY = "master_key";

	/* export kunci */
	public static final String EXPORT_FILENAME = "export_filename";
	public static final String EXPORT_KEY_TYPE = "export_key_type";
	public static final String EXPORT_ALL = "export_all";
	public static final String EXPORT_KEY_RING_MASTER_KEY_ID = "export_key_ring_id";
	
	/* import key */
	public static final String IMPORT_KEY_LIST = "import_key_list";
	public static final String IMPORT_BYTES = "import_bytes";
	public static final String RESULT_IMPORT_ADDED = "added";
	public static final String RESULT_IMPORT_UPDATED = "updated";
	public static final String RESULT_IMPORT_BAD = "bad";

	/* keys */
	public static final String RESULT_NEW_KEY = "new_key";
	
	/* query key */
	public static final String QUERY_KEY_SERVER = "query_key_server";
	public static final String QUERY_KEY_TYPE = "query_key_type";
	public static final String QUERY_KEY_STRING = "query_key_string";
	public static final String QUERY_KEY_ID = "query_key_id";
	public static final String RESULT_QUERY_KEY_DATA = "query_key_data";
	public static final String RESULT_QUERY_KEY_SEARCH_RESULT = "query_key_search_result";
	/**
	 * result
	 */

	/* export */
	public static final String RESULT_EXPORT = "exported";

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
		} else if (ACTION_EXPORT_KEYRING.equals(action)) {
			try {

				/* Input */
				int keyType = Id.tipe.public_key;
				if (data.containsKey(EXPORT_KEY_TYPE)) {
					keyType = data.getInt(EXPORT_KEY_TYPE);
				}

				String outputFile = data.getString(EXPORT_FILENAME);

				boolean exportAll = data.getBoolean(EXPORT_ALL);
				long keyRingMasterKeyId = -1;
				if (!exportAll) {
					keyRingMasterKeyId = data.getLong(EXPORT_KEY_RING_MASTER_KEY_ID);
				}

				/* Operation */

				// check if storage is ready
				if (!FileHelper.isStorageMounted(outputFile)) {
					throw new PgpGeneralException(getString(R.string.error_external_storage_not_ready));
				}

				// OutputStream
				FileOutputStream outStream = new FileOutputStream(outputFile);

				ArrayList<Long> keyRingMasterKeyIds = new ArrayList<Long>();
				if (exportAll) {
					// get all key ring row ids based on export type

					if (keyType == Id.tipe.public_key) {
						keyRingMasterKeyIds = ProviderHelper.getPublicKeyRingsMasterKeyIds(this);
					} else {
						keyRingMasterKeyIds = ProviderHelper.getSecretKeyRingsMasterKeyIds(this);
					}
				} else {
					keyRingMasterKeyIds.add(keyRingMasterKeyId);
				}

				Bundle resultData = new Bundle();

				PgpImportExport pgpImportExport = new PgpImportExport(this, this);
				resultData = pgpImportExport.exportKeyRings(keyRingMasterKeyIds, keyType, outStream);

				sendMessageToHandler(KeyIntentServiceHandler.MESSAGE_OKAY, resultData);
			} catch (Exception e) {
				sendErrorToHandler(e);
			}
		} else if (ACTION_GENERATE_KEY.equals(action)) {
			try {
				/* Input */
				int algorithm = data.getInt(GENERATE_KEY_ALGORITHM);
				String passphrase = data.getString(GENERATE_KEY_SYMMETRIC_PASSPHRASE);
				int keysize = data.getInt(GENERATE_KEY_KEY_SIZE);
				PGPSecretKey masterKey = null;
				if (data.containsKey(GENERATE_KEY_MASTER_KEY)) {
					masterKey = PgpConvert.BytesToPGPSecretKey(data.getByteArray(GENERATE_KEY_MASTER_KEY));
				}

				/* Operation */
				PgpKeyOperation keyOperations = new PgpKeyOperation(this, this);
				PGPSecretKeyRing newKeyRing = keyOperations.createKey(algorithm, keysize,
						passphrase, masterKey);

				/* Output */
				Bundle resultData = new Bundle();
				resultData.putByteArray(RESULT_NEW_KEY,
						PgpConvert.PGPSecretKeyRingToBytes(newKeyRing));

				OtherHelper.logDebugBundle(resultData, "resultData");

				sendMessageToHandler(KeyIntentServiceHandler.MESSAGE_OKAY, resultData);
			} catch (Exception e) {
				sendErrorToHandler(e);
			}
		} else if (ACTION_QUERY_KEYRING.equals(action)) {
			try {

				/* Input */
				int queryType = data.getInt(QUERY_KEY_TYPE);
				String keyServer = data.getString(QUERY_KEY_SERVER);

				String queryString = data.getString(QUERY_KEY_STRING);
				long keyId = data.getLong(QUERY_KEY_ID);

				/* Operation */
				Bundle resultData = new Bundle();

				HkpKeyServer server = new HkpKeyServer(keyServer);
				if (queryType == Id.keyserver.search) {	
					ArrayList<KeyInfo> searchResult = server.search(queryString);

					resultData.putParcelableArrayList(RESULT_QUERY_KEY_SEARCH_RESULT, searchResult);
				} else if (queryType == Id.keyserver.get) {
					String keyData = server.get(keyId);

					resultData.putString(RESULT_QUERY_KEY_DATA, keyData);
				}

				sendMessageToHandler(KeyIntentServiceHandler.MESSAGE_OKAY, resultData);
			} catch (Exception e) {
				sendErrorToHandler(e);
			}
		} else if (ACTION_IMPORT_KEYRING.equals(action)) {
			try {

				/* Input */
				int target = data.getInt(TARGET);

				/* Operation */
				InputStream inStream = null;
				long inLength = -1;
				InputData inputData = null;
				switch (target) {
				case TARGET_BYTES: /* import key from bytes directly */
					byte[] bytes = data.getByteArray(IMPORT_BYTES);

					inStream = new ByteArrayInputStream(bytes);
					inLength = bytes.length;

					inputData = new InputData(inStream, inLength);

					break;
					
				case TARGET_STREAM:
					// TODO: not implemented
					break;

				}

				Bundle resultData = new Bundle();

				ArrayList<Long> keyIds = (ArrayList<Long>) data.getSerializable(IMPORT_KEY_LIST);

				PgpImportExport pgpImportExport = new PgpImportExport(this, this);
				resultData = pgpImportExport.importKeyRings(inputData, keyIds);

				sendMessageToHandler(KeyIntentServiceHandler.MESSAGE_OKAY, resultData);
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

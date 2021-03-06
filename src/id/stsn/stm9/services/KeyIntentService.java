package id.stsn.stm9.services;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;
import id.stsn.stm9.pgp.PgpConvert;
import id.stsn.stm9.pgp.PgpGeneralException;
import id.stsn.stm9.pgp.PgpImportExport;
import id.stsn.stm9.pgp.PgpKeyOperation;
import id.stsn.stm9.pgp.PgpOperation;
import id.stsn.stm9.provider.ProviderHelper;
import id.stsn.stm9.utility.HkpKeyServer;
import id.stsn.stm9.utility.InputData;
import id.stsn.stm9.utility.KeyServer.KeyInfo;
import id.stsn.stm9.utility.ProgressDialogUpdater;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;

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
public class KeyIntentService extends IntentService implements ProgressDialogUpdater {

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
	public static final String ACTION_UPLOAD_KEYRING = "id.stsn.stm9" + ".action." + "UPLOAD_KEYRING";
	public static final String ACTION_ENCRYPT_SIGN = "id.stsn.stm9.stm_9" + ".action." + "ENCRYPT_SIGN";
	public static final String ACTION_DECRYPT_VERIFY = "id.stsn.stm9" + ".action." + "DECRYPT_VERIFY";

	/* data bundle kunci2 */

	/* encrypt, decrypt, import export */
	public static final String TARGET = "target";
	  
	/* target */ 
	public static final int TARGET_BYTES = 1;
    public static final int TARGET_FILE = 2;
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
    public static final String IMPORT_FILENAME = "import_filename";
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
	
	/* upload key */
	public static final String UPLOAD_KEY_SERVER = "upload_key_server";
	public static final String UPLOAD_KEY_KEYRING_ROW_ID = "upload_key_ring_id";

	/* encrypt dan sign */ 
	public static final String ENCRYPT_SECRET_KEY_ID = "secret_key_id";
	public static final String ENCRYPT_USE_ASCII_ARMOR = "use_ascii_armor";
	public static final String ENCRYPT_ENCRYPTION_KEYS_IDS = "encryption_keys_ids";
	public static final String ENCRYPT_COMPRESSION_ID = "compression_id";
	public static final String ENCRYPT_GENERATE_SIGNATURE = "generate_signature";
	public static final String ENCRYPT_SIGN_ONLY = "sign_only";
	public static final String ENCRYPT_MESSAGE_BYTES = "message_bytes";
    public static final String ENCRYPT_PROVIDER_URI = "provider_uri";

	  /* encrypt sign result */
	  public static final String RESULT_SIGNATURE_BYTES = "signature_data";
	  public static final String RESULT_SIGNATURE_STRING = "signature_text";
	  public static final String RESULT_ENCRYPTED_STRING = "encrypted_message";
	  public static final String RESULT_ENCRYPTED_BYTES = "encrypted_data";

	  /* decrypt dan verify */ 
	  public static final String DECRYPT_SIGNED_ONLY = "signed_only";
	  public static final String DECRYPT_RETURN_BYTES = "return_binary";
	  public static final String DECRYPT_CIPHERTEXT_BYTES = "ciphertext_bytes";
	  public static final String RESULT_DECRYPTED_STRING = "decrypted_message";
	  public static final String RESULT_DECRYPTED_BYTES = "decrypted_data";
	  public static final String RESULT_SIGNATURE = "signature";
	  public static final String RESULT_SIGNATURE_KEY_ID = "signature_key_id";
	  public static final String RESULT_SIGNATURE_USER_ID = "signature_user_id";
	  public static final String RESULT_SIGNATURE_SUCCESS = "signature_success";
	  public static final String RESULT_SIGNATURE_UNKNOWN = "signature_unknown";
	  public static final String DECRYPT_ASSUME_SYMMETRIC = "assume_symmetric";
	  public static final String DECRYPT_LOOKUP_UNKNOWN_KEY = "lookup_unknownKey";
	  public static final String RESULT_SIGNATURE_LOOKUP_KEY = "lookup_key";

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
		} else if (ACTION_UPLOAD_KEYRING.equals(action)) {
            try {

                /* Input */
                int keyRingRowId = data.getInt(UPLOAD_KEY_KEYRING_ROW_ID);
                String keyServer = data.getString(UPLOAD_KEY_SERVER);

                /* Operation */
                HkpKeyServer server = new HkpKeyServer(keyServer);

                PGPPublicKeyRing keyring = ProviderHelper.getPGPPublicKeyRingByRowId(this, keyRingRowId);
                if (keyring != null) {
                    PgpImportExport pgpImportExport = new PgpImportExport(this, null);

                    boolean uploaded = pgpImportExport.uploadKeyRingToServer(server, (PGPPublicKeyRing) keyring);
                    if (!uploaded) {
                        throw new PgpGeneralException("Unable to export key to selected server");
                    }
                }

                sendMessageToHandler(KeyIntentServiceHandler.MESSAGE_OKAY);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
		} else if (ACTION_ENCRYPT_SIGN.equals(action)) {
            try {
                /* Input */
                int target = data.getInt(TARGET);

                long secretKeyId = data.getLong(ENCRYPT_SECRET_KEY_ID);
                String encryptionPassphrase = data.getString(GENERATE_KEY_SYMMETRIC_PASSPHRASE);

                boolean useAsciiArmor = data.getBoolean(ENCRYPT_USE_ASCII_ARMOR);
                long encryptionKeyIds[] = data.getLongArray(ENCRYPT_ENCRYPTION_KEYS_IDS);
                int compressionId = data.getInt(ENCRYPT_COMPRESSION_ID);
                boolean generateSignature = data.getBoolean(ENCRYPT_GENERATE_SIGNATURE);
                boolean signOnly = data.getBoolean(ENCRYPT_SIGN_ONLY);

                InputStream inStream = null;
                long inLength = -1;
                InputData inputData = null;
                OutputStream outStream = null;
                switch (target) {
                case TARGET_BYTES: /* encrypting bytes directly */
                    byte[] bytes = data.getByteArray(ENCRYPT_MESSAGE_BYTES);

                    inStream = new ByteArrayInputStream(bytes);
                    inLength = bytes.length;

                    inputData = new InputData(inStream, inLength);
                    outStream = new ByteArrayOutputStream();

                    break;

                default:
                    throw new PgpGeneralException("No target choosen!");

                }

                /* Operation */
                PgpOperation operation = new PgpOperation(this, this, inputData, outStream);
                if (generateSignature) {
                    Log.d("stm-9", "generating signature...");
                    operation.generateSignature(useAsciiArmor, false, secretKeyId, PassphraseCacheService.getCachedPassphrase(this, secretKeyId), Preferences.getPreferences(this).getDefaultHashAlgorithm(), Preferences.getPreferences(this).getForceV3Signatures());
                } else if (signOnly) {
                    Log.d("stm-9", "sign only...");
                    operation.signText(secretKeyId, PassphraseCacheService.getCachedPassphrase(this, secretKeyId), Preferences.getPreferences(this).getDefaultHashAlgorithm(), Preferences.getPreferences(this).getForceV3Signatures());
                } else {
                    Log.d("stm-9", "encrypt...");
                    operation.signAndEncrypt(useAsciiArmor, compressionId, encryptionKeyIds, encryptionPassphrase, Preferences.getPreferences(this).getDefaultEncryptionAlgorithm(), secretKeyId, Preferences.getPreferences(this).getDefaultHashAlgorithm(), Preferences.getPreferences(this).getForceV3Signatures(), PassphraseCacheService.getCachedPassphrase(this, secretKeyId));
                }

                outStream.close();

                /* Output */

                Bundle resultData = new Bundle();

                switch (target) {
                case TARGET_BYTES:
                    if (useAsciiArmor) {
                        String output = new String(
                                ((ByteArrayOutputStream) outStream).toByteArray());
                        if (generateSignature) {
                            resultData.putString(RESULT_SIGNATURE_STRING, output);
                        } else {
                            resultData.putString(RESULT_ENCRYPTED_STRING, output);
                        }
                    } else {
                        byte output[] = ((ByteArrayOutputStream) outStream).toByteArray();
                        if (generateSignature) {
                            resultData.putByteArray(RESULT_SIGNATURE_BYTES, output);
                        } else {
                            resultData.putByteArray(RESULT_ENCRYPTED_BYTES, output);
                        }
                    }

                    break;
                }

                OtherHelper.logDebugBundle(resultData, "resultData");

                sendMessageToHandler(KeyIntentServiceHandler.MESSAGE_OKAY, resultData);
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
		} else if (ACTION_DECRYPT_VERIFY.equals(action)) {
          try {
          /* Input */
          int target = data.getInt(TARGET);

          long secretKeyId = data.getLong(ENCRYPT_SECRET_KEY_ID);
          byte[] bytes = data.getByteArray(DECRYPT_CIPHERTEXT_BYTES);
          boolean signedOnly = data.getBoolean(DECRYPT_SIGNED_ONLY);
          boolean returnBytes = data.getBoolean(DECRYPT_RETURN_BYTES);

          InputStream inStream = null;
          long inLength = -1;
          InputData inputData = null;
          OutputStream outStream = null;
          switch (target) {
          case TARGET_BYTES: /* decrypting bytes directly */
              inStream = new ByteArrayInputStream(bytes);
              inLength = bytes.length;

              inputData = new InputData(inStream, inLength);
              outStream = new ByteArrayOutputStream();

              break;

          default:
              throw new PgpGeneralException("No target choosen!");

          }

          /* Operation */

          Bundle resultData = new Bundle();

          // verifyText and decrypt returning additional resultData values for the verification of signatures
          PgpOperation operation = new PgpOperation(this, this, inputData, outStream);
          if (signedOnly) {
              resultData = operation.verifyText();
          } else {
              resultData = operation.decryptAndVerify(PassphraseCacheService.getCachedPassphrase(this, secretKeyId));
          }

          outStream.close();

          /* Output */

          switch (target) {
          case TARGET_BYTES:
              if (returnBytes) {
                  byte output[] = ((ByteArrayOutputStream) outStream).toByteArray();
                  resultData.putByteArray(RESULT_DECRYPTED_BYTES, output);
              } else {
                  String output = new String(
                          ((ByteArrayOutputStream) outStream).toByteArray());
                  resultData.putString(RESULT_DECRYPTED_STRING, output);
              }

              break;
          }

          OtherHelper.logDebugBundle(resultData, "resultData");

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
	
    /**
     * Set progress of ProgressDialog by sending message to handler on UI thread
     */
    public void setProgress(String message, int progress, int max) {
        Log.d("stm-9", "Send message by setProgress with progress=" + progress + ", max="
                + max);

        Bundle data = new Bundle();
        if (message != null) {
            data.putString(KeyIntentServiceHandler.DATA_MESSAGE, message);
        }
        data.putInt(KeyIntentServiceHandler.DATA_PROGRESS, progress);
        data.putInt(KeyIntentServiceHandler.DATA_PROGRESS_MAX, max);

        sendMessageToHandler(KeyIntentServiceHandler.MESSAGE_UPDATE_PROGRESS, null, data);
    }

    public void setProgress(int resourceId, int progress, int max) {
        setProgress(getString(resourceId), progress, max);
    }

    public void setProgress(int progress, int max) {
        setProgress(null, progress, max);
    }
}

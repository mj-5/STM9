package id.stsn.stm9.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;

import id.stsn.stm9.pgp.PgpConvert;
import id.stsn.stm9.pgp.PgpKeyHelper;
import id.stsn.stm9.provider.KeyContract.KeyRings;
import id.stsn.stm9.provider.KeyContract.Keys;
import id.stsn.stm9.provider.KeyContract.UserIds;
import id.stsn.stm9.utility.IterableIterator;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

public class ProviderHelper {

	/**
	 * Private helper method to get PGPKeyRing from database
	 * 
	 * @param context
	 * @param queryUri
	 * @return
	 */
	private static PGPKeyRing getPGPKeyRing(Context context, Uri queryUri) {
		Cursor cursor = context.getContentResolver().query(queryUri,
				new String[] { KeyRings._ID, KeyRings.KEY_RING_DATA }, null, null, null);

		PGPKeyRing keyRing = null;
		if (cursor != null && cursor.moveToFirst()) {
			int keyRingDataCol = cursor.getColumnIndex(KeyRings.KEY_RING_DATA);

			byte[] data = cursor.getBlob(keyRingDataCol);
			if (data != null) {
				keyRing = PgpConvert.BytesToPGPKeyRing(data);
			}
		}

		if (cursor != null) {
			cursor.close();
		}

		return keyRing;
	}

	/**
	 * Retrieves the actual PGPSecretKeyRing object from the database blob associated with a key
	 * with this keyId
	 * 
	 * @param context
	 * @param keyId
	 * @return
	 */
	public static PGPSecretKeyRing getPGPSecretKeyRingByKeyId(Context context, long keyId) {
		Uri queryUri = KeyRings.buildSecretKeyRingsByKeyIdUri(Long.toString(keyId));
		return (PGPSecretKeyRing) getPGPKeyRing(context, queryUri);
	}

	/**
	 * Private helper method
	 * 
	 * @param context
	 * @param queryUri
	 * @return
	 */
	private static ArrayList<Long> getKeyRingsMasterKeyIds(Context context, Uri queryUri) {
		Cursor cursor = context.getContentResolver().query(queryUri,
				new String[] { KeyRings.MASTER_KEY_ID }, null, null, null);

		ArrayList<Long> masterKeyIds = new ArrayList<Long>();
		if (cursor != null) {
			int masterKeyIdCol = cursor.getColumnIndex(KeyRings.MASTER_KEY_ID);
			if (cursor.moveToFirst()) {
				do {
					masterKeyIds.add(cursor.getLong(masterKeyIdCol));
				} while (cursor.moveToNext());
			}
		}

		if (cursor != null) {
			cursor.close();
		}

		return masterKeyIds;
	}

	/**
	 * Retrieves ids of all SecretKeyRings
	 * 
	 * @param context
	 * @return
	 */
	public static ArrayList<Long> getSecretKeyRingsMasterKeyIds(Context context) {
		Uri queryUri = KeyRings.buildSecretKeyRingsUri();
		return getKeyRingsMasterKeyIds(context, queryUri);
	}

	/**
	 * Retrieves ids of all PublicKeyRings
	 * 
	 * @param context
	 * @return
	 */
	public static ArrayList<Long> getPublicKeyRingsMasterKeyIds(Context context) {
		Uri queryUri = KeyRings.buildPublicKeyRingsUri();
		return getKeyRingsMasterKeyIds(context, queryUri);
	}

	/**
	 * Saves PGPPublicKeyRing with its keys and userIds in DB
	 * 
	 * @param context
	 * @param keyRing
	 * @return
	 * @throws IOException
	 * @throws GeneralException
	 */
	@SuppressWarnings("unchecked")
	public static void saveKeyRing(Context context, PGPPublicKeyRing keyRing) throws IOException {
		PGPPublicKey masterKey = keyRing.getPublicKey();
		long masterKeyId = masterKey.getKeyID();

		// delete old version of this keyRing, which also deletes all keys and userIds on cascade
		Uri deleteUri = KeyRings.buildPublicKeyRingsByMasterKeyIdUri(Long.toString(masterKeyId));

		try {
			context.getContentResolver().delete(deleteUri, null, null);
		} catch (UnsupportedOperationException e) {
			Log.e("Stm-9", "Key could not be deleted! Maybe we are creating a new one!", e);
		}

		ContentValues values = new ContentValues();
		values.put(KeyRings.MASTER_KEY_ID, masterKeyId);
		values.put(KeyRings.KEY_RING_DATA, keyRing.getEncoded());

		// insert new version of this keyRing
		Uri uri = KeyRings.buildPublicKeyRingsUri();
		Uri insertedUri = context.getContentResolver().insert(uri, values);
		long keyRingRowId = Long.valueOf(insertedUri.getLastPathSegment());

		// save all keys and userIds included in keyRing object in database
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

		int rank = 0;
		for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
			operations.add(buildPublicKeyOperations(context, keyRingRowId, key, rank));
			++rank;
		}

		int userIdRank = 0;
		for (String userId : new IterableIterator<String>(masterKey.getUserIDs())) {
			operations.add(buildPublicUserIdOperations(context, keyRingRowId, userId, userIdRank));
			++userIdRank;
		}

		try {
			context.getContentResolver().applyBatch(KeyContract.CONTENT_AUTHORITY, operations);
		} catch (RemoteException e) {
			Log.e("Stm-9", "applyBatch failed!", e);
		} catch (OperationApplicationException e) {
			Log.e("Stm-9", "applyBatch failed!", e);
		}
	}

	/**
	 * Saves PGPSecretKeyRing with its keys and userIds in DB
	 * 
	 * @param context
	 * @param keyRing
	 * @return
	 * @throws IOException
	 * @throws GeneralException
	 */
	@SuppressWarnings("unchecked")
	public static void saveKeyRing(Context context, PGPSecretKeyRing keyRing) throws IOException {
		PGPSecretKey masterKey = keyRing.getSecretKey();
		long masterKeyId = masterKey.getKeyID();

		// delete old version of this keyRing, which also deletes all keys and userIds on cascade
		Uri deleteUri = KeyRings.buildSecretKeyRingsByMasterKeyIdUri(Long.toString(masterKeyId));

		try {
			context.getContentResolver().delete(deleteUri, null, null);
		} catch (UnsupportedOperationException e) {
			Log.e("Stm-9", "Key could not be deleted! Maybe we are creating a new one!", e);
		}

		ContentValues values = new ContentValues();
		values.put(KeyRings.MASTER_KEY_ID, masterKeyId);
		values.put(KeyRings.KEY_RING_DATA, keyRing.getEncoded());

		// insert new version of this keyRing
		Uri uri = KeyRings.buildSecretKeyRingsUri();
		Uri insertedUri = context.getContentResolver().insert(uri, values);
		long keyRingRowId = Long.valueOf(insertedUri.getLastPathSegment());

		// save all keys and userIds included in keyRing object in database
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

		int rank = 0;
		for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
			operations.add(buildSecretKeyOperations(context, keyRingRowId, key, rank));
			++rank;
		}

		int userIdRank = 0;
		for (String userId : new IterableIterator<String>(masterKey.getUserIDs())) {
			operations.add(buildSecretUserIdOperations(context, keyRingRowId, userId, userIdRank));
			++userIdRank;
		}

		try {
			context.getContentResolver().applyBatch(KeyContract.CONTENT_AUTHORITY, operations);
		} catch (RemoteException e) {
			Log.e("Stm-9", "applyBatch failed!", e);
		} catch (OperationApplicationException e) {
			Log.e("Stm-9", "applyBatch failed!", e);
		}
	}

	/**
	 * Build ContentProviderOperation to add PGPPublicKey to database corresponding to a keyRing
	 * 
	 * @param context
	 * @param keyRingRowId
	 * @param key
	 * @param rank
	 * @return
	 * @throws IOException
	 */
	private static ContentProviderOperation buildPublicKeyOperations(Context context,
			long keyRingRowId, PGPPublicKey key, int rank) throws IOException {
		ContentValues values = new ContentValues();
		values.put(Keys.KEY_ID, key.getKeyID());
		values.put(Keys.IS_MASTER_KEY, key.isMasterKey());
		values.put(Keys.ALGORITHM, key.getAlgorithm());
		values.put(Keys.KEY_SIZE, key.getBitStrength());
		values.put(Keys.CAN_SIGN, PgpKeyHelper.isSigningKey(key));
		values.put(Keys.CAN_ENCRYPT, PgpKeyHelper.isEncryptionKey(key));
		values.put(Keys.IS_REVOKED, key.isRevoked());
		values.put(Keys.CREATION, PgpKeyHelper.getCreationDate(key).getTime() / 1000);
		Date expiryDate = PgpKeyHelper.getExpiryDate(key);
		if (expiryDate != null) {
			values.put(Keys.EXPIRY, expiryDate.getTime() / 1000);
		}
		values.put(Keys.KEY_RING_ROW_ID, keyRingRowId);
		values.put(Keys.KEY_DATA, key.getEncoded());
		values.put(Keys.RANK, rank);

		Uri uri = Keys.buildPublicKeysUri(Long.toString(keyRingRowId));

		return ContentProviderOperation.newInsert(uri).withValues(values).build();
	}


	/**
	 * Build ContentProviderOperation to add PublicUserIds to database corresponding to a keyRing
	 * 
	 * @param context
	 * @param keyRingRowId
	 * @param key
	 * @param rank
	 * @return
	 * @throws IOException
	 */
	private static ContentProviderOperation buildPublicUserIdOperations(Context context,
			long keyRingRowId, String userId, int rank) {
		ContentValues values = new ContentValues();
		values.put(UserIds.KEY_RING_ROW_ID, keyRingRowId);
		values.put(UserIds.USER_ID, userId);
		values.put(UserIds.RANK, rank);

		Uri uri = UserIds.buildPublicUserIdsUri(Long.toString(keyRingRowId));

		return ContentProviderOperation.newInsert(uri).withValues(values).build();
	}

	/**
	 * Build ContentProviderOperation to add PGPSecretKey to database corresponding to a keyRing
	 * 
	 * @param context
	 * @param keyRingRowId
	 * @param key
	 * @param rank
	 * @return
	 * @throws IOException
	 */
	private static ContentProviderOperation buildSecretKeyOperations(Context context,
			long keyRingRowId, PGPSecretKey key, int rank) throws IOException {
		ContentValues values = new ContentValues();

		boolean has_private = true;
		if (key.isMasterKey()) {
			if (PgpKeyHelper.isSecretKeyPrivateEmpty(key)) {
				has_private = false;
			}
		}
		values.put(Keys.KEY_ID, key.getKeyID());
        values.put(Keys.IS_MASTER_KEY, key.isMasterKey());
        values.put(Keys.ALGORITHM, key.getPublicKey().getAlgorithm());
        values.put(Keys.KEY_SIZE, key.getPublicKey().getBitStrength());
        values.put(Keys.CAN_CERTIFY, (PgpKeyHelper.isCertificationKey(key) && has_private));
        values.put(Keys.CAN_SIGN, (PgpKeyHelper.isSigningKey(key) && has_private));
        values.put(Keys.CAN_ENCRYPT, PgpKeyHelper.isEncryptionKey(key));
        values.put(Keys.IS_REVOKED, key.getPublicKey().isRevoked());
        values.put(Keys.CREATION, PgpKeyHelper.getCreationDate(key).getTime() / 1000);
        Date expiryDate = PgpKeyHelper.getExpiryDate(key);
        if (expiryDate != null) {
            values.put(Keys.EXPIRY, expiryDate.getTime() / 1000);
        }
        values.put(Keys.KEY_RING_ROW_ID, keyRingRowId);
        values.put(Keys.KEY_DATA, key.getEncoded());
        values.put(Keys.RANK, rank);

        Uri uri = Keys.buildSecretKeysUri(Long.toString(keyRingRowId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }


		/**
		 * Build ContentProviderOperation to add SecretUserIds to database corresponding to a keyRing
		 * 
		 * @param context
		 * @param keyRingRowId
		 * @param key
		 * @param rank
		 * @return
		 * @throws IOException
		 */
		private static ContentProviderOperation buildSecretUserIdOperations(Context context,
				long keyRingRowId, String userId, int rank) {
			ContentValues values = new ContentValues();
			values.put(UserIds.KEY_RING_ROW_ID, keyRingRowId);
			values.put(UserIds.USER_ID, userId);
			values.put(UserIds.RANK, rank);

			Uri uri = UserIds.buildSecretUserIdsUri(Long.toString(keyRingRowId));

			return ContentProviderOperation.newInsert(uri).withValues(values).build();
		}
	}
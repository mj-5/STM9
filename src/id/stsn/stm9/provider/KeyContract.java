package id.stsn.stm9.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class KeyContract {

    interface KeyRingsColumns {
        String MASTER_KEY_ID = "master_key_id"; // not a database id
        String TYPE = "type"; // see KeyTypes
        String KEY_RING_DATA = "key_ring_data"; // PGPPublicKeyRing / PGPSecretKeyRing blob
    }

    interface KeysColumns {
        String KEY_ID = "key_id"; // not a database id
        String TYPE = "type"; // see KeyTypes
        String IS_MASTER_KEY = "is_master_key";
        String ALGORITHM = "algorithm";
        String KEY_SIZE = "key_size";
        String CAN_CERTIFY = "can_certify";
        String CAN_SIGN = "can_sign";
        String CAN_ENCRYPT = "can_encrypt";
        String IS_REVOKED = "is_revoked";
        String CREATION = "creation";
        String EXPIRY = "expiry";
        String KEY_RING_ROW_ID = "key_ring_row_id"; // foreign key to key_rings._ID
        String KEY_DATA = "key_data"; // PGPPublicKey/PGPSecretKey blob
        String RANK = "rank";
    }

    interface UserIdsColumns {
        String KEY_RING_ROW_ID = "key_ring_row_id"; // foreign key to key_rings._ID
        String USER_ID = "user_id"; // not a database id
        String RANK = "rank";
    }

    public static final class KeyTypes {
        public static final int PUBLIC = 0;
        public static final int SECRET = 1;
    }

    public static final String CONTENT_AUTHORITY = "id.stsn.stm9";

    private static final Uri BASE_CONTENT_URI_INTERNAL = Uri.parse("content://"
            + CONTENT_AUTHORITY);

    public static final String BASE_KEY_RINGS = "key_rings";
    public static final String BASE_DATA = "data";

    public static final String PATH_PUBLIC = "public";
    public static final String PATH_SECRET = "secret";

    public static final String PATH_BY_MASTER_KEY_ID = "master_key_id";
    public static final String PATH_BY_KEY_ID = "key_id";
    public static final String PATH_BY_EMAILS = "emails";
    public static final String PATH_BY_LIKE_EMAIL = "like_email";

    public static final String PATH_USER_IDS = "user_ids";
    public static final String PATH_KEYS = "keys";

    public static class KeyRings implements KeyRingsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        /** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.thialfihar.apg.key_ring";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.thialfihar.apg.key_ring";

        public static Uri buildPublicKeyRingsUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_PUBLIC).build();
        }

        public static Uri buildPublicKeyRingsUri(String keyRingRowId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_PUBLIC).appendPath(keyRingRowId).build();
        }

        public static Uri buildPublicKeyRingsByMasterKeyIdUri(String masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_PUBLIC)
                    .appendPath(PATH_BY_MASTER_KEY_ID).appendPath(masterKeyId).build();
        }

        public static Uri buildPublicKeyRingsByKeyIdUri(String keyId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_PUBLIC).appendPath(PATH_BY_KEY_ID)
                    .appendPath(keyId).build();
        }

        public static Uri buildPublicKeyRingsByEmailsUri(String emails) {
            return CONTENT_URI.buildUpon().appendPath(PATH_PUBLIC).appendPath(PATH_BY_EMAILS)
                    .appendPath(emails).build();
        }

        public static Uri buildPublicKeyRingsByLikeEmailUri(String emails) {
            return CONTENT_URI.buildUpon().appendPath(PATH_PUBLIC).appendPath(PATH_BY_LIKE_EMAIL)
                    .appendPath(emails).build();
        }

        public static Uri buildSecretKeyRingsUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_SECRET).build();
        }

        public static Uri buildSecretKeyRingsUri(String keyRingRowId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SECRET).appendPath(keyRingRowId).build();
        }

        public static Uri buildSecretKeyRingsByMasterKeyIdUri(String masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SECRET)
                    .appendPath(PATH_BY_MASTER_KEY_ID).appendPath(masterKeyId).build();
        }

        public static Uri buildSecretKeyRingsByKeyIdUri(String keyId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SECRET).appendPath(PATH_BY_KEY_ID)
                    .appendPath(keyId).build();
        }

        public static Uri buildSecretKeyRingsByEmailsUri(String emails) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SECRET).appendPath(PATH_BY_EMAILS)
                    .appendPath(emails).build();
        }

        public static Uri buildSecretKeyRingsByLikeEmails(String emails) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SECRET).appendPath(PATH_BY_LIKE_EMAIL)
                    .appendPath(emails).build();
        }
    }

    public static class Keys implements KeysColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        /** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.thialfihar.apg.key";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.thialfihar.apg.key";

        public static Uri buildPublicKeysUri(String keyRingRowId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_PUBLIC).appendPath(keyRingRowId)
                    .appendPath(PATH_KEYS).build();
        }

        public static Uri buildPublicKeysUri(String keyRingRowId, String keyRowId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_PUBLIC).appendPath(keyRingRowId)
                    .appendPath(PATH_KEYS).appendPath(keyRowId).build();
        }

        public static Uri buildSecretKeysUri(String keyRingRowId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SECRET).appendPath(keyRingRowId)
                    .appendPath(PATH_KEYS).build();
        }

        public static Uri buildSecretKeysUri(String keyRingRowId, String keyRowId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SECRET).appendPath(keyRingRowId)
                    .appendPath(PATH_KEYS).appendPath(keyRowId).build();
        }
    }

    public static class UserIds implements UserIdsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        /** Use if multiple items get returned */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.thialfihar.apg.user_id";

        /** Use if a single item is returned */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.thialfihar.apg.user_id";

        public static Uri buildPublicUserIdsUri(String keyRingRowId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_PUBLIC).appendPath(keyRingRowId)
                    .appendPath(PATH_USER_IDS).build();
        }

        public static Uri buildPublicUserIdsUri(String keyRingRowId, String userIdRowId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_PUBLIC).appendPath(keyRingRowId)
                    .appendPath(PATH_USER_IDS).appendPath(userIdRowId).build();
        }

        public static Uri buildSecretUserIdsUri(String keyRingRowId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SECRET).appendPath(keyRingRowId)
                    .appendPath(PATH_USER_IDS).build();
        }

        public static Uri buildSecretUserIdsUri(String keyRingRowId, String userIdRowId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SECRET).appendPath(keyRingRowId)
                    .appendPath(PATH_USER_IDS).appendPath(userIdRowId).build();
        }
    }

    private KeyContract() {
    }
}

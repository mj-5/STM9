package id.stsn.stm9.provider;

import java.util.Arrays;
import java.util.HashMap;

import id.stsn.stm9.BuildConfig;
import id.stsn.stm9.provider.KeyContract.KeyRings;
import id.stsn.stm9.provider.KeyContract.KeyRingsColumns;
import id.stsn.stm9.provider.KeyContract.KeyTypes;
import id.stsn.stm9.provider.KeyContract.Keys;
import id.stsn.stm9.provider.KeyContract.KeysColumns;
import id.stsn.stm9.provider.KeyContract.UserIds;
import id.stsn.stm9.provider.KeyContract.UserIdsColumns;
import id.stsn.stm9.provider.KeyDatabase.Tables;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class KeyProvider extends ContentProvider {

    private static final int PUBLIC_KEY_RING = 101;
    private static final int PUBLIC_KEY_RING_BY_ROW_ID = 102;
    private static final int PUBLIC_KEY_RING_BY_MASTER_KEY_ID = 103;
    private static final int PUBLIC_KEY_RING_BY_KEY_ID = 104;
    private static final int PUBLIC_KEY_RING_BY_EMAILS = 105;
    private static final int PUBLIC_KEY_RING_BY_LIKE_EMAIL = 106;

    private static final int PUBLIC_KEY_RING_KEY = 111;
    private static final int PUBLIC_KEY_RING_KEY_BY_ROW_ID = 112;

    private static final int PUBLIC_KEY_RING_USER_ID = 121;
    private static final int PUBLIC_KEY_RING_USER_ID_BY_ROW_ID = 122;

    private static final int SECRET_KEY_RING = 201;
    private static final int SECRET_KEY_RING_BY_ROW_ID = 202;
    private static final int SECRET_KEY_RING_BY_MASTER_KEY_ID = 203;
    private static final int SECRET_KEY_RING_BY_KEY_ID = 204;
    private static final int SECRET_KEY_RING_BY_EMAILS = 205;
    private static final int SECRET_KEY_RING_BY_LIKE_EMAIL = 206;

    private static final int SECRET_KEY_RING_KEY = 211;
    private static final int SECRET_KEY_RING_KEY_BY_ROW_ID = 212;

    private static final int SECRET_KEY_RING_USER_ID = 221;
    private static final int SECRET_KEY_RING_USER_ID_BY_ROW_ID = 222;


    protected UriMatcher mUriMatcher;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri} variations supported by
     * this {@link ContentProvider}.
     */
    protected UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        String authority = KeyContract.CONTENT_AUTHORITY;

        /**
         * public key rings
         * 
         * <pre>
         * key_rings/public
         * key_rings/public/#
         * key_rings/public/master_key_id/_
         * key_rings/public/key_id/_
         * key_rings/public/emails/_
         * key_rings/public/like_email/_
         * </pre>
         */
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_PUBLIC, PUBLIC_KEY_RING);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_PUBLIC + "/#", PUBLIC_KEY_RING_BY_ROW_ID);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_PUBLIC + "/" + KeyContract.PATH_BY_MASTER_KEY_ID
                + "/*", PUBLIC_KEY_RING_BY_MASTER_KEY_ID);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_PUBLIC + "/" + KeyContract.PATH_BY_KEY_ID + "/*",
                PUBLIC_KEY_RING_BY_KEY_ID);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_PUBLIC + "/" + KeyContract.PATH_BY_EMAILS + "/*",
                PUBLIC_KEY_RING_BY_EMAILS);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_PUBLIC + "/" + KeyContract.PATH_BY_EMAILS,
                PUBLIC_KEY_RING_BY_EMAILS); // without emails specified
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_PUBLIC + "/" + KeyContract.PATH_BY_LIKE_EMAIL + "/*",
                PUBLIC_KEY_RING_BY_LIKE_EMAIL);

        /**
         * public keys
         * 
         * <pre>
         * key_rings/public/#/keys
         * key_rings/public/#/keys/#
         * </pre>
         */
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_PUBLIC + "/#/" + KeyContract.PATH_KEYS,
                PUBLIC_KEY_RING_KEY);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_PUBLIC + "/#/" + KeyContract.PATH_KEYS + "/#",
                PUBLIC_KEY_RING_KEY_BY_ROW_ID);

        /**
         * public user ids
         * 
         * <pre>
         * key_rings/public/#/user_ids
         * key_rings/public/#/user_ids/#
         * </pre>
         */
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_PUBLIC + "/#/" + KeyContract.PATH_USER_IDS,
                PUBLIC_KEY_RING_USER_ID);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_PUBLIC + "/#/" + KeyContract.PATH_USER_IDS + "/#",
                PUBLIC_KEY_RING_USER_ID_BY_ROW_ID);

        /**
         * secret key rings
         * 
         * <pre>
         * key_rings/secret
         * key_rings/secret/#
         * key_rings/secret/master_key_id/_
         * key_rings/secret/key_id/_
         * key_rings/secret/emails/_
         * key_rings/secret/like_email/_
         * </pre>
         */
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_SECRET, SECRET_KEY_RING);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_SECRET + "/#", SECRET_KEY_RING_BY_ROW_ID);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_SECRET + "/" + KeyContract.PATH_BY_MASTER_KEY_ID
                + "/*", SECRET_KEY_RING_BY_MASTER_KEY_ID);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_SECRET + "/" + KeyContract.PATH_BY_KEY_ID + "/*",
                SECRET_KEY_RING_BY_KEY_ID);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_SECRET + "/" + KeyContract.PATH_BY_EMAILS + "/*",
                SECRET_KEY_RING_BY_EMAILS);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_SECRET + "/" + KeyContract.PATH_BY_EMAILS,
                SECRET_KEY_RING_BY_EMAILS); // without emails specified
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_SECRET + "/" + KeyContract.PATH_BY_LIKE_EMAIL + "/*",
                SECRET_KEY_RING_BY_LIKE_EMAIL);

        /**
         * secret keys
         * 
         * <pre>
         * key_rings/secret/#/keys
         * key_rings/secret/#/keys/#
         * </pre>
         */
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_SECRET + "/#/" + KeyContract.PATH_KEYS,
                SECRET_KEY_RING_KEY);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_SECRET + "/#/" + KeyContract.PATH_KEYS + "/#",
                SECRET_KEY_RING_KEY_BY_ROW_ID);

        /**
         * secret user ids
         * 
         * <pre>
         * key_rings/secret/#/user_ids
         * key_rings/secret/#/user_ids/#
         * </pre>
         */
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_SECRET + "/#/" + KeyContract.PATH_USER_IDS,
                SECRET_KEY_RING_USER_ID);
        matcher.addURI(authority, KeyContract.BASE_KEY_RINGS + "/"
                + KeyContract.PATH_SECRET + "/#/" + KeyContract.PATH_USER_IDS + "/#",
                SECRET_KEY_RING_USER_ID_BY_ROW_ID);

        /**
         * data stream
         * 
         * <pre>
         * data / _
         * </pre>
         */
        // matcher.addURI(authority, KeychainContract.BASE_DATA + "/*", DATA_STREAM);

        return matcher;
    }

    private KeyDatabase mApgDatabase;

    /** {@inheritDoc} */
    @Override
    public boolean onCreate() {
        mUriMatcher = buildUriMatcher();
        mApgDatabase = new KeyDatabase(getContext());
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        final int match = mUriMatcher.match(uri);
        switch (match) {
        case PUBLIC_KEY_RING:
        case PUBLIC_KEY_RING_BY_EMAILS:
        case PUBLIC_KEY_RING_BY_LIKE_EMAIL:
        case SECRET_KEY_RING:
        case SECRET_KEY_RING_BY_EMAILS:
        case SECRET_KEY_RING_BY_LIKE_EMAIL:
            return KeyRings.CONTENT_TYPE;

        case PUBLIC_KEY_RING_BY_ROW_ID:
        case PUBLIC_KEY_RING_BY_MASTER_KEY_ID:
        case PUBLIC_KEY_RING_BY_KEY_ID:
        case SECRET_KEY_RING_BY_ROW_ID:
        case SECRET_KEY_RING_BY_MASTER_KEY_ID:
        case SECRET_KEY_RING_BY_KEY_ID:
            return KeyRings.CONTENT_ITEM_TYPE;

        case PUBLIC_KEY_RING_KEY:
        case SECRET_KEY_RING_KEY:
            return Keys.CONTENT_TYPE;

        case PUBLIC_KEY_RING_KEY_BY_ROW_ID:
        case SECRET_KEY_RING_KEY_BY_ROW_ID:
            return Keys.CONTENT_ITEM_TYPE;

        case PUBLIC_KEY_RING_USER_ID:
        case SECRET_KEY_RING_USER_ID:
            return UserIds.CONTENT_TYPE;

        case PUBLIC_KEY_RING_USER_ID_BY_ROW_ID:
        case SECRET_KEY_RING_USER_ID_BY_ROW_ID:
            return UserIds.CONTENT_ITEM_TYPE;

        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * Returns type of the query (secret/public)
     * 
     * @param uri
     * @return
     */
    private int getKeyType(int match) {
        int type;
        switch (match) {
        case PUBLIC_KEY_RING:
        case PUBLIC_KEY_RING_BY_ROW_ID:
        case PUBLIC_KEY_RING_BY_MASTER_KEY_ID:
        case PUBLIC_KEY_RING_BY_KEY_ID:
        case PUBLIC_KEY_RING_BY_EMAILS:
        case PUBLIC_KEY_RING_BY_LIKE_EMAIL:
        case PUBLIC_KEY_RING_KEY:
        case PUBLIC_KEY_RING_KEY_BY_ROW_ID:
        case PUBLIC_KEY_RING_USER_ID:
        case PUBLIC_KEY_RING_USER_ID_BY_ROW_ID:
            type = KeyTypes.PUBLIC;
            break;

        case SECRET_KEY_RING:
        case SECRET_KEY_RING_BY_ROW_ID:
        case SECRET_KEY_RING_BY_MASTER_KEY_ID:
        case SECRET_KEY_RING_BY_KEY_ID:
        case SECRET_KEY_RING_BY_EMAILS:
        case SECRET_KEY_RING_BY_LIKE_EMAIL:
        case SECRET_KEY_RING_KEY:
        case SECRET_KEY_RING_KEY_BY_ROW_ID:
        case SECRET_KEY_RING_USER_ID:
        case SECRET_KEY_RING_USER_ID_BY_ROW_ID:
            type = KeyTypes.SECRET;
            break;

        default:
            Log.e("Stm-9", "Unknown match " + match);
            type = -1;
            break;
        }

        return type;
    }

    /**
     * Set result of query to specific columns, don't show blob column for external content provider
     * 
     * @return
     */
    private HashMap<String, String> getProjectionMapForKeyRings() {
        HashMap<String, String> projectionMap = new HashMap<String, String>();

        projectionMap.put(BaseColumns._ID, Tables.KEY_RINGS + "." + BaseColumns._ID);
        projectionMap.put(KeyRingsColumns.MASTER_KEY_ID, Tables.KEY_RINGS + "."
                + KeyRingsColumns.MASTER_KEY_ID);
        projectionMap.put(KeyRingsColumns.KEY_RING_DATA, Tables.KEY_RINGS + "."
                + KeyRingsColumns.KEY_RING_DATA);
        projectionMap.put(UserIdsColumns.USER_ID, Tables.USER_IDS + "." + UserIdsColumns.USER_ID);

        return projectionMap;
    }

    /**
     * Set result of query to specific columns, don't show blob column for external content provider
     * 
     * @return
     */
    private HashMap<String, String> getProjectionMapForKeys() {
        HashMap<String, String> projectionMap = new HashMap<String, String>();

        projectionMap.put(BaseColumns._ID, BaseColumns._ID);
        projectionMap.put(KeysColumns.KEY_ID, KeysColumns.KEY_ID);
        projectionMap.put(KeysColumns.IS_MASTER_KEY, KeysColumns.IS_MASTER_KEY);
        projectionMap.put(KeysColumns.ALGORITHM, KeysColumns.ALGORITHM);
        projectionMap.put(KeysColumns.KEY_SIZE, KeysColumns.KEY_SIZE);
        projectionMap.put(KeysColumns.CAN_CERTIFY, KeysColumns.CAN_CERTIFY);
        projectionMap.put(KeysColumns.CAN_SIGN, KeysColumns.CAN_SIGN);
        projectionMap.put(KeysColumns.CAN_ENCRYPT, KeysColumns.CAN_ENCRYPT);
        projectionMap.put(KeysColumns.IS_REVOKED, KeysColumns.IS_REVOKED);
        projectionMap.put(KeysColumns.CREATION, KeysColumns.CREATION);
        projectionMap.put(KeysColumns.EXPIRY, KeysColumns.EXPIRY);
        projectionMap.put(KeysColumns.KEY_RING_ROW_ID, KeysColumns.KEY_RING_ROW_ID);
        projectionMap.put(KeysColumns.KEY_DATA, KeysColumns.KEY_DATA);
        projectionMap.put(KeysColumns.RANK, KeysColumns.RANK);

        return projectionMap;
    }

    /**
     * Builds default query for keyRings: KeyRings table is joined with UserIds
     * 
     * @param qb
     * @param match
     * @param isMasterKey
     * @param sortOrder
     * @return
     */
    private SQLiteQueryBuilder buildKeyRingQuery(SQLiteQueryBuilder qb, int match, String sortOrder) {
        // public or secret keyring
        qb.appendWhere(Tables.KEY_RINGS + "." + KeyRingsColumns.TYPE + " = ");
        qb.appendWhereEscapeString(Integer.toString(getKeyType(match)));

        // join keyrings with userIds to every keyring
        qb.setTables(Tables.KEY_RINGS + " INNER JOIN " + Tables.USER_IDS + " ON " + "("
                + Tables.KEY_RINGS + "." + BaseColumns._ID + " = " + Tables.USER_IDS + "."
                + UserIdsColumns.KEY_RING_ROW_ID + " AND " + Tables.USER_IDS + "."
                + UserIdsColumns.RANK + " = '0')");

        qb.setProjectionMap(getProjectionMapForKeyRings());

        return qb;
    }

    /**
     * Builds default query for keyRings: KeyRings table is joined with Keys and UserIds
     * 
     * @param qb
     * @param match
     * @param isMasterKey
     * @param sortOrder
     * @return
     */
    private SQLiteQueryBuilder buildKeyRingQueryWithKeys(SQLiteQueryBuilder qb, int match,
            String sortOrder) {
        // public or secret keyring
        qb.appendWhere(Tables.KEY_RINGS + "." + KeyRingsColumns.TYPE + " = ");
        qb.appendWhereEscapeString(Integer.toString(getKeyType(match)));

        // join keyrings with keys and userIds to every keyring
        qb.setTables(Tables.KEY_RINGS + " INNER JOIN " + Tables.KEYS + " ON " + "("
                + Tables.KEY_RINGS + "." + BaseColumns._ID + " = " + Tables.KEYS + "."
                + KeysColumns.KEY_RING_ROW_ID + ") " + " INNER JOIN " + Tables.USER_IDS + " ON "
                + "(" + Tables.KEY_RINGS + "." + BaseColumns._ID + " = " + Tables.USER_IDS + "."
                + UserIdsColumns.KEY_RING_ROW_ID + " AND " + Tables.USER_IDS + "."
                + UserIdsColumns.RANK + " = '0')");

        qb.setProjectionMap(getProjectionMapForKeyRings());

        return qb;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Log.v("Stm-9", "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mApgDatabase.getReadableDatabase();

        int match = mUriMatcher.match(uri);

        switch (match) {
        case PUBLIC_KEY_RING:
        case SECRET_KEY_RING:
            qb = buildKeyRingQuery(qb, match, sortOrder);

            if (TextUtils.isEmpty(sortOrder)) {
                sortOrder = Tables.USER_IDS + "." + UserIdsColumns.USER_ID + " ASC";
            }

            break;

        case PUBLIC_KEY_RING_BY_ROW_ID:
        case SECRET_KEY_RING_BY_ROW_ID:
            qb = buildKeyRingQuery(qb, match, sortOrder);

            qb.appendWhere(" AND " + Tables.KEY_RINGS + "." + BaseColumns._ID + " = ");
            qb.appendWhereEscapeString(uri.getLastPathSegment());

            if (TextUtils.isEmpty(sortOrder)) {
                sortOrder = Tables.USER_IDS + "." + UserIdsColumns.USER_ID + " ASC";
            }

            break;

        case PUBLIC_KEY_RING_BY_MASTER_KEY_ID:
        case SECRET_KEY_RING_BY_MASTER_KEY_ID:
            qb = buildKeyRingQuery(qb, match, sortOrder);

            qb.appendWhere(" AND " + Tables.KEY_RINGS + "." + KeyRingsColumns.MASTER_KEY_ID + " = ");
            qb.appendWhereEscapeString(uri.getLastPathSegment());

            if (TextUtils.isEmpty(sortOrder)) {
                sortOrder = Tables.USER_IDS + "." + UserIdsColumns.USER_ID + " ASC";
            }

            break;

        case SECRET_KEY_RING_BY_KEY_ID:
        case PUBLIC_KEY_RING_BY_KEY_ID:
            qb = buildKeyRingQueryWithKeys(qb, match, sortOrder);

            qb.appendWhere(" AND " + Tables.KEYS + "." + KeysColumns.KEY_ID + " = ");
            qb.appendWhereEscapeString(uri.getLastPathSegment());

            if (TextUtils.isEmpty(sortOrder)) {
                sortOrder = Tables.USER_IDS + "." + UserIdsColumns.USER_ID + " ASC";
            }

            break;

        case SECRET_KEY_RING_BY_EMAILS:
        case PUBLIC_KEY_RING_BY_EMAILS:
            qb = buildKeyRingQuery(qb, match, sortOrder);

            String emails = uri.getLastPathSegment();
            String chunks[] = emails.split(" *, *");
            boolean gotCondition = false;
            String emailWhere = "";
            for (int i = 0; i < chunks.length; ++i) {
                if (chunks[i].length() == 0) {
                    continue;
                }
                if (i != 0) {
                    emailWhere += " OR ";
                }
                emailWhere += "tmp." + UserIdsColumns.USER_ID + " LIKE ";
                // match '*<email>', so it has to be at the *end* of the user id
                emailWhere += DatabaseUtils.sqlEscapeString("%<" + chunks[i] + ">");
                gotCondition = true;
            }

            if (gotCondition) {
                qb.appendWhere(" AND EXISTS (SELECT tmp." + BaseColumns._ID + " FROM "
                        + Tables.USER_IDS + " AS tmp WHERE tmp." + UserIdsColumns.KEY_RING_ROW_ID
                        + " = " + Tables.KEY_RINGS + "." + BaseColumns._ID + " AND (" + emailWhere
                        + "))");
            }

            break;

        case SECRET_KEY_RING_BY_LIKE_EMAIL:
        case PUBLIC_KEY_RING_BY_LIKE_EMAIL:
            qb = buildKeyRingQuery(qb, match, sortOrder);

            String likeEmail = uri.getLastPathSegment();

            String likeEmailWhere = "tmp." + UserIdsColumns.USER_ID + " LIKE "
                    + DatabaseUtils.sqlEscapeString("%<%" + likeEmail + "%>");

            qb.appendWhere(" AND EXISTS (SELECT tmp." + BaseColumns._ID + " FROM "
                    + Tables.USER_IDS + " AS tmp WHERE tmp." + UserIdsColumns.KEY_RING_ROW_ID
                    + " = " + Tables.KEY_RINGS + "." + BaseColumns._ID + " AND (" + likeEmailWhere
                    + "))");

            break;

        case PUBLIC_KEY_RING_KEY:
        case SECRET_KEY_RING_KEY:
            qb.setTables(Tables.KEYS);
            qb.appendWhere(KeysColumns.TYPE + " = ");
            qb.appendWhereEscapeString(Integer.toString(getKeyType(match)));

            qb.appendWhere(" AND " + KeysColumns.KEY_RING_ROW_ID + " = ");
            qb.appendWhereEscapeString(uri.getPathSegments().get(2));

            qb.setProjectionMap(getProjectionMapForKeys());

            break;

        case PUBLIC_KEY_RING_KEY_BY_ROW_ID:
        case SECRET_KEY_RING_KEY_BY_ROW_ID:
            qb.setTables(Tables.KEYS);
            qb.appendWhere(KeysColumns.TYPE + " = ");
            qb.appendWhereEscapeString(Integer.toString(getKeyType(match)));

            qb.appendWhere(" AND " + KeysColumns.KEY_RING_ROW_ID + " = ");
            qb.appendWhereEscapeString(uri.getPathSegments().get(2));

            qb.appendWhere(" AND " + BaseColumns._ID + " = ");
            qb.appendWhereEscapeString(uri.getLastPathSegment());

            qb.setProjectionMap(getProjectionMapForKeys());

            break;

        case PUBLIC_KEY_RING_USER_ID:
        case SECRET_KEY_RING_USER_ID:
            qb.setTables(Tables.USER_IDS);
            qb.appendWhere(UserIdsColumns.KEY_RING_ROW_ID + " = ");
            qb.appendWhereEscapeString(uri.getPathSegments().get(2));

            break;

        case PUBLIC_KEY_RING_USER_ID_BY_ROW_ID:
        case SECRET_KEY_RING_USER_ID_BY_ROW_ID:
            qb.setTables(Tables.USER_IDS);
            qb.appendWhere(UserIdsColumns.KEY_RING_ROW_ID + " = ");
            qb.appendWhereEscapeString(uri.getPathSegments().get(2));

            qb.appendWhere(" AND " + BaseColumns._ID + " = ");
            qb.appendWhereEscapeString(uri.getLastPathSegment());

            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);

        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = null;
        } else {
            orderBy = sortOrder;
        }

        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);

        if (BuildConfig.DEBUG) {
            Log.d("Stm-9",
                    "Query: "
                            + qb.buildQuery(projection, selection, selectionArgs, null, null,
                                    orderBy, null));
            Log.d("Stm-9", "Cursor: " + DatabaseUtils.dumpCursorToString(c));
        }

        return c;
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d("Stm-9", "insert(uri=" + uri + ", values=" + values.toString() + ")");

        final SQLiteDatabase db = mApgDatabase.getWritableDatabase();

        Uri rowUri = null;
        long rowId = -1;
        try {
            final int match = mUriMatcher.match(uri);

            switch (match) {
            case PUBLIC_KEY_RING:
                values.put(KeyRings.TYPE, KeyTypes.PUBLIC);

                rowId = db.insertOrThrow(Tables.KEY_RINGS, null, values);
                rowUri = KeyRings.buildPublicKeyRingsUri(Long.toString(rowId));
                sendBroadcastDatabaseChange(getKeyType(match), getType(uri));

                break;
            case PUBLIC_KEY_RING_KEY:
                values.put(Keys.TYPE, KeyTypes.PUBLIC);

                rowId = db.insertOrThrow(Tables.KEYS, null, values);
                rowUri = Keys.buildPublicKeysUri(Long.toString(rowId));
                sendBroadcastDatabaseChange(getKeyType(match), getType(uri));

                break;
            case PUBLIC_KEY_RING_USER_ID:
                rowId = db.insertOrThrow(Tables.USER_IDS, null, values);
                rowUri = UserIds.buildPublicUserIdsUri(Long.toString(rowId));
                sendBroadcastDatabaseChange(getKeyType(match), getType(uri));

                break;
            case SECRET_KEY_RING:
                values.put(KeyRings.TYPE, KeyTypes.SECRET);

                rowId = db.insertOrThrow(Tables.KEY_RINGS, null, values);
                rowUri = KeyRings.buildSecretKeyRingsUri(Long.toString(rowId));
                sendBroadcastDatabaseChange(getKeyType(match), getType(uri));

                break;
            case SECRET_KEY_RING_KEY:
                values.put(Keys.TYPE, KeyTypes.SECRET);

                rowId = db.insertOrThrow(Tables.KEYS, null, values);
                rowUri = Keys.buildSecretKeysUri(Long.toString(rowId));
                sendBroadcastDatabaseChange(getKeyType(match), getType(uri));

                break;
            case SECRET_KEY_RING_USER_ID:
                rowId = db.insertOrThrow(Tables.USER_IDS, null, values);
                rowUri = UserIds.buildSecretUserIdsUri(Long.toString(rowId));

                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }

            // notify of changes in db
            getContext().getContentResolver().notifyChange(uri, null);

        } catch (SQLiteConstraintException e) {
            Log.e("Stm-9", "Constraint exception on insert! Entry already existing?");
        }

        return rowUri;
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.v("Stm-9", "delete(uri=" + uri + ")");

        final SQLiteDatabase db = mApgDatabase.getWritableDatabase();

        int count;
        final int match = mUriMatcher.match(uri);

        String defaultSelection = null;
        switch (match) {
        case PUBLIC_KEY_RING_BY_ROW_ID:
        case SECRET_KEY_RING_BY_ROW_ID:
            defaultSelection = BaseColumns._ID + "=" + uri.getLastPathSegment();
            // corresponding keys and userIds are deleted by ON DELETE CASCADE
            count = db.delete(Tables.KEY_RINGS,
                    buildDefaultKeyRingsSelection(defaultSelection, getKeyType(match), selection),
                    selectionArgs);
            sendBroadcastDatabaseChange(getKeyType(match), getType(uri));
            break;
        case PUBLIC_KEY_RING_BY_MASTER_KEY_ID:
        case SECRET_KEY_RING_BY_MASTER_KEY_ID:
            defaultSelection = KeyRings.MASTER_KEY_ID + "=" + uri.getLastPathSegment();
            // corresponding keys and userIds are deleted by ON DELETE CASCADE
            count = db.delete(Tables.KEY_RINGS,
                    buildDefaultKeyRingsSelection(defaultSelection, getKeyType(match), selection),
                    selectionArgs);
            sendBroadcastDatabaseChange(getKeyType(match), getType(uri));
            break;
        case PUBLIC_KEY_RING_KEY_BY_ROW_ID:
        case SECRET_KEY_RING_KEY_BY_ROW_ID:
            count = db.delete(Tables.KEYS,
                    buildDefaultKeysSelection(uri, getKeyType(match), selection), selectionArgs);
            sendBroadcastDatabaseChange(getKeyType(match), getType(uri));
            break;
        case PUBLIC_KEY_RING_USER_ID_BY_ROW_ID:
        case SECRET_KEY_RING_USER_ID_BY_ROW_ID:
            count = db.delete(Tables.KEYS, buildDefaultUserIdsSelection(uri, selection),
                    selectionArgs);
            break;

        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // notify of changes in db
        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.v("Stm-9", "update(uri=" + uri + ", values=" + values.toString() + ")");

        final SQLiteDatabase db = mApgDatabase.getWritableDatabase();

        String defaultSelection = null;
        int count = 0;
        try {
            final int match = mUriMatcher.match(uri);
            switch (match) {
            case PUBLIC_KEY_RING_BY_ROW_ID:
            case SECRET_KEY_RING_BY_ROW_ID:
                defaultSelection = BaseColumns._ID + "=" + uri.getLastPathSegment();

                count = db.update(
                        Tables.KEY_RINGS,
                        values,
                        buildDefaultKeyRingsSelection(defaultSelection, getKeyType(match),
                                selection), selectionArgs);
                sendBroadcastDatabaseChange(getKeyType(match), getType(uri));

                break;
            case PUBLIC_KEY_RING_BY_MASTER_KEY_ID:
            case SECRET_KEY_RING_BY_MASTER_KEY_ID:
                defaultSelection = KeyRings.MASTER_KEY_ID + "=" + uri.getLastPathSegment();

                count = db.update(
                        Tables.KEY_RINGS,
                        values,
                        buildDefaultKeyRingsSelection(defaultSelection, getKeyType(match),
                                selection), selectionArgs);
                sendBroadcastDatabaseChange(getKeyType(match), getType(uri));

                break;
            case PUBLIC_KEY_RING_KEY_BY_ROW_ID:
            case SECRET_KEY_RING_KEY_BY_ROW_ID:
                count = db
                        .update(Tables.KEYS, values,
                                buildDefaultKeysSelection(uri, getKeyType(match), selection),
                                selectionArgs);
                sendBroadcastDatabaseChange(getKeyType(match), getType(uri));

                break;
            case PUBLIC_KEY_RING_USER_ID_BY_ROW_ID:
            case SECRET_KEY_RING_USER_ID_BY_ROW_ID:
                count = db.update(Tables.USER_IDS, values,
                        buildDefaultUserIdsSelection(uri, selection), selectionArgs);
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }

            // notify of changes in db
            getContext().getContentResolver().notifyChange(uri, null);

        } catch (SQLiteConstraintException e) {
            Log.e("Stm-9", "Constraint exception on update! Entry already existing?");
        }

        return count;
    }

    /**
     * Build default selection statement for KeyRings. If no extra selection is specified only build
     * where clause with rowId
     * 
     * @param uri
     * @param selection
     * @return
     */
    private String buildDefaultKeyRingsSelection(String defaultSelection, Integer keyType,
            String selection) {
        String andType = "";
        if (keyType != null) {
            andType = " AND " + KeyRingsColumns.TYPE + "=" + keyType;
        }

        String andSelection = "";
        if (!TextUtils.isEmpty(selection)) {
            andSelection = " AND (" + selection + ")";
        }

        return defaultSelection + andType + andSelection;
    }

    /**
     * Build default selection statement for Keys. If no extra selection is specified only build
     * where clause with rowId
     * 
     * @param uri
     * @param selection
     * @return
     */
    private String buildDefaultKeysSelection(Uri uri, Integer keyType, String selection) {
        String rowId = uri.getLastPathSegment();

        String foreignKeyRingRowId = uri.getPathSegments().get(2);
        String andForeignKeyRing = " AND " + KeysColumns.KEY_RING_ROW_ID + " = "
                + foreignKeyRingRowId;

        String andType = "";
        if (keyType != null) {
            andType = " AND " + KeysColumns.TYPE + "=" + keyType;
        }

        String andSelection = "";
        if (!TextUtils.isEmpty(selection)) {
            andSelection = " AND (" + selection + ")";
        }

        return BaseColumns._ID + "=" + rowId + andForeignKeyRing + andType + andSelection;
    }

    /**
     * Build default selection statement for UserIds. If no extra selection is specified only build
     * where clause with rowId
     * 
     * @param uri
     * @param selection
     * @return
     */
    private String buildDefaultUserIdsSelection(Uri uri, String selection) {
        String rowId = uri.getLastPathSegment();

        String foreignKeyRingRowId = uri.getPathSegments().get(2);
        String andForeignKeyRing = " AND " + KeysColumns.KEY_RING_ROW_ID + " = "
                + foreignKeyRingRowId;

        String andSelection = "";
        if (!TextUtils.isEmpty(selection)) {
            andSelection = " AND (" + selection + ")";
        }

        return BaseColumns._ID + "=" + rowId + andForeignKeyRing + andSelection;
    }

    /**
     * This broadcast is send system wide to inform other application that a keyring was inserted,
     * updated, or deleted
     */
    private void sendBroadcastDatabaseChange(int keyType, String contentItemType) {
    }
}
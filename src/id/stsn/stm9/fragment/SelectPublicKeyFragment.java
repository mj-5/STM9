package id.stsn.stm9.fragment;

import java.util.Date;
import java.util.Vector;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;
import id.stsn.stm9.provider.KeyDatabase;
import id.stsn.stm9.provider.KeyContract.KeyRings;
import id.stsn.stm9.provider.KeyContract.Keys;
import id.stsn.stm9.provider.KeyContract.UserIds;
import id.stsn.stm9.provider.KeyDatabase.Tables;
import id.stsn.stm9.services.SelectKeyCursorAdapter;

import android.app.Activity;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.widget.ListView;

public class SelectPublicKeyFragment extends ListFragmentWorkaround implements
        LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_PRESELECTED_KEY_IDS = "preselected_key_ids";

    private Activity mActivity;
    private SelectKeyCursorAdapter mAdapter;
    private ListView mListView;

    private long mSelectedMasterKeyIds[];

    /**
     * Creates new instance of this fragment
     */
    public static SelectPublicKeyFragment newInstance(long[] preselectedKeyIds) {
        SelectPublicKeyFragment frag = new SelectPublicKeyFragment();
        Bundle args = new Bundle();

        args.putLongArray(ARG_PRESELECTED_KEY_IDS, preselectedKeyIds);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSelectedMasterKeyIds = getArguments().getLongArray(ARG_PRESELECTED_KEY_IDS);
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivity = getSherlockActivity();
        mListView = getListView();

        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        setEmptyText(getString(R.string.list_empty));

        mAdapter = new SelectKeyCursorAdapter(mActivity, null, 0, mListView, Id.tipe.public_key);

        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        getLoaderManager().initLoader(0, null, this);
    }

    /**
     * Selects items based on master key ids in list view
     * 
     * @param masterKeyIds
     */
    private void preselectMasterKeyIds(long[] masterKeyIds) {
        if (masterKeyIds != null) {
            for (int i = 0; i < mListView.getCount(); ++i) {
                long keyId = mAdapter.getMasterKeyId(i);
                for (int j = 0; j < masterKeyIds.length; ++j) {
                    if (keyId == masterKeyIds[j]) {
                        mListView.setItemChecked(i, true);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Returns all selected master key ids
     * 
     * @return
     */
    public long[] getSelectedMasterKeyIds() {
        Vector<Long> vector = new Vector<Long>();
        for (int i = 0; i < mListView.getCount(); ++i) {
            if (mListView.isItemChecked(i)) {
                vector.add(mAdapter.getMasterKeyId(i));
            }
        }

        // convert to long array
        long[] selectedMasterKeyIds = new long[vector.size()];
        for (int i = 0; i < vector.size(); ++i) {
            selectedMasterKeyIds[i] = vector.get(i);
        }

        return selectedMasterKeyIds;
    }

    /**
     * Returns all selected user ids
     * 
     * @return
     */
    public String[] getSelectedUserIds() {
        Vector<String> userIds = new Vector<String>();
        for (int i = 0; i < mListView.getCount(); ++i) {
            if (mListView.isItemChecked(i)) {
                userIds.add((String) mAdapter.getUserId(i));
            }
        }

        // make empty array to not return null
        String userIdArray[] = new String[0];
        return userIds.toArray(userIdArray);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri = KeyRings.buildPublicKeyRingsUri();

        long now = new Date().getTime() / 1000;
        String[] projection = new String[] {
                KeyRings._ID,
                KeyRings.MASTER_KEY_ID,
                UserIds.USER_ID,
                "(SELECT COUNT(available_keys." + Keys._ID + ") FROM " + Tables.KEYS
                        + " AS available_keys WHERE available_keys." + Keys.KEY_RING_ROW_ID + " = "
                        + KeyDatabase.Tables.KEY_RINGS + "." + KeyRings._ID
                        + " AND available_keys." + Keys.IS_REVOKED + " = '0' AND  available_keys."
                        + Keys.CAN_ENCRYPT + " = '1') AS "
                        + SelectKeyCursorAdapter.PROJECTION_ROW_AVAILABLE,
                "(SELECT COUNT(valid_keys." + Keys._ID + ") FROM " + Tables.KEYS
                        + " AS valid_keys WHERE valid_keys." + Keys.KEY_RING_ROW_ID + " = "
                        + KeyDatabase.Tables.KEY_RINGS + "." + KeyRings._ID
                        + " AND valid_keys." + Keys.IS_REVOKED + " = '0' AND valid_keys."
                        + Keys.CAN_ENCRYPT + " = '1' AND valid_keys." + Keys.CREATION + " <= '"
                        + now + "' AND " + "(valid_keys." + Keys.EXPIRY + " IS NULL OR valid_keys."
                        + Keys.EXPIRY + " >= '" + now + "')) AS "
                        + SelectKeyCursorAdapter.PROJECTION_ROW_VALID, };

        String inMasterKeyList = null;
        if (mSelectedMasterKeyIds != null && mSelectedMasterKeyIds.length > 0) {
            inMasterKeyList = KeyRings.MASTER_KEY_ID + " IN (";
            for (int i = 0; i < mSelectedMasterKeyIds.length; ++i) {
                if (i != 0) {
                    inMasterKeyList += ", ";
                }
                inMasterKeyList += DatabaseUtils.sqlEscapeString("" + mSelectedMasterKeyIds[i]);
            }
            inMasterKeyList += ")";
        }

        String orderBy = UserIds.USER_ID + " ASC";
        if (inMasterKeyList != null) {
            // sort by selected master keys
            orderBy = inMasterKeyList + " DESC, " + orderBy;
        }

        return new CursorLoader(getActivity(), baseUri, projection, null, null, orderBy);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }

        // preselect given master keys
        preselectMasterKeyIds(mSelectedMasterKeyIds);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}

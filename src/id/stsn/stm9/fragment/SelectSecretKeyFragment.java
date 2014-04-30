package id.stsn.stm9.fragment;

import java.util.Date;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;
import id.stsn.stm9.activity.SelectSecretKeyActivity;
import id.stsn.stm9.provider.KeyDatabase;
import id.stsn.stm9.provider.KeyContract.KeyRings;
import id.stsn.stm9.provider.KeyContract.Keys;
import id.stsn.stm9.provider.KeyContract.UserIds;
import id.stsn.stm9.provider.KeyDatabase.Tables;
import id.stsn.stm9.services.SelectKeyCursorAdapter;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

public class SelectSecretKeyFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private SelectSecretKeyActivity mActivity;
    private SelectKeyCursorAdapter mAdapter;
    private ListView mListView;
    
    private boolean mFilterCertify;
    
    private static final String ARG_FILTER_CERTIFY = "filter_certify";

    /**
     * Creates new instance of this fragment
     */
    public static SelectSecretKeyFragment newInstance(boolean filterCertify) {
        SelectSecretKeyFragment frag = new SelectSecretKeyFragment();
        Bundle args = new Bundle();

        args.putBoolean(ARG_FILTER_CERTIFY, filterCertify);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilterCertify = getArguments().getBoolean(ARG_FILTER_CERTIFY);
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivity = (SelectSecretKeyActivity) getSherlockActivity();
        mListView = getListView();

        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                long masterKeyId = mAdapter.getMasterKeyId(position);
                String userId = mAdapter.getUserId(position);

                // return data to activity, which results in finishing it
                mActivity.afterListSelection(masterKeyId, userId);
            }
        });

        setEmptyText(getString(R.string.list_empty));

        mAdapter = new SelectKeyCursorAdapter(mActivity, null, 0, mListView, Id.tipe.secret_key);

        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri = KeyRings.buildSecretKeyRingsUri();

        String CapFilter = null;
        if (mFilterCertify) {
            CapFilter = "(cert > 0)";
        }

        long now = new Date().getTime() / 1000;
        String[] projection = new String[] {
                KeyRings._ID,
                KeyRings.MASTER_KEY_ID,
                UserIds.USER_ID,
                "(SELECT COUNT(cert_keys." + Keys._ID + ") FROM " + Tables.KEYS
                        + " AS cert_keys WHERE cert_keys." + Keys.KEY_RING_ROW_ID + " = "
                        + KeyDatabase.Tables.KEY_RINGS + "." + KeyRings._ID + " AND cert_keys."
                        + Keys.CAN_CERTIFY + " = '1') AS cert",
                "(SELECT COUNT(available_keys." + Keys._ID + ") FROM " + Tables.KEYS
                        + " AS available_keys WHERE available_keys." + Keys.KEY_RING_ROW_ID + " = "
                        + KeyDatabase.Tables.KEY_RINGS + "." + KeyRings._ID
                        + " AND available_keys." + Keys.IS_REVOKED + " = '0' AND  available_keys."
                        + Keys.CAN_SIGN + " = '1') AS "
                        + SelectKeyCursorAdapter.PROJECTION_ROW_AVAILABLE,
                "(SELECT COUNT(valid_keys." + Keys._ID + ") FROM " + Tables.KEYS
                        + " AS valid_keys WHERE valid_keys." + Keys.KEY_RING_ROW_ID + " = "
                        + KeyDatabase.Tables.KEY_RINGS + "." + KeyRings._ID + " AND valid_keys."
                        + Keys.IS_REVOKED + " = '0' AND valid_keys." + Keys.CAN_SIGN
                        + " = '1' AND valid_keys." + Keys.CREATION + " <= '" + now + "' AND "
                        + "(valid_keys." + Keys.EXPIRY + " IS NULL OR valid_keys." + Keys.EXPIRY
                        + " >= '" + now + "')) AS " + SelectKeyCursorAdapter.PROJECTION_ROW_VALID, };

        String orderBy = UserIds.USER_ID + " ASC";

        return new CursorLoader(getActivity(), baseUri, projection, CapFilter, null, orderBy);
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
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}

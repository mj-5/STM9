package id.stsn.stm9.fragment;

import id.stsn.stm9.Id;
import id.stsn.stm9.activity.MasterKeyActivity;
import id.stsn.stm9.provider.ProviderHelper;
import id.stsn.stm9.provider.KeyContract.KeyRings;
import id.stsn.stm9.provider.KeyContract.UserIds;
import id.stsn.stm9.services.KeyListAdapter;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class MasterKeyListFragment extends ListKeyFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private MasterKeyActivity mKeyListSecretActivity;

    private KeyListAdapter mAdapter;

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mKeyListSecretActivity = (MasterKeyActivity) getActivity();

        mAdapter = new KeyListAdapter(mKeyListSecretActivity, null, Id.tipe.secret_key);
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        getLoaderManager().initLoader(-1, null, this);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        ExpandableListContextMenuInfo expInfo = (ExpandableListContextMenuInfo) item.getMenuInfo();

        int groupPosition = ExpandableListView.getPackedPositionGroup(expInfo.packedPosition);
        long keyRingRowId = getExpandableListAdapter().getGroupId(groupPosition);

        // get master key id using row id
        long masterKeyId = ProviderHelper
                .getSecretMasterKeyId(mKeyListSecretActivity, keyRingRowId);

        boolean masterCanSign = ProviderHelper.getSecretMasterKeyCanSign(mKeyListSecretActivity,
                keyRingRowId);
        
		return super.onContextItemSelected(item);
    }

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[] { KeyRings._ID, KeyRings.MASTER_KEY_ID,
            UserIds.USER_ID };

    static final String SORT_ORDER = UserIds.USER_ID + " ASC";

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    	Uri baseUri = KeyRings.buildSecretKeyRingsUri();

        return new CursorLoader(getActivity(), baseUri, PROJECTION, null, null, SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.setGroupCursor(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.setGroupCursor(null);
    }

}

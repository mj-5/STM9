package id.stsn.stm9.fragment;

import id.stsn.stm9.R;
import id.stsn.stm9.activity.ListKeyActivity;
import id.stsn.stm9.provider.ProviderHelper;
import id.stsn.stm9.Id;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class ListKeyFragment extends ExpandableListFragment {
    protected ListKeyActivity mKeyListActivity;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mKeyListActivity = (ListKeyActivity) getActivity();

        // register long press context menu
        registerForContextMenu(getListView());

        // Give some text to display if there is no data. In a real application this would come from a resource.
        setEmptyText(getString(R.string.list_empty));
    }

    /**
     * Context Menu on Long Click
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, Id.menu.export, 5, R.string.menu_export_key);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        ExpandableListContextMenuInfo expInfo = (ExpandableListContextMenuInfo) item.getMenuInfo();

        // expInfo.id would also return row id of childs, but we always want to get the row id of
        // the group item, thus we are using the following way
        int groupPosition = ExpandableListView.getPackedPositionGroup(expInfo.packedPosition);
        long keyRingRowId = getExpandableListAdapter().getGroupId(groupPosition);

        switch (item.getItemId()) {
        case Id.menu.export:
            long masterKeyId = ProviderHelper.getPublicMasterKeyId(mKeyListActivity, keyRingRowId);
            if (masterKeyId == -1) {
                masterKeyId = ProviderHelper.getSecretMasterKeyId(mKeyListActivity, keyRingRowId);
            }

            mKeyListActivity.showExportKeysDialog(masterKeyId);
            return true;

        default:
            return super.onContextItemSelected(item);

        }
    }
// error commit
}

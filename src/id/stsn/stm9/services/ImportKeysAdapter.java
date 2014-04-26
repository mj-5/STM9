package id.stsn.stm9.services;

import java.util.List;

import id.stsn.stm9.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class ImportKeysAdapter extends ArrayAdapter<ImportKeysListEntry> {
    protected LayoutInflater mInflater;
    protected Activity mActivity;

    protected List<ImportKeysListEntry> data;

    public ImportKeysAdapter(Activity activity) {
        super(activity, -1);
        mActivity = activity;
        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @SuppressLint("NewApi")
    public void setData(List<ImportKeysListEntry> data) {
        clear();
        if (data != null) {
            this.data = data;
            
            // add data to extended ArrayAdapter
            if (Build.VERSION.SDK_INT >= 11) {
                addAll(data);
            } else {
                for (ImportKeysListEntry entry : data) {
                    add(entry);
                }
            }
        }
    }

    public List<ImportKeysListEntry> getData() {
        return data;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImportKeysListEntry entry = data.get(position);

        View view = mInflater.inflate(R.layout.import_key_list, null);

        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        mainUserId.setText(R.string.unknown_user_id);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        mainUserIdRest.setText("");
        TextView keyId = (TextView) view.findViewById(R.id.keyId);
        keyId.setText(R.string.no_key);
        TextView fingerprint = (TextView) view.findViewById(R.id.fingerprint);
        TextView algorithm = (TextView) view.findViewById(R.id.algorithm);
        algorithm.setText("");
        TextView status = (TextView) view.findViewById(R.id.status);
        status.setText("");

        String userId = entry.userIds.get(0);
        if (userId != null) {
            String chunks[] = userId.split(" <", 2);
            userId = chunks[0];
            if (chunks.length > 1) {
                mainUserIdRest.setText("<" + chunks[1]);
            }
            if (entry.secretKey) {
                userId = mActivity.getString(R.string.secret_key) + " " + userId;
                mainUserId.setTextColor(Color.RED);
            }
            mainUserId.setText(userId);
        }

        keyId.setText(entry.hexKeyId);
        fingerprint.setText(mActivity.getString(R.string.fingerprint) + " " + entry.fingerPrint);

        if (mainUserIdRest.getText().length() == 0) {
            mainUserIdRest.setVisibility(View.GONE);
        }

        algorithm.setText("" + entry.bitStrength + "/" + entry.algorithm);

        if (entry.revoked) {
            status.setText("revoked");
        } else {
            status.setVisibility(View.GONE);
        }

        LinearLayout ll = (LinearLayout) view.findViewById(R.id.list);
        if (entry.userIds.size() == 1) {
            ll.setVisibility(View.GONE);
        } else {
            boolean first = true;
            boolean second = true;
            for (String uid : entry.userIds) {
                if (first) {
                    first = false;
                    continue;
                }
                if (!second) {
                    View sep = new View(mActivity);
                    sep.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 1));
                    sep.setBackgroundResource(android.R.drawable.divider_horizontal_dark);
                    ll.addView(sep);
                }
                TextView uidView = (TextView) mInflater.inflate(
                        R.layout.import_key_list_user, null);
                uidView.setText(uid);
                ll.addView(uidView);
                second = false;
            }
        }

        CheckBox cBox = (CheckBox) view.findViewById(R.id.selected);
        cBox.setChecked(entry.isSelected());

        return view;
    }

}

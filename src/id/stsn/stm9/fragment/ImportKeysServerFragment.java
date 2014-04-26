package id.stsn.stm9.fragment;

import id.stsn.stm9.R;
import id.stsn.stm9.activity.KeyServerQueryActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ImportKeysServerFragment extends Fragment {
    private Button mButton;

    /**
     * Creates new instance of this fragment
     */
    public static ImportKeysServerFragment newInstance() {
        ImportKeysServerFragment frag = new ImportKeysServerFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.import_keyserver_fragment, container, false);

        mButton = (Button) view.findViewById(R.id.import_keyserver_button);
        mButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO: use fragment instead of activity, handle onresult here!
                startActivityForResult(new Intent(getActivity(), KeyServerQueryActivity.class), 0);
            }
        });

        return view;
    }

}

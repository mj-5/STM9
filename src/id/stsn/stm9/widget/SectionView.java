package id.stsn.stm9.widget;

import java.util.Iterator;
import java.util.Vector;

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;
import id.stsn.stm9.fragment.ProgressDialogFragment; //v
import id.stsn.stm9.pgp.PgpConvert; //v
import id.stsn.stm9.services.KeyIntentService; //v
import id.stsn.stm9.services.KeyIntentServiceHandler; //v
import id.stsn.stm9.services.PassphraseCacheService; //v
import id.stsn.stm9.utility.Choice; //v
import id.stsn.stm9.widget.Editor.EditorListener; //v

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class SectionView extends LinearLayout implements OnClickListener, EditorListener {
    private LayoutInflater mInflater;
    private View mAdd;
    private ImageView mPlusButton;
    private ViewGroup mEditors;
    private TextView mTitle;
    private int mType = 0;

    private Choice mNewKeyAlgorithmChoice;
    private int mNewKeySize;
    private boolean canEdit = true;

    private SherlockFragmentActivity mActivity;

    private ProgressDialogFragment mGeneratingDialog;

    public SectionView(Context context) {
        super(context);
        mActivity = (SherlockFragmentActivity) context;
    }

    public SectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mActivity = (SherlockFragmentActivity) context;
    }

    public ViewGroup getEditors() {
        return mEditors;
    }

    public void setType(int type) {
        mType = type;
        switch (type) {
        case Id.tipe.user_id: {
            mTitle.setText(R.string.section_user_ids);
            break;
        }

        case Id.tipe.key: {
        mTitle.setText(R.string.section_keys);
            break;
        }

        default: {
            break;
        }
        }
    }

    public void setCanEdit(boolean bCanEdit) {
        canEdit = bCanEdit;
        mPlusButton = (ImageView) findViewById(R.id.plusbutton);
        if (!canEdit) {
            mPlusButton.setVisibility(View.INVISIBLE);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onFinishInflate() {
        mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        mAdd = findViewById(R.id.header);
        mAdd.setOnClickListener(this);

        mEditors = (ViewGroup) findViewById(R.id.editors);
        mTitle = (TextView) findViewById(R.id.title);

        updateEditorsVisible();
        super.onFinishInflate();
    }

    /** {@inheritDoc} */
    public void onDeleted(Editor editor) {
        this.updateEditorsVisible();
    }

    protected void updateEditorsVisible() {
        final boolean hasChildren = mEditors.getChildCount() > 0;
        mEditors.setVisibility(hasChildren ? View.VISIBLE : View.GONE);
    }

    /** {@inheritDoc} */
    public void onClick(View v) {
        if (canEdit) {
            switch (mType) {
            /* User ID*/
            case Id.tipe.user_id: {
             UserIdEditor view = (UserIdEditor) mInflater.inflate(
                        R.layout.edit_key_user_id_item, mEditors, false);
                view.setEditorListener(this);
                if (mEditors.getChildCount() == 0) {
                    view.setIsMainUserId(true);
                }
                mEditors.addView(view);
                break;
            }

            /* Keys */
            case Id.tipe.key: {
           AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());

                View view = mInflater.inflate(R.layout.create_key, null);
                dialog.setView(view);
                dialog.setTitle(R.string.title_generate_kunci);

                final Spinner algorithm = (Spinner) view.findViewById(R.id.create_key_algorithm);
                Vector<Choice> choices = new Vector<Choice>();
                choices.add(new Choice(Id.pilihan.algoritma.rsa, getResources().getString(
                        R.string.rsa)));

                ArrayAdapter<Choice> adapter = new ArrayAdapter<Choice>(getContext(),
                        android.R.layout.simple_spinner_item, choices);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                algorithm.setAdapter(adapter);
                /* make RSA the default */ 
                for (int i = 0; i < choices.size(); ++i) {
                    if (choices.get(i).getId() == Id.pilihan.algoritma.rsa) {
                        algorithm.setSelection(i);
                        break;
                    }
                }

                final Spinner keySize = (Spinner) view.findViewById(R.id.create_key_size);
                ArrayAdapter<CharSequence> keySizeAdapter = ArrayAdapter.createFromResource(
                        getContext(), R.array.key_size_spinner_values,
                        android.R.layout.simple_spinner_item);
                keySizeAdapter
                        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                keySize.setAdapter(keySizeAdapter);
                keySize.setSelection(0); // Default to 1024 for the key length
                dialog.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface di, int id) {
                                di.dismiss();
                                try {
                                    int nKeyIndex = keySize.getSelectedItemPosition();
                                    switch (nKeyIndex) {
                                    case 0:
                                        mNewKeySize = 1024;
                                        break;
                                    case 1:
                                        mNewKeySize = 2048;
                                        break;
                                    }
                                } catch (NumberFormatException e) {
                                    mNewKeySize = 0;
                                }

                                mNewKeyAlgorithmChoice = (Choice) algorithm.getSelectedItem();
                                createKey();
                            }
                        });

                dialog.setCancelable(true);
                dialog.setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface di, int id) {
                                di.dismiss();
                            }
                        });

                dialog.create().show();
                break;
            }

            default: {
                break;
            }
            }
            this.updateEditorsVisible();
        }
    }

    /**
     * set User Id
     * @param list
     */
    public void setUserIds(Vector<String> list) {
        if (mType != Id.tipe.user_id) {
            return;
        }

        mEditors.removeAllViews();
        for (String userId : list) {
            UserIdEditor view = (UserIdEditor) mInflater.inflate(R.layout.edit_key_user_id_item,
                    mEditors, false);
            view.setEditorListener(this);
            view.setValue(userId);
            if (mEditors.getChildCount() == 0) {
                view.setIsMainUserId(true);
            }
            view.setCanEdit(canEdit);
            mEditors.addView(view);
        }

        this.updateEditorsVisible();
    }

    /**
     * set Keys
     * @param list
     * @param usages
     */
    public void setKeys(Vector<PGPSecretKey> list, Vector<Integer> usages) {
        if (mType != Id.tipe.key) {
            return;
        }

        mEditors.removeAllViews();

        // go through all keys and set view based on them
        for (int i = 0; i < list.size(); i++) {
            KeyEditor view = (KeyEditor) mInflater.inflate(R.layout.edit_key_key_item, mEditors,
                    false);
            view.setEditorListener(this);
            boolean isMasterKey = (mEditors.getChildCount() == 0);
            view.setValue(list.get(i), isMasterKey, usages.get(i));
            view.setCanEdit(canEdit);
            mEditors.addView(view);
        }

        this.updateEditorsVisible();
    }

    private void createKey() {
        /*Send all information needed to service to edit key in other thread*/ 
        Intent intent = new Intent(mActivity, KeyIntentService.class);

        intent.setAction(KeyIntentService.ACTION_GENERATE_KEY);

        // fill values for this action
        Bundle data = new Bundle();

        String passPhrase;
        if (mEditors.getChildCount() > 0) {
            PGPSecretKey masterKey = ((KeyEditor) mEditors.getChildAt(0)).getValue();
            passPhrase = PassphraseCacheService
                    .getCachedPassphrase(mActivity, masterKey.getKeyID());

            data.putByteArray(KeyIntentService.GENERATE_KEY_MASTER_KEY,
                    PgpConvert.PGPSecretKeyToBytes(masterKey));
        } else {
            passPhrase = "";
        }
        data.putString(KeyIntentService.GENERATE_KEY_SYMMETRIC_PASSPHRASE, passPhrase);
        data.putInt(KeyIntentService.GENERATE_KEY_ALGORITHM, mNewKeyAlgorithmChoice.getId());
        data.putInt(KeyIntentService.GENERATE_KEY_KEY_SIZE, mNewKeySize);

        intent.putExtra(KeyIntentService.EXTRA_DATA, data);

        // show progress dialog
        mGeneratingDialog = ProgressDialogFragment.newInstance(R.string.proses_generate,
                ProgressDialog.STYLE_SPINNER);

        // Message is received after generating is done in ApgService
        KeyIntentServiceHandler saveHandler = new KeyIntentServiceHandler(mActivity,
                mGeneratingDialog) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == KeyIntentServiceHandler.MESSAGE_OKAY) {
                    // get new key from data bundle returned from service
                    Bundle data = message.getData();
                    PGPSecretKeyRing newKeyRing = (PGPSecretKeyRing) PgpConvert
                            .BytesToPGPKeyRing(data
                                    .getByteArray(KeyIntentService.RESULT_NEW_KEY));

                    boolean isMasterKey = (mEditors.getChildCount() == 0);

                    // take only the key from this ring
                    PGPSecretKey newKey = null;
                    @SuppressWarnings("unchecked")
                    Iterator<PGPSecretKey> it = newKeyRing.getSecretKeys();

                    if (isMasterKey) {
                        newKey = it.next();
                    } else {
                        // first one is the master key
                        it.next();
                        newKey = it.next();
                    }

                    // add view with new key
                    KeyEditor view = (KeyEditor) mInflater.inflate(R.layout.edit_key_key_item,
                            mEditors, false);
                    view.setEditorListener(SectionView.this);
                    view.setValue(newKey, isMasterKey, -1);
                    mEditors.addView(view);
                    SectionView.this.updateEditorsVisible();
                }
            };
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeyIntentService.EXTRA_MESSENGER, messenger);

        mGeneratingDialog.show(mActivity.getSupportFragmentManager(), "dialog");

        // start service with intent
        mActivity.startService(intent);
    }
}

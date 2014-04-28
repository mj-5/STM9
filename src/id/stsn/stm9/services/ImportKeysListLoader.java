package id.stsn.stm9.services;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPUtil;

import id.stsn.stm9.utility.InputData;
import id.stsn.stm9.utility.PositionAwareInputStream;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

public class ImportKeysListLoader extends AsyncTaskLoader<List<ImportKeysListEntry>> {
    Context mContext;

    InputData mInputData;

    ArrayList<ImportKeysListEntry> data = new ArrayList<ImportKeysListEntry>();

    public ImportKeysListLoader(Context context, InputData inputData) {
        super(context);
        this.mContext = context;
        this.mInputData = inputData;
    }

    @Override
    public List<ImportKeysListEntry> loadInBackground() {
        if (mInputData == null) {
            Log.e("stm-9", "Input data is null!");
            return data;
        }

        generateListOfKeyrings(mInputData);

        return data;
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void deliverResult(List<ImportKeysListEntry> data) {
        super.deliverResult(data);
    }

    /**
     * Reads all PGPKeyRing objects from input
     * 
     * @param keyringBytes
     * @return
     */
    private void generateListOfKeyrings(InputData inputData) {
        PositionAwareInputStream progressIn = new PositionAwareInputStream(
                inputData.getInputStream());

        // need to have access to the bufferedInput, so we can reuse it for the possible
        // PGPObject chunks after the first one, e.g. files with several consecutive ASCII
        // armour blocks
        BufferedInputStream bufferedInput = new BufferedInputStream(progressIn);
        try {

            // read all available blocks... (asc files can contain many blocks with BEGIN END)
            while (bufferedInput.available() > 0) {
                InputStream in = PGPUtil.getDecoderStream(bufferedInput);
                PGPObjectFactory objectFactory = new PGPObjectFactory(in);

                // go through all objects in this block
                Object obj;
                while ((obj = objectFactory.nextObject()) != null) {
                    Log.d("stm-9", "Found class: " + obj.getClass());

                    if (obj instanceof PGPKeyRing) {
                        PGPKeyRing newKeyring = (PGPKeyRing) obj;
                        addToData(newKeyring);
                    } else {
                        Log.e("stm-9", "Object not recognized as PGPKeyRing!");
                    }
                }
            }
        } catch (Exception e) {
            Log.e("stm-9", "Exception on parsing key file!", e);
        }
    }

    private void addToData(PGPKeyRing keyring) {
        ImportKeysListEntry item = new ImportKeysListEntry(keyring);
        data.add(item);
    }

}

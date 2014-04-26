package id.stsn.stm9.pgp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;

import id.stsn.stm9.R;
import id.stsn.stm9.Id;
import id.stsn.stm9.pgp.PgpGeneralException;
import id.stsn.stm9.provider.ProviderHelper;
import id.stsn.stm9.services.KeyIntentService;
import id.stsn.stm9.utility.HkpKeyServer;
import id.stsn.stm9.utility.IterableIterator;
import id.stsn.stm9.utility.PositionAwareInputStream;
import id.stsn.stm9.utility.ProgressDialogUpdater;
//import id.stsn.stm9.fragment.ImportKeysListFragment.InputData;
import id.stsn.stm9.utility.InputData;
import id.stsn.stm9.utility.KeyServer.AddKeyException;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class PgpImportExport {
    private Context mContext;
    private ProgressDialogUpdater mProgress;

    public PgpImportExport(Context context, ProgressDialogUpdater progress) {
        super();
        this.mContext = context;
        this.mProgress = progress;
    }
    
    public void updateProgress(int message, int current, int total) {
        if (mProgress != null) {
            mProgress.setProgress(message, current, total);
        }
    }
    
    public void updateProgress(int current, int total) {
        if (mProgress != null) {
            mProgress.setProgress(current, total);
        }
    }
    
    /**
     * export public key rings with or not secret keyrings
     * 
     * @param keyRingMasterKeyIds
     * @param keyType
     * @param outStream
     * @return
     * @throws PgpGeneralException
     * @throws FileNotFoundException
     * @throws PGPException
     * @throws IOException
     */
    public Bundle exportKeyRings(ArrayList<Long> keyRingMasterKeyIds, int keyType,
            OutputStream outStream) throws PgpGeneralException, FileNotFoundException,
            PGPException, IOException {
        Bundle returnData = new Bundle();

        if (keyRingMasterKeyIds.size() == 1) {
            updateProgress(R.string.proses_exporting_key, 0, 100);
        } else {
            updateProgress(R.string.proses_exporting_keys, 0, 100);
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new PgpGeneralException(
                    mContext.getString(R.string.error_external_storage_not_ready));
        }

        // export public keyrings...
        ArmoredOutputStream outPub = new ArmoredOutputStream(outStream);
        outPub.setHeader("Version", PgpHelper.getFullVersion(mContext));

        int numKeys = 0;
        for (int i = 0; i < keyRingMasterKeyIds.size(); ++i) {
            // double the needed time if exporting both public and secret parts
            if (keyType == Id.tipe.secret_key) {
                updateProgress(i * 100 / keyRingMasterKeyIds.size() / 2, 100);
            } else {
                updateProgress(i * 100 / keyRingMasterKeyIds.size(), 100);
            }

            PGPPublicKeyRing publicKeyRing = ProviderHelper.getPGPPublicKeyRingByMasterKeyId(
                    mContext, keyRingMasterKeyIds.get(i));

            if (publicKeyRing != null) {
                publicKeyRing.encode(outPub);
            }
            ++numKeys;
        }
        outPub.close();

        // if we export secret keyrings, append all secret parts after the public parts
        if (keyType == Id.tipe.secret_key) {
            ArmoredOutputStream outSec = new ArmoredOutputStream(outStream);
            outSec.setHeader("Version", PgpHelper.getFullVersion(mContext));

            for (int i = 0; i < keyRingMasterKeyIds.size(); ++i) {
                updateProgress(i * 100 / keyRingMasterKeyIds.size() / 2, 100);

                PGPSecretKeyRing secretKeyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(
                        mContext, keyRingMasterKeyIds.get(i));

                if (secretKeyRing != null) {
                    secretKeyRing.encode(outSec);
                }
            }
            outSec.close();
        }

        returnData.putInt(KeyIntentService.RESULT_EXPORT, numKeys);

        updateProgress(R.string.proses_done, 100, 100);

        return returnData;
    }
    

    /**
     * Imports keys from given data. If keyIds is given only those are imported
     * 
     * @param data
     * @param keyIds
     * @return
     * @throws PgpGeneralException
     * @throws FileNotFoundException
     * @throws PGPException
     * @throws IOException
     */
    public Bundle importKeyRings(InputData data, ArrayList<Long> keyIds)
            throws PgpGeneralException, FileNotFoundException, PGPException, IOException {
        Bundle returnData = new Bundle();

        updateProgress(R.string.proses_importing_secret_keys, 0, 100);

        PositionAwareInputStream progressIn = new PositionAwareInputStream(data.getInputStream());

        // need to have access to the bufferedInput, so we can reuse it for the possible
        // PGPObject chunks after the first one, e.g. files with several consecutive ASCII
        // armour blocks
        BufferedInputStream bufferedInput = new BufferedInputStream(progressIn);
        int newKeys = 0;
        int oldKeys = 0;
        int badKeys = 0;
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
                        PGPKeyRing keyring = (PGPKeyRing) obj;

                        int status = Integer.MIN_VALUE; // out of bounds value

                        if (keyIds != null) {
                            if (keyIds.contains(keyring.getPublicKey().getKeyID())) {
                                status = storeKeyRingInCache(keyring);
                            } else {
                                Log.d("stm-9", "not selected! key id: "
                                        + keyring.getPublicKey().getKeyID());
                            }
                        } else {
                            status = storeKeyRingInCache(keyring);
                        }

                        if (status == Id.return_value.error) {
                         throw new PgpGeneralException(
                                    mContext.getString(R.string.error_saving_keys));
                        }

                        // update the counts to display to the user at the end
                        if (status == Id.return_value.updated) {
                            ++oldKeys;
                        } else if (status == Id.return_value.ok) {
                        ++newKeys;
                        } else if (status == Id.return_value.bad) {
             ++badKeys;
                        }

                        updateProgress((int) (100 * progressIn.position() / data.getSize()), 100);
                    } else {
                        Log.e("stm-9", "Object not recognized as PGPKeyRing!");
                    }
                }
            }
        } catch (Exception e) {
            Log.e("stm-9", "Exception on parsing key file!", e);
        }

        returnData.putInt(KeyIntentService.RESULT_IMPORT_ADDED, newKeys);
        returnData.putInt(KeyIntentService.RESULT_IMPORT_UPDATED, oldKeys);
        returnData.putInt(KeyIntentService.RESULT_IMPORT_BAD, badKeys);

        updateProgress(R.string.proses_done, 100, 100);

        return returnData;
    }
    

    /**
     * TODO: implement Id.return_value.updated as status when key already existed
     * 
     * @param context
     * @param keyring
     * @return
     */
    @SuppressWarnings("unchecked")
    public int storeKeyRingInCache(PGPKeyRing keyring) {
        int status = Integer.MIN_VALUE; // out of bounds value (Id.return_value.*)
        try {
            if (keyring instanceof PGPSecretKeyRing) {
                PGPSecretKeyRing secretKeyRing = (PGPSecretKeyRing) keyring;
                boolean save = true;

                for (PGPSecretKey testSecretKey : new IterableIterator<PGPSecretKey>(
                        secretKeyRing.getSecretKeys())) {
                    if (!testSecretKey.isMasterKey()) {
                        if (PgpKeyHelper.isSecretKeyPrivateEmpty(testSecretKey)) {
                            // this is bad, something is very wrong...
                            save = false;
                            status = Id.return_value.bad;
                       }
                    }
                }

                if (save) {
                    ProviderHelper.saveKeyRing(mContext, secretKeyRing);
                    // TODO: preserve certifications
                    // (http://osdir.com/ml/encryption.bouncy-castle.devel/2007-01/msg00054.html ?)
                    PGPPublicKeyRing newPubRing = null;
                    for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(
                            secretKeyRing.getPublicKeys())) {
                        if (newPubRing == null) {
                            newPubRing = new PGPPublicKeyRing(key.getEncoded(),
                                    new JcaKeyFingerprintCalculator());
                        }
                        newPubRing = PGPPublicKeyRing.insertPublicKey(newPubRing, key);
                    }
                    if (newPubRing != null)
                        ProviderHelper.saveKeyRing(mContext, newPubRing);
                    // TODO: remove status returns, use exceptions!
                    status = Id.return_value.ok;
                }
            } else if (keyring instanceof PGPPublicKeyRing) {
                PGPPublicKeyRing publicKeyRing = (PGPPublicKeyRing) keyring;
                ProviderHelper.saveKeyRing(mContext, publicKeyRing);
                // TODO: remove status returns, use exceptions!
                status = Id.return_value.ok;
         }
        } catch (IOException e) {
            status = Id.return_value.error;
        }

        return status;
    }
    
    public boolean uploadKeyRingToServer(HkpKeyServer server, PGPPublicKeyRing keyring) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArmoredOutputStream aos = new ArmoredOutputStream(bos);
        try {
            aos.write(keyring.getEncoded());
            aos.close();

            String armouredKey = bos.toString("UTF-8");
            server.add(armouredKey);

            return true;
        } catch (IOException e) {
            return false;
        } catch (AddKeyException e) {
            // TODO: tell the user?
            return false;
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
            }
        }
    }
}

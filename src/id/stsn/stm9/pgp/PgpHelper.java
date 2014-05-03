package id.stsn.stm9.pgp;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;
import id.stsn.stm9.provider.ProviderHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPUtil;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class PgpHelper {

	public static Pattern PGP_PUBLIC_KEY = Pattern.compile(
            ".*?(-----BEGIN PGP PUBLIC KEY BLOCK-----.*?-----END PGP PUBLIC KEY BLOCK-----).*",Pattern.DOTALL);
    
    public static Pattern PGP_MESSAGE = Pattern.compile(
            ".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*", Pattern.DOTALL);

    public static Pattern PGP_SIGNED_MESSAGE = Pattern.compile(
    		".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
    		Pattern.DOTALL);

    public static String getVersion(Context context) {
        String version = null;
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo("id.stsn.stm9", 0);
            version = pi.versionName;
            return version;
        } catch (NameNotFoundException e) {
            Log.e("Stm-9", "Version could not be retrieved!", e);
            return "0.0.0";
        }
    }

    public static String getFullVersion(Context context) {
        return "STM9 v" + getVersion(context);
    }
    
    public static long getDecryptionKeyId(Context context, InputStream inputStream)
            throws PgpGeneralException, NoAsymmetricEncryptionException, IOException {
        InputStream in = PGPUtil.getDecoderStream(inputStream);
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();

        // the first object might be a PGP marker packet.
        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        if (enc == null) {
            throw new PgpGeneralException(context.getString(R.string.error_invalid_data));
        }

        // TODO: currently we always only look at the first known key
        // find the secret key
        PGPSecretKey secretKey = null;
        Iterator<?> it = enc.getEncryptedDataObjects();
        boolean gotAsymmetricEncryption = false;
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof PGPPublicKeyEncryptedData) {
                gotAsymmetricEncryption = true;
                PGPPublicKeyEncryptedData pbe = (PGPPublicKeyEncryptedData) obj;
                secretKey = ProviderHelper.getPGPSecretKeyByKeyId(context, pbe.getKeyID());
                if (secretKey != null) {
                    break;
                }
            }
        }

        if (!gotAsymmetricEncryption) {
            throw new NoAsymmetricEncryptionException();
        }

        if (secretKey == null) {
            return Id.kunci.none;
        }

        return secretKey.getKeyID();
    }

}

package id.stsn.stm9.services;

import android.content.Context;
import android.content.SharedPreferences;

import id.stsn.stm9.Id;

import java.util.Vector;

import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.openpgp.PGPEncryptedData;

/**
 * Singleton Implementation of a Preference Helper
 */
public class Preferences {
    private static Preferences mPreferences;
    private SharedPreferences mSharedPreferences;

    public static synchronized Preferences getPreferences(Context context) {
        return getPreferences(context, false);
    }

    public static synchronized Preferences getPreferences(Context context, boolean force_new) {
        if (mPreferences == null || force_new) {
            mPreferences = new Preferences(context);
        }
        return mPreferences;
    }

    private Preferences(Context context) {
        mSharedPreferences = context.getSharedPreferences("APG.main", Context.MODE_PRIVATE);
    }

    public String[] getKeyServers() {
        String rawData = mSharedPreferences.getString("keyServers", "pgp.mit.edu");
        Vector<String> servers = new Vector<String>();
        String chunks[] = rawData.split(",");
        for (int i = 0; i < chunks.length; ++i) {
            String tmp = chunks[i].trim();
            if (tmp.length() > 0) {
                servers.add(tmp);
            }
        }
        return servers.toArray(chunks);
    }
    
    public int getDefaultEncryptionAlgorithm() {
        return mSharedPreferences.getInt("defaultEncryptionAlgorithm",
                PGPEncryptedData.AES_128);
    }

    public int getDefaultHashAlgorithm() {
        return mSharedPreferences.getInt("defaultHashAlgorithm",
                HashAlgorithmTags.SHA1);
    }

    public boolean getForceV3Signatures() {
        return mSharedPreferences.getBoolean("forceV3Signatures", false);
    }
    

    public int getDefaultMessageCompression() {
        return mSharedPreferences.getInt("defaultMessageCompression",
                Id.pilihan.compression.zip);
    }

    /**
     * bisa dihapus
     */
    public boolean getDefaultAsciiArmour() {
        return mSharedPreferences.getBoolean("defaultAsciiArmour", false);
    }

    public int getDefaultFileCompression() {
        return mSharedPreferences.getInt("defaultFileCompression",
                Id.pilihan.compression.none);
    }

}

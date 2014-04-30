package id.stsn.stm9.pgp;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Vector;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;

import android.content.Context;
import android.util.Log;

import id.stsn.stm9.R;
import id.stsn.stm9.provider.ProviderHelper;
import id.stsn.stm9.utility.IterableIterator;

public class PgpKeyHelper {

    public static Date getCreationDate(PGPPublicKey key) {
        return key.getCreationTime();
    }

    public static Date getCreationDate(PGPSecretKey key) {
        return key.getPublicKey().getCreationTime();
    }

    public static Date getExpiryDate(PGPPublicKey key) {
        Date creationDate = getCreationDate(key);
        if (key.getValidDays() == 0) {
            // no expiry
            return null;
        }
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(creationDate);
        calendar.add(Calendar.DATE, key.getValidDays());
        Date expiryDate = calendar.getTime();

        return expiryDate;
    }

    public static Date getExpiryDate(PGPSecretKey key) {
        return getExpiryDate(key.getPublicKey());
    }

    @SuppressWarnings("unchecked")
    public static boolean isEncryptionKey(PGPPublicKey key) {
        if (!key.isEncryptionKey()) {
            return false;
        }

        if (key.getVersion() <= 3) {
            // this must be true now
            return key.isEncryptionKey();
        }

        // special cases
        if (key.getAlgorithm() == PGPPublicKey.RSA_ENCRYPT) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignatures())) {
            if (key.isMasterKey() && sig.getKeyID() != key.getKeyID()) {
                continue;
            }
            
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();
            /* hashed */
            if (hashed != null && (hashed.getKeyFlags() & (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0) {
                return true;
            }
            /* unhashed */
            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null && (unhashed.getKeyFlags() & (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEncryptionKey(PGPSecretKey key) {
        return isEncryptionKey(key.getPublicKey());
    }

    @SuppressWarnings("unchecked")
    public static boolean isSigningKey(PGPPublicKey key) {
        if (key.getVersion() <= 3) {
            return true;
        }

        // special case
        if (key.getAlgorithm() == PGPPublicKey.RSA_SIGN) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignatures())) {
            if (key.isMasterKey() && sig.getKeyID() != key.getKeyID()) {
                continue;
            }
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();
            /* hashed */
            if (hashed != null && (hashed.getKeyFlags() & KeyFlags.SIGN_DATA) != 0) {
                return true;
            }
            /* unhashed */
            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null && (unhashed.getKeyFlags() & KeyFlags.SIGN_DATA) != 0) {
                return true;
            }
        }

        return false;
    }

    public static boolean isSigningKey(PGPSecretKey key) {
        return isSigningKey(key.getPublicKey());
    }

    public static String getAlgorithmInfo(PGPPublicKey key) {
        return getAlgorithmInfo(key.getAlgorithm(), key.getBitStrength());
    }

    public static String getAlgorithmInfo(PGPSecretKey key) {
        return getAlgorithmInfo(key.getPublicKey());
    }

    public static String getAlgorithmInfo(int algorithm, int keySize) {
        String algorithmStr = null;

        switch (algorithm) {
        case PGPPublicKey.RSA_ENCRYPT:
        case PGPPublicKey.RSA_GENERAL:
        case PGPPublicKey.RSA_SIGN: {
            algorithmStr = "RSA";
            break;
        }
        
        default: {
            algorithmStr = "???";
            break;
        }
        }
        return algorithmStr + ", " + keySize + "bit";
    }

    public static String convertKeyIdToHex(long keyId) {
        String fingerPrint = Long.toHexString(keyId & 0xffffffffL).toUpperCase(Locale.US);
        while (fingerPrint.length() < 8) {
            fingerPrint = "0" + fingerPrint;
        }
        return fingerPrint;
    }
    
    public static boolean isSecretKeyPrivateEmpty(PGPSecretKey secretKey) {
        return secretKey.isPrivateKeyEmpty();
    }
    
    @SuppressWarnings("unchecked")
    public static boolean isCertificationKey(PGPPublicKey key) {
        if (key.getVersion() <= 3) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignatures())) {
            if (key.isMasterKey() && sig.getKeyID() != key.getKeyID()) {
                continue;
            }
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

            if (hashed != null && (hashed.getKeyFlags() & KeyFlags.CERTIFY_OTHER) != 0) {
                return true;
            }

            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null && (unhashed.getKeyFlags() & KeyFlags.CERTIFY_OTHER) != 0) {
                return true;
            }
        }

        return false;
    }

    public static boolean isCertificationKey(PGPSecretKey key) {
        return isCertificationKey(key.getPublicKey());
    }

    public static String convertKeyToHex(long keyId) {
        return convertKeyIdToHex(keyId >> 32) + convertKeyIdToHex(keyId);
    }
    
    public static long convertHexToKeyId(String data) {
        int len = data.length();
        String s2 = data.substring(len - 8);
        String s1 = data.substring(0, len - 8);
        return (Long.parseLong(s1, 16) << 32) | Long.parseLong(s2, 16);
    }
    
    public static String convertFingerprintToHex(byte[] fp) {
    	String fingerPrint = "";
    	for (int i = 0; i < fp.length; ++i) {
    		if (i != 0 && i % 10 == 0) {
    			fingerPrint += "  ";
    		} else if (i != 0 && i % 2 == 0) {
    			fingerPrint += " ";
    		}
    		String chunk = Integer.toHexString((fp[i] + 256) % 256).toUpperCase(Locale.US);
    		while (chunk.length() < 2) {
    			chunk = "0" + chunk;
    		}
    		fingerPrint += chunk;
    	}

    	return fingerPrint;
    }
    
    public static String getFingerPrint(Context context, long keyId) {
        PGPPublicKey key = ProviderHelper.getPGPPublicKeyByKeyId(context, keyId);
        // if it is no public key get it from your own keys...
        if (key == null) {
            PGPSecretKey secretKey = ProviderHelper.getPGPSecretKeyByKeyId(context, keyId);
            if (secretKey == null) {
                Log.e("Stm-9", "Key could not be found!");
                return null;
            }
            key = secretKey.getPublicKey();
        }

        return convertFingerprintToHex(key.getFingerprint());
    }
    

    @SuppressWarnings("unchecked")
    public static PGPSecretKey getMasterKey(PGPSecretKeyRing keyRing) {
        if (keyRing == null) {
            return null;
        }
        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
            if (key.isMasterKey()) {
                return key;
            }
        }

        return null;
    }
    

    public static Vector<PGPSecretKey> getUsableSigningKeys(PGPSecretKeyRing keyRing) {
        Vector<PGPSecretKey> usableKeys = new Vector<PGPSecretKey>();
        Vector<PGPSecretKey> signingKeys = getSigningKeys(keyRing);
        PGPSecretKey masterKey = null;
        for (int i = 0; i < signingKeys.size(); ++i) {
            PGPSecretKey key = signingKeys.get(i);
            if (key.isMasterKey()) {
                masterKey = key;
            } else {
                usableKeys.add(key);
            }
        }
        if (masterKey != null) {
            usableKeys.add(masterKey);
        }
        return usableKeys;
    }
    

    @SuppressWarnings("unchecked")
    public static Vector<PGPSecretKey> getSigningKeys(PGPSecretKeyRing keyRing) {
        Vector<PGPSecretKey> signingKeys = new Vector<PGPSecretKey>();

        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
            if (isSigningKey(key)) {
                signingKeys.add(key);
            }
        }

        return signingKeys;
    }
    
    @SuppressWarnings("unchecked")
    public static PGPPublicKey getMasterKey(PGPPublicKeyRing keyRing) {
        if (keyRing == null) {
            return null;
        }
        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
            if (key.isMasterKey()) {
                return key;
            }
        }

        return null;
    }
    

    public static Vector<PGPPublicKey> getUsableEncryptKeys(PGPPublicKeyRing keyRing) {
        Vector<PGPPublicKey> usableKeys = new Vector<PGPPublicKey>();
        Vector<PGPPublicKey> encryptKeys = getEncryptKeys(keyRing);
        PGPPublicKey masterKey = null;
        for (int i = 0; i < encryptKeys.size(); ++i) {
            PGPPublicKey key = encryptKeys.get(i);
            if (!isExpired(key) && !key.isRevoked()) {
                if (key.isMasterKey()) {
                    masterKey = key;
                } else {
                    usableKeys.add(key);
                }
            }
        }
        if (masterKey != null) {
            usableKeys.add(masterKey);
        }
        return usableKeys;
    }

    @SuppressWarnings("unchecked")
    public static Vector<PGPPublicKey> getEncryptKeys(PGPPublicKeyRing keyRing) {
        Vector<PGPPublicKey> encryptKeys = new Vector<PGPPublicKey>();

        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
            if (isEncryptionKey(key)) {
                encryptKeys.add(key);
            }
        }

        return encryptKeys;
    }

    public static boolean isExpired(PGPPublicKey key) {
        Date creationDate = getCreationDate(key);
        Date expiryDate = getExpiryDate(key);
        Date now = new Date();
        if (now.compareTo(creationDate) >= 0
                && (expiryDate == null || now.compareTo(expiryDate) <= 0)) {
            return false;
        }
        return true;
    }

    public static PGPSecretKey getSigningKey(Context context, long masterKeyId) {
        PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(context,
                masterKeyId);
        if (keyRing == null) {
            return null;
        }
        Vector<PGPSecretKey> signingKeys = getUsableSigningKeys(keyRing);
        if (signingKeys.size() == 0) {
            return null;
        }
        return signingKeys.get(0);
    }
    
    public static PGPPublicKey getEncryptPublicKey(Context context, long masterKeyId) {
        PGPPublicKeyRing keyRing = ProviderHelper.getPGPPublicKeyRingByMasterKeyId(context,
                masterKeyId);
        if (keyRing == null) {
            Log.e("Stm-9", "keyRing is null!");
            return null;
        }
        Vector<PGPPublicKey> encryptKeys = getUsableEncryptKeys(keyRing);
        if (encryptKeys.size() == 0) {
            Log.e("Stm-9", "encryptKeys is null!");
            return null;
        }
        return encryptKeys.get(0);
    }
    
    @SuppressWarnings("unchecked")
    public static String getMainUserId(PGPSecretKey key) {
        for (String userId : new IterableIterator<String>(key.getUserIDs())) {
            return userId;
        }
        return null;
    }

    public static String getMainUserIdSafe(Context context, PGPSecretKey key) {
        String userId = getMainUserId(key);
        if (userId == null || userId.equals("")) {
            userId = context.getString(R.string.unknown_user_id);
        }
        return userId;
    }
    

    @SuppressWarnings("unchecked")
    public static PGPSecretKey getKeyNum(PGPSecretKeyRing keyRing, long num) {
        long cnt = 0;
        if (keyRing == null) {
            return null;
        }
        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
            if (cnt == num) {
                return key;
            }
            cnt++;
        }

        return null;
    }
}

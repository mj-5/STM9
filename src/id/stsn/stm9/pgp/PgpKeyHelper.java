package id.stsn.stm9.pgp;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;

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
}

package id.stsn.stm9.services;

import id.stsn.stm9.pgp.PgpKeyHelper;
import id.stsn.stm9.utility.IterableIterator;

import java.io.Serializable;
import java.util.ArrayList;

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;

public class ImportKeysListEntry implements Serializable {
    private static final long serialVersionUID = -7797972103284992662L;
    public ArrayList<String> userIds;
    public long keyId;
    public boolean revoked;
    public String fingerPrint;
    public String hexKeyId;
    public int bitStrength;
    public String algorithm;
    public boolean secretKey;
    private boolean selected;

    /**
     * Constructor for later querying from keyserver
     */
    public ImportKeysListEntry() {
        secretKey = false;
        userIds = new ArrayList<String>();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    /**
     * Constructor based on key object, used for import from NFC, QR Codes, files
     * 
     * @param pgpKey
     */
    @SuppressWarnings("unchecked")
    public ImportKeysListEntry(PGPKeyRing pgpKeyRing) {
        // selected is default
        this.selected = true;

        if (pgpKeyRing instanceof PGPSecretKeyRing) {
            secretKey = true;
        } else {
            secretKey = false;
        }

        userIds = new ArrayList<String>();
        for (String userId : new IterableIterator<String>(pgpKeyRing.getPublicKey().getUserIDs())) {
            userIds.add(userId);
        }
        this.keyId = pgpKeyRing.getPublicKey().getKeyID();

        this.revoked = pgpKeyRing.getPublicKey().isRevoked();
        this.fingerPrint = PgpKeyHelper.convertFingerprintToHex(pgpKeyRing.getPublicKey()
                .getFingerprint());
        this.hexKeyId = PgpKeyHelper.convertKeyIdToHex(keyId);
        this.bitStrength = pgpKeyRing.getPublicKey().getBitStrength();
        int algorithm = pgpKeyRing.getPublicKey().getAlgorithm();
        if (algorithm == PGPPublicKey.RSA_ENCRYPT || algorithm == PGPPublicKey.RSA_GENERAL
                || algorithm == PGPPublicKey.RSA_SIGN) {
            this.algorithm = "RSA";
        } else if (algorithm == PGPPublicKey.DSA) {
            this.algorithm = "DSA";
        } else if (algorithm == PGPPublicKey.ELGAMAL_ENCRYPT
                || algorithm == PGPPublicKey.ELGAMAL_GENERAL) {
            this.algorithm = "ElGamal";
        } else if (algorithm == PGPPublicKey.EC || algorithm == PGPPublicKey.ECDSA) {
            this.algorithm = "ECC";
        } else {
            // TODO: with resources
            this.algorithm = "unknown";
        }
    }
}
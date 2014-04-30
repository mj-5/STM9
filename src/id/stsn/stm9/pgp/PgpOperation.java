package id.stsn.stm9.pgp;

import id.stsn.stm9.Id;
import id.stsn.stm9.R;
import id.stsn.stm9.provider.ProviderHelper;
import id.stsn.stm9.utility.InputData;
import id.stsn.stm9.utility.ProgressDialogUpdater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Date;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.BCPGOutputStream;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

import android.content.Context;
import android.util.Log;

public class PgpOperation {
    private Context mContext;
    private ProgressDialogUpdater mProgress;
    private InputData mData;
    private OutputStream mOutStream;

    public PgpOperation(Context context, ProgressDialogUpdater progress, InputData data,
            OutputStream outStream) {
        super();
        this.mContext = context;
        this.mProgress = progress;
        this.mData = data;
        this.mOutStream = outStream;
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

    public void signAndEncrypt(boolean useAsciiArmor, int compression, long[] encryptionKeyIds,
            String encryptionPassphrase, int symmetricEncryptionAlgorithm, long signatureKeyId,
            int signatureHashAlgorithm, boolean signatureForceV3, String signaturePassphrase)
            throws IOException, PgpGeneralException, PGPException, NoSuchProviderException,
            NoSuchAlgorithmException, SignatureException {

        if (encryptionKeyIds == null) {
            encryptionKeyIds = new long[0];
        }

        ArmoredOutputStream armorOut = null;
        OutputStream out = null;
        OutputStream encryptOut = null;
        if (useAsciiArmor) {
            armorOut = new ArmoredOutputStream(mOutStream);
            armorOut.setHeader("Version", PgpHelper.getFullVersion(mContext));
            out = armorOut;
        } else {
            out = mOutStream;
        }
        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;

        if (encryptionKeyIds.length == 0 && encryptionPassphrase == null) {
            throw new PgpGeneralException(
                    mContext.getString(R.string.error_no_encryption_keys_or_passphrase));
        }

        if (signatureKeyId != Id.kunci.none) {
            signingKeyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(mContext, signatureKeyId);
            signingKey = PgpKeyHelper.getSigningKey(mContext, signatureKeyId);
            if (signingKey == null) {
                throw new PgpGeneralException(mContext.getString(R.string.error_signature_failed));
            }

            if (signaturePassphrase == null) {
                throw new PgpGeneralException(
                        mContext.getString(R.string.error_no_signature_passphrase));
            }

            updateProgress(R.string.proses_extracting_signature_key, 0, 100);

            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                    BouncyCastleProvider.PROVIDER_NAME).build(signaturePassphrase.toCharArray());
            signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
            if (signaturePrivateKey == null) {
                throw new PgpGeneralException(
                        mContext.getString(R.string.error_could_not_extract_private_key));
            }
        }
        updateProgress(R.string.proses_preparing_streams, 5, 100);

        // encrypt and compress input file content
        JcePGPDataEncryptorBuilder encryptorBuilder = new JcePGPDataEncryptorBuilder(
                symmetricEncryptionAlgorithm).setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .setWithIntegrityPacket(true);

        PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(encryptorBuilder);

            // Asymmetric encryption
            for (long id : encryptionKeyIds) {
                PGPPublicKey key = PgpKeyHelper.getEncryptPublicKey(mContext, id);
                if (key != null) {

                    JcePublicKeyKeyEncryptionMethodGenerator pubKeyEncryptionGenerator = new JcePublicKeyKeyEncryptionMethodGenerator(
                            key);
                    cPk.addMethod(pubKeyEncryptionGenerator);
            }
        }
        encryptOut = cPk.open(out, new byte[1 << 16]);

        PGPSignatureGenerator signatureGenerator = null;

        if (signatureKeyId != Id.kunci.none) {
            updateProgress(R.string.proses_preparing_signature, 10, 100);

            // content signer based on signing key algorithm and choosen hash algorithm
            JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(
                    signingKey.getPublicKey().getAlgorithm(), signatureHashAlgorithm)
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);

                signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
                signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, signaturePrivateKey);

                String userId = PgpKeyHelper.getMainUserId(PgpKeyHelper
                        .getMasterKey(signingKeyRing));
                PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
                spGen.setSignerUserID(false, userId);
                signatureGenerator.setHashedSubpackets(spGen.generate());
        }

        PGPCompressedDataGenerator compressGen = null;
        BCPGOutputStream bcpgOut = null;
        
        // compression with zip
        compressGen = new PGPCompressedDataGenerator(compression);
        bcpgOut = new BCPGOutputStream(compressGen.open(encryptOut));

        if (signatureKeyId != Id.kunci.none) {
            
                signatureGenerator.generateOnePassVersion(false).encode(bcpgOut);
        }

        PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
        // file name not needed, so empty string
        OutputStream pOut = literalGen.open(bcpgOut, PGPLiteralData.BINARY, "", new Date(),
                new byte[1 << 16]);
        updateProgress(R.string.proses_encrypting, 20, 100);

        long done = 0;
        int n = 0;
        byte[] buffer = new byte[1 << 16];
        InputStream in = mData.getInputStream();
        while ((n = in.read(buffer)) > 0) {
            pOut.write(buffer, 0, n);
            if (signatureKeyId != Id.kunci.none) {
                    signatureGenerator.update(buffer, 0, n);
            }
            done += n;
            if (mData.getSize() != 0) {
                updateProgress((int) (20 + (95 - 20) * done / mData.getSize()), 100);
            }
        }

        literalGen.close();

        if (signatureKeyId != Id.kunci.none) {
            updateProgress(R.string.proses_generating_signature, 95, 100);
                signatureGenerator.generate().encode(pOut);
        }
        if (compressGen != null) {
            compressGen.close();
        }
        encryptOut.close();
        if (useAsciiArmor) {
            armorOut.close();
        }

        updateProgress(R.string.proses_done, 100, 100);
    }

    public void signText(long signatureKeyId, String signaturePassphrase,
            int signatureHashAlgorithm, boolean forceV3Signature) throws PgpGeneralException,
            PGPException, IOException, NoSuchAlgorithmException, SignatureException {

        ArmoredOutputStream armorOut = new ArmoredOutputStream(mOutStream);
        armorOut.setHeader("Version", PgpHelper.getFullVersion(mContext));

        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;

        if (signatureKeyId == 0) {
            armorOut.close();
            throw new PgpGeneralException(mContext.getString(R.string.error_no_signature_key));
        }

        signingKeyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(mContext, signatureKeyId);
        signingKey = PgpKeyHelper.getSigningKey(mContext, signatureKeyId);
        if (signingKey == null) {
            armorOut.close();
            throw new PgpGeneralException(mContext.getString(R.string.error_signature_failed));
        }

        if (signaturePassphrase == null) {
            armorOut.close();
            throw new PgpGeneralException(mContext.getString(R.string.error_no_signature_passphrase));
        }
        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                BouncyCastleProvider.PROVIDER_NAME).build(signaturePassphrase.toCharArray());
        signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
        if (signaturePrivateKey == null) {
            armorOut.close();
            throw new PgpGeneralException(
                    mContext.getString(R.string.error_could_not_extract_private_key));
        }
        updateProgress(R.string.proses_preparing_streams, 0, 100);

        updateProgress(R.string.proses_preparing_signature, 30, 100);

        PGPSignatureGenerator signatureGenerator = null;

        // content signer based on signing key algorithm and choosen hash algorithm
        JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(signingKey
                .getPublicKey().getAlgorithm(), signatureHashAlgorithm)
                .setProvider(BouncyCastleProvider.PROVIDER_NAME);

            signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
            signatureGenerator.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, signaturePrivateKey);

            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            String userId = PgpKeyHelper.getMainUserId(PgpKeyHelper.getMasterKey(signingKeyRing));
            spGen.setSignerUserID(false, userId);
            signatureGenerator.setHashedSubpackets(spGen.generate());

        updateProgress(R.string.proses_signing, 40, 100);

        armorOut.beginClearText(signatureHashAlgorithm);

        InputStream inStream = mData.getInputStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));

        final byte[] newline = "\r\n".getBytes("UTF-8");

            processLine(reader.readLine(), armorOut, signatureGenerator);

        while (true) {
            final String line = reader.readLine();

            if (line == null) {
                armorOut.write(newline);
                break;
            }

            armorOut.write(newline);
                signatureGenerator.update(newline);
                processLine(line, armorOut, signatureGenerator);
        }

        armorOut.endClearText();

        BCPGOutputStream bOut = new BCPGOutputStream(armorOut);
            signatureGenerator.generate().encode(bOut);

            armorOut.close();

        updateProgress(R.string.proses_done, 100, 100);
    }

    public void generateSignature(boolean armored, boolean binary, long signatureKeyId,
            String signaturePassPhrase, int hashAlgorithm, boolean forceV3Signature)
            throws PgpGeneralException, PGPException, IOException, NoSuchAlgorithmException,
            SignatureException {

        OutputStream out = null;

        // Ascii Armor (Base64)
        ArmoredOutputStream armorOut = null;
        if (armored) {
            armorOut = new ArmoredOutputStream(mOutStream);
            armorOut.setHeader("Version", PgpHelper.getFullVersion(mContext));
            out = armorOut;
        } else {
            out = mOutStream;
        }

        PGPSecretKey signingKey = null;
        PGPSecretKeyRing signingKeyRing = null;
        PGPPrivateKey signaturePrivateKey = null;

        if (signatureKeyId == 0) {
            throw new PgpGeneralException(mContext.getString(R.string.error_no_signature_key));
        }

        signingKeyRing = ProviderHelper.getPGPSecretKeyRingByKeyId(mContext, signatureKeyId);
        signingKey = PgpKeyHelper.getSigningKey(mContext, signatureKeyId);
        if (signingKey == null) {
            throw new PgpGeneralException(mContext.getString(R.string.error_signature_failed));
        }

        if (signaturePassPhrase == null) {
            throw new PgpGeneralException(mContext.getString(R.string.error_no_signature_passphrase));
        }

        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                BouncyCastleProvider.PROVIDER_NAME).build(signaturePassPhrase.toCharArray());
        signaturePrivateKey = signingKey.extractPrivateKey(keyDecryptor);
        if (signaturePrivateKey == null) {
            throw new PgpGeneralException(
                    mContext.getString(R.string.error_could_not_extract_private_key));
        }
        updateProgress(R.string.proses_preparing_streams, 0, 100);

        updateProgress(R.string.proses_preparing_signature, 30, 100);

        PGPSignatureGenerator signatureGenerator = null;

        int type = PGPSignature.CANONICAL_TEXT_DOCUMENT;
        if (binary) {
            type = PGPSignature.BINARY_DOCUMENT;
        }

        // content signer based on signing key algorithm and choosen hash algorithm
        JcaPGPContentSignerBuilder contentSignerBuilder = new JcaPGPContentSignerBuilder(signingKey
                .getPublicKey().getAlgorithm(), hashAlgorithm)
                .setProvider(BouncyCastleProvider.PROVIDER_NAME);

        // signature
            signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
            signatureGenerator.init(type, signaturePrivateKey);

            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            String userId = PgpKeyHelper.getMainUserId(PgpKeyHelper.getMasterKey(signingKeyRing));
            spGen.setSignerUserID(false, userId);
            signatureGenerator.setHashedSubpackets(spGen.generate());

        updateProgress(R.string.proses_signing, 40, 100);

        InputStream inStream = mData.getInputStream();
        if (binary) {
            byte[] buffer = new byte[1 << 16];
            int n = 0;
            while ((n = inStream.read(buffer)) > 0) {
                    signatureGenerator.update(buffer, 0, n);
            }
        } else {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
            final byte[] newline = "\r\n".getBytes("UTF-8");

            while (true) {
                final String line = reader.readLine();

                if (line == null) {
                    break;
                }

                    processLine(line, null, signatureGenerator);
                    signatureGenerator.update(newline);
            }
        }

        BCPGOutputStream bOut = new BCPGOutputStream(out);
            // signature
        signatureGenerator.generate().encode(bOut);
        out.close();
        mOutStream.close();

        if (mProgress != null)
            mProgress.setProgress(R.string.proses_done, 100, 100);
    }

    private static void processLine(final String pLine, final ArmoredOutputStream pArmoredOutput,
            final PGPSignatureGenerator pSignatureGenerator) throws IOException, SignatureException {

        if (pLine == null) {
            return;
        }

        final char[] chars = pLine.toCharArray();
        int len = chars.length;

        while (len > 0) {
            if (!Character.isWhitespace(chars[len - 1])) {
                break;
            }
            len--;
        }

        final byte[] data = pLine.substring(0, len).getBytes("UTF-8");

        if (pArmoredOutput != null) {
            pArmoredOutput.write(data);
        }
        pSignatureGenerator.update(data);
    }
}

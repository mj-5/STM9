package id.stsn.stm9.pgp;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Date;

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPKeyRingGenerator;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.spongycastle.openpgp.operator.PGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.PGPDigestCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;

import id.stsn.stm9.R;
import id.stsn.stm9.Id;
import id.stsn.stm9.pgp.PgpGeneralException;
import id.stsn.stm9.provider.ProviderHelper;
import id.stsn.stm9.utility.ProgressDialogUpdater;

import android.content.Context;
import android.util.Log;

public class PgpKeyOperation {
	private Context mContext;
	
	private ProgressDialogUpdater mProgress;

	private static final int[] PREFERRED_SYMMETRIC_ALGORITHMS = new int[] { SymmetricKeyAlgorithmTags.AES_128 };
	private static final int[] PREFERRED_HASH_ALGORITHMS = new int[] { HashAlgorithmTags.SHA1 };
	private static final int[] PREFERRED_COMPRESSION_ALGORITHMS = new int[] {CompressionAlgorithmTags.ZIP };

	public PgpKeyOperation(Context context, ProgressDialogUpdater progress) {
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
	 * Creates new secret key. Returned PGPSecretKeyRing contains only one newly generated key when this key is the new masterkey.
	 * 
	 * @param algorithmChoice
	 * @param keySize
	 * @param passPhrase
	 * @param masterSecretKey
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws PGPException
	 * @throws NoSuchProviderException
	 * @throws PgpGeneralException
	 * @throws InvalidAlgorithmParameterException
	 */
	public PGPSecretKeyRing createKey(int algorithmChoice, int keySize, String passPhrase,
			PGPSecretKey masterSecretKey) throws NoSuchAlgorithmException, PGPException,
			NoSuchProviderException, PgpGeneralException, InvalidAlgorithmParameterException {

		if (keySize < 512) {
			throw new PgpGeneralException(mContext.getString(R.string.error_key_size_minimum512bit));
		}

		if (passPhrase == null) {
			passPhrase = "";
		}

		int algorithm = 0;
		KeyPairGenerator keyGen = null;

		switch (algorithmChoice) {
		case Id.pilihan.algoritma.rsa: {
			keyGen = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
			keyGen.initialize(keySize, new SecureRandom());

			algorithm = PGPPublicKey.RSA_GENERAL;
			break;
		}

		default: {
			throw new PgpGeneralException(mContext.getString(R.string.error_unknown_algorithm_choice));
		}
		}

		// create new key pair
		PGPKeyPair keyPair = new JcaPGPKeyPair(algorithm, keyGen.generateKeyPair(), new Date());

		// hashing and signing algor
		PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);

		// Build key encrypter and decrypter based on passphrase
		PBESecretKeyEncryptor keyEncryptor = new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.CAST5, sha1Calc).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(passPhrase.toCharArray());
		PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build(passPhrase.toCharArray());

		PGPKeyRingGenerator ringGen = null;
		PGPContentSignerBuilder certificationSignerBuilder = null;
		if (masterSecretKey == null) {
			certificationSignerBuilder = new JcaPGPContentSignerBuilder(keyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1);

			// build keyRing with only this one master key in it!
			ringGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, keyPair, "",sha1Calc, null, null, certificationSignerBuilder, keyEncryptor);
		} else {
			PGPPublicKey masterPublicKey = masterSecretKey.getPublicKey();
			PGPPrivateKey masterPrivateKey = masterSecretKey.extractPrivateKey(keyDecryptor);
			PGPKeyPair masterKeyPair = new PGPKeyPair(masterPublicKey, masterPrivateKey);

			certificationSignerBuilder = new JcaPGPContentSignerBuilder(masterKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1);

			// build keyRing with master key and new key as subkey (certified by masterkey)
			ringGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, masterKeyPair, "", sha1Calc, null, null, certificationSignerBuilder, keyEncryptor);
		}

		PGPSecretKeyRing secKeyRing = ringGen.generateSecretKeyRing();

		return secKeyRing;
	}

	/**
	 * change passphrase on secret key
	 * 
	 * @param keyRing
	 * @param oldPassPhrase
	 * @param newPassPhrase
	 * @throws IOException
	 * @throws PGPException
	 * @throws PGPException
	 * @throws NoSuchProviderException
	 */
	public void changeSecretKeyPassphrase(PGPSecretKeyRing keyRing, String oldPassPhrase,
			String newPassPhrase) throws IOException, PGPException, PGPException,
			NoSuchProviderException {

		updateProgress(R.string.proses_building_key, 0, 100);
		if (oldPassPhrase == null) {
			oldPassPhrase = "";
		}
		if (newPassPhrase == null) {
			newPassPhrase = "";
		}

		PGPSecretKeyRing newKeyRing = PGPSecretKeyRing.copyWithNewPassword(
				keyRing,
				new JcePBESecretKeyDecryptorBuilder(new JcaPGPDigestCalculatorProviderBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build()).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(oldPassPhrase.toCharArray()), 
				new JcePBESecretKeyEncryptorBuilder(keyRing.getSecretKey().getKeyEncryptionAlgorithm()).build(newPassPhrase.toCharArray()));

		updateProgress(R.string.proses_saving_key_ring, 50, 100);

		ProviderHelper.saveKeyRing(mContext, newKeyRing);

		updateProgress(R.string.proses_done, 100, 100);
	}
	
	/**
	 * After all param recieved, in here build operation of secret key
	 * 
	 * @param userIds
	 * @param keys
	 * @param keysUsages
	 * @param masterKeyId
	 * @param oldPassPhrase
	 * @param newPassPhrase
	 * @throws PgpGeneralException
	 * @throws NoSuchProviderException
	 * @throws PGPException
	 * @throws NoSuchAlgorithmException
	 * @throws SignatureException
	 * @throws IOException
	 */
	public void buildSecretKey(ArrayList<String> userIds, ArrayList<PGPSecretKey> keys, ArrayList<Integer> keysUsages, long masterKeyId, String oldPassPhrase,
          String newPassPhrase) throws PgpGeneralException, NoSuchProviderException, PGPException, NoSuchAlgorithmException, SignatureException, IOException {

      Log.d("stm-9", "userIds: " + userIds.toString());

      updateProgress(R.string.proses_building_key, 0, 100);

      if (oldPassPhrase == null) {
          oldPassPhrase = "";
      }
      if (newPassPhrase == null) {
          newPassPhrase = "";
      }

      updateProgress(R.string.proses_preparing_master_key, 10, 100);

      int usageId = keysUsages.get(0);
      boolean canSign = (usageId == Id.pilihan.usage.sign_only || usageId == Id.pilihan.usage.sign_and_encrypt);
      boolean canEncrypt = (usageId == Id.pilihan.usage.encrypt_only || usageId == Id.pilihan.usage.sign_and_encrypt);

      String mainUserId = userIds.get(0);

      PGPSecretKey masterKey = keys.get(0);

      // this removes all userIds and certifications previously attached to the masterPublicKey
      PGPPublicKey tmpKey = masterKey.getPublicKey();
      @SuppressWarnings("deprecation")
	PGPPublicKey masterPublicKey = new PGPPublicKey(tmpKey.getAlgorithm(), tmpKey.getKey(new BouncyCastleProvider()), tmpKey.getCreationTime());

      PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build(oldPassPhrase.toCharArray());
      PGPPrivateKey masterPrivateKey = masterKey.extractPrivateKey(keyDecryptor);

      updateProgress(R.string.proses_certifying_master_key, 20, 100);

      for (String userId : userIds) {
          PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                  masterPublicKey.getAlgorithm(), HashAlgorithmTags.SHA1)
                  .setProvider(BouncyCastleProvider.PROVIDER_NAME);
          PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);

          sGen.init(PGPSignature.POSITIVE_CERTIFICATION, masterPrivateKey);

          PGPSignature certification = sGen.generateCertification(userId, masterPublicKey);

          masterPublicKey = PGPPublicKey.addCertification(masterPublicKey, userId, certification);
      }

      PGPKeyPair masterKeyPair = new PGPKeyPair(masterPublicKey, masterPrivateKey);

      PGPSignatureSubpacketGenerator hashedPacketsGen = new PGPSignatureSubpacketGenerator();
      PGPSignatureSubpacketGenerator unhashedPacketsGen = new PGPSignatureSubpacketGenerator();

      int keyFlags = KeyFlags.CERTIFY_OTHER | KeyFlags.SIGN_DATA;
      if (canEncrypt) {
          keyFlags |= KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE;
      }
      hashedPacketsGen.setKeyFlags(true, keyFlags);

      hashedPacketsGen.setPreferredSymmetricAlgorithms(true, PREFERRED_SYMMETRIC_ALGORITHMS);
      hashedPacketsGen.setPreferredHashAlgorithms(true, PREFERRED_HASH_ALGORITHMS);
      hashedPacketsGen.setPreferredCompressionAlgorithms(true, PREFERRED_COMPRESSION_ALGORITHMS);

      updateProgress(R.string.proses_building_key, 30, 100);

      // define hashing and signing algor
      PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);
      PGPContentSignerBuilder certificationSignerBuilder = new JcaPGPContentSignerBuilder(masterKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1);

      // Build key encrypter based on passphrase
      PBESecretKeyEncryptor keyEncryptor = new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_128, sha1Calc)
              .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(newPassPhrase.toCharArray());

      PGPKeyRingGenerator keyGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION,
              masterKeyPair, mainUserId, sha1Calc, hashedPacketsGen.generate(),
              unhashedPacketsGen.generate(), certificationSignerBuilder, keyEncryptor);

      updateProgress(R.string.proses_adding_sub_keys, 40, 100);

      for (int i = 1; i < keys.size(); ++i) {
          updateProgress(40 + 50 * (i - 1) / (keys.size() - 1), 100);

          PGPSecretKey subKey = keys.get(i);
          PGPPublicKey subPublicKey = subKey.getPublicKey();

          PBESecretKeyDecryptor keyDecryptor2 = new JcePBESecretKeyDecryptorBuilder()
                  .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(
                          oldPassPhrase.toCharArray());
          PGPPrivateKey subPrivateKey = subKey.extractPrivateKey(keyDecryptor2);

          PGPKeyPair subKeyPair = new PGPKeyPair(subPublicKey, subPrivateKey);

          hashedPacketsGen = new PGPSignatureSubpacketGenerator();
          unhashedPacketsGen = new PGPSignatureSubpacketGenerator();

          keyFlags = 0;

          usageId = keysUsages.get(i);
          canSign = (usageId == Id.pilihan.usage.sign_only || usageId == Id.pilihan.usage.sign_and_encrypt);
          canEncrypt = (usageId == Id.pilihan.usage.encrypt_only || usageId == Id.pilihan.usage.sign_and_encrypt);
          if (canSign) {
              keyFlags |= KeyFlags.SIGN_DATA;
          }
          if (canEncrypt) {
              keyFlags |= KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE;
          }
          
          hashedPacketsGen.setKeyFlags(true, keyFlags);

          keyGen.addSubKey(subKeyPair, hashedPacketsGen.generate(), unhashedPacketsGen.generate());
      }

      PGPSecretKeyRing secretKeyRing = keyGen.generateSecretKeyRing();
      PGPPublicKeyRing publicKeyRing = keyGen.generatePublicKeyRing();

      updateProgress(R.string.proses_saving_key_ring, 90, 100);

      ProviderHelper.saveKeyRing(mContext, secretKeyRing);
      ProviderHelper.saveKeyRing(mContext, publicKeyRing);

      updateProgress(R.string.proses_done, 100, 100);
  }
}

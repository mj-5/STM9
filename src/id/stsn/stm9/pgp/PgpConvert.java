package id.stsn.stm9.pgp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;

import android.util.Log;


public class PgpConvert {

	/**
	 * Convert from byte[] to PGPKeyRing
	 * 
	 * @param keysBytes
	 * @return
	 */
	public static PGPKeyRing BytesToPGPKeyRing(byte[] keysBytes) {
		PGPObjectFactory factory = new PGPObjectFactory(keysBytes);
		PGPKeyRing keyRing = null;
		try {
			if ((keyRing = (PGPKeyRing) factory.nextObject()) == null) {
				Log.e("Stm-9", "No keys given!");
			}
		} catch (IOException e) {
			Log.e("Stm-9", "Error while converting to PGPKeyRing!", e);
		}

		return keyRing;
	}

	/**
	 * Convert from ArrayList<PGPSecretKey> to byte[]
	 * 
	 * @param keys
	 * @return
	 */
	public static byte[] PGPSecretKeyArrayListToBytes(ArrayList<PGPSecretKey> keys) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		for (PGPSecretKey key : keys) {
			try {
				key.encode(os);
			} catch (IOException e) {
				Log.e("Stm-9", "Error while converting ArrayList<PGPSecretKey> to byte[]!", e);
			}
		}

		return os.toByteArray();
	}

	/**
	 * Convert from byte[] to ArrayList<PGPSecretKey>
	 * 
	 * @param keysBytes
	 * @return
	 */
	public static ArrayList<PGPSecretKey> BytesToPGPSecretKeyList(byte[] keysBytes) {
		PGPSecretKeyRing keyRing = (PGPSecretKeyRing) BytesToPGPKeyRing(keysBytes);
		ArrayList<PGPSecretKey> keys = new ArrayList<PGPSecretKey>();

		@SuppressWarnings("unchecked")
		Iterator<PGPSecretKey> itr = keyRing.getSecretKeys();
		while (itr.hasNext()) {
			keys.add(itr.next());
		}

		return keys;
	}

	/**
	 * Convert from PGPSecretKeyRing to byte[]
	 * 
	 * @param keysBytes
	 * @return
	 */
	public static byte[] PGPSecretKeyRingToBytes(PGPSecretKeyRing keyRing) {
		try {
			return keyRing.getEncoded();
		} catch (IOException e) {
			Log.e("Stm-9", "Encoding failed", e);

			return null;
		}
	}

}

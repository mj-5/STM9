package id.stsn.stm9.pgp;

import id.stsn.stm9.R;
import id.stsn.stm9.utility.ProgressDialogUpdater;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class PgpHelper {

    public static Pattern PGP_PUBLIC_KEY = Pattern.compile(
            ".*?(-----BEGIN PGP PUBLIC KEY BLOCK-----.*?-----END PGP PUBLIC KEY BLOCK-----).*",
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
    
//    /**
//     * Deletes file securely by overwriting it with random data before deleting it.
//     * 
//     * TODO: Does this really help on flash storage?
//     * 
//     * @param context
//     * @param progress
//     * @param file
//     * @throws FileNotFoundException
//     * @throws IOException
//     */
//    public static void deleteFileSecurely(Context context, ProgressDialogUpdater progress, File file)
//            throws FileNotFoundException, IOException {
//        long length = file.length();
//        SecureRandom random = new SecureRandom();
//        RandomAccessFile raf = new RandomAccessFile(file, "rws");
//        raf.seek(0);
//        raf.getFilePointer();
//        byte[] data = new byte[1 << 16];
//        int pos = 0;
//        String msg = context.getString(R.string.proses_deleting_securely, file.getName());
//        while (pos < length) {
//            if (progress != null)
//                progress.setProgress(msg, (int) (100 * pos / length), 100);
//            random.nextBytes(data);
//            raf.write(data);
//            pos += data.length;
//        }
//        raf.close();
//        file.delete();
//    }
}

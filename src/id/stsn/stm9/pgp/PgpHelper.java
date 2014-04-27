package id.stsn.stm9.pgp;

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
}

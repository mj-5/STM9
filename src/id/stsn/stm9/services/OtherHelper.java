package id.stsn.stm9.services;

import java.util.Iterator;
import java.util.Set;

import id.stsn.stm9.BuildConfig;

import android.os.Bundle;
import android.util.Log;

public class OtherHelper {
	
	public static final boolean DEBUG = BuildConfig.DEBUG;

    /**
     * Logs bundle content to debug for inspecting the content
     * 
     * @param bundle
     * @param bundleName
     */
    public static void logDebugBundle(Bundle bundle, String bundleName) {
        if (DEBUG) {
            if (bundle != null) {
                Set<String> ks = bundle.keySet();
                Iterator<String> iterator = ks.iterator();

                Log.d("stm-9", "Bundle " + bundleName + ":");
                Log.d("stm-9", "------------------------------");
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    Object value = bundle.get(key);

                    if (value != null) {
                        Log.d("stm-9", key + " : " + value.toString());
                    } else {
                        Log.d("stm-9", key + " : null");
                    }
                }
                Log.d("stm-9", "------------------------------");
            } else {
                Log.d("stm-9", "Bundle " + bundleName + ": null");
            }
        }
    }

    /**
     * Splits userId string into naming part and email part
     * 
     * @param userId
     * @return array with naming (0) and email (1)
     */
    public static String[] splitUserId(String userId) {
        String[] output = new String[2];

        String chunks[] = userId.split(" <", 2);
        userId = chunks[0];
        if (chunks.length > 1) {
            output[1] = "<" + chunks[1];
        }
        output[0] = userId;

        return output;
    }

}

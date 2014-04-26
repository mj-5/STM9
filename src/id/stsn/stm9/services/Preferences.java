package id.stsn.stm9.services;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Vector;

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
}

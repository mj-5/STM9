package id.stsn.stm9.services;

import id.stsn.stm9.pgp.PgpKeyHelper;
import id.stsn.stm9.provider.ProviderHelper;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

//public class PassphraseCacheService extends Service {
public class PassphraseCacheService extends Service {

    public static final String TAG = "stm-9" + ": PassphraseCacheService";

    public static final String ACTION_PASSPHRASE_CACHE_ADD = "id.stsn.stm9" + ".action." + "PASSPHRASE_CACHE_ADD";
    public static final String ACTION_PASSPHRASE_CACHE_GET = "id.stsn.stm9" + ".action." + "PASSPHRASE_CACHE_GET";

    public static final String EXTRA_TTL = "ttl";
    public static final String EXTRA_KEY_ID = "key_id";
    public static final String EXTRA_PASSPHRASE = "passphrase";
    public static final String EXTRA_MESSENGER = "messenger";

    Context mContext;

    /**
     * This caches a new passphrase in memory by sending a new command to the service. An android
     * service is only run once. Thus, when the service is already started, new commands just add
     * new events to the alarm manager for new passphrases to let them timeout in the future.
     * 
     * @param context
     * @param keyId
     * @param passphrase
     */
    public static void addCachedPassphrase(Context context, long keyId, String passphrase) {
    	Log.d(TAG, "cacheNewPassphrase() for " + keyId);

    	Intent intent = new Intent(context, PassphraseCacheService.class);
    	intent.setAction(ACTION_PASSPHRASE_CACHE_ADD);
//    	intent.putExtra(EXTRA_TTL, Preferences.getPreferences(context).getPassPhraseCacheTtl());
    	intent.putExtra(EXTRA_PASSPHRASE, passphrase);
    	intent.putExtra(EXTRA_KEY_ID, keyId);

    	context.startService(intent);
    }

    /**
     * Gets a cached passphrase from memory by sending an intent to the service. This method is
     * designed to wait until the service returns the passphrase.
     * 
     * @param context
     * @param keyId
     * @return passphrase or null (if no passphrase is cached for this keyId)
     */
    public static String getCachedPassphrase(Context context, long keyId) {
        Log.d(TAG, "getCachedPassphrase() get masterKeyId for " + keyId);

        Intent intent = new Intent(context, PassphraseCacheService.class);
        intent.setAction(ACTION_PASSPHRASE_CACHE_GET);

        final Object mutex = new Object();
        final Bundle returnBundle = new Bundle();

        HandlerThread handlerThread = new HandlerThread("getPassphraseThread");
        handlerThread.start();
        Handler returnHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                if (message.obj != null) {
                    String passphrase = ((Bundle) message.obj).getString(EXTRA_PASSPHRASE);
                    returnBundle.putString(EXTRA_PASSPHRASE, passphrase);
                }
                synchronized (mutex) {
                    mutex.notify();
                }
                // quit handlerThread
                getLooper().quit();
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);
        intent.putExtra(EXTRA_KEY_ID, keyId);
        intent.putExtra(EXTRA_MESSENGER, messenger);
        // send intent to this service
        context.startService(intent);

        // Wait on mutex until passphrase is returned to handlerThread
        synchronized (mutex) {
            try {
                mutex.wait(3000);
            } catch (InterruptedException e) {
            }
        }

        if (returnBundle.containsKey(EXTRA_PASSPHRASE)) {
            return returnBundle.getString(EXTRA_PASSPHRASE);
        } else {
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class PassphraseCacheBinder extends Binder {
        public PassphraseCacheService getService() {
            return PassphraseCacheService.this;
        }
    }

    private final IBinder mBinder = new PassphraseCacheBinder();
}
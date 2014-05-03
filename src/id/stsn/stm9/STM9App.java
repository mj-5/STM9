package id.stsn.stm9;

import java.security.Provider;
import java.security.Security;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import android.app.Application;
import android.util.Log;

/* class yg pertama kali running */
public class STM9App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		/* security provider = bouncyCastle */
		Security.insertProviderAt(new BouncyCastleProvider(), 1);

		/* jalankan PRNGFixes */
		PRNGFixes.apply();
		Log.d("stm-9", "Bouncy Castle set and PRNG Fixes applied!");

		if (BuildConfig.DEBUG) {
			Provider[] providers = Security.getProviders();
			Log.d("stm-9", "Installed Security Providers:");
			for (Provider p : providers) {
				Log.d("stm-9", "provider class: " + p.getClass().getName());
			}
		}
	}
}

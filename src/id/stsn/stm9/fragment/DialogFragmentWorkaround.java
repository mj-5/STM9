package id.stsn.stm9.fragment;

import android.os.Build;
import android.os.Handler;

/**
 * Bug on Android >= 4.2
 * 
 * http://code.google.com/p/android/issues/detail?id=41901
 * 
 * DialogFragment disappears on pressing home and comming back. This also happens especially in
 * FileDialogFragment after launching a file manager and coming back.
 * 
 * Usage: <code>
 * DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
 *          public void run() {
 *              // show dialog...
 *          }
 *      });
 * </code>
 */
public class DialogFragmentWorkaround {
    public static final SDKLevel17Interface INTERFACE = ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) ? new SDKLevel17Impl()
            : new SDKLevelPriorLevel17Impl());

    private static final int RUNNABLE_DELAY = 300;

    public interface SDKLevel17Interface {
        // Workaround for http://code.google.com/p/android/issues/detail?id=41901
        void runnableRunDelayed(Runnable runnable);
    }

    private static class SDKLevelPriorLevel17Impl implements SDKLevel17Interface {
        @Override
        public void runnableRunDelayed(Runnable runnable) {
            runnable.run();
        }
    }

    private static class SDKLevel17Impl implements SDKLevel17Interface {
        @Override
        public void runnableRunDelayed(Runnable runnable) {
            new Handler().postDelayed(runnable, RUNNABLE_DELAY);
        }
    }
}

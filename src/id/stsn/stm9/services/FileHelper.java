package id.stsn.stm9.services;

import id.stsn.stm9.R;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.widget.Toast;

public class FileHelper {

    public static void openFile(Activity activity, String filename, String mimeType, int requestCode) {
        Intent intent = buildFileIntent(filename, mimeType);

        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(activity, R.string.no_filemanager_installed, Toast.LENGTH_SHORT).show();
        }
    }

    private static Intent buildFileIntent(String filename, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setData(Uri.parse("file://" + filename));
        intent.setType(mimeType);

        return intent;
    }
    

    /**
     * Checks if external storage is mounted if file is located on external storage
     * 
     * @param file
     * @return true if storage is mounted
     */
    public static boolean isStorageMounted(String file) {
        if (file.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return false;
            }
        }

        return true;
    }
    
    public static void openFile(Fragment fragment, String filename, String mimeType, int requestCode) {
        Intent intent = buildFileIntent(filename, mimeType);

        try {
            fragment.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(fragment.getActivity(), R.string.no_filemanager_installed,
                    Toast.LENGTH_SHORT).show();
        }
    }

}
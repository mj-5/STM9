package id.stsn.stm9.services;

import id.stsn.stm9.R;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
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
    
    /**
     * Get a file path from a Uri.
     * 
     * from https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/
     * afilechooser/utils/FileUtils.java
     * 
     * @param context
     * @param uri
     * @return
     * 
     * @author paulburke
     */
    public static String getPath(Context context, Uri uri) {
        Log.d("Stm-9" + " File -",
                "Authority: " + uri.getAuthority() + ", Fragment: " + uri.getFragment()
                        + ", Port: " + uri.getPort() + ", Query: " + uri.getQuery() + ", Scheme: "
                        + uri.getScheme() + ", Host: " + uri.getHost() + ", Segments: "
                        + uri.getPathSegments().toString());

        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }

        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
}
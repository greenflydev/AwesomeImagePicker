package in.myinnos.imagepicker;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MyInnos on 06-03-2017.
 */

public class Helper {

    public static String[] PERMISSIONS = new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE };

    static {
        /*
         * Different permissions if the device is running android 13 SDK 33
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            PERMISSIONS = new String[] {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO };
        }
    }

    public static boolean checkPermissionForExternalStorage(Activity activity) {

        for (String permission : PERMISSIONS) {
            int permissionStatus = ContextCompat.checkSelfPermission(activity, permission);
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    public static boolean requestStoragePermission(Activity activity, int READ_STORAGE_PERMISSION) {

        List<String> permissionsNeeded = new ArrayList<>();

        for (String permission : PERMISSIONS) {
            int permissionStatus = ContextCompat.checkSelfPermission(activity, permission);
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (permissionsNeeded.size() > 0) {
            ActivityCompat.requestPermissions(activity,
                    permissionsNeeded.toArray(new String[0]),
                    READ_STORAGE_PERMISSION);
        }
        return false;
    }

}

package apptex.jgsg.apptex;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import java.util.HashMap;
import java.util.Map;

public class PermissionHandler {

    private static final String PREFS_FILE_NAME = "prefsfile";
    private static Map<Integer, PermissionRequestListener> listenerMap = new HashMap<>(4);

    /**
     * Checks if the android version is Marshmellow or above, if it is not, no runtimePermissions are required.
     * @return true if androidversion>=marshmallow, false otherwise.
     */
    public static boolean needsRuntimePermission() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    /**
     * Checks if permission from the user has to be requested.
     * @param context the current context.
     * @param permission the permission which needs to be checked.
     * @return true if {@link #needsRuntimePermission()} returns false or the permission has already been granted,
     * false otherwise.
     */
    private static boolean shouldAskPermission(Context context, String permission) {
        if (needsRuntimePermission()) {
            int permissionResult = ActivityCompat.checkSelfPermission(context, permission);
            if (permissionResult != PackageManager.PERMISSION_GRANTED)
                return true;
        }
        return false;
    }

    public static void doWithPermission(Activity activity, String permission, PermissionRequestListener listener) {
        if(shouldAskPermission(activity, permission)) {
            //The permission has not yet been granted => we need to request is.
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                listener.onPermissionPreviouslyDenied();
            } else {
                //Permission has been denied, or this is a first-time request.
                if (isFirstTimeAskingPermission(activity, permission)) {
                    //it actually is the first time, so now the first-time flag is set to false.
                    firstTimeAskingPermission(activity, permission, false);
                    int id = permission.hashCode()%65535; //can only use lower 16 bits
                    requestPermission(activity, permission, id);
                    listenerMap.put(id, listener);
                } else {
                    //Permission is denied
                    listener.onPermissionDisabled();
                }
            }
        } else {
            //Permission is granted.
            listener.onPermissionGranted();
        }
    }

    private static void requestPermission(Activity activity, String permission, int id) {
        ActivityCompat.requestPermissions(activity, new String[] {permission}, id);
    }

    public static void handlePermissionRequestResult(int id, int[] grantResults) {
        PermissionRequestListener listener = listenerMap.remove(id);
        if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            listener.onPermissionGranted();
        else
            listener.onPermissionPreviouslyDenied();
    }

    private static void firstTimeAskingPermission(Context context, String permission, boolean isFirst) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(permission, isFirst).apply();
    }

    private static boolean isFirstTimeAskingPermission(Context context, String permission) {
        return context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getBoolean(permission,true);
    }

    public interface PermissionRequestListener {

        void onPermissionPreviouslyDenied();
        void onPermissionDisabled();
        void onPermissionGranted();
    }
}

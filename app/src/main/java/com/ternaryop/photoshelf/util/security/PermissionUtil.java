package com.ternaryop.photoshelf.util.security;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.ternaryop.photoshelf.R;

/**
 * Created by dave on 03/07/16.
 * Code useful under Android M to check permissions
 */
public class PermissionUtil {

    public static void askPermission(final Activity activity, final String permission, final int requestCode) {
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{permission},
                                    requestCode);
                            break;
                    }
                }
            };

            new AlertDialog.Builder(activity)
                    .setMessage(R.string.import_permission_rationale)
                    .setPositiveButton(android.R.string.yes, dialogClickListener)
                    .setNegativeButton(android.R.string.no, dialogClickListener)
                    .show();
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{permission}, requestCode);
        }
    }
}

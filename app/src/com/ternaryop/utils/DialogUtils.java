package com.ternaryop.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogUtils {

    public static void showErrorDialog(Context context, Throwable t) {
        showErrorDialog(context, "Error", t);
    }

    public static void showErrorDialog(Context context, String title, Throwable t) {
        showSimpleMessageDialog(context, title, t.getLocalizedMessage());
    }

    public static void showErrorDialog(Context context, int resId, Throwable t) {
        showSimpleMessageDialog(context, context.getString(resId), t.getLocalizedMessage());
    }

    public static void showSimpleMessageDialog(Context context, int resId, String message) {
        showSimpleMessageDialog(context, context.getString(resId), message);
    }

    public static void showSimpleMessageDialog(Context context, String title, String message) {
        new AlertDialog.Builder(context)
        .setCancelable(false) // This blocks the 'BACK' button
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();                    
            }
        })
        .show();
    }
}

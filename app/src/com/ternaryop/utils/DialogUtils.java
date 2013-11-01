package com.ternaryop.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogUtils {

	public static void showErrorDialog(Context context, Exception e) {
		showErrorDialog(context, "Error", e);
	}

	public static void showErrorDialog(Context context, String title, Exception e) {
		new AlertDialog.Builder(context)
		.setCancelable(false) // This blocks the 'BACK' button
	    .setTitle(title)
	    .setMessage(e.getLocalizedMessage())
	    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
		        dialog.dismiss();                    
	        }
		})
	    .show();
	}

	public static void showErrorDialog(Context context, int resId, Exception e) {
		new AlertDialog.Builder(context)
		.setCancelable(false) // This blocks the 'BACK' button
	    .setTitle(resId)
	    .setMessage(e.getLocalizedMessage())
	    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
		        dialog.dismiss();                    
	        }
		})
	    .show();
	}
}

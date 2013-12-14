package com.ternaryop.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

public abstract class AbsProgressBarAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> implements OnCancelListener {
	private ProgressDialog progressDialog;
	private Exception error;
	private Context context;
	
	public AbsProgressBarAsyncTask(Context context, String message) {
		this.context = context;
		progressDialog = new ProgressDialog(context);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage(message);
		progressDialog.setOnCancelListener(this);
	}

	protected void onPreExecute() {
		progressDialog.show();
	}
	
	@Override
	protected void onPostExecute(Result result) {
		progressDialog.dismiss();
		
		if (error != null) {
			DialogUtils.showErrorDialog(context, error);
		}
	}

	public Exception getError() {
		return error;
	}

	public void setError(Exception error) {
		this.error = error;
	}

	public ProgressDialog getProgressDialog() {
		return progressDialog;
	}

	public Context getContext() {
		return context;
	}

    @Override
    public void onCancel(DialogInterface dialog) {
        cancel(true);
    }
}

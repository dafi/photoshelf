package com.ternaryop.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.widget.TextView;

public abstract class AbsProgressIndicatorAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> implements TaskWithUI, OnCancelListener {
    private ProgressDialog progressDialog;
    private Exception error;
    private final Context context;
    private final String message;
    private TextView textView;

    public AbsProgressIndicatorAsyncTask(Context context, String message) {
        this.context = context;
        this.message = message;
        initProgressDialog();
    }

    public AbsProgressIndicatorAsyncTask(Context context, String message, TextView textView) {
        this.context = context;
        this.message = message;
        this.textView = textView;
        textView.setText(message);
    }

    private void initProgressDialog() {
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(message);
        progressDialog.setOnCancelListener(this);
        progressDialog.show();
    }
    
    public void recreateUI() {
        if (progressDialog != null) {
            initProgressDialog();
            progressDialog.show();
        } else if (textView != null) {
            textView.setText(message);
        }
    }

    public void dismiss() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    protected void onPreExecute() {
        if (progressDialog != null) {
            progressDialog.show();
        }
    }
    
    @Override
    protected void onPostExecute(Result result) {
        dismiss();

        if (error != null) {
            DialogUtils.showErrorDialog(context, error);
        }
    }

    public boolean hasError() {
        return error != null;
    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        cancel(true);
    }

    public boolean isRunning() {
        return !isCancelled() && getStatus().equals(AsyncTask.Status.RUNNING);
    }

    public void setProgressMessage(String message) {
        if (progressDialog != null) {
            progressDialog.setMessage(message);
        } else if (textView != null) {
            textView.setText(message);
        }
    }
}

package com.ternaryop.tumblr;

import android.app.Dialog;
import android.content.Context;

import com.ternaryop.utils.DialogUtils;
import org.json.JSONObject;

public abstract class AbsCallback implements Callback<JSONObject> {

    private final Context context;
    private final int errorResId;
    private Dialog dialog;

    public AbsCallback(Context context, int errorResId) {
        this.context = context;
        this.errorResId = errorResId;
    }

    public AbsCallback(Dialog dialog, int errorResId) {
        this.dialog = dialog;
        this.context = dialog.getContext();
        this.errorResId = errorResId;
    }
    
    @Override
    public void failure(Exception e) {
        if (dialog != null) {
            dialog.dismiss();
        }
        DialogUtils.showErrorDialog(context, errorResId, e);
    }
}

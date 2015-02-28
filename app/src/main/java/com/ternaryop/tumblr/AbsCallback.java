package com.ternaryop.tumblr;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;

import com.ternaryop.utils.DialogUtils;
import org.json.JSONObject;

public abstract class AbsCallback implements Callback<JSONObject> {

    private final Context context;
    private final int errorResId;
    private Dialog dialog;
    private DialogFragment dialogFragment;

    public AbsCallback(Context context, int errorResId) {
        this.context = context;
        this.errorResId = errorResId;
    }

    public AbsCallback(Dialog dialog, int errorResId) {
        this(dialog.getContext(), errorResId);
        if (this.dialogFragment != null) {
            throw new IllegalArgumentException("dialogFragment must be null if dialog is set");
        }
        this.dialog = dialog;
    }

    public AbsCallback(DialogFragment dialog, int errorResId) {
        this(dialog.getActivity(), errorResId);
        if (this.dialog != null) {
            throw new IllegalArgumentException("dialog must be null if dialogFragment is set");
        }
        this.dialogFragment = dialog;
    }

    @Override
    public void failure(Exception e) {
        if (dialog != null) {
            dialog.dismiss();
        }
        if (dialogFragment != null) {
            dialogFragment.dismiss();
        }
        DialogUtils.showErrorDialog(context, errorResId, e);
    }
}

package com.ternaryop.photoshelf.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.ActionMode;
import android.view.View;
import android.widget.TextView;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.dialogs.TumblrPostDialog;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.utils.TaskWithUI;
import io.reactivex.disposables.CompositeDisposable;

public abstract class AbsPhotoShelfFragment extends Fragment implements TumblrPostDialog.PostListener {
    protected FragmentActivityStatus fragmentActivityStatus;
    protected TaskWithUI task;
    private ActionMode actionMode;

    protected CompositeDisposable compositeDisposable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        if (task != null) {
            task.dismiss();
        }
        super.onDetach();
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
     
        // all Activities must adhere to FragmentActivityStatus
        fragmentActivityStatus = (FragmentActivityStatus)activity;
    }
    
    protected boolean taskUIRecreated() {
        if (task != null && task.isRunning()) {
            task.recreateUI();
            return true;
        }
        return false;
    }
    
    public String getBlogName() {
        return fragmentActivityStatus.getAppSupport().getSelectedBlogName();
    }

    protected void refreshUI() {
        
    }

    protected void showEditDialog(final TumblrPhotoPost item, final ActionMode mode) {
        actionMode = mode;
        TumblrPostDialog.newInstance(item, this).show(getFragmentManager(), "dialog");
    }

    @Override
    public void onEditDone(TumblrPostDialog dialog, TumblrPhotoPost post) {
        post.setTags(dialog.getPostTags());
        post.setCaption(dialog.getPostTitle());
        refreshUI();
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public ActionBar getSupportActionBar() {
        return ((AppCompatActivity)getActivity()).getSupportActionBar();
    }

    protected void showSnackbar(@NonNull Snackbar snackbar) {
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.image_picker_detail_text_bg));
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(getActivity(), R.color.image_picker_detail_text_text));
        textView.setMaxLines(3);
        snackbar.show();
    }

    @NonNull
    protected Snackbar makeSnake(@NonNull View view, @NonNull Throwable t) {
        return Snackbar.make(view, t.getLocalizedMessage(), Snackbar.LENGTH_LONG);
    }

}

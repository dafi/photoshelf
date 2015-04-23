package com.ternaryop.photoshelf.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.ActionMode;

import com.ternaryop.photoshelf.dialogs.TumblrPostDialog;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.utils.TaskWithUI;

public abstract class AbsPhotoShelfFragment extends Fragment implements TumblrPostDialog.PostListener {
    protected FragmentActivityStatus fragmentActivityStatus;
    protected TaskWithUI task;
    private ActionMode actionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
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
        TumblrPostDialog.newInstance(item, false, this).show(getFragmentManager(), "dialog");
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
}

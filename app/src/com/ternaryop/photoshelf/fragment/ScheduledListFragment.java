package com.ternaryop.photoshelf.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;

public class ScheduledListFragment extends AbsPostsListFragment {
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        photoAdapter.setOnPhotoBrowseClick(this);
        if (getBlogName() != null) {
            offset = 0;
            hasMorePosts = true;
            readPhotoPosts();
        }
    }

    @Override
    protected int getActionModeMenuId() {
        return R.menu.scheduled_context;
    }
    
    @Override
    protected void readPhotoPosts() {
        if (isScrolling) {
            return;
        }
        refreshUI();
        isScrolling = true;

        new AbsProgressBarAsyncTask<Void, String, List<PhotoShelfPost> >(getActivity(), getString(R.string.reading_scheduled_posts)) {
            @Override
            protected void onProgressUpdate(String... values) {
                getProgressDialog().setMessage(values[0]);
            }
            
            @Override
            protected void onPostExecute(List<PhotoShelfPost> posts) {
                super.onPostExecute(posts);
                
                if (getError() == null) {
                    photoAdapter.addAll(posts);
                    refreshUI();
                }
                isScrolling = false;
            }

            @Override
            protected List<PhotoShelfPost> doInBackground(Void... voidParams) {
                try {
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("offset", String.valueOf(offset));
                    List<TumblrPost> photoPosts = Tumblr.getSharedTumblr(getContext())
                            .getQueue(getBlogName(), params);

                    List<PhotoShelfPost> photoList = new ArrayList<PhotoShelfPost>(); 
                    for (TumblrPost post : photoPosts) {
                        if (post.getType().equals("photo")) {
                            photoList.add(new PhotoShelfPost((TumblrPhotoPost)post,
                                    post.getScheduledPublishTime() * 1000));
                        }
                    }
                    if (photoPosts.size() > 0) {
                        // tumblr doesn't return the total posts count for scheduled posts
                        // so we set to the photoList count
                        totalPosts = photoList.size();
                        hasMorePosts = true;
                    } else {
                        totalPosts = photoAdapter.getCount() + photoList.size();
                        hasMorePosts = false;
                    }
                    return photoList;
                } catch (Exception e) {
                    e.printStackTrace();
                    setError(e);
                }
                return Collections.emptyList();
            }
        }.execute();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scheduler, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }
}

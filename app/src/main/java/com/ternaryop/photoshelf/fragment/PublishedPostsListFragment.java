package com.ternaryop.photoshelf.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressIndicatorAsyncTask;
import com.ternaryop.utils.TaskWithUI;

public class PublishedPostsListFragment extends ScheduledListFragment {
    @Override
    protected void readPhotoPosts() {
        if (isScrolling) {
            return;
        }
        refreshUI();
        isScrolling = true;

        task = (TaskWithUI) new AbsProgressIndicatorAsyncTask<Void, String, List<PhotoShelfPost> >(getActivity(), getString(R.string.reading_published_posts)) {
            @Override
            protected void onProgressUpdate(String... values) {
                setProgressMessage(values[0]);
            }

            @Override
            protected void onPostExecute(List<PhotoShelfPost> posts) {
                super.onPostExecute(posts);

                if (!hasError()) {
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
                    params.put("type", "photo");
                    params.put("notes_info", "true");
                    List<TumblrPost> photoPosts = Tumblr.getSharedTumblr(getContext())
                            .getPublicPosts(getBlogName(), params);

                    List<PhotoShelfPost> photoList = new ArrayList<PhotoShelfPost>(); 
                    for (TumblrPost post : photoPosts) {
                        if (post.getType().equals("photo")) {
                            photoList.add(new PhotoShelfPost((TumblrPhotoPost)post, post.getTimestamp() * 1000));
                        }
                    }
                    if (photoPosts.size() > 0) {
                        totalPosts += photoPosts.size();
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
    protected int getActionModeMenuId() {
        return R.menu.published_context;
    }
}

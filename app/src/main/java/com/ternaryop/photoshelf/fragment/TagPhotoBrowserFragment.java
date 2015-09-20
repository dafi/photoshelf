package com.ternaryop.photoshelf.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost;
import com.ternaryop.photoshelf.db.TagCursorAdapter;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressIndicatorAsyncTask;

public class TagPhotoBrowserFragment extends AbsPostsListFragment implements SearchView.OnSuggestionListener {
    private String postTag;
    private boolean allowSearch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        photoAdapter.setOnPhotoBrowseClick(this);
        photoListView.setEmptyView(rootView.findViewById(android.R.id.empty));
        
        if (getBlogName() != null && postTag != null && postTag.trim().length() > 0) {
            onQueryTextSubmit(postTag.trim());
        }
        
        return rootView;
    }
    
    @Override
    protected int getPostListViewResource() {
        return R.layout.fragment_tag_browse_photo_list;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Bundle bundle = activity.getIntent().getExtras();
        if (bundle == null) {
            allowSearch = true;
        } else {
            postTag = bundle.getString(Constants.EXTRA_BROWSE_TAG);
            allowSearch = bundle.getBoolean(Constants.EXTRA_ALLOW_SEARCH, true);
        }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.tag_browser, menu);
        
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean isMenuVisible = allowSearch && !fragmentActivityStatus.isDrawerOpen();
        menu.findItem(R.id.action_search).setVisible(isMenuVisible);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected SearchView setupSearchView(Menu menu) {
        super.setupSearchView(menu);

        searchView.setOnSuggestionListener(this);
        TagCursorAdapter adapter = new TagCursorAdapter(
                getSupportActionBar().getThemedContext(),
                R.layout.ab_simple_dropdown_item_1line,
                getBlogName());
        searchView.setSuggestionsAdapter(adapter);
        return searchView;
    }
    
    protected void readPhotoPosts() {
        if (isScrolling) {
            return;
        }
        refreshUI();
        isScrolling = true;

        new AbsProgressIndicatorAsyncTask<Void, String, List<PhotoShelfPost> >(getActivity(), getString(R.string.reading_tags_title, postTag)) {
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
                    params.put("tag", postTag);
                    params.put("notes_info", "true");
                    params.put("offset", String.valueOf(offset));
                    List<TumblrPhotoPost> photoPosts = Tumblr.getSharedTumblr(getContext())
                            .getPhotoPosts(getBlogName(), params);

                    List<PhotoShelfPost> photoList = new ArrayList<PhotoShelfPost>();
                    for (TumblrPost post : photoPosts) {
                        photoList.add(new PhotoShelfPost((TumblrPhotoPost)post,
                                post.getTimestamp() * 1000));
                    }
                    if (photoPosts.size() > 0) {
                        totalPosts = photoList.get(0).getTotalPosts();
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
        return R.menu.tag_browser_context;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        postTag = query;
        offset = 0;
        hasMorePosts = true;
        photoAdapter.clear();
        photoAdapter.notifyDataSetChanged();
        readPhotoPosts();
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        String query = ((Cursor)searchView.getSuggestionsAdapter().getItem(position)).getString(1);
        searchView.setQuery(query, true);
        return true;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return true;
    }

    @Override
    public void onTagClick(int position) {
        final PhotoShelfPost post = photoAdapter.getItem(position);
        // do nothing if tags are equal otherwise a new TagBrowser on same tag is launched
        if (!postTag.equalsIgnoreCase(post.getFirstTag())) {
            super.onTagClick(position);
        }
    }
}

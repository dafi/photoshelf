package com.ternaryop.phototumblrshare.db;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import android.content.Context;

import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.PostRetriever;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.DialogUtils;

public class Importer {
	public static void importPostsFromCSV(final Context context, final String importPath) {
		try {
			new DbImportAsyncTask(context, new CSVIterator(importPath), true).execute();
		} catch (Exception error) {
			DialogUtils.showErrorDialog(context, error);
		}
	}

	public static void importFromTumblr(final Context context, final String blogName) {
		PostTag post = DBHelper.getInstance(context).getPostTagDAO().findLastPublishedPost(blogName);
		PostRetriever postRetriever = new PostRetriever(context, post.getPublishTimestamp(), new Callback<List<TumblrPost>>() {
			
			@Override
			public void failure(Tumblr tumblr, Exception error) {
				if (error != null) {
					DialogUtils.showErrorDialog(context, error);
				}
			}
			
			@Override
			public void complete(Tumblr tumblr, List<TumblrPost> allPosts) {
				List<PostTag> allPostTags = new ArrayList<PostTag>();
				for (TumblrPost tumblrPost : allPosts) {
					allPostTags.addAll(PostTag.postTagsFromTumblrPost(tumblrPost));
				}
				new DbImportAsyncTask(context, allPostTags.iterator(), false).execute();
			}
		});
		Tumblr.getSharedTumblr(context).readPublicPhotoPosts(blogName, null, postRetriever);
	}

	static class CSVIterator implements Iterator<PostTag> {
		private BufferedReader bufferedReader;
		private String line;

		public CSVIterator(String importPath) throws IOException {
	        bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(importPath)));
			line = bufferedReader.readLine();
		}

		@Override
		public boolean hasNext() {
			return line != null;
		}

		@Override
		public PostTag next() {
			try {
	        	String[] args = line.split(";");
				PostTag postTag = new PostTag(
						Long.parseLong(args[0]),
						args[1],
						args[2],
						Long.parseLong(args[3]),
						Long.parseLong(args[4]));
				line = bufferedReader.readLine();
				return postTag;
			} catch (IOException e) {
				throw new NoSuchElementException(e.getMessage());
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
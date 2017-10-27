package com.ternaryop.photoshelf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;

import com.ternaryop.tumblr.TumblrPost;

/**
 * Created by dave on 13/03/15.
 * Used to caches posts to file system
 */
public class TumblrPostCache {
    private final File cacheFileDir;

    public TumblrPostCache(Context context, String cacheDir) {
        cacheFileDir = new File(context.getCacheDir(), cacheDir);
    }

    public void deleteItem(TumblrPost item) {
        new File(getCacheDir(), String.valueOf(item.getPostId())).delete();
    }

    public void updateItem(TumblrPost item) {
        File cacheDir = getCacheDir();
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(new File(cacheDir, String.valueOf(item.getPostId()))));
            ObjectOutput output = new ObjectOutputStream(bos);
            output.writeObject(item);
        } catch (Exception ignored) {
        } finally {
            if (bos != null) try { bos.close(); } catch (Exception ignored) {}
        }
    }

    public void clearCache() {
        File[] list = getCacheDir().listFiles();

        if (list != null) {
            for (File f : list) {
                f.delete();
            }
        }
    }

    public List<TumblrPost> read() {
        File[] list = getCacheDir().listFiles();

        if (list == null) {
            return Collections.emptyList();
        }
        ArrayList<TumblrPost> posts = new ArrayList<>(list.length);

        for (File f : list) {
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(new FileInputStream(f));
                ObjectInput input = new ObjectInputStream(bis);
                posts.add((TumblrPost) input.readObject());
            } catch (Exception ignored) {
            } finally {
                if (bis != null) try { bis.close(); } catch (Exception ignored) {}
            }
        }
        return posts;
    }

    public void write(List<TumblrPost> photos, boolean clearCache) {
        if (clearCache) {
            clearCache();
        }
        File cacheDir = getCacheDir();
        for (TumblrPost p : photos) {
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(new File(cacheDir, String.valueOf(p.getPostId()))));
                ObjectOutput output = new ObjectOutputStream(bos);
                output.writeObject(p);
            } catch (Exception ignored) {
            } finally {
                if (bos != null) try { bos.close(); } catch (Exception ignored) {}
            }
        }
    }

    private File getCacheDir() {
        if (!cacheFileDir.exists()) {
            cacheFileDir.mkdirs();
        }
        return cacheFileDir;
    }
}

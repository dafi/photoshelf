package com.fedorvlasov.lazylist;

import java.io.File;
import java.io.FilenameFilter;

import android.content.Context;

public class FileCache {
    
    private File cacheDir;
    private String prefix;

    public FileCache(Context context) {
    	this(context, null);
    }
    
    public FileCache(Context context, String prefix) {
    	this.prefix = prefix;
		cacheDir = context.getCacheDir();
        
		if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }
    
    public File getFile(String url) {
        //I identify images by hashcode. Not a perfect solution, good for the demo.
        String filename = String.valueOf(Integer.toHexString(url.hashCode()));
        //Another possible solution (thanks to grantland)
        //String filename = URLEncoder.encode(url);
        if (prefix != null) {
        	filename = prefix + filename;
        }
        return new File(cacheDir, filename);
    }
    
    public void clear() {
        File[] files;
        
        if (prefix == null) {
        	files = cacheDir.listFiles();
        } else {
        	files = cacheDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					return filename.startsWith(prefix);
				}
			});
        }
        if (files != null) {
        	for (File f : files) {
        		f.delete();
        	}
        }
    }
}
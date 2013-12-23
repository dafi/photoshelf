package com.ternaryop.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IOUtils {
	private static final int BUFFER_SIZE = 100 * 1024;

	public static void copy(InputStream is, OutputStream os) throws IOException {
		byte[] buff = new byte[BUFFER_SIZE];
		int count;
	    BufferedInputStream bis = new BufferedInputStream(is);
	    BufferedOutputStream bos = new BufferedOutputStream(os);

		while ((count = bis.read(buff)) != -1) {
			bos.write(buff, 0, count);
		}
	    bos.flush();
	}

    public static String generateUniqueFileName(String path) {
    	File file = new File(path);
    	File parentFile = file.getParentFile();

    	String nameWithExt = file.getName();
    	Pattern patternCount = Pattern.compile("(.*) \\((\\d+)\\)");
    	
    	while (file.exists()) {
    		String name;
    		String ext;
    		int extPos = nameWithExt.lastIndexOf('.');
    		if (extPos < 0) {
    			name = nameWithExt;
    			ext = "";
    		} else {
    			name = nameWithExt.substring(0, extPos);
    			// contains dot
    			ext = nameWithExt.substring(extPos); 
    		}
    		Matcher matcherCount = patternCount.matcher(name);
    		int count = 1;
    		if (matcherCount.matches()) {
    			name = matcherCount.group(1);
    			count = Integer.parseInt(matcherCount.group(2)) + 1;
    		}
    		nameWithExt = name + " (" + count + ")" + ext;
    		file = new File(parentFile, nameWithExt);
    	}
    	return file.getAbsolutePath();
    }

    public static byte[] readURL(String url) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            saveURL(url, baos);
        } finally {
            if (baos != null) try { baos.close(); } catch (Exception e) {}
        }

        return baos.toByteArray();
    }

    public static void saveURL(String url, OutputStream os) throws IOException {
        HttpURLConnection connection = null;
        InputStream is = null;

        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            is = connection.getInputStream();
            copy(is, os);
        } finally {
            if (is != null) try { is.close(); } catch (Exception e) {}
            if (connection != null) try { connection.disconnect(); } catch (Exception e) {}
        }
    }
}
package com.ternaryop.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
}

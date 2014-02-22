package com.ternaryop.photoshelf.importer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CSVIterator<T> implements Iterator<T> {
	private BufferedReader bufferedReader;
	private String line;
	private CSVBuilder<T> builder;

	public CSVIterator(String importPath, CSVBuilder<T> builder) throws IOException {
		this(new FileInputStream(importPath), builder);
	}

	public CSVIterator(FileInputStream fis, CSVBuilder<T> builder) throws IOException {
		this.builder = builder;
		bufferedReader = new BufferedReader(new InputStreamReader(fis));
		line = bufferedReader.readLine();
	}
	
	@Override
	public boolean hasNext() {
		return line != null;
	}

	@Override
	public T next() {
		try {
			String[] fields = line.split(";");
			T result = builder.parseCSVFields(fields);
			line = bufferedReader.readLine();
			return result;
		} catch (Exception e) {
			throw new NoSuchElementException(e.getMessage());
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	public interface CSVBuilder<T> {
		public T parseCSVFields(String fields[]) throws ParseException;
	}
}


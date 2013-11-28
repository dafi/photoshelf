package com.ternaryop.phototumblrshare.db;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.provider.BaseColumns;

// SELECT date('now', '-1 day'), name, strftime('%d-%m-%Y', birth_date) FROM consolr_birthday 
// where strftime('%m',birth_date) = strftime('%m', date('now'))
// order by strftime('%d',birth_date)

public class Birthday implements BaseColumns, Serializable {
	private static final long serialVersionUID = 5887665998065237345L;
	private String name;
	private Date birthDate;
	private String tumblrName;

	public static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	
	public Birthday() {
	}

	public Birthday(String name, Date birthDate, String tumblrName) {
		this.tumblrName = tumblrName;
		this.name = name.toLowerCase(Locale.US);
		this.birthDate = birthDate;
	}

	public Birthday(String name, String birthDate, String tumblrName) throws ParseException {
		this(name, ISO_DATE_FORMAT.parse(birthDate), tumblrName);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name.toLowerCase(Locale.US);
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public String getTumblrName() {
		return tumblrName;
	}

	public void setTumblrName(String tumblrName) {
		this.tumblrName = tumblrName;
	}
	
	@Override
	public String toString() {
		return ISO_DATE_FORMAT.format(birthDate) + " " + name;
	}
}
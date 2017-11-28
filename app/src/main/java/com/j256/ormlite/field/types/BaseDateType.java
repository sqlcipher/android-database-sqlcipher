package com.j256.ormlite.field.types;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;

/**
 * Base class for all of the {@link java.sql.Date} class types.
 * 
 * @author graywatson
 */
public abstract class BaseDateType extends BaseDataType {

	protected static final DateStringFormatConfig defaultDateFormatConfig = new DateStringFormatConfig(
			"yyyy-MM-dd HH:mm:ss.SSSSSS");

	protected BaseDateType(SqlType sqlType, Class<?>[] classes) {
		super(sqlType, classes);
	}

	protected BaseDateType(SqlType sqlType) {
		super(sqlType);
	}

	protected static DateStringFormatConfig convertDateStringConfig(FieldType fieldType,
			DateStringFormatConfig defaultDateFormatConfig) {
		if (fieldType == null) {
			return defaultDateFormatConfig;
		}
		DateStringFormatConfig configObj = (DateStringFormatConfig) fieldType.getDataTypeConfigObj();
		if (configObj == null) {
			return defaultDateFormatConfig;
		} else {
			return (DateStringFormatConfig) configObj;
		}
	}

	protected static Date parseDateString(DateStringFormatConfig formatConfig, String dateStr) throws ParseException {
		DateFormat dateFormat = formatConfig.getDateFormat();
		return dateFormat.parse(dateStr);
	}

	protected static String normalizeDateString(DateStringFormatConfig formatConfig, String dateStr)
			throws ParseException {
		DateFormat dateFormat = formatConfig.getDateFormat();
		Date date = dateFormat.parse(dateStr);
		return dateFormat.format(date);
	}

	protected static class DateStringFormatConfig {
		private final ThreadLocal<DateFormat> threadLocal = new ThreadLocal<DateFormat>() {
			@Override
			protected DateFormat initialValue() {
				return new SimpleDateFormat(dateFormatStr);
			}
		};
		final String dateFormatStr;
		public DateStringFormatConfig(String dateFormatStr) {
			this.dateFormatStr = dateFormatStr;
		}
		public DateFormat getDateFormat() {
			return threadLocal.get();
		}
		@Override
		public String toString() {
			return dateFormatStr;
		}
	}

	@Override
	public boolean isValidForVersion() {
		return true;
	}

	@Override
	public Object moveToNextValue(Object currentValue) {
		long newVal = System.currentTimeMillis();
		if (currentValue == null) {
			return new Date(newVal);
		} else if (newVal == ((Date) currentValue).getTime()) {
			return new Date(newVal + 1L);
		} else {
			return new Date(newVal);
		}
	}

	@Override
	public boolean isValidForField(Field field) {
		return (field.getType() == Date.class);
	}
}

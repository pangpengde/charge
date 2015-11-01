package com.common.database;

import android.database.Cursor;

public class DbUtils {
	public static short getShort(Cursor cursor, int index) {
		return getShort(cursor, index, (short) 0);
	}
	public static short getShort(Cursor cursor, int index, short defValue) {
		if (cursor == null || cursor.isNull(index)) {
			return defValue;
		} else {
			return cursor.getShort(index);
		}
	}
	public static int getInt(Cursor cursor, int index) {
		return getInt(cursor, index, 0);
	}
	public static int getInt(Cursor cursor, int index, int defValue) {
		if (cursor == null || cursor.isNull(index)) {
			return defValue;
		} else {
			return cursor.getInt(index);
		}
	}
	public static long getLong(Cursor cursor, int index) {
		return getLong(cursor, index, 0);
	}
	public static long getLong(Cursor cursor, int index, long defValue) {
		if (cursor == null || cursor.isNull(index)) {
			return defValue;
		} else {
			return cursor.getLong(index);
		}
	}
	public static String getString(Cursor cursor, int index) {
		return getString(cursor, index, "");
	}
	public static String getString(Cursor cursor, int index, String defValue) {
		if (cursor == null || cursor.isNull(index)) {
			return defValue;
		} else {
			return cursor.getString(index);
		}
	}
}

package com.common.database;

import android.database.sqlite.SQLiteDatabase;

interface SQLiteDatabaseLink {
	SQLiteDatabase getRef();
	SQLiteDatabase acquireRef();
	void releaseRef();
}

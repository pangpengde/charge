package com.common.database;

public class ManagedDatabaseInfo {
	protected String mDbUri = "";
	protected String mBkUri = "";
	
	// ### 构造函数 ###
	public ManagedDatabaseInfo(String dbUri) {
		this(dbUri, "");
	}
	public ManagedDatabaseInfo(String dbUri, String backupUri) {
		mDbUri = dbUri;
		mBkUri = backupUri;
	}
	public ManagedDatabaseInfo(ManagedDatabaseInfo dbInfo) {
		mDbUri = dbInfo.mDbUri;
		mBkUri = dbInfo.mBkUri;
	}
}

package com.common.database;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

public class ManagedCursor implements Cursor {
	private final SQLiteDatabaseLink mSqlLink;
	private final Cursor mSqlCursor;
	
	// ### 构造函数 ###
	protected ManagedCursor(SQLiteDatabaseLink sqlLink, Cursor sqlCursor) {
		assert sqlLink != null;
		assert sqlCursor != null;
		
		mSqlLink = sqlLink;
		mSqlCursor = sqlCursor;
		mSqlLink.acquireRef();
	}
	
	// ### Cursor接口实现 ###
	@Override
	public void close() {
		if (isClosed())
			return;
		
		mSqlCursor.close();
		mSqlLink.releaseRef();
	}
	@Override
	public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
		mSqlCursor.copyStringToBuffer(columnIndex, buffer);
	}
	@SuppressWarnings("deprecation")
	@Override
	public void deactivate() {
		mSqlCursor.deactivate();
	}
	@Override
	public byte[] getBlob(int columnIndex) {
		return mSqlCursor.getBlob(columnIndex);
	}
	@Override
	public int getColumnCount() {
		return mSqlCursor.getColumnCount();
	}
	@Override
	public int getColumnIndex(String columnName) {
		return mSqlCursor.getColumnIndex(columnName);
	}
	@Override
	public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
		return mSqlCursor.getColumnIndexOrThrow(columnName);
	}
	@Override
	public String getColumnName(int columnIndex) {
		return mSqlCursor.getColumnName(columnIndex);
	}
	@Override
	public String[] getColumnNames() {
		return mSqlCursor.getColumnNames();
	}
	@Override
	public int getCount() {
		return mSqlCursor.getCount();
	}
	@Override
	public double getDouble(int columnIndex) {
		return mSqlCursor.getDouble(columnIndex);
	}
	@Override
	public Bundle getExtras() {
		return mSqlCursor.getExtras();
	}
	@Override
	public float getFloat(int columnIndex) {
		return mSqlCursor.getFloat(columnIndex);
	}
	@Override
	public int getInt(int columnIndex) {
		return mSqlCursor.getInt(columnIndex);
	}
	@Override
	public long getLong(int columnIndex) {
		return mSqlCursor.getLong(columnIndex);
	}
	@Override
	public int getPosition() {
		return mSqlCursor.getPosition();
	}
	@Override
	public short getShort(int columnIndex) {
		return mSqlCursor.getShort(columnIndex);
	}
	@Override
	public String getString(int columnIndex) {
		return mSqlCursor.getString(columnIndex);
	}
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public int getType(int columnIndex) {
		return mSqlCursor.getType(columnIndex);
	}
	@Override
	public boolean getWantsAllOnMoveCalls() {
		return mSqlCursor.getWantsAllOnMoveCalls();
	}
	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	public Uri getNotificationUri() {
		return mSqlCursor.getNotificationUri();
	}
	@Override
	public boolean isAfterLast() {
		return mSqlCursor.isAfterLast();
	}
	@Override
	public boolean isBeforeFirst() {
		return mSqlCursor.isBeforeFirst();
	}
	@Override
	public boolean isClosed() {
		return mSqlCursor.isClosed();
	}
	@Override
	public boolean isFirst() {
		return mSqlCursor.isFirst();
	}
	@Override
	public boolean isLast() {
		return mSqlCursor.isLast();
	}
	@Override
	public boolean isNull(int columnIndex) {
		return mSqlCursor.isNull(columnIndex);
	}
	@Override
	public boolean move(int offset) {
		return mSqlCursor.move(offset);
	}
	@Override
	public boolean moveToFirst() {
		return mSqlCursor.moveToFirst();
	}
	@Override
	public boolean moveToLast() {
		return mSqlCursor.moveToLast();
	}
	@Override
	public boolean moveToNext() {
		return mSqlCursor.moveToNext();
	}
	@Override
	public boolean moveToPosition(int position) {
		return mSqlCursor.moveToPosition(position);
	}
	@Override
	public boolean moveToPrevious() {
		return mSqlCursor.moveToPrevious();
	}
	@Override
	public void registerContentObserver(ContentObserver observer) {
		mSqlCursor.registerContentObserver(observer);
	}
	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		mSqlCursor.registerDataSetObserver(observer);		
	}
	@Override
	public boolean requery() {
		return false;
	}
	@Override
	public Bundle respond(Bundle extras) {
		return mSqlCursor.respond(extras);
	}
	@Override
	public void setNotificationUri(ContentResolver cr, Uri uri) {
		mSqlCursor.setNotificationUri(cr, uri);
	}
	@Override
	public void unregisterContentObserver(ContentObserver observer) {
		mSqlCursor.unregisterContentObserver(observer);
	}
	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		mSqlCursor.unregisterDataSetObserver(observer);
	}
    @Override
    public void setExtras(Bundle extras) {
        mSqlCursor.setExtras(extras);
    }

    // ### Object函数重写 ###
	@Override
	public void finalize() {
		close();
	}
}

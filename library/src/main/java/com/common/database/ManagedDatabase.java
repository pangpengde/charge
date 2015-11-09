package com.common.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import com.common.utils.FileUtils;
import com.common.sys.MainThread;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

// ATTENTION(by lizhan@duokan.com):
// 实现中应保证上层synchronize和底层数据库线程锁作用域内无互相等待的操作, 避免死锁发生.
// 需要防止如下情况出现:
// synchronize(this) {
//     mDb.beginTransaction(); // 此处可能创建线程锁, 并且在调用endTransaction()前, 一直处于锁死状态.
// }
// 
// 底层数据库创建的线程锁可能依然处于锁死状态, 如下代码导致线程锁互相依赖:
// synchronize(this) { 
//     mDb... // 此处容易发生死锁
// }
public class ManagedDatabase {
	private final ReentrantLock mLock = new ReentrantLock();
	private final ManagedDatabaseInfo mDbInfo;
	private final SQLiteDatabaseLink mSqlLink;
	private boolean mClosed = false;
	private boolean mBackuping = false;
	private Runnable mBackupTask = null;
	
	// ### 构造函数 ###
	public ManagedDatabase(String dbUri) {
		this(dbUri, "");
	}
	public ManagedDatabase(String dbUri, String backupUri) {
		mDbInfo = new ManagedDatabaseInfo(dbUri, backupUri);
		
		if (TextUtils.isEmpty(mDbInfo.mBkUri) == false) {
//			ManagedApp.get().addOnRunningStateChangedListener(new ManagedApp.OnRunningStateChangedListener() {
//				@Override
//				public void onRunningStateChanged(ManagedApp app, RunningState oldState, RunningState newState) {
//					if (newState.ordinal() > RunningState.BACKGROUND.ordinal()) {
//						cancelBackup();
//					} else if (newState.ordinal() < oldState.ordinal()) {
//						scheduleBackup();
//					}
//				}
//			});
		}
		
		mSqlLink = new SQLiteDatabaseLink() {
			private SQLiteDatabase mSqlDb = null;
			private int mRefCount = 1;
			
			@Override
			public SQLiteDatabase getRef() {
				return mSqlDb;
			}
			@Override
			public SQLiteDatabase acquireRef() {
				mLock.lock();
				try {
					if (++mRefCount > 1 && mSqlDb == null) {
						restoreDatabase();
						try {
							final File dbFile = new File(Uri.parse(mDbInfo.mDbUri).getPath());
                            File parentFile = dbFile.getParentFile();
                            if (!parentFile.exists()) {
                                parentFile.mkdirs();
                            }
							mSqlDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
						} catch (Throwable e) {
							e.printStackTrace();
							mSqlDb = SQLiteDatabase.create(null);
						}
					}
				} finally {
					mLock.unlock();
				}
				
				return mSqlDb;
			}
			@Override
			public void releaseRef() {
				mLock.lock();
				try {
					if (--mRefCount == 0 && mSqlDb != null) {
						mSqlDb.close();
						mSqlDb = null;
					}
				} finally {
					mLock.unlock();
				}
			}
		};
	}
		
	// ### 属性 ###
	public String getDatabaseUri() {
		return mDbInfo.mDbUri;
	}
	public String getBackupUri() {
		return mDbInfo.mBkUri;
	}
	
	// ### 方法 ###
//	public void beginTransaction() {
//		final int maxRetryTimes = 2;
//		int retryTimes = 0;
//		while (retryTimes <= maxRetryTimes) {
//			if (retryTimes == maxRetryTimes) {
//				beginTransactionImpl();
//				break;
//			} else {
//				try {
//					beginTransactionImpl();
//					break;
//				} catch (Throwable th) {
//					retryTimes++;
//					try {
//						Thread.sleep(retryTimes * 2000);
//					} catch (InterruptedException ex) {
//					}
//				}
//			}
//		}
//	}
//	private void beginTransactionImpl() {
//		synchronized (this) {
//			open().beginTransaction();
//			mTransactionCount++;
//		}
//	}
	public boolean beginTransactionNoThrow() {
		try {
			// 如果beginTransaction()成功, 申请的引用由endTransaction()负责释放.
			mSqlLink.acquireRef().beginTransaction();
		} catch (Throwable e) {
			mSqlLink.releaseRef();
			return false;
		}
		
		return true;
	}
	public void beginTransaction() {
		// 这里申请的引用由endTransaction()负责释放
		mSqlLink.acquireRef().beginTransaction();
	}
//TODO(by lizhan@duokan.com): 返回值需要转换成ManagedDatabaseStatement...
//	public SQLiteStatement compileStatement(String sql) {
//		try {
//			return mSqlLink.acquireRef().compileStatement(sql);
//		} finally {
//			mSqlLink.releaseRef();
//		}
//	}
	public int delete(String table, String whereClause, String[] whereArgs) {
		try {
			return mSqlLink.acquireRef().delete(table, whereClause, whereArgs);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public void endTransaction() {
		try {
			mSqlLink.getRef().endTransaction();
		} finally {
			// 释放beginTransaction()时申请的引用
			mSqlLink.releaseRef();
		}
	}
	public void execSQL(String sql, Object[] bindArgs) throws SQLException {
		try {
			mSqlLink.acquireRef().execSQL(sql, bindArgs);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public void execSQL(String sql) throws SQLException {
		try {
			mSqlLink.acquireRef().execSQL(sql);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public int getVersion() {
		try {
			return mSqlLink.acquireRef().getVersion();
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public boolean inTransaction() {
		try {
			return mSqlLink.acquireRef().inTransaction();
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public long insert(String table, String nullColumnHack, ContentValues values) {
		try {
			return mSqlLink.acquireRef().insert(table, nullColumnHack, values);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public long insertOrThrow(String table, String nullColumnHack, ContentValues values) throws SQLException {
		try {
			return mSqlLink.acquireRef().insertOrThrow(table, nullColumnHack, values);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public long insertWithOnConflict(String table, String nullColumnHack, ContentValues initialValues, int conflictAlgorithm) {
		try {
			return mSqlLink.acquireRef().insertWithOnConflict(table, nullColumnHack, initialValues, conflictAlgorithm);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public boolean isReadOnly() {
		try {
			return mSqlLink.acquireRef().isReadOnly();
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		try {
			final Cursor sqlCursor = mSqlLink.acquireRef().query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
			return new ManagedCursor(mSqlLink, sqlCursor);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		try {
			final Cursor sqlCursor = mSqlLink.acquireRef().query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
			return new ManagedCursor(mSqlLink, sqlCursor);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy) {
		try {
			final Cursor sqlCursor = mSqlLink.acquireRef().query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
			return new ManagedCursor(mSqlLink, sqlCursor);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public Cursor rawQuery(String sql, String[] selectionArgs) {
		try {
			final Cursor sqlCursor = mSqlLink.acquireRef().rawQuery(sql, selectionArgs);
			return new ManagedCursor(mSqlLink, sqlCursor);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public long replace(String table, String nullColumnHack, ContentValues initialValues) {
		try {
			return mSqlLink.acquireRef().replace(table, nullColumnHack, initialValues);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public long replaceOrThrow(String table, String nullColumnHack,	ContentValues initialValues) throws SQLException {
		try {
			return mSqlLink.acquireRef().replaceOrThrow(table, nullColumnHack, initialValues);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public void setTransactionSuccessful() {
		try {
			mSqlLink.acquireRef().setTransactionSuccessful();
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public void setVersion(int version) {
		try {
			mSqlLink.acquireRef().setVersion(version);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
		try {
			return mSqlLink.acquireRef().update(table, values, whereClause, whereArgs);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public int updateWithOnConflict(String table, ContentValues values, String whereClause, String[] whereArgs, int conflictAlgorithm) {
		try {
			return mSqlLink.acquireRef().updateWithOnConflict(table, values, whereClause, whereArgs, conflictAlgorithm);
		} finally {
			mSqlLink.releaseRef();
		}
	}
	public void close() {
		final boolean closed;
		mLock.lock();
		try {
			closed = mClosed;
			mClosed = true;
		} finally {
			mLock.unlock();
		}
		
		if (closed == false) {
			mSqlLink.releaseRef();
		}
	}
	
	// ### 实现函数 ###
	private void cancelBackup() {
		if (mBackuping)
			return;
		
		mBackupTask = null;
	}
	private void scheduleBackup() {
		assert mLock.isHeldByCurrentThread();
		assert TextUtils.isEmpty(mDbInfo.mBkUri) == false;

		if (mBackuping)
			return;

		final File dbFile = new File(Uri.parse(mDbInfo.mDbUri).getPath());
		final File bkFile = new File(Uri.parse(mDbInfo.mBkUri).getPath());

		if (bkFile.length() == dbFile.length() && bkFile.lastModified() > dbFile.lastModified())
			return;

		mBackupTask = new Runnable() {
			@Override
			public void run() {
				if (mBackupTask != this)
					return;

				assert mBackuping == false;
				if (mBackuping)
					return;
				
//				if (ManagedApp.get().getRunningState().ordinal() > RunningState.BACKGROUND.ordinal())
//					return;
					
				mBackuping = true;
				final Thread backupThread = new Thread(new Runnable() {
					@Override
					public void run() {
						mLock.lock();
						try {
							if (mClosed) {
								doBackupDatabase(dbFile, bkFile);
							} else {
								mSqlLink.releaseRef();
								doBackupDatabase(dbFile, bkFile);
								mSqlLink.acquireRef();
							}
						} finally {
							mLock.unlock();
							mBackuping = false;
						}
					}
				});
				
				backupThread.start();
			}
		};
		
		MainThread.runLater(mBackupTask, 15000);
	}
	private void doBackupDatabase(File dbFile, File bkFile) {
		assert mLock.isHeldByCurrentThread();
		if (mSqlLink.getRef() != null)
			return;

		try {
			final File tmpFile = new File(Uri.parse(mDbInfo.mBkUri + ".tmp").getPath());
			if (FileUtils.copyFile(dbFile, tmpFile)) {
				bkFile.delete();
				tmpFile.renameTo(bkFile);
			} else {
				tmpFile.delete();
			}
		} catch (Throwable e) {
			
		}
	}
	private void restoreDatabase() {
		assert mLock.isHeldByCurrentThread();

		if (TextUtils.isEmpty(mDbInfo.mBkUri))
			return;

		final File dbFile = new File(Uri.parse(mDbInfo.mDbUri).getPath());
		final File bkFile = new File(Uri.parse(mDbInfo.mBkUri).getPath());
		
		if (dbFile.exists() == false && bkFile.exists()) {
			final File tmpFile = new File(Uri.parse(mDbInfo.mDbUri + ".tmp").getPath());
			try {
				if (FileUtils.copyFile(bkFile, tmpFile)) {
					tmpFile.renameTo(dbFile);
				} else {
					tmpFile.delete();
				}
			} catch (Throwable e) {
				
			}
		}
	}
}

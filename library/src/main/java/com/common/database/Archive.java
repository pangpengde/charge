package com.common.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.common.sys.ThreadSafe;


public class Archive implements ThreadSafe {
	private static final int DB_VERSION_FIRST = 1;
	private static final ScheduledExecutorService mDbWriter = Executors.newSingleThreadScheduledExecutor();
	private final ManagedDatabase mDb;

	// ### 构造函数 ###
	public Archive(String archiveUri) {
		this(archiveUri, "");
	}
	public Archive(String archiveUri, String backupUri) {
		mDb = new ManagedDatabase(archiveUri, backupUri);
		
		asyncWriteInTransaction(new WriteTask() {
			@Override
			public boolean write(ManagedDatabase db) {
				if (db.getVersion() < DB_VERSION_FIRST) {
					createObjectTable(db);
					db.setVersion(DB_VERSION_FIRST);
				}
				return true;
			}
		});
	}

	// ### 方法 ###
	public <T extends Serializable> T readObject(String name, T defautObject) {
		final T readObject = readObject(name);
		return readObject != null ? readObject : defautObject; 
	}
	public <T extends Serializable> T readObject(String name) {
		final byte[] bytes = readObjectBytes(mDb, name);
		return objectFromBytes(bytes);
	}
	public void beginBatchWrite() {
		asyncWrite(new WriteTask() {
			@Override
			public boolean write(ManagedDatabase db) {
				db.beginTransaction();
				return true;
			}
		});
	}
	public void setBatchWriteSuccessful() {
		asyncWrite(new WriteTask() {
			@Override
			public boolean write(ManagedDatabase db) {
				db.setTransactionSuccessful();
				return true;
			}
		});
	}
	public Future<Boolean> endBatchWrite() {
		return asyncWrite(new WriteTask() {
			@Override
			public boolean write(ManagedDatabase db) {
				db.endTransaction();
				return true;
			}
		});
	}
	public boolean endBatchWriteNow() {
		final Future<Boolean> f = endBatchWrite();
		while (true) try {
			try {
				final boolean succeed = f.get();
				return succeed;
			} catch (InterruptedException e) {
				
			}
		} catch (Throwable e) {
			return false;
		}
	}
	public Future<Boolean> writeObject(final String name, Serializable object) {
		final byte[] bytes = objectToBytes(object);
		return asyncWriteInTransaction(new WriteTask() {
			@Override
			public boolean write(ManagedDatabase db) {
				writeObjectBytes(db, name, bytes);
				return true;
			}
		});
	}
	public boolean writeObjectNow(String name, Serializable object) {
		final Future<Boolean> f = writeObject(name, object);
		while (true) try {
			try {
				final boolean succeed = f.get();
				return succeed;
			} catch (InterruptedException e) {
				
			}
		} catch (Throwable e) {
			return false;
		}
	}
	public <T extends Serializable> List<T> readList(String name) { 
		// TODO
		return null;
	}
	public <T extends Serializable> Future<Boolean> writeList(final String name, List<T> list) {
		// TODO
		return null;
	}
	public <T extends Serializable> T readListItem(final String name, int index) {
		// TODO
		return null;
	}
	public Future<Boolean> addListItem(final String name, Serializable item) {
		// TODO
		return null;
	}
	public Future<Boolean> addListItem(final String name, int index, Serializable item) {
		// TODO
		return null;
	}
	public Future<Boolean> replaceListItem(final String name, int index, Serializable item) {
		// TODO
		return null;
	}
	public Future<Boolean> eraseListItem(final String name, int index) {
		// TODO
		return null;
	}
	public List<String> listRecords() {
		final LinkedList<String> recordList = new LinkedList<String>();
		fillObjectNames(mDb, recordList);
		fillListNames(mDb, recordList);
		fillMapNames(mDb, recordList);
		return recordList;
	}
	public Future<Boolean> eraseRecord(final String name) {
		return asyncWriteInTransaction(new WriteTask() {
			@Override
			public boolean write(ManagedDatabase db) {
				eraseRecordData(db, name);
				return true;
			}
		});
	}
	public boolean eraseRecordNow(String name) {
		final Future<Boolean> f = eraseRecord(name);
		while (true) try {
			try {
				final boolean succeed = f.get();
				return succeed;
			} catch (InterruptedException e) {
				
			}
		} catch (Throwable e) {
			return false;
		}
	}
	public Future<Boolean> eraseAll() {
		return null;
	}
	public boolean eraseAllNow() {
		return false;
	}
	
	// ### 实现方法 ###
	private Future<Boolean> asyncWriteInTransaction(final WriteTask task) {
		return asyncWrite(new WriteTask() {
			@Override
			public boolean write(ManagedDatabase db) {
				db.beginTransaction();
				try {
					final boolean succeed = task.write(db);
					db.setTransactionSuccessful();
					return succeed;
				} catch (Throwable e) {
					return false;
				} finally {
					db.endTransaction();
				}
			}
		});
	}
	private Future<Boolean> asyncWrite(final WriteTask task) {
		return mDbWriter.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				try {
					return task.write(mDb);
				} catch (Throwable e) {
					return false;
				}
			}
		});
	}
	private static byte[] readObjectBytes(ManagedDatabase db, String name) {
		final String sqlQuery = String.format(Locale.getDefault(), "SELECT %2$s FROM %1$s WHERE %3$s == '%4$s'",
				ObjectTable.TABLE_NAME,
				ObjectTable.Columns.OBJECT_STREAM,
				ObjectTable.Columns.OBJECT_NAME,
				name);
		Cursor cursor = null;
		try {
			cursor = db.rawQuery(sqlQuery, new String[0]);
			if (cursor.moveToFirst()) {
 				final byte[] bytes = cursor.getBlob(0);
 				return bytes;
			}
			
		} catch (Throwable e) {
			e.printStackTrace();
			
		} finally {
			if (cursor != null) try {
				cursor.close();
			} catch (Throwable e) {

			}
		}

		return null;
	}
	private static void writeObjectBytes(ManagedDatabase db, String name, byte[] bytes) {
		if (bytes == null) 
			new RuntimeException();
		
		final ContentValues values = new ContentValues();
		values.put(ObjectTable.Columns.OBJECT_NAME, name);
		values.put(ObjectTable.Columns.OBJECT_STREAM, bytes);
		db.insertWithOnConflict(ObjectTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}
	private static void fillObjectNames(ManagedDatabase db, List<String> nameList) {
		final String sqlQuery = String.format(Locale.getDefault(), "SELECT %2$s FROM %1$s",
				ObjectTable.TABLE_NAME,
				ObjectTable.Columns.OBJECT_NAME);
		Cursor cursor = null;
		try {
			cursor = db.rawQuery(sqlQuery, new String[0]);
			while (cursor.moveToNext()) {
				nameList.add(cursor.getString(0));
			}
			
		} catch (Throwable e) {
			e.printStackTrace();
			
		} finally {
			if (cursor != null) try {
				cursor.close();
			} catch (Throwable e) {

			}
		}
	}
	private static void eraseRecordData(ManagedDatabase db, String name) {
		final String sqlDelObj = String.format(Locale.getDefault(), "DELETE FROM %1$s WHERE %2$s == '%3$s'", 
				ObjectTable.TABLE_NAME,
				ObjectTable.Columns.OBJECT_NAME,
				name);
		db.execSQL(sqlDelObj);
		
		final String sqlDelList = "DROP TABLE IF EXISTS " + ListTable.TABLE_NAME(name);
		db.execSQL(sqlDelList);
		
		final String sqlDelMap = "DROP TABLE IF EXISTS " + MapTable.TABLE_NAME(name);
		db.execSQL(sqlDelMap);
	}
	private static byte[] readListItemBytes(ManagedDatabase db, int index) {
		// TODO
		return null;
	}
	private static void writeListItemBytes(ManagedDatabase db, int index, byte[] bytes) {
		// TODO
	}
	private static void fillListNames(ManagedDatabase db, List<String> nameList) {
		final String sqlQuery = String.format(Locale.getDefault(), "SELECT name FROM sqlite_master WHERE type='table' AND name GLOB 'list-*'");
		Cursor cursor = null;
		try {
			cursor = db.rawQuery(sqlQuery, new String[0]);
			while (cursor.moveToNext()) {
				nameList.add(cursor.getString(0));
			}
			
		} catch (Throwable e) {
			e.printStackTrace();
			
		} finally {
			if (cursor != null) try {
				cursor.close();
			} catch (Throwable e) {

			}
		}
	}
	private static byte[] readMapItemBytes(ManagedDatabase db, String key) {
		// TODO
		return null;
	}
	private static void writeMapItemBytes(ManagedDatabase db, String key, byte[] bytes) {
		// TODO
	}
	private static void fillMapNames(ManagedDatabase db, List<String> nameList) {
		final String sqlQuery = String.format(Locale.getDefault(), "SELECT name FROM sqlite_master WHERE type='table' AND name GLOB 'map-*'");
		Cursor cursor = null;
		try {
			cursor = db.rawQuery(sqlQuery, new String[0]);
			while (cursor.moveToNext()) {
				nameList.add(cursor.getString(0));
			}
			
		} catch (Throwable e) {
			e.printStackTrace();
			
		} finally {
			if (cursor != null) try {
				cursor.close();
			} catch (Throwable e) {

			}
		}
	}
	private static byte[] objectToBytes(Serializable object) {
		byte[] objBytes = null;
		ObjectOutputStream outObjStream = null;
		try {
			final ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
			outObjStream = new ObjectOutputStream(outByteStream);
			outObjStream.writeObject(object);
			objBytes = outByteStream.toByteArray();
			
		} catch (Throwable e) {
			e.printStackTrace();
			
		} finally {
			if (outObjStream != null) try {
				outObjStream.close();
				
			} catch (Throwable e) {
				
			}
		}

		return objBytes;
	}
	@SuppressWarnings("unchecked")
	private static <T extends Serializable> T objectFromBytes(byte[] bytes) {
		ObjectInputStream inObjStream = null;
		try {
			inObjStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
			return (T) inObjStream.readObject();
			
		} catch (Throwable e) {
			e.printStackTrace();
			
		} finally {
			if (inObjStream != null) try {
				inObjStream.close();
			} catch (Throwable e) {
				
			}
		}

		return null;
	}
	private static void createObjectTable(ManagedDatabase db) {
		final String sqlCreate = String.format(Locale.getDefault(), "CREATE TABLE IF NOT EXISTS %1$s("
				+ "%2$s TEXT PRIMARY KEY, "
				+ "%3$s BLOB)",
				ObjectTable.TABLE_NAME,
				ObjectTable.Columns.OBJECT_NAME,
				ObjectTable.Columns.OBJECT_STREAM);
		db.execSQL(sqlCreate);
	}
	
	// ### 内嵌类 ###
	private static class ObjectTable {
		public static final String TABLE_NAME = "objects";

		public static class Columns {
			public static final String OBJECT_NAME = "name";
			public static final String OBJECT_STREAM = "stream";
		}
	}
	private static class ListTable {
		public static String TABLE_NAME(String name) {
			return String.format(Locale.getDefault(), "'list-%s'", name);
		}
		public static class Columns {
			public static final String ITEM_INDEX = "index";
			public static final String ITEM_STREAM = "stream";
		}
	}
	private static class MapTable {
		public static String TABLE_NAME(String name) {
			return String.format(Locale.getDefault(), "'map-%s'", name);
		}
		public static class Columns {
			public static final String ITEM_KEY = "key";
			public static final String ITEM_STREAM = "stream";
		}
	}
	private static interface WriteTask {
		boolean write(ManagedDatabase db);
	}
}

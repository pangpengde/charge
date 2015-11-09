package com.common.database;

import com.common.sys.ThreadSafe;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by pangpengde on 15/8/13.
 */
public abstract class AsyncDatabase implements ThreadSafe {

    private static final int DB_VERSION_FIRST = 1;
    protected static final ScheduledExecutorService mDbWriter = Executors.newSingleThreadScheduledExecutor();
    protected final ManagedDatabase mDb;

    // ### 构造函数 ###
    public AsyncDatabase(String archiveUri) {
        this(archiveUri, "");
    }
    public AsyncDatabase(String archiveUri, String backupUri) {
        mDb = new ManagedDatabase(archiveUri, backupUri);

        asyncWriteInTransaction(new WriteTask() {
            @Override
            public boolean write(ManagedDatabase db) {
                if (db.getVersion() < DB_VERSION_FIRST) {
                    createTable(db);
                    db.setVersion(DB_VERSION_FIRST);
                }
                return true;
            }
        });
    }

    // ### 方法 ###
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

    // ### 实现方法 ###
    protected abstract void createTable(ManagedDatabase db);

    protected Future<Boolean> asyncWriteInTransaction(final WriteTask task) {
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
    protected Future<Boolean> asyncWrite(final WriteTask task) {
        return mDbWriter.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    return task.write(mDb);
                } catch (Throwable e) {
                    e.printStackTrace();
                    return false;
                }
            }
        });
    }

    // ### 内部类 ###
    protected static interface WriteTask {
        boolean write(ManagedDatabase db);
    }
}

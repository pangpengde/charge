package com.px.charge.dao;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.common.database.AsyncDatabase;
import com.common.database.ManagedDatabase;
import com.common.sys.MainThread;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by pangpengde on 15/7/26.
 */
public class AccountBook extends AsyncDatabase {
    // ### 值域 ###
    private static AccountBook mSingleton = null;
    private Context mContent;
    private final LinkedList<Charge> mChargeList = new LinkedList<Charge>();
    private final LinkedList<AccountBookChangedListener> mListeners = new LinkedList<AccountBookChangedListener>();

    public static interface AccountBookChangedListener {
        void onAccountBookChanged();
    }

    // ### 构造函数 ###
    public AccountBook(Context context) {
        super(Uri.fromFile(new File(context.getDatabasePath("name").getParentFile(), "main.db")).toString());
        mContent = context;
    }

    // ### 方法 ###
    public synchronized static void startUp(Context context) {
        mSingleton = new AccountBook(context);
    }
    public static AccountBook get() {
        return mSingleton;
    }

    public void removeListener(AccountBookChangedListener listener) {
        mListeners.remove(listener);
    }
    public void addListener(AccountBookChangedListener listener) {
        mListeners.add(listener);
    }

    public void add(final String title, final float price, final String des) {
        final Charge charge = new Charge(title, price, des);
        mChargeList.add(charge);
        asyncWrite(new WriteTask() {
            @Override
            public boolean write(ManagedDatabase db) {
                String sql = String.format("insert into charge values(null, %d, %d, \'%s\', %.2f, \'%s\')", new Date().getTime(), new Date().getTime(), title, price, des);
                Log.w("", "px sql " + sql);
                db.execSQL(sql);
                notifyListeners();
                return true;
            }
        });
    }
    public Observable query(final int index, final int pageCount) {
        return Observable.create(new Observable.OnSubscribe<List<? extends Charge>>() {
            @Override
            public void call(final Subscriber<? super List<? extends Charge>> subscriber) {
                    try {
                        String sql = String.format("select * from charge order by _id desc limit %d offset %d", pageCount, index);
                        Log.w("", "px sql " + sql);
                        Cursor cursor = mDb.rawQuery(sql, null);
                        LinkedList<Charge> chargeList = new LinkedList<Charge>();
                        if (cursor.moveToFirst()) {
                            do {
                                final long id = cursor.getLong(cursor.getColumnIndex("_id"));
                                final Date paidDate = new Date(cursor.getLong(cursor.getColumnIndex("paid_date")));
                                final Date createDate = new Date(cursor.getLong(cursor.getColumnIndex("create_date")));
                                final String title = cursor.getString(cursor.getColumnIndex("title"));
                                final float number = cursor.getFloat(cursor.getColumnIndex("number"));
                                final String des = cursor.getString(cursor.getColumnIndex("description"));
                                chargeList.add(new Charge(id, paidDate, createDate, title, number, des));
                            } while (cursor.moveToNext());
                        }
                        subscriber.onNext(chargeList);
                        subscriber.onCompleted();
                    } catch (final Throwable e) {
                        subscriber.onError(e);
                    }
                }
            }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.from(mDbWriter));
    }

    // ### 实现函数 ###
    @Override
    protected void createTable(ManagedDatabase db) {
        db.execSQL("create table charge(_id integer primary key autoincrement, paid_date integer, create_date integer, title text, number real, description text)");
    }
    private void notifyListeners() {
        MainThread.runLater(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mListeners.size(); i++) {
                    mListeners.get(i).onAccountBookChanged();
                }
            }
        });
    }
}

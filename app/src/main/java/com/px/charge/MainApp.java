package com.px.charge;

import android.app.Application;

import com.github.mmin18.layoutcast.LayoutCast;
import com.px.charge.dao.AccountBook;

/**
 * Created by pangpengde on 15/7/26.
 */
public class MainApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        LayoutCast.init(this);

        AccountBook.startUp(this);
    }
}

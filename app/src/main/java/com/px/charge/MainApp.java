package com.px.charge;

import android.Manifest;
import android.os.Environment;
import android.support.annotation.RequiresPermission;

import com.common.sys.MyApp;
import com.px.charge.dao.AccountBook;

import java.io.File;

/**
 * Created by pangpengde on 15/7/26.
 */
public class MainApp extends MyApp {

    @Override
    public void onCreate() {
        super.onCreate();

        // TODO log系统
        // 1crash save
        // 2anr save
        // 3custom save
        // 4debug save

        AccountBook.startUp(this);
    }

    @Override
    public String getAppName() {
        return "Charge";
    }

    @RequiresPermission(allOf = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE})
    @Override
    public File getRootFile() {
        return Environment.getExternalStorageDirectory();
    }
}

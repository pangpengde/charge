package com.common.sys;

import android.app.Application;
import android.os.Environment;

import java.io.File;

/**
 * Created by pangpengde on 15/11/5.
 */
public abstract class MyApp extends Application {
    // ### 值域 ###
    private final File mDiagnosticDir;

    // ### 构造函数 ###
    public MyApp() {
        mDiagnosticDir = new File(Environment.getExternalStorageDirectory(), getAppName() + "/Diagnostic");
    }

    // ### 重载 ###
    @Override
    public void onCreate() {
        super.onCreate();

        Debugger.startup(this, getDiagnosticDirectory());

        Env.startup(this, getRootFile());
        // TODO 网络
        Persistence.startup(this, Env.get(), Debugger.get());
        // TODO Account

    }

    // ### 需要重载的反法 ###
    public abstract String getAppName();
    public abstract File getRootFile();


    // ### 属性 ###
    public File getDiagnosticDirectory() {
        return mDiagnosticDir;
    }
}

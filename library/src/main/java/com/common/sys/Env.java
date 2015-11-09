package com.common.sys;

import java.io.File;

/**
 * Created by pangpengde on 15/11/5.
 */
// TODO 获取文件的时候直接ensure可能会导致速度变慢
public class Env implements ThreadSafe {
    // ### 值域 ###
    private static Env mSingleton = null;
    private final MyApp mApp;
    private final String mAppName;
    private final File mRootFile;

    // ### 构造函数 ###
    private Env(MyApp app, File rootFile) {
        mApp = app;
        mAppName = mApp.getAppName();
        mRootFile = new File(rootFile, mAppName);
    }

    // ### 静态方法 ###
    public static void startup(MyApp app, File rootFile) {
        mSingleton = new Env(app, rootFile);
    }
    public static Env get() {
        return mSingleton;
    }

    // ### 方法 ###
    public File getDebugDir() {
        File file = new File(mRootFile, "Diagnostic");
        ensureDirectoryExists(file);
        return file;
    }

    // ### 实现函数 ###
    private void ensureDirectoryExists(File directory) {
        if (directory != null && directory.exists() == false) {
            directory.mkdirs();
        }
    }
}

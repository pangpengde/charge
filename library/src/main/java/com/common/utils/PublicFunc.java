package com.common.utils;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Created by pangpengde on 15/11/6.
 */
public abstract class PublicFunc {

    public static String getVersionName(Context context) {
        final PackageManager pm = context.getPackageManager();
        try {
            return pm.getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Throwable e) {

            return "";
        }
    }
    public static int getVersionCode(Context context) {
        final PackageManager pm = context.getPackageManager();
        try {
            return pm.getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (Throwable e) {

            return 0;
        }
    }
}

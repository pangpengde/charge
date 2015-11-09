package com.common.sys;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Created by pangpengde on 15/11/6.
 */
public class Persistence implements ThreadSafe, SharedPreferences {
    // ### 值域 ###
    private static final String TAG = "Persistence";
    private static final int LOG_LEVEL = Debugger.LOG_CONSOLE;
    protected static Persistence mSingleton = null;
    private final Context mApp;
    private final Debugger mDebugger;
    private final SharedPreferences mSp;
    private final Editor mEditor;

    // ### 构造函数 ###
    private Persistence(MyApp app, Env env, Debugger debugger) {
        mApp = app;
        mDebugger = debugger;
        mSp = mApp.getSharedPreferences(TAG, Context.MODE_PRIVATE);
        mEditor = new Editor(mSp.edit());
    }

    // ### 静态方法 ###
    public static void startup(MyApp app, Env env, Debugger debugger) {
        mSingleton = new Persistence(app, env, debugger);
    }
    public static Persistence get() {
        return mSingleton;
    }

    // ### 方法 ###
    @Override
    public Map<String, ?> getAll() {
        return mSp.getAll();
    }

    @Nullable
    @Override
    public String getString(String key, String defValue) {
        return mSp.getString(key, defValue);
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return mSp.getStringSet(key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return mSp.getInt(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return mSp.getLong(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return mSp.getFloat(key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return mSp.getBoolean(key, defValue);
    }

    @Override
    public boolean contains(String key) {
        return mSp.contains(key);
    }

    @Override
    public Editor edit() {
        return mEditor;
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        // TODO 转屏幕后这个listener会无效, 可以考虑这里加上强引用
        mSp.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mSp.unregisterOnSharedPreferenceChangeListener(listener);
    }

    // ### 实现函数 ###
    private void log(String format, Object... objects) {
        mDebugger.log(TAG, LOG_LEVEL, String.format(format, objects));
    }

    public final class Editor implements SharedPreferences.Editor {
        private final SharedPreferences.Editor mProxy;
        public Editor(SharedPreferences.Editor proxy) {
            mProxy = proxy;
        }
        @Override
        public SharedPreferences.Editor putString(String key, String value) {
            log("putString key:%s ; value:%s ;", key, value);
            return mProxy.putString(key, value);
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
            log("putStringSet key:%s ; value:%s ;", key, values.toString());
            return mProxy.putStringSet(key, values);
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            log("putInt key:%s ; value:%s ;", key, String.valueOf(value));
            return mProxy.putInt(key, value);
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            log("putLong key:%s ; value:%s ;", key, String.valueOf(value));
            return mProxy.putLong(key, value);
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            log("putFloat key:%s ; value:%s ;", key, String.valueOf(value));
            return mProxy.putFloat(key, value);
        }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) {
            log("putBoolean key:%s ; value:%s ;", key, String.valueOf(value));
            return mProxy.putBoolean(key, value);
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            log("remove key:%s ;", key);
            return mProxy.remove(key);
        }

        @Override
        public SharedPreferences.Editor clear() {
            log("clear ;");
            return mProxy.clear();
        }

        @Override
        public boolean commit() {
            log("commit ;");
            return mProxy.commit();
        }

        @Override
        public void apply() {
            log("apply ;");
            mProxy.apply();
        }
    }
}

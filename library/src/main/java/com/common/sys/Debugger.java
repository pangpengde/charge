package com.common.sys;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by pangpengde on 15/11/5.
 */
public class Debugger extends Logger {
    protected static Debugger mSingleton = null;
    private final Thread.UncaughtExceptionHandler mNextExceptionHandler;
    private final Thread mMonitorThread;

    protected Debugger(MyApp app, File rootDir) {
        super(app, rootDir);
        mNextExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                logThrowable("crash", LOG_PERSISTENCE | LOG_SERVER, "crash detected!", ex);

                if (mNextExceptionHandler != null) {
                    mNextExceptionHandler.uncaughtException(thread, ex);
                }
            }
        });

        mMonitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                monitorThreadCore();
            }
        });
        mMonitorThread.setDaemon(true);
        mMonitorThread.start();
    }

    // ### 静态方法 ###
    public static void startup(MyApp app, File rootDir) {
        mSingleton = new Debugger(app, rootDir);
    }
    public static Debugger get() {
        return mSingleton;
    }

    // ### 实现函数 ###
    private void monitorThreadCore() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Semaphore response = new Semaphore(0);
        while (true) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    response.release();
                }
            });

            try {
                Thread.sleep(2000);

                if (response.tryAcquire(3000, TimeUnit.MILLISECONDS) == false) {
                    logAllStackTraces("anr", LOG_PERSISTENCE | LOG_SERVER, "ANR detected!");
                }

                response.drainPermits();

            } catch (InterruptedException e) {
                break;

            } catch (Throwable e) {

            }
        }
    }
}

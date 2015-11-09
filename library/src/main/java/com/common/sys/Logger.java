package com.common.sys;

import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by pangpengde on 15/11/5.
 */
public class Logger implements ThreadSafe {
    // ### 值域 ###
    protected static final boolean DEBUG = Constans.DEBUG | true;
    public static final int LOG_CONSOLE = 1 << 0;
    public static final int LOG_PERSISTENCE = 1 << 1;
    public static final int LOG_SERVER = 1 << 2;
    protected final MyApp mApp;
    private final Object mLock;
    private final File mDumpRootDir;
    private final String TAG;

    // ### 接口 ###
    public interface Reporter {
        void report(String log);
    }

    // ### 构造函数 ###
    protected Logger(MyApp app, File rootDir) {
        mApp = app;
        mLock = this;
        mDumpRootDir = rootDir;

        TAG = mApp.getAppName();
    }

    // ### 方法 ###
    public void logAllStackTraces(String tag, int level, String header) {
        final Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        for (Map.Entry<Thread, StackTraceElement[]> each : traces.entrySet()) {
            final Thread thread = each.getKey();
            final StackTraceElement[] elements = each.getValue();

            pw.println(thread.toString());
            for (StackTraceElement element : elements) {
                pw.print("\t");
                pw.println(element.toString());
            }
        }

        pw.flush();
        pw.close();
        printMultiLines(tag, level, header, sw.toString());
    }
    public void logThrowable(String tag, int level, String header, Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        throwable.printStackTrace(pw);
        pw.flush();
        pw.close();
        printMultiLines(tag, level, header, sw.toString());
    }
    public void printMultiLines(String tag, int level, String header, String multiLines) {
        final String msg = String.format(Locale.getDefault(), ">>>%s\n%s\n<<<", header, multiLines);
        printLine(tag, level, msg);
    }
    public void printLine(String tag, int level, String msg) {
        final LogEntry entry = new LogEntry(tag, msg);

        // TODO crash
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        final String date = dateFormat.format(new Date(System.currentTimeMillis()));
        final String fileName = String.format(Locale.US, "debug.%s.%d.log", date, Process.myPid());
        dumpNow(entry, new File(mDumpRootDir, fileName));
        report(level, msg);
    }
    private static void doDump(LogEntry entry, File file) {
        if (file == null)
            return;

        FileOutputStream fileStream = null;
        try {
            final File dir = file.getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }

            fileStream = new FileOutputStream(file, true);
            final BufferedOutputStream bufStream = new BufferedOutputStream(fileStream);
            final OutputStreamWriter streamWriter = new OutputStreamWriter(bufStream, "utf-8");
            final PrintWriter printWriter = new PrintWriter(streamWriter, false);

            printWriter.println(entry.toString());

            printWriter.flush();
            printWriter.close();
            fileStream = null;

        } catch (Throwable e) {
            e.printStackTrace();

        } finally {
            if (fileStream != null) try {
                fileStream.close();
                fileStream = null;
            } catch (Throwable e) {

            }
        }
    }
    public void dump(final LogEntry entry, final File file) {
        synchronized (mLock) {

            PooledThread.runInQueue(new Runnable() {
                @Override
                public void run() {
                    doDump(entry, file);
                }
            }, TAG);
        }
    }
    public void dumpNow(final LogEntry entry, final File file) {
        final Future<?> future;
        synchronized (mLock) {

            future = PooledThread.runInQueue(new Runnable() {
                @Override
                public void run() {
                    doDump(entry, file);
                }
            }, TAG);
        }

        try {
            future.get();

        } catch (Throwable e) {

        }
    }
    public void log(String log) {
        log(LOG_CONSOLE, log);
    }
    public void log(int level, String log) {
        log(TAG, level, log);
    }
    public void log(String tag, int level, String log) {
        if ((level & LOG_CONSOLE) == LOG_CONSOLE && DEBUG) {
            Log.d(tag, log);
        }
        dumpToFile(level, log);
        report(level, log);
    }

    // ### 实现函数 ###
    private void dumpToFile(int level, String log) {
        if ((level & LOG_PERSISTENCE) == LOG_PERSISTENCE) {

        }
    }
    private void report(int level, String log) {
        if ((level & LOG_SERVER) == LOG_SERVER) {
            Event.report(log);
        }
    }

    // ### 内部类 ###
    private static class LogEntry {
        private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        private final Thread mThread;
        private final long mTimeStamp;
        private final String mTag;
        private final String mMessage;

        public LogEntry(@NonNull String tag, @NonNull String msg) {
            mThread = CurrentThread.get();
            mTimeStamp = System.currentTimeMillis();
            mTag = tag;
            mMessage = msg;
        }

        @Override
        public String toString() {
            final String date = mDateFormat.format(new Date(mTimeStamp));
            final String text = String.format(Locale.getDefault(), "[%s]%s //@%s, %s",
                    mTag, mMessage, mThread.toString(), date);
            return text;
        }
    }
}

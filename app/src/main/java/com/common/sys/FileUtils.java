package com.common.sys;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

public class FileUtils {
    private static final String ENV_EXTERNAL_STORAGE = "EXTERNAL_STORAGE";
    private static final String ENV_SECONDARY_STORAGE = "SECONDARY_STORAGE";
    private static final String ENV_EMULATED_STORAGE_TARGET = "EMULATED_STORAGE_TARGET";
    private static final String DEFAULT_EXTERNAL_STORAGE_PATH = "/storage/sdcard0";
    private static final String MOUNT_PATH = "/mnt";
    private static final String STORAGE_PATH = "/storage";

    public static File[] getExternalStorageDirectories(Context context) {
        final File[] dirsFromContext = getExternalStoragesByContext(context);
        final File[] dirsFromScaning = getExternalStoragesByScaning();
        final File[] dirsFromEnvironment = getExternalStoragesByEnvironment();

        // 从上面三个结果中取扫描处的外存设备最多的方法
        File[] maxCountDirs = dirsFromContext;
        if (maxCountDirs.length < dirsFromScaning.length) {
            maxCountDirs = dirsFromScaning;
        }
        if (maxCountDirs.length < dirsFromEnvironment.length) {
            maxCountDirs = dirsFromEnvironment;
        }

        // 某些奇葩手机就算没挂载SD卡也会把容量为0的目录挂载为外存!!!!!!
        final List<File> externalStorageDirs = new ArrayList<File>();
        for (File externalStorageDir : maxCountDirs) {
            if (0 != externalStorageDir.getTotalSpace()) {
                externalStorageDirs.add(externalStorageDir);
            }
        }
        return externalStorageDirs.toArray(new File[externalStorageDirs.size()]);

    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static long getFreeSpace(File dir) {
        if (dir != null && dir.isDirectory()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                return dir.getFreeSpace();
            }
            StatFs sf = new StatFs(dir.getPath());
            @SuppressWarnings("deprecation")
            long blockSize = sf.getBlockSize();
            @SuppressWarnings("deprecation")
            long availCount = sf.getAvailableBlocks();
            return blockSize * availCount;
        }
        return -1;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static long getTotalSpace(File dir) {
        if (dir != null) {
            if (Build.VERSION.SDK_INT >= 9) {
                return dir.getTotalSpace();
            }
            StatFs sf = new StatFs(dir.getPath());
            @SuppressWarnings("deprecation")
            long blockSize = sf.getBlockSize();
            @SuppressWarnings("deprecation")
            long blockCount = sf.getBlockCount();
            return blockSize * blockCount;
        }
        return -1;
    }

    public static boolean copyFile(File srcFile, File dstFile) {
        RandomAccessFile src = null;
        RandomAccessFile dst = null;
        try {
            src = new RandomAccessFile(srcFile, "r");
            dst = new RandomAccessFile(dstFile, "rws");
            final FileChannel srcChannel = src.getChannel();
            final FileChannel dstChannel = dst.getChannel();

            final long total = srcChannel.size();
            long transferred = 0;
            srcChannel.position(0);
            dstChannel.position(0);
            for (; transferred < total; ) {
                final long bytes = dstChannel.transferFrom(srcChannel, transferred, total - transferred);
                if (bytes <= 0) // 容错处理, 防止出现无限循环.
                    break;

                transferred += bytes;
            }

            if (transferred == total) {
                dstChannel.truncate(transferred);
                dstChannel.force(true);
                return true;

            } else {
                return false;
            }

        } catch (Throwable e) {

            return false;
        } finally {
            try {
                if (src != null)
                    src.close();
                if (dst != null)
                    dst.close();

            } catch (Throwable e) {

            }
        }
    }

    public static boolean saveAsFile(InputStream inputStream, File dstFile) {
        File tmpFile = null;
        FileOutputStream fileStream = null;
        ;
        try {
            tmpFile = File.createTempFile("" + System.currentTimeMillis(), ".tmp", dstFile.getParentFile());
            fileStream = new FileOutputStream(tmpFile);
            final byte[] bytes = new byte[1024];
            int readBytes = 0;
            while (true) {
                readBytes = inputStream.read(bytes, 0, 1024);
                if (readBytes <= 0)
                    break;
                fileStream.write(bytes, 0, readBytes);
            }
            fileStream.flush();
            fileStream.getFD().sync();

            if (dstFile.exists() && dstFile.delete() == false) {
                return false;
            }
            return tmpFile.renameTo(dstFile);
        } catch (IOException ex) {
            return false;
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (Exception e) {
                }
            }

            if (tmpFile != null && tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }

    public static boolean safeDelete(File dirOrFile) {
        if (dirOrFile == null)
            return true;

        if (dirOrFile.exists() == false)
            return true;

        File tmpFile = new File(dirOrFile.getAbsoluteFile() + ".tmp");
        for (int r = 0; r < 10; ++r) {
            if (dirOrFile.renameTo(tmpFile)) {
                return delete(tmpFile);
            }

            tmpFile = new File(tmpFile.getAbsoluteFile() + ".tmp");
            Thread.yield();
        }

        return delete(dirOrFile);
    }

    public static boolean delete(File dirOrFile) {
        if (dirOrFile == null)
            return true;

        if (dirOrFile.exists() == false)
            return true;

        if (dirOrFile.isFile())
            return dirOrFile.delete();

        final File[] children = dirOrFile.listFiles();
        if (children == null)
            return false;

        boolean deleted = true;
        for (File each : children) {
            deleted &= delete(each);
        }

        deleted &= dirOrFile.delete();
        return deleted;
    }

    public static String readSmallTxtFile(InputStream stream) throws IOException {
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            final byte[] buffer = new byte[4096];
            while (true) {
                int len = stream.read(buffer);
                if (len == -1) {
                    break;
                } else {
                    outStream.write(buffer, 0, len);
                }
            }
            outStream.close();
            return outStream.toString("UTF-8");
        } finally {
            outStream.close();
        }
    }

    // 实现函数
    @TargetApi(19)
    private static File[] getExternalStoragesByContext(Context context) {
        // google为4.4以上版本提供的API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final File primaryDir = context.getExternalFilesDir(null);
            final File[] externalDirs = context.getExternalFilesDirs(null);

            final ArrayList<File> dirList = new ArrayList<File>(Math.max(externalDirs.length, 1));
            if (primaryDir != null && externalDirs != null) {
                for (File each : externalDirs) {
                    if (each == null)
                        continue; // 外存储卡未挂载

                    if (each.getAbsolutePath().equals(primaryDir.getAbsolutePath())) {
                        dirList.add(Environment.getExternalStorageDirectory());
                    } else {
                        dirList.add(each);
                    }
                }
            }

            if (dirList.size() < 1) {
                // 主存储卡可能未挂载, 或者系统异常...
                dirList.add(Environment.getExternalStorageDirectory());
            }

            return dirList.toArray(new File[0]);
        } else {
            return new File[0];
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static File[] getExternalStoragesByScaning() {
        List<File> scanPaths = new ArrayList<File>();
        Map<Long, File> scanedPathsMap = new HashMap<Long, File>();
        scanPaths.add(new File(MOUNT_PATH));
        scanPaths.add(new File(STORAGE_PATH));

        for (int currentIndex = 0; currentIndex < scanPaths.size(); currentIndex++) {
            File scanningPath = scanPaths.get(currentIndex);
            File[] subFiles = scanningPath.listFiles();

            if (!scanningPath.canWrite() && subFiles != null && subFiles.length > 0) {
                // 不能写的目录肯定不是外存，故将其子目录添加到扫描列表
                scanPaths.addAll(Arrays.asList(subFiles));
            } else if (scanningPath.canWrite() && 0 != scanningPath.getTotalSpace()) {
                // 如果两个外存设备的总空间与可用空间都一样，则认为它们是同一个
                long key = scanningPath.getTotalSpace();
                scanedPathsMap.put(key, scanningPath);
            }
        }

        Collection<File> scanedPaths = scanedPathsMap.values();
        return scanedPathsMap.size() > 0 ? scanedPaths.toArray(new File[scanedPaths.size()]) : new File[0];
    }

    private static File[] getExternalStoragesByEnvironment() {
        final HashSet<String> pathSet = new HashSet<String>();
        final String externalStorageValue = System.getenv(ENV_EXTERNAL_STORAGE);
        final String secondaryStorageValue = System.getenv(ENV_SECONDARY_STORAGE);
        final String emulatedTargetValue = System.getenv(ENV_EMULATED_STORAGE_TARGET);
        if (TextUtils.isEmpty(emulatedTargetValue)) {
            if (TextUtils.isEmpty(externalStorageValue)) {
                pathSet.add(DEFAULT_EXTERNAL_STORAGE_PATH);
            } else {
                pathSet.add(externalStorageValue);
            }
        } else {
            final String userId;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                userId = "";
            } else {
                final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                final String[] segs = path.split("" + File.separatorChar);
                final String lastSeg = segs[segs.length - 1];
                boolean isDigit = false;
                try {
                    Integer.valueOf(lastSeg);
                    isDigit = true;
                } catch (NumberFormatException ignored) {

                }
                userId = isDigit ? lastSeg : "";
            }
            // /storage/emulated/0[1,2,...]
            if (TextUtils.isEmpty(userId)) {
                pathSet.add(emulatedTargetValue);
            } else {
                pathSet.add(emulatedTargetValue + File.separator + userId);
            }
        }
        if (!TextUtils.isEmpty(secondaryStorageValue)) {
            // All Secondary SD-CARDs splited into array
            final String[] rawSecondaryStorages = secondaryStorageValue.split(File.pathSeparator);
            Collections.addAll(pathSet, rawSecondaryStorages);
        }

        final String[] paths = pathSet.size() > 0 ? pathSet.toArray(new String[0]) : new String[]{Environment.getExternalStorageDirectory().getAbsolutePath()};
        assert paths.length > 0;

        final ArrayList<File> dirList = new ArrayList<File>(paths.length);
        for (int n = 0; n < paths.length; ++n) {
            final File dir = new File(paths[n]);
            if (dir.canRead() && dir.canWrite()) {
                dirList.add(dir);
            }
        }
        return dirList.toArray(new File[0]);
    }
}

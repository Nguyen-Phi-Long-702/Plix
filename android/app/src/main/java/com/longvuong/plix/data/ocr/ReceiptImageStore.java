package com.longvuong.plix.data.ocr;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

//Kho ảnh hoá đơn tạm trong cacheDir/receipts (SEC-10): ảnh chỉ nằm trên máy và được xoá sau khi dùng
@Singleton
public class ReceiptImageStore {

    private static final String RECEIPT_DIR = "receipts"; //khớp với <cache-path path="receipts/"> trong res/xml/file_paths.xml
    private static final String FILE_PREFIX = "receipt_";
    private static final String FILE_SUFFIX = ".jpg";

    private final Context context;

    @Inject
    public ReceiptImageStore(@ApplicationContext Context context) {
        this.context = context;
    }

    //Tạo sẵn tệp rỗng cacheDir/receipts/receipt_<millis>.jpg để ứng dụng máy ảnh ghi ảnh vào
    public File newCaptureFile() {
        try {
            File dir = receiptDir();
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Không tạo được thư mục ảnh hoá đơn tạm");
            }
            long millis = System.currentTimeMillis();
            File file = new File(dir, FILE_PREFIX + millis + FILE_SUFFIX);
            while (!file.createNewFile()) { //trùng tên trong cùng một mili giây thì tăng số lên
                millis++;
                file = new File(dir, FILE_PREFIX + millis + FILE_SUFFIX);
            }
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    //authority phải trùng android:authorities của FileProvider trong Manifest: ${applicationId}.fileprovider
    public Uri uriFor(File file) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }

    public void delete(File file) {
        file.delete();
    }

    //Xoá mọi tệp trong cacheDir/receipts, giữ lại thư mục
    public void deleteAll() {
        File[] files = receiptDir().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            file.delete();
        }
    }

    private File receiptDir() {
        return new File(context.getCacheDir(), RECEIPT_DIR);
    }
}
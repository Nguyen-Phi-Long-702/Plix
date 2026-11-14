package com.longvuong.plix.data.ocr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class ReceiptImageStoreInstrumentedTest {
    private Context context;
    private ReceiptImageStore store;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        store = new ReceiptImageStore(context);
        store.deleteAll(); //bắt đầu từ thư mục rỗng, không phụ thuộc lần chạy trước
    }

    @After
    public void tearDown() {
        store.deleteAll();
    }

    /**
     * TN2-I01
     * newCaptureFile(): tệp nằm trong cacheDir/receipts, tên bắt đầu 'receipt_' và kết thúc '.jpg'; uriFor(file) có authority '<applicationId>.fileprovider'.
     */
    @Test
    public void newCaptureFile_createsReceiptJpgInCacheReceiptsDir_andUriHasFileProviderAuthority() {
        File file = store.newCaptureFile();

        File receiptsDir = new File(context.getCacheDir(), "receipts");
        assertEquals(receiptsDir.getAbsolutePath(), file.getParentFile().getAbsolutePath());
        assertTrue(file.getName().startsWith("receipt_"));
        assertTrue(file.getName().endsWith(".jpg"));
        assertTrue(file.exists());
        assertEquals(context.getPackageName() + ".fileprovider", store.uriFor(file).getAuthority());
    }

    /**
     * TN2-I02
     * delete(file) xoá tệp; tạo 3 tệp rồi deleteAll(): sau đó thư mục receipts rỗng.
     */
    @Test
    public void deleteAndDeleteAll_removeFiles_leavingReceiptsDirEmpty() {
        File single = store.newCaptureFile();
        assertTrue(single.exists());
        store.delete(single);
        assertFalse(single.exists());

        store.newCaptureFile();
        store.newCaptureFile();
        store.newCaptureFile();
        File receiptsDir = new File(context.getCacheDir(), "receipts");
        assertEquals(3, receiptsDir.listFiles().length);

        store.deleteAll();
        File[] remaining = receiptsDir.listFiles();
        assertNotNull(remaining);
        assertEquals(0, remaining.length);
    }
}
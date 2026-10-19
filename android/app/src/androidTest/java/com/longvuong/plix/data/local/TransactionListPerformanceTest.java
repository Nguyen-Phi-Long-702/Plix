package com.longvuong.plix.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Log;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.longvuong.plix.data.local.dao.TransactionDao;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.domain.usecase.transaction.FilterTransactionsUseCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

//Đo hiệu năng truy vấn danh sách giao dịch trên Room thật (file DB riêng, không đụng tới DB thật của app), mục tiêu dưới 100 ms
//Kết quả đo được in ra Logcat với tag PlixPerf
@RunWith(AndroidJUnit4.class)
public class TransactionListPerformanceTest {
    private static final String TAG = "PlixPerf";
    private static final String DATABASE_NAME = "plix_perf_ngay50.db";
    private static final String USER_ID = "user-perf-ngay-50";
    private static final int TOTAL_TRANSACTIONS = 5000;
    private static final int RUN_COUNT = 5;
    private static final double LIMIT_MS = 100.0;

    private static final long DAY_MS = 86_400_000L;
    private static final long THREE_HOURS_MS = 3L * 3_600_000L;
    private static final long BASE_TIME = 1735689600000L; //01/01/2025 00:00 UTC, mỗi giao dịch cách nhau 3 giờ nên 5.000 giao dịch trải khoảng 21 tháng

    private static final String[] CATEGORY_IDS = {
            "sys_an_uong", "sys_di_chuyen", "sys_nhu_yeu_pham", "sys_giai_tri", "sys_hoa_don",
            "sys_suc_khoe", "sys_giao_duc", "sys_nha_o", "sys_mua_sam", "sys_khac_chi"
    };

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private Context context;
    private AppDatabase database;
    private TransactionDao transactionDao;
    private final FilterTransactionsUseCase filterTransactionsUseCase = new FilterTransactionsUseCase();

    @Before
    public void createDatabaseAndSeed() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DATABASE_NAME);
        database = Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME).allowMainThreadQueries().build();
        transactionDao = database.transactionDao();
        seedTransactions();
    }

    @After
    public void closeAndDeleteDatabase() {
        database.close();
        context.deleteDatabase(DATABASE_NAME);
    }

    //Danh sách đầy đủ, sắp xếp theo ngày giao dịch mới nhất trước (đúng truy vấn mà TransactionListViewModel đang dùng)
    @Test
    public void transactionList_5000Rows_sortedListUnder100ms() throws Exception {
        List<TransactionEntity> result = measure("getAll (sap xep occurred_at giam dan)", TOTAL_TRANSACTIONS,
                () -> getOrAwaitValue(transactionDao.getAll()));

        assertEquals("perf-tx-4999", result.get(0).id);
        assertEquals("perf-tx-0", result.get(result.size() - 1).id);
    }

    //Lọc theo danh mục + khoảng ngày: giống TransactionListViewModel (đọc getAll rồi lọc trong bộ nhớ), nên đây là mức tối đa của lần lọc đầu tiên
    @Test
    public void transactionList_filterByCategoryAndDateRange_under100ms() throws Exception {
        long startDate = BASE_TIME + 200 * DAY_MS;
        long endDate = BASE_TIME + 290 * DAY_MS;

        measure("getAll + loc danh muc + khoang ngay", 73,
                () -> filterTransactionsUseCase.execute(getOrAwaitValue(transactionDao.getAll()), CATEGORY_IDS[0], startDate, endDate));
    }

    //Tìm theo ghi chú (LIKE trong SQL) và vẫn sắp xếp theo ngày mới nhất trước
    @Test
    public void transactionList_searchByNote_under100ms() throws Exception {
        measure("searchByNote + sap xep", 111,
                () -> getOrAwaitValue(transactionDao.searchByNote("phe ngay 49")));
    }

    private void seedTransactions() {
        database.runInTransaction(() -> {
            for (int i = 0; i < TOTAL_TRANSACTIONS; i++) {
                TransactionEntity entity = new TransactionEntity();
                entity.id = "perf-tx-" + i;
                entity.userId = USER_ID;
                entity.amount = 10000L + (i % 100) * 1000L;
                entity.type = "expense";
                entity.categoryId = CATEGORY_IDS[i % CATEGORY_IDS.length];
                entity.note = "Ca phe ngay " + i;
                entity.occurredAt = BASE_TIME + i * THREE_HOURS_MS;
                entity.updatedAt = entity.occurredAt;
                entity.syncStatus = "synced";
                entity.isDeleted = false;
                transactionDao.insert(entity);
            }
        });
    }

    //Chạy 1 lần khởi động (không tính), kiểm tra số dòng trả về, rồi đo RUN_COUNT lần và so trung vị với ngưỡng
    private List<TransactionEntity> measure(String label, int expectedSize, Callable<List<TransactionEntity>> query) throws Exception {
        List<TransactionEntity> result = query.call();
        assertEquals(expectedSize, result.size());

        double[] runsMs = new double[RUN_COUNT];
        for (int i = 0; i < RUN_COUNT; i++) {
            long start = System.nanoTime();
            result = query.call();
            runsMs[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        Arrays.sort(runsMs);
        double min = runsMs[0];
        double median = runsMs[RUN_COUNT / 2];
        double max = runsMs[RUN_COUNT - 1];

        Log.i(TAG, String.format(Locale.US, "%s | %d dong | nho nhat=%.1f ms, trung vi=%.1f ms, lon nhat=%.1f ms (muc tieu < %.0f ms)",
                label, result.size(), min, median, max, LIMIT_MS));
        assertTrue(label + ": trung vi " + median + " ms, muc tieu < " + LIMIT_MS + " ms", median < LIMIT_MS);
        return result;
    }

    private <T> T getOrAwaitValue(LiveData<T> liveData) throws InterruptedException {
        final Object[] holder = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T value) {
                holder[0] = value;
                latch.countDown();
                liveData.removeObserver(this);
            }
        };
        liveData.observeForever(observer);
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("LiveData khong tra ve gia tri nao trong 10 giay.");
        }
        @SuppressWarnings("unchecked")
        T result = (T) holder[0];
        return result;
    }
}
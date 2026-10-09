package com.longvuong.plix.data.sync;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

//Race condition: đăng xuất phải chờ SyncWorker đang chạy dở kết thúc hẳn rồi mới được đi tiếp
public class SyncLockTest {

    @Test(timeout = 5000)
    public void awaitIdle_whenNobodyHoldsLock_returnsImmediately() {
        SyncLock syncLock = new SyncLock();

        syncLock.awaitIdle();
    }

    @Test(timeout = 5000)
    public void awaitIdle_whileWorkerHoldsLock_waitsUntilWorkerFinishes() throws InterruptedException {
        SyncLock syncLock = new SyncLock();
        CountDownLatch workerHasLock = new CountDownLatch(1);
        CountDownLatch letWorkerFinish = new CountDownLatch(1);
        AtomicBoolean workerFinished = new AtomicBoolean(false);
        AtomicBoolean awaitReturnedBeforeWorkerFinished = new AtomicBoolean(false);

        Thread worker = new Thread(() -> {
            syncLock.lock();
            try {
                workerHasLock.countDown();
                letWorkerFinish.await();
                workerFinished.set(true);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                syncLock.unlock();
            }
        });
        worker.start();
        workerHasLock.await();

        Thread logout = new Thread(() -> {
            syncLock.awaitIdle();
            awaitReturnedBeforeWorkerFinished.set(!workerFinished.get());
        });
        logout.start();
        Thread.sleep(100); //cho luồng đăng xuất kịp chạm vào khoá

        assertTrue("Luồng đăng xuất phải đang chờ", logout.isAlive());

        letWorkerFinish.countDown();
        logout.join();
        worker.join();

        assertFalse("Đăng xuất không được đi tiếp trước khi Worker xong", awaitReturnedBeforeWorkerFinished.get());
    }
}
package com.longvuong.plix.data.sync;

import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Singleton;

//Khoá dùng chung giữa SyncWorker và luồng đăng xuất: SyncWorker giữ khoá suốt lúc chạy, đăng xuất phải chờ lấy được khoá thì mới xoá dữ liệu cục bộ
@Singleton
public class SyncLock {
    private final ReentrantLock lock = new ReentrantLock();

    @Inject
    public SyncLock() {
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    //Chờ tới khi không còn ai giữ khoá (lượt SyncWorker đang chạy dở đã kết thúc hẳn)
    public void awaitIdle() {
        lock.lock();
        lock.unlock();
    }
}
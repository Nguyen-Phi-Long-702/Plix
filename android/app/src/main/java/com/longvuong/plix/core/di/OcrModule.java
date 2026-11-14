package com.longvuong.plix.core.di;

import com.longvuong.plix.data.ocr.MlKitReceiptOcrEngine;
import com.longvuong.plix.data.ocr.ReceiptOcrEngine;
import com.longvuong.plix.data.repository.ReceiptRepository;
import com.longvuong.plix.data.repository.ReceiptRepositoryImpl;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

//Đăng ký bộ đọc chữ và ReceiptRepository cho tính năng Quét hoá đơn (DataModule không đổi)
@Module
@InstallIn(SingletonComponent.class)
public abstract class OcrModule {

    @Binds
    abstract ReceiptOcrEngine bindReceiptOcrEngine(MlKitReceiptOcrEngine impl);

    @Binds
    abstract ReceiptRepository bindReceiptRepository(ReceiptRepositoryImpl impl);
}
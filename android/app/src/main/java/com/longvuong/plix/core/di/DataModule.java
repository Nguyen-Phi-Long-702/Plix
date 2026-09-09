package com.longvuong.plix.core.di;

import android.content.Context;

import androidx.room.Room;

import com.longvuong.plix.data.local.AppDatabase;
import com.longvuong.plix.data.local.dao.TransactionDao;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.data.repository.TransactionRepositoryImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class DataModule {

    private static final String DATABASE_NAME = "plix_database";

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME)
                .addCallback(AppDatabase.SEED_CATEGORIES_CALLBACK)
                .build();
    }

    @Provides
    @Singleton
    public TransactionDao provideTransactionDao(AppDatabase appDatabase) {
        return appDatabase.transactionDao();
    }

    @Provides
    @Singleton
    public TransactionRepository provideTransactionRepository(TransactionRepositoryImpl impl) {
        return impl;
    }
}
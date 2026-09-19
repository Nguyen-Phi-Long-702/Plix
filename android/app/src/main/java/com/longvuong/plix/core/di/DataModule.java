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
import com.longvuong.plix.data.local.dao.CategoryDao;
import com.longvuong.plix.data.repository.CategoryRepository;
import com.longvuong.plix.data.repository.CategoryRepositoryImpl;
import com.longvuong.plix.data.local.dao.BudgetDao;
import com.longvuong.plix.data.repository.BudgetRepository;
import com.longvuong.plix.data.repository.BudgetRepositoryImpl;
import com.longvuong.plix.core.notification.NotificationHelper;
import com.longvuong.plix.domain.usecase.budget.BudgetThresholdNotifier;
import com.longvuong.plix.data.local.dao.GoalDao;
import com.longvuong.plix.data.repository.GoalRepository;
import com.longvuong.plix.data.repository.GoalRepositoryImpl;

@Module
@InstallIn(SingletonComponent.class)
public class DataModule {
    private static final String DATABASE_NAME = "plix_database";

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME).addCallback(AppDatabase.SEED_CATEGORIES_CALLBACK).build();
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

    @Provides
    @Singleton
    public CategoryDao provideCategoryDao(AppDatabase appDatabase) {
        return appDatabase.categoryDao();
    }

    @Provides
    @Singleton
    public CategoryRepository provideCategoryRepository(CategoryRepositoryImpl impl) {
        return impl;
    }

    @Provides
    @Singleton
    public BudgetDao provideBudgetDao(AppDatabase appDatabase) {
        return appDatabase.budgetDao();
    }

    @Provides
    @Singleton
    public BudgetRepository provideBudgetRepository(BudgetRepositoryImpl impl) {
        return impl;
    }

    @Provides
    @Singleton
    public BudgetThresholdNotifier provideBudgetThresholdNotifier(NotificationHelper notificationHelper) {
        return notificationHelper;
    }

    @Provides
    @Singleton
    public GoalDao provideGoalDao(AppDatabase appDatabase) {
        return appDatabase.goalDao();
    }

    @Provides
    @Singleton
    public GoalRepository provideGoalRepository(GoalRepositoryImpl impl) {
        return impl;
    }
}
package com.longvuong.plix.core.di;

import com.longvuong.plix.core.auth.UserSessionCleaner;

import java.util.Set;

import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.Multibinds;

//Khai báo tập UserSessionCleaner (có thể rỗng) để LogoutRepositoryImpl inject; mỗi tính năng tự thêm bộ dọn của mình bằng @IntoSet (NF-11)
@Module
@InstallIn(SingletonComponent.class)
public abstract class SessionCleanerModule {

    @Multibinds
    abstract Set<UserSessionCleaner> cleaners();
}
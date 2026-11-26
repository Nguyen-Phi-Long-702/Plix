package com.longvuong.plix.core.di;

import com.longvuong.plix.core.auth.UserSessionCleaner;
import com.longvuong.plix.data.reminder.ReminderPreferences;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoSet;

//Đăng ký ReminderPreferences vào tập UserSessionCleaner để cấu hình nhắc nhở bị xoá khi đăng xuất (NF-11)
@Module
@InstallIn(SingletonComponent.class)
public abstract class ReminderModule {

    @Binds
    @IntoSet
    abstract UserSessionCleaner bindReminderPreferencesCleaner(ReminderPreferences impl);
}

package com.longvuong.plix.core.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.annotation.SuppressLint;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.longvuong.plix.MainActivity;
import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.domain.usecase.budget.BudgetThresholdNotifier;

import java.text.NumberFormat;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class NotificationHelper implements BudgetThresholdNotifier, NotificationPermissionChecker {
    private static final String CHANNEL_ID = "budget_warning";
    private static final String REMINDER_CHANNEL_ID = "reminders";
    private static final String PREF_NAME = "notification_prefs";
    private static final String KEY_PERMISSION_ASKED = "notification_permission_asked";

    private final Context context;
    private final MutableLiveData<Boolean> permissionRequestNeeded = new MutableLiveData<>(false);

    @Inject
    public NotificationHelper(@ApplicationContext Context context) {
        this.context = context;
        createChannelIfNeeded();
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Cảnh báo ngân sách", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Thông báo khi chi tiêu vượt ngưỡng cảnh báo của ngân sách");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                manager.createNotificationChannel(new NotificationChannel(
                        REMINDER_CHANNEL_ID, "Nhắc nhở", NotificationManager.IMPORTANCE_DEFAULT));
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void notifyThresholdCrossed(BudgetEntity budget, long spentAfterAmount) {
        if (!isNotificationPermissionGranted()) {
            requestPermissionIfNeeded();
            return;
        }
        String content = "Đã đạt " + budget.thresholdPercent + "% ngưỡng — chi tiêu "
                + formatCurrency(spentAfterAmount) + " / " + formatCurrency(budget.limitAmount);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_budget_warning)
                .setContentTitle("Cảnh báo ngân sách")
                .setContentText(content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(context).notify(budget.id.hashCode(), builder.build());
    }

    //Phát thông báo nhắc nhở trên kênh "reminders" (FIN-10, FIN-11, FIN-12); không ghi log nội dung vì có thể chứa ghi chú và số tiền (SEC-08)
    @SuppressLint("MissingPermission")
    public void notifyReminder(int notificationId, String title, String text) {
        if (!isNotificationPermissionGranted()) {
            return; //quyền bị thu hồi: không phát, không văng (DP_02 mục 3.7)
        }
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //Activity được mở từ thông báo, ngoài ngữ cảnh của một Activity
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_reminder)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    @Override
    public boolean isNotificationPermissionGranted() {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    //Nếu đang thiếu quyền thông báo thì yêu cầu hiện hộp thoại xin quyền (MainActivity quan sát LiveData để hiện); chỉ hỏi 1 lần (KEY_PERMISSION_ASKED)
    public void requestPermissionIfNeeded() {
        if (isNotificationPermissionGranted()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !hasAskedPermissionBefore()
                && !Boolean.TRUE.equals(permissionRequestNeeded.getValue())) {
            permissionRequestNeeded.postValue(true);
        }
    }

    public LiveData<Boolean> getPermissionRequestNeededLiveData() {
        return permissionRequestNeeded;
    }

    public void onPermissionRequestHandled() {
        markPermissionAsked();
        permissionRequestNeeded.postValue(false);
    }

    public Intent buildNotificationSettingsIntent() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        return intent;
    }

    private boolean hasAskedPermissionBefore() {
        return prefs().getBoolean(KEY_PERMISSION_ASKED, false);
    }

    private void markPermissionAsked() {
        prefs().edit().putBoolean(KEY_PERMISSION_ASKED, true).apply();
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private String formatCurrency(long amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ";
    }
}
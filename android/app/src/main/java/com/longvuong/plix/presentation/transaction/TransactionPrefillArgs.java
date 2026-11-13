package com.longvuong.plix.presentation.transaction;

import android.os.Bundle;

import androidx.annotation.Nullable;

//Khoá và hàm đóng gói đối số điền sẵn cho form Thêm giao dịch (FIN-20); tên khoá phải trùng tên <argument> trong nav_graph.xml
public final class TransactionPrefillArgs {
    public static final String KEY_AMOUNT = "prefillAmount"; //long, mặc định 0
    public static final String KEY_TYPE = "prefillType"; //String, mặc định rỗng ("expense" hoặc "income")
    public static final String KEY_OCCURRED_AT = "prefillOccurredAt"; //long mili giây, mặc định 0
    public static final String KEY_NOTE = "prefillNote"; //String, nullable
    public static final String KEY_CATEGORY_ID = "prefillCategoryId"; //String, nullable
    public static final String KEY_RECURRING_DAY = "prefillRecurringDay"; //int 1..31, mặc định 0 (0 = không định kỳ)

    private TransactionPrefillArgs() {
    }

    //type không được null vì đối số prefillType trong nav_graph.xml không nullable
    public static Bundle toBundle(long amount, String type, long occurredAt, @Nullable String note,
                                  @Nullable String categoryId, int recurringDay) {
        Bundle bundle = new Bundle();
        bundle.putLong(KEY_AMOUNT, amount);
        bundle.putString(KEY_TYPE, type);
        bundle.putLong(KEY_OCCURRED_AT, occurredAt);
        bundle.putString(KEY_NOTE, note);
        bundle.putString(KEY_CATEGORY_ID, categoryId);
        bundle.putInt(KEY_RECURRING_DAY, recurringDay);
        return bundle;
    }
}
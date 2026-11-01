package com.longvuong.plix.presentation.common;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.longvuong.plix.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Nhãn và icon của phương thức thanh toán, theo đúng 5 mã backend cho phép (models/sync.py). Thứ tự khai báo cũng là thứ tự hiển thị
public final class PaymentMethodUi {
    @DrawableRes
    private static final int DEFAULT_ICON = R.drawable.ic_other;

    private static final Map<String, String> LABELS = new LinkedHashMap<>();
    private static final Map<String, Integer> ICONS = new HashMap<>();

    static {
        LABELS.put("cash", "Tiền mặt");
        ICONS.put("cash", R.drawable.ic_cash);
        LABELS.put("bank_transfer", "Chuyển khoản");
        ICONS.put("bank_transfer", R.drawable.ic_bank);
        LABELS.put("e_wallet", "Ví điện tử");
        ICONS.put("e_wallet", R.drawable.ic_ewallet);
        LABELS.put("credit_card", "Thẻ tín dụng");
        ICONS.put("credit_card", R.drawable.ic_cardpay);
        LABELS.put("other", "Khác");
        ICONS.put("other", R.drawable.ic_other);
    }

    private PaymentMethodUi() {
    }

    public static List<String> codes() {
        return Collections.unmodifiableList(new ArrayList<>(LABELS.keySet()));
    }

    //Trả về null khi mã null hoặc không thuộc 5 mã trên (giao dịch cũ có thể chưa có phương thức thanh toán)
    @Nullable
    public static String labelOf(@Nullable String code) {
        return code == null ? null : LABELS.get(code);
    }

    @DrawableRes
    public static int iconOf(@Nullable String code) {
        if (code == null) {
            return DEFAULT_ICON;
        }
        Integer icon = ICONS.get(code);
        return icon != null ? icon : DEFAULT_ICON;
    }
}
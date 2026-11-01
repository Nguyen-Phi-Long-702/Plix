package com.longvuong.plix.presentation.common;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.NumberFormat;
import java.util.Locale;

//Ô nhập số tiền tự thêm dấu phân cách nghìn (1.500.000). Fragment chỉ nhận số tiền (long) qua Listener và gọi setAmount() khi nạp lại dữ liệu vào ô
public class AmountTextWatcher implements TextWatcher {
    public interface Listener {
        void onAmountChanged(long amount);
    }

    private final EditText editText;
    private final Listener listener;
    private boolean suppressed = false;

    public AmountTextWatcher(EditText editText, Listener listener) {
        this.editText = editText;
        this.listener = listener;
        editText.addTextChangedListener(this);
    }

    //Nạp số tiền vào ô mà không kích hoạt Listener; 0 hiện thành ô trống (ô đã có gợi ý "0")
    public void setAmount(long amount) {
        suppressed = true;
        editText.setText(amount == 0 ? "" : format(amount));
        suppressed = false;
    }

    public static long parse(String raw) {
        String digitsOnly = raw.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(digitsOnly);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static String format(long amount) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (suppressed) {
            return;
        }
        long amount = parse(s.toString());
        String formatted = s.length() == 0 ? "" : format(amount);
        if (!formatted.equals(s.toString())) {
            suppressed = true;
            s.replace(0, s.length(), formatted);
            suppressed = false;
            editText.setSelection(s.length());
            amount = parse(s.toString()); //bộ lọc maxLength có thể cắt bớt chuỗi vừa định dạng (khi dán số rất dài)
        }
        listener.onAmountChanged(amount);
    }
}
package com.longvuong.plix.presentation.transaction;

import static org.junit.Assert.assertEquals;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TransactionPrefillArgsInstrumentedTest {
    private static final long OCCURRED_AT = 1_794_121_500_000L; //mốc thời gian cố định T

    /**
     * TNX-I01
     * toBundle(135000, 'expense', T, 'HIGHLANDS COFFEE', 'cat-food', 0) rồi đọc lại bằng hằng số khoá: mọi khoá đọc lại đúng giá trị.
     */
    @Test
    public void toBundle_withAllValues_readsBackSameValuesByKeyConstants() {
        Bundle bundle = TransactionPrefillArgs.toBundle(135000L, "expense", OCCURRED_AT, "HIGHLANDS COFFEE", "cat-food", 0);

        assertEquals(135000L, bundle.getLong(TransactionPrefillArgs.KEY_AMOUNT));
        assertEquals("expense", bundle.getString(TransactionPrefillArgs.KEY_TYPE));
        assertEquals(OCCURRED_AT, bundle.getLong(TransactionPrefillArgs.KEY_OCCURRED_AT));
        assertEquals("HIGHLANDS COFFEE", bundle.getString(TransactionPrefillArgs.KEY_NOTE));
        assertEquals("cat-food", bundle.getString(TransactionPrefillArgs.KEY_CATEGORY_ID));
        assertEquals(0, bundle.getInt(TransactionPrefillArgs.KEY_RECURRING_DAY));
    }
}
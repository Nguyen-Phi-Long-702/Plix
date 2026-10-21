package com.longvuong.plix.presentation.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.longvuong.plix.R;

import org.junit.Test;

import java.util.Arrays;

public class PaymentMethodUiTest {
    @Test
    public void codes_hasFiveMethodsIncludingOther_inDisplayOrder() {
        assertEquals(Arrays.asList("cash", "bank_transfer", "e_wallet", "credit_card", "other"), PaymentMethodUi.codes());
    }

    @Test
    public void labelOf_eachMethod_returnsVietnameseLabel() {
        assertEquals("Tiền mặt", PaymentMethodUi.labelOf("cash"));
        assertEquals("Chuyển khoản", PaymentMethodUi.labelOf("bank_transfer"));
        assertEquals("Ví điện tử", PaymentMethodUi.labelOf("e_wallet"));
        assertEquals("Thẻ tín dụng", PaymentMethodUi.labelOf("credit_card"));
        assertEquals("Khác", PaymentMethodUi.labelOf("other"));
    }

    @Test
    public void iconOf_eachMethod_returnsMatchingIcon() {
        assertEquals(R.drawable.ic_cash, PaymentMethodUi.iconOf("cash"));
        assertEquals(R.drawable.ic_bank, PaymentMethodUi.iconOf("bank_transfer"));
        assertEquals(R.drawable.ic_ewallet, PaymentMethodUi.iconOf("e_wallet"));
        assertEquals(R.drawable.ic_cardpay, PaymentMethodUi.iconOf("credit_card"));
        assertEquals(R.drawable.ic_other, PaymentMethodUi.iconOf("other"));
    }

    @Test
    public void unknownOrNullCode_hasNoLabelAndFallsBackToOtherIcon() {
        assertNull(PaymentMethodUi.labelOf(null));
        assertNull(PaymentMethodUi.labelOf("unknown"));
        assertEquals(R.drawable.ic_other, PaymentMethodUi.iconOf(null));
        assertEquals(R.drawable.ic_other, PaymentMethodUi.iconOf("unknown"));
    }
}
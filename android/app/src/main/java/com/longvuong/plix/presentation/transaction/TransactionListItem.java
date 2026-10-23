package com.longvuong.plix.presentation.transaction;

import com.longvuong.plix.data.local.entity.TransactionEntity;

//1 dòng của danh sách giao dịch: tiêu đề nhóm theo ngày hoặc 1 giao dịch.
public abstract class TransactionListItem {

    private TransactionListItem() {
    }

    public static final class Header extends TransactionListItem {
        public final String title; //vd "Hôm nay", "Hôm qua", "28/10/2026"

        public Header(String title) {
            this.title = title;
        }
    }

    public static final class Row extends TransactionListItem {
        public final TransactionEntity transaction;

        public Row(TransactionEntity transaction) {
            this.transaction = transaction;
        }
    }
}
package com.longvuong.plix.presentation.transaction;

//Giá trị điền sẵn cho form Thêm giao dịch khi người dùng chấp nhận gợi ý định kỳ (FIN-18).
//Lớp giá trị thuần Java để unit test không cần Android; Fragment đổi sang Bundle bằng TransactionPrefillArgs.toBundle
public final class RecurringPrefill {
    public final long amount;
    public final String type;
    public final String categoryId;
    public final String note;
    public final long occurredAt; //mili giây, đầu ngày của lần đến hạn kế tiếp
    public final int recurringDay; //ngày lặp hằng tháng 1..31

    public RecurringPrefill(long amount, String type, String categoryId, String note,
                            long occurredAt, int recurringDay) {
        this.amount = amount;
        this.type = type;
        this.categoryId = categoryId;
        this.note = note;
        this.occurredAt = occurredAt;
        this.recurringDay = recurringDay;
    }
}
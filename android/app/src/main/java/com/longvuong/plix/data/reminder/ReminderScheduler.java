package com.longvuong.plix.data.reminder;

//Giao diện xếp và huỷ lịch nhắc nhở (FIN-10, FIN-13). Lớp gọi chỉ biết giao diện này, không biết WorkManager, nên test thay được bằng bản giả
public interface ReminderScheduler {

    //Đồng bộ lịch với cấu hình hiện tại của người dùng đang đăng nhập: bật thì xếp lại từ đầu, tắt hoặc chưa đăng nhập thì huỷ
    void applyForCurrentUser();

    //Xếp lượt nhắc ghi chép kế tiếp, tính lại từ đồng hồ hiện tại. Chỉ xếp khi đã đăng nhập và công tắc nhắc hằng ngày còn bật
    void scheduleNextDaily();

    //Huỷ nhắc ghi chép hằng ngày (cả hai tên xen kẽ)
    void cancelDaily();

    //Huỷ nhắc mục tiêu tới hạn
    void cancelDue();

    //Huỷ mọi lịch nhắc, dùng khi đăng xuất
    void cancelAll();
}

# Sơ đồ Use Case

Tài liệu mô tả các chức năng người dùng thực hiện được trong Plix (ứng dụng Android và Backend FastAPI). Sơ đồ chia làm 3 phần cho dễ đọc, viết bằng Mermaid, GitHub tự hiển thị.

## Tác nhân (Actor)

| Tác nhân | Vai trò |
|---|---|
| Người dùng | Tác nhân chính, dùng ứng dụng Plix |
| Supabase Auth | Hệ thống bên ngoài, xác thực khi đăng ký và đăng nhập |
| Gemini API | Hệ thống bên ngoài, chỉ được gọi khi người dùng bật Gemini dự phòng cho Nhập nhanh |
| WorkManager | Tác vụ nền của Android, tự chạy đồng bộ và tạo giao dịch định kỳ |

Quy ước: đường liền là quan hệ giữa tác nhân và use case, đường đứt «extend» là use case mở rộng thêm cho use case gốc.

## 1. Tài khoản, cài đặt và đồng bộ

```mermaid
flowchart LR
    U(["Người dùng"]):::actor
    SA(["Supabase Auth"]):::actor
    WM(["WorkManager (tác vụ nền)"]):::actor

    subgraph SYS["Plix - Tài khoản, cài đặt và đồng bộ"]
        A1(["Đăng ký"])
        A2(["Đăng nhập"])
        A3(["Đăng xuất"])
        S3(["Bật, tắt Nhập nhanh"])
        S4(["Bật, tắt Gemini dự phòng"])
        Y1(["Xem trạng thái đồng bộ"])
        Y2(["Đồng bộ dữ liệu với máy chủ"])
        Y3(["Tạo giao dịch định kỳ tự động"])
    end

    U --- A1
    U --- A2
    U --- A3
    U --- S3
    U --- S4
    U --- Y1
    A1 --- SA
    A2 --- SA
    Y2 --- WM
    Y3 --- WM

    classDef actor fill:#fff3cd,stroke:#b58900,stroke-width:2px
```

## 2. Giao dịch và trợ lý AI

```mermaid
flowchart LR
    U(["Người dùng"]):::actor
    GM(["Gemini API"]):::actor

    subgraph SYS["Plix - Giao dịch và trợ lý AI"]
        T1(["Thêm giao dịch"])
        T2(["Sửa giao dịch"])
        T3(["Xoá giao dịch"])
        T4(["Xem, tìm kiếm, lọc giao dịch"])
        T5(["Xem tổng quan trang chủ"])
        T6(["Nhập nhanh bằng ngôn ngữ tự nhiên"])
        T7(["Dùng Gemini dự phòng"])
        T8(["Đặt giao dịch định kỳ hằng tháng"])
        I1(["Nhận gợi ý danh mục"])
        I2(["Sửa gợi ý của AI"])
        I3(["Huấn luyện lại mô hình gợi ý"])
        I4(["Cảnh báo giao dịch tương tự"])
        I5(["Cảnh báo chi tiêu bất thường"])
        B4(["Nhận thông báo ngưỡng ngân sách"])
    end

    U --- T1
    U --- T2
    U --- T3
    U --- T4
    U --- T5
    U --- T6
    U --- I3
    T7 --- GM

    T7 -.->|«extend»| T6
    T8 -.->|«extend»| T1
    I1 -.->|«extend»| T1
    I2 -.->|«extend»| I1
    I4 -.->|«extend»| T1
    I5 -.->|«extend»| T1
    B4 -.->|«extend»| T1

    classDef actor fill:#fff3cd,stroke:#b58900,stroke-width:2px
```

## 3. Danh mục, ngân sách, mục tiêu và thống kê

```mermaid
flowchart LR
    U(["Người dùng"]):::actor

    subgraph SYS["Plix - Danh mục, ngân sách, mục tiêu và thống kê"]
        C1(["Xem danh mục"])
        C2(["Thêm danh mục"])
        C3(["Sửa danh mục"])
        C4(["Xoá danh mục"])
        B1(["Xem ngân sách và tiến độ"])
        B2(["Thêm, sửa ngân sách"])
        B3(["Xoá ngân sách"])
        M1(["Xem mục tiêu và tiến độ"])
        M2(["Thêm, sửa mục tiêu"])
        M3(["Xoá mục tiêu"])
        S1(["Xem thống kê theo khoảng thời gian"])
        S2(["Xem dự báo chi tiêu cuối tháng"])
    end

    U --- C1
    U --- C2
    U --- C3
    U --- C4
    U --- B1
    U --- B2
    U --- B3
    U --- M1
    U --- M2
    U --- M3
    U --- S1

    S2 -.->|«extend»| S1

    classDef actor fill:#fff3cd,stroke:#b58900,stroke-width:2px
```
import asyncio
from datetime import datetime

from app.services.ai.forecast import (
    STATUS_INSUFFICIENT_DATA,
    STATUS_OK,
    VN_TZ,
    check_forecast,
    compute_forecast_net,
    exponential_smoothing,
    weekly_net_series,
)


def ms(year, month, day, hour=12, minute=0):
    return round(datetime(year, month, day, hour, minute, tzinfo=VN_TZ).timestamp() * 1000)


# "Bây giờ" cố định: thứ Tư 14/10/2026 12:00 (giờ VN) -> tuần hiện tại bắt đầu thứ Hai 12/10.
# 4 tuần trọn vẹn đứng trước: thứ Hai 14/9, 21/9, 28/9 và 5/10.
NOW = ms(2026, 10, 14)

# Có đủ cả thu lẫn chi. NET theo tuần: [+700.000, -200.000, +700.000, -400.000]
TRANSACTIONS = [
    ("income", 1000000, ms(2026, 9, 15)),
    ("expense", 300000, ms(2026, 9, 15)),
    ("income", 500000, ms(2026, 9, 22)),
    ("expense", 700000, ms(2026, 9, 22)),
    ("income", 800000, ms(2026, 9, 29)),
    ("expense", 100000, ms(2026, 10, 2)),  # tuần 28/9 nhưng thuộc tháng 10
    ("expense", 400000, ms(2026, 10, 6)),
    ("expense", 50000, ms(2026, 10, 13)),  # tuần hiện tại: không vào chuỗi tuần, vẫn vào NET tháng
]

# Làm mượt alpha = 0,3 trên chuỗi trên:
# 700.000 -> 0,3 x (-200.000) + 0,7 x 700.000 = 430.000
# -> 0,3 x 700.000 + 0,7 x 430.000 = 511.000
# -> 0,3 x (-400.000) + 0,7 x 511.000 = 237.700 (NET dự báo mỗi tuần)
WEEKLY_FORECAST = 237700


def test_exponential_smoothing_applies_alpha_0_3():
    # Số thực nên làm tròn trước khi so sánh (tránh sai số 237699,99999...)
    assert round(exponential_smoothing([700000, -200000, 700000, -400000]), 2) == WEEKLY_FORECAST


def test_exponential_smoothing_of_constant_series_is_that_constant():
    assert round(exponential_smoothing([100000, 100000, 100000, 100000]), 2) == 100000


def test_weekly_series_is_income_minus_expense_per_week():
    series = weekly_net_series(
        [("income", 1000000, ms(2026, 9, 15)), ("expense", 300000, ms(2026, 9, 16))],
        ms(2026, 9, 23),
    )

    assert series == [700000]


def test_weekly_series_skips_current_week_and_fills_empty_weeks_with_zero():
    series = weekly_net_series(
        [
            ("income", 900000, ms(2026, 9, 15)),
            ("expense", 200000, ms(2026, 10, 6)),
            ("expense", 50000, ms(2026, 10, 13)),
        ],
        NOW,
    )

    assert series == [900000, 0, 0, -200000]


def test_weekly_series_ignores_unknown_transaction_type():
    series = weekly_net_series([("transfer", 500000, ms(2026, 9, 15))], NOW)

    assert series == []


def test_week_boundary_uses_vietnam_time_monday():
    # Chủ nhật 4/10 23:30 (VN) thuộc tuần 28/9; thứ Hai 5/10 00:30 (VN) thuộc tuần 5/10.
    # (Nếu tính theo UTC thì cả hai cùng rơi vào ngày 4/10 -> cùng 1 tuần.)
    series = weekly_net_series(
        [
            ("income", 100000, ms(2026, 10, 4, 23, 30)),
            ("income", 200000, ms(2026, 10, 5, 0, 30)),
        ],
        NOW,
    )

    assert series == [100000, 200000]


def test_forecast_for_current_month_by_default_uses_actual_plus_remaining_weeks():
    # NET thực tế tháng 10 đến hiện tại: -100.000 - 400.000 - 50.000 = -550.000.
    # Còn lại từ 14/10 12:00 đến hết tháng 10: 17,5 ngày = 2,5 tuần.
    # -550.000 + 237.700 x 2,5 = 44.250
    assert compute_forecast_net(TRANSACTIONS, "2026-10", NOW) == 44250


def test_forecast_for_other_months_is_not_the_current_month():
    # Tháng 11 (chưa bắt đầu): NET thực tế 0, còn nguyên 30 ngày = 30/7 tuần
    # 237.700 x 30 / 7 = 1.018.714,28... -> 1.018.714
    assert compute_forecast_net(TRANSACTIONS, "2026-11", NOW) == 1018714
    # Tháng 9 (đã qua): không còn tuần nào còn lại -> bằng NET thực tế của tháng 9
    # = (1.000.000 - 300.000) + (500.000 - 700.000) + 800.000 = 1.300.000
    assert compute_forecast_net(TRANSACTIONS, "2026-09", NOW) == 1300000


def test_forecast_ignores_transactions_dated_after_now():
    with_future = TRANSACTIONS + [("expense", 999999, ms(2026, 10, 20))]

    assert compute_forecast_net(with_future, "2026-10", NOW) == 44250


def test_forecast_counts_income_not_only_expense():
    expense_only = [t for t in TRANSACTIONS if t[0] == "expense"]

    assert compute_forecast_net(expense_only, "2026-10", NOW) != compute_forecast_net(
        TRANSACTIONS, "2026-10", NOW
    )


def test_three_complete_weeks_is_insufficient_data():
    # now = thứ Tư 7/10: tuần hiện tại 5/10 -> chỉ có 3 tuần trọn vẹn (14/9, 21/9, 28/9)
    three_weeks_now = ms(2026, 10, 7)

    assert compute_forecast_net(TRANSACTIONS, "2026-10", three_weeks_now) is None


def test_four_complete_weeks_is_enough():
    assert compute_forecast_net(TRANSACTIONS, "2026-10", NOW) is not None


def test_no_transactions_is_insufficient_data():
    assert compute_forecast_net([], "2026-10", NOW) is None


def test_only_current_week_transactions_is_insufficient_data():
    assert compute_forecast_net([("income", 500000, ms(2026, 10, 13))], "2026-10", NOW) is None


class FakeForecastPool:
    def __init__(self, transactions):
        self._transactions = transactions
        self.fetch_query = None
        self.fetch_args = None

    async def fetch(self, query, *args):
        self.fetch_query = query
        self.fetch_args = args
        return [
            {"type": tx_type, "amount": amount, "occurred_at": occurred_at}
            for tx_type, amount, occurred_at in self._transactions
        ]


def test_check_forecast_returns_ok_with_amount_and_queries_only_this_users_live_rows():
    pool = FakeForecastPool(TRANSACTIONS)

    result = asyncio.run(check_forecast(pool, "user-1", "2026-11", NOW))

    assert result == (STATUS_OK, 1018714)
    assert pool.fetch_args == ("user-1",)
    assert "user_id = $1" in pool.fetch_query
    assert "is_deleted = false" in pool.fetch_query


def test_check_forecast_defaults_to_current_month_when_month_is_none():
    pool = FakeForecastPool(TRANSACTIONS)

    result = asyncio.run(check_forecast(pool, "user-1", None, NOW))

    assert result == (STATUS_OK, 44250)


def test_check_forecast_returns_insufficient_data_without_amount():
    pool = FakeForecastPool([])

    result = asyncio.run(check_forecast(pool, "user-1", None, NOW))

    assert result == (STATUS_INSUFFICIENT_DATA, None)
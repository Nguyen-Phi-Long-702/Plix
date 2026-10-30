package com.longvuong.plix.presentation.transaction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.AiRepository;
import com.longvuong.plix.data.repository.BudgetRepository;
import com.longvuong.plix.data.repository.CategoryRepository;
import com.longvuong.plix.data.repository.TransactionRepository;
import com.longvuong.plix.domain.usecase.analytics.AggregateAnalyticsUseCase;
import com.longvuong.plix.domain.usecase.analytics.AnalyticsSummary;
import com.longvuong.plix.domain.usecase.budget.BudgetProgress;
import com.longvuong.plix.domain.usecase.budget.CalculateBudgetProgressUseCase;
import com.longvuong.plix.domain.usecase.transaction.FilterTransactionsUseCase;
import com.longvuong.plix.presentation.budget.BudgetDisplayName;
import com.longvuong.plix.presentation.common.UiState;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HomeViewModel extends ViewModel {
    private static final int RECENT_TRANSACTION_COUNT = 4;

    private final AiRepository aiRepository;
    private final FilterTransactionsUseCase filterTransactionsUseCase;
    private final AggregateAnalyticsUseCase aggregateAnalyticsUseCase;
    private final CalculateBudgetProgressUseCase calculateBudgetProgressUseCase;

    private final String currentPeriod = YearMonth.now().toString();

    //true = hiện nút Nhập nhanh; false = ẩn (đã tắt trong Cài đặt hoặc đang mất mạng)
    private final MediatorLiveData<Boolean> quickAddVisible = new MediatorLiveData<>();
    private final MediatorLiveData<UiState<HomeUiModel>> homeState = new MediatorLiveData<>();

    //null = nguồn dữ liệu đó chưa trả kết quả lần đầu
    private List<TransactionEntity> latestTransactions;
    private List<BudgetEntity> latestBudgets;
    private List<CategoryEntity> latestCategories;

    @Inject
    public HomeViewModel(AiRepository aiRepository,
                         TransactionRepository transactionRepository,
                         BudgetRepository budgetRepository,
                         CategoryRepository categoryRepository,
                         FilterTransactionsUseCase filterTransactionsUseCase,
                         AggregateAnalyticsUseCase aggregateAnalyticsUseCase,
                         CalculateBudgetProgressUseCase calculateBudgetProgressUseCase) {
        this.aiRepository = aiRepository;
        this.filterTransactionsUseCase = filterTransactionsUseCase;
        this.aggregateAnalyticsUseCase = aggregateAnalyticsUseCase;
        this.calculateBudgetProgressUseCase = calculateBudgetProgressUseCase;

        quickAddVisible.addSource(aiRepository.observeQuickAddEnabled(), enabled -> updateQuickAddVisible());
        quickAddVisible.addSource(aiRepository.observeConnectivity(), isConnected -> updateQuickAddVisible());

        homeState.setValue(new UiState.Loading<>());
        homeState.addSource(transactionRepository.getAll(), transactions -> {
            latestTransactions = transactions != null ? transactions : new ArrayList<>();
            publishHomeState();
        });
        homeState.addSource(budgetRepository.getActiveByPeriod(currentPeriod), budgets -> {
            latestBudgets = budgets != null ? budgets : new ArrayList<>();
            publishHomeState();
        });
        homeState.addSource(categoryRepository.getActiveCategories(), categories -> {
            latestCategories = categories != null ? categories : new ArrayList<>();
            publishHomeState();
        });
    }

    public LiveData<Boolean> getQuickAddVisible() {
        return quickAddVisible;
    }

    public LiveData<UiState<HomeUiModel>> getHomeState() {
        return homeState;
    }

    private void updateQuickAddVisible() {
        boolean enabled = Boolean.TRUE.equals(aiRepository.observeQuickAddEnabled().getValue());
        boolean offline = Boolean.FALSE.equals(aiRepository.observeConnectivity().getValue());
        quickAddVisible.setValue(enabled && !offline);
    }

    private void publishHomeState() {
        if (latestTransactions == null || latestBudgets == null || latestCategories == null) {
            return; //chờ đủ cả 3 nguồn để màn hình không nháy dữ liệu thiếu
        }
        if (latestTransactions.isEmpty()) {
            homeState.setValue(new UiState.Empty<>());
            return;
        }

        long periodStart = CalculateBudgetProgressUseCase.periodStartAtMillis(currentPeriod);
        long periodEnd = CalculateBudgetProgressUseCase.periodEndAtMillis(currentPeriod);
        List<TransactionEntity> monthTransactions =
                filterTransactionsUseCase.execute(latestTransactions, null, periodStart, periodEnd);
        AnalyticsSummary summary = aggregateAnalyticsUseCase.execute(monthTransactions);

        Map<String, String> categoryNamesById = new HashMap<>();
        for (CategoryEntity category : latestCategories) {
            categoryNamesById.put(category.id, category.name);
        }

        //getAll() đã sắp theo ngày giảm dần nên lấy thẳng những giao dịch đầu danh sách
        int recentCount = Math.min(RECENT_TRANSACTION_COUNT, latestTransactions.size());
        List<TransactionEntity> recentTransactions = new ArrayList<>(latestTransactions.subList(0, recentCount));

        homeState.setValue(new UiState.Success<>(new HomeUiModel(
                formatMonthLabel(),
                summary.totalIncome,
                summary.totalExpense,
                summary.totalIncome - summary.totalExpense,
                recentTransactions,
                categoryNamesById,
                pickMostUsedBudget(categoryNamesById, periodStart, periodEnd),
                calculateOverallBudgetRemaining(periodStart, periodEnd))));
    }

    //Ngân sách có % đã chi cao nhất trong tháng (bằng nhau thì lấy cái đứng trước); null khi tháng này chưa có ngân sách
    private HomeUiModel.BudgetSummary pickMostUsedBudget(Map<String, String> categoryNamesById, long periodStart, long periodEnd) {
        HomeUiModel.BudgetSummary mostUsed = null;
        for (BudgetEntity budget : latestBudgets) {
            BudgetProgress progress = calculateBudgetProgressUseCase.execute(budget, latestTransactions, periodStart, periodEnd);
            if (mostUsed == null || progress.percent > mostUsed.percent) {
                mostUsed = new HomeUiModel.BudgetSummary(
                        BudgetDisplayName.of(budget, categoryNamesById),
                        progress.percent,
                        progress.spentAmount,
                        budget.limitAmount,
                        levelOf(progress.percent, budget.thresholdPercent));
            }
        }
        return mostUsed;
    }

    //Còn lại của ngân sách tổng = hạn mức - đã chi, âm khi chi vượt; null khi tháng này chưa có ngân sách tổng
    //(getActiveByPeriod đã xếp ngân sách tổng lên đầu và AddBudgetUseCase không cho tạo 2 ngân sách tổng, nên lấy cái đầu tiên có categoryId == null)
    private Long calculateOverallBudgetRemaining(long periodStart, long periodEnd) {
        for (BudgetEntity budget : latestBudgets) {
            if (budget.categoryId == null) {
                BudgetProgress progress = calculateBudgetProgressUseCase.execute(budget, latestTransactions, periodStart, periodEnd);
                return budget.limitAmount - progress.spentAmount;
            }
        }
        return null;
    }

    private HomeUiModel.BudgetLevel levelOf(int percent, int thresholdPercent) {
        if (percent >= 100) {
            return HomeUiModel.BudgetLevel.DANGER;
        }
        if (percent >= thresholdPercent) {
            return HomeUiModel.BudgetLevel.WARNING;
        }
        return HomeUiModel.BudgetLevel.NORMAL;
    }

    private String formatMonthLabel() {
        YearMonth month = YearMonth.parse(currentPeriod);
        return "Tháng " + month.getMonthValue() + "/" + month.getYear();
    }
}
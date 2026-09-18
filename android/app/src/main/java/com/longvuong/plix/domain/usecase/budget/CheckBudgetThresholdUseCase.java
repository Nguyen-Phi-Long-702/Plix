package com.longvuong.plix.domain.usecase.budget;

import androidx.annotation.Nullable;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.data.repository.BudgetRepository;
import com.longvuong.plix.data.repository.TransactionRepository;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class CheckBudgetThresholdUseCase {
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CalculateBudgetProgressUseCase calculateBudgetProgressUseCase;
    private final BudgetThresholdChecker budgetThresholdChecker;
    private final BudgetThresholdNotifier budgetThresholdNotifier;

    @Inject
    public CheckBudgetThresholdUseCase(BudgetRepository budgetRepository, TransactionRepository transactionRepository, CalculateBudgetProgressUseCase calculateBudgetProgressUseCase, BudgetThresholdChecker budgetThresholdChecker, BudgetThresholdNotifier budgetThresholdNotifier) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.calculateBudgetProgressUseCase = calculateBudgetProgressUseCase;
        this.budgetThresholdChecker = budgetThresholdChecker;
        this.budgetThresholdNotifier = budgetThresholdNotifier;
    }

    public void checkAfterAdd(TransactionEntity newTransaction) {
        if (!"expense".equals(newTransaction.type)) {
            return; //Giao dịch thu nhập không ảnh hưởng ngân sách chi tiêu
        }
        transactionRepository.getAllOnce(result -> {
            if (!(result instanceof Result.Success)) {
                return; //Không chặn luồng thêm giao dịch chỉ vì bước cảnh báo phụ này lỗi
            }
            List<TransactionEntity> afterList = ((Result.Success<List<TransactionEntity>>) result).data;
            List<TransactionEntity> beforeList = excludeById(afterList, newTransaction.id);
            String period = periodOf(newTransaction.occurredAt);

            checkScope(newTransaction.userId, period, null, beforeList, afterList);
            if (newTransaction.categoryId != null) {
                checkScope(newTransaction.userId, period, newTransaction.categoryId, beforeList, afterList);
            }
        });
    }

    public void checkAfterUpdate(TransactionEntity oldTransaction, TransactionEntity newTransaction) {
        transactionRepository.getAllOnce(result -> {
            if (!(result instanceof Result.Success)) {
                return;
            }
            List<TransactionEntity> afterList = ((Result.Success<List<TransactionEntity>>) result).data;
            List<TransactionEntity> beforeList = replaceById(afterList, oldTransaction);

            Set<String> visitedScopes = new LinkedHashSet<>();
            checkScopeOnce(visitedScopes, newTransaction.userId, periodOf(oldTransaction.occurredAt), null, beforeList, afterList);
            if (oldTransaction.categoryId != null) {
                checkScopeOnce(visitedScopes, newTransaction.userId, periodOf(oldTransaction.occurredAt), oldTransaction.categoryId, beforeList, afterList);
            }
            checkScopeOnce(visitedScopes, newTransaction.userId, periodOf(newTransaction.occurredAt), null, beforeList, afterList);
            if (newTransaction.categoryId != null) {
                checkScopeOnce(visitedScopes, newTransaction.userId, periodOf(newTransaction.occurredAt), newTransaction.categoryId, beforeList, afterList);
            }
        });
    }

    private void checkScopeOnce(Set<String> visitedScopes, String userId, String period, @Nullable String categoryId,
                                List<TransactionEntity> beforeList, List<TransactionEntity> afterList) {
        if (!visitedScopes.add(period + "|" + categoryId)) {
            return; //Phạm vi này đã kiểm tra rồi
        }
        checkScope(userId, period, categoryId, beforeList, afterList);
    }

    private void checkScope(String userId, String period, @Nullable String categoryId, List<TransactionEntity> beforeList, List<TransactionEntity> afterList) {
        RepositoryCallback<BudgetEntity> onBudgetLoaded = result -> {
            BudgetEntity budget = result instanceof Result.Success ? ((Result.Success<BudgetEntity>) result).data : null;
            if (budget == null) {
                return; //Không có budget nào cho phạm vi này thì không có gì để cảnh báo
            }
            long periodStart = CalculateBudgetProgressUseCase.periodStartAtMillis(period);
            long periodEnd = CalculateBudgetProgressUseCase.periodEndAtMillis(period);
            long spentBefore = calculateBudgetProgressUseCase.execute(budget, beforeList, periodStart, periodEnd).spentAmount;
            long spentAfter = calculateBudgetProgressUseCase.execute(budget, afterList, periodStart, periodEnd).spentAmount;
            if (budgetThresholdChecker.justCrossedThreshold(budget.limitAmount, budget.thresholdPercent, spentBefore, spentAfter)) {
                budgetThresholdNotifier.notifyThresholdCrossed(budget, spentAfter);
            }
        };

        if (categoryId == null) {
            budgetRepository.findOverallBudgetByUserAndPeriod(userId, period, onBudgetLoaded);
        } else {
            budgetRepository.findCategoryBudgetByUserAndPeriod(userId, period, categoryId, onBudgetLoaded);
        }
    }

    private static String periodOf(long occurredAtEpochMs) {
        return YearMonth.from(Instant.ofEpochMilli(occurredAtEpochMs).atZone(ZoneId.systemDefault())).toString();
    }

    private static List<TransactionEntity> excludeById(List<TransactionEntity> source, String id) {
        List<TransactionEntity> result = new ArrayList<>();
        for (TransactionEntity transaction : source) {
            if (!transaction.id.equals(id)) {
                result.add(transaction);
            }
        }
        return result;
    }

    private static List<TransactionEntity> replaceById(List<TransactionEntity> source, TransactionEntity replacement) {
        List<TransactionEntity> result = new ArrayList<>();
        for (TransactionEntity transaction : source) {
            result.add(transaction.id.equals(replacement.id) ? replacement : transaction);
        }
        return result;
    }
}
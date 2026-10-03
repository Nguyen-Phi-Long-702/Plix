package com.longvuong.plix.data.sync;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

//Nơi duy nhất quy định thứ tự đồng bộ cố định giữa 5 bảng
public class SyncTables {
    private final CategorySyncableEntity categories;
    private final TransactionSyncableEntity transactions;
    private final BudgetSyncableEntity budgets;
    private final GoalSyncableEntity goals;
    private final CorrectionSyncableEntity corrections;

    @Inject
    public SyncTables(CategorySyncableEntity categories, TransactionSyncableEntity transactions,
                      BudgetSyncableEntity budgets, GoalSyncableEntity goals, CorrectionSyncableEntity corrections) {
        this.categories = categories;
        this.transactions = transactions;
        this.budgets = budgets;
        this.goals = goals;
        this.corrections = corrections;
    }

    //categories -> transactions -> budgets -> goals -> corrections
    //categories đi trước vì transactions/budgets tham chiếu category_id, corrections đi cuối vì luôn tham chiếu transaction_id
    public List<SyncableEntity<?, ?>> inSyncOrder() {
        return Arrays.<SyncableEntity<?, ?>>asList(categories, transactions, budgets, goals, corrections);
    }

    public TransactionSyncableEntity transactions() {
        return transactions;
    }
}
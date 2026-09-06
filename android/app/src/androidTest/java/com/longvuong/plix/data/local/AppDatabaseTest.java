package com.longvuong.plix.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.CorrectionEntity;
import com.longvuong.plix.data.local.entity.GoalEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseTest {

    private AppDatabase database;

    @Before
    public void createDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .addCallback(AppDatabase.SEED_CATEGORIES_CALLBACK)
                // allowMainThreadQueries: CHI dung trong test cho don gian.
                // KHONG duoc dung trong code that - moi truy van DB o code that phai
                // qua AppExecutors.diskIO() (bat buoc theo Muc 2.2b).
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    @Test
    public void seedCategories_insertsExactly12RowsOnCreate() {
        assertEquals(12, database.categoryDao().countAll());
    }

    @Test
    public void transaction_insertAndGetById_returnsSameData() {
        TransactionEntity entity = new TransactionEntity();
        entity.id = "tx-1";
        entity.userId = "user-1";
        entity.amount = 50000;
        entity.type = "expense";
        entity.occurredAt = 1735500000000L;
        entity.updatedAt = 1735500000000L;
        entity.syncStatus = "pending";
        entity.isDeleted = false;

        database.transactionDao().insert(entity);
        TransactionEntity loaded = database.transactionDao().getById("tx-1");

        assertNotNull(loaded);
        assertEquals("user-1", loaded.userId);
        assertEquals(50000, loaded.amount);
    }

    @Test
    public void budget_insertAndGetById_returnsSameData() {
        BudgetEntity entity = new BudgetEntity();
        entity.id = "budget-1";
        entity.userId = "user-1";
        entity.period = "2026-09";
        entity.limitAmount = 5000000;
        entity.thresholdPercent = 80;
        entity.updatedAt = 1735500000000L;
        entity.syncStatus = "pending";
        entity.isDeleted = false;

        database.budgetDao().insert(entity);
        BudgetEntity loaded = database.budgetDao().getById("budget-1");

        assertNotNull(loaded);
        assertEquals("2026-09", loaded.period);
    }

    @Test
    public void goal_insertAndGetById_returnsSameData() {
        GoalEntity entity = new GoalEntity();
        entity.id = "goal-1";
        entity.userId = "user-1";
        entity.name = "Mua xe";
        entity.targetAmount = 100000000;
        entity.currentAmount = 0;
        entity.deadline = 1767225600000L;
        entity.updatedAt = 1735500000000L;
        entity.syncStatus = "pending";
        entity.isDeleted = false;

        database.goalDao().insert(entity);
        GoalEntity loaded = database.goalDao().getById("goal-1");

        assertNotNull(loaded);
        assertEquals("Mua xe", loaded.name);
    }

    @Test
    public void correction_insertAndGetById_returnsSameData() {
        CorrectionEntity entity = new CorrectionEntity();
        entity.id = "corr-1";
        entity.userId = "user-1";
        entity.transactionId = "tx-1";
        entity.correctedCategoryId = "sys_an_uong";
        entity.createdAt = 1735500000000L;
        entity.updatedAt = 1735500000000L;
        entity.syncStatus = "pending";
        entity.isDeleted = false;

        database.correctionDao().insert(entity);
        CorrectionEntity loaded = database.correctionDao().getById("corr-1");

        assertNotNull(loaded);
        assertEquals("sys_an_uong", loaded.correctedCategoryId);
    }

    @Test
    public void category_sameNameDifferentType_bothInsertSuccessfully() {
        CategoryEntity expenseCategory = new CategoryEntity();
        expenseCategory.id = "cat-gift-expense";
        expenseCategory.userId = "user-1";
        expenseCategory.name = "Quà tặng";
        expenseCategory.type = "expense";
        expenseCategory.updatedAt = 1735500000000L;
        expenseCategory.syncStatus = "pending";
        expenseCategory.isDeleted = false;

        CategoryEntity incomeCategory = new CategoryEntity();
        incomeCategory.id = "cat-gift-income";
        incomeCategory.userId = "user-1";
        incomeCategory.name = "Quà tặng";
        incomeCategory.type = "income";
        incomeCategory.updatedAt = 1735500000000L;
        incomeCategory.syncStatus = "pending";
        incomeCategory.isDeleted = false;

        database.categoryDao().insert(expenseCategory);
        database.categoryDao().insert(incomeCategory);

        assertNotNull(database.categoryDao().getById("cat-gift-expense"));
        assertNotNull(database.categoryDao().getById("cat-gift-income"));
    }

    @Test(expected = SQLiteConstraintException.class)
    public void category_exactDuplicateNameAndType_throwsConstraintException() {
        CategoryEntity first = new CategoryEntity();
        first.id = "cat-dup-1";
        first.userId = "user-1";
        first.name = "Trùng tên";
        first.type = "expense";
        first.updatedAt = 1735500000000L;
        first.syncStatus = "pending";
        first.isDeleted = false;

        CategoryEntity duplicate = new CategoryEntity();
        duplicate.id = "cat-dup-2";
        duplicate.userId = "user-1";
        duplicate.name = "Trùng tên";
        duplicate.type = "expense";
        duplicate.updatedAt = 1735500000000L;
        duplicate.syncStatus = "pending";
        duplicate.isDeleted = false;

        database.categoryDao().insert(first);
        database.categoryDao().insert(duplicate);
    }
}
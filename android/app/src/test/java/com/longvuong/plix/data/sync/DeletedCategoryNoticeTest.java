package com.longvuong.plix.data.sync;

import static org.junit.Assert.assertEquals;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class DeletedCategoryNoticeTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeTransactionDao fakeTransactionDao;
    private DeletedCategoryNotice notice;

    @Before
    public void setUp() {
        fakeTransactionDao = new FakeTransactionDao();
        notice = new DeletedCategoryNotice(fakeTransactionDao);
    }

    private CategoryEntity category(String id, boolean isDeleted) {
        CategoryEntity entity = new CategoryEntity();
        entity.id = id;
        entity.userId = "user-1";
        entity.name = "Tiền điện";
        entity.type = "expense";
        entity.updatedAt = 1000L;
        entity.syncStatus = "synced";
        entity.isDeleted = isDeleted;
        return entity;
    }

    //beforeDeleted = null nghĩa là danh mục chưa từng có trên máy
    private List<EntityChange<CategoryEntity>> change(String id, Boolean beforeDeleted, boolean afterDeleted) {
        CategoryEntity before = beforeDeleted == null ? null : category(id, beforeDeleted);
        return Collections.singletonList(new EntityChange<>(before, category(id, afterDeleted)));
    }

    private void insertTransaction(String id, String categoryId, boolean isDeleted) {
        TransactionEntity entity = new TransactionEntity();
        entity.id = id;
        entity.userId = "user-1";
        entity.amount = 50000L;
        entity.type = "expense";
        entity.categoryId = categoryId;
        entity.occurredAt = 1L;
        entity.updatedAt = 1000L;
        entity.syncStatus = "synced";
        entity.isDeleted = isDeleted;
        fakeTransactionDao.insert(entity);
    }

    @Test
    public void checkAfterPull_categoryDeletedElsewhereAndTransactionStillUsesIt_showsNotice() {
        insertTransaction("t1", "cat-1", false);

        notice.checkAfterPull(change("cat-1", false, true));

        assertEquals(Boolean.TRUE, notice.getShouldShow().getValue());
    }

    @Test
    public void checkAfterPull_categoryDeletedElsewhereButNoTransactionUsesIt_noNotice() {
        insertTransaction("t1", "cat-other", false);

        notice.checkAfterPull(change("cat-1", false, true));

        assertEquals(Boolean.FALSE, notice.getShouldShow().getValue());
    }

    @Test
    public void checkAfterPull_onlySoftDeletedTransactionUsesIt_noNotice() {
        insertTransaction("t1", "cat-1", true);

        notice.checkAfterPull(change("cat-1", false, true));

        assertEquals(Boolean.FALSE, notice.getShouldShow().getValue());
    }

    @Test
    public void checkAfterPull_categoryReceivedAlreadyDeletedAndTransactionUsesIt_showsNotice() {
        insertTransaction("t1", "cat-1", false);

        notice.checkAfterPull(change("cat-1", null, true));

        assertEquals(Boolean.TRUE, notice.getShouldShow().getValue());
    }

    @Test
    public void checkAfterPull_categoryWasAlreadyDeletedBeforeAndOverwritten_noNotice() {
        insertTransaction("t1", "cat-1", false);

        notice.checkAfterPull(change("cat-1", true, true));

        assertEquals(Boolean.FALSE, notice.getShouldShow().getValue());
    }

    @Test
    public void checkAfterPull_categoryChangedButNotDeleted_noNotice() {
        insertTransaction("t1", "cat-1", false);

        notice.checkAfterPull(change("cat-1", false, false));

        assertEquals(Boolean.FALSE, notice.getShouldShow().getValue());
    }

    @Test
    public void markHandled_afterNotice_resetsToFalse() {
        insertTransaction("t1", "cat-1", false);
        notice.checkAfterPull(change("cat-1", false, true));

        notice.markHandled();

        assertEquals(Boolean.FALSE, notice.getShouldShow().getValue());
    }
}
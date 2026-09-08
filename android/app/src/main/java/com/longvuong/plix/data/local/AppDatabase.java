package com.longvuong.plix.data.local;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.longvuong.plix.data.local.dao.BudgetDao;
import com.longvuong.plix.data.local.dao.CategoryDao;
import com.longvuong.plix.data.local.dao.CorrectionDao;
import com.longvuong.plix.data.local.dao.GoalDao;
import com.longvuong.plix.data.local.dao.TransactionDao;
import com.longvuong.plix.data.local.entity.BudgetEntity;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.CorrectionEntity;
import com.longvuong.plix.data.local.entity.GoalEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;

@Database(
        entities = {
                TransactionEntity.class,
                CategoryEntity.class,
                BudgetEntity.class,
                GoalEntity.class,
                CorrectionEntity.class
        },
        version = 1,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TransactionDao transactionDao();

    public abstract CategoryDao categoryDao();

    public abstract BudgetDao budgetDao();

    public abstract GoalDao goalDao();

    public abstract CorrectionDao correctionDao();

    public static final RoomDatabase.Callback SEED_CATEGORIES_CALLBACK = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            seedCategory(db, "sys_an_uong", "Ăn uống", "expense");
            seedCategory(db, "sys_di_chuyen", "Di chuyển", "expense");
            seedCategory(db, "sys_nhu_yeu_pham", "Nhu yếu phẩm", "expense");
            seedCategory(db, "sys_giai_tri", "Giải trí", "expense");
            seedCategory(db, "sys_hoa_don", "Hoá đơn (điện/nước/internet)", "expense");
            seedCategory(db, "sys_suc_khoe", "Sức khoẻ", "expense");
            seedCategory(db, "sys_giao_duc", "Giáo dục", "expense");
            seedCategory(db, "sys_nha_o", "Nhà ở/Thuê nhà", "expense");
            seedCategory(db, "sys_mua_sam", "Mua sắm", "expense");
            seedCategory(db, "sys_khac_chi", "Khác", "expense");
            seedCategory(db, "sys_luong", "Lương", "income");
            seedCategory(db, "sys_thu_nhap_khac", "Thu nhập khác", "income");
        }

        private void seedCategory(SupportSQLiteDatabase db, String id, String name, String type) {
            db.execSQL(
                    "INSERT INTO categories (id, user_id, name, type, updated_at, sync_status, is_deleted) "
                            + "VALUES (?, NULL, ?, ?, 0, 'synced', 0)",
                    new Object[]{id, name, type}
            );
        }
    };
}
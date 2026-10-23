package com.longvuong.plix.presentation.transaction;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.presentation.common.CategoryIconMapper;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//Danh sách "Giao dịch gần đây" trên Home: chỉ hiển thị, không có sửa/xoá
public class RecentTransactionAdapter extends RecyclerView.Adapter<RecentTransactionAdapter.RecentTransactionViewHolder> {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private List<TransactionEntity> transactions = new ArrayList<>();
    private Map<String, String> categoryNamesById = new HashMap<>();

    public void submit(List<TransactionEntity> newTransactions, Map<String, String> newCategoryNamesById) {
        this.transactions = newTransactions != null ? newTransactions : new ArrayList<>();
        this.categoryNamesById = newCategoryNamesById != null ? newCategoryNamesById : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecentTransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_transaction, parent, false);
        return new RecentTransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentTransactionViewHolder holder, int position) {
        holder.bind(transactions.get(position), categoryNamesById, position == transactions.size() - 1);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class RecentTransactionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageCategoryIcon;
        private final TextView textCategoryName;
        private final TextView textNoteAndDate;
        private final TextView textAmount;
        private final View viewDivider;

        RecentTransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCategoryIcon = itemView.findViewById(R.id.imageCategoryIcon);
            textCategoryName = itemView.findViewById(R.id.textCategoryName);
            textNoteAndDate = itemView.findViewById(R.id.textNoteAndDate);
            textAmount = itemView.findViewById(R.id.textAmount);
            viewDivider = itemView.findViewById(R.id.viewDivider);
        }

        void bind(TransactionEntity transaction, Map<String, String> categoryNamesById, boolean isLast) {
            Context context = itemView.getContext();
            boolean isIncome = "income".equals(transaction.type);
            int amountColor = ContextCompat.getColor(context, isIncome ? R.color.color_income : R.color.color_expense);
            int tileColor = ContextCompat.getColor(context, isIncome ? R.color.income_tint : R.color.expense_tint);

            imageCategoryIcon.setImageResource(CategoryIconMapper.iconFor(transaction.categoryId));
            imageCategoryIcon.setImageTintList(ColorStateList.valueOf(amountColor));
            imageCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(tileColor));

            textCategoryName.setText(categoryLabel(transaction, categoryNamesById));
            String date = Instant.ofEpochMilli(transaction.occurredAt)
                    .atZone(ZoneId.systemDefault())
                    .format(DATE_FORMATTER);
            String note = transaction.note != null && !transaction.note.isEmpty()
                    ? transaction.note : "Không có ghi chú";
            textNoteAndDate.setText(note + " · " + date);

            textAmount.setText((isIncome ? "+" : "-") + formatCurrency(transaction.amount));
            textAmount.setTextColor(amountColor);

            viewDivider.setVisibility(isLast ? View.GONE : View.VISIBLE);
        }

        //Chưa chọn danh mục -> "Chưa phân loại"; có danh mục nhưng không còn trong danh sách đang dùng (đã bị xoá) -> "Danh mục không xác định"
        private String categoryLabel(TransactionEntity transaction, Map<String, String> categoryNamesById) {
            if (transaction.categoryId == null) {
                return "Chưa phân loại";
            }
            String categoryName = categoryNamesById.get(transaction.categoryId);
            return categoryName != null ? categoryName : "Danh mục không xác định";
        }

        private String formatCurrency(long amount) {
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            return formatter.format(amount) + "đ";
        }
    }
}
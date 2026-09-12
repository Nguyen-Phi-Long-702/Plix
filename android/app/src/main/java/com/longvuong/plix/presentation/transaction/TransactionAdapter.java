package com.longvuong.plix.presentation.transaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    public interface OnTransactionActionListener {
        void onEditTransaction(TransactionEntity transaction);
        void onDeleteTransaction(TransactionEntity transaction);
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final OnTransactionActionListener actionListener;
    private List<TransactionEntity> transactions = new ArrayList<>();
    private Map<String, String> categoryNamesById = new HashMap<>();

    public TransactionAdapter(OnTransactionActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(List<TransactionEntity> newTransactions) {
        this.transactions = newTransactions != null ? newTransactions : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void submitCategories(List<CategoryEntity> categories) {
        Map<String, String> map = new HashMap<>();
        if (categories != null) {
            for (CategoryEntity category : categories) {
                map.put(category.id, category.name);
            }
        }
        this.categoryNamesById = map;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        holder.bind(transactions.get(position), categoryNamesById, actionListener);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final TextView textCategoryName;
        private final TextView textNoteAndDate;
        private final TextView textAmount;
        private final ImageButton buttonDelete;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategoryName = itemView.findViewById(R.id.textCategoryName);
            textNoteAndDate = itemView.findViewById(R.id.textNoteAndDate);
            textAmount = itemView.findViewById(R.id.textAmount);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteTransaction);
        }

        void bind(TransactionEntity transaction, Map<String, String> categoryNamesById,
                  OnTransactionActionListener actionListener) {
            String categoryName = transaction.categoryId != null
                    ? categoryNamesById.get(transaction.categoryId) : null;
            textCategoryName.setText(categoryName != null ? categoryName : "Chưa phân loại");

            String date = Instant.ofEpochMilli(transaction.occurredAt)
                    .atZone(ZoneId.systemDefault())
                    .format(DATE_FORMATTER);
            String note = transaction.note != null && !transaction.note.isEmpty()
                    ? transaction.note : "Không có ghi chú";
            textNoteAndDate.setText(note + " · " + date);

            boolean isIncome = "income".equals(transaction.type);
            textAmount.setText((isIncome ? "+" : "-") + formatCurrency(transaction.amount));
            textAmount.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    isIncome ? R.color.color_income : R.color.color_expense));

            itemView.setOnClickListener(v -> actionListener.onEditTransaction(transaction));
            buttonDelete.setOnClickListener(v -> actionListener.onDeleteTransaction(transaction));
        }

        private String formatCurrency(long amount) {
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            return formatter.format(amount) + "đ";
        }
    }
}
package com.longvuong.plix.presentation.transaction;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.longvuong.plix.R;
import com.longvuong.plix.data.local.entity.CategoryEntity;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.presentation.common.CategoryIconMapper;
import com.longvuong.plix.presentation.common.PaymentMethodUi;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface OnTransactionActionListener {
        void onEditTransaction(TransactionEntity transaction);
        void onDeleteTransaction(TransactionEntity transaction);
    }

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ROW = 1;

    private final OnTransactionActionListener actionListener;
    private List<TransactionListItem> items = new ArrayList<>();
    private Map<String, String> categoryNamesById = new HashMap<>();

    public TransactionAdapter(OnTransactionActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(List<TransactionListItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
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

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof TransactionListItem.Header ? VIEW_TYPE_HEADER : VIEW_TYPE_ROW;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_group_header, parent, false));
        }
        return new RowViewHolder(inflater.inflate(R.layout.item_transaction, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TransactionListItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((TransactionListItem.Header) item);
            return;
        }
        //Vạch kẻ chỉ hiện giữa các giao dịch trong cùng 1 nhóm (không hiện ở giao dịch cuối nhóm)
        boolean hasRowBelow = position + 1 < items.size() && items.get(position + 1) instanceof TransactionListItem.Row;
        ((RowViewHolder) holder).bind(((TransactionListItem.Row) item).transaction, categoryNamesById, hasRowBelow, actionListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView textGroupTitle;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textGroupTitle = itemView.findViewById(R.id.textGroupTitle);
        }

        void bind(TransactionListItem.Header header) {
            textGroupTitle.setText(header.title);
        }
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {
        private final View layoutCategoryIcon;
        private final ImageView imageCategoryIcon;
        private final TextView textCategoryName;
        private final TextView textNoteAndMethod;
        private final TextView textAmount;
        private final ImageButton buttonDelete;
        private final View viewDivider;

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCategoryIcon = itemView.findViewById(R.id.layoutCategoryIcon);
            imageCategoryIcon = itemView.findViewById(R.id.imageCategoryIcon);
            textCategoryName = itemView.findViewById(R.id.textCategoryName);
            textNoteAndMethod = itemView.findViewById(R.id.textNoteAndMethod);
            textAmount = itemView.findViewById(R.id.textAmount);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteTransaction);
            viewDivider = itemView.findViewById(R.id.viewDivider);
        }

        void bind(TransactionEntity transaction, Map<String, String> categoryNamesById, boolean showDivider, OnTransactionActionListener actionListener) {
            Context context = itemView.getContext();
            boolean isIncome = "income".equals(transaction.type);
            int typeColor = ContextCompat.getColor(context, isIncome ? R.color.color_income : R.color.color_expense);

            layoutCategoryIcon.setBackgroundResource(isIncome ? R.drawable.bg_category_icon_income : R.drawable.bg_category_icon_expense);
            imageCategoryIcon.setImageResource(CategoryIconMapper.iconFor(transaction.categoryId));
            imageCategoryIcon.setImageTintList(ColorStateList.valueOf(typeColor));

            textCategoryName.setText(categoryLabel(transaction, categoryNamesById));
            String note = transaction.note != null && !transaction.note.isEmpty()
                    ? transaction.note : "Không có ghi chú";
            String paymentMethodLabel = PaymentMethodUi.labelOf(transaction.paymentMethod);
            textNoteAndMethod.setText(paymentMethodLabel != null ? note + " · " + paymentMethodLabel : note);

            textAmount.setText((isIncome ? "+" : "-") + formatCurrency(transaction.amount));
            textAmount.setTextColor(typeColor);

            viewDivider.setVisibility(showDivider ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> actionListener.onEditTransaction(transaction));
            buttonDelete.setOnClickListener(v -> actionListener.onDeleteTransaction(transaction));
        }

        //Chưa chọn danh mục -> "Chưa phân loại"; có danh mục nhưng không còn trong danh sách đang dùng (đã bị xoá) -> "Danh mục không xác định"
        private String categoryLabel(TransactionEntity transaction, Map<String, String> categoryNamesById) {
            if (transaction.categoryId == null) {
                return "Chưa phân loại";
            }
            if (categoryNamesById.isEmpty()) {
                return ""; //danh mục chưa tải xong (tải xong luôn có 12 danh mục hệ thống), tránh nháy chữ "Danh mục không xác định"
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
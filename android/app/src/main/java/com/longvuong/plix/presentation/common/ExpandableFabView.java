package com.longvuong.plix.presentation.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.longvuong.plix.R;

//FAB mở rộng dùng chung: nút chính (+) mở menu "Nhập nhanh (AI)" / "Thêm giao dịch" kèm lớp phủ. Chỉ giữ trạng thái đóng/mở của giao diện; hành động bấm do Fragment gắn listener
public class ExpandableFabView extends FrameLayout {
    private static final float EXPANDED_ROTATION = 45f;
    private static final long ROTATION_DURATION_MS = 200L;

    private final View scrim;
    private final View menu;
    private final View cardQuickAdd;
    private final View cardAddTransaction;
    private final FloatingActionButton fabMain;
    private boolean expanded = false;

    public ExpandableFabView(@NonNull Context context) {
        this(context, null);
    }

    public ExpandableFabView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandableFabView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_expandable_fab, this, true);

        scrim = findViewById(R.id.viewFabScrim);
        menu = findViewById(R.id.layoutFabMenu);
        cardQuickAdd = findViewById(R.id.cardFabQuickAdd);
        cardAddTransaction = findViewById(R.id.cardFabAddTransaction);
        fabMain = findViewById(R.id.fabMain);

        //Thiết kế: FAB bo góc vuông-tròn thay vì tròn hẳn; lấy cỡ bo từ token radius_large
        fabMain.setShapeAppearanceModel(fabMain.getShapeAppearanceModel().toBuilder()
                .setAllCornerSizes(getResources().getDimension(R.dimen.radius_large))
                .build());

        fabMain.setOnClickListener(v -> setExpanded(!expanded));
        scrim.setOnClickListener(v -> setExpanded(false));
    }

    //Ẩn/hiện mục "Nhập nhanh (AI)" (Home dùng giá trị quickAddVisible của ViewModel)
    public void setQuickAddVisible(boolean visible) {
        cardQuickAdd.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setOnQuickAddClickListener(@Nullable View.OnClickListener listener) {
        cardQuickAdd.setOnClickListener(v -> onItemClicked(v, listener));
    }

    public void setOnAddTransactionClickListener(@Nullable View.OnClickListener listener) {
        cardAddTransaction.setOnClickListener(v -> onItemClicked(v, listener));
    }

    public void collapse() {
        setExpanded(false);
    }

    public boolean isExpanded() {
        return expanded;
    }

    private void onItemClicked(View view, @Nullable View.OnClickListener listener) {
        collapse();
        if (listener != null) {
            listener.onClick(view);
        }
    }

    private void setExpanded(boolean expand) {
        if (expanded == expand) {
            return;
        }
        expanded = expand;
        scrim.setVisibility(expand ? View.VISIBLE : View.GONE);
        menu.setVisibility(expand ? View.VISIBLE : View.GONE);
        fabMain.animate()
                .rotation(expand ? EXPANDED_ROTATION : 0f)
                .setDuration(ROTATION_DURATION_MS)
                .start();
    }
}
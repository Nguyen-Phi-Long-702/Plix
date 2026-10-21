package com.longvuong.plix.presentation.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.longvuong.plix.R;

//Khối trạng thái rỗng dùng chung: icon + tiêu đề + mô tả, có thể kèm 1 nút hành động. Nội dung khai báo trong XML qua các thuộc tính emptyIcon/emptyTitle/emptyMessage/emptyActionText
public class EmptyStateView extends FrameLayout {
    private final MaterialButton buttonAction;

    public EmptyStateView(@NonNull Context context) {
        this(context, null);
    }

    public EmptyStateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmptyStateView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_empty_state, this, true);

        ImageView imageIcon = findViewById(R.id.imageEmptyIcon);
        TextView textTitle = findViewById(R.id.textEmptyTitle);
        TextView textMessage = findViewById(R.id.textEmptyMessage);
        buttonAction = findViewById(R.id.buttonEmptyAction);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.EmptyStateView, defStyleAttr, 0);
        try {
            int iconRes = typedArray.getResourceId(R.styleable.EmptyStateView_emptyIcon, 0);
            if (iconRes != 0) {
                imageIcon.setImageResource(iconRes);
            }
            textTitle.setText(typedArray.getString(R.styleable.EmptyStateView_emptyTitle));
            textMessage.setText(typedArray.getString(R.styleable.EmptyStateView_emptyMessage));
            String actionText = typedArray.getString(R.styleable.EmptyStateView_emptyActionText);
            if (!TextUtils.isEmpty(actionText)) {
                buttonAction.setText(actionText);
                buttonAction.setVisibility(View.VISIBLE);
            }
        } finally {
            typedArray.recycle();
        }
    }

    //Chỉ có tác dụng khi đã khai báo emptyActionText (nút mới hiện)
    public void setOnActionClickListener(@Nullable View.OnClickListener listener) {
        buttonAction.setOnClickListener(listener);
    }
}
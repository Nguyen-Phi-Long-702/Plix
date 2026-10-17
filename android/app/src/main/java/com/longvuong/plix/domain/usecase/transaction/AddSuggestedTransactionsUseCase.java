package com.longvuong.plix.domain.usecase.transaction;

import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.local.entity.TransactionEntity;
import com.longvuong.plix.domain.validation.FormValidator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

//Lưu nhiều giao dịch đề xuất từ câu nhập nhanh: kiểm tra hợp lệ TẤT CẢ trước, hợp lệ hết mới bắt đầu lưu,
//để không xảy ra tình huống lưu dở dang (vài giao dịch đã lưu, vài giao dịch bị từ chối) làm người dùng bấm lưu lại bị trùng
public class AddSuggestedTransactionsUseCase {
    private final AddTransactionUseCase addTransactionUseCase;
    private final FormValidator formValidator;

    @Inject
    public AddSuggestedTransactionsUseCase(AddTransactionUseCase addTransactionUseCase, FormValidator formValidator) {
        this.addTransactionUseCase = addTransactionUseCase;
        this.formValidator = formValidator;
    }

    //callback nhận số giao dịch đã lưu (bằng entities.size()) hoặc lỗi đầu tiên gặp phải
    public void execute(List<TransactionEntity> entities, RepositoryCallback<Integer> callback) {
        for (TransactionEntity entity : entities) {
            Result<Void> validation = formValidator.validateTransaction(entity.amount, entity.note, entity.occurredAt);
            if (validation instanceof Result.Error) {
                Result.Error<Void> error = (Result.Error<Void>) validation;
                callback.onResult(new Result.Error<>(error.type, error.message, error.cause));
                return;
            }
        }
        saveFrom(new ArrayList<>(entities), 0, callback);
    }

    private void saveFrom(List<TransactionEntity> entities, int index, RepositoryCallback<Integer> callback) {
        if (index >= entities.size()) {
            callback.onResult(new Result.Success<>(entities.size()));
            return;
        }
        addTransactionUseCase.execute(entities.get(index), result -> {
            if (result instanceof Result.Error) {
                Result.Error<Void> error = (Result.Error<Void>) result;
                callback.onResult(new Result.Error<>(error.type, error.message, error.cause));
                return;
            }
            saveFrom(entities, index + 1, callback);
        });
    }
}
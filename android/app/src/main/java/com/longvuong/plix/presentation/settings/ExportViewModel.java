package com.longvuong.plix.presentation.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.core.error.Result;
import com.longvuong.plix.data.repository.ExportRepository;
import com.longvuong.plix.presentation.common.UiState;

import java.io.File;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ExportViewModel extends ViewModel {

    private final ExportRepository exportRepository;
    //Empty = chưa làm gì; Loading = đang tải file CSV; Success = đã có file để chia sẻ; Error = lỗi (mất mạng, bị giới hạn 1 lần/giờ, máy chủ lỗi...)
    private final MutableLiveData<UiState<File>> exportState = new MutableLiveData<>(new UiState.Empty<>());

    @Inject
    public ExportViewModel(ExportRepository exportRepository) {
        this.exportRepository = exportRepository;
    }

    public LiveData<UiState<File>> getExportState() {
        return exportState;
    }

    public void exportCsv() {
        if (exportState.getValue() instanceof UiState.Loading) {
            return; //đang tải, bỏ qua lần nhấn thứ hai
        }
        exportState.setValue(new UiState.Loading<>());
        exportRepository.exportCsv(result -> exportState.setValue(toUiState(result)));
    }

    //Fragment đã mở bảng chia sẻ hoặc đã báo lỗi xong: đưa về Empty để xoay màn hình không chia sẻ/báo lỗi lần nữa
    public void onExportHandled() {
        exportState.setValue(new UiState.Empty<>());
    }

    private UiState<File> toUiState(Result<File> result) {
        if (result instanceof Result.Success) {
            return new UiState.Success<>(((Result.Success<File>) result).data);
        }
        return new UiState.Error<>(((Result.Error<File>) result).message);
    }
}
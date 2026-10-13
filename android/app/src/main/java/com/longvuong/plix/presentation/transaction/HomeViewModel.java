package com.longvuong.plix.presentation.transaction;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.longvuong.plix.data.repository.AiRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final AiRepository aiRepository;
    //true = hiện nút Nhập nhanh; false = ẩn (đã tắt trong Cài đặt hoặc đang mất mạng)
    private final MediatorLiveData<Boolean> quickAddVisible = new MediatorLiveData<>();

    @Inject
    public HomeViewModel(AiRepository aiRepository) {
        this.aiRepository = aiRepository;
        quickAddVisible.addSource(aiRepository.observeQuickAddEnabled(), enabled -> updateQuickAddVisible());
        quickAddVisible.addSource(aiRepository.observeConnectivity(), isConnected -> updateQuickAddVisible());
    }

    public LiveData<Boolean> getQuickAddVisible() {
        return quickAddVisible;
    }

    private void updateQuickAddVisible() {
        boolean enabled = Boolean.TRUE.equals(aiRepository.observeQuickAddEnabled().getValue());
        boolean offline = Boolean.FALSE.equals(aiRepository.observeConnectivity().getValue());
        quickAddVisible.setValue(enabled && !offline);
    }
}
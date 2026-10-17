package com.longvuong.plix.presentation.transaction;

import static org.junit.Assert.assertEquals;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HomeViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeAiRepository fakeAiRepository;
    private HomeViewModel viewModel;

    @Before
    public void setUp() {
        fakeAiRepository = new FakeAiRepository();
        viewModel = new HomeViewModel(fakeAiRepository);
        viewModel.getQuickAddVisible().observeForever(visible -> { }); //MediatorLiveData chỉ cập nhật khi có người quan sát
    }

    @Test
    public void quickAddVisible_whenEnabledAndOnline_isTrue() {
        assertEquals(Boolean.TRUE, viewModel.getQuickAddVisible().getValue());
    }

    @Test
    public void quickAddVisible_whenDisabledInSettings_isFalse() {
        fakeAiRepository.setQuickAddEnabled(false);

        assertEquals(Boolean.FALSE, viewModel.getQuickAddVisible().getValue());
    }

    @Test
    public void quickAddVisible_whenOffline_isFalse() {
        fakeAiRepository.setConnected(false);

        assertEquals(Boolean.FALSE, viewModel.getQuickAddVisible().getValue());
    }

    @Test
    public void quickAddVisible_returnsWhenBackOnline() {
        fakeAiRepository.setConnected(false);
        fakeAiRepository.setConnected(true);

        assertEquals(Boolean.TRUE, viewModel.getQuickAddVisible().getValue());
    }

    @Test
    public void quickAddVisible_staysFalseWhenBackOnlineButDisabled() {
        fakeAiRepository.setQuickAddEnabled(false);
        fakeAiRepository.setConnected(false);
        fakeAiRepository.setConnected(true);

        assertEquals(Boolean.FALSE, viewModel.getQuickAddVisible().getValue());
    }
}
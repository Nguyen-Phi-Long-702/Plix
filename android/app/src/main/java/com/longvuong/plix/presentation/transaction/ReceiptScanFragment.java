package com.longvuong.plix.presentation.transaction;

import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.longvuong.plix.R;
import com.longvuong.plix.data.repository.ReceiptCaptureTarget;
import com.longvuong.plix.presentation.common.UiState;

import dagger.hilt.android.AndroidEntryPoint;

//Màn Quét hoá đơn (AI-08): chỉ hiển thị. Mở máy ảnh hệ thống hoặc trình chọn ảnh, quan sát ReceiptScanViewModel rồi mở form Thêm giao dịch kèm dữ liệu điền sẵn.
//Không log nội dung hoá đơn, số tiền hay đường dẫn ảnh (SEC-10)
@AndroidEntryPoint
public class ReceiptScanFragment extends Fragment {
    private static final String STATE_CAPTURE_URI = "pendingCaptureUri";
    private static final String STATE_CAPTURE_TOKEN = "pendingCaptureToken";
    private static final String NO_CAMERA_MESSAGE = "Không tìm thấy ứng dụng máy ảnh trên thiết bị";
    private static final String NO_GALLERY_MESSAGE = "Không mở được thư viện ảnh trên thiết bị";
    private static final String LOW_CONFIDENCE_MESSAGE = "Số tiền chưa chắc chắn, vui lòng kiểm tra";

    private ReceiptScanViewModel viewModel;
    private MaterialButton buttonCapture;
    private MaterialButton buttonPickGallery;
    private View layoutScanLoading;
    private View layoutScanError;
    private TextView textScanError;
    private MaterialButton buttonEnterManually;

    //Đích ảnh chụp đang chờ máy ảnh trả kết quả; lưu qua onSaveInstanceState để xoay máy lúc đang ở máy ảnh không làm mất ảnh vừa chụp
    @Nullable
    private ReceiptCaptureTarget pendingCapture;

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), this::onTakePictureResult);
    private final ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onPickImageResult);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            String uriString = savedInstanceState.getString(STATE_CAPTURE_URI);
            String token = savedInstanceState.getString(STATE_CAPTURE_TOKEN);
            if (uriString != null && token != null) {
                pendingCapture = new ReceiptCaptureTarget(uriString, token);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_receipt_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ReceiptScanViewModel.class);

        bindViews(view);
        setupButtons();

        viewModel.getCaptureTarget().observe(getViewLifecycleOwner(), this::onCaptureTarget);
        viewModel.getScanState().observe(getViewLifecycleOwner(), this::renderScanState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingCapture != null) {
            outState.putString(STATE_CAPTURE_URI, pendingCapture.uriString);
            outState.putString(STATE_CAPTURE_TOKEN, pendingCapture.token);
        }
    }

    private void bindViews(View view) {
        buttonCapture = view.findViewById(R.id.buttonCapture);
        buttonPickGallery = view.findViewById(R.id.buttonPickGallery);
        layoutScanLoading = view.findViewById(R.id.layoutScanLoading);
        layoutScanError = view.findViewById(R.id.layoutScanError);
        textScanError = view.findViewById(R.id.textScanError);
        buttonEnterManually = view.findViewById(R.id.buttonEnterManually);
    }

    private void setupButtons() {
        buttonCapture.setOnClickListener(v -> viewModel.onCaptureRequested());
        buttonPickGallery.setOnClickListener(v -> launchGalleryPicker());
        buttonEnterManually.setOnClickListener(v -> NavHostFragment.findNavController(this)
                .navigate(R.id.action_receiptScanFragment_to_addEditTransactionFragment)); //không kèm đối số: form trống
    }

    //ViewModel đã tạo xong tệp ảnh tạm: mở máy ảnh hệ thống rồi bỏ đích để không mở lại khi màn hình được tạo lại
    private void onCaptureTarget(@Nullable ReceiptCaptureTarget target) {
        if (target == null) {
            return;
        }
        viewModel.onCaptureTargetConsumed();
        pendingCapture = target;
        try {
            takePictureLauncher.launch(Uri.parse(target.uriString));
        } catch (ActivityNotFoundException e) {
            pendingCapture = null;
            viewModel.onCaptureCancelled(); //xoá tệp ảnh tạm vừa tạo
            showMessage(NO_CAMERA_MESSAGE);
        }
    }

    private void onTakePictureResult(Boolean saved) {
        ReceiptCaptureTarget target = pendingCapture;
        pendingCapture = null;
        if (Boolean.TRUE.equals(saved) && target != null) {
            viewModel.onImageCaptured(target.token, target.uriString);
        } else {
            viewModel.onCaptureCancelled();
        }
    }

    private void launchGalleryPicker() {
        try {
            pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        } catch (ActivityNotFoundException e) {
            showMessage(NO_GALLERY_MESSAGE);
        }
    }

    //uri null = người dùng thoát trình chọn ảnh mà không chọn: không làm gì
    private void onPickImageResult(@Nullable Uri uri) {
        if (uri != null) {
            viewModel.onImagePicked(uri.toString());
        }
    }

    private void renderScanState(@Nullable UiState<ReceiptPrefill> state) {
        boolean loading = state instanceof UiState.Loading;
        boolean error = state instanceof UiState.Error;
        layoutScanLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        layoutScanError.setVisibility(error ? View.VISIBLE : View.GONE);
        buttonCapture.setEnabled(!loading);
        buttonPickGallery.setEnabled(!loading);

        if (error) {
            textScanError.setText(((UiState.Error<ReceiptPrefill>) state).message);
            buttonEnterManually.setVisibility(viewModel.canEnterManually() ? View.VISIBLE : View.GONE);
        } else if (state instanceof UiState.Success) {
            openTransactionForm(((UiState.Success<ReceiptPrefill>) state).data);
        }
    }

    //Mở form Thêm giao dịch kèm dữ liệu điền sẵn (không tự lưu, AI-11); màn Quét bị bỏ khỏi back stack nhờ popUpTo trong nav_graph
    private void openTransactionForm(ReceiptPrefill prefill) {
        View content = requireActivity().findViewById(android.R.id.content);
        Bundle args = TransactionPrefillArgs.toBundle(prefill.getAmount(), prefill.getType(),
                prefill.getOccurredAt(), prefill.getNote(), null, 0);
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_receiptScanFragment_to_addEditTransactionFragment, args);
        if (prefill.isLowConfidence()) {
            Snackbar.make(content, LOW_CONFIDENCE_MESSAGE, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showMessage(String message) {
        Snackbar.make(requireActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}
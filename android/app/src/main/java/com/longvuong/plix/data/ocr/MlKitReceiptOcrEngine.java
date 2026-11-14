package com.longvuong.plix.data.ocr;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.longvuong.plix.core.error.ErrorMapper;
import com.longvuong.plix.core.error.RepositoryCallback;
import com.longvuong.plix.core.error.Result;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

//Đọc chữ trên hoá đơn bằng ML Kit Text Recognition, chạy trên thiết bị; không ghi log nội dung (SEC-10)
@Singleton
public class MlKitReceiptOcrEngine implements ReceiptOcrEngine {

    private final Context context;
    private final ErrorMapper errorMapper;
    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    @Inject
    public MlKitReceiptOcrEngine(@ApplicationContext Context context, ErrorMapper errorMapper) {
        this.context = context;
        this.errorMapper = errorMapper;
    }

    @Override
    public void recognize(String imageUriString, RepositoryCallback<List<String>> callback) {
        InputImage image;
        try {
            image = InputImage.fromFilePath(context, Uri.parse(imageUriString));
        } catch (Exception e) {
            Result<List<String>> error = errorMapper.mapThrowable(e);
            callback.onResult(error);
            return;
        }
        recognizer.process(image)
                .addOnSuccessListener(text -> callback.onResult(new Result.Success<>(toLinesTopToBottom(text))))
                .addOnFailureListener(e -> {
                    Result<List<String>> error = errorMapper.mapThrowable(e);
                    callback.onResult(error);
                });
    }

    //Gom mọi dòng của mọi khối rồi sắp theo toạ độ trên xuống (sắp xếp ổn định: dòng cùng toạ độ giữ thứ tự ML Kit trả về)
    private List<String> toLinesTopToBottom(Text text) {
        List<Text.Line> lines = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            lines.addAll(block.getLines());
        }
        lines.sort(Comparator.comparingInt(MlKitReceiptOcrEngine::topOf));
        List<String> result = new ArrayList<>();
        for (Text.Line line : lines) {
            result.add(line.getText());
        }
        return result;
    }

    private static int topOf(Text.Line line) {
        Rect box = line.getBoundingBox();
        return box != null ? box.top : Integer.MAX_VALUE; //không có khung thì xếp cuối
    }
}
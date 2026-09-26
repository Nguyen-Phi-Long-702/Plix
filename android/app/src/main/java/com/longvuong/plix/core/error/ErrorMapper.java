package com.longvuong.plix.core.error;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.longvuong.plix.data.remote.dto.ErrorResponseDto;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ErrorMapper {

    private final Gson gson = new Gson();

    @Inject
    public ErrorMapper() {
    }

    public <T> Result<T> mapThrowable(Throwable throwable) {
        if (throwable instanceof IOException) {
            return new Result.Error<>(ErrorType.NETWORK, "Không có kết nối mạng", throwable);
        }
        return new Result.Error<>(ErrorType.UNKNOWN, "Đã xảy ra lỗi không xác định, vui lòng thử lại", throwable);
    }

    public <T> Result<T> mapHttpCode(int code) {
        return mapHttpCode(code, null);
    }

    public <T> Result<T> mapHttpCode(int code, @Nullable String errorBodyJson) {
        if (code == 401) {
            return new Result.Error<>(ErrorType.AUTH, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại", null);
        }
        if (code == 429) {
            String serverMessage = extractServerMessage(errorBodyJson);
            return new Result.Error<>(ErrorType.SERVER,
                    serverMessage != null ? serverMessage : "Vui lòng thử lại sau ít phút", null);
        }
        if (code == 409) {
            String serverMessage = extractServerMessage(errorBodyJson);
            return new Result.Error<>(ErrorType.SERVER,
                    serverMessage != null ? serverMessage : "Đang huấn luyện, thử lại sau", null);
        }
        if (code >= 500) {
            return new Result.Error<>(ErrorType.SERVER, "Máy chủ đang bận, thử lại sau", null);
        }
        return new Result.Error<>(ErrorType.UNKNOWN, "Đã xảy ra lỗi không xác định (mã " + code + ")", null);
    }

    @Nullable
    private String extractServerMessage(@Nullable String errorBodyJson) {
        if (errorBodyJson == null || errorBodyJson.isEmpty()) {
            return null;
        }
        try {
            ErrorResponseDto errorResponse = gson.fromJson(errorBodyJson, ErrorResponseDto.class);
            return errorResponse != null ? errorResponse.message : null;
        } catch (Exception e) {
            return null;
        }
    }
}
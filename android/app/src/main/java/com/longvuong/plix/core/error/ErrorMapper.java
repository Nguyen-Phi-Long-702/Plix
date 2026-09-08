package com.longvuong.plix.core.error;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ErrorMapper {

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
        if (code == 401) {
            return new Result.Error<>(ErrorType.AUTH, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại", null);
        }
        if (code >= 500) {
            return new Result.Error<>(ErrorType.SERVER, "Máy chủ đang bận, thử lại sau", null);
        }
        return new Result.Error<>(ErrorType.UNKNOWN, "Đã xảy ra lỗi không xác định (mã " + code + ")", null);
    }
}
package com.longvuong.plix.core.error;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.longvuong.plix.data.remote.dto.ErrorResponseDto;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ErrorMapper {
    private static final String AI_BUSY_MESSAGE = "AI đang bận, vui lòng nhập tay";
    private static final String AI_BUSY_ERROR_CODE = "AI_BUSY"; //error_code backend trả kèm HTTP 503 khi Gemini lỗi/hết hạn mức
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
        if (code == 400) {
            String serverMessage = extractServerMessage(errorBodyJson);
            return new Result.Error<>(ErrorType.SERVER,
                    serverMessage != null ? serverMessage : "Yêu cầu không hợp lệ, vui lòng thử lại", null);
        }
        if (code == 503 && AI_BUSY_ERROR_CODE.equals(extractServerErrorCode(errorBodyJson))) {
            return mapAiBusy(); //503 do Gemini (không phải máy chủ sập) -> hiện "AI đang bận, nhập tay"
        }
        if (code >= 500) {
            return new Result.Error<>(ErrorType.SERVER, "Máy chủ đang bận, thử lại sau", null);
        }
        return new Result.Error<>(ErrorType.UNKNOWN, "Đã xảy ra lỗi không xác định (mã " + code + ")", null);
    }

    //Lỗi trả về từ Supabase Auth khi đăng nhập/đăng ký: nhận diện theo error_code của Supabase; lỗi lạ dùng câu chung, không đưa mã HTTP ra giao diện
    public <T> Result<T> mapAuthHttpCode(int code, @Nullable String errorBodyJson) {
        String errorCode = extractServerErrorCode(errorBodyJson);
        if ("invalid_credentials".equals(errorCode)) {
            return new Result.Error<>(ErrorType.AUTH, "Email hoặc mật khẩu không đúng", null);
        }
        if ("user_already_exists".equals(errorCode) || "email_exists".equals(errorCode)) {
            return new Result.Error<>(ErrorType.VALIDATION, "Email này đã được đăng ký", null);
        }
        if ("weak_password".equals(errorCode)) {
            return new Result.Error<>(ErrorType.VALIDATION, "Mật khẩu quá yếu, vui lòng chọn mật khẩu mạnh hơn", null);
        }
        if ("email_address_invalid".equals(errorCode)) {
            return new Result.Error<>(ErrorType.VALIDATION, "Email không hợp lệ", null);
        }
        if (code == 429 || code >= 500) {
            return mapHttpCode(code, errorBodyJson);
        }
        return new Result.Error<>(ErrorType.UNKNOWN, "Không thực hiện được, vui lòng kiểm tra lại thông tin", null);
    }

    //Dịch vụ dự phòng Gemini lỗi, quá thời gian chờ hoặc hết hạn mức: không phải lỗi kỹ thuật, chỉ cần báo người dùng nhập tay
    public <T> Result<T> mapAiBusy() {
        return new Result.Error<>(ErrorType.AI_BUSY, AI_BUSY_MESSAGE, null);
    }

    @Nullable
    private String extractServerErrorCode(@Nullable String errorBodyJson) {
        if (errorBodyJson == null || errorBodyJson.isEmpty()) {
            return null;
        }
        try {
            ErrorResponseDto errorResponse = gson.fromJson(errorBodyJson, ErrorResponseDto.class);
            return errorResponse != null ? errorResponse.errorCode : null;
        } catch (Exception e) {
            return null;
        }
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
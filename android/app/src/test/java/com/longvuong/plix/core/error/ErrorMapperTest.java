package com.longvuong.plix.core.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class ErrorMapperTest {

    private final ErrorMapper errorMapper = new ErrorMapper();

    @Test
    public void mapThrowable_ioException_returnsNetworkError() {
        Result<Object> result = errorMapper.mapThrowable(new IOException("Mất kết nối"));

        assertTrue(result instanceof Result.Error);
        Result.Error<Object> error = (Result.Error<Object>) result;
        assertEquals(ErrorType.NETWORK, error.type);
        assertEquals("Không có kết nối mạng", error.message);
    }

    @Test
    public void mapThrowable_unknownException_returnsUnknownError() {
        Result<Object> result = errorMapper.mapThrowable(new RuntimeException("Lỗi lạ"));

        assertTrue(result instanceof Result.Error);
        assertEquals(ErrorType.UNKNOWN, ((Result.Error<Object>) result).type);
    }

    @Test
    public void mapHttpCode_401_returnsAuthError() {
        Result<Object> result = errorMapper.mapHttpCode(401);

        assertTrue(result instanceof Result.Error);
        assertEquals(ErrorType.AUTH, ((Result.Error<Object>) result).type);
    }

    @Test
    public void mapHttpCode_500_returnsServerErrorWithCorrectMessage() {
        Result<Object> result = errorMapper.mapHttpCode(500);

        assertTrue(result instanceof Result.Error);
        Result.Error<Object> error = (Result.Error<Object>) result;
        assertEquals(ErrorType.SERVER, error.type);
        assertEquals("Máy chủ đang bận, thử lại sau", error.message);
    }

    @Test
    public void mapHttpCode_503_returnsServerError() {
        Result<Object> result = errorMapper.mapHttpCode(503);

        assertTrue(result instanceof Result.Error);
        assertEquals(ErrorType.SERVER, ((Result.Error<Object>) result).type);
    }

    @Test
    public void mapHttpCode_400_withServerBody_returnsServerMessage() {
        String body = "{\"error_code\":\"BAD_REQUEST\",\"message\":\"Chưa có giao dịch nào có danh mục trên máy chủ để huấn luyện. Vui lòng thêm và đồng bộ giao dịch trước.\"}";

        Result<Object> result = errorMapper.mapHttpCode(400, body);

        assertTrue(result instanceof Result.Error);
        Result.Error<Object> error = (Result.Error<Object>) result;
        assertEquals(ErrorType.SERVER, error.type);
        assertEquals("Chưa có giao dịch nào có danh mục trên máy chủ để huấn luyện. Vui lòng thêm và đồng bộ giao dịch trước.", error.message);
    }

    @Test
    public void mapHttpCode_400_withoutBody_returnsFallbackMessage() {
        Result<Object> result = errorMapper.mapHttpCode(400);

        assertTrue(result instanceof Result.Error);
        Result.Error<Object> error = (Result.Error<Object>) result;
        assertEquals(ErrorType.SERVER, error.type);
        assertEquals("Yêu cầu không hợp lệ, vui lòng thử lại", error.message);
    }
}
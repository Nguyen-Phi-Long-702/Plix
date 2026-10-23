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

    @Test
    public void mapAiBusy_returnsAiBusyErrorWithFriendlyMessage() {
        Result<Object> result = errorMapper.mapAiBusy();

        assertTrue(result instanceof Result.Error);
        Result.Error<Object> error = (Result.Error<Object>) result;
        assertEquals(ErrorType.AI_BUSY, error.type);
        assertEquals("AI đang bận, vui lòng nhập tay", error.message);
    }

    @Test
    public void mapHttpCode_503_withAiBusyBody_returnsAiBusyError() {
        String body = "{\"error_code\":\"AI_BUSY\",\"message\":\"AI đang bận, vui lòng nhập tay\"}";

        Result<Object> result = errorMapper.mapHttpCode(503, body);

        assertTrue(result instanceof Result.Error);
        Result.Error<Object> error = (Result.Error<Object>) result;
        assertEquals(ErrorType.AI_BUSY, error.type);
        assertEquals("AI đang bận, vui lòng nhập tay", error.message);
    }

    @Test
    public void mapHttpCode_503_withOtherBody_stillReturnsServerError() {
        String body = "{\"error_code\":\"INTERNAL_ERROR\",\"message\":\"x\"}";

        Result<Object> result = errorMapper.mapHttpCode(503, body);

        assertEquals(ErrorType.SERVER, ((Result.Error<Object>) result).type);
    }

    @Test
    public void mapHttpCode_503_withInvalidJsonBody_stillReturnsServerError() {
        Result<Object> result = errorMapper.mapHttpCode(503, "<html>Bad gateway</html>");

        assertEquals(ErrorType.SERVER, ((Result.Error<Object>) result).type);
    }

    @Test
    public void mapAuthHttpCode_invalidCredentials_returnsFriendlyAuthError() {
        String body = "{\"code\":400,\"error_code\":\"invalid_credentials\",\"msg\":\"Invalid login credentials\"}";

        Result<Object> result = errorMapper.mapAuthHttpCode(400, body);

        Result.Error<Object> error = (Result.Error<Object>) result;
        assertEquals(ErrorType.AUTH, error.type);
        assertEquals("Email hoặc mật khẩu không đúng", error.message);
    }

    @Test
    public void mapAuthHttpCode_userAlreadyExists_returnsEmailRegisteredMessage() {
        String body = "{\"code\":422,\"error_code\":\"user_already_exists\",\"msg\":\"User already registered\"}";

        Result<Object> result = errorMapper.mapAuthHttpCode(422, body);

        Result.Error<Object> error = (Result.Error<Object>) result;
        assertEquals(ErrorType.VALIDATION, error.type);
        assertEquals("Email này đã được đăng ký", error.message);
    }

    @Test
    public void mapAuthHttpCode_weakPassword_returnsWeakPasswordMessage() {
        String body = "{\"code\":422,\"error_code\":\"weak_password\",\"msg\":\"Password should be at least 8 characters.\"}";

        Result<Object> result = errorMapper.mapAuthHttpCode(422, body);

        Result.Error<Object> error = (Result.Error<Object>) result;
        assertEquals(ErrorType.VALIDATION, error.type);
        assertEquals("Mật khẩu quá yếu, vui lòng chọn mật khẩu mạnh hơn", error.message);
    }

    @Test
    public void mapAuthHttpCode_invalidEmail_returnsInvalidEmailMessage() {
        String body = "{\"code\":400,\"error_code\":\"email_address_invalid\",\"msg\":\"Email address is invalid\"}";

        Result<Object> result = errorMapper.mapAuthHttpCode(400, body);

        assertEquals("Email không hợp lệ", ((Result.Error<Object>) result).message);
    }

    @Test
    public void mapAuthHttpCode_429_returnsRetryLaterMessage() {
        Result<Object> result = errorMapper.mapAuthHttpCode(429, null);

        Result.Error<Object> error = (Result.Error<Object>) result;
        assertEquals(ErrorType.SERVER, error.type);
        assertEquals("Vui lòng thử lại sau ít phút", error.message);
    }

    @Test
    public void mapAuthHttpCode_500_returnsServerBusyMessage() {
        Result<Object> result = errorMapper.mapAuthHttpCode(500, null);

        assertEquals("Máy chủ đang bận, thử lại sau", ((Result.Error<Object>) result).message);
    }

    @Test
    public void mapAuthHttpCode_unknownError_neverShowsHttpCode() {
        Result<Object> result = errorMapper.mapAuthHttpCode(422, "{\"code\":422,\"error_code\":\"signup_disabled\"}");

        Result.Error<Object> error = (Result.Error<Object>) result;
        assertEquals(ErrorType.UNKNOWN, error.type);
        assertEquals("Không thực hiện được, vui lòng kiểm tra lại thông tin", error.message);
    }
}
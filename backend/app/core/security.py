import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.core.config import SUPABASE_JWKS_URL

_jwks_client = jwt.PyJWKClient(SUPABASE_JWKS_URL)


def get_jwks_client() -> jwt.PyJWKClient:
    return _jwks_client


def decode_and_get_user_id(token: str, jwks_client: jwt.PyJWKClient) -> str:
    """Verify chữ ký thật (ES256, public key lấy từ JWKS Supabase) + hạn dùng.
    Trả về user_id (claim 'sub'). Raise lỗi PyJWT nếu token sai hoặc hết hạn.
    Hàm thuần, không phụ thuộc FastAPI -> dùng trực tiếp trong unit test."""
    signing_key = jwks_client.get_signing_key_from_jwt(token)
    payload = jwt.decode(
        token, signing_key.key, algorithms=["ES256"], audience="authenticated"
    )
    user_id = payload.get("sub")
    if not user_id:
        raise jwt.InvalidTokenError("Token thiếu claim 'sub'")
    return user_id


security_scheme = HTTPBearer()


def verify_jwt(
    credentials: HTTPAuthorizationCredentials = Depends(security_scheme),
    jwks_client: jwt.PyJWKClient = Depends(get_jwks_client),
) -> str:
    """Dependency FastAPI — dùng Depends(verify_jwt) trong route cần bảo vệ."""
    try:
        return decode_and_get_user_id(credentials.credentials, jwks_client)
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, detail="Token đã hết hạn"
        )
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, detail="Token không hợp lệ"
        )
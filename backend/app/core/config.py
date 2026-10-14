import os
from dotenv import load_dotenv

load_dotenv()

SUPABASE_URL = os.getenv("SUPABASE_URL")
if not SUPABASE_URL:
    raise RuntimeError("Thiếu biến môi trường SUPABASE_URL — kiểm tra file .env")

SUPABASE_JWKS_URL = f"{SUPABASE_URL}/auth/v1/.well-known/jwks.json"

DATABASE_URL = os.getenv("DATABASE_URL")
if not DATABASE_URL:
    raise RuntimeError("Thiếu biến môi trường DATABASE_URL — kiểm tra file .env")

RETRAIN_COOLDOWN_SECONDS = int(os.getenv("RETRAIN_COOLDOWN_SECONDS", "3600"))

MAX_PULL_LIMIT = int(os.getenv("MAX_PULL_LIMIT", "500"))

# Không bắt buộc: thiếu key thì /parse-nl vẫn chạy nhánh rule-based, chỉ nhánh dự phòng Gemini báo "AI đang bận".
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
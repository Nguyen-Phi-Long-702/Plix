from typing import Union

from fastapi import APIRouter, Depends, Request
from fastapi.responses import JSONResponse

from app.core.security import verify_jwt
from app.models.error_response import ErrorResponse
from app.models.parse_nl import ParseNlItem, ParseNlRequest, ParseNlResponse
from app.services.ai.gemini_fallback import (
    AI_BUSY_ERROR_CODE,
    AI_BUSY_MESSAGE,
    SOURCE_GEMINI,
    GeminiUnavailableError,
    parse_with_gemini,
    should_call_gemini,
)
from app.services.ai.nl_parser import SOURCE_RULE, parse_quick_add

router = APIRouter()


@router.post("/parse-nl", response_model=ParseNlResponse)
async def parse_nl(
    body: ParseNlRequest,
    request: Request,
    user_id: str = Depends(verify_jwt),
) -> Union[ParseNlResponse, JSONResponse]:
    pool = request.app.state.db_pool
    parsed = await parse_quick_add(pool, user_id, body.text)
    if body.allow_gemini and should_call_gemini(parsed):
        try:
            gemini_items = await parse_with_gemini(pool, user_id, body.text)
        except GeminiUnavailableError:
            error = ErrorResponse(error_code=AI_BUSY_ERROR_CODE, message=AI_BUSY_MESSAGE)
            return JSONResponse(status_code=503, content=error.model_dump())
        if gemini_items:
            return ParseNlResponse(
                items=[
                    ParseNlItem(
                        amount=item.amount,
                        category_id=item.category_id,
                        confidence=item.confidence,
                        source=SOURCE_GEMINI,
                    )
                    for item in gemini_items
                ]
            )
    return ParseNlResponse(
        items=[
            ParseNlItem(
                amount=amount,
                category_id=category_id,
                confidence=confidence,
                source=SOURCE_RULE,
            )
            for amount, category_id, confidence in parsed
        ]
    )
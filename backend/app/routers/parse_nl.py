from fastapi import APIRouter, Depends, Request

from app.core.security import verify_jwt
from app.models.parse_nl import ParseNlItem, ParseNlRequest, ParseNlResponse
from app.services.ai.nl_parser import SOURCE_RULE, parse_quick_add

router = APIRouter()


@router.post("/parse-nl", response_model=ParseNlResponse)
async def parse_nl(
    body: ParseNlRequest,
    request: Request,
    user_id: str = Depends(verify_jwt),
) -> ParseNlResponse:
    parsed = await parse_quick_add(request.app.state.db_pool, user_id, body.text)
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
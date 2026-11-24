from typing import List, Literal, Optional
from pydantic import BaseModel, Field

class ChatReqModel(BaseModel):
    message: str = Field(..., description="User query or question")
    conversation_id: Optional[str] = Field(None, description="Client session ID for multi-turn history")

class ChatRespModel(BaseModel):
    status: Literal["success", "failed"]
    message: str = Field(..., description="Assistant response text")
    sources: Optional[List[str]] = Field(default_factory=list, description="Referenced knowledge sources")
    conversation_id: Optional[str] = Field(None, description="Conversation session ID")

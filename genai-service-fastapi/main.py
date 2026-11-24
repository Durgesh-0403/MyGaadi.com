import logging
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from app.schemas import ChatReqModel, ChatRespModel
from app.rag import rag_pipeline
from app.config import settings

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger("genai-service")

app = FastAPI(
    title="MyGaadi GenAI Chatbot",
    description="Intelligent RAG assistant for India's used-car marketplace (Aapki Gaadi, Aapka Bharosa)",
    version="1.0.0"
)

# Open CORS internally for Gateway forwarding
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def root():
    return {
        "service": "MyGaadi GenAI Chatbot",
        "status": "Running",
        "tagline": "Aapki Gaadi, Aapka Bharosa"
    }

@app.get("/health")
def health():
    return {"status": "UP", "service": "genai-service"}

@app.post("/chat", response_model=ChatRespModel)
def chat(req: ChatReqModel):
    if not req.message or not req.message.strip():
        raise HTTPException(status_code=400, detail="Query message cannot be empty")

    try:
        answer, sources, conv_id = rag_pipeline.ask(req.message.strip(), req.conversation_id)
        return ChatRespModel(
            status="success",
            message=answer,
            sources=sources,
            conversation_id=conv_id
        )
    except Exception as e:
        logger.error(f"Error processing chat request: {e}", exc_info=True)
        return ChatRespModel(
            status="failed",
            message="We encountered an issue processing your query. Please try again or visit MyGaadi.com support.",
            sources=[],
            conversation_id=req.conversation_id
        )

# > uvicorn main:app --reload --port 8083
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=settings.port, reload=True)

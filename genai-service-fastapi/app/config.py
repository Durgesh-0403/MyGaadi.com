import os
from pathlib import Path
from pydantic import ConfigDict
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    model_config = ConfigDict(env_file=".env", extra="allow")

    groq_api_key: str = os.getenv("GROQ_API_KEY", "")
    groq_model: str = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile")
    backend_service_url: str = os.getenv("BACKEND_SERVICE_URL", "http://localhost:8081")
    internal_api_key: str = os.getenv("INTERNAL_API_KEY", "your_internal_service_secret_token_32chars")
    chroma_persist_dir: str = os.getenv("CHROMA_PERSIST_DIR", str(Path(__file__).parent.parent / "chroma_db"))
    knowledge_base_dir: str = os.getenv("KNOWLEDGE_BASE_DIR", str(Path(__file__).parent.parent / "knowledge_base"))
    port: int = int(os.getenv("PORT", "8083"))

settings = Settings()

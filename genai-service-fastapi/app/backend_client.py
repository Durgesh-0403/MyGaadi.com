import logging
from typing import Any, Dict, List
import requests
from app.config import settings

logger = logging.getLogger(__name__)

class BackendClient:
    def __init__(self, base_url: str = settings.backend_service_url, api_key: str = settings.internal_api_key):
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key

    def fetch_car_inventory(self) -> List[Dict[str, Any]]:
        """
        Pulls verified public car inventory from mygaadi-backend for live RAG index.
        Car records omit seller personal identifiable information (PII).
        """
        url = f"{self.base_url}/internal/cars/export"
        headers = {
            "X-Internal-Api-Key": self.api_key,
            "Accept": "application/json"
        }

        try:
            response = requests.get(url, headers=headers, timeout=5)
            if response.status_code == 200:
                raw_data = response.json()
                if isinstance(raw_data, dict) and "content" in raw_data:
                    car_list = raw_data.get("content", [])
                elif isinstance(raw_data, list):
                    car_list = raw_data
                else:
                    car_list = []
                logger.info(f"Successfully fetched {len(car_list)} cars from backend for GenAI RAG")
                return car_list
            else:
                logger.warning(f"Backend returned status {response.status_code} on car export: {response.text}")
                return []
        except Exception as e:
            logger.warning(f"Could not connect to backend at {url} to fetch live inventory: {e}")
            return []

backend_client = BackendClient()

import os
import uuid
import logging
from typing import Dict, List, Tuple, Any
from app.config import settings
from app.vectorstore import LocalMemoryVectorStore, Document, build_knowledge_base_store
from app.backend_client import backend_client

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """You are GaadiMitra, a friendly, human automotive advisor for MyGaadi.com — India's verified pre-owned car marketplace.

Core Guidelines:
1. Speak in a warm, polite, and natural human conversational tone.
2. Keep your answers brief, clear, and directly to the point (2 to 4 sentences or 2-3 short bullet points max). Do NOT dump excessive legal policies or huge walls of text unless specifically asked.
3. Answer only the necessary information that directly addresses the user's specific query.
4. Token Booking: 2% of listed price (min ₹5,000, max ₹50,000), 100% refundable if the car doesn't pass showroom inspection.
5. Seller KYC: All dealership showrooms submit PAN or Aadhaar for admin verification so every listing is verified.
6. When recommending cars from inventory, give 1-2 relevant vehicles with model, year, price, and city.
7. Always format prices in Indian format (e.g. ₹6,40,000).
"""

class ConversationManager:
    def __init__(self, max_turns: int = 6):
        self.max_turns = max_turns
        self.history: Dict[str, List[Dict[str, str]]] = {}

    def get_history(self, conv_id: str) -> List[Dict[str, str]]:
        return self.history.get(conv_id, [])

    def add_turn(self, conv_id: str, user_msg: str, assistant_msg: str):
        if conv_id not in self.history:
            self.history[conv_id] = []
        self.history[conv_id].append({"role": "user", "content": user_msg})
        self.history[conv_id].append({"role": "assistant", "content": assistant_msg})
        if len(self.history[conv_id]) > self.max_turns * 2:
            self.history[conv_id] = self.history[conv_id][-(self.max_turns * 2):]

class RAGPipeline:
    def __init__(self):
        self.vectorstore: LocalMemoryVectorStore = None
        self.conversation_manager = ConversationManager(max_turns=6)
        self.live_cars: List[Dict[str, Any]] = []
        self.initialize_store()

    def initialize_store(self):
        logger.info("Initializing vector store from knowledge base...")
        try:
            self.live_cars = backend_client.fetch_car_inventory()
        except Exception as e:
            logger.warning(f"Could not fetch initial car inventory: {e}")
            self.live_cars = []

        self.vectorstore = build_knowledge_base_store(
            kb_dir=settings.knowledge_base_dir,
            car_inventory=self.live_cars
        )

    def refresh_inventory(self):
        try:
            cars = backend_client.fetch_car_inventory()
            if isinstance(cars, list):
                # Only keep cars that are strictly AVAILABLE
                self.live_cars = [c for c in cars if str(c.get('status', '')).upper() == 'AVAILABLE']
        except Exception as e:
            logger.warning(f"Inventory refresh notice: {e}")

    def ask(self, query: str, conversation_id: str = None) -> Tuple[str, List[str], str]:
        if not conversation_id:
            conversation_id = str(uuid.uuid4())

        # Attempt to refresh latest inventory
        self.refresh_inventory()

        # Retrieve relevant context from vector store
        retrieved_docs: List[Document] = self.vectorstore.similarity_search(query, k=5)
        sources = list(dict.fromkeys([doc.metadata.get("source", "knowledge_base") for doc in retrieved_docs]))

        # Format live inventory context
        inventory_context = self._format_live_inventory_context(query)
        context_text = "\n\n---\n\n".join([doc.page_content for doc in retrieved_docs])
        if inventory_context:
            context_text = f"Live Marketplace Inventory:\n{inventory_context}\n\n---\n\nKnowledge Base:\n{context_text}"

        history = self.conversation_manager.get_history(conversation_id)

        # Call Groq LLM if key is present
        groq_key = settings.groq_api_key.strip()
        if groq_key and not groq_key.startswith("your_") and len(groq_key) > 10:
            try:
                from groq import Groq
                client = Groq(api_key=groq_key)

                messages = [{"role": "system", "content": f"{SYSTEM_PROMPT}\n\nContext Information:\n{context_text}"}]
                for h in history:
                    messages.append({"role": h["role"], "content": h["content"]})
                messages.append({"role": "user", "content": query})

                chat_completion = client.chat.completions.create(
                    messages=messages,
                    model=settings.groq_model,
                    temperature=0.3,
                    max_tokens=1024,
                )
                answer = chat_completion.choices[0].message.content
                self.conversation_manager.add_turn(conversation_id, query, answer)
                return answer, sources, conversation_id
            except Exception as e:
                logger.warning(f"Groq API call encountered an error: {e}. Falling back to intelligent local synthesis.")

        # High-Quality Humanized Offline Synthesizer
        answer = self._offline_synthesize(query, retrieved_docs, history)
        self.conversation_manager.add_turn(conversation_id, query, answer)
        return answer, sources, conversation_id

    def _format_live_inventory_context(self, query: str) -> str:
        if not self.live_cars:
            return "NOTICE: There are NO cars currently available on the marketplace. All vehicles have been SOLD or RESERVED. If the user asks about cars or available inventory, explicitly inform them that all certified cars are currently sold out and fresh inventory is arriving soon."

        lines = []
        for car in self.live_cars[:8]:
            p = car.get('price', 0)
            km = car.get('kmDriven', 0)
            p_str = f"₹{p:,}" if isinstance(p, (int, float)) else f"₹{p}"
            km_str = f"{km:,} km" if isinstance(km, (int, float)) else f"{km} km"
            lines.append(
                f"- Car #{car.get('id')}: {car.get('title')} ({car.get('manufactureYear') or car.get('year')}) "
                f"| Price: {p_str} | Odometer: {km_str} | Fuel: {car.get('fuelType')} | "
                f"Transmission: {car.get('transmission')} | Location: {car.get('city')}, {car.get('state')} "
                f"| Status: AVAILABLE"
            )
        return "\n".join(lines)

    def _offline_synthesize(self, query: str, docs: List[Document], history: List[Dict[str, str]]) -> str:
        q_lower = query.lower()

        # 1. Car Search & Recommendations (Concise, 2 cars max)
        if any(w in q_lower for w in ["car", "cars", "swift", "creta", "city", "innova", "baleno", "thar", "hyundai", "maruti", "honda", "tata", "ignis", "available", "inventory", "stock", "buy", "recommend"]):
            if self.live_cars:
                matching = []
                for c in self.live_cars:
                    text_blob = f"{c.get('title', '')} {c.get('brand', '')} {c.get('model', '')} {c.get('city', '')} {c.get('fuelType', '')}".lower()
                    if any(term in text_blob for term in q_lower.split() if len(term) > 2 and term not in ["car", "cars", "the", "for", "and", "show", "give", "tell", "all"]):
                        matching.append(c)

                selected = matching[:2] if matching else self.live_cars[:2]
                response_lines = ["Here are verified cars available on MyGaadi.com:"]
                for car in selected:
                    p = car.get('price', 0)
                    p_str = f"₹{p:,.0f}" if isinstance(p, (int, float)) else f"₹{p}"
                    response_lines.append(
                        f"• **{car.get('title')}** ({car.get('manufactureYear') or car.get('year')}) — {p_str} in {car.get('city')}, {car.get('fuelType')} ({car.get('transmission')})"
                    )
                response_lines.append("You can book a test drive reservation with a 2% refundable token deposit on our website!")
                return "\n".join(response_lines)
            else:
                return (
                    "All certified vehicles currently on MyGaadi.com have been sold! "
                    "Certified dealerships are onboarding fresh inventory daily. "
                    "Please check back shortly or explore 'Buy Cars' to see upcoming verified listings."
                )

        # 2. Refund specific question
        if any(w in q_lower for w in ["refund", "money back", "return money"]):
            return (
                "Yes, your token deposit is 100% refundable! If you inspect the car at the showroom and find any mechanical discrepancy or simply decide not to proceed before the sale contract, your deposit is returned in full."
            )

        # 3. Token Booking rules
        if any(w in q_lower for w in ["token", "booking", "how to book", "reserve", "deposit", "pay"]):
            return (
                "To book a car on MyGaadi.com, you place a 2% token deposit (min ₹5,000 to max ₹50,000). This reserves the vehicle for your showroom visit and test drive. The deposit is 100% refundable if the car doesn't meet your expectations."
            )

        # 4. Seller KYC
        if any(w in q_lower for w in ["kyc", "verify seller", "verification", "aadhaar", "pan", "showroom", "seller"]):
            return (
                "All dealership showrooms on MyGaadi must submit government PAN or Aadhaar verification for admin approval before listings go live. This guarantees you are dealing directly with authentic, verified sellers."
            )

        # 5. RTO Ownership Transfer
        if any(w in q_lower for w in ["rto", "transfer", "form 29", "form 30", "noc", "paperwork"]):
            return (
                "For vehicle ownership transfer, you will need Form 29 & 30 signed by the seller, original RC, valid insurance, PUC, and buyer address proof. Verified dealerships assist you with the entire RTO transfer process."
            )

        # 6. Inspection & Quality
        if any(w in q_lower for w in ["inspect", "checklist", "engine", "damage", "accident", "flood", "odometer"]):
            return (
                "Every car listed by verified sellers on MyGaadi comes with a comprehensive 140-point technical inspection covering engine health, chassis integrity, and non-accidental verification."
            )

        # 7. Default friendly human reply
        return (
            "Hello! I'm GaadiMitra, your car advisor. You can ask me about available cars, our 2% refundable token booking, showroom KYC verification, or RTO paperwork. How can I help you today?"
        )

        # 5. Pre-Purchase Inspection Checklist
        if any(w in q_lower for w in ["inspect", "checklist", "engine", "damage", "accident", "flood", "odometer"]):
            return (
                "Expert 5-Point **Pre-Purchase Used Car Checklist**:\n\n"
                "1. **Engine Condition**: Check engine oil color (clean amber is healthy; thick black sludge indicates neglected oil changes). Check coolant for milky emulsion.\n"
                "2. **Chassis & Aprons**: Inspect factory spot-welds under the bonnet and around the spare wheel well for collision repair or non-factory sealant.\n"
                "3. **Flood Check**: Pull back door rubbers and inspect underneath floor carpets and seat rails for dried rust, silt, or foul damp odors.\n"
                "4. **Odometer Verification**: Compare pedal rubber and steering wheel wear against the stated kilometers and dealer service history logs.\n"
                "5. **Suspension & Brakes**: Conduct a test drive over speed bumps to listen for knocking ball joints or steering rack play."
            )

        # 6. Fuel & Transmission Advice
        if any(w in q_lower for w in ["fuel", "petrol", "diesel", "cng", "ev", "transmission", "amt", "cvt", "automatic"]):
            return (
                "Automotive Advisory: **Fuel & Transmission Selection** in India:\n\n"
                "• **Petrol**: Smooth, low maintenance, and ideal for city commutes under 35 km/day. Has full 15-year registration validity in Delhi-NCR.\n"
                "• **Diesel**: Strong low-end torque and 18–22 km/l highway economy. Best for drivers exceeding 1,500 km/month (note: 10-year limit in NCR).\n"
                "• **CNG**: Ultra-low running cost (approx ₹2.50 to ₹3.20/km). Great for daily high-mileage urban commuting.\n"
                "• **AMT / AGS**: Cost-effective clutchless manual for dense traffic.\n"
                "• **CVT / Torque Converter / DCT**: Smooth gear transitions without jerkiness."
            )

        # 7. General Context Synthesis from Retrieved Knowledge Chunks
        if docs:
            top_chunk = docs[0].page_content.strip()
            lines = [line.strip() for line in top_chunk.split("\n") if line.strip() and not line.startswith("#")]
            summary = " ".join(lines[:4])
            return (
                f"{summary}\n\n"
                "For more guidance, explore our verified listings on **MyGaadi.com** — *Buy • Sell • Drive Better*!"
            )

        return (
            "Namaste! I am **GaadiMitra**, your AI automotive assistant on MyGaadi.com.\n\n"
            "I can assist you with:\n"
            "• **Car Recommendations & Live Inventory**\n"
            "• **Token Booking & 100% Refund Rules** (2% deposit, 15-min lock)\n"
            "• **RTO Transfer Documents** (Form 29, 30 & NOC)\n"
            "• **Vehicle Inspection Checklists**\n"
            "• **Seller Showroom KYC Verification**\n\n"
            "What would you like to explore today?"
        )

rag_pipeline = RAGPipeline()

import React, { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import config from '../services/config'

function AiAssistantModal({ isOpen, onClose }) {
    const [messages, setMessages] = useState([
        {
            sender: 'ai',
            text: 'Hello! I am GaadiMitra, your car advisor. Ask me anything about our 2% refundable token booking, verified cars, or showroom KYC!'
        }
    ])
    const [input, setInput] = useState('')
    const [loading, setLoading] = useState(false)
    const messagesEndRef = useRef(null)

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }

    useEffect(() => {
        if (isOpen) {
            scrollToBottom()
        }
    }, [messages, isOpen])

    if (!isOpen) return null

    const handleSendPrompt = async (promptText) => {
        if (!promptText.trim() || loading) return

        const userMsg = promptText.trim()
        setMessages(prev => [...prev, { sender: 'user', text: userMsg }])
        setInput('')
        setLoading(true)

        try {
            // Call GenAI microservice
            const response = await axios.post(`${config.BASE_URL}/api/chat`, { message: userMsg, prompt: userMsg }, { timeout: 4500 })
            const reply = response.data?.message || response.data?.reply || response.data?.data
            if (reply) {
                setMessages(prev => [...prev, { sender: 'ai', text: reply }])
                return
            }
            throw new Error('Local fallback')
        } catch (error) {
            // Human, brief, and concise local fallback responses
            const lower = userMsg.toLowerCase()
            let localReply = "At MyGaadi.com, all cars are inspected by certified technicians. You can reserve any car with a 2% refundable token deposit."

            if (lower.includes('refund')) {
                localReply = "Yes, your token deposit is 100% refundable! If you inspect the car at the showroom and decide not to buy it, your full deposit will be credited back immediately."
            } else if (lower.includes('token') || lower.includes('booking') || lower.includes('reserve') || lower.includes('deposit')) {
                localReply = "To reserve a car, you pay a 2% token deposit (between ₹5,000 and ₹50,000). This holds the car for your inspection at the showroom and is 100% refundable if you don't proceed."
            } else if (lower.includes('kyc') || lower.includes('seller') || lower.includes('dealer')) {
                localReply = "All dealership showrooms must submit government PAN or Aadhaar card for administrator verification before listings go live. This ensures 100% genuine cars and sellers."
            } else if (lower.includes('rto') || lower.includes('transfer') || lower.includes('paperwork')) {
                localReply = "For ownership transfer, you'll need Form 29 & 30 signed by the seller, the original RC, insurance, PUC, and buyer ID proof. Verified dealerships assist you with the entire transfer process."
            } else if (lower.includes('inspect') || lower.includes('damage') || lower.includes('check')) {
                localReply = "Every vehicle listed on MyGaadi includes a comprehensive 140-point technical engine and chassis inspection report for complete peace of mind."
            }

            setMessages(prev => [...prev, { sender: 'ai', text: localReply }])
        } finally {
            setLoading(false)
        }
    }

    const handleSubmit = (e) => {
        e.preventDefault()
        handleSendPrompt(input)
    }

    return (
        /* Floating Corner Chatbot Window */
        <div 
            className="ai-chatbot-window shadow-lg"
            style={{
                position: 'fixed',
                bottom: '24px',
                right: '24px',
                width: '385px',
                maxWidth: 'calc(100vw - 32px)',
                height: '560px',
                maxHeight: 'calc(100vh - 80px)',
                zIndex: 1060,
                borderRadius: '18px',
                border: '1px solid #E2E8F0',
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden'
            }}
        >
            {/* Header with high contrast white text */}
            <div 
                className="d-flex align-items-center justify-content-between px-3 py-3" 
                style={{ background: 'linear-gradient(135deg, #0B2545 0%, #153B6B 100%)' }}
            >
                <div className="d-flex align-items-center gap-2">
                    <div className="position-relative">
                        <span className="p-2 rounded-circle bg-white bg-opacity-15 text-white d-inline-flex align-items-center justify-content-center" style={{ width: '38px', height: '38px' }}>
                            <i className="bi bi-robot fs-5 text-warning"></i>
                        </span>
                        <span className="position-absolute bottom-0 end-0 p-1 bg-success border border-light rounded-circle"></span>
                    </div>
                    <div>
                        <h6 className="fw-bold mb-0 text-white" style={{ fontSize: '0.94rem', letterSpacing: '0.2px' }}>
                            GaadiMitra AI Assistant
                        </h6>
                        <small className="text-white-50" style={{ fontSize: '0.72rem' }}>
                            Online • MyGaadi Automotive Advisor
                        </small>
                    </div>
                </div>
                <button 
                    type="button" 
                    className="btn btn-sm btn-link text-white text-decoration-none p-1 opacity-75 hover-opacity-100" 
                    onClick={onClose}
                    title="Close Chat"
                >
                    <i className="bi bi-x-lg fs-6"></i>
                </button>
            </div>

            {/* Messages Body */}
            <div 
                className="p-3 d-flex flex-column gap-2" 
                style={{ 
                    flexGrow: 1, 
                    overflowY: 'auto', 
                    background: '#F8FAFC',
                    fontSize: '0.86rem'
                }}
            >
                {messages.map((msg, index) => (
                    <div 
                        key={index}
                        className={`d-flex ${msg.sender === 'user' ? 'justify-content-end' : 'justify-content-start'}`}
                    >
                        <div 
                            className="p-2 px-3 shadow-sm" 
                            style={{
                                maxWidth: '85%',
                                borderRadius: msg.sender === 'user' ? '16px 16px 2px 16px' : '16px 16px 16px 2px',
                                background: msg.sender === 'user' ? '#0B2545' : '#FFFFFF',
                                color: msg.sender === 'user' ? '#FFFFFF' : '#1E293B',
                                border: msg.sender === 'user' ? 'none' : '1px solid #E2E8F0',
                                lineHeight: '1.5',
                                whiteSpace: 'pre-wrap',
                                wordBreak: 'break-word',
                                fontSize: '0.86rem'
                            }}
                        >
                            {msg.text}
                        </div>
                    </div>
                ))}

                {loading && (
                    <div className="d-flex justify-content-start">
                        <div className="bg-white p-2 px-3 rounded-3 shadow-sm text-muted small d-flex align-items-center gap-2 border">
                            <div className="spinner-border spinner-border-sm text-primary" role="status" style={{ width: '13px', height: '13px' }}></div>
                            <span>GaadiMitra is typing...</span>
                        </div>
                    </div>
                )}
                <div ref={messagesEndRef} />
            </div>

            {/* Quick Questions: Clean Vertical List (Per User Request) */}
            <div className="px-3 py-2 bg-white border-top">
                <div className="d-flex justify-content-between align-items-center mb-1">
                    <span className="text-muted fw-semibold" style={{ fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                        Suggested Questions
                    </span>
                </div>
                <div className="d-flex flex-column gap-1">
                    <button 
                        type="button" 
                        className="btn btn-sm text-start py-1 px-2 rounded-2 border d-flex align-items-center justify-content-between bg-light"
                        onClick={() => handleSendPrompt("How does token booking work?")}
                        style={{ fontSize: '0.78rem', borderColor: '#E2E8F0', color: '#1E293B' }}
                    >
                        <span><i className="bi bi-shield-check text-primary me-2"></i>Token booking rules</span>
                        <i className="bi bi-chevron-right text-muted" style={{ fontSize: '0.65rem' }}></i>
                    </button>
                    <button 
                        type="button" 
                        className="btn btn-sm text-start py-1 px-2 rounded-2 border d-flex align-items-center justify-content-between bg-light"
                        onClick={() => handleSendPrompt("Is the token deposit refundable?")}
                        style={{ fontSize: '0.78rem', borderColor: '#E2E8F0', color: '#1E293B' }}
                    >
                        <span><i className="bi bi-arrow-repeat text-success me-2"></i>Refund policy</span>
                        <i className="bi bi-chevron-right text-muted" style={{ fontSize: '0.65rem' }}></i>
                    </button>
                    <button 
                        type="button" 
                        className="btn btn-sm text-start py-1 px-2 rounded-2 border d-flex align-items-center justify-content-between bg-light"
                        onClick={() => handleSendPrompt("How does seller KYC verification work?")}
                        style={{ fontSize: '0.78rem', borderColor: '#E2E8F0', color: '#1E293B' }}
                    >
                        <span><i className="bi bi-patch-check-fill text-warning me-2"></i>KYC verification</span>
                        <i className="bi bi-chevron-right text-muted" style={{ fontSize: '0.65rem' }}></i>
                    </button>
                </div>
            </div>

            {/* Footer Input Form */}
            <div className="p-2 px-3 bg-white border-top">
                <form onSubmit={handleSubmit} className="d-flex align-items-center gap-2">
                    <input
                        type="text"
                        className="form-control rounded-pill px-3 py-1 border shadow-none"
                        placeholder="Ask a question..."
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        style={{ fontSize: '0.86rem' }}
                    />
                    <button 
                        type="submit" 
                        className="btn rounded-circle d-flex align-items-center justify-content-center text-white"
                        style={{ width: '36px', height: '36px', flexShrink: 0, backgroundColor: '#FF7A1A', border: 'none' }}
                        disabled={!input.trim() || loading}
                    >
                        <i className="bi bi-send-fill" style={{ fontSize: '0.8rem' }}></i>
                    </button>
                </form>
            </div>
        </div>
    )
}

export default AiAssistantModal

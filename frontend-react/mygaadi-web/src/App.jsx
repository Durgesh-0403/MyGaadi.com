import React, { createContext, useEffect, useState } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { ToastContainer } from 'react-toastify'
import { useDispatch } from 'react-redux'

import Navbar from './component/Navbar'
import Footer from './component/Footer'
import AiAssistantModal from './component/AiAssistantModal'

import Home from './pages/Home'
import BrowseCars from './pages/BrowseCars'
import CarDetails from './pages/CarDetails'
import Login from './pages/Login'
import AdminLogin from './pages/AdminLogin'
import Register from './pages/Register'
import Wishlist from './pages/Wishlist'
import MyBookings from './pages/MyBookings'
import SellerDashboard from './pages/SellerDashboard'
import AdminDashboard from './pages/AdminDashboard'

import { getStoredToken, getStoredUser } from './services/authservice'
import { getWishlist } from './services/buyerservice'
import { setWishlistAction } from './slices/WishlistSlice'

export const UserContext = createContext()

function App() {
    const dispatch = useDispatch()
    const [loginStatus, setLoginStatus] = useState(false)
    const [user, setUser] = useState(null)
    const [aiModalOpen, setAiModalOpen] = useState(false)

    // Ensure clean default theme across all browsers
    useEffect(() => {
        document.documentElement.removeAttribute('data-bs-theme')
        localStorage.removeItem('mygaadi_theme')
    }, [])

    // Check existing stored authentication on initial mount
    useEffect(() => {
        const token = getStoredToken()
        const storedUser = getStoredUser()
        if (token && storedUser) {
            setLoginStatus(true)
            setUser(storedUser)

            // If user is buyer, preload their wishlist into Redux store
            if (storedUser.userRole === 'ROLE_BUYER') {
                getWishlist(token)
                    .then(res => {
                        if (res.data) {
                            dispatch(setWishlistAction(res.data))
                        }
                    })
                    .catch(() => {})
            }
        }
    }, [dispatch])

    return (
        <UserContext.Provider value={{ loginStatus, setLoginStatus, user, setUser }}>
            <div className="d-flex flex-column min-vh-100">
                {/* Global Top Navbar */}
                <Navbar onOpenAi={() => setAiModalOpen(true)} />

                {/* Main Content Router */}
                <main className="flex-grow-1">
                    <Routes>
                        <Route path="/" element={<Home />} />
                        <Route path="/cars" element={<BrowseCars />} />
                        <Route path="/cars/:id" element={<CarDetails />} />
                        <Route path="/login" element={<Login />} />
                        <Route path="/admin/login" element={<AdminLogin />} />
                        <Route path="/register" element={<Register />} />
                        <Route path="/wishlist" element={<Wishlist />} />
                        <Route path="/my-bookings" element={<MyBookings />} />
                        <Route path="/seller/dashboard" element={<SellerDashboard />} />
                        <Route path="/admin/dashboard" element={<AdminDashboard />} />
                        <Route path="*" element={<Navigate to="/" replace />} />
                    </Routes>
                </main>

                {/* Floating AI Assistant Button (corner toggle) */}
                {!aiModalOpen && (
                    <button
                        type="button"
                        className="ai-assistant-btn"
                        onClick={() => setAiModalOpen(true)}
                        title="Ask GaadiMitra AI Assistant"
                    >
                        <span className="badge rounded-circle bg-success p-1"></span>
                        <span>AI Assistant</span>
                    </button>
                )}

                {/* AI Assistant Floating Corner Widget */}
                <AiAssistantModal isOpen={aiModalOpen} onClose={() => setAiModalOpen(false)} />

                {/* Global Footer */}
                <Footer />

                {/* Global Toast Notifications */}
                <ToastContainer position="top-right" autoClose={3000} hideProgressBar={false} />
            </div>
        </UserContext.Provider>
    )
}

export default App

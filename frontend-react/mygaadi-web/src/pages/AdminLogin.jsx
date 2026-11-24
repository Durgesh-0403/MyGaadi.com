import React, { useContext, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { adminSignin, setSessionAuth } from '../services/authservice'
import { toast } from 'react-toastify'
import { UserContext } from '../App'

function AdminLogin() {
    const navigate = useNavigate()
    const { setLoginStatus, setUser } = useContext(UserContext)

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [loading, setLoading] = useState(false)

    const handleAdminSignIn = async (e) => {
        e.preventDefault()
        if (!email.trim() || !password.trim()) {
            toast.error('Please enter administrator email and password')
            return
        }

        setLoading(true)
        try {
            const response = await adminSignin(email.trim(), password)
            const result = response.data

            if (result && result.jwt) {
                if (result.userRole !== 'ROLE_ADMIN') {
                    toast.error('Access Denied: This portal is strictly restricted to platform administrators.')
                    setLoading(false)
                    return
                }

                toast.success(`Administrator authentication successful. Welcome, ${result.completeName || 'Admin'}!`)
                setSessionAuth(result.jwt, result)
                setLoginStatus(true)
                setUser(result)
                navigate('/admin/dashboard')
            } else {
                toast.error(result.message || 'Invalid administrator credentials')
            }
        } catch (error) {
            console.error('Admin Sign-in error:', error)
            const errMsg = error.response?.data?.message || 'Administrator authentication failed.'
            toast.error(errMsg)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="container py-5 my-4">
            <div className="row justify-content-center">
                <div className="col-md-7 col-lg-5">
                    <div className="card border-0 shadow-lg rounded-4 overflow-hidden bg-white">
                        {/* Administrator Restricted Banner */}
                        <div 
                            className="p-4 text-center text-white position-relative" 
                            style={{ background: 'linear-gradient(135deg, #0A192F 0%, #153B6B 100%)' }}
                        >
                            <div className="mb-2">
                                <Link to="/">
                                    <img 
                                        src="/logo.png" 
                                        alt="MyGaadi.com" 
                                        style={{ height: '42px', objectFit: 'contain', filter: 'brightness(0) invert(1)' }} 
                                    />
                                </Link>
                            </div>
                            <div 
                                className="d-inline-flex align-items-center justify-content-center p-2 px-3 rounded-pill mb-2 mt-1 shadow-sm"
                                style={{ backgroundColor: 'rgba(255, 255, 255, 0.18)', border: '1px solid rgba(255, 255, 255, 0.25)' }}
                            >
                                <i className="bi bi-shield-lock-fill text-warning me-2 fs-6"></i>
                                <span className="small fw-bold text-white">Administrator Restricted Area</span>
                            </div>
                            <h4 className="fw-bold mb-1 text-white" style={{ color: '#FFFFFF', textShadow: '0 2px 4px rgba(0,0,0,0.4)', letterSpacing: '0.3px' }}>
                                Platform Governance
                            </h4>
                            <p className="small mb-0 text-white" style={{ color: '#E2E8F0', opacity: 0.9 }}>
                                Internal Control Console • Moderation & Verification
                            </p>
                        </div>

                        {/* Security notice pill */}
                        <div className="bg-warning-subtle text-warning-emphasis px-4 py-2 small d-flex align-items-center gap-2 border-bottom">
                            <i className="bi bi-exclamation-triangle-fill flex-shrink-0"></i>
                            <span>Authorized personnel only. All access attempts are monitored and audited.</span>
                        </div>

                        {/* Admin Form */}
                        <div className="card-body p-4 p-md-5">
                            <form onSubmit={handleAdminSignIn}>
                                <div className="mb-3">
                                    <label htmlFor="adminEmail" className="form-label small fw-semibold text-secondary">
                                        Admin Email
                                    </label>
                                    <div className="input-group">
                                        <span className="input-group-text bg-light text-muted border-end-0">
                                            <i className="bi bi-person-badge"></i>
                                        </span>
                                        <input
                                            type="email"
                                            id="adminEmail"
                                            className="form-control border-start-0 ps-1"
                                            placeholder="admin@mygaadi.com"
                                            value={email}
                                            onChange={e => setEmail(e.target.value)}
                                            required
                                            autoFocus
                                        />
                                    </div>
                                </div>

                                <div className="mb-4">
                                    <label htmlFor="adminPassword" className="form-label small fw-semibold text-secondary">
                                        Admin Master Password
                                    </label>
                                    <div className="input-group">
                                        <span className="input-group-text bg-light text-muted border-end-0">
                                            <i className="bi bi-key-fill"></i>
                                        </span>
                                        <input
                                            type="password"
                                            id="adminPassword"
                                            className="form-control border-start-0 ps-1"
                                            placeholder="••••••••••••"
                                            value={password}
                                            onChange={e => setPassword(e.target.value)}
                                            required
                                        />
                                    </div>
                                </div>

                                <button
                                    type="submit"
                                    className="btn btn-primary w-100 py-3 fw-bold mb-3 shadow-sm d-flex align-items-center justify-content-center gap-2"
                                    disabled={loading}
                                    style={{ backgroundColor: '#0B2545', borderColor: '#0B2545' }}
                                >
                                    {loading ? (
                                        <>
                                            <span className="spinner-border spinner-border-sm" role="status"></span>
                                            Authenticating...
                                        </>
                                    ) : (
                                        <>
                                            <i className="bi bi-box-arrow-in-right"></i>
                                            Sign In to Admin Console
                                        </>
                                    )}
                                </button>

                                <div className="text-center pt-2">
                                    <Link to="/login" className="small text-muted text-decoration-none">
                                        <i className="bi bi-arrow-left me-1"></i> Return to Buyer & Seller Login
                                    </Link>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default AdminLogin

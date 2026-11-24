import React, { useContext, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { setSessionAuth, userSignin } from '../services/authservice'
import { toast } from 'react-toastify'
import { UserContext } from '../App'

function Login() {
    const navigate = useNavigate()
    const { setLoginStatus, setUser } = useContext(UserContext)

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [loading, setLoading] = useState(false)

    const handleSignInClick = async (e) => {
        e.preventDefault()
        if (!email.trim() || !password.trim()) {
            toast.error('Please enter both email and password')
            return
        }

        setLoading(true)
        try {
            const response = await userSignin(email.trim(), password)
            const result = response.data

            if (result && result.jwt) {
                toast.success(`Welcome back, ${result.completeName || 'User'}!`)
                setSessionAuth(result.jwt, result)
                setLoginStatus(true)
                setUser(result)

                if (result.userRole === 'ROLE_SELLER') {
                    navigate('/seller/dashboard')
                } else if (result.userRole === 'ROLE_BUYER') {
                    navigate('/cars')
                } else {
                    navigate('/admin/dashboard')
                }
            } else {
                toast.error(result.message || 'Invalid email or password')
            }
        } catch (error) {
            console.error('Sign-in error:', error)
            const errMsg = error.response?.data?.message || 'Login failed. Please check your credentials.'
            toast.error(errMsg)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="container py-5 my-3">
            <div className="row justify-content-center">
                <div className="col-md-8 col-lg-5">
                    <div className="card border-0 shadow-lg rounded-4 overflow-hidden bg-white">
                        {/* Header Banner */}
                        <div className="p-4 text-center bg-white border-bottom">
                            <Link to="/">
                                <img 
                                    src="/logo.png" 
                                    alt="MyGaadi.com" 
                                    style={{ height: '48px', objectFit: 'contain' }} 
                                    className="mb-2"
                                />
                            </Link>
                            <h4 className="fw-bold mb-1" style={{ color: '#0B2545' }}>Buyer & Seller Login</h4>
                            <p className="small text-muted mb-0">
                                Sign in to browse, book, or manage your showroom inventory
                            </p>
                        </div>

                        {/* Form Body */}
                        <div className="card-body p-4 p-md-5">
                            <form onSubmit={handleSignInClick}>
                                <div className="mb-3">
                                    <label htmlFor="inputEmail" className="form-label small fw-semibold text-secondary">
                                        Email Address
                                    </label>
                                    <div className="input-group">
                                        <span className="input-group-text bg-light text-muted border-end-0">
                                            <i className="bi bi-envelope"></i>
                                        </span>
                                        <input
                                            type="email"
                                            id="inputEmail"
                                            className="form-control border-start-0 ps-1"
                                            placeholder="name@example.com"
                                            value={email}
                                            onChange={e => setEmail(e.target.value)}
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="mb-4">
                                    <label htmlFor="inputPassword" className="form-label small fw-semibold text-secondary">
                                        Password
                                    </label>
                                    <div className="input-group">
                                        <span className="input-group-text bg-light text-muted border-end-0">
                                            <i className="bi bi-lock"></i>
                                        </span>
                                        <input
                                            type="password"
                                            id="inputPassword"
                                            className="form-control border-start-0 ps-1"
                                            placeholder="Enter your password"
                                            value={password}
                                            onChange={e => setPassword(e.target.value)}
                                            required
                                        />
                                    </div>
                                </div>

                                <button
                                    type="submit"
                                    className="btn btn-mg-primary w-100 py-3 fw-bold mb-3 shadow-sm"
                                    disabled={loading}
                                    style={{ backgroundColor: '#FF7A1A', borderColor: '#FF7A1A' }}
                                >
                                    {loading ? (
                                        <>
                                            <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                                            Signing in...
                                        </>
                                    ) : (
                                        'Sign In'
                                    )}
                                </button>

                                <div className="text-center pt-3 border-top d-flex flex-column gap-2">
                                    <div>
                                        <span className="small text-muted">Don't have an account yet? </span>
                                        <Link to="/register" className="small fw-bold text-primary text-decoration-none">
                                            Create Account
                                        </Link>
                                    </div>
                                    <div>
                                        <span className="small text-muted">Looking to sell cars? </span>
                                        <Link to="/register?role=seller" className="small fw-semibold text-secondary text-decoration-none">
                                            Register as Dealership Showroom
                                        </Link>
                                    </div>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default Login

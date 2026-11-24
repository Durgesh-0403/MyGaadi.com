import React, { useContext, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { UserContext } from '../App'
import { useSelector } from 'react-redux'
import { clearSessionAuth } from '../services/authservice'
import { toast } from 'react-toastify'

function Navbar({ onOpenAi }) {
    const navigate = useNavigate()
    const { loginStatus, setLoginStatus, user, setUser } = useContext(UserContext)
    const wishlistCount = useSelector(state => state.wishlist.count)
    const [searchTerm, setSearchTerm] = useState('')
    const [dropdownOpen, setDropdownOpen] = useState(false)
    const dropdownRef = useRef(null)

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setDropdownOpen(false)
            }
        }
        document.addEventListener('mousedown', handleClickOutside)
        return () => {
            document.removeEventListener('mousedown', handleClickOutside)
        }
    }, [])

    const handleSearchSubmit = (e) => {
        e.preventDefault()
        if (searchTerm.trim()) {
            navigate(`/cars?q=${encodeURIComponent(searchTerm.trim())}`)
        } else {
            navigate('/cars')
        }
    }

    const handleLogout = () => {
        clearSessionAuth()
        setLoginStatus(false)
        setUser(null)
        setDropdownOpen(false)
        toast.success('Logged out successfully')
        navigate('/login')
    }

    return (
        <nav className="navbar navbar-expand-lg bg-white sticky-top shadow-sm py-2" style={{ zIndex: 1040 }}>
            <div className="container">
                {/* Brand Logo & Name */}
                <Link to="/" className="navbar-brand d-flex align-items-center">
                    <img 
                        src="/logo.png" 
                        alt="MyGaadi.com" 
                        style={{ height: '38px', objectFit: 'contain' }} 
                    />
                </Link>

                {/* Center Search Pill matching screenshot */}
                <form onSubmit={handleSearchSubmit} className="d-none d-md-flex align-items-center mx-auto" style={{ maxWidth: '420px', width: '100%' }}>
                    <div className="input-group" style={{ background: '#F8FAFC', borderRadius: '50px', padding: '3px 12px', border: '1px solid #E2E8F0' }}>
                        <span className="input-group-text bg-transparent border-0 text-muted ps-2">
                            <i className="bi bi-search"></i>
                        </span>
                        <input
                            type="text"
                            className="form-control bg-transparent border-0 shadow-none ps-1"
                            placeholder="Search cars, brands, models..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            style={{ fontSize: '0.92rem' }}
                        />
                    </div>
                </form>

                {/* Right Links & Actions */}
                <div className="d-flex align-items-center gap-2 gap-md-3">
                    <Link to="/cars" className="nav-link fw-semibold text-secondary px-2">
                        Buy Cars
                    </Link>

                    {/* AI Assistant Navigation trigger */}
                    <button 
                        type="button" 
                        onClick={onOpenAi} 
                        className="btn btn-link nav-link fw-semibold text-secondary px-2 d-none d-sm-inline-flex align-items-center gap-1 border-0"
                    >
                        <i className="bi bi-robot text-primary"></i> AI Assistant
                    </button>

                    {/* If logged in: Show Role Dashboard and Account Dropdown */}
                    {loginStatus && user ? (
                        <div className="dropdown position-relative" ref={dropdownRef}>
                            <button 
                                className="btn d-flex align-items-center gap-2 rounded-pill px-3 py-1 shadow-sm border navbar-user-btn"
                                type="button" 
                                onClick={() => setDropdownOpen(prev => !prev)}
                                aria-expanded={dropdownOpen}
                            >
                                <span className="badge rounded-pill text-bg-primary px-2" style={{ fontSize: '0.72rem', fontWeight: '700' }}>
                                    {user.userRole === 'ROLE_ADMIN' ? 'ADMIN' : user.userRole === 'ROLE_SELLER' ? 'SELLER' : 'BUYER'}
                                </span>
                                <span className="text-truncate navbar-user-name" style={{ maxWidth: '140px', fontWeight: '700', fontSize: '0.88rem' }}>
                                    {user.completeName || 'My Account'}
                                </span>
                                <i className="bi bi-chevron-down navbar-chevron" style={{ fontSize: '0.72rem' }}></i>
                            </button>

                            {/* Dropdown Menu with React State visibility */}
                            <ul 
                                className={`dropdown-menu dropdown-menu-end shadow-lg mt-2 p-2 navbar-dropdown-menu ${dropdownOpen ? 'show' : ''}`} 
                                style={{ 
                                    borderRadius: '16px', 
                                    minWidth: '240px', 
                                    position: 'absolute', 
                                    right: 0, 
                                    top: '100%', 
                                    zIndex: 1060,
                                    display: dropdownOpen ? 'block' : 'none'
                                }}
                            >
                                <li className="px-3 py-2 border-bottom mb-1">
                                    <small className="text-muted d-block" style={{ fontSize: '0.75rem' }}>Signed in as</small>
                                    <div className="fw-bold text-truncate" style={{ fontSize: '0.88rem' }}>{user.email}</div>
                                </li>

                                {user.userRole === 'ROLE_BUYER' && (
                                    <>
                                        <li>
                                            <Link 
                                                to="/wishlist" 
                                                onClick={() => setDropdownOpen(false)}
                                                className="dropdown-item py-2 d-flex justify-content-between align-items-center rounded"
                                            >
                                                <span><i className="bi bi-heart me-2 text-danger"></i> Wishlist</span>
                                                <span className="badge bg-danger rounded-pill">{wishlistCount}</span>
                                            </Link>
                                        </li>
                                        <li>
                                            <Link 
                                                to="/my-bookings" 
                                                onClick={() => setDropdownOpen(false)}
                                                className="dropdown-item py-2 rounded"
                                            >
                                                <i className="bi bi-calendar-check me-2 text-primary"></i> My Bookings
                                            </Link>
                                        </li>
                                    </>
                                )}

                                {user.userRole === 'ROLE_SELLER' && (
                                    <>
                                        <li>
                                            <Link 
                                                to="/seller/dashboard" 
                                                onClick={() => setDropdownOpen(false)}
                                                className="dropdown-item py-2 rounded"
                                            >
                                                <i className="bi bi-speedometer2 me-2 text-primary"></i> Seller Hub
                                            </Link>
                                        </li>
                                    </>
                                )}

                                {user.userRole === 'ROLE_ADMIN' && (
                                    <>
                                        <li>
                                            <Link 
                                                to="/admin/dashboard" 
                                                onClick={() => setDropdownOpen(false)}
                                                className="dropdown-item py-2 rounded"
                                            >
                                                <i className="bi bi-shield-check me-2 text-primary"></i> Admin Portal
                                            </Link>
                                        </li>
                                    </>
                                )}

                                <li><hr className="dropdown-divider my-1" /></li>
                                <li>
                                    <button 
                                        type="button"
                                        onClick={handleLogout} 
                                        className="dropdown-item text-danger py-2 rounded d-flex align-items-center fw-semibold"
                                    >
                                        <i className="bi bi-box-arrow-right me-2 fs-6"></i> Sign Out
                                    </button>
                                </li>
                            </ul>
                        </div>
                    ) : (
                        /* Exact blue pill Login button from screenshot */
                        <Link to="/login" className="btn btn-mg-primary px-4 py-2" style={{ fontSize: '0.92rem' }}>
                            Login
                        </Link>
                    )}
                </div>
            </div>
        </nav>
    )
}

export default Navbar
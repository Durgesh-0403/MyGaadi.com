import React, { useContext, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { approveCar, blockUserAdmin, deleteUserAdmin, getAdminBookings, getAdminCars, getAdminUsers, getDashboardStats, rejectCar, unblockUserAdmin, updateSellerKycStatus } from '../services/adminservice'
import { formatPrice, getImageUrl } from '../services/carservice'
import { clearSessionAuth, getStoredToken } from '../services/authservice'
import { UserContext } from '../App'
import { toast } from 'react-toastify'
import ReceiptModal from '../component/ReceiptModal'

function AdminDashboard() {
    const navigate = useNavigate()
    const { loginStatus, setLoginStatus, user, setUser } = useContext(UserContext)

    const [activeTab, setActiveTab] = useState('overview')
    const [stats, setStats] = useState(null)
    const [pendingCars, setPendingCars] = useState([])
    const [usersList, setUsersList] = useState([])
    const [bookings, setBookings] = useState([])
    const [loading, setLoading] = useState(false)
    const [selectedReceipt, setSelectedReceipt] = useState(null)

    useEffect(() => {
        if (!loginStatus || !user) {
            navigate('/login')
            return
        }
        if (user.userRole !== 'ROLE_ADMIN') {
            toast.warning('Access denied: Administrator credentials required')
            navigate('/cars')
            return
        }

        loadAdminData()
    }, [loginStatus, user])

    const loadAdminData = async () => {
        setLoading(true)
        const token = getStoredToken()
        try {
            const [statsRes, carsRes, usersRes, bookingsRes] = await Promise.allSettled([
                getDashboardStats(token),
                getAdminCars(token, null, false, null, 0, 50),
                getAdminUsers(token, null, null, 0, 50),
                getAdminBookings(token, 0, 50)
            ])

            if (statsRes.status === 'fulfilled') {
                setStats(statsRes.value.data)
            }
            if (carsRes.status === 'fulfilled') {
                const data = carsRes.value.data
                setPendingCars(data.content || data || [])
            }
            if (usersRes.status === 'fulfilled') {
                const data = usersRes.value.data
                setUsersList(data.content || data || [])
            }
            if (bookingsRes.status === 'fulfilled') {
                const data = bookingsRes.value.data
                setBookings(data.content || data || [])
            }
        } catch (error) {
            console.error('Error loading admin portal data:', error)
        } finally {
            setLoading(false)
        }
    }

    const handleApproveCar = async (carId) => {
        const token = getStoredToken()
        try {
            await approveCar(carId, token)
            toast.success(`Car #${carId} approved and published to marketplace!`)
            loadAdminData()
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to approve car')
        }
    }

    const handleRejectCar = async (carId) => {
        const reason = window.prompt('Enter rejection reason for seller:', 'Quality standards or missing documentation')
        if (!reason) return
        const token = getStoredToken()
        try {
            await rejectCar(carId, reason, token)
            toast.info(`Car #${carId} rejected. Seller notified.`)
            loadAdminData()
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to reject car')
        }
    }

    const handleVerifyKyc = async (sellerId, approve) => {
        const token = getStoredToken()
        const status = approve ? 'VERIFIED' : 'REJECTED'
        let reason = null
        if (!approve) {
            reason = window.prompt('Enter KYC rejection reason:', 'Invalid or blurry identity document')
            if (!reason) return
        }

        try {
            await updateSellerKycStatus(sellerId, status, reason, token)
            toast.success(`Seller #${sellerId} KYC status updated to ${status}`)
            loadAdminData()
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to update seller KYC')
        }
    }

    const handleBlockUser = async (userId, userEmail) => {
        if (!window.confirm(`Are you sure you want to block user ${userEmail} (#${userId})? They will be unable to log in, book, or list vehicles.`)) {
            return
        }
        try {
            const token = getStoredToken()
            await blockUserAdmin(userId, token)
            toast.warning(`User #${userId} blocked successfully`)
            loadAdminData()
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to block user')
        }
    }

    const handleUnblockUser = async (userId, userEmail) => {
        try {
            const token = getStoredToken()
            await unblockUserAdmin(userId, token)
            toast.success(`User #${userId} unblocked successfully`)
            loadAdminData()
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to unblock user')
        }
    }

    const handleDeleteUser = async (userId, userEmail) => {
        if (!window.confirm(`Are you sure you want to permanently delete user ${userEmail} (#${userId})?\n\nThis will remove their profile and all associated listings/bookings.`)) {
            return
        }
        try {
            const token = getStoredToken()
            await deleteUserAdmin(userId, token)
            toast.success(`User #${userId} deleted successfully`)
            loadAdminData()
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to delete user')
        }
    }

    // Filtered views
    const pendingApprovalCars = pendingCars.filter(c => c.approvalStatus === 'PENDING')
    const sellersWithKyc = usersList.filter(u => u.userRole === 'ROLE_SELLER' && u.sellerProfile)

    return (
        <div className="container-fluid px-lg-5 py-4">
            <div className="row g-4">
                {/* ================= SINGLE CLEAN LEFT SIDEBAR (Admin Left Navigation) ================= */}
                <div className="col-lg-3 col-xl-2">
                    <div className="card border-0 shadow-sm rounded-4 overflow-hidden bg-white sticky-top" style={{ top: '85px' }}>
                        {/* Admin Header with Crisp High-Contrast White Text */}
                        <div className="p-3 text-white" style={{ background: 'linear-gradient(135deg, #0A192F 0%, #153B6B 100%)' }}>
                            <div className="d-flex align-items-center gap-2 mb-2">
                                <span className="p-2 rounded-circle bg-danger bg-opacity-25 text-white">
                                    <i className="bi bi-shield-lock fs-5"></i>
                                </span>
                                <div className="text-truncate">
                                    <h6 className="fw-bold mb-0 text-truncate text-white" style={{ color: '#FFFFFF', textShadow: '0 1px 2px rgba(0,0,0,0.4)' }}>
                                        {user?.completeName || 'Administrator'}
                                    </h6>
                                    <small className="text-white opacity-75 d-block" style={{ fontSize: '0.75rem' }}>Platform Control Center</small>
                                </div>
                            </div>
                            <span className="badge bg-danger rounded-pill px-2 py-1 small">Super Admin</span>
                        </div>

                        {/* Left Navigation Buttons */}
                        <div className="p-2 d-flex flex-column gap-1">
                            <button
                                type="button"
                                className={`btn text-start py-2 px-3 rounded-3 fw-semibold border-0 ${activeTab === 'overview' ? 'btn-primary' : 'btn-light text-secondary'}`}
                                onClick={() => setActiveTab('overview')}
                            >
                                <i className="bi bi-speedometer2 me-2"></i> Dashboard
                            </button>

                            <button
                                type="button"
                                className={`btn text-start py-2 px-3 rounded-3 fw-semibold border-0 ${activeTab === 'cars' ? 'btn-primary' : 'btn-light text-secondary'}`}
                                onClick={() => setActiveTab('cars')}
                            >
                                <i className="bi bi-car-front me-2"></i> Moderate Cars ({pendingApprovalCars.length})
                            </button>

                            <button
                                type="button"
                                className={`btn text-start py-2 px-3 rounded-3 fw-semibold border-0 ${activeTab === 'kyc' ? 'btn-primary' : 'btn-light text-secondary'}`}
                                onClick={() => setActiveTab('kyc')}
                            >
                                <i className="bi bi-person-check me-2"></i> Seller KYC ({sellersWithKyc.length})
                            </button>

                            <button
                                type="button"
                                className={`btn text-start py-2 px-3 rounded-3 fw-semibold border-0 ${activeTab === 'users' ? 'btn-primary' : 'btn-light text-secondary'}`}
                                onClick={() => setActiveTab('users')}
                            >
                                <i className="bi bi-people me-2"></i> All Users ({usersList.length})
                            </button>

                            <button
                                type="button"
                                className={`btn text-start py-2 px-3 rounded-3 fw-semibold border-0 ${activeTab === 'bookings' ? 'btn-primary' : 'btn-light text-secondary'}`}
                                onClick={() => setActiveTab('bookings')}
                            >
                                <i className="bi bi-calendar-check me-2"></i> All Bookings ({bookings.length})
                            </button>

                            {/* Admin Profile & Sign Out Card */}
                            <div className="p-3 border-top mt-3 bg-light rounded-3">
                                <div className="d-flex align-items-center gap-2 mb-2">
                                    <div className="bg-danger text-white d-flex align-items-center justify-content-center rounded-circle fw-bold" style={{ width: '32px', height: '32px', fontSize: '0.82rem', flexShrink: 0 }}>
                                        {user?.completeName ? user.completeName.charAt(0).toUpperCase() : 'A'}
                                    </div>
                                    <div className="overflow-hidden">
                                        <div className="fw-bold small text-truncate" style={{ maxWidth: '140px' }}>{user?.completeName || 'Admin'}</div>
                                        <div className="text-muted text-truncate" style={{ fontSize: '0.72rem', maxWidth: '140px' }}>{user?.email}</div>
                                    </div>
                                </div>
                                <button 
                                    type="button" 
                                    onClick={() => {
                                        clearSessionAuth()
                                        setLoginStatus(false)
                                        setUser(null)
                                        toast.success('Logged out successfully')
                                        navigate('/login')
                                    }} 
                                    className="btn btn-outline-danger btn-sm w-100 rounded-pill py-1 fw-semibold d-flex align-items-center justify-content-center gap-1"
                                    style={{ fontSize: '0.8rem' }}
                                >
                                    <i className="bi bi-box-arrow-right"></i> Sign Out
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                {/* ================= RIGHT MAIN ADMIN VIEW ================= */}
                <div className="col-lg-9 col-xl-10">
                    {/* TAB 1: OVERVIEW */}
                    {activeTab === 'overview' && (
                        <div>
                            {/* KPI Metrics */}
                            <div className="row g-3 mb-4">
                                <div className="col-sm-6 col-xl-3">
                                    <div className="card border-0 shadow-sm rounded-4 p-3 bg-white border-start border-primary border-4">
                                        <small className="text-muted fw-semibold text-uppercase">Platform Users</small>
                                        <h3 className="fw-bold text-primary mt-1 mb-0">{stats?.totalUsers || usersList.length}</h3>
                                        <small className="text-muted">Registered accounts</small>
                                    </div>
                                </div>
                                <div className="col-sm-6 col-xl-3">
                                    <div className="card border-0 shadow-sm rounded-4 p-3 bg-white border-start border-warning border-4">
                                        <small className="text-muted fw-semibold text-uppercase">Pending Cars</small>
                                        <h3 className="fw-bold text-warning mt-1 mb-0">{stats?.pendingCars || pendingApprovalCars.length}</h3>
                                        <small className="text-muted">Requires approval</small>
                                    </div>
                                </div>
                                <div className="col-sm-6 col-xl-3">
                                    <div className="card border-0 shadow-sm rounded-4 p-3 bg-white border-start border-success border-4">
                                        <small className="text-muted fw-semibold text-uppercase">Active Listings</small>
                                        <h3 className="fw-bold text-success mt-1 mb-0">{stats?.approvedCars || pendingCars.filter(c => c.approvalStatus === 'APPROVED').length}</h3>
                                        <small className="text-muted">Visible to buyers</small>
                                    </div>
                                </div>
                                <div className="col-sm-6 col-xl-3">
                                    <div className="card border-0 shadow-sm rounded-4 p-3 bg-white border-start border-danger border-4">
                                        <small className="text-muted fw-semibold text-uppercase">Platform Bookings</small>
                                        <h3 className="fw-bold text-danger mt-1 mb-0">{stats?.totalBookings || bookings.length}</h3>
                                        <small className="text-muted">Total token deposits</small>
                                    </div>
                                </div>
                            </div>

                            {/* Pending Moderation Queue Preview */}
                            <div className="card border-0 shadow-sm rounded-4 p-4 bg-white mb-4">
                                <div className="d-flex justify-content-between align-items-center mb-3">
                                    <h5 className="fw-bold mb-0" style={{ color: '#0B2545' }}>
                                        <i className="bi bi-clock-history me-2 text-warning"></i> Pending Cars Moderation Queue
                                    </h5>
                                    <button onClick={() => setActiveTab('cars')} className="btn btn-link btn-sm text-primary text-decoration-none">
                                        Manage All &rarr;
                                    </button>
                                </div>

                                {pendingApprovalCars.length > 0 ? (
                                    <div className="table-responsive">
                                        <table className="table align-middle">
                                            <thead className="table-light">
                                                <tr>
                                                    <th>Vehicle</th>
                                                    <th>Showroom</th>
                                                    <th>Price</th>
                                                    <th>Actions</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {pendingApprovalCars.slice(0, 5).map(c => (
                                                    <tr key={c.id}>
                                                        <td>
                                                            <div className="d-flex align-items-center gap-2">
                                                                <img 
                                                                    src={getImageUrl(c.primaryImageUrl)} 
                                                                    alt={c.title} 
                                                                    className="rounded-2 object-fit-cover"
                                                                    style={{ width: '45px', height: '35px' }}
                                                                />
                                                                <div>
                                                                    <strong>{c.brand} {c.model}</strong>
                                                                    <small className="text-muted d-block">{c.manufactureYear} • {c.variant}</small>
                                                                </div>
                                                            </div>
                                                        </td>
                                                        <td>{c.sellerShowroom || c.sellerName}</td>
                                                        <td className="fw-bold">{formatPrice(c.price)}</td>
                                                        <td>
                                                            {c.status === 'SOLD' ? (
                                                                <span className="badge bg-light text-muted border px-2 py-1">
                                                                    Sold (No Actions)
                                                                </span>
                                                            ) : (
                                                                <div className="d-flex gap-2">
                                                                    <button onClick={() => handleApproveCar(c.id)} className="btn btn-sm btn-success rounded-pill px-3">
                                                                        Approve
                                                                    </button>
                                                                    <button onClick={() => handleRejectCar(c.id)} className="btn btn-sm btn-outline-danger rounded-pill px-3">
                                                                        Reject
                                                                    </button>
                                                                </div>
                                                            )}
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                ) : (
                                    <div className="text-center py-4 text-muted small">
                                        <i className="bi bi-check-circle text-success fs-3 d-block mb-2"></i>
                                        All car submissions have been moderated! Zero pending cars.
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    {/* TAB 2: MODERATE CARS */}
                    {activeTab === 'cars' && (
                        <div className="card border-0 shadow-sm rounded-4 p-4 bg-white">
                            <h4 className="fw-bold mb-1" style={{ color: '#0B2545' }}>Marketplace Car Moderation</h4>
                            <p className="text-muted small mb-4">Review car specs, pictures, and pricing before publishing to marketplace</p>

                            {pendingCars.length > 0 ? (
                                <div className="table-responsive">
                                    <table className="table table-hover align-middle">
                                        <thead className="table-light">
                                            <tr>
                                                <th>Vehicle</th>
                                                <th>Seller Showroom</th>
                                                <th>Specs</th>
                                                <th>Price</th>
                                                <th>Status</th>
                                                <th>Moderation Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {pendingCars.map(c => (
                                                <tr key={c.id}>
                                                    <td>
                                                        <div className="d-flex align-items-center gap-3">
                                                            <img 
                                                                src={getImageUrl(c.primaryImageUrl)} 
                                                                alt={c.title} 
                                                                className="rounded-3 object-fit-cover"
                                                                style={{ width: '60px', height: '45px' }}
                                                            />
                                                            <div>
                                                                <strong className="d-block">{c.brand} {c.model}</strong>
                                                                <small className="text-muted">{c.manufactureYear} • {c.variant}</small>
                                                            </div>
                                                        </div>
                                                    </td>
                                                    <td>
                                                        <div>{c.sellerShowroom || c.sellerName}</div>
                                                        <small className="text-muted">{c.city}, {c.state}</small>
                                                    </td>
                                                    <td>
                                                        <small className="d-block">{c.kmDriven ? c.kmDriven.toLocaleString('en-IN') : '--'} km</small>
                                                        <small className="text-muted">{c.fuelType} • {c.transmission}</small>
                                                    </td>
                                                    <td className="fw-bold text-dark">{formatPrice(c.price)}</td>
                                                    <td>
                                                        <div className="d-flex flex-column gap-1">
                                                            <span className={`badge ${c.approvalStatus === 'APPROVED' ? 'text-bg-success' : c.approvalStatus === 'REJECTED' ? 'text-bg-danger' : 'text-bg-warning'}`}>
                                                                {c.approvalStatus}
                                                            </span>
                                                            {c.status === 'SOLD' && (
                                                                <span className="badge bg-danger-subtle text-danger border border-danger-subtle" style={{ fontSize: '0.7rem' }}>
                                                                    SOLD
                                                                </span>
                                                            )}
                                                        </div>
                                                    </td>
                                                    <td>
                                                        {c.status === 'SOLD' ? (
                                                            <div className="d-flex align-items-center gap-2">
                                                                <span className="badge bg-light text-muted border px-2 py-1">
                                                                    Sold (No Actions)
                                                                </span>
                                                                <Link to={`/cars/${c.id}`} className="btn btn-sm btn-light border py-0 px-2" style={{ fontSize: '0.78rem' }}>
                                                                    View
                                                                </Link>
                                                            </div>
                                                        ) : (
                                                            <div className="d-flex gap-2">
                                                                {c.approvalStatus !== 'APPROVED' && (
                                                                    <button onClick={() => handleApproveCar(c.id)} className="btn btn-sm btn-success">
                                                                        Approve
                                                                    </button>
                                                                )}
                                                                {c.approvalStatus !== 'REJECTED' && (
                                                                    <button onClick={() => handleRejectCar(c.id)} className="btn btn-sm btn-outline-danger">
                                                                        Reject
                                                                    </button>
                                                                )}
                                                                <Link to={`/cars/${c.id}`} className="btn btn-sm btn-light border">
                                                                    View
                                                                </Link>
                                                            </div>
                                                        )}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            ) : (
                                <div className="text-center py-5 text-muted">
                                    No cars in database.
                                </div>
                            )}
                        </div>
                    )}

                    {/* TAB 3: SELLER KYC MODERATION */}
                    {activeTab === 'kyc' && (
                        <div className="card border-0 shadow-sm rounded-4 p-4 bg-white">
                            <h4 className="fw-bold mb-1" style={{ color: '#0B2545' }}>Seller KYC Moderation</h4>
                            <p className="text-muted small mb-4">Verify dealership government identity documents and grant certified seller status</p>

                            {sellersWithKyc.length > 0 ? (
                                <div className="table-responsive">
                                    <table className="table table-hover align-middle">
                                        <thead className="table-light">
                                            <tr>
                                                <th>Showroom & Seller</th>
                                                <th>Contact Details</th>
                                                <th>Document Type & Number</th>
                                                <th>Uploaded Document</th>
                                                <th>KYC Status</th>
                                                <th>Verification Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {sellersWithKyc.map(s => {
                                                const prof = s.sellerProfile || {}
                                                return (
                                                    <tr key={s.id}>
                                                        <td>
                                                            <strong className="d-block">{prof.showroomName || 'Dealership'}</strong>
                                                            <small className="text-muted">{s.completeName}</small>
                                                        </td>
                                                        <td>
                                                            <div>{s.email}</div>
                                                            <small className="text-muted">{s.phone} • {prof.city}</small>
                                                        </td>
                                                        <td>
                                                            {prof.kycDocumentType ? (
                                                                <>
                                                                    <span className="badge bg-light text-dark border me-1">{prof.kycDocumentType}</span>
                                                                    <strong className="small">{prof.kycDocumentNumber}</strong>
                                                                </>
                                                            ) : (
                                                                <span className="text-muted small">Not submitted</span>
                                                            )}
                                                        </td>
                                                        <td>
                                                            {prof.kycDocumentUrl ? (
                                                                <a 
                                                                    href={getImageUrl(prof.kycDocumentUrl)} 
                                                                    target="_blank" 
                                                                    rel="noreferrer" 
                                                                    className="btn btn-sm btn-outline-primary"
                                                                >
                                                                    <i className="bi bi-file-earmark-arrow-down me-1"></i> View Doc
                                                                </a>
                                                            ) : (
                                                                <span className="text-muted small">No file</span>
                                                            )}
                                                        </td>
                                                        <td>
                                                            <span className={`badge ${prof.kycStatus === 'VERIFIED' ? 'text-bg-success' : prof.kycStatus === 'REJECTED' ? 'text-bg-danger' : 'text-bg-warning'}`}>
                                                                {prof.kycStatus || 'PENDING'}
                                                            </span>
                                                        </td>
                                                        <td>
                                                            <div className="d-flex gap-2">
                                                                <button 
                                                                    onClick={() => handleVerifyKyc(s.id, true)} 
                                                                    className="btn btn-sm btn-success rounded-pill px-3"
                                                                    disabled={prof.kycStatus === 'VERIFIED'}
                                                                >
                                                                    Approve
                                                                </button>
                                                                <button 
                                                                    onClick={() => handleVerifyKyc(s.id, false)} 
                                                                    className="btn btn-sm btn-outline-danger rounded-pill px-3"
                                                                    disabled={prof.kycStatus === 'REJECTED'}
                                                                >
                                                                    Reject
                                                                </button>
                                                            </div>
                                                        </td>
                                                    </tr>
                                                )
                                            })}
                                        </tbody>
                                    </table>
                                </div>
                            ) : (
                                <div className="text-center py-5 text-muted small">
                                    No seller showroom registrations found.
                                </div>
                            )}
                        </div>
                    )}

                    {/* TAB 4: ALL REGISTERED USERS */}
                    {activeTab === 'users' && (
                        <div className="card border-0 shadow-sm rounded-4 p-4 bg-white">
                            <h4 className="fw-bold mb-1" style={{ color: '#0B2545' }}>Platform Users Directory</h4>
                            <p className="text-muted small mb-4">Overview of all registered car buyers, sellers, and platform administrators</p>

                            {usersList.length > 0 ? (
                                <div className="table-responsive">
                                    <table className="table table-hover align-middle">
                                        <thead className="table-light">
                                            <tr>
                                                <th>User ID</th>
                                                <th>Name</th>
                                                <th>Email</th>
                                                <th>Phone</th>
                                                <th>Role</th>
                                                <th>Status</th>
                                                <th className="text-end">Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {usersList.map(u => (
                                                <tr key={u.id}>
                                                    <td>#{u.id}</td>
                                                    <td className="fw-bold">{u.completeName}</td>
                                                    <td>{u.email}</td>
                                                    <td>{u.phone || '--'}</td>
                                                    <td>
                                                        <span className={`badge ${u.userRole === 'ROLE_ADMIN' ? 'text-bg-danger' : u.userRole === 'ROLE_SELLER' ? 'text-bg-primary' : 'text-bg-secondary'}`}>
                                                            {u.userRole}
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <span className={`badge ${u.status === 'ACTIVE' || u.active ? 'text-bg-success' : 'text-bg-danger'}`}>
                                                            {u.status || (u.active ? 'ACTIVE' : 'BLOCKED')}
                                                        </span>
                                                    </td>
                                                    <td className="text-end">
                                                        {u.userRole === 'ROLE_ADMIN' ? (
                                                            <span className="badge bg-light text-muted border px-2 py-1">System Admin</span>
                                                        ) : (
                                                            <div className="d-inline-flex align-items-center gap-1">
                                                                {(u.status === 'ACTIVE' || (u.active && u.status !== 'BLOCKED')) ? (
                                                                    <button 
                                                                        type="button"
                                                                        onClick={() => handleBlockUser(u.id, u.email)} 
                                                                        className="btn btn-sm btn-outline-warning rounded-pill px-2 py-1"
                                                                        style={{ fontSize: '0.75rem' }}
                                                                        title="Block User Account"
                                                                    >
                                                                        <i className="bi bi-slash-circle me-1"></i> Block
                                                                    </button>
                                                                ) : (
                                                                    <button 
                                                                        type="button"
                                                                        onClick={() => handleUnblockUser(u.id, u.email)} 
                                                                        className="btn btn-sm btn-outline-success rounded-pill px-2 py-1"
                                                                        style={{ fontSize: '0.75rem' }}
                                                                        title="Unblock User Account"
                                                                    >
                                                                        <i className="bi bi-check-circle me-1"></i> Unblock
                                                                    </button>
                                                                )}
                                                                <button 
                                                                    type="button"
                                                                    onClick={() => handleDeleteUser(u.id, u.email)} 
                                                                    className="btn btn-sm btn-outline-danger rounded-pill px-2 py-1"
                                                                    style={{ fontSize: '0.75rem' }}
                                                                    title="Permanently Delete User"
                                                                >
                                                                    <i className="bi bi-trash3 me-1"></i> Delete
                                                                </button>
                                                            </div>
                                                        )}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            ) : (
                                <div className="text-center py-5 text-muted">
                                    No users found.
                                </div>
                            )}
                        </div>
                    )}

                    {/* TAB 5: ALL BOOKINGS */}
                    {activeTab === 'bookings' && (
                        <div className="card border-0 shadow-sm rounded-4 p-4 bg-white">
                            <h4 className="fw-bold mb-1" style={{ color: '#0B2545' }}>All Platform Token Bookings</h4>
                            <p className="text-muted small mb-4">Complete audit trail of buyer token deposits, Razorpay payments, and deals</p>

                            {bookings.length > 0 ? (
                                <div className="table-responsive">
                                    <table className="table table-hover align-middle">
                                        <thead className="table-light">
                                            <tr>
                                                <th>Booking #</th>
                                                <th>Car ID & Title</th>
                                                <th>Buyer</th>
                                                <th>Seller Showroom</th>
                                                <th>Token Amount</th>
                                                <th>Status</th>
                                                <th>Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {bookings.map(b => (
                                                <tr key={b.id}>
                                                    <td><strong>#{b.id}</strong></td>
                                                    <td>
                                                        <strong>{b.carTitle || `Car #${b.carId}`}</strong>
                                                        <small className="text-muted d-block">ID: #{b.carId}</small>
                                                    </td>
                                                    <td>
                                                        <div>{b.buyerName}</div>
                                                        <small className="text-muted">{b.buyerPhone}</small>
                                                    </td>
                                                    <td>{b.sellerShowroom || 'Certified Hub'}</td>
                                                    <td className="fw-bold text-primary">₹ {b.tokenAmount ? Number(b.tokenAmount).toLocaleString('en-IN') : '10,000'}</td>
                                                    <td>
                                                        <span className={`badge ${b.status === 'CONFIRMED' ? 'text-bg-success' : b.status === 'COMPLETED' ? 'text-bg-primary' : 'text-bg-warning'}`}>
                                                            {b.status}
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <button 
                                                            type="button" 
                                                            onClick={() => setSelectedReceipt(b)} 
                                                            className="btn btn-sm btn-outline-primary rounded-pill px-3 py-1 d-flex align-items-center gap-1"
                                                            style={{ fontSize: '0.78rem' }}
                                                        >
                                                            <i className="bi bi-receipt"></i> View Receipt
                                                        </button>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            ) : (
                                <div className="text-center py-5 text-muted small">
                                    No bookings recorded yet.
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>

            {/* Admin Receipt Modal */}
            <ReceiptModal 
                booking={selectedReceipt} 
                onClose={() => setSelectedReceipt(null)} 
            />
        </div>
    )
}

export default AdminDashboard

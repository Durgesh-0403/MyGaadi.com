import React, { useContext, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { addCar, completeBooking, deleteCar, getSellerBookings, getSellerCars, getSellerKyc, submitSellerKyc } from '../services/sellerservice'
import { formatPrice, getImageUrl } from '../services/carservice'
import { clearSessionAuth, getStoredToken } from '../services/authservice'
import { UserContext } from '../App'
import { toast } from 'react-toastify'
import ReceiptModal from '../component/ReceiptModal'

function SellerDashboard() {
    const navigate = useNavigate()
    const { loginStatus, setLoginStatus, user, setUser } = useContext(UserContext)

    const [activeTab, setActiveTab] = useState('overview')
    const [cars, setCars] = useState([])
    const [bookings, setBookings] = useState([])
    const [kycProfile, setKycProfile] = useState(null)
    const [loading, setLoading] = useState(false)
    const [selectedReceipt, setSelectedReceipt] = useState(null)

    // Add Car Form State
    const [carForm, setCarForm] = useState({
        title: '',
        brand: 'Hyundai',
        model: '',
        variant: '',
        manufactureYear: 2021,
        price: '',
        kmDriven: '',
        fuelType: 'PETROL',
        transmission: 'MANUAL',
        bodyType: 'SEDAN',
        ownerCount: 1,
        color: 'White',
        city: '',
        state: '',
        description: ''
    })
    // 3 Dedicated Photo Upload States
    const [primaryImage, setPrimaryImage] = useState(null)
    const [primaryPreview, setPrimaryPreview] = useState(null)
    const [exteriorImages, setExteriorImages] = useState([])
    const [exteriorPreviews, setExteriorPreviews] = useState([])
    const [interiorImages, setInteriorImages] = useState([])
    const [interiorPreviews, setInteriorPreviews] = useState([])
    const [formSubmitting, setFormSubmitting] = useState(false)

    // KYC Form State
    const [kycDocType, setKycDocType] = useState('PAN')
    const [kycDocNumber, setKycDocNumber] = useState('')
    const [kycFile, setKycFile] = useState(null)
    const [kycSubmitting, setKycSubmitting] = useState(false)

    useEffect(() => {
        if (!loginStatus || !user) {
            navigate('/login')
            return
        }
        if (user.userRole !== 'ROLE_SELLER') {
            toast.warning('Access denied: Seller Hub is for registered dealerships')
            navigate('/cars')
            return
        }

        loadSellerData()
    }, [loginStatus, user])

    const loadSellerData = async () => {
        setLoading(true)
        const token = getStoredToken()
        try {
            const [carsRes, bookingsRes, kycRes] = await Promise.allSettled([
                getSellerCars(token, 0, 50),
                getSellerBookings(token, 0, 50),
                getSellerKyc(token)
            ])

            if (carsRes.status === 'fulfilled') {
                const data = carsRes.value.data
                setCars(data.content || data || [])
            }
            if (bookingsRes.status === 'fulfilled') {
                const data = bookingsRes.value.data
                setBookings(data.content || data || [])
            }
            if (kycRes.status === 'fulfilled') {
                setKycProfile(kycRes.value.data)
            }
        } catch (error) {
            console.error('Error loading seller hub data:', error)
        } finally {
            setLoading(false)
        }
    }

    const handleCarFormChange = (field, value) => {
        setCarForm(prev => ({
            ...prev,
            [field]: value
        }))
    }

    const handlePrimaryChange = (e) => {
        const file = e.target.files[0]
        if (file) {
            setPrimaryImage(file)
            setPrimaryPreview(URL.createObjectURL(file))
        }
    }

    const handleExteriorChange = (e) => {
        const files = Array.from(e.target.files)
        const combined = [...exteriorImages, ...files].slice(0, 8)
        setExteriorImages(combined)
        setExteriorPreviews(combined.map(f => URL.createObjectURL(f)))
    }

    const removeExteriorImage = (index) => {
        const nextImages = exteriorImages.filter((_, i) => i !== index)
        setExteriorImages(nextImages)
        setExteriorPreviews(nextImages.map(f => URL.createObjectURL(f)))
    }

    const handleInteriorChange = (e) => {
        const files = Array.from(e.target.files)
        const combined = [...interiorImages, ...files].slice(0, 8)
        setInteriorImages(combined)
        setInteriorPreviews(combined.map(f => URL.createObjectURL(f)))
    }

    const removeInteriorImage = (index) => {
        const nextImages = interiorImages.filter((_, i) => i !== index)
        setInteriorImages(nextImages)
        setInteriorPreviews(nextImages.map(f => URL.createObjectURL(f)))
    }

    const handleAddCarSubmit = async (e) => {
        e.preventDefault()

        if (!carForm.title || !carForm.brand || !carForm.model || !carForm.price || !carForm.kmDriven) {
            toast.error('Please fill in all required car fields')
            return
        }

        if (!primaryImage && exteriorImages.length === 0 && interiorImages.length === 0) {
            toast.error('Please upload at least the 1st Hero / Original car photo')
            return
        }

        setFormSubmitting(true)
        const token = getStoredToken()

        const carPayload = {
            ...carForm,
            manufactureYear: Number(carForm.manufactureYear),
            price: Number(carForm.price),
            kmDriven: Number(carForm.kmDriven),
            ownerCount: Number(carForm.ownerCount),
            city: carForm.city || user.sellerProfile?.city || 'Pune',
            state: carForm.state || user.sellerProfile?.state || 'Maharashtra'
        }

        try {
            await addCar(carPayload, {
                primaryImage,
                exteriorImages,
                interiorImages
            }, token)
            toast.success('Car submitted successfully! Listing is now pending admin moderation.')
            // Reset form
            setCarForm({
                title: '',
                brand: 'Hyundai',
                model: '',
                variant: '',
                manufactureYear: 2021,
                price: '',
                kmDriven: '',
                fuelType: 'PETROL',
                transmission: 'MANUAL',
                bodyType: 'SEDAN',
                ownerCount: 1,
                color: 'White',
                city: '',
                state: '',
                description: ''
            })
            setPrimaryImage(null)
            setPrimaryPreview(null)
            setExteriorImages([])
            setExteriorPreviews([])
            setInteriorImages([])
            setInteriorPreviews([])
            loadSellerData()
            setActiveTab('listings')
        } catch (error) {
            console.error('Add car error:', error)
            toast.error(error.response?.data?.message || 'Failed to add car')
        } finally {
            setFormSubmitting(false)
        }
    }

    const handleDeleteCar = async (id) => {
        if (!window.confirm('Are you sure you want to delete this listing?')) return
        const token = getStoredToken()
        try {
            await deleteCar(id, token)
            toast.success('Car listing deleted')
            loadSellerData()
        } catch (error) {
            toast.error('Failed to delete car')
        }
    }

    const handleKycSubmit = async (e) => {
        e.preventDefault()
        if (!kycDocNumber.trim() || !kycFile) {
            toast.error('Please enter document number and choose document file')
            return
        }

        setKycSubmitting(true)
        const token = getStoredToken()
        try {
            await submitSellerKyc(kycDocType, kycDocNumber.trim(), kycFile, token)
            toast.success('KYC submitted! Our admin team will review it shortly.')
            setKycFile(null)
            loadSellerData()
        } catch (error) {
            console.error('KYC submission error:', error)
            toast.error(error.response?.data?.message || 'Failed to submit KYC')
        } finally {
            setKycSubmitting(false)
        }
    }

    const handleCompleteBooking = async (bookingId) => {
        if (!window.confirm(`Mark booking #${bookingId} as complete? The car will be marked SOLD.`)) return
        const token = getStoredToken()
        try {
            await completeBooking(bookingId, token)
            toast.success('Booking completed! Car status updated to SOLD.')
            loadSellerData()
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to update booking')
        }
    }

    const handleLogout = () => {
        clearSessionAuth()
        setLoginStatus(false)
        setUser(null)
        toast.success('Logged out successfully')
        navigate('/login')
    }

    // Computed Stats
    const activeListingsCount = cars.filter(c => c.approvalStatus === 'APPROVED' && c.status === 'AVAILABLE').length
    const underReviewCount = cars.filter(c => c.approvalStatus === 'PENDING').length
    const activeBookingsCount = bookings.filter(b => b.status === 'CONFIRMED' || b.status === 'PENDING').length
    const inventoryValue = cars.reduce((acc, c) => acc + (Number(c.price) || 0), 0)

    const isKycVerified = kycProfile?.kycStatus === 'VERIFIED'

    return (
        <div className="container-fluid px-lg-5 py-4">
            <div className="row g-4">
                {/* ================= SINGLE CLEAN LEFT SIDEBAR ================= */}
                <div className="col-lg-3 col-xl-2">
                    <div className="card border-0 shadow-sm rounded-4 overflow-hidden bg-white sticky-top" style={{ top: '85px' }}>
                        {/* Showroom Header with Crisp High-Contrast White Text */}
                        <div className="p-3 text-white" style={{ background: 'linear-gradient(135deg, #0A192F 0%, #153B6B 100%)' }}>
                            <div className="d-flex align-items-center gap-2 mb-2">
                                <span className="p-2 rounded-circle bg-primary bg-opacity-25 text-white">
                                    <i className="bi bi-shop fs-5"></i>
                                </span>
                                <div className="text-truncate">
                                    <h6 className="fw-bold mb-0 text-truncate text-white" style={{ color: '#FFFFFF', textShadow: '0 1px 2px rgba(0,0,0,0.4)' }}>
                                        {user?.sellerProfile?.showroomName || user?.completeName || 'Mahajan Dealership Hub'}
                                    </h6>
                                    <small className="text-white opacity-75 d-block" style={{ fontSize: '0.75rem' }}>Verified Dealership Hub</small>
                                </div>
                            </div>
                            <span className={`badge rounded-pill ${isKycVerified ? 'text-bg-success' : 'text-bg-warning'} small`}>
                                <i className={`bi ${isKycVerified ? 'bi-patch-check-fill' : 'bi-clock-history'} me-1`}></i>
                                {kycProfile?.kycStatus || 'KYC Pending'}
                            </span>
                        </div>

                        {/* Navigation Menu */}
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
                                className={`btn text-start py-2 px-3 rounded-3 fw-semibold border-0 ${activeTab === 'listings' ? 'btn-primary' : 'btn-light text-secondary'}`}
                                onClick={() => setActiveTab('listings')}
                            >
                                <i className="bi bi-car-front me-2"></i> My Listings ({cars.length})
                            </button>

                            <button
                                type="button"
                                className={`btn text-start py-2 px-3 rounded-3 fw-semibold border-0 ${activeTab === 'add-car' ? 'btn-primary' : 'btn-light text-secondary'}`}
                                onClick={() => setActiveTab('add-car')}
                            >
                                <i className="bi bi-plus-circle me-2"></i> Add New Car
                            </button>

                            <button
                                type="button"
                                className={`btn text-start py-2 px-3 rounded-3 fw-semibold border-0 ${activeTab === 'kyc' ? 'btn-primary' : 'btn-light text-secondary'}`}
                                onClick={() => setActiveTab('kyc')}
                            >
                                <i className="bi bi-shield-check me-2"></i> KYC Verification
                            </button>

                            <button
                                type="button"
                                className={`btn text-start py-2 px-3 rounded-3 fw-semibold border-0 ${activeTab === 'bookings' ? 'btn-primary' : 'btn-light text-secondary'}`}
                                onClick={() => setActiveTab('bookings')}
                            >
                                <i className="bi bi-calendar-check me-2"></i> Customer Bookings ({bookings.length})
                            </button>

                            {/* Showroom Profile & Sign Out Card */}
                            <div className="p-3 border-top mt-3 bg-light rounded-3">
                                <div className="d-flex align-items-center gap-2 mb-2">
                                    <div className="bg-primary text-white d-flex align-items-center justify-content-center rounded-circle fw-bold" style={{ width: '32px', height: '32px', fontSize: '0.82rem', flexShrink: 0 }}>
                                        {user?.completeName ? user.completeName.charAt(0).toUpperCase() : 'S'}
                                    </div>
                                    <div className="overflow-hidden">
                                        <div className="fw-bold small text-truncate" style={{ maxWidth: '140px' }}>{user?.completeName}</div>
                                        <div className="text-muted text-truncate" style={{ fontSize: '0.72rem', maxWidth: '140px' }}>{user?.email}</div>
                                    </div>
                                </div>
                                <button 
                                    type="button" 
                                    onClick={handleLogout} 
                                    className="btn btn-outline-danger btn-sm w-100 rounded-pill py-1 fw-semibold d-flex align-items-center justify-content-center gap-1"
                                    style={{ fontSize: '0.8rem' }}
                                >
                                    <i className="bi bi-box-arrow-right"></i> Sign Out
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                {/* ================= RIGHT MAIN CONTENT AREA ================= */}
                <div className="col-lg-9 col-xl-10">
                    {/* KYC Warning Banner if not verified */}
                    {!isKycVerified && (
                        <div className="alert alert-warning border-0 rounded-4 shadow-sm d-flex align-items-center justify-content-between p-3 mb-4">
                            <div className="d-flex align-items-center gap-3">
                                <i className="bi bi-exclamation-triangle-fill fs-3 text-warning"></i>
                                <div>
                                    <strong className="d-block">Action Required: Showroom KYC Verification</strong>
                                    <small className="text-secondary">
                                        Your car listings remain pending and hidden from buyers until your KYC (PAN/Aadhaar) is reviewed and approved by an administrator.
                                    </small>
                                </div>
                            </div>
                            <button 
                                onClick={() => setActiveTab('kyc')} 
                                className="btn btn-warning btn-sm rounded-pill px-3 fw-bold text-nowrap"
                            >
                                Submit / View KYC
                            </button>
                        </div>
                    )}

                    {/* TAB 1: OVERVIEW */}
                    {activeTab === 'overview' && (
                        <div>
                            {/* KPI Cards Grid */}
                            <div className="row g-3 mb-4">
                                <div className="col-sm-6 col-xl-3">
                                    <div className="card border-0 shadow-sm rounded-4 p-3 bg-white border-start border-primary border-4">
                                        <small className="text-muted fw-semibold text-uppercase">Active Listings</small>
                                        <h3 className="fw-bold text-primary mt-1 mb-0">{activeListingsCount}</h3>
                                        <small className="text-muted">Visible to buyers</small>
                                    </div>
                                </div>
                                <div className="col-sm-6 col-xl-3">
                                    <div className="card border-0 shadow-sm rounded-4 p-3 bg-white border-start border-warning border-4">
                                        <small className="text-muted fw-semibold text-uppercase">Under Review</small>
                                        <h3 className="fw-bold text-warning mt-1 mb-0">{underReviewCount}</h3>
                                        <small className="text-muted">Pending admin moderation</small>
                                    </div>
                                </div>
                                <div className="col-sm-6 col-xl-3">
                                    <div className="card border-0 shadow-sm rounded-4 p-3 bg-white border-start border-success border-4">
                                        <small className="text-muted fw-semibold text-uppercase">Active Bookings</small>
                                        <h3 className="fw-bold text-success mt-1 mb-0">{activeBookingsCount}</h3>
                                        <small className="text-muted">Token deposited</small>
                                    </div>
                                </div>
                                <div className="col-sm-6 col-xl-3">
                                    <div className="card border-0 shadow-sm rounded-4 p-3 bg-white border-start border-danger border-4">
                                        <small className="text-muted fw-semibold text-uppercase">Inventory Value</small>
                                        <h3 className="fw-bold text-dark mt-1 mb-0">{formatPrice(inventoryValue)}</h3>
                                        <small className="text-muted">{cars.length} vehicles total</small>
                                    </div>
                                </div>
                            </div>

                            {/* Recent Bookings Table */}
                            <div className="card border-0 shadow-sm rounded-4 p-4 bg-white mb-4">
                                <div className="d-flex justify-content-between align-items-center mb-3">
                                    <h5 className="fw-bold mb-0" style={{ color: '#0B2545' }}>Recent Inquiries & Token Bookings</h5>
                                    <button onClick={() => setActiveTab('bookings')} className="btn btn-link btn-sm text-primary text-decoration-none">
                                        View All &rarr;
                                    </button>
                                </div>

                                {bookings.length > 0 ? (
                                    <div className="table-responsive">
                                        <table className="table align-middle">
                                            <thead className="table-light">
                                                <tr>
                                                    <th>Booking #</th>
                                                    <th>Car Title</th>
                                                    <th>Buyer Name</th>
                                                    <th>Buyer Phone</th>
                                                    <th>Token Amount</th>
                                                    <th>Status</th>
                                                    <th>Actions</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {bookings.slice(0, 5).map(b => (
                                                    <tr key={b.id}>
                                                        <td><strong>#{b.id}</strong></td>
                                                        <td>{b.carTitle}</td>
                                                        <td>{b.buyerName}</td>
                                                        <td>{b.buyerPhone}</td>
                                                        <td className="fw-bold text-primary">₹ {b.tokenAmount ? Number(b.tokenAmount).toLocaleString('en-IN') : '10,000'}</td>
                                                        <td>
                                                            <span className={`badge ${b.status === 'CONFIRMED' ? 'text-bg-success' : b.status === 'COMPLETED' ? 'text-bg-primary' : 'text-bg-warning'}`}>
                                                                {b.status}
                                                            </span>
                                                        </td>
                                                        <td>
                                                            <div className="d-flex align-items-center gap-1">
                                                                {(b.status === 'CONFIRMED' || b.status === 'COMPLETED') && (
                                                                    <button 
                                                                        type="button" 
                                                                        onClick={() => setSelectedReceipt(b)} 
                                                                        className="btn btn-sm btn-outline-primary rounded-pill px-2 py-1"
                                                                        style={{ fontSize: '0.75rem' }}
                                                                        title="View Token Receipt"
                                                                    >
                                                                        <i className="bi bi-receipt me-1"></i> Receipt
                                                                    </button>
                                                                )}
                                                                {b.status === 'CONFIRMED' && (
                                                                    <button 
                                                                        onClick={() => handleCompleteBooking(b.id)} 
                                                                        className="btn btn-sm btn-outline-success rounded-pill px-2 py-1"
                                                                        style={{ fontSize: '0.75rem' }}
                                                                    >
                                                                        Mark Sold
                                                                    </button>
                                                                )}
                                                            </div>
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                ) : (
                                    <div className="text-center py-4 text-muted small">
                                        No bookings received yet. Add listings with detailed photos to attract buyers!
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    {/* TAB 2: MY LISTINGS */}
                    {activeTab === 'listings' && (
                        <div className="card border-0 shadow-sm rounded-4 p-4 bg-white">
                            <div className="d-flex justify-content-between align-items-center mb-4">
                                <div>
                                    <h4 className="fw-bold mb-1" style={{ color: '#0B2545' }}>My Car Listings</h4>
                                    <small className="text-muted">Manage your inventory, pricing, and view approval status</small>
                                </div>
                                <button onClick={() => setActiveTab('add-car')} className="btn btn-mg-primary btn-sm px-3">
                                    <i className="bi bi-plus-lg me-1"></i> Add New Car
                                </button>
                            </div>

                            {cars.length > 0 ? (
                                <div className="table-responsive">
                                    <table className="table table-hover align-middle">
                                        <thead className="table-light">
                                            <tr>
                                                <th>Vehicle</th>
                                                <th>Specs</th>
                                                <th>Price</th>
                                                <th>Approval</th>
                                                <th>Status</th>
                                                <th>Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {cars.map(c => (
                                                <tr key={c.id}>
                                                    <td>
                                                        <div className="d-flex align-items-center gap-3">
                                                            <img 
                                                                src={getImageUrl(c.primaryImageUrl)} 
                                                                alt={c.title} 
                                                                className="rounded-3 object-fit-cover"
                                                                style={{ width: '60px', height: '45px' }}
                                                                onError={(e) => {
                                                                    e.target.onerror = null
                                                                    e.target.src = 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80'
                                                                }}
                                                            />
                                                            <div>
                                                                <strong className="d-block">{c.brand} {c.model}</strong>
                                                                <small className="text-muted">{c.manufactureYear} • {c.variant}</small>
                                                            </div>
                                                        </div>
                                                    </td>
                                                    <td>
                                                        <small className="text-muted d-block">{c.kmDriven ? c.kmDriven.toLocaleString('en-IN') : '--'} km</small>
                                                        <small className="text-secondary">{c.fuelType} • {c.transmission}</small>
                                                    </td>
                                                    <td>
                                                        <strong className="text-dark">{formatPrice(c.price)}</strong>
                                                    </td>
                                                    <td>
                                                        <span className={`badge ${c.approvalStatus === 'APPROVED' ? 'text-bg-success' : c.approvalStatus === 'REJECTED' ? 'text-bg-danger' : 'text-bg-warning'}`}>
                                                            {c.approvalStatus}
                                                        </span>
                                                        {c.rejectionReason && (
                                                            <small className="d-block text-danger mt-1">{c.rejectionReason}</small>
                                                        )}
                                                    </td>
                                                    <td>
                                                        <span className={`badge ${c.status === 'AVAILABLE' ? 'text-bg-info' : 'text-bg-secondary'}`}>
                                                            {c.status}
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <div className="d-flex gap-2">
                                                            <Link to={`/cars/${c.id}`} className="btn btn-sm btn-outline-primary">
                                                                View
                                                            </Link>
                                                            <button 
                                                                onClick={() => handleDeleteCar(c.id)} 
                                                                className="btn btn-sm btn-outline-danger"
                                                            >
                                                                <i className="bi bi-trash"></i>
                                                            </button>
                                                        </div>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            ) : (
                                <div className="text-center py-5">
                                    <i className="bi bi-car-front text-muted fs-1"></i>
                                    <h5 className="fw-bold mt-2">No cars listed yet</h5>
                                    <p className="text-muted small">Click "Add New Car" to create your first listing.</p>
                                </div>
                            )}
                        </div>
                    )}

                    {/* TAB 3: ADD NEW CAR */}
                    {activeTab === 'add-car' && (
                        <div className="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white">
                            <h4 className="fw-bold mb-1" style={{ color: '#0B2545' }}>Add New Vehicle Listing</h4>
                            <p className="text-muted small mb-4">Upload complete specifications and photos for buyer review and admin approval</p>

                            <form onSubmit={handleAddCarSubmit}>
                                <div className="row g-3 mb-3">
                                    <div className="col-md-12">
                                        <label className="form-label small fw-semibold text-secondary">Listing Title *</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            placeholder="e.g. 2021 Hyundai Creta SX (O) Turbo Petrol"
                                            value={carForm.title}
                                            onChange={e => handleCarFormChange('title', e.target.value)}
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="row g-3 mb-3">
                                    <div className="col-md-4">
                                        <label className="form-label small fw-semibold text-secondary">Brand *</label>
                                        <select
                                            className="form-select"
                                            value={carForm.brand}
                                            onChange={e => handleCarFormChange('brand', e.target.value)}
                                            required
                                        >
                                            <option value="Hyundai">Hyundai</option>
                                            <option value="Maruti Suzuki">Maruti Suzuki</option>
                                            <option value="Honda">Honda</option>
                                            <option value="Tata">Tata</option>
                                            <option value="Mahindra">Mahindra</option>
                                            <option value="Toyota">Toyota</option>
                                            <option value="Kia">Kia</option>
                                            <option value="Volkswagen">Volkswagen</option>
                                            <option value="Skoda">Skoda</option>
                                            <option value="BMW">BMW</option>
                                            <option value="Mercedes-Benz">Mercedes-Benz</option>
                                        </select>
                                    </div>
                                    <div className="col-md-4">
                                        <label className="form-label small fw-semibold text-secondary">Model *</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            placeholder="e.g. Creta"
                                            value={carForm.model}
                                            onChange={e => handleCarFormChange('model', e.target.value)}
                                            required
                                        />
                                    </div>
                                    <div className="col-md-4">
                                        <label className="form-label small fw-semibold text-secondary">Variant *</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            placeholder="e.g. SX (O) Automatic"
                                            value={carForm.variant}
                                            onChange={e => handleCarFormChange('variant', e.target.value)}
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="row g-3 mb-3">
                                    <div className="col-md-3">
                                        <label className="form-label small fw-semibold text-secondary">Manufacture Year *</label>
                                        <input
                                            type="number"
                                            className="form-control"
                                            min="1990"
                                            max="2026"
                                            value={carForm.manufactureYear}
                                            onChange={e => handleCarFormChange('manufactureYear', e.target.value)}
                                            required
                                        />
                                    </div>
                                    <div className="col-md-3">
                                        <label className="form-label small fw-semibold text-secondary">Price (₹) *</label>
                                        <input
                                            type="number"
                                            className="form-control"
                                            min="10000"
                                            placeholder="e.g. 950000"
                                            value={carForm.price}
                                            onChange={e => handleCarFormChange('price', e.target.value)}
                                            required
                                        />
                                    </div>
                                    <div className="col-md-3">
                                        <label className="form-label small fw-semibold text-secondary">Kilometers Driven *</label>
                                        <input
                                            type="number"
                                            className="form-control"
                                            min="0"
                                            placeholder="e.g. 32000"
                                            value={carForm.kmDriven}
                                            onChange={e => handleCarFormChange('kmDriven', e.target.value)}
                                            required
                                        />
                                    </div>
                                    <div className="col-md-3">
                                        <label className="form-label small fw-semibold text-secondary">Owner Count *</label>
                                        <select
                                            className="form-select"
                                            value={carForm.ownerCount}
                                            onChange={e => handleCarFormChange('ownerCount', e.target.value)}
                                        >
                                            <option value="1">1st Owner</option>
                                            <option value="2">2nd Owner</option>
                                            <option value="3">3rd Owner</option>
                                        </select>
                                    </div>
                                </div>

                                <div className="row g-3 mb-3">
                                    <div className="col-md-4">
                                        <label className="form-label small fw-semibold text-secondary">Fuel Type *</label>
                                        <select
                                            className="form-select"
                                            value={carForm.fuelType}
                                            onChange={e => handleCarFormChange('fuelType', e.target.value)}
                                        >
                                            <option value="PETROL">Petrol</option>
                                            <option value="DIESEL">Diesel</option>
                                            <option value="CNG">CNG</option>
                                            <option value="ELECTRIC">Electric</option>
                                            <option value="HYBRID">Hybrid</option>
                                        </select>
                                    </div>
                                    <div className="col-md-4">
                                        <label className="form-label small fw-semibold text-secondary">Transmission *</label>
                                        <select
                                            className="form-select"
                                            value={carForm.transmission}
                                            onChange={e => handleCarFormChange('transmission', e.target.value)}
                                        >
                                            <option value="MANUAL">Manual</option>
                                            <option value="AUTOMATIC">Automatic</option>
                                        </select>
                                    </div>
                                    <div className="col-md-4">
                                        <label className="form-label small fw-semibold text-secondary">Body Type *</label>
                                        <select
                                            className="form-select"
                                            value={carForm.bodyType}
                                            onChange={e => handleCarFormChange('bodyType', e.target.value)}
                                        >
                                            <option value="SEDAN">Sedan</option>
                                            <option value="HATCHBACK">Hatchback</option>
                                            <option value="SUV">SUV</option>
                                            <option value="MUV">MUV</option>
                                            <option value="COUPE">Coupe</option>
                                        </select>
                                    </div>
                                </div>

                                <div className="row g-3 mb-3">
                                    <div className="col-md-4">
                                        <label className="form-label small fw-semibold text-secondary">Color *</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            placeholder="e.g. Polar White"
                                            value={carForm.color}
                                            onChange={e => handleCarFormChange('color', e.target.value)}
                                            required
                                        />
                                    </div>
                                    <div className="col-md-4">
                                        <label className="form-label small fw-semibold text-secondary">City *</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            placeholder="e.g. Pune"
                                            value={carForm.city}
                                            onChange={e => handleCarFormChange('city', e.target.value)}
                                            required
                                        />
                                    </div>
                                    <div className="col-md-4">
                                        <label className="form-label small fw-semibold text-secondary">State *</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            placeholder="e.g. Maharashtra"
                                            value={carForm.state}
                                            onChange={e => handleCarFormChange('state', e.target.value)}
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="mb-3">
                                    <label className="form-label small fw-semibold text-secondary">Vehicle Description</label>
                                    <textarea
                                        className="form-control"
                                        rows="3"
                                        placeholder="Certified showroom vehicle with complete service records, valid insurance, zero accidents..."
                                        value={carForm.description}
                                        onChange={e => handleCarFormChange('description', e.target.value)}
                                    ></textarea>
                                </div>

                                {/* ================= 3 PHOTO UPLOAD SECTIONS ================= */}
                                <div className="border rounded-4 p-3 p-md-4 bg-light mb-4">
                                    <div className="d-flex align-items-center justify-content-between mb-3 border-bottom pb-2 flex-wrap gap-2">
                                        <div>
                                            <h5 className="fw-bold mb-0 text-dark">
                                                <i className="bi bi-images text-primary me-2"></i> Vehicle Photo Gallery (3 Dedicated Sections)
                                            </h5>
                                            <small className="text-muted">High resolution photos in all 3 sections significantly improve buyer inquiry and booking rates</small>
                                        </div>
                                        <span className="badge bg-primary px-3 py-2 rounded-pill fs-6 shadow-sm">
                                            Total: {(primaryImage ? 1 : 0) + exteriorImages.length + interiorImages.length} Images
                                        </span>
                                    </div>

                                    {/* Section 1: Hero Cover Photo (Original Car Photo) */}
                                    <div className="card border-0 shadow-sm rounded-4 p-3 mb-3 bg-white">
                                        <div className="d-flex justify-content-between align-items-center mb-2 flex-wrap gap-2">
                                            <div>
                                                <strong className="text-dark d-block" style={{ fontSize: '0.92rem' }}>
                                                    <i className="bi bi-star-fill text-warning me-1"></i> 1. Original Car Photo (Hero Cover) *
                                                </strong>
                                                <small className="text-muted">Main hero picture shown in car marketplace cards, search results, and top header</small>
                                            </div>
                                            <span className="badge bg-danger text-white px-2 py-1">Required • 1 Main Image</span>
                                        </div>
                                        
                                        {primaryPreview ? (
                                            <div className="d-flex align-items-center gap-3 p-2 border rounded-3 bg-light">
                                                <img src={primaryPreview} alt="Hero Preview" className="rounded-3 object-fit-cover shadow-sm" style={{ width: '110px', height: '75px' }} />
                                                <div className="flex-grow-1 overflow-hidden">
                                                    <span className="fw-bold small text-truncate d-block">{primaryImage.name}</span>
                                                    <small className="text-muted">{(primaryImage.size / 1024).toFixed(1)} KB • Hero Cover</small>
                                                </div>
                                                <button 
                                                    type="button" 
                                                    onClick={() => { setPrimaryImage(null); setPrimaryPreview(null); }} 
                                                    className="btn btn-sm btn-outline-danger rounded-pill px-3"
                                                >
                                                    <i className="bi bi-trash3 me-1"></i> Remove
                                                </button>
                                            </div>
                                        ) : (
                                            <input 
                                                type="file" 
                                                className="form-control" 
                                                accept="image/*" 
                                                onChange={handlePrimaryChange} 
                                                required 
                                            />
                                        )}
                                    </div>

                                    {/* Section 2: Exterior Showcase (Up to 8 Images) */}
                                    <div className="card border-0 shadow-sm rounded-4 p-3 mb-3 bg-white">
                                        <div className="d-flex justify-content-between align-items-center mb-2 flex-wrap gap-2">
                                            <div>
                                                <strong className="text-dark d-block" style={{ fontSize: '0.92rem' }}>
                                                    <i className="bi bi-camera-fill text-primary me-1"></i> 2. Exterior Views & Bodywork
                                                </strong>
                                                <small className="text-muted">Front, rear, side profile, alloy wheels, headlights, taillights, and paint condition</small>
                                            </div>
                                            <span className="badge bg-primary text-white px-2 py-1">
                                                {exteriorImages.length} / 8 Photos
                                            </span>
                                        </div>

                                        <input 
                                            type="file" 
                                            className="form-control mb-2" 
                                            accept="image/*" 
                                            multiple 
                                            onChange={handleExteriorChange}
                                            disabled={exteriorImages.length >= 8}
                                        />
                                        
                                        {exteriorPreviews.length > 0 && (
                                            <div className="row g-2 mt-1">
                                                {exteriorPreviews.map((prev, idx) => (
                                                    <div key={idx} className="col-4 col-sm-3 col-md-2 position-relative">
                                                        <div className="rounded-3 overflow-hidden border position-relative shadow-sm" style={{ height: '75px' }}>
                                                            <img src={prev} alt={`Exterior ${idx}`} className="w-100 h-100 object-fit-cover" />
                                                            <button
                                                                type="button"
                                                                onClick={() => removeExteriorImage(idx)}
                                                                className="btn btn-sm btn-danger position-absolute top-0 end-0 p-0 d-flex align-items-center justify-content-center rounded-circle"
                                                                style={{ width: '22px', height: '22px', fontSize: '0.75rem', margin: '3px' }}
                                                                title="Remove exterior photo"
                                                            >
                                                                ×
                                                            </button>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>

                                    {/* Section 3: Interior & Cabin Showcase (Up to 8 Images) */}
                                    <div className="card border-0 shadow-sm rounded-4 p-3 bg-white">
                                        <div className="d-flex justify-content-between align-items-center mb-2 flex-wrap gap-2">
                                            <div>
                                                <strong className="text-dark d-block" style={{ fontSize: '0.92rem' }}>
                                                    <i className="bi bi-speedometer text-success me-1"></i> 3. Interior & Cabin Details
                                                </strong>
                                                <small className="text-muted">Dashboard console, steering wheel & cluster, front & rear seats, infotainment screen, and boot space</small>
                                            </div>
                                            <span className="badge bg-success text-white px-2 py-1">
                                                {interiorImages.length} / 8 Photos
                                            </span>
                                        </div>

                                        <input 
                                            type="file" 
                                            className="form-control mb-2" 
                                            accept="image/*" 
                                            multiple 
                                            onChange={handleInteriorChange}
                                            disabled={interiorImages.length >= 8}
                                        />

                                        {interiorPreviews.length > 0 && (
                                            <div className="row g-2 mt-1">
                                                {interiorPreviews.map((prev, idx) => (
                                                    <div key={idx} className="col-4 col-sm-3 col-md-2 position-relative">
                                                        <div className="rounded-3 overflow-hidden border position-relative shadow-sm" style={{ height: '75px' }}>
                                                            <img src={prev} alt={`Interior ${idx}`} className="w-100 h-100 object-fit-cover" />
                                                            <button
                                                                type="button"
                                                                onClick={() => removeInteriorImage(idx)}
                                                                className="btn btn-sm btn-danger position-absolute top-0 end-0 p-0 d-flex align-items-center justify-content-center rounded-circle"
                                                                style={{ width: '22px', height: '22px', fontSize: '0.75rem', margin: '3px' }}
                                                                title="Remove interior photo"
                                                            >
                                                                ×
                                                            </button>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                </div>

                                <button
                                    type="submit"
                                    className="btn btn-mg-primary py-3 px-5 fw-bold"
                                    disabled={formSubmitting}
                                >
                                    {formSubmitting ? (
                                        <>
                                            <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                                            Uploading & Saving Listing...
                                        </>
                                    ) : (
                                        'Submit Car for Moderation'
                                    )}
                                </button>
                            </form>
                        </div>
                    )}

                    {/* TAB 4: KYC VERIFICATION */}
                    {activeTab === 'kyc' && (
                        <div className="card border-0 shadow-sm rounded-4 p-4 p-md-5 bg-white">
                            <h4 className="fw-bold mb-1" style={{ color: '#0B2545' }}>Showroom KYC Verification</h4>
                            <p className="text-muted small mb-4">
                                Submit government identity verification to unlock the "Verified Dealer" badge and publish car listings to buyers.
                            </p>

                            {/* Current KYC Status */}
                            <div className="bg-light rounded-4 p-4 mb-4">
                                <h6 className="fw-bold mb-2">Current Verification Status</h6>
                                <div className="d-flex align-items-center gap-3">
                                    <span className={`badge rounded-pill ${isKycVerified ? 'text-bg-success' : 'text-bg-warning'} px-3 py-2 fs-6`}>
                                        {kycProfile?.kycStatus || 'NOT SUBMITTED'}
                                    </span>
                                    {kycProfile?.kycDocumentType && (
                                        <span className="text-muted small">
                                            Document: <strong>{kycProfile.kycDocumentType}</strong> ({kycProfile.kycDocumentNumber})
                                        </span>
                                    )}
                                </div>
                                {kycProfile?.kycRejectionReason && (
                                    <div className="alert alert-danger mt-3 mb-0 small">
                                        Rejection Reason: {kycProfile.kycRejectionReason}
                                    </div>
                                )}
                            </div>

                            {/* KYC Upload Form */}
                            <form onSubmit={handleKycSubmit}>
                                <div className="row g-3 mb-3">
                                    <div className="col-md-6">
                                        <label className="form-label small fw-semibold text-secondary">Document Type *</label>
                                        <select
                                            className="form-select"
                                            value={kycDocType}
                                            onChange={e => setKycDocType(e.target.value)}
                                        >
                                            <option value="PAN">PAN Card</option>
                                            <option value="AADHAAR">Aadhaar Card</option>
                                        </select>
                                    </div>
                                    <div className="col-md-6">
                                        <label className="form-label small fw-semibold text-secondary">Document Number *</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            placeholder={kycDocType === 'PAN' ? 'ABCDE1234F' : '12-digit Aadhaar number'}
                                            value={kycDocNumber}
                                            onChange={e => setKycDocNumber(e.target.value)}
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="mb-4">
                                    <label className="form-label small fw-semibold text-secondary">Upload Document (PDF or Image) *</label>
                                    <input
                                        type="file"
                                        className="form-control"
                                        accept="image/*,application/pdf"
                                        onChange={e => setKycFile(e.target.files[0])}
                                        required
                                    />
                                </div>

                                <button
                                    type="submit"
                                    className="btn btn-mg-primary py-3 px-4 fw-bold"
                                    disabled={kycSubmitting}
                                >
                                    {kycSubmitting ? (
                                        <>
                                            <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                                            Uploading Document...
                                        </>
                                    ) : (
                                        'Upload & Submit KYC Document'
                                    )}
                                </button>
                            </form>
                        </div>
                    )}

                    {/* TAB 5: CUSTOMER BOOKINGS */}
                    {activeTab === 'bookings' && (
                        <div className="card border-0 shadow-sm rounded-4 p-4 bg-white">
                            <h4 className="fw-bold mb-1" style={{ color: '#0B2545' }}>Customer Token Bookings</h4>
                            <p className="text-muted small mb-4">Review buyers who placed 2% token deposits on your inventory</p>

                            {bookings.length > 0 ? (
                                <div className="table-responsive">
                                    <table className="table table-hover align-middle">
                                        <thead className="table-light">
                                            <tr>
                                                <th>Booking #</th>
                                                <th>Car Details</th>
                                                <th>Buyer</th>
                                                <th>Token Deposit</th>
                                                <th>Status</th>
                                                <th>Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {bookings.map(b => (
                                                <tr key={b.id}>
                                                    <td><strong>#{b.id}</strong></td>
                                                    <td>
                                                        <strong className="d-block">{b.carTitle}</strong>
                                                        <small className="text-muted">Car ID: #{b.carId}</small>
                                                    </td>
                                                    <td>
                                                        <div>{b.buyerName}</div>
                                                        <small className="text-muted">{b.buyerPhone} • {b.buyerEmail}</small>
                                                    </td>
                                                    <td className="fw-bold text-primary">₹ {b.tokenAmount ? Number(b.tokenAmount).toLocaleString('en-IN') : '10,000'}</td>
                                                    <td>
                                                        <span className={`badge ${b.status === 'CONFIRMED' ? 'text-bg-success' : b.status === 'COMPLETED' ? 'text-bg-primary' : 'text-bg-warning'}`}>
                                                            {b.status}
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <div className="d-flex align-items-center gap-1">
                                                            {(b.status === 'CONFIRMED' || b.status === 'COMPLETED') && (
                                                                <button
                                                                    type="button"
                                                                    onClick={() => setSelectedReceipt(b)}
                                                                    className="btn btn-sm btn-outline-primary rounded-pill px-3 py-1"
                                                                    title="View Customer Token Receipt"
                                                                >
                                                                    <i className="bi bi-receipt me-1"></i> Receipt
                                                                </button>
                                                            )}
                                                            {b.status === 'CONFIRMED' && (
                                                                <button
                                                                    onClick={() => handleCompleteBooking(b.id)}
                                                                    className="btn btn-sm btn-success rounded-pill px-3 py-1"
                                                                >
                                                                    Mark Sold
                                                                </button>
                                                            )}
                                                        </div>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            ) : (
                                <div className="text-center py-5 text-muted small">
                                    No customer bookings received yet.
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>

            {/* Token Deposit Receipt Modal */}
            <ReceiptModal 
                booking={selectedReceipt} 
                onClose={() => setSelectedReceipt(null)} 
            />
        </div>
    )
}

export default SellerDashboard

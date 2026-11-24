import React, { useContext, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { formatPrice, getCarById, getImageUrl } from '../services/carservice'
import { addToWishlist, createBooking, getMyBookings } from '../services/buyerservice'
import { createPaymentOrder, verifyPayment, recordPaymentFailure } from '../services/paymentservice'
import { getStoredToken } from '../services/authservice'
import { toast } from 'react-toastify'
import { UserContext } from '../App'
import { useDispatch } from 'react-redux'
import { addToWishlistAction } from '../slices/WishlistSlice'
import ReceiptModal from '../component/ReceiptModal'

function CarDetails() {
    const { id } = useParams()
    const navigate = useNavigate()
    const dispatch = useDispatch()
    const { loginStatus, user } = useContext(UserContext)

    const [car, setCar] = useState(null)
    const [activeImage, setActiveImage] = useState(null)
    const [loading, setLoading] = useState(true)
    const [bookingLoading, setBookingLoading] = useState(false)
    const [existingBooking, setExistingBooking] = useState(null)
    const [paymentFailed, setPaymentFailed] = useState(false)
    const [selectedReceipt, setSelectedReceipt] = useState(null)
    const [photoSection, setPhotoSection] = useState('ALL')

    useEffect(() => {
        loadCar()
        checkExistingBooking()
    }, [id, loginStatus, user])

    const checkExistingBooking = async () => {
        if (!loginStatus || !user || user.userRole !== 'ROLE_BUYER') return
        const token = getStoredToken()
        if (!token) return
        try {
            const res = await getMyBookings(token, 0, 50)
            const list = res.data?.content || res.data || []
            const found = list.find(b => (Number(b.carId) === Number(id) || Number(b.car?.id) === Number(id)) && ['PENDING_PAYMENT', 'CONFIRMED', 'COMPLETED'].includes(b.status))
            if (found) {
                setExistingBooking(found)
                if (found.paymentStatus === 'FAILED') {
                    setPaymentFailed(true)
                }
            } else {
                setExistingBooking(null)
            }
        } catch (err) {
            console.warn('Booking check error:', err)
        }
    }

    const loadCar = async () => {
        setLoading(true)
        try {
            const response = await getCarById(id)
            setCar(response.data)
            if (response.data.primaryImageUrl) {
                setActiveImage(response.data.primaryImageUrl)
            } else if (response.data.images && response.data.images.length > 0) {
                setActiveImage(response.data.images[0].imageUrl)
            }
        } catch (error) {
            console.error('Failed to load car details:', error)
            toast.error('Failed to load car details')
        } finally {
            setLoading(false)
        }
    }

    const calculateTokenDeposit = (price) => {
        if (!price) return 10000
        const twoPercent = Math.round(Number(price) * 0.02)
        return Math.min(50000, Math.max(5000, twoPercent))
    }

    const handleWishlist = async () => {
        if (!loginStatus || !user) {
            toast.info('Please login to save to wishlist')
            navigate('/login')
            return
        }
        try {
            const token = getStoredToken()
            await addToWishlist(car.id, token)
            dispatch(addToWishlistAction(car))
            toast.success('Car added to your wishlist!')
        } catch (error) {
            toast.error(error.response?.data?.message || 'Could not update wishlist')
        }
    }

    const handleBookNow = async () => {
        if (!loginStatus || !user) {
            toast.info('Please sign in as a buyer to book this car')
            navigate('/login')
            return
        }

        if (user.userRole !== 'ROLE_BUYER') {
            toast.warning('Only buyer accounts can place token bookings')
            return
        }

        setBookingLoading(true)
        const token = getStoredToken()

        try {
            // Step 1: Obtain or reuse booking for this car
            let bookingToPay = existingBooking
            if (!bookingToPay) {
                const bookingRes = await createBooking(car.id, token)
                bookingToPay = bookingRes.data
                setExistingBooking(bookingToPay)
                toast.success(`Booking reserved! Booking ID: #${bookingToPay.id}`)
            }

            // Step 2: Initialize Razorpay Payment Order
            try {
                const orderRes = await createPaymentOrder(bookingToPay.id, token)
                const orderData = orderRes.data

                const key = orderData.razorpayKeyId || orderData.keyId || ''
                const isMock = !key || 
                    key.includes('placeholder') || 
                    (orderData.razorpayOrderId && orderData.razorpayOrderId.startsWith('order_mock_'))

                if (isMock) {
                    // Seamless mock mode payment verification for offline testing
                    const mockPaymentId = `pay_mock_${Date.now()}`
                    const mockSignature = `sig_mock_${Date.now()}`
                    await verifyPayment({
                        orderId: orderData.razorpayOrderId,
                        paymentId: mockPaymentId,
                        signature: mockSignature
                    }, token)
                    toast.success('Token deposit confirmed! Booking is active.')
                    setPaymentFailed(false)
                    await checkExistingBooking()
                    navigate('/my-bookings')
                    return
                }

                // Razorpay JS SDK Checkout
                if (window.Razorpay) {
                    const amountInPaise = Math.round(Number(orderData.amount || tokenAmount) * 100)
                    const prefillData = {}
                    const customerName = orderData.prefill?.name || user?.completeName || user?.name || ''
                    if (customerName) prefillData.name = customerName

                    const customerEmail = orderData.prefill?.email || user?.email || ''
                    if (customerEmail) prefillData.email = customerEmail

                    const rawPhone = orderData.prefill?.contact || user?.phone || user?.buyerProfile?.phone || ''
                    const cleanPhone = String(rawPhone).replace(/\D/g, '')
                    if (cleanPhone.length >= 10) {
                        prefillData.contact = cleanPhone.slice(-10)
                    }

                    const options = {
                        key: key,
                        amount: amountInPaise,
                        currency: 'INR',
                        name: 'MyGaadi.com',
                        description: `Token Deposit for ${car.brand} ${car.model}`,
                        order_id: orderData.razorpayOrderId,
                        handler: async function (response) {
                            try {
                                await verifyPayment({
                                    orderId: response.razorpay_order_id,
                                    paymentId: response.razorpay_payment_id,
                                    signature: response.razorpay_signature
                                }, token)
                                toast.success('Token deposit verified successfully!')
                                setPaymentFailed(false)
                                await checkExistingBooking()
                                navigate('/my-bookings')
                            } catch (err) {
                                toast.error('Payment verification failed')
                            }
                        },
                        prefill: prefillData,
                        theme: {
                            color: '#0B2545'
                        },
                        modal: {
                            ondismiss: function () {
                                toast.info('Payment window closed. You can complete payment anytime from My Bookings or retry below.')
                                checkExistingBooking()
                            }
                        }
                    }
                    const rzp = new window.Razorpay(options)
                    rzp.on('payment.failed', async function (resp) {
                        console.warn('Razorpay payment failed:', resp)
                        setPaymentFailed(true)
                        try {
                            await recordPaymentFailure({
                                orderId: orderData.razorpayOrderId,
                                reason: resp.error?.description || 'Payment failed or declined'
                            }, token)
                        } catch (e) {
                            console.warn('Could not record failure:', e)
                        }
                        toast.error(resp.error?.description || 'Payment failed. You can retry payment below.')
                        checkExistingBooking()
                    })
                    rzp.open()
                } else {
                    toast.info('Booking registered! Proceed to payments in My Bookings.')
                    navigate('/my-bookings')
                }
            } catch (payError) {
                console.warn('Payment order initialisation note:', payError)
                toast.success('Booking registered! Proceed to payments in My Bookings.')
                navigate('/my-bookings')
            }
        } catch (error) {
            console.error('Booking failed:', error)
            toast.error(error.response?.data?.message || 'Failed to place booking')
        } finally {
            setBookingLoading(false)
        }
    }

    if (loading) {
        return (
            <div className="container py-5 text-center">
                <div className="spinner-border text-primary my-5" role="status"></div>
                <p className="text-muted">Loading vehicle specifications...</p>
            </div>
        )
    }

    if (!car) {
        return (
            <div className="container py-5 text-center">
                <h4 className="fw-bold">Car Not Found</h4>
                <p className="text-muted">This vehicle listing is no longer available or was removed.</p>
                <Link to="/cars" className="btn btn-mg-primary">Back to Listings</Link>
            </div>
        )
    }

    const tokenAmount = calculateTokenDeposit(car.price)

    return (
        <div className="container py-4 my-2">
            {/* Breadcrumb Navigation */}
            <nav aria-label="breadcrumb" className="mb-3">
                <ol className="breadcrumb small">
                    <li className="breadcrumb-item"><Link to="/" className="text-muted">Home</Link></li>
                    <li className="breadcrumb-item"><Link to="/cars" className="text-muted">Cars</Link></li>
                    <li className="breadcrumb-item active" aria-current="page">{car.brand} {car.model}</li>
                </ol>
            </nav>

            <div className="row g-4">
                {/* Left Column: Image Gallery & Description */}
                <div className="col-lg-7">
                    {/* Main Image Display */}
                    <div 
                        className="bg-white rounded-4 overflow-hidden border shadow-sm mb-3 position-relative"
                        style={{ height: '420px' }}
                    >
                        <img
                            src={getImageUrl(activeImage)}
                            alt={car.title || `${car.brand} ${car.model}`}
                            className="w-100 h-100 object-fit-cover"
                            onError={(e) => {
                                e.target.onerror = null
                                e.target.src = 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80'
                            }}
                        />
                        {car.sellerVerified && (
                            <span 
                                className="position-absolute top-0 start-0 m-3 badge px-3 py-2 text-white shadow-sm"
                                style={{ background: '#1B998B', borderRadius: '50px', fontSize: '0.8rem' }}
                            >
                                <i className="bi bi-patch-check-fill me-1"></i> VERIFIED SELLER
                            </span>
                        )}
                        <span 
                            className="position-absolute top-0 end-0 m-3 badge px-3 py-2 text-white shadow-sm"
                            style={{ 
                                background: 'rgba(11, 37, 69, 0.85)', 
                                borderRadius: '50px', 
                                fontSize: '0.78rem',
                                backdropFilter: 'blur(4px)'
                            }}
                        >
                            <i className="bi bi-camera me-1"></i>
                            {(() => {
                                const currentObj = (car.images || []).find(i => (i.url || i.imageUrl) === activeImage)
                                if (currentObj?.category === 'INTERIOR') return 'Interior View'
                                if (currentObj?.category === 'PRIMARY' || currentObj?.primary) return 'Original Car Photo'
                                return 'Exterior View'
                            })()}
                        </span>
                    </div>

                    {/* 3 Dedicated Photo Category Tabs */}
                    {car.images && car.images.length > 0 && (() => {
                        const allCarImages = car.images || []
                        const primaryImages = allCarImages.filter(img => img.category === 'PRIMARY' || img.primary)
                        const exteriorImages = allCarImages.filter(img => img.category === 'EXTERIOR' || (!img.primary && img.category !== 'INTERIOR'))
                        const interiorImages = allCarImages.filter(img => img.category === 'INTERIOR')

                        const displayedThumbnails = photoSection === 'PRIMARY' 
                            ? primaryImages 
                            : photoSection === 'EXTERIOR' 
                            ? exteriorImages 
                            : photoSection === 'INTERIOR' 
                            ? interiorImages 
                            : allCarImages

                        return (
                            <div className="mb-4">
                                <div className="d-flex align-items-center gap-2 mb-2 overflow-auto pb-1">
                                    <button
                                        type="button"
                                        onClick={() => setPhotoSection('ALL')}
                                        className={`btn btn-sm rounded-pill px-3 py-1 fw-semibold border shadow-sm ${photoSection === 'ALL' ? 'btn-primary' : 'btn-light text-secondary'}`}
                                        style={{ fontSize: '0.78rem' }}
                                    >
                                        All Photos ({allCarImages.length})
                                    </button>
                                    {primaryImages.length > 0 && (
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setPhotoSection('PRIMARY')
                                                const first = primaryImages[0]
                                                if (first) setActiveImage(first.url || first.imageUrl)
                                            }}
                                            className={`btn btn-sm rounded-pill px-3 py-1 fw-semibold border shadow-sm ${photoSection === 'PRIMARY' ? 'btn-primary' : 'btn-light text-secondary'}`}
                                            style={{ fontSize: '0.78rem' }}
                                        >
                                            <i className="bi bi-star-fill text-warning me-1"></i> 1. Original Photo ({primaryImages.length})
                                        </button>
                                    )}
                                    {exteriorImages.length > 0 && (
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setPhotoSection('EXTERIOR')
                                                const first = exteriorImages[0]
                                                if (first) setActiveImage(first.url || first.imageUrl)
                                            }}
                                            className={`btn btn-sm rounded-pill px-3 py-1 fw-semibold border shadow-sm ${photoSection === 'EXTERIOR' ? 'btn-primary' : 'btn-light text-secondary'}`}
                                            style={{ fontSize: '0.78rem' }}
                                        >
                                            <i className="bi bi-camera-fill text-info me-1"></i> 2. Exterior ({exteriorImages.length})
                                        </button>
                                    )}
                                    {interiorImages.length > 0 && (
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setPhotoSection('INTERIOR')
                                                const first = interiorImages[0]
                                                if (first) setActiveImage(first.url || first.imageUrl)
                                            }}
                                            className={`btn btn-sm rounded-pill px-3 py-1 fw-semibold border shadow-sm ${photoSection === 'INTERIOR' ? 'btn-primary' : 'btn-light text-secondary'}`}
                                            style={{ fontSize: '0.78rem' }}
                                        >
                                            <i className="bi bi-speedometer text-success me-1"></i> 3. Interior ({interiorImages.length})
                                        </button>
                                    )}
                                </div>

                                {/* Thumbnail Strip */}
                                <div className="d-flex gap-2 overflow-auto pb-2">
                                    {displayedThumbnails.map((img, idx) => {
                                        const imgUrl = img.url || img.imageUrl
                                        const isSelected = activeImage === imgUrl
                                        return (
                                            <button
                                                key={idx}
                                                type="button"
                                                onClick={() => setActiveImage(imgUrl)}
                                                className={`btn p-0 rounded-3 overflow-hidden border-2 flex-shrink-0 position-relative ${isSelected ? 'border-primary shadow' : 'border-light'}`}
                                                style={{ width: '88px', height: '65px' }}
                                            >
                                                <img 
                                                    src={getImageUrl(imgUrl)} 
                                                    alt={`Thumbnail ${idx}`} 
                                                    className="w-100 h-100 object-fit-cover"
                                                />
                                                <span 
                                                    className="position-absolute bottom-0 start-0 end-0 bg-dark bg-opacity-75 text-white text-truncate text-center"
                                                    style={{ fontSize: '0.62rem', padding: '1px 2px' }}
                                                >
                                                    {img.category === 'INTERIOR' ? 'Interior' : (img.category === 'PRIMARY' || img.primary) ? 'Original' : 'Exterior'}
                                                </span>
                                            </button>
                                        )
                                    })}
                                </div>
                            </div>
                        )
                    })()}

                    {/* Overview & Description Card */}
                    <div className="card border-0 shadow-sm rounded-4 p-4 mb-4 bg-white">
                        <h5 className="fw-bold mb-3" style={{ color: '#0B2545' }}>Vehicle Overview</h5>
                        <p className="text-secondary" style={{ lineHeight: '1.7' }}>
                            {car.description || `Inspected ${car.manufactureYear} ${car.brand} ${car.model} ${car.variant || ''} with complete dealership maintenance history. Registered in ${car.city || 'India'}, offering pristine condition and genuine odometer reading.`}
                        </p>

                        <hr className="my-3 opacity-25" />

                        {/* Inspection Features */}
                        <h6 className="fw-bold mb-3" style={{ color: '#0B2545' }}>Inspection & Certification</h6>
                        <div className="row g-3 text-secondary small">
                            <div className="col-sm-6 d-flex align-items-center gap-2">
                                <i className="bi bi-shield-check text-success fs-5"></i>
                                <span>140-point technical engine inspection</span>
                            </div>
                            <div className="col-sm-6 d-flex align-items-center gap-2">
                                <i className="bi bi-file-earmark-text text-success fs-5"></i>
                                <span>Zero outstanding traffic challans</span>
                            </div>
                            <div className="col-sm-6 d-flex align-items-center gap-2">
                                <i className="bi bi-check-circle text-success fs-5"></i>
                                <span>Non-accidental certified chassis</span>
                            </div>
                            <div className="col-sm-6 d-flex align-items-center gap-2">
                                <i className="bi bi-arrow-repeat text-success fs-5"></i>
                                <span>Dealership transfer & RC guarantee</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Right Column: Pricing & Quick Booking Action */}
                <div className="col-lg-5">
                    <div className="card border-0 shadow-sm rounded-4 p-4 bg-white sticky-top" style={{ top: '90px' }}>
                        <div className="d-flex justify-content-between align-items-start mb-2">
                            <div>
                                <span className="badge bg-light text-dark border px-2 py-1 mb-2 fw-bold">
                                    {car.manufactureYear || '2021'}
                                </span>
                                <h3 className="fw-bold mb-1" style={{ color: '#0B2545' }}>
                                    {car.brand} {car.model}
                                </h3>
                                <p className="text-muted small mb-0">{car.variant}</p>
                            </div>
                            <button
                                type="button"
                                onClick={handleWishlist}
                                className="btn btn-outline-danger rounded-circle p-2"
                                title="Save to wishlist"
                            >
                                <i className="bi bi-heart"></i>
                            </button>
                        </div>

                        {/* Price & Token Deposit Highlight / Sold Indicator */}
                        <div className={`rounded-4 p-3 my-3 ${car.status === 'SOLD' ? 'bg-danger-subtle border border-danger-subtle' : 'bg-light'}`}>
                            <div className="d-flex justify-content-between align-items-center mb-2 flex-wrap gap-2">
                                <span className="text-muted small fw-semibold">Fixed Vehicle Price</span>
                                <div className="d-flex align-items-center gap-2">
                                    <span className="fs-3 fw-bold text-dark">{formatPrice(car.price)}</span>
                                    {car.status === 'SOLD' && (
                                        <span className="badge bg-danger fs-6 px-3 py-2 text-uppercase fw-bold shadow-sm">
                                            <i className="bi bi-tag-fill me-1"></i> SOLD
                                        </span>
                                    )}
                                </div>
                            </div>
                            {car.status === 'SOLD' ? (
                                <div className="d-flex align-items-center gap-2 pt-2 border-top border-danger-subtle text-danger fw-semibold small">
                                    <i className="bi bi-check-circle-fill fs-5"></i>
                                    <span>This vehicle has been purchased and marked SOLD by the dealership.</span>
                                </div>
                            ) : (
                                <>
                                    <div className="d-flex justify-content-between align-items-center pt-2 border-top">
                                        <span className="small text-secondary fw-semibold">
                                            <i className="bi bi-lightning-charge-fill text-warning me-1"></i>
                                            2% Token Deposit
                                        </span>
                                        <span className="fw-bold text-primary">₹ {tokenAmount.toLocaleString('en-IN')}</span>
                                    </div>
                                    <small className="text-muted d-block mt-1" style={{ fontSize: '0.75rem' }}>
                                        Fully refundable if vehicle does not match the inspection condition.
                                    </small>
                                </>
                            )}
                        </div>

                        {/* Specifications Grid with High-Contrast Bold Text */}
                        <div className="row g-2 mb-4 text-center">
                            <div className="col-4">
                                <div className="border rounded-3 p-2 bg-white shadow-sm">
                                    <small className="text-muted d-block fw-semibold" style={{ fontSize: '0.72rem', textTransform: 'uppercase' }}>Kilometers</small>
                                    <strong className="text-dark fw-bold d-block" style={{ fontSize: '0.88rem' }}>{car.kmDriven ? car.kmDriven.toLocaleString('en-IN') : '35,000'} km</strong>
                                </div>
                            </div>
                            <div className="col-4">
                                <div className="border rounded-3 p-2 bg-white shadow-sm">
                                    <small className="text-muted d-block fw-semibold" style={{ fontSize: '0.72rem', textTransform: 'uppercase' }}>Fuel</small>
                                    <strong className="text-dark fw-bold d-block" style={{ fontSize: '0.88rem' }}>{car.fuelType || 'Petrol'}</strong>
                                </div>
                            </div>
                            <div className="col-4">
                                <div className="border rounded-3 p-2 bg-white shadow-sm">
                                    <small className="text-muted d-block fw-semibold" style={{ fontSize: '0.72rem', textTransform: 'uppercase' }}>Gearbox</small>
                                    <strong className="text-dark fw-bold d-block" style={{ fontSize: '0.88rem' }}>{car.transmission || 'Manual'}</strong>
                                </div>
                            </div>
                            <div className="col-4">
                                <div className="border rounded-3 p-2 bg-white shadow-sm">
                                    <small className="text-muted d-block fw-semibold" style={{ fontSize: '0.72rem', textTransform: 'uppercase' }}>Owners</small>
                                    <strong className="text-dark fw-bold d-block" style={{ fontSize: '0.88rem' }}>{car.ownerCount ? `${car.ownerCount} Owner` : '1st Owner'}</strong>
                                </div>
                            </div>
                            <div className="col-4">
                                <div className="border rounded-3 p-2 bg-white shadow-sm">
                                    <small className="text-muted d-block fw-semibold" style={{ fontSize: '0.72rem', textTransform: 'uppercase' }}>Color</small>
                                    <strong className="text-dark fw-bold text-capitalize d-block" style={{ fontSize: '0.88rem' }}>{car.color || 'White'}</strong>
                                </div>
                            </div>
                            <div className="col-4">
                                <div className="border rounded-3 p-2 bg-white shadow-sm">
                                    <small className="text-muted d-block fw-semibold" style={{ fontSize: '0.72rem', textTransform: 'uppercase' }}>Location</small>
                                    <strong className="text-dark fw-bold text-truncate d-block" style={{ fontSize: '0.88rem' }}>{car.city || 'India'}</strong>
                                </div>
                            </div>
                        </div>

                        {/* Dealership Info */}
                        <div className="border rounded-3 p-3 mb-4 d-flex align-items-center gap-3 bg-light">
                            <div className="p-2 rounded-circle bg-primary bg-opacity-10 text-primary">
                                <i className="bi bi-shop-window fs-4"></i>
                            </div>
                            <div className="flex-grow-1 text-truncate">
                                <small className="text-muted d-block">Dealership Showroom</small>
                                <span className="fw-bold d-block text-truncate text-dark">{car.sellerShowroom || car.sellerName || 'Certified Hub'}</span>
                                <small className="text-success fw-semibold"><i className="bi bi-check-circle me-1"></i>Verified Dealer</small>
                            </div>
                        </div>

                        {/* Booking CTA Section with Real-Time Payment Status */}
                        {existingBooking && (existingBooking.status === 'CONFIRMED' || existingBooking.status === 'COMPLETED') ? (
                            <div className="alert alert-success d-flex flex-column flex-sm-row align-items-sm-center justify-content-between p-3 rounded-4 mb-0 border-0 shadow-sm gap-2" style={{ background: '#E8F5E9' }}>
                                <div>
                                    <div className="fw-bold text-success mb-1">
                                        <i className="bi bi-shield-check me-2"></i>You Reserved This Car
                                    </div>
                                    <small className="text-muted">
                                        Booking #{existingBooking.id} • Status: <span className="badge bg-success">{existingBooking.status}</span>
                                    </small>
                                </div>
                                <div className="d-flex align-items-center gap-2">
                                    <button
                                        type="button"
                                        onClick={() => setSelectedReceipt(existingBooking)}
                                        className="btn btn-sm btn-outline-success fw-semibold px-3 py-2 rounded-pill shadow-sm"
                                    >
                                        <i className="bi bi-receipt me-1"></i> View Receipt
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => navigate('/my-bookings')}
                                        className="btn btn-sm btn-success fw-semibold px-3 py-2 rounded-pill shadow-sm"
                                    >
                                        My Bookings <i className="bi bi-arrow-right ms-1"></i>
                                    </button>
                                </div>
                            </div>
                        ) : (paymentFailed || existingBooking?.paymentStatus === 'FAILED') ? (
                            <div className="card border-danger rounded-4 p-3 mb-0 shadow-sm" style={{ backgroundColor: '#FFF5F5' }}>
                                <div className="d-flex align-items-center justify-content-between mb-2">
                                    <div className="fw-bold text-danger d-flex align-items-center gap-2">
                                        <i className="bi bi-x-circle-fill fs-5"></i>
                                        <span>Payment Failed</span>
                                    </div>
                                    <span className="badge bg-danger text-white px-2 py-1">PAYMENT FAILED</span>
                                </div>
                                <p className="small text-muted mb-3">
                                    Your 2% token payment attempt for Booking #{existingBooking?.id || ''} was not completed. Your car reservation hold is temporarily maintained. Please retry below to secure your car.
                                </p>
                                <div className="d-flex gap-2">
                                    <button
                                        type="button"
                                        onClick={handleBookNow}
                                        disabled={bookingLoading}
                                        className="btn btn-danger flex-fill py-2 fw-bold shadow-sm rounded-pill"
                                    >
                                        {bookingLoading ? (
                                            <>
                                                <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                                                Opening Gateway...
                                            </>
                                        ) : (
                                            <>
                                                <i className="bi bi-arrow-repeat me-1"></i> Retry Payment (₹ {tokenAmount.toLocaleString('en-IN')})
                                            </>
                                        )}
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => navigate('/my-bookings')}
                                        className="btn btn-outline-secondary py-2 px-3 rounded-pill small"
                                    >
                                        My Bookings
                                    </button>
                                </div>
                            </div>
                        ) : existingBooking?.status === 'PENDING_PAYMENT' ? (
                            <div className="card border-warning rounded-4 p-3 mb-0 shadow-sm" style={{ backgroundColor: '#FFFDF5' }}>
                                <div className="d-flex align-items-center justify-content-between mb-2">
                                    <div className="fw-bold text-warning-emphasis d-flex align-items-center gap-2">
                                        <i className="bi bi-clock-history fs-5"></i>
                                        <span>Token Payment Pending</span>
                                    </div>
                                    <span className="badge bg-warning text-dark px-2 py-1">PENDING</span>
                                </div>
                                <p className="small text-muted mb-3">
                                    You have initiated Booking #{existingBooking.id}. Complete the 2% token deposit now to confirm your car hold.
                                </p>
                                <div className="d-flex gap-2">
                                    <button
                                        type="button"
                                        onClick={handleBookNow}
                                        disabled={bookingLoading}
                                        className="btn btn-mg-primary flex-fill py-2 fw-bold shadow-sm rounded-pill"
                                    >
                                        {bookingLoading ? (
                                            <>
                                                <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                                                Opening Gateway...
                                            </>
                                        ) : (
                                            <>
                                                <i className="bi bi-credit-card me-1"></i> Complete Payment (₹ {tokenAmount.toLocaleString('en-IN')})
                                            </>
                                        )}
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => navigate('/my-bookings')}
                                        className="btn btn-outline-secondary py-2 px-3 rounded-pill small"
                                    >
                                        My Bookings
                                    </button>
                                </div>
                            </div>
                        ) : car.status === 'SOLD' ? (
                            <button
                                type="button"
                                disabled
                                className="btn btn-secondary w-100 py-3 fw-bold fs-6 shadow-sm"
                                style={{ borderRadius: '50px' }}
                            >
                                <i className="bi bi-slash-circle me-2"></i> Vehicle Sold Out
                            </button>
                        ) : car.status === 'RESERVED' ? (
                            <div>
                                <button
                                    type="button"
                                    disabled
                                    className="btn btn-warning w-100 py-3 fw-bold fs-6 shadow-sm text-dark"
                                    style={{ borderRadius: '50px' }}
                                >
                                    <i className="bi bi-lock-fill me-2"></i> Currently Under Reservation
                                </button>
                                <small className="text-muted d-block text-center mt-2">
                                    Another buyer currently has an active token reservation on this vehicle.
                                </small>
                            </div>
                        ) : (
                            <button
                                type="button"
                                onClick={handleBookNow}
                                disabled={bookingLoading}
                                className="btn btn-mg-primary w-100 py-3 fw-bold fs-6 shadow"
                                style={{ borderRadius: '50px' }}
                            >
                                {bookingLoading ? (
                                    <>
                                        <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                                        Processing Token Booking...
                                    </>
                                ) : (
                                    <>Book with 2% Token (₹ {tokenAmount.toLocaleString('en-IN')})</>
                                )}
                            </button>
                        )}
                    </div>
                </div>
            </div>

            {/* Official Token Receipt Modal */}
            <ReceiptModal 
                booking={selectedReceipt} 
                onClose={() => setSelectedReceipt(null)} 
            />
        </div>
    )
}

export default CarDetails

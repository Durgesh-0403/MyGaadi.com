import React, { useContext, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { cancelBooking, getMyBookings } from '../services/buyerservice'
import { createPaymentOrder, verifyPayment, recordPaymentFailure } from '../services/paymentservice'
import { getImageUrl } from '../services/carservice'
import { getStoredToken } from '../services/authservice'
import { UserContext } from '../App'
import { toast } from 'react-toastify'
import ReceiptModal from '../component/ReceiptModal'

function MyBookings() {
    const navigate = useNavigate()
    const { loginStatus, user } = useContext(UserContext)
    const [bookings, setBookings] = useState([])
    const [loading, setLoading] = useState(true)
    const [selectedReceipt, setSelectedReceipt] = useState(null)

    useEffect(() => {
        if (!loginStatus || !user) {
            navigate('/login')
            return
        }
        loadBookings()
    }, [loginStatus, user])

    const loadBookings = async () => {
        setLoading(true)
        try {
            const token = getStoredToken()
            const response = await getMyBookings(token)
            const data = response.data
            if (data && data.content) {
                setBookings(data.content)
            } else if (Array.isArray(data)) {
                setBookings(data)
            } else {
                setBookings([])
            }
        } catch (error) {
            console.error('Error fetching bookings:', error)
            toast.error('Failed to load bookings')
        } finally {
            setLoading(false)
        }
    }

    const handleCancel = async (bookingId) => {
        if (!window.confirm(`Are you sure you want to cancel booking #${bookingId}?`)) {
            return
        }
        try {
            const token = getStoredToken()
            await cancelBooking(bookingId, token)
            toast.success('Booking cancelled successfully')
            loadBookings()
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to cancel booking')
        }
    }

    const handlePayNow = async (booking) => {
        const token = getStoredToken()
        try {
            const orderRes = await createPaymentOrder(booking.id, token)
            const orderData = orderRes.data

            const key = orderData.razorpayKeyId || orderData.keyId || ''
            const isMock = !key || 
                key.includes('placeholder') || 
                (orderData.razorpayOrderId && orderData.razorpayOrderId.startsWith('order_mock_'))

            if (isMock) {
                // Seamless mock payment verification for development / offline testing
                const mockPaymentId = `pay_mock_${Date.now()}`
                const mockSignature = `sig_mock_${Date.now()}`
                await verifyPayment({
                    orderId: orderData.razorpayOrderId,
                    paymentId: mockPaymentId,
                    signature: mockSignature
                }, token)
                toast.success('Token deposit received! Booking confirmed.')
                loadBookings()
                return
            }

            if (window.Razorpay) {
                const amountInPaise = Math.round(Number(orderData.amount || booking.tokenAmount) * 100)
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
                    description: `Token Deposit for Booking #${booking.id}`,
                    order_id: orderData.razorpayOrderId,
                    handler: async function (response) {
                        try {
                            await verifyPayment({
                                orderId: response.razorpay_order_id,
                                paymentId: response.razorpay_payment_id,
                                signature: response.razorpay_signature
                            }, token)
                            toast.success('Payment verified! Booking confirmed.')
                            loadBookings()
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
                            toast.info('Payment window closed.')
                            loadBookings()
                        }
                    }
                }
                const rzp = new window.Razorpay(options)
                rzp.on('payment.failed', async function (resp) {
                    console.error('Payment failed event:', resp)
                    try {
                        await recordPaymentFailure({
                            orderId: orderData.razorpayOrderId,
                            reason: resp.error?.description || 'Payment failed or declined'
                        }, token)
                    } catch (e) {
                        console.warn('Could not record failure:', e)
                    }
                    toast.error(resp.error?.description || 'Payment failed or declined. You can retry anytime.')
                    loadBookings()
                })
                rzp.open()
            } else {
                toast.info('Razorpay SDK initializing. Please try again.')
            }
        } catch (error) {
            console.error('Payment order creation error:', error)
            toast.error(error.response?.data?.message || 'Failed to initialize payment gateway')
        }
    }

    const getStatusBadge = (status, paymentStatus) => {
        if (paymentStatus === 'FAILED') {
            return (
                <span className="badge bg-danger text-white px-3 py-2 shadow-sm">
                    <i className="bi bi-x-circle me-1"></i> PAYMENT FAILED
                </span>
            )
        }
        switch (status) {
            case 'CONFIRMED':
                return <span className="badge bg-success-subtle text-success border border-success-subtle px-3 py-2 fw-semibold"><i className="bi bi-check-circle me-1"></i>CONFIRMED</span>
            case 'COMPLETED':
                return <span className="badge bg-primary-subtle text-primary border border-primary-subtle px-3 py-2 fw-semibold"><i className="bi bi-patch-check me-1"></i>COMPLETED</span>
            case 'CANCELLED':
                return <span className="badge bg-danger-subtle text-danger border border-danger-subtle px-3 py-2">CANCELLED</span>
            default:
                return <span className="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle px-3 py-2 fw-semibold"><i className="bi bi-clock me-1"></i>PENDING PAYMENT</span>
        }
    }

    return (
        <div className="container py-5 my-2">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h3 className="fw-bold mb-1" style={{ color: '#0B2545' }}>
                        <i className="bi bi-calendar-check text-primary me-2"></i> My Bookings & Token Deposits
                    </h3>
                    <p className="text-muted small mb-0">Track your reserved cars, 2% token payments, and dealership inspection status</p>
                </div>
                <Link to="/cars" className="btn btn-mg-primary btn-sm px-4">
                    Explore More Cars
                </Link>
            </div>

            {loading ? (
                <div className="text-center py-5 bg-white rounded-4 border p-5">
                    <div className="spinner-border text-primary" role="status"></div>
                    <p className="text-muted mt-2">Loading your booking records...</p>
                </div>
            ) : bookings.length > 0 ? (
                <div className="d-flex flex-column gap-3">
                    {bookings.map(item => (
                        <div key={item.id} className="card border-0 shadow-sm rounded-4 overflow-hidden bg-white p-3 p-md-4">
                            <div className="row align-items-center g-3">
                                {/* Thumbnail */}
                                <div className="col-md-3 col-lg-2">
                                    <div className="rounded-3 overflow-hidden bg-light" style={{ height: '100px' }}>
                                        <img 
                                            src={getImageUrl(item.primaryImageUrl || item.carPrimaryImageUrl || item.car?.primaryImageUrl)} 
                                            alt={item.carTitle || 'Car'} 
                                            className="w-100 h-100 object-fit-cover"
                                            onError={(e) => {
                                                e.target.onerror = null
                                                e.target.src = 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80'
                                            }}
                                        />
                                    </div>
                                </div>

                                {/* Car and Showroom Details */}
                                <div className="col-md-5 col-lg-6">
                                    <div className="d-flex align-items-center gap-2 mb-1">
                                        <span className="badge bg-light text-muted border small">Booking #{item.id}</span>
                                        <small className="text-muted">{item.createdOn ? new Date(item.createdOn).toLocaleDateString() : ''}</small>
                                    </div>
                                    <h5 className="fw-bold mb-1 text-truncate" style={{ color: '#0B2545' }}>
                                        {item.carTitle || `Car #${item.carId}`}
                                    </h5>
                                    <p className="text-muted small mb-0">
                                        Showroom: <span className="fw-semibold text-dark">{item.sellerShowroom || 'Certified Dealer'}</span>
                                        {item.sellerPhone && ` • Tel: ${item.sellerPhone}`}
                                    </p>
                                </div>

                                {/* Token Amount & Status */}
                                <div className="col-md-4 col-lg-4 text-md-end">
                                    <div className="mb-2">
                                        <small className="text-muted d-block">2% Token Deposit</small>
                                        <strong className="fs-5 text-primary">₹ {item.tokenAmount ? Number(item.tokenAmount).toLocaleString('en-IN') : '10,000'}</strong>
                                    </div>
                                    <div className="mb-3">
                                        {getStatusBadge(item.status, item.paymentStatus)}
                                    </div>

                                    {/* Action buttons */}
                                    <div className="d-flex justify-content-md-end gap-2 flex-wrap">
                                        {(item.status === 'CONFIRMED' || item.status === 'COMPLETED') && (
                                            <button 
                                                type="button" 
                                                onClick={() => setSelectedReceipt(item)} 
                                                className="btn btn-outline-success btn-sm px-3 fw-semibold shadow-sm"
                                            >
                                                <i className="bi bi-receipt me-1"></i> View Receipt
                                            </button>
                                        )}
                                        {item.status === 'PENDING_PAYMENT' && (
                                            <>
                                                {item.paymentStatus === 'FAILED' ? (
                                                    <button 
                                                        type="button" 
                                                        onClick={() => handlePayNow(item)} 
                                                        className="btn btn-danger btn-sm px-3 shadow-sm fw-semibold"
                                                    >
                                                        <i className="bi bi-arrow-repeat me-1"></i> Retry Payment
                                                    </button>
                                                ) : (
                                                    <button 
                                                        type="button" 
                                                        onClick={() => handlePayNow(item)} 
                                                        className="btn btn-mg-primary btn-sm px-3 shadow-sm fw-semibold"
                                                    >
                                                        <i className="bi bi-credit-card me-1"></i> Pay Token
                                                    </button>
                                                )}
                                                <button 
                                                    type="button" 
                                                    onClick={() => handleCancel(item.id)} 
                                                    className="btn btn-outline-danger btn-sm px-3"
                                                >
                                                    Cancel
                                                </button>
                                            </>
                                        )}
                                        {item.carId && (
                                            <Link to={`/cars/${item.carId}`} className="btn btn-light btn-sm border px-3">
                                                View Car
                                            </Link>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            ) : (
                <div className="text-center py-5 bg-white rounded-4 border p-5 shadow-sm">
                    <i className="bi bi-calendar-x text-muted" style={{ fontSize: '3rem' }}></i>
                    <h5 className="fw-bold mt-3" style={{ color: '#0B2545' }}>No Bookings Yet</h5>
                    <p className="text-muted small">You haven't placed any 2% token bookings yet. Browse our inventory to reserve a car today.</p>
                    <Link to="/cars" className="btn btn-mg-primary mt-2">
                        Explore Cars
                    </Link>
                </div>
            )}

            {/* Official Token Receipt Modal */}
            <ReceiptModal 
                booking={selectedReceipt} 
                onClose={() => setSelectedReceipt(null)} 
            />
        </div>
    )
}

export default MyBookings

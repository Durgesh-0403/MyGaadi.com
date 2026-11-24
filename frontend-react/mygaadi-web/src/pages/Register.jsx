import React, { useContext, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { buyerSignup, sellerSignup, setSessionAuth } from '../services/authservice'
import { toast } from 'react-toastify'
import { UserContext } from '../App'

function Register() {
    const navigate = useNavigate()
    const [searchParams] = useSearchParams()
    const { setLoginStatus, setUser } = useContext(UserContext)

    // Role tab: 'buyer' or 'seller'
    const [role, setRole] = useState(searchParams.get('role') === 'seller' ? 'seller' : 'buyer')

    // Common fields
    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [phone, setPhone] = useState('')

    // Seller Showroom fields
    const [showroomName, setShowroomName] = useState('')
    const [city, setCity] = useState('')
    const [state, setState] = useState('')
    const [showroomAddress, setShowroomAddress] = useState('')
    const [gstNumber, setGstNumber] = useState('')

    const [loading, setLoading] = useState(false)

    const handleRegister = async (e) => {
        e.preventDefault()

        if (!firstName.trim() || !lastName.trim() || !email.trim() || !password.trim() || !phone.trim()) {
            toast.error('Please fill in all required personal details')
            return
        }

        if (phone.trim().length !== 10 || !/^\d+$/.test(phone.trim())) {
            toast.error('Phone number must be a 10-digit number')
            return
        }

        setLoading(true)
        try {
            let response
            if (role === 'buyer') {
                const body = {
                    firstName: firstName.trim(),
                    lastName: lastName.trim(),
                    email: email.trim(),
                    password,
                    phone: phone.trim()
                }
                response = await buyerSignup(body)
            } else {
                if (!showroomName.trim() || !city.trim() || !state.trim() || !showroomAddress.trim()) {
                    toast.error('Please fill in all showroom dealership details')
                    setLoading(false)
                    return
                }
                const body = {
                    firstName: firstName.trim(),
                    lastName: lastName.trim(),
                    email: email.trim(),
                    password,
                    phone: phone.trim(),
                    showroomName: showroomName.trim(),
                    city: city.trim(),
                    state: state.trim(),
                    address: showroomAddress.trim(),
                    gstin: gstNumber.trim() || undefined
                }
                response = await sellerSignup(body)
            }

            const result = response.data
            if (result && result.jwt) {
                toast.success('Registration successful! Welcome to MyGaadi.')
                setSessionAuth(result.jwt, result)
                setLoginStatus(true)
                setUser(result)

                if (result.userRole === 'ROLE_SELLER') {
                    navigate('/seller/dashboard')
                } else {
                    navigate('/cars')
                }
            } else {
                toast.error(result.message || 'Registration failed')
            }
        } catch (error) {
            console.error('Registration error:', error)
            const errMsg = error.response?.data?.message || 'Registration failed. Please check the entered data.'
            toast.error(errMsg)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="container py-5 my-3">
            <div className="row justify-content-center">
                <div className="col-md-9 col-lg-7">
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
                            <h4 className="fw-bold mb-1" style={{ color: '#0B2545' }}>Create Your Account</h4>
                            <p className="small text-muted mb-0">
                                {role === 'buyer' 
                                    ? 'Create your free buyer account to save wishlist & reserve cars' 
                                    : 'Register your certified car dealership or independent showroom'}
                            </p>
                        </div>

                        {/* Role Selector Tabs */}
                        <div className="d-flex border-bottom bg-light">
                            <button
                                type="button"
                                className={`btn flex-fill py-3 rounded-0 fw-semibold border-0 ${role === 'buyer' ? 'bg-white text-primary border-bottom border-primary border-3' : 'text-muted'}`}
                                onClick={() => setRole('buyer')}
                            >
                                <i className="bi bi-person me-1"></i> Car Buyer
                            </button>
                            <button
                                type="button"
                                className={`btn flex-fill py-3 rounded-0 fw-semibold border-0 ${role === 'seller' ? 'bg-white text-primary border-bottom border-primary border-3' : 'text-muted'}`}
                                onClick={() => setRole('seller')}
                            >
                                <i className="bi bi-shop me-1"></i> Dealership / Seller
                            </button>
                        </div>

                        {/* Form Body */}
                        <div className="card-body p-4 p-md-5">
                            <form onSubmit={handleRegister}>
                                <div className="row g-3 mb-3">
                                    <div className="col-md-6">
                                        <label className="form-label small fw-semibold text-secondary">First Name *</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            placeholder="e.g. Rahul"
                                            value={firstName}
                                            onChange={e => setFirstName(e.target.value)}
                                            required
                                        />
                                    </div>
                                    <div className="col-md-6">
                                        <label className="form-label small fw-semibold text-secondary">Last Name *</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            placeholder="e.g. Sharma"
                                            value={lastName}
                                            onChange={e => setLastName(e.target.value)}
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="row g-3 mb-3">
                                    <div className="col-md-6">
                                        <label className="form-label small fw-semibold text-secondary">Email Address *</label>
                                        <input
                                            type="email"
                                            className="form-control"
                                            placeholder="rahul@example.com"
                                            value={email}
                                            onChange={e => setEmail(e.target.value)}
                                            required
                                        />
                                    </div>
                                    <div className="col-md-6">
                                        <label className="form-label small fw-semibold text-secondary">Phone Number (10 digits) *</label>
                                        <input
                                            type="tel"
                                            className="form-control"
                                            placeholder="9876543210"
                                            value={phone}
                                            onChange={e => setPhone(e.target.value)}
                                            maxLength="10"
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="mb-3">
                                    <label className="form-label small fw-semibold text-secondary">Password *</label>
                                    <input
                                        type="password"
                                        className="form-control"
                                        placeholder="Min 6 characters"
                                        value={password}
                                        onChange={e => setPassword(e.target.value)}
                                        required
                                    />
                                </div>

                                {/* Seller Specific Fields */}
                                {role === 'seller' && (
                                    <div className="border rounded-4 p-3 bg-light mb-4">
                                        <h6 className="fw-bold mb-3 text-dark">
                                            <i className="bi bi-shop me-2 text-primary"></i> Showroom & Dealership Details
                                        </h6>

                                        <div className="mb-3">
                                            <label className="form-label small fw-semibold text-secondary">Showroom / Business Name *</label>
                                            <input
                                                type="text"
                                                className="form-control"
                                                placeholder="e.g. Apex Auto Hub"
                                                value={showroomName}
                                                onChange={e => setShowroomName(e.target.value)}
                                                required={role === 'seller'}
                                            />
                                        </div>

                                        <div className="row g-3 mb-3">
                                            <div className="col-md-6">
                                                <label className="form-label small fw-semibold text-secondary">City *</label>
                                                <input
                                                    type="text"
                                                    className="form-control"
                                                    placeholder="e.g. Pune"
                                                    value={city}
                                                    onChange={e => setCity(e.target.value)}
                                                    required={role === 'seller'}
                                                />
                                            </div>
                                            <div className="col-md-6">
                                                <label className="form-label small fw-semibold text-secondary">State *</label>
                                                <input
                                                    type="text"
                                                    className="form-control"
                                                    placeholder="e.g. Maharashtra"
                                                    value={state}
                                                    onChange={e => setState(e.target.value)}
                                                    required={role === 'seller'}
                                                />
                                            </div>
                                        </div>

                                        <div className="mb-3">
                                            <label className="form-label small fw-semibold text-secondary">Full Showroom Address *</label>
                                            <textarea
                                                className="form-control"
                                                rows="2"
                                                placeholder="Plot 42, Senapati Bapat Road, Shivajinagar"
                                                value={showroomAddress}
                                                onChange={e => setShowroomAddress(e.target.value)}
                                                required={role === 'seller'}
                                            ></textarea>
                                        </div>

                                        <div>
                                            <label className="form-label small fw-semibold text-secondary">GST Number (Optional)</label>
                                            <input
                                                type="text"
                                                className="form-control"
                                                placeholder="27ABCDE1234F1Z5"
                                                value={gstNumber}
                                                onChange={e => setGstNumber(e.target.value)}
                                            />
                                        </div>
                                    </div>
                                )}

                                <button
                                    type="submit"
                                    className="btn btn-mg-primary w-100 py-3 fw-bold mb-3 shadow"
                                    disabled={loading}
                                >
                                    {loading ? (
                                        <>
                                            <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                                            Creating Account...
                                        </>
                                    ) : (
                                        role === 'buyer' ? 'Create Buyer Account' : 'Register Dealership'
                                    )}
                                </button>

                                <div className="text-center pt-2 border-top">
                                    <span className="small text-muted">Already registered on MyGaadi? </span>
                                    <Link to="/login" className="small fw-bold text-primary text-decoration-none">
                                        Sign In
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

export default Register

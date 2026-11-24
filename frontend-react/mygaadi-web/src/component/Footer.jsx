import React from 'react'
import { Link } from 'react-router'

function Footer() {
    return (
        <footer style={{ backgroundColor: '#0B2545', color: '#CBD5E1' }} className="pt-5 pb-4 mt-auto">
            <div className="container">
                <div className="row g-4 mb-4">
                    {/* Brand Info */}
                    <div className="col-lg-4 col-md-6">
                        <div className="bg-white px-3 py-1 rounded-3 d-inline-block mb-3 shadow-sm">
                            <img 
                                src="/logo.png" 
                                alt="MyGaadi.com" 
                                style={{ height: '36px', objectFit: 'contain' }} 
                            />
                        </div>
                        <p className="small text-secondary mb-3" style={{ lineHeight: '1.7' }}>
                            India's trusted pre-owned car marketplace connecting genuine buyers with certified local dealerships. Transparent pricing, multi-point certified inspection reports, and 100% refundable token bookings.
                        </p>
                        <div className="d-flex gap-2">
                            <span className="badge rounded-pill text-bg-light px-3 py-2 text-dark font-monospace">
                                <i className="bi bi-shield-check text-success me-1"></i> 100% Refundable Token
                            </span>
                        </div>
                    </div>

                    {/* Marketplace Links */}
                    <div className="col-lg-2 col-md-6">
                        <h6 className="text-white fw-bold mb-3 text-uppercase small" style={{ letterSpacing: '1px' }}>Marketplace</h6>
                        <ul className="list-unstyled small d-flex flex-column gap-2">
                            <li><Link to="/cars" className="text-secondary text-decoration-none hover-white">Buy Cars</Link></li>
                            <li><Link to="/register?role=seller" className="text-secondary text-decoration-none hover-white">Sell Your Car</Link></li>
                            <li><Link to="/wishlist" className="text-secondary text-decoration-none hover-white">My Wishlist</Link></li>
                            <li><Link to="/login" className="text-secondary text-decoration-none hover-white">User Login</Link></li>
                        </ul>
                    </div>

                    {/* Trust & Guarantees */}
                    <div className="col-lg-3 col-md-6">
                        <h6 className="text-white fw-bold mb-3 text-uppercase small" style={{ letterSpacing: '1px' }}>Trust & Protection</h6>
                        <ul className="list-unstyled small d-flex flex-column gap-2 text-secondary">
                            <li><i className="bi bi-check-circle-fill text-primary me-2"></i>Token Deposit: 2% (₹5,000–₹50,000)</li>
                            <li><i className="bi bi-check-circle-fill text-primary me-2"></i>Multi-point Inspection Guarantee</li>
                            <li><i className="bi bi-check-circle-fill text-primary me-2"></i>Secured by Razorpay Gateway</li>
                            <li><i className="bi bi-check-circle-fill text-primary me-2"></i>Direct Dealership RC Transfer</li>
                        </ul>
                    </div>

                    {/* Contact & Cities */}
                    <div className="col-lg-3 col-md-6">
                        <h6 className="text-white fw-bold mb-3 text-uppercase small" style={{ letterSpacing: '1px' }}>Top Hubs</h6>
                        <div className="d-flex flex-wrap gap-1 mb-3">
                            {['Delhi NCR', 'Mumbai', 'Bengaluru', 'Pune', 'Hyderabad', 'Chennai'].map((city, idx) => (
                                <Link 
                                    key={idx} 
                                    to={`/cars?city=${city}`}
                                    className="badge bg-secondary bg-opacity-25 text-white text-decoration-none py-1 px-2 small"
                                >
                                    {city}
                                </Link>
                            ))}
                        </div>
                        <div className="small text-secondary">
                            <div><i className="bi bi-envelope me-2"></i>support@mygaadi.com</div>
                            <div className="mt-1"><i className="bi bi-telephone me-2"></i>+91 (11) 4000-GAADI</div>
                        </div>
                    </div>
                </div>

                <hr className="border-secondary opacity-25 my-4" />

                <div className="d-flex flex-column flex-md-row justify-content-between align-items-center small text-secondary">
                    <div>
                        &copy; {new Date().getFullYear()} MyGaadi.com. Built with Mentor Architecture & Modern Design.
                    </div>
                    <div className="d-flex gap-3 mt-2 mt-md-0">
                        <Link to="/admin/login" className="text-secondary text-decoration-none">
                            <i className="bi bi-lock me-1"></i> Admin Portal
                        </Link>
                    </div>
                </div>
            </div>
        </footer>
    )
}

export default Footer

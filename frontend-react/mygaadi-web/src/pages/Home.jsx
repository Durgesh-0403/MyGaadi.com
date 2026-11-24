import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { getCars } from '../services/carservice'
import CarCard from '../component/CarCard'

function Home() {
    const navigate = useNavigate()
    const [searchKeyword, setSearchKeyword] = useState('')
    const [cars, setCars] = useState([])
    const [loading, setLoading] = useState(true)

    const popularBrands = ['Hyundai', 'Maruti Suzuki', 'Honda', 'Tata', 'Mahindra']

    useEffect(() => {
        loadRecommendedCars()
    }, [])

    const loadRecommendedCars = async () => {
        setLoading(true)
        try {
            const response = await getCars({ size: 8, sortBy: 'id', direction: 'desc' })
            const data = response.data
            // If response is Spring PageResponse, data.content is the list
            if (data && data.content) {
                setCars(data.content)
            } else if (Array.isArray(data)) {
                setCars(data)
            } else {
                setCars([])
            }
        } catch (error) {
            console.error('Failed to load recommended cars:', error)
            setCars([])
        } finally {
            setLoading(false)
        }
    }

    const handleSearchSubmit = (e) => {
        e.preventDefault()
        if (searchKeyword.trim()) {
            navigate(`/cars?q=${encodeURIComponent(searchKeyword.trim())}`)
        } else {
            navigate('/cars')
        }
    }

    const handleBrandClick = (brand) => {
        navigate(`/cars?brand=${encodeURIComponent(brand)}`)
    }

    return (
        <div>
            {/* ================= HERO SECTION (Exact Screenshot 1 & 2) ================= */}
            <div className="hero-gradient">
                <div className="container">
                    <div className="row align-items-center g-5">
                        {/* Left Hero Column */}
                        <div className="col-lg-7">
                            <span 
                                className="badge text-uppercase px-3 py-2 mb-3"
                                style={{ 
                                    background: 'rgba(255, 255, 255, 0.1)', 
                                    letterSpacing: '1.5px', 
                                    fontSize: '0.75rem', 
                                    color: '#93C5FD',
                                    borderRadius: '50px' 
                                }}
                            >
                                TRUSTED PRE-OWNED MARKETPLACE
                            </span>

                            <h1 className="display-4 fw-bold text-white mb-3" style={{ lineHeight: '1.15', letterSpacing: '-1px' }}>
                                Find verified pre-owned cars with confidence.
                            </h1>

                            <p className="lead text-light opacity-75 mb-4" style={{ fontSize: '1.05rem', lineHeight: '1.6' }}>
                                Compare verified listings, review complete vehicle details and manage bookings securely.
                            </p>

                            {/* White Pill Search Box */}
                            <form onSubmit={handleSearchSubmit} className="mb-4">
                                <div 
                                    className="d-flex align-items-center p-2 bg-white rounded-pill shadow-lg"
                                    style={{ maxWidth: '540px' }}
                                >
                                    <span className="ps-3 pe-2 text-muted">
                                        <i className="bi bi-search fs-5"></i>
                                    </span>
                                    <input
                                        type="text"
                                        className="form-control border-0 bg-transparent shadow-none"
                                        placeholder="Search by car, brand or city"
                                        value={searchKeyword}
                                        onChange={(e) => setSearchKeyword(e.target.value)}
                                        style={{ fontSize: '0.95rem' }}
                                    />
                                    <button 
                                        type="submit" 
                                        className="btn btn-mg-primary rounded-pill px-4 py-2 text-nowrap fw-semibold"
                                    >
                                        Search cars
                                    </button>
                                </div>
                            </form>

                            {/* Quick Brand Pills */}
                            <div className="d-flex flex-wrap align-items-center">
                                {popularBrands.map((brand, idx) => (
                                    <button
                                        key={idx}
                                        type="button"
                                        className="brand-chip"
                                        onClick={() => handleBrandClick(brand)}
                                    >
                                        {brand}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* Right Hero Glassmorphic Card (Screenshot 1) */}
                        <div className="col-lg-5">
                            <div className="glass-card shadow-lg">
                                <div className="d-flex align-items-center gap-2 mb-3">
                                    <span className="p-2 rounded-circle" style={{ background: 'rgba(255, 255, 255, 0.15)' }}>
                                        <i className="bi bi-shield-check text-info fs-5"></i>
                                    </span>
                                    <h4 className="fw-bold mb-0 text-white">Buy with confidence</h4>
                                </div>

                                <div className="d-flex flex-column gap-4 mt-4">
                                    <div className="d-flex gap-3">
                                        <span className="text-info fs-5 mt-1"><i className="bi bi-check2-circle"></i></span>
                                        <div>
                                            <h6 className="fw-bold text-white mb-1">Verified listings</h6>
                                            <p className="small text-light opacity-75 mb-0">Seller identity and listing details are thoroughly reviewed.</p>
                                        </div>
                                    </div>

                                    <div className="d-flex gap-3">
                                        <span className="text-warning fs-5 mt-1"><i className="bi bi-shield-lock"></i></span>
                                        <div>
                                            <h6 className="fw-bold text-white mb-1">Secure bookings</h6>
                                            <p className="small text-light opacity-75 mb-0">Booking and payment status remain visible throughout the transaction.</p>
                                        </div>
                                    </div>

                                    <div className="d-flex gap-3">
                                        <span className="text-success fs-5 mt-1"><i className="bi bi-patch-check"></i></span>
                                        <div>
                                            <h6 className="fw-bold text-white mb-1">Transparent information</h6>
                                            <p className="small text-light opacity-75 mb-0">Review vehicle specifications, seller verification and booking status in one place.</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* ================= RECOMMENDED CARS SECTION (Exact Screenshot 2) ================= */}
            <div className="container py-5 my-3">
                <div className="d-flex justify-content-between align-items-end mb-4">
                    <div>
                        <span className="text-primary fw-bold small text-uppercase" style={{ letterSpacing: '1px' }}>
                            FEATURED INVENTORY
                        </span>
                        <h2 className="fw-bold mb-0 mt-1" style={{ color: '#0B2545' }}>
                            Recommended cars
                        </h2>
                    </div>
                    <Link to="/cars" className="btn btn-link text-primary fw-semibold text-decoration-none">
                        Browse all &rarr;
                    </Link>
                </div>

                {loading ? (
                    <div className="text-center py-5">
                        <div className="spinner-border text-primary" role="status"></div>
                        <p className="text-muted mt-2">Loading verified cars...</p>
                    </div>
                ) : cars.length > 0 ? (
                    <div className="row row-cols-1 row-cols-sm-2 row-cols-lg-4 g-4">
                        {cars.map(car => (
                            <div key={car.id} className="col">
                                <CarCard car={car} />
                            </div>
                        ))}
                    </div>
                ) : (
                    /* Fallback Mock Showcase if database is completely fresh */
                    <div className="text-center py-5 bg-white rounded-4 border p-4 shadow-sm">
                        <i className="bi bi-car-front text-muted" style={{ fontSize: '3rem' }}></i>
                        <h5 className="fw-bold mt-3" style={{ color: '#0B2545' }}>No cars listed yet</h5>
                        <p className="text-muted small">Be the first to list a car or explore our marketplace catalog.</p>
                        <Link to="/register?role=seller" className="btn btn-mg-primary mt-2">
                            List Your Car as Seller
                        </Link>
                    </div>
                )}
            </div>
        </div>
    )
}

export default Home

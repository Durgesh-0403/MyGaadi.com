import React, { useContext, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { getWishlist } from '../services/buyerservice'
import { getStoredToken } from '../services/authservice'
import { UserContext } from '../App'
import { useDispatch } from 'react-redux'
import { setWishlistAction } from '../slices/WishlistSlice'
import CarCard from '../component/CarCard'

function Wishlist() {
    const navigate = useNavigate()
    const dispatch = useDispatch()
    const { loginStatus, user } = useContext(UserContext)
    const [cars, setCars] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        if (!loginStatus || !user) {
            navigate('/login')
            return
        }
        loadWishlist()
    }, [loginStatus, user])

    const loadWishlist = async () => {
        setLoading(true)
        try {
            const token = getStoredToken()
            const response = await getWishlist(token)
            const list = response.data || []
            setCars(list)
            dispatch(setWishlistAction(list))
        } catch (error) {
            console.error('Error loading wishlist:', error)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="container py-5 my-2">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h3 className="fw-bold mb-1" style={{ color: '#0B2545' }}>
                        <i className="bi bi-heart-fill text-danger me-2"></i> My Saved Cars
                    </h3>
                    <p className="text-muted small mb-0">Cars you have bookmarked for comparison and booking</p>
                </div>
                <Link to="/cars" className="btn btn-mg-primary btn-sm px-4">
                    Explore More Cars
                </Link>
            </div>

            {loading ? (
                <div className="text-center py-5 bg-white rounded-4 border p-5">
                    <div className="spinner-border text-primary" role="status"></div>
                    <p className="text-muted mt-2">Loading your saved cars...</p>
                </div>
            ) : cars.length > 0 ? (
                <div className="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-4">
                    {cars.map(car => (
                        <div key={car.id} className="col">
                            <CarCard car={car} />
                        </div>
                    ))}
                </div>
            ) : (
                <div className="text-center py-5 bg-white rounded-4 border p-5 shadow-sm">
                    <i className="bi bi-heart text-muted" style={{ fontSize: '3rem' }}></i>
                    <h5 className="fw-bold mt-3" style={{ color: '#0B2545' }}>Your Wishlist is Empty</h5>
                    <p className="text-muted small">Explore our certified pre-owned car listings and tap the heart icon to save favorites.</p>
                    <Link to="/cars" className="btn btn-mg-primary mt-2">
                        Browse Marketplace Cars
                    </Link>
                </div>
            )}
        </div>
    )
}

export default Wishlist

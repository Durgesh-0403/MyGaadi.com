import React, { useContext } from 'react'
import { Link, useNavigate } from 'react-router'
import { formatPrice, getImageUrl } from '../services/carservice'
import { addToWishlist, removeFromWishlist } from '../services/buyerservice'
import { getStoredToken } from '../services/authservice'
import { useDispatch, useSelector } from 'react-redux'
import { addToWishlistAction, removeFromWishlistAction } from '../slices/WishlistSlice'
import { toast } from 'react-toastify'
import { UserContext } from '../App'

function CarCard({ car }) {
    const navigate = useNavigate()
    const dispatch = useDispatch()
    const { loginStatus, user } = useContext(UserContext)
    const wishlistItems = useSelector(state => state.wishlist.items)
    const isWishlisted = wishlistItems.some(item => item.id === car.id)

    const handleWishlistToggle = async (e) => {
        e.preventDefault()
        e.stopPropagation()

        if (!loginStatus || !user) {
            toast.info('Please sign in as a buyer to save cars to your wishlist')
            navigate('/login')
            return
        }

        const token = getStoredToken()
        try {
            if (isWishlisted) {
                await removeFromWishlist(car.id, token)
                dispatch(removeFromWishlistAction(car.id))
                toast.success('Removed from wishlist')
            } else {
                await addToWishlist(car.id, token)
                dispatch(addToWishlistAction(car))
                toast.success('Added to wishlist')
            }
        } catch (error) {
            console.error('Wishlist error:', error)
            toast.error(error.response?.data?.message || 'Failed to update wishlist')
        }
    }

    return (
        <div className="car-card">
            <div className="car-card-img-wrapper">
                <img 
                    src={getImageUrl(car.primaryImageUrl)} 
                    alt={`${car.brand} ${car.model}`}
                    className="car-card-img"
                    onError={(e) => {
                        e.target.onerror = null
                        e.target.src = 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=800&q=80'
                    }}
                />
                
                {car.sellerVerified && (
                    <span className="car-card-badge">
                        <i className="bi bi-patch-check-fill text-info me-1"></i> VERIFIED
                    </span>
                )}

                {car.status === 'SOLD' && (
                    <span 
                        className="position-absolute top-0 start-0 m-2 badge bg-danger px-3 py-1 shadow-sm fw-bold"
                        style={{ zIndex: 2, fontSize: '0.75rem' }}
                    >
                        <i className="bi bi-tag-fill me-1"></i> SOLD
                    </span>
                )}

                <button 
                    type="button"
                    className={`car-card-wishlist ${isWishlisted ? 'active' : ''}`}
                    onClick={handleWishlistToggle}
                    title={isWishlisted ? 'Remove from Wishlist' : 'Add to Wishlist'}
                >
                    <i className={`bi ${isWishlisted ? 'bi-heart-fill' : 'bi-heart'}`}></i>
                </button>
            </div>

            <div className="p-3 d-flex flex-column flex-grow-1">
                <div className="d-flex align-items-baseline gap-2 mb-1">
                    <span className="badge bg-light text-dark border small fw-bold">
                        {car.manufactureYear || '2021'}
                    </span>
                    <span className="text-muted small text-truncate">
                        {car.city || 'India'}
                    </span>
                </div>

                <h6 className="fw-bold mb-2 text-truncate" title={`${car.brand} ${car.model} ${car.variant || ''}`}>
                    {car.brand} {car.model} <span className="text-muted fw-normal small">{car.variant}</span>
                </h6>

                {/* Specs Chips */}
                <div className="d-flex flex-wrap gap-2 text-muted small mb-3">
                    <span className="badge bg-light text-secondary border-0 px-2 py-1">
                        <i className="bi bi-speedometer2 me-1"></i>
                        {car.kmDriven ? car.kmDriven.toLocaleString('en-IN') + ' km' : '35,000 km'}
                    </span>
                    <span className="badge bg-light text-secondary border-0 px-2 py-1">
                        <i className="bi bi-fuel-pump me-1"></i>
                        {car.fuelType || 'Petrol'}
                    </span>
                    <span className="badge bg-light text-secondary border-0 px-2 py-1">
                        <i className="bi bi-gear me-1"></i>
                        {car.transmission || 'Manual'}
                    </span>
                </div>

                <div className="mt-auto pt-2 border-top d-flex align-items-center justify-content-between">
                    <div>
                        <small className="text-muted d-block" style={{ fontSize: '0.72rem' }}>OFFER PRICE</small>
                        <div className="d-flex align-items-center gap-2">
                            <span className="fw-bold fs-5 text-dark">
                                {formatPrice(car.price)}
                            </span>
                            {car.status === 'SOLD' && (
                                <span className="badge bg-danger small px-2 py-1">SOLD</span>
                            )}
                        </div>
                    </div>

                    <Link to={`/cars/${car.id}`} className={`btn py-1 px-3 small ${car.status === 'SOLD' ? 'btn-outline-secondary' : 'btn-mg-primary'}`}>
                        {car.status === 'SOLD' ? 'View (Sold)' : 'View Details'}
                    </Link>
                </div>
            </div>
        </div>
    )
}

export default CarCard

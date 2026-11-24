import React, { useEffect, useState } from 'react'
import { useLocation } from 'react-router'
import { getCars } from '../services/carservice'
import CarCard from '../component/CarCard'
import CarFilters from '../component/CarFilters'

function BrowseCars() {
    const location = useLocation()
    const [cars, setCars] = useState([])
    const [totalResults, setTotalResults] = useState(0)
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(1)
    const [loading, setLoading] = useState(false)

    // Filters state
    const [filters, setFilters] = useState({
        q: '',
        brand: '',
        minPrice: '',
        maxPrice: '',
        fuelType: '',
        transmission: '',
        city: ''
    })

    // Parse URL search params on mount or when location.search changes
    useEffect(() => {
        const queryParams = new URLSearchParams(location.search)
        const initialFilters = {
            q: queryParams.get('q') || '',
            brand: queryParams.get('brand') || '',
            minPrice: queryParams.get('minPrice') || '',
            maxPrice: queryParams.get('maxPrice') || '',
            fuelType: queryParams.get('fuelType') || '',
            transmission: queryParams.get('transmission') || '',
            city: queryParams.get('city') || ''
        }
        setFilters(initialFilters)
        fetchCars(0, initialFilters)
    }, [location.search])

    const fetchCars = async (pageNumber = 0, currentFilters = filters) => {
        setLoading(true)
        try {
            const params = {
                page: pageNumber,
                size: 12
            }
            if (currentFilters.q) params.q = currentFilters.q
            if (currentFilters.brand) params.brand = currentFilters.brand
            if (currentFilters.minPrice) params.minPrice = currentFilters.minPrice
            if (currentFilters.maxPrice) params.maxPrice = currentFilters.maxPrice
            if (currentFilters.fuelType) params.fuelType = currentFilters.fuelType
            if (currentFilters.transmission) params.transmission = currentFilters.transmission
            if (currentFilters.city) params.city = currentFilters.city

            const response = await getCars(params)
            const data = response.data
            if (data && data.content) {
                setCars(data.content)
                setTotalResults(data.totalElements || data.content.length)
                setTotalPages(data.totalPages || 1)
                setPage(data.pageNumber || 0)
            } else if (Array.isArray(data)) {
                setCars(data)
                setTotalResults(data.length)
                setTotalPages(1)
                setPage(0)
            } else {
                setCars([])
                setTotalResults(0)
            }
        } catch (error) {
            console.error('Error fetching cars:', error)
            setCars([])
            setTotalResults(0)
        } finally {
            setLoading(false)
        }
    }

    const handleApplyFilters = () => {
        setPage(0)
        fetchCars(0, filters)
    }

    const handleResetFilters = () => {
        const resetState = {
            q: '',
            brand: '',
            minPrice: '',
            maxPrice: '',
            fuelType: '',
            transmission: '',
            city: ''
        }
        setFilters(resetState)
        setPage(0)
        fetchCars(0, resetState)
    }

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < totalPages) {
            setPage(newPage)
            fetchCars(newPage, filters)
            window.scrollTo({ top: 0, behavior: 'smooth' })
        }
    }

    return (
        <div className="container py-4 my-2">
            <div className="row g-4">
                {/* Left Column: Filter Card (Screenshot 3) */}
                <div className="col-lg-3">
                    <CarFilters
                        filters={filters}
                        setFilters={setFilters}
                        onApplyFilters={handleApplyFilters}
                        onResetFilters={handleResetFilters}
                    />
                </div>

                {/* Right Column: Browse Results */}
                <div className="col-lg-9">
                    <div className="d-flex justify-content-between align-items-center mb-3">
                        <div>
                            <h3 className="fw-bold mb-0" style={{ color: '#0B2545' }}>Browse Cars</h3>
                            <small className="text-muted">{totalResults} results found</small>
                        </div>
                    </div>

                    {loading ? (
                        <div className="text-center py-5 bg-white rounded-4 border p-5">
                            <div className="spinner-border text-primary" role="status"></div>
                            <p className="text-muted mt-2">Loading marketplace inventory...</p>
                        </div>
                    ) : cars.length > 0 ? (
                        <>
                            <div className="row row-cols-1 row-cols-md-2 row-cols-xl-3 g-4">
                                {cars.map(car => (
                                    <div key={car.id} className="col">
                                        <CarCard car={car} />
                                    </div>
                                ))}
                            </div>

                            {/* Pagination Controls */}
                            {totalPages > 1 && (
                                <div className="d-flex justify-content-center align-items-center gap-2 mt-5">
                                    <button
                                        className="btn btn-outline-secondary btn-sm rounded-pill px-3"
                                        onClick={() => handlePageChange(page - 1)}
                                        disabled={page === 0}
                                    >
                                        &larr; Previous
                                    </button>
                                    <span className="small text-muted px-2">
                                        Page {page + 1} of {totalPages}
                                    </span>
                                    <button
                                        className="btn btn-outline-secondary btn-sm rounded-pill px-3"
                                        onClick={() => handlePageChange(page + 1)}
                                        disabled={page >= totalPages - 1}
                                    >
                                        Next &rarr;
                                    </button>
                                </div>
                            )}
                        </>
                    ) : (
                        <div className="text-center py-5 bg-white rounded-4 border p-5 shadow-sm">
                            <i className="bi bi-search text-muted" style={{ fontSize: '2.5rem' }}></i>
                            <h5 className="fw-bold mt-3">No cars match your criteria</h5>
                            <p className="text-muted small">Try adjusting your filters or resetting the price and brand.</p>
                            <button onClick={handleResetFilters} className="btn btn-mg-primary btn-sm px-4">
                                Reset Filters
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

export default BrowseCars

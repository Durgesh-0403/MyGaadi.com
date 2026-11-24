import React from 'react'

function CarFilters({ filters, setFilters, onApplyFilters, onResetFilters }) {
    const handleChange = (field, value) => {
        setFilters(prev => ({
            ...prev,
            [field]: value
        }))
    }

    const handleSubmit = (e) => {
        e.preventDefault()
        onApplyFilters()
    }

    return (
        <div className="card border-0 shadow-sm" style={{ borderRadius: '16px', background: '#FFFFFF' }}>
            <div className="card-body p-4">
                <div className="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                    <h5 className="fw-bold mb-0 d-flex align-items-center gap-2" style={{ color: '#0B2545' }}>
                        <i className="bi bi-sliders"></i> Filters
                    </h5>
                    <button 
                        type="button" 
                        onClick={onResetFilters} 
                        className="btn btn-sm btn-link text-muted text-decoration-none p-0"
                    >
                        Reset
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="d-flex flex-column gap-3">
                    {/* Search Keyword */}
                    <div>
                        <label className="form-label small fw-semibold text-secondary mb-1">Search Keyword</label>
                        <div className="input-group">
                            <span className="input-group-text bg-white border-end-0 text-muted" style={{ borderRadius: '10px 0 0 10px' }}>
                                <i className="bi bi-search small"></i>
                            </span>
                            <input
                                type="text"
                                className="form-control border-start-0"
                                placeholder="Search keyword..."
                                value={filters.q || ''}
                                onChange={(e) => handleChange('q', e.target.value)}
                                style={{ borderRadius: '0 10px 10px 0', fontSize: '0.9rem' }}
                            />
                        </div>
                    </div>

                    {/* Brand */}
                    <div>
                        <label className="form-label small fw-semibold text-secondary mb-1">Brand</label>
                        <select
                            className="form-select"
                            value={filters.brand || ''}
                            onChange={(e) => handleChange('brand', e.target.value)}
                            style={{ borderRadius: '10px', fontSize: '0.9rem' }}
                        >
                            <option value="">All Brands</option>
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

                    {/* Price Range: Min & Max */}
                    <div>
                        <label className="form-label small fw-semibold text-secondary mb-1">Price Range (₹)</label>
                        <div className="d-flex gap-2">
                            <input
                                type="number"
                                className="form-control"
                                placeholder="Min ₹"
                                value={filters.minPrice || ''}
                                onChange={(e) => handleChange('minPrice', e.target.value)}
                                style={{ borderRadius: '10px', fontSize: '0.9rem' }}
                            />
                            <input
                                type="number"
                                className="form-control"
                                placeholder="Max ₹"
                                value={filters.maxPrice || ''}
                                onChange={(e) => handleChange('maxPrice', e.target.value)}
                                style={{ borderRadius: '10px', fontSize: '0.9rem' }}
                            />
                        </div>
                    </div>

                    {/* Fuel Type */}
                    <div>
                        <label className="form-label small fw-semibold text-secondary mb-1">Fuel Type</label>
                        <select
                            className="form-select"
                            value={filters.fuelType || ''}
                            onChange={(e) => handleChange('fuelType', e.target.value)}
                            style={{ borderRadius: '10px', fontSize: '0.9rem' }}
                        >
                            <option value="">All Fuels</option>
                            <option value="PETROL">Petrol</option>
                            <option value="DIESEL">Diesel</option>
                            <option value="CNG">CNG</option>
                            <option value="ELECTRIC">Electric</option>
                            <option value="HYBRID">Hybrid</option>
                        </select>
                    </div>

                    {/* Transmission */}
                    <div>
                        <label className="form-label small fw-semibold text-secondary mb-1">Transmission</label>
                        <select
                            className="form-select"
                            value={filters.transmission || ''}
                            onChange={(e) => handleChange('transmission', e.target.value)}
                            style={{ borderRadius: '10px', fontSize: '0.9rem' }}
                        >
                            <option value="">All Transmissions</option>
                            <option value="MANUAL">Manual</option>
                            <option value="AUTOMATIC">Automatic</option>
                        </select>
                    </div>

                    {/* City */}
                    <div>
                        <label className="form-label small fw-semibold text-secondary mb-1">City</label>
                        <input
                            type="text"
                            className="form-control"
                            placeholder="e.g. Pune, Mumbai, Delhi"
                            value={filters.city || ''}
                            onChange={(e) => handleChange('city', e.target.value)}
                            style={{ borderRadius: '10px', fontSize: '0.9rem' }}
                        />
                    </div>

                    {/* Submit Button */}
                    <button 
                        type="submit" 
                        className="btn btn-mg-primary w-100 mt-2 py-2 fw-semibold"
                        style={{ borderRadius: '50px' }}
                    >
                        Apply Filters
                    </button>
                </form>
            </div>
        </div>
    )
}

export default CarFilters

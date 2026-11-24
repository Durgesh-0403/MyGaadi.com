import React from 'react'

function ReceiptModal({ booking, onClose }) {
    if (!booking) return null

    const tokenAmountNum = Number(booking.tokenAmount || 0)
    const carPriceNum = Number(booking.carPrice || 0)
    const balanceNum = Math.max(0, carPriceNum - tokenAmountNum)

    const receiptNumber = `MG-REC-${booking.id}-${booking.bookingNumber || 'BKG'}`
    const dateFormatted = booking.createdOn 
        ? new Date(booking.createdOn).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
        : new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })

    const handlePrint = () => {
        window.print()
    }

    return (
        <div 
            className="modal fade show d-block" 
            tabIndex="-1" 
            style={{ backgroundColor: 'rgba(11, 37, 69, 0.65)', zIndex: 1070 }}
            onClick={(e) => {
                if (e.target === e.currentTarget) onClose()
            }}
        >
            <div className="modal-dialog modal-dialog-centered modal-lg">
                <div className="modal-content border-0 shadow-lg rounded-4 overflow-hidden" id="printable-receipt">
                    {/* Top Action Bar (Hidden in Print) */}
                    <div className="d-flex justify-content-between align-items-center px-4 py-3 bg-light border-bottom d-print-none">
                        <div className="d-flex align-items-center gap-2">
                            <span className="badge bg-success-subtle text-success border border-success px-3 py-1">
                                <i className="bi bi-shield-check me-1"></i>Official Token Receipt
                            </span>
                            <small className="text-muted">Receipt #{receiptNumber}</small>
                        </div>
                        <div className="d-flex align-items-center gap-2">
                            <button 
                                type="button" 
                                onClick={handlePrint}
                                className="btn btn-sm btn-primary d-flex align-items-center gap-1 shadow-sm"
                                style={{ backgroundColor: '#0B2545', borderColor: '#0B2545' }}
                            >
                                <i className="bi bi-printer"></i> Print / Save PDF
                            </button>
                            <button 
                                type="button" 
                                onClick={onClose} 
                                className="btn-close"
                                aria-label="Close"
                            ></button>
                        </div>
                    </div>

                    {/* Receipt Document Body */}
                    <div className="modal-body p-4 p-md-5 bg-white">
                        {/* Receipt Header */}
                        <div className="d-flex justify-content-between align-items-start border-bottom pb-4 mb-4">
                            <div>
                                <img 
                                    src="/logo.png" 
                                    alt="MyGaadi.com" 
                                    style={{ height: '44px', objectFit: 'contain' }} 
                                    className="mb-2"
                                />
                                <div className="small text-muted">India's Trusted Pre-Owned Car Marketplace</div>
                                <div className="small text-muted">Support: support@mygaadi.com • www.mygaadi.com</div>
                            </div>
                            <div className="text-end">
                                <h5 className="fw-bold text-uppercase mb-1" style={{ color: '#0B2545', letterSpacing: '0.5px' }}>
                                    Token Booking Receipt
                                </h5>
                                <div className="small text-muted"><strong>Receipt No:</strong> {receiptNumber}</div>
                                <div className="small text-muted"><strong>Booking Ref:</strong> {booking.bookingNumber || `BKG-${booking.id}`}</div>
                                <div className="small text-muted"><strong>Date:</strong> {dateFormatted}</div>
                                <div className="mt-2">
                                    <span className="badge bg-success text-white px-3 py-1">
                                        <i className="bi bi-check-circle-fill me-1"></i>
                                        {booking.paymentStatus === 'PAID' || booking.status === 'CONFIRMED' || booking.status === 'COMPLETED' ? 'PAYMENT CONFIRMED' : 'PENDING'}
                                    </span>
                                </div>
                            </div>
                        </div>

                        {/* Parties: Buyer & Dealership */}
                        <div className="row g-4 mb-4">
                            <div className="col-md-6">
                                <div className="p-3 rounded-3 bg-light border h-100">
                                    <small className="text-muted text-uppercase fw-semibold d-block mb-1" style={{ fontSize: '0.75rem' }}>
                                        Reserved For (Buyer)
                                    </small>
                                    <h6 className="fw-bold mb-1 text-dark">{booking.buyerName || 'Registered Buyer'}</h6>
                                    {booking.buyerEmail && <div className="small text-muted"><i className="bi bi-envelope me-1"></i>{booking.buyerEmail}</div>}
                                    {booking.buyerPhone && <div className="small text-muted"><i className="bi bi-telephone me-1"></i>{booking.buyerPhone}</div>}
                                </div>
                            </div>
                            <div className="col-md-6">
                                <div className="p-3 rounded-3 bg-light border h-100">
                                    <small className="text-muted text-uppercase fw-semibold d-block mb-1" style={{ fontSize: '0.75rem' }}>
                                        Dealership Showroom
                                    </small>
                                    <h6 className="fw-bold mb-1 text-dark">{booking.sellerShowroom || 'Certified Dealership Hub'}</h6>
                                    {booking.sellerName && <div className="small text-muted"><i className="bi bi-person me-1"></i>Dealer: {booking.sellerName}</div>}
                                    <div className="small text-success"><i className="bi bi-patch-check-fill me-1"></i>Verified Showroom Partner</div>
                                </div>
                            </div>
                        </div>

                        {/* Vehicle Information */}
                        <div className="p-3 rounded-3 border mb-4" style={{ backgroundColor: '#F8FAFC' }}>
                            <div className="row align-items-center">
                                <div className="col-md-8">
                                    <small className="text-muted text-uppercase fw-semibold" style={{ fontSize: '0.75rem' }}>Vehicle Reserved</small>
                                    <h5 className="fw-bold mb-1" style={{ color: '#0B2545' }}>
                                        {booking.carTitle || `${booking.carBrand || 'Verified'} ${booking.carModel || 'Car'}`}
                                    </h5>
                                    <div className="small text-muted">
                                        Car ID: #{booking.carId} • Brand: {booking.carBrand || 'Certified'} • Model: {booking.carModel || 'Pre-Owned'}
                                    </div>
                                </div>
                                <div className="col-md-4 text-md-end mt-2 mt-md-0">
                                    <small className="text-muted d-block">Agreed Vehicle Price</small>
                                    <strong className="fs-5 text-dark">
                                        ₹ {carPriceNum > 0 ? carPriceNum.toLocaleString('en-IN') : '--'}
                                    </strong>
                                </div>
                            </div>
                        </div>

                        {/* Financial Breakdown Table */}
                        <div className="table-responsive mb-4">
                            <table className="table table-bordered mb-0">
                                <thead className="table-light">
                                    <tr>
                                        <th>Description</th>
                                        <th className="text-center" style={{ width: '120px' }}>Rate</th>
                                        <th className="text-end" style={{ width: '160px' }}>Amount (INR)</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td>
                                            <div className="fw-semibold">2% Refundable Token Reservation Deposit</div>
                                            <small className="text-muted">
                                                Secures exclusive right to inspect vehicle before final purchase contract
                                            </small>
                                        </td>
                                        <td className="text-center align-middle">2.0%</td>
                                        <td className="text-end align-middle fw-bold">
                                            ₹ {tokenAmountNum.toLocaleString('en-IN')}
                                        </td>
                                    </tr>
                                    <tr className="table-light">
                                        <td colSpan="2" className="text-end fw-bold">Total Token Deposit Paid:</td>
                                        <td className="text-end fw-bold text-success fs-6">
                                            ₹ {tokenAmountNum.toLocaleString('en-IN')}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td colSpan="2" className="text-end text-muted small">
                                            Balance Payable at Showroom on Handover:
                                        </td>
                                        <td className="text-end fw-semibold text-secondary">
                                            ₹ {balanceNum.toLocaleString('en-IN')}
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>

                        {/* Refund Policy & Seal */}
                        <div className="p-3 rounded-3 bg-light border d-flex align-items-center justify-content-between mb-2">
                            <div className="small text-muted me-3" style={{ fontSize: '0.8rem', lineHeight: '1.4' }}>
                                <i className="bi bi-info-circle-fill text-primary me-1"></i>
                                <strong>100% Refund Guarantee:</strong> If the vehicle inspection at the dealership showroom reveals undisclosed mechanical defects or does not match the 140-point certification report, this token deposit is refunded back to the original source.
                            </div>
                            <div className="text-center flex-shrink-0 d-none d-sm-block">
                                <div className="border border-success rounded-circle p-2 d-inline-flex flex-column align-items-center justify-content-center" style={{ width: '85px', height: '85px', borderColor: '#198754' }}>
                                    <i className="bi bi-shield-check text-success fs-4"></i>
                                    <span style={{ fontSize: '0.6rem', fontWeight: 'bold', color: '#198754', textTransform: 'uppercase' }}>Verified</span>
                                </div>
                            </div>
                        </div>

                        <div className="text-center text-muted mt-3" style={{ fontSize: '0.72rem' }}>
                            This is a computer-generated official receipt for MyGaadi Marketplace Token Reservation. No physical signature is required.
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default ReceiptModal

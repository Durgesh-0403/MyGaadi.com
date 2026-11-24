package com.mygaadi.service;

import com.mygaadi.dtos.BookingDTO;
import com.mygaadi.dtos.InternalBookingDTO;
import com.mygaadi.dtos.PageResponse;
import com.mygaadi.entities.PaymentStatus;

public interface BookingService {

	BookingDTO createBooking(Long buyerId, Long carId);

	PageResponse<BookingDTO> getBuyerBookings(Long buyerId, int page, int size);

	BookingDTO getBuyerBookingById(Long buyerId, Long bookingId);

	void cancelBooking(Long buyerId, Long bookingId);

	PageResponse<BookingDTO> getSellerBookings(Long sellerId, int page, int size);

	void completeBooking(Long sellerId, Long bookingId);

	PageResponse<BookingDTO> getAdminBookings(int page, int size);

	InternalBookingDTO getInternalBooking(Long bookingId);

	void updatePaymentStatus(Long bookingId, PaymentStatus paymentStatus, String razorpayPaymentId);

	void expirePendingBookings();
}

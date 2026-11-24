package com.mygaadi.service;

import java.util.List;

import com.mygaadi.dtos.CarDTO;

public interface WishlistService {

	List<CarDTO> getBuyerWishlist(Long buyerId);

	void addToWishlist(Long buyerId, Long carId);

	void removeFromWishlist(Long buyerId, Long carId);
}

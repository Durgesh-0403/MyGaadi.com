package com.mygaadi.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mygaadi.custom_exceptions.ResourceNotFoundException;
import com.mygaadi.dtos.CarDTO;
import com.mygaadi.entities.Car;
import com.mygaadi.entities.User;
import com.mygaadi.entities.WishlistItem;
import com.mygaadi.repository.CarRepository;
import com.mygaadi.repository.UserRepository;
import com.mygaadi.repository.WishlistItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

	private final WishlistItemRepository wishlistRepository;
	private final UserRepository userRepository;
	private final CarRepository carRepository;
	private final CarService carService;

	@Override
	public List<CarDTO> getBuyerWishlist(Long buyerId) {
		List<WishlistItem> items = wishlistRepository.findActiveWishlistByUserId(buyerId);
		return items.stream()
				.map(item -> carService.getCarById(item.getCar().getId()))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void addToWishlist(Long buyerId, Long carId) {
		User buyer = userRepository.findById(buyerId)
				.orElseThrow(() -> new ResourceNotFoundException("Buyer not found with id: " + buyerId));

		Car car = carRepository.findByIdAndDeletedFalse(carId)
				.orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + carId));

		Optional<WishlistItem> existingItem = wishlistRepository.findByUserIdAndCarId(buyerId, carId);

		if (existingItem.isPresent()) {
			WishlistItem item = existingItem.get();
			if (item.isDeleted()) {
				item.setDeleted(false);
				item.setDeletedAt(null);
				wishlistRepository.save(item);
				log.info("Restored car {} in buyer {}'s wishlist", carId, buyerId);
			}
		} else {
			WishlistItem newItem = WishlistItem.builder()
					.user(buyer)
					.car(car)
					.deleted(false)
					.build();
			wishlistRepository.save(newItem);
			log.info("Added car {} to buyer {}'s wishlist", carId, buyerId);
		}
	}

	@Override
	@Transactional
	public void removeFromWishlist(Long buyerId, Long carId) {
		WishlistItem item = wishlistRepository.findByUserIdAndCarId(buyerId, carId)
				.orElseThrow(() -> new ResourceNotFoundException("Car not found in wishlist"));

		if (!item.isDeleted()) {
			item.setDeleted(true);
			item.setDeletedAt(LocalDateTime.now());
			wishlistRepository.save(item);
			log.info("Soft-deleted car {} from buyer {}'s wishlist", carId, buyerId);
		}
	}
}

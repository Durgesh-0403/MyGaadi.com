package com.mygaadi.repository.specifications;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.mygaadi.entities.ApprovalStatus;
import com.mygaadi.entities.BodyType;
import com.mygaadi.entities.Car;
import com.mygaadi.entities.CarStatus;
import com.mygaadi.entities.FuelType;
import com.mygaadi.entities.Transmission;

import jakarta.persistence.criteria.Predicate;

public class CarSpecification {

	public static Specification<Car> getFilterSpec(
			String q,
			String brand,
			String model,
			FuelType fuelType,
			Transmission transmission,
			BodyType bodyType,
			String city,
			String state,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Integer minYear,
			Integer maxYear,
			Integer maxKm,
			Integer maxOwners) {

		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			// Always enforce public marketplace constraints
			predicates.add(cb.equal(root.get("approvalStatus"), ApprovalStatus.APPROVED));
			predicates.add(cb.isFalse(root.get("deleted")));
			predicates.add(root.get("status").in(CarStatus.AVAILABLE, CarStatus.RESERVED));
			predicates.add(cb.isTrue(root.join("seller").join("sellerProfile").get("verified")));

			if (StringUtils.hasText(q)) {
				String term = "%" + q.trim().toLowerCase() + "%";
				Predicate titleMatch = cb.like(cb.lower(root.get("title")), term);
				Predicate brandMatch = cb.like(cb.lower(root.get("brand")), term);
				Predicate modelMatch = cb.like(cb.lower(root.get("model")), term);
				Predicate variantMatch = cb.like(cb.lower(root.get("variant")), term);
				Predicate cityMatch = cb.like(cb.lower(root.get("city")), term);
				predicates.add(cb.or(titleMatch, brandMatch, modelMatch, variantMatch, cityMatch));
			}

			if (StringUtils.hasText(brand)) {
				predicates.add(cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase()));
			}

			if (StringUtils.hasText(model)) {
				predicates.add(cb.equal(cb.lower(root.get("model")), model.trim().toLowerCase()));
			}

			if (fuelType != null) {
				predicates.add(cb.equal(root.get("fuelType"), fuelType));
			}

			if (transmission != null) {
				predicates.add(cb.equal(root.get("transmission"), transmission));
			}

			if (bodyType != null) {
				predicates.add(cb.equal(root.get("bodyType"), bodyType));
			}

			if (StringUtils.hasText(city)) {
				predicates.add(cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase()));
			}

			if (StringUtils.hasText(state)) {
				predicates.add(cb.equal(cb.lower(root.get("state")), state.trim().toLowerCase()));
			}

			if (minPrice != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
			}

			if (maxPrice != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
			}

			if (minYear != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("manufactureYear"), minYear));
			}

			if (maxYear != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("manufactureYear"), maxYear));
			}

			if (maxKm != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("kmDriven"), maxKm));
			}

			if (maxOwners != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("ownerCount"), maxOwners));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}

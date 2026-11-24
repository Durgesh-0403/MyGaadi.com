package com.mygaadi.dtos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.mygaadi.entities.BodyType;
import com.mygaadi.entities.FuelType;
import com.mygaadi.entities.Transmission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CarFilterOptionsDTO {
	private List<String> brands;
	private Map<String, List<String>> brandModelsMap;
	private List<String> cities;
	private List<FuelType> fuelTypes;
	private List<Transmission> transmissions;
	private List<BodyType> bodyTypes;
	private BigDecimal minPrice;
	private BigDecimal maxPrice;
	private Integer minYear;
	private Integer maxYear;
}

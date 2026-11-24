package com.mygaadi.dtos;

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
public class CarImageDTO {
	private Long id;
	private String fileName;
	private String url;
	private boolean primary;
	private String category;
	private int displayOrder;
}

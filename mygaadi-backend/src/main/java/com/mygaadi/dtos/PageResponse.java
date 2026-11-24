package com.mygaadi.dtos;

import java.util.List;

import org.springframework.data.domain.Page;

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
public class PageResponse<T> {
	private List<T> content;
	private int page;
	private int size;
	private long totalElements;
	private int totalPages;
	private boolean first;
	private boolean last;

	public static <T> PageResponse<T> of(Page<T> page) {
		return PageResponse.<T>builder()
				.content(page.getContent())
				.page(page.getNumber())
				.size(page.getSize())
				.totalElements(page.getTotalElements())
				.totalPages(page.getTotalPages())
				.first(page.isFirst())
				.last(page.isLast())
				.build();
	}

	public static <T, R> PageResponse<R> of(Page<T> page, List<R> transformedContent) {
		return PageResponse.<R>builder()
				.content(transformedContent)
				.page(page.getNumber())
				.size(page.getSize())
				.totalElements(page.getTotalElements())
				.totalPages(page.getTotalPages())
				.first(page.isFirst())
				.last(page.isLast())
				.build();
	}
}

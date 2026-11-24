package com.mygaadi.payment.entities;

import java.time.LocalDateTime;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "webhook_events")
@AttributeOverride(name = "id", column = @Column(name = "event_id_pk"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WebhookEvent extends BaseClass {

	@Column(name = "event_id", length = 100, nullable = false, unique = true)
	private String eventId;

	@Column(name = "event_type", length = 100, nullable = false)
	private String eventType;

	@Column(name = "processed_at", nullable = false)
	private LocalDateTime processedAt;
}

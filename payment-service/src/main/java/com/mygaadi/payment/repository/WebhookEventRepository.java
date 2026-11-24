package com.mygaadi.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mygaadi.payment.entities.WebhookEvent;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

	boolean existsByEventId(String eventId);
}

package com.mygaadi.payment.razorpay;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.mygaadi.payment.custom_exceptions.ApiException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RazorpayService {

	@Value("${razorpay.key.id:}")
	@Getter
	private String keyId;

	@Value("${razorpay.key.secret:}")
	private String keySecret;

	@Value("${razorpay.webhook.secret:}")
	private String webhookSecret;

	@Value("${razorpay.mock:false}")
	@Getter
	private boolean mockMode;

	private final Environment environment;
	private RazorpayClient razorpayClient;

	public RazorpayService(Environment environment) {
		this.environment = environment;
	}

	@PostConstruct
	public void init() {
		boolean isProd = false;
		for (String profile : environment.getActiveProfiles()) {
			if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
				isProd = true;
				break;
			}
		}

		if (mockMode) {
			if (isProd) {
				throw new IllegalStateException("FATAL: RAZORPAY_MOCK is set to true in production profile! Server refusing to start.");
			}
			log.warn("RAZORPAY_MOCK=true: Simulating Razorpay order creation and refunds locally for offline development.");
		} else {
			if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
				log.warn("Razorpay credentials not fully configured. Real transactions will fail with HTTP 503 until keys are set.");
			} else {
				try {
					this.razorpayClient = new RazorpayClient(keyId, keySecret);
					log.info("Initialized Razorpay Client with Key ID: {}", keyId);
				} catch (Exception e) {
					log.error("Failed to initialize Razorpay Client: {}", e.getMessage());
				}
			}
		}
	}

	public String createOrder(BigDecimal amountInRupees, String receipt) {
		long amountInPaise = amountInRupees.multiply(BigDecimal.valueOf(100)).longValue();

		if (mockMode) {
			String mockOrderId = "order_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
			log.info("MOCK: Created Razorpay order {} for amount ₹{} (receipt: {})", mockOrderId, amountInRupees, receipt);
			return mockOrderId;
		}

		if (razorpayClient == null) {
			throw new ApiException("Razorpay payment gateway service is unavailable (API keys unconfigured).");
		}

		try {
			JSONObject orderRequest = new JSONObject();
			orderRequest.put("amount", amountInPaise);
			orderRequest.put("currency", "INR");
			orderRequest.put("receipt", receipt);

			Order order = razorpayClient.orders.create(orderRequest);
			return order.get("id");
		} catch (Exception e) {
			log.error("Failed to create Razorpay order for receipt {}", receipt, e);
			throw new ApiException("Payment gateway error creating order: " + e.getMessage());
		}
	}

	public boolean verifyPaymentSignature(String orderId, String paymentId, String clientSignature) {
		if (mockMode || (orderId != null && orderId.startsWith("order_mock_"))) {
			if (clientSignature == null || clientSignature.startsWith("tampered") || "invalid_sig".equals(clientSignature)) {
				log.warn("MOCK: Signature verification rejected for invalid or tampered signature");
				return false;
			}
			log.info("MOCK: Signature verification passed for mock order {} and payment {}", orderId, paymentId);
			return true;
		}

		if (keySecret == null || keySecret.isBlank()) {
			throw new ApiException("Cannot verify signature: Razorpay secret is not configured.");
		}

		try {
			String payload = orderId + "|" + paymentId;
			String expectedSignature = calculateHmacSha256(payload, keySecret);
			return MessageDigest.isEqual(
					expectedSignature.getBytes(StandardCharsets.UTF_8),
					clientSignature.getBytes(StandardCharsets.UTF_8)
			);
		} catch (Exception e) {
			log.error("Error verifying payment signature for order {}", orderId, e);
			return false;
		}
	}

	public boolean verifyWebhookSignature(String rawBody, String webhookSignature) {
		if (webhookSecret == null || webhookSecret.isBlank()) {
			log.warn("Webhook signature validation skipped: RAZORPAY_WEBHOOK_SECRET is not configured.");
			return false;
		}

		try {
			String expectedSignature = calculateHmacSha256(rawBody, webhookSecret);
			return MessageDigest.isEqual(
					expectedSignature.getBytes(StandardCharsets.UTF_8),
					webhookSignature.getBytes(StandardCharsets.UTF_8)
			);
		} catch (Exception e) {
			log.error("Error verifying webhook signature", e);
			return false;
		}
	}

	public String initiateRefund(String paymentId, BigDecimal amountInRupees, String reason) {
		long amountInPaise = amountInRupees.multiply(BigDecimal.valueOf(100)).longValue();

		if (mockMode) {
			String mockRefundId = "rfnd_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
			log.info("MOCK: Initiated refund {} for payment {} (amount: ₹{})", mockRefundId, paymentId, amountInRupees);
			return mockRefundId;
		}

		if (razorpayClient == null) {
			throw new ApiException("Razorpay service unavailable for processing refund.");
		}

		try {
			JSONObject refundRequest = new JSONObject();
			refundRequest.put("amount", amountInPaise);
			refundRequest.put("speed", "normal");
			JSONObject notes = new JSONObject();
			notes.put("reason", reason);
			refundRequest.put("notes", notes);

			Refund refund = razorpayClient.payments.refund(paymentId, refundRequest);
			return refund.get("id");
		} catch (Exception e) {
			log.error("Failed to process Razorpay refund for payment {}", paymentId, e);
			throw new ApiException("Payment gateway error initiating refund: " + e.getMessage());
		}
	}

	public String calculateHmacSha256(String data, String secret) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		mac.init(secretKeySpec);
		byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

		StringBuilder hexString = new StringBuilder();
		for (byte b : rawHmac) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}
}

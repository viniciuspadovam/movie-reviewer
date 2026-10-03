package com.moviereview.security;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.moviereview.common.TooManyRequestsException;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

@Component
class LoginRateLimiter {

	private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

	private static final int MAX_TRACKED_IPS = 10_000;

	private final Cache<String, Bucket> buckets;

	private final AuthProperties.LoginRateLimit limit;

	LoginRateLimiter(AuthProperties properties) {
		this.limit = properties.loginRateLimit();
		this.buckets = Caffeine.newBuilder()
			.maximumSize(MAX_TRACKED_IPS)
			.expireAfterAccess(limit.period())
			.build();
	}

	void consume(String clientIp) {
		Bucket bucket = buckets.get(clientIp, ip -> newBucket());
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		if (!probe.isConsumed()) {
			log.warn("Limite de tentativas de login excedido para o IP {}", clientIp);
			throw new TooManyRequestsException("Muitas tentativas de login. Tente novamente mais tarde.",
					Duration.ofNanos(probe.getNanosToWaitForRefill()));
		}
	}

	private Bucket newBucket() {
		return Bucket.builder()
			.addLimit(Bandwidth.builder()
				.capacity(limit.attempts())
				.refillIntervally(limit.attempts(), limit.period())
				.build())
			.build();
	}

}

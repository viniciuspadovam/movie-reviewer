package com.moviereview.review;

import java.time.Instant;
import java.time.LocalDate;

import com.moviereview.common.BusinessRuleException;
import com.moviereview.title.Title;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Review {

	public static final int MIN_RATING = 1;

	public static final int MAX_RATING = 10;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "title_id", updatable = false)
	private Title title;

	private LocalDate watchedOn;

	private Short rating;

	private String content;

	private boolean hasSpoilers;

	@Enumerated(EnumType.STRING)
	private ReviewStatus status;

	private Instant publishedAt;

	@Column(updatable = false)
	private Instant createdAt;

	private Instant updatedAt;

	protected Review() {
	}

	public Review(Title title, ReviewContent reviewContent, LocalDate today, Instant now) {
		this.title = title;
		this.createdAt = now;
		apply(reviewContent, today, now);
	}

	public void update(ReviewContent reviewContent, LocalDate today, Instant now) {
		apply(reviewContent, today, now);
	}

	private void apply(ReviewContent reviewContent, LocalDate today, Instant now) {
		if (reviewContent.watchedOn().isAfter(today)) {
			throw new BusinessRuleException("A data em que assistiu não pode estar no futuro.");
		}
		if (reviewContent.rating() < MIN_RATING || reviewContent.rating() > MAX_RATING) {
			throw new BusinessRuleException("A nota deve estar entre 0,5 e 5 estrelas.");
		}
		String text = reviewContent.content() == null ? "" : reviewContent.content().strip();
		if (reviewContent.status() == ReviewStatus.PUBLISHED && text.isEmpty()) {
			throw new BusinessRuleException("Uma review publicada precisa ter texto.");
		}
		this.watchedOn = reviewContent.watchedOn();
		this.rating = (short) reviewContent.rating();
		this.content = text;
		this.hasSpoilers = reviewContent.hasSpoilers();
		this.status = reviewContent.status();
		if (status == ReviewStatus.PUBLISHED && publishedAt == null) {
			this.publishedAt = now;
		}
		this.updatedAt = now;
	}

	public boolean isPublished() {
		return status == ReviewStatus.PUBLISHED;
	}

	public Long getId() {
		return id;
	}

	public Title getTitle() {
		return title;
	}

	public LocalDate getWatchedOn() {
		return watchedOn;
	}

	public int getRating() {
		return rating;
	}

	public String getContent() {
		return content;
	}

	public boolean hasSpoilers() {
		return hasSpoilers;
	}

	public ReviewStatus getStatus() {
		return status;
	}

	public Instant getPublishedAt() {
		return publishedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}

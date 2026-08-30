package de.capswap.dto;

import de.capswap.entity.Review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long listingId,
        String listingTitle,
        Long reviewerCompanyId,
        String reviewerCompanyName,
        Long targetCompanyId,
        String targetCompanyName,
        Integer rating,
        String comment,
        Instant createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getListing().getId(),
                review.getListing().getTitle(),
                review.getReviewerCompany().getId(),
                review.getReviewerCompany().getName(),
                review.getTargetCompany().getId(),
                review.getTargetCompany().getName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}

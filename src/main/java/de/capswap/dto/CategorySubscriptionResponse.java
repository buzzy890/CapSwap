package de.capswap.dto;

import de.capswap.entity.CategorySubscription;

import java.time.Instant;

public record CategorySubscriptionResponse(
        Long id,
        Long companyId,
        String companyName,
        Long categoryId,
        String categoryName,
        Instant createdAt
) {
    public static CategorySubscriptionResponse from(CategorySubscription subscription) {
        return new CategorySubscriptionResponse(
                subscription.getId(),
                subscription.getCompany().getId(),
                subscription.getCompany().getName(),
                subscription.getCategory().getId(),
                subscription.getCategory().getName(),
                subscription.getCreatedAt()
        );
    }
}

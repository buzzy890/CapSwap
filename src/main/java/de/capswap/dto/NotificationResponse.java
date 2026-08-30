package de.capswap.dto;

import de.capswap.entity.Notification;

import java.time.Instant;


public record NotificationResponse(
        Long id,
        Long companyId,
        Long listingId,
        String listingTitle,
        String message,
        Boolean isRead,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        boolean hasListing = notification.getListing() != null;
        return new NotificationResponse(
                notification.getId(),
                notification.getCompany().getId(),
                hasListing ? notification.getListing().getId() : null,
                hasListing ? notification.getListing().getTitle() : null,
                notification.getMessage(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}

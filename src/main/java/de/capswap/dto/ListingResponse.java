package de.capswap.dto;

import de.capswap.entity.Listing;
import de.capswap.entity.ListingPhoto;
import de.capswap.entity.enums.ListingStatus;

import java.time.Instant;
import java.util.List;

// dto hilft die daten zwischen backend und frontend zu übertragen, ohne die gesamte entity zu senden
public record ListingResponse(
        Long id,
        Long companyId,
        String companyName,
        Long categoryId,
        String categoryName,
        String title,
        String description,
        String location,
        ListingStatus status,
        List<String> photoUrls,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        Boolean expiryWarningSent
) {
    public static ListingResponse from(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getCompany().getId(),
                listing.getCompany().getName(),
                listing.getCategory().getId(),
                listing.getCategory().getName(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getLocation(),
                listing.getStatus(),
                listing.getPhotos().stream().map(ListingPhoto::getPhotoUrl).toList(),
                listing.getCreatedAt(),
                listing.getUpdatedAt(),
                listing.getExpiresAt(),
                listing.isExpiryWarningSent()
        );
    }
}

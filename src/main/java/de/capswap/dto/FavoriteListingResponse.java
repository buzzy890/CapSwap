package de.capswap.dto;

import de.capswap.entity.FavoriteListing;

import java.time.Instant;

public record FavoriteListingResponse(
        Long id,
        Long companyId,
        Long listingId,
        String listingTitle,
        Instant createdAt
) {
    public static FavoriteListingResponse from(FavoriteListing favorite) {
        return new FavoriteListingResponse(
                favorite.getId(),
                favorite.getCompany().getId(),
                favorite.getListing().getId(),
                favorite.getListing().getTitle(),
                favorite.getCreatedAt()
        );
    }
}

package de.capswap.dto;

import de.capswap.entity.OfferProposal;
import de.capswap.entity.OfferProposalPhoto;
import de.capswap.entity.enums.OfferStatus;

import java.time.Instant;
import java.util.List;

public record OfferProposalResponse(
        Long id,
        Long listingId,
        String listingTitle,
        Long proposingCompanyId,
        String proposingCompanyName,
        String message,
        OfferStatus status,
        List<String> photoUrls,
        Instant createdAt,
        Instant updatedAt
) {
    public static OfferProposalResponse from(OfferProposal offer) {
        return new OfferProposalResponse(
                offer.getId(),
                offer.getListing().getId(),
                offer.getListing().getTitle(),
                offer.getProposingCompany().getId(),
                offer.getProposingCompany().getName(),
                offer.getMessage(),
                offer.getStatus(),
                offer.getPhotos().stream().map(OfferProposalPhoto::getPhotoUrl).toList(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }
}

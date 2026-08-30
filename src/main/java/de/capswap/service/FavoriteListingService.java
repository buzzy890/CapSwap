package de.capswap.service;

import de.capswap.dto.FavoriteListingResponse;
import de.capswap.entity.Company;
import de.capswap.entity.FavoriteListing;
import de.capswap.entity.Listing;
import de.capswap.repository.FavoriteListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteListingService {

    private final FavoriteListingRepository favoriteListingRepository;

    public List<FavoriteListingResponse> getFavoritesByCompany(Long companyId) {
        return favoriteListingRepository.findByCompanyId(companyId).stream()
                .map(FavoriteListingResponse::from)
                .toList();
    }

    public boolean isFavorite(Long companyId, Long listingId) {
        return favoriteListingRepository.existsByCompanyIdAndListingId(companyId, listingId);
    }

    @Transactional
    public FavoriteListingResponse addFavorite(Company company, Listing listing) {
        if (favoriteListingRepository.existsByCompanyIdAndListingId(company.getId(), listing.getId())) {
            throw new IllegalArgumentException("Listing is already in favorites.");
        }
        FavoriteListing favoriteListing = FavoriteListing.builder()
                .company(company)
                .listing(listing)
                .build();
        return FavoriteListingResponse.from(favoriteListingRepository.save(favoriteListing));
    }

    @Transactional
    public void removeFavorite(Long companyId, Long listingId) {
        favoriteListingRepository.deleteByCompanyIdAndListingId(companyId, listingId);
    }
}

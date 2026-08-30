package de.capswap.repository;

import de.capswap.entity.FavoriteListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteListingRepository extends JpaRepository<FavoriteListing, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "listing", "listing.company", "listing.category", "listing.photos"})
    List<FavoriteListing> findByCompanyId(Long companyId);
    boolean existsByCompanyIdAndListingId(Long companyId, Long listingId);
    void deleteByCompanyIdAndListingId(Long companyId, Long listingId);
}

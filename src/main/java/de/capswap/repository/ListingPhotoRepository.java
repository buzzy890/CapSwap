package de.capswap.repository;

import de.capswap.entity.ListingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingPhotoRepository extends JpaRepository<ListingPhoto, Long> {
    List<ListingPhoto> findByListingIdOrderBySortOrderAsc(Long listingId);
    void deleteByListingId(Long listingId);
}

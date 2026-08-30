package de.capswap.repository;

import de.capswap.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByTargetCompanyId(Long targetCompanyId);
    List<Review> findByListingId(Long listingId);
    List<Review> findByReviewerCompanyId(Long reviewerCompanyId);
}

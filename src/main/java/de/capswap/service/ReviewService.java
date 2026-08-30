package de.capswap.service;

import de.capswap.dto.ReviewResponse;
import de.capswap.entity.Company;
import de.capswap.entity.Listing;
import de.capswap.entity.Review;
import de.capswap.repository.CompanyRepository;
import de.capswap.repository.ListingRepository;
import de.capswap.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ListingRepository listingRepository;
    private final CompanyRepository companyRepository;

    public List<ReviewResponse> getReviewsForCompany(Long targetCompanyId) {
        return reviewRepository.findByTargetCompanyId(targetCompanyId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    public List<ReviewResponse> getReviewsByListing(Long listingId) {
        return reviewRepository.findByListingId(listingId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public ReviewResponse createReview(Review review) {
        Listing listing = listingRepository.findById(review.getListing().getId())
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
        Company reviewerCompany = companyRepository.findById(review.getReviewerCompany().getId())
                .orElseThrow(() -> new IllegalArgumentException("Reviewer company not found"));
        Company targetCompany = companyRepository.findById(review.getTargetCompany().getId())
                .orElseThrow(() -> new IllegalArgumentException("Target company not found"));
        review.setListing(listing);
        review.setReviewerCompany(reviewerCompany);
        review.setTargetCompany(targetCompany);

        return ReviewResponse.from(reviewRepository.save(review));
    }
}

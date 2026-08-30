package de.capswap.controller;

import de.capswap.dto.ReviewResponse;
import de.capswap.entity.Review;
import de.capswap.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(
            @RequestParam(required = false) Long targetCompanyId,
            @RequestParam(required = false) Long listingId) {

        if (targetCompanyId != null) {
            return ResponseEntity.ok(reviewService.getReviewsForCompany(targetCompanyId));
        }
        if (listingId != null) {
            return ResponseEntity.ok(reviewService.getReviewsByListing(listingId));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody Review review) {
        ReviewResponse created = reviewService.createReview(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}

package de.capswap.controller;

import de.capswap.dto.FavoriteListingResponse;
import de.capswap.entity.Company;
import de.capswap.entity.Listing;
import de.capswap.service.CompanyService;
import de.capswap.service.FavoriteListingService;
import de.capswap.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteListingController {

    private final FavoriteListingService favoriteListingService;
    private final CompanyService companyService;
    private final ListingService listingService;

    @GetMapping
    public ResponseEntity<List<FavoriteListingResponse>> getFavorites(@RequestParam Long companyId) {
        return ResponseEntity.ok(favoriteListingService.getFavoritesByCompany(companyId));
    }

    @PostMapping
    public ResponseEntity<FavoriteListingResponse> addFavorite(
            @RequestParam Long companyId,
            @RequestParam Long listingId) {

        Company company = companyService.getCompanyById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        Listing listing = listingService.getListingEntityById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        FavoriteListingResponse favorite = favoriteListingService.addFavorite(company, listing);
        return ResponseEntity.status(HttpStatus.CREATED).body(favorite);
    }

    @DeleteMapping
    public ResponseEntity<Void> removeFavorite(
            @RequestParam Long companyId,
            @RequestParam Long listingId) {
        favoriteListingService.removeFavorite(companyId, listingId);
        return ResponseEntity.noContent().build();
    }
}

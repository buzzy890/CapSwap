package de.capswap.controller;

import de.capswap.dto.ListingResponse;
import de.capswap.entity.Listing;
import de.capswap.entity.enums.ListingStatus;
import de.capswap.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/listings") //lieg den Url-Pfad für die API fest
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @GetMapping //mappt eine GET call
    public ResponseEntity<List<ListingResponse>> getAllListings(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) ListingStatus status) {

        if (categoryId != null) {
            return ResponseEntity.ok(listingService.getListingsByCategory(categoryId));
        }
        if (companyId != null) {
            return ResponseEntity.ok(listingService.getListingsByCompany(companyId));
        }
        if (status != null) {
            return ResponseEntity.ok(listingService.getListingsByStatus(status));
        }
        return ResponseEntity.ok(listingService.getAllListings());
    }

    @GetMapping("/{id}") //mappt eine GET call mit id
    public ResponseEntity<ListingResponse> getListingById(@PathVariable Long id) {
        return listingService.getListingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping //mappt eine POST call
    public ResponseEntity<ListingResponse> createListing(@Valid @RequestBody Listing listing) {
        Listing created = listingService.createListing(listing);
        return ResponseEntity.status(HttpStatus.CREATED).body(ListingResponse.from(created));
    }

    @PutMapping("/{id}") 
    public ResponseEntity<ListingResponse> updateListing(@PathVariable Long id, @Valid @RequestBody Listing listing) {
        return ResponseEntity.ok(listingService.updateListing(id, listing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        listingService.deleteListing(id);
        return ResponseEntity.noContent().build();
    }
}

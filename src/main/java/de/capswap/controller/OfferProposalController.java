package de.capswap.controller;

import de.capswap.dto.OfferProposalResponse;
import de.capswap.entity.OfferProposal;
import de.capswap.entity.enums.OfferStatus;
import de.capswap.service.OfferProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferProposalController {

    private final OfferProposalService offerProposalService;

    @GetMapping
    public ResponseEntity<List<OfferProposalResponse>> getOffers(
            @RequestParam(required = false) Long listingId,
            @RequestParam(required = false) Long companyId) {

        if (listingId != null) {
            return ResponseEntity.ok(offerProposalService.getOffersByListing(listingId));
        }
        if (companyId != null) {
            return ResponseEntity.ok(offerProposalService.getOffersByCompany(companyId));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferProposalResponse> getOfferById(@PathVariable Long id) {
        return offerProposalService.getOfferById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OfferProposalResponse> createOffer(@Valid @RequestBody OfferProposal offerProposal) {
        OfferProposalResponse created = offerProposalService.createOffer(offerProposal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OfferProposalResponse> updateOfferStatus(
            @PathVariable Long id,
            @RequestParam OfferStatus status) {
        return ResponseEntity.ok(offerProposalService.updateOfferStatus(id, status));
    }

    @PostMapping("/{id}/retract")
    public ResponseEntity<OfferProposalResponse> retractOffer(@PathVariable Long id) {
        return ResponseEntity.ok(offerProposalService.retractOffer(id));
    }
}

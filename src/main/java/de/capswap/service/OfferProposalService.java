package de.capswap.service;

import de.capswap.dto.OfferProposalResponse;
import de.capswap.entity.Company;
import de.capswap.entity.Listing;
import de.capswap.entity.OfferProposal;
import de.capswap.entity.enums.OfferStatus;
import de.capswap.repository.CompanyRepository;
import de.capswap.repository.ListingRepository;
import de.capswap.repository.OfferProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfferProposalService {

    private final OfferProposalRepository offerProposalRepository;
    private final ListingRepository listingRepository;
    private final CompanyRepository companyRepository;

    public List<OfferProposalResponse> getOffersByListing(Long listingId) {
        return offerProposalRepository.findByListingId(listingId).stream()
                .map(OfferProposalResponse::from)
                .toList();
    }

    public List<OfferProposalResponse> getOffersByCompany(Long companyId) {
        return offerProposalRepository.findByProposingCompanyId(companyId).stream()
                .map(OfferProposalResponse::from)
                .toList();
    }

    public Optional<OfferProposalResponse> getOfferById(Long id) {
        return offerProposalRepository.findById(id).map(OfferProposalResponse::from);
    }

    @Transactional
    public OfferProposalResponse createOffer(OfferProposal offerProposal) {
        Listing listing = listingRepository.findById(offerProposal.getListing().getId())
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
        Company proposingCompany = companyRepository.findById(offerProposal.getProposingCompany().getId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        offerProposal.setListing(listing);
        offerProposal.setProposingCompany(proposingCompany);
        offerProposal.setStatus(OfferStatus.PENDING);

        return OfferProposalResponse.from(offerProposalRepository.save(offerProposal));
    }

    @Transactional
    public OfferProposalResponse updateOfferStatus(Long id, OfferStatus newStatus) {
        OfferProposal updated = offerProposalRepository.findById(id)
                .map(existing -> {
                    existing.setStatus(newStatus);
                    return offerProposalRepository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("OfferProposal not found with id: " + id));
        return OfferProposalResponse.from(updated);
    }

    @Transactional
    public OfferProposalResponse retractOffer(Long id) {
        return updateOfferStatus(id, OfferStatus.RETRACTED);
    }
}

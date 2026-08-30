package de.capswap.repository;

import de.capswap.entity.OfferProposal;
import de.capswap.entity.enums.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferProposalRepository extends JpaRepository<OfferProposal, Long> {

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"listing", "proposingCompany", "photos", "listing.company", "listing.category", "listing.photos"})
    List<OfferProposal> findAll();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"listing", "proposingCompany", "photos", "listing.company", "listing.category", "listing.photos"})
    List<OfferProposal> findByListingId(Long listingId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"listing", "proposingCompany", "photos", "listing.company", "listing.category", "listing.photos"})
    List<OfferProposal> findByProposingCompanyId(Long proposingCompanyId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"listing", "proposingCompany", "photos", "listing.company", "listing.category", "listing.photos"})
    List<OfferProposal> findByStatus(OfferStatus status);
}

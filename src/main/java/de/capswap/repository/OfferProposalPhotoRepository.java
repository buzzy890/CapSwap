package de.capswap.repository;

import de.capswap.entity.OfferProposalPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferProposalPhotoRepository extends JpaRepository<OfferProposalPhoto, Long> {
    List<OfferProposalPhoto> findByOfferProposalIdOrderBySortOrderAsc(Long offerProposalId);
    void deleteByOfferProposalId(Long offerProposalId);
}

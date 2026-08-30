package de.capswap.repository;

import de.capswap.entity.Listing;
import de.capswap.entity.enums.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "category", "photos"})
    List<Listing> findAll();

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "category", "photos"})
    java.util.Optional<Listing> findById(Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "category", "photos"})
    List<Listing> findByCompanyId(Long companyId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "category", "photos"})
    List<Listing> findByCategoryId(Long categoryId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "category", "photos"})
    List<Listing> findByStatus(ListingStatus status);
    boolean existsByCategoryId(Long categoryId);

    // FA 12: Angebote, deren Ablauf bevorsteht und für die noch keine Vorab-Warnung versendet wurde.
    List<Listing> findByStatusAndExpiresAtBeforeAndExpiryWarningSentFalse(ListingStatus status, Instant threshold);

    // FA 11: Noch nicht gelöschte Angebote, deren Ablaufdatum bereits verstrichen ist.
    List<Listing> findByStatusNotAndExpiresAtBefore(ListingStatus status, Instant now);
}

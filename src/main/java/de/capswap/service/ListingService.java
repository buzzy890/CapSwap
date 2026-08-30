package de.capswap.service;

import de.capswap.dto.ListingResponse;
import de.capswap.entity.Category;
import de.capswap.entity.Company;
import de.capswap.entity.Listing;
import de.capswap.entity.enums.ListingStatus;
import de.capswap.repository.CategoryRepository;
import de.capswap.repository.CompanyRepository;
import de.capswap.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListingService {

    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<ListingResponse> getAllListings() {
        return listingRepository.findAll().stream().map(ListingResponse::from).toList();
    }

    public Optional<ListingResponse> getListingById(Long id) {
        return listingRepository.findById(id).map(ListingResponse::from);
    }

    public Optional<Listing> getListingEntityById(Long id) {
        return listingRepository.findById(id);
    }

    public List<ListingResponse> getListingsByCompany(Long companyId) {
        return listingRepository.findByCompanyId(companyId).stream().map(ListingResponse::from).toList();
    }

    public List<ListingResponse> getListingsByCategory(Long categoryId) {
        return listingRepository.findByCategoryId(categoryId).stream().map(ListingResponse::from).toList();
    }

    public List<ListingResponse> getListingsByStatus(ListingStatus status) {
        return listingRepository.findByStatus(status).stream().map(ListingResponse::from).toList();
    }

    @Transactional
    public Listing createListing(Listing listing) {
        Company company = companyRepository.findById(listing.getCompany().getId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        Category category = categoryRepository.findById(listing.getCategory().getId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        listing.setCompany(company);
        listing.setCategory(category);

        Listing saved = listingRepository.save(listing);
        eventPublisher.publishEvent(new de.capswap.socket.NewListingEvent(this, saved));
        return saved;
    }

    @Transactional
    public ListingResponse updateListing(Long id, Listing updatedDetails) {
        Listing existing = listingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found with id: " + id));

        existing.setTitle(updatedDetails.getTitle());
        existing.setDescription(updatedDetails.getDescription());
        existing.setLocation(updatedDetails.getLocation());
        existing.setStatus(updatedDetails.getStatus());
        Category category = categoryRepository.findById(updatedDetails.getCategory().getId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        existing.setCategory(category);

        return ListingResponse.from(listingRepository.save(existing));
    }

    @Transactional
    public void deleteListing(Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found with id: " + id));
        listing.setStatus(ListingStatus.DELETED);
        listingRepository.save(listing);
    }
}

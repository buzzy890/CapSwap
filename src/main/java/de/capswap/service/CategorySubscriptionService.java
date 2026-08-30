package de.capswap.service;

import de.capswap.dto.CategorySubscriptionResponse;
import de.capswap.entity.Category;
import de.capswap.entity.CategorySubscription;
import de.capswap.entity.Company;
import de.capswap.repository.CategorySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategorySubscriptionService {

    private final CategorySubscriptionRepository subscriptionRepository;

    public List<CategorySubscriptionResponse> getSubscriptionsByCompany(Long companyId) {
        return subscriptionRepository.findByCompanyId(companyId).stream()
                .map(CategorySubscriptionResponse::from)
                .toList();
    }

    public List<CategorySubscriptionResponse> getSubscriptionsByCategory(Long categoryId) {
        return subscriptionRepository.findByCategoryId(categoryId).stream()
                .map(CategorySubscriptionResponse::from)
                .toList();
    }

    @Transactional
    public CategorySubscriptionResponse subscribe(Company company, Category category) {
        if (subscriptionRepository.existsByCompanyIdAndCategoryId(company.getId(), category.getId())) {
            throw new IllegalArgumentException("Already subscribed to category.");
        }
        CategorySubscription subscription = CategorySubscription.builder()
                .company(company)
                .category(category)
                .build();
        return CategorySubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Transactional
    public void unsubscribe(Long companyId, Long categoryId) {
        subscriptionRepository.deleteByCompanyIdAndCategoryId(companyId, categoryId);
    }
}

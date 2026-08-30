package de.capswap.repository;

import de.capswap.entity.CategorySubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategorySubscriptionRepository extends JpaRepository<CategorySubscription, Long> {
    List<CategorySubscription> findByCompanyId(Long companyId);
    List<CategorySubscription> findByCategoryId(Long categoryId);
    boolean existsByCompanyIdAndCategoryId(Long companyId, Long categoryId);
    void deleteByCompanyIdAndCategoryId(Long companyId, Long categoryId);
}

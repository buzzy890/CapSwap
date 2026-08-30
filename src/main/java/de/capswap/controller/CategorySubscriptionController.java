package de.capswap.controller;

import de.capswap.dto.CategorySubscriptionResponse;
import de.capswap.entity.Category;
import de.capswap.entity.Company;
import de.capswap.service.CategoryService;
import de.capswap.service.CategorySubscriptionService;
import de.capswap.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class CategorySubscriptionController {

    private final CategorySubscriptionService subscriptionService;
    private final CompanyService companyService;
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategorySubscriptionResponse>> getSubscriptions(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long categoryId) {

        if (companyId != null) {
            return ResponseEntity.ok(subscriptionService.getSubscriptionsByCompany(companyId));
        }
        if (categoryId != null) {
            return ResponseEntity.ok(subscriptionService.getSubscriptionsByCategory(categoryId));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping
    public ResponseEntity<CategorySubscriptionResponse> subscribe(
            @RequestParam Long companyId,
            @RequestParam Long categoryId) {

        Company company = companyService.getCompanyById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        Category category = categoryService.getCategoryById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        CategorySubscriptionResponse subscription = subscriptionService.subscribe(company, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    @DeleteMapping
    public ResponseEntity<Void> unsubscribe(
            @RequestParam Long companyId,
            @RequestParam Long categoryId) {
        subscriptionService.unsubscribe(companyId, categoryId);
        return ResponseEntity.noContent().build();
    }
}

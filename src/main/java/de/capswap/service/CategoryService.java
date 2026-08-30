package de.capswap.service;

import de.capswap.entity.Category;
import de.capswap.repository.CategoryRepository;
import de.capswap.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ListingRepository listingRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Transactional
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (listingRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException(
                    "Kategorie kann nicht gelöscht werden: es existieren noch Angebote in dieser Kategorie.");
        }
        categoryRepository.deleteById(id);
    }
}

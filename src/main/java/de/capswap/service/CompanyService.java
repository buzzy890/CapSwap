package de.capswap.service;

import de.capswap.entity.Company;
import de.capswap.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Optional<Company> getCompanyById(Long id) {
        return companyRepository.findById(id);
    }

    public Optional<Company> getCompanyByEmail(String email) {
        return companyRepository.findByEmail(email);
    }

    @Transactional
    public Company createCompany(Company company) {
        return companyRepository.save(company);
    }

    @Transactional
    public Company updateCompany(Long id, Company updatedDetails) {
        return companyRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedDetails.getName());
                    existing.setLocation(updatedDetails.getLocation());
                    existing.setDescription(updatedDetails.getDescription());
                    return companyRepository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("Company not found with id: " + id));
    }
}

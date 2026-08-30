package de.capswap.dto;

import de.capswap.entity.Company;
import java.time.Instant;

public record CompanyResponse(
        Long id,
        String name,
        String email,
        String location,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static CompanyResponse from(Company company) {
        if (company == null) {
            return null;
        }
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getEmail(),
                company.getLocation(),
                company.getDescription(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}

package de.capswap.repository;

import de.capswap.entity.Company;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CompanyRepositoryUniqueEmailTests { //prüft auf der Repository-Ebene, ob der UNIQUE-Constraint auf companies.email  funktioniert

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void secondCompanyWithSameEmailViolatesUniqueConstraint() {
        String sharedEmail = "duplicate@capswap.de";

        companyRepository.saveAndFlush(Company.builder()
                .name("Erste Firma GmbH")
                .email(sharedEmail)
                .passwordHash("hashed-pw-1")
                .build());

        Company second = Company.builder()
                .name("Zweite Firma GmbH")
                .email(sharedEmail)
                .passwordHash("hashed-pw-2")
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> companyRepository.saveAndFlush(second),
                "Der UNIQUE-Constraint auf companies.email soll den 2. Insert ablehnen.");
    }
}

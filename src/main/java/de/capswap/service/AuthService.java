package de.capswap.service;

import de.capswap.entity.Company;
import de.capswap.entity.PasswordResetToken;
import de.capswap.repository.CompanyRepository;
import de.capswap.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final CompanyRepository companyRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Company register(de.capswap.dto.AuthDtos.RegisterRequest request) {
        if (companyRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("A company with this email already exists (NFA 12).");
        }
        
        Company company = Company.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .location(request.getLocation())
                .description(request.getDescription())
                .build();
                
        return companyRepository.save(company);
    }

    public Optional<Company> authenticate(String email, String password) {
        return companyRepository.findByEmail(email)
                .filter(c -> c.getPasswordHash() != null)
                .filter(c -> passwordEncoder.matches(password, c.getPasswordHash()));
    }

    @Transactional
    public PasswordResetToken createPasswordResetToken(String email) {
        Company company = companyRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No company found with email: " + email));

        tokenRepository.deleteByCompanyId(company.getId());

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .company(company)
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        return tokenRepository.save(resetToken);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token."));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Password reset token has expired.");
        }

        Company company = resetToken.getCompany();
        company.setPasswordHash(passwordEncoder.encode(newPassword));
        companyRepository.save(company);

        tokenRepository.delete(resetToken);
    }
}

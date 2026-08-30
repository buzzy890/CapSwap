package de.capswap.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

public class AuthDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "Unternehmensname darf nicht leer sein.")
        private String name;

        @NotBlank(message = "E-Mail-Adresse darf nicht leer sein.")
        @Email(message = "Es muss eine gültige E-Mail-Adresse angegeben werden.")
        private String email;

        @NotBlank(message = "Passwort darf nicht leer sein.")
        private String password;

        private String location;
        
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginRequest {
        @NotBlank(message = "E-Mail-Adresse darf nicht leer sein.")
        @Email(message = "Ungültige E-Mail-Adresse.")
        private String email;

        @NotBlank(message = "Passwort darf nicht leer sein.")
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PasswordResetRequest {
        @NotBlank(message = "E-Mail-Adresse darf nicht leer sein.")
        @Email(message = "Ungültige E-Mail-Adresse.")
        private String email;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PasswordResetConfirm {
        @NotBlank(message = "Token darf nicht leer sein.")
        private String token;

        @NotBlank(message = "Neues Passwort darf nicht leer sein.")
        private String newPassword;
    }
}

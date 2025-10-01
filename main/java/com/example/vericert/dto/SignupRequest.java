package com.example.vericert.dto;

import jakarta.validation.constraints.*;

public class SignupRequest {

    @NotBlank(message = "Il nome utente è obbligatorio")
    @Size(min = 3, max = 50, message = "Il nome utente deve avere tra 3 e 50 caratteri")
    private String username;

    @NotBlank(message = "Il nome del tenant è obbligatorio")
    @Size(min = 4, max = 50, message = "Il nome del tenant deve avere tra 4 e 50 caratteri")
    private String tenantName;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    private String email;

    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 8, message = "La password deve avere almeno 8 caratteri")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[@$!%*?&]).+$",
            message = "La password deve contenere almeno una maiuscola, una minuscola, un numero e un carattere speciale"
    )
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }
}
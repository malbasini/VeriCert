package com.example.vericert.dto;

import jakarta.validation.constraints.*;

public record SignupRequest (

    @NotBlank(message = "Il nome utente è obbligatorio")
    @Size(min = 3, max = 50, message = "Il nome utente deve avere tra 3 e 50 caratteri")
    String username,

    @NotBlank(message = "Password obbligatoria")
    @Size(min = 8, message = "La password deve essere almeno di 8 caratteri")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
            message = "La password deve contenere maiuscola, minuscola, numero e carattere speciale"
    )
    String password,

    @NotBlank(message = "Il nome del tenant è obbligatorio")
    @Size(min = 4, max = 50, message = "Il nome del tenant deve avere tra 4 e 50 caratteri")
    String tenantName,

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    String email){}

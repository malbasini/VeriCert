package com.example.vericert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactFormDto {

    @NotBlank
    @Size(max = 120)
    private String name;

    @Email
    @NotBlank
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(max = 4000)
    private String message;

    @Size(max = 60)
    private String subject; // opzionale

    // getters/setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
}

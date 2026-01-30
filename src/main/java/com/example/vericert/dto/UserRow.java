package com.example.vericert.dto;

import com.example.vericert.enumerazioni.Status;

public record UserRow(
        Long id,          // tipo uguale a u.id (Long, UUID, ecc.)
        String userName,
        String email,
        com.example.vericert.enumerazioni.Role role,
        Status status
) {}


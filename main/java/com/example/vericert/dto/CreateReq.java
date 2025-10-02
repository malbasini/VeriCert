package com.example.vericert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateReq(@NotNull Long templateId, @NotNull Map<String,Object> vars, @NotBlank String ownerName, @NotBlank String ownerEmail, @NotBlank String courseCode){}
package com.example.vericert.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResp<T>(List<T> content, int number, int size, long totalElements, int totalPages, boolean first, boolean last) {
    public static <T> PageResp<T> of(Page<T> p) {
        return new PageResp<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast());
    }
}

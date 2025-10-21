package com.example.vericert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class MyService {
    private final ObjectMapper mapper;
    public MyService(ObjectMapper mapper) { this.mapper = mapper; }
}

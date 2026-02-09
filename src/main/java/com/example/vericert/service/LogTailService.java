package com.example.vericert.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class LogTailService {

    // es: src/main/resources/application.properties
    // logging.file.name=logs/vericert.log
    @Value("${logging.file.name:logs/vericert.log}")
    private String logFilePath;

    // ultime N righe
    private static final int MAX_LINES = 200;

    public List<String> tail() {
        try {
            Path p = Paths.get(logFilePath);
            if (!Files.exists(p)) {
                return List.of("[no log file found]");
            }
            List<String> all = Files.readAllLines(p);
            if (all.size() <= MAX_LINES) {
                return all;
            }
            return all.subList(all.size() - MAX_LINES, all.size());
        } catch (Exception e) {
            return List.of("[error reading log file: " + e.getMessage() + "]");
        }
    }
}

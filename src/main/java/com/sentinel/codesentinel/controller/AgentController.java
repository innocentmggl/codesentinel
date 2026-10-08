package com.sentinel.codesentinel.controller;

import com.sentinel.codesentinel.domain.AnalysisResult;
import com.sentinel.codesentinel.service.AnalysisService;
import com.sentinel.codesentinel.service.RepositoryScanService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    public record ScanRequest(String path) {}

    private final AnalysisService analysisService;
    private final RepositoryScanService scanService;
    private final String allowedRoot;

    public AgentController(AnalysisService analysisService, RepositoryScanService scanService,
                           @Value("${sentinel.scan.allowed-root}") String allowedRoot) {
        this.analysisService = analysisService;
        this.scanService = scanService;
        this.allowedRoot = allowedRoot;
    }

    /** Analyze one uploaded .java file. */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResult analyze(@RequestParam("file") MultipartFile file) throws IOException {
        String source = new String(file.getBytes(), StandardCharsets.UTF_8);
        return analysisService.analyzeSource(file.getOriginalFilename(), source);
    }

    /** Analyze every .java file under a folder on this machine (local use only). */
    @PostMapping("/analyze-repo")
    public List<AnalysisResult> analyzeRepo(@RequestBody ScanRequest request) throws IOException {
        Path root;
        Path allowed;
        try {
            root = Path.of(request.path()).toRealPath();
            allowed = Path.of(allowedRoot).toRealPath();
        } catch (NoSuchFileException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path not found: " + e.getFile());
        }
        if (!root.startsWith(allowed) || !Files.isDirectory(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Path must be a folder inside " + allowed);
        }
        return scanService.analyzeRepository(root);
    }
}
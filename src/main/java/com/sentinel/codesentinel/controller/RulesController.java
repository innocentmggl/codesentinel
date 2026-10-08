package com.sentinel.codesentinel.controller;

import com.sentinel.codesentinel.vectorstore.RuleIngestionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rules")
public class RulesController {

    private final RuleIngestionService ingestionService;

    public RulesController(RuleIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /** Upload a Markdown or plain-text style guide. */
    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> ingest(@RequestParam("file") MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        int chunks = ingestionService.ingest(file.getOriginalFilename(), content);
        return Map.of("file", String.valueOf(file.getOriginalFilename()), "chunksStored", chunks);
    }
}
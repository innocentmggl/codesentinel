package com.sentinel.application.port;

import java.util.List;

import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.review.Review;

public interface SnippetReviewer {

    Review review(String code, List<CodeChunk> relatedCode);
}

package com.sentinel.application.port;

import com.sentinel.domain.review.DiffReviewRequest;
import com.sentinel.domain.review.Review;

public interface DiffReviewer {

    Review review(DiffReviewRequest request);
}

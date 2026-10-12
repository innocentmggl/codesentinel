package com.sentinel.application.pullrequest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sentinel.domain.review.Finding;
import com.sentinel.domain.review.Severity;
import com.sentinel.domain.scm.ReviewComment;
import org.springframework.stereotype.Component;

@Component
class CommentFormatter {

    /**
     * @param suggestionFits true when the suggestion replaces exactly the commented lines and can therefore
     *                       be offered as an "Apply suggestion" button
     */
    String commentBody(Finding finding, boolean suggestionFits) {
        var body = new StringBuilder("**%s: %s**\n\n%s".formatted(
                finding.severity(), orEmpty(finding.title()), orEmpty(finding.explanation())));

        if (finding.suggestion() != null && !finding.suggestion().isBlank()) {
            String code = finding.suggestion().stripTrailing();
            String fence = fenceFor(code);
            body.append("\n\n");
            if (suggestionFits) {
                body.append(fence).append("suggestion\n");
            } else {
                body.append("Suggested change:\n\n").append(fence).append('\n');
            }
            body.append(code).append('\n').append(fence);
        }
        return body.toString();
    }

    String summary(List<ReviewComment> comments, int filesReviewed, List<String> filesSkipped) {
        var summary = new StringBuilder("### CodeSentinel review\n\n");
        if (comments.isEmpty()) {
            summary.append("No issues found in %d reviewed file(s).\n".formatted(filesReviewed));
        } else {
            summary.append("Reviewed %d file(s) and left %d comment(s): %s.\n"
                    .formatted(filesReviewed, comments.size(), countsBySeverity(comments)));
        }
        if (!filesSkipped.isEmpty()) {
            summary.append("\n<details><summary>Skipped files (%d)</summary>\n\n".formatted(filesSkipped.size()));
            filesSkipped.forEach(entry -> summary.append("- ").append(entry).append('\n'));
            summary.append("\n</details>\n");
        }
        summary.append("\n_Automated review. Findings can be wrong, so use your own judgement._");
        return summary.toString();
    }

    private static String countsBySeverity(List<ReviewComment> comments) {
        Map<Severity, Long> counts = comments.stream().collect(Collectors.groupingBy(
                ReviewComment::severity, () -> new EnumMap<>(Severity.class), Collectors.counting()));
        Severity[] severities = Severity.values();
        var parts = new ArrayList<String>();
        for (int i = severities.length - 1; i >= 0; i--) {
            Long count = counts.get(severities[i]);
            if (count != null) {
                parts.add(count + " " + severities[i]);
            }
        }
        return String.join(", ", parts);
    }

    /** A code fence longer than any backtick run inside the code, so the code cannot close it early. */
    private static String fenceFor(String code) {
        String fence = "```";
        while (code.contains(fence)) {
            fence += "`";
        }
        return fence;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}

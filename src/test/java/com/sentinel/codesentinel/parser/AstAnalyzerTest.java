package com.sentinel.codesentinel.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AstAnalyzerTest {

    private final AstAnalyzer analyzer = new AstAnalyzer();

    @Test
    void computesComplexityAndMetadata() {
        String source = """
                package demo;
                import java.util.List;
                // a comment that should be stripped
                class Sample {
                    int pick(int a, int b) {
                        if (a > 0 && b > 0) { return a; }
                        for (int i = 0; i < b; i++) { if (i == a) return i; }
                        return b;
                    }
                }
                """;

        ParsedSource parsed = analyzer.parse(source);

        assertThat(parsed.metrics().className()).isEqualTo("Sample");
        assertThat(parsed.metrics().packageName()).isEqualTo("demo");
        assertThat(parsed.metrics().imports()).containsExactly("java.util.List");
        assertThat(parsed.metrics().methods()).hasSize(1);
        // 1 + if + && + for + inner if
        assertThat(parsed.metrics().methods().get(0).complexity()).isEqualTo(5);
        assertThat(parsed.condensedSource()).doesNotContain("a comment that should be stripped");
    }

    @Test
    void rejectsInvalidJava() {
        assertThatThrownBy(() -> analyzer.parse("this is not java"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
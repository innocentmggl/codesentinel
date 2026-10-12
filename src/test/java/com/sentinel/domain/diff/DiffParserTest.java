package com.sentinel.domain.diff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DiffParserTest {

    private static final String PATCH = String.join("\n",
            "@@ -10,4 +10,5 @@ class Demo {",
            " context one",
            "-removed",
            "+added one",
            "+added two",
            " context two");

    @Test
    void numbersEveryLineThatExistsInTheNewFile() {
        AnnotatedPatch patch = DiffParser.parse(PATCH);

        assertThat(patch.numberedText()).contains(
                "[10]   context one",
                "[--] - removed",
                "[11] + added one",
                "[12] + added two",
                "[13]   context two");
    }

    @Test
    void tracksCommentableAndAddedLines() {
        AnnotatedPatch patch = DiffParser.parse(PATCH);

        assertThat(patch.commentableLines()).containsExactlyInAnyOrder(10, 11, 12, 13);
        assertThat(patch.addedLines()).containsExactlyInAnyOrder(11, 12);
        assertThat(patch.addedCode()).isEqualTo("added one\nadded two\n");
    }

    @Test
    void removedLinesAreNotCommentable() {
        AnnotatedPatch patch = DiffParser.parse(PATCH);

        assertThat(patch.accepts(9)).isFalse();
        assertThat(patch.accepts(14)).isFalse();
    }

    @Test
    void rangesMustBeFullyInsideTheDiff() {
        AnnotatedPatch patch = DiffParser.parse(PATCH);

        assertThat(patch.accepts(11, 13)).isTrue();
        assertThat(patch.accepts(12, 14)).isFalse();
        assertThat(patch.accepts(13, 11)).isFalse();
    }

    @Test
    void numbersEachHunkFromItsOwnHeader() {
        String patch = String.join("\n",
                "@@ -1,2 +1,2 @@", " a", "+b",
                "@@ -50,1 +60,2 @@", " c", "+d");

        assertThat(DiffParser.parse(patch).commentableLines()).containsExactlyInAnyOrder(1, 2, 60, 61);
    }

    @Test
    void treatsAnEmptyLineInsideAHunkAsBlankContext() {
        String patch = String.join("\n", "@@ -1,3 +1,3 @@", " a", "", "+b");

        AnnotatedPatch parsed = DiffParser.parse(patch);

        assertThat(parsed.commentableLines()).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(parsed.addedLines()).containsExactly(3);
    }

    @Test
    void ignoresNoNewlineMarkers() {
        String patch = String.join("\n", "@@ -1 +1 @@", "-old", "\\ No newline at end of file", "+new");

        assertThat(DiffParser.parse(patch).addedLines()).containsExactly(1);
    }

    @Test
    void anEmptyPatchHasNothingToComment() {
        assertThat(DiffParser.parse("").commentableLines()).isEmpty();
    }
}

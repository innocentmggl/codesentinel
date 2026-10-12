package com.sentinel.domain.diff;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the unified-diff hunks of one file into text a model can cite precisely.
 *
 * <p>Models are poor at counting lines in a raw diff, so every line that exists in the new file is
 * prefixed with its real line number:
 * <pre>
 * [42] + added line
 * [43]   unchanged line
 * [--] - removed line
 * </pre>
 * The parser also records which lines the hosting platform accepts review comments on, so invalid
 * model output can be discarded before it reaches the platform.
 */
public final class DiffParser {

    private static final Pattern HUNK_HEADER = Pattern.compile("^@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@.*$");

    private DiffParser() {
    }

    public static AnnotatedPatch parse(String patch) {
        var numbered = new StringBuilder();
        var addedCode = new StringBuilder();
        var commentable = new TreeSet<Integer>();
        var added = new TreeSet<Integer>();

        String[] lines = patch.split("\r?\n", -1);
        int newLine = 0;
        boolean insideHunk = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher header = HUNK_HEADER.matcher(line);
            if (header.matches()) {
                newLine = Integer.parseInt(header.group(1));
                insideHunk = true;
                numbered.append(line).append('\n');
                continue;
            }
            if (!insideHunk) {
                continue;
            }
            if (line.isEmpty()) {
                // The end of the patch, or a blank context line whose single leading space was stripped.
                if (i == lines.length - 1) {
                    continue;
                }
                line = " ";
            }

            String content = line.substring(1);
            switch (line.charAt(0)) {
                case '+' -> {
                    numbered.append("[%d] + %s\n".formatted(newLine, content));
                    addedCode.append(content).append('\n');
                    commentable.add(newLine);
                    added.add(newLine);
                    newLine++;
                }
                case ' ' -> {
                    numbered.append("[%d]   %s\n".formatted(newLine, content));
                    commentable.add(newLine);
                    newLine++;
                }
                case '-' -> numbered.append("[--] - ").append(content).append('\n');
                default -> {
                    // "\ No newline at end of file" and similar markers carry no code.
                }
            }
        }
        return new AnnotatedPatch(
                numbered.toString(), Set.copyOf(commentable), Set.copyOf(added), addedCode.toString());
    }
}

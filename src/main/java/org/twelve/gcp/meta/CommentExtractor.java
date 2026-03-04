package org.twelve.gcp.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts comments from Outline source code.
 * <p>
 * Supports single-line ({@code //}) and multi-line ({@code /* *\/}) comments.
 * Used by {@link MetaExtractor} to attach JavaDoc-like descriptions to schema elements.
 */
public final class CommentExtractor {

    private static final Pattern SINGLE_LINE = Pattern.compile("//[^\n]*", Pattern.MULTILINE);
    private static final Pattern MULTI_LINE = Pattern.compile("/\\*[\\s\\S]*?\\*/", Pattern.MULTILINE);

    /**
     * A comment block with its end position (exclusive) and trimmed text.
     */
    public record Comment(long endOffset, String text) {}

    /**
     * Extracts all comments from the source and returns them sorted by end offset.
     */
    public static List<Comment> extractAll(String source) {
        if (source == null || source.isEmpty()) return List.of();

        List<Comment> result = new ArrayList<>();

        Matcher sl = SINGLE_LINE.matcher(source);
        while (sl.find()) {
            String raw = sl.group();
            String text = raw.substring(2).trim();
            result.add(new Comment(sl.end(), text));
        }

        Matcher ml = MULTI_LINE.matcher(source);
        while (ml.find()) {
            String raw = ml.group();
            // Strip /* and */; normalize Javadoc-style leading * on each line
            String inner = raw.substring(2, raw.length() - 2);
            String text = inner.lines()
                    .map(line -> line.replaceFirst("^\\s*\\*\\s*", "").trim())
                    .filter(s -> !s.isEmpty())
                    .reduce((a, b) -> a + " " + b).orElse("");
            result.add(new Comment(ml.end(), text));
        }

        result.sort((a, b) -> Long.compare(a.endOffset, b.endOffset));
        return result;
    }

    /**
     * Finds the offset of the first non-comment, non-whitespace character in the source.
     * Used to locate module/statement start when a block comment appears at file beginning.
     */
    public static long startOfFirstContent(String source) {
        if (source == null || source.isEmpty()) return 0;
        int i = 0;
        int len = source.length();
        while (i < len) {
            while (i < len && Character.isWhitespace(source.charAt(i))) i++;
            if (i >= len) return len;
            if (source.startsWith("/*", i)) {
                int end = source.indexOf("*/", i);
                i = end >= 0 ? end + 2 : len;
            } else if (source.startsWith("//", i)) {
                int nl = source.indexOf('\n', i);
                i = nl >= 0 ? nl + 1 : len;
            } else {
                return i;
            }
        }
        return i;
    }

    /**
     * Returns the comment immediately preceding the given offset, or null if none.
     * A comment "precedes" offset if its end is before offset and only whitespace
     * exists between the comment end and offset.
     */
    public static String precedingComment(String source, long offset) {
        if (source == null || offset <= 0) return null;
        List<Comment> comments = extractAll(source);

        Comment best = null;
        for (Comment c : comments) {
            if (c.endOffset > offset) break;
            // Check only whitespace between c.endOffset and offset
            if (offset <= source.length()) {
                String between = source.substring((int) c.endOffset, (int) offset);
                if (between.isBlank()) {
                    best = c;
                }
            } else {
                best = c;
            }
        }
        return best != null ? best.text : null;
    }
}

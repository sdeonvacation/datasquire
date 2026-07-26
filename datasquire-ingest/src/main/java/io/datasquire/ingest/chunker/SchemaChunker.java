package io.datasquire.ingest.chunker;

import io.datasquire.core.schema.ChunkKind;
import io.datasquire.core.schema.SchemaChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses schema markdown documentation into discrete {@link SchemaChunk} instances
 * suitable for embedding and vector storage.
 */
@Component
public class SchemaChunker {

    private static final Pattern H2_PATTERN = Pattern.compile("^## (.+)$", Pattern.MULTILINE);
    private static final Pattern H1_CATEGORICAL = Pattern.compile(
            "^# Known categorical values$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern EXAMPLE_BLOCK = Pattern.compile(
            "<example>\\s*<question>(.*?)</question>\\s*<sql>(.*?)</sql>\\s*</example>",
            Pattern.DOTALL);

    /**
     * Chunks the given markdown content into schema chunks by kind.
     *
     * @param markdownContent full schema documentation in markdown
     * @return ordered list of schema chunks
     */
    public List<SchemaChunk> chunk(String markdownContent) {
        if (markdownContent == null || markdownContent.isBlank()) {
            return List.of();
        }

        List<SchemaChunk> chunks = new ArrayList<>();

        chunks.addAll(extractExamples(markdownContent));
        chunks.addAll(extractEnums(markdownContent));
        chunks.addAll(extractOverviewAndTables(markdownContent));

        return chunks;
    }

    private List<SchemaChunk> extractOverviewAndTables(String content) {
        List<SchemaChunk> chunks = new ArrayList<>();

        // Strip enum section and example blocks before processing headings
        String stripped = stripEnumSection(content);
        stripped = EXAMPLE_BLOCK.matcher(stripped).replaceAll("");

        Matcher h2Matcher = H2_PATTERN.matcher(stripped);
        int firstH2Start = -1;

        // Find all H2 sections for TABLE chunks
        List<int[]> h2Positions = new ArrayList<>();
        List<String> h2Titles = new ArrayList<>();

        while (h2Matcher.find()) {
            if (firstH2Start == -1) {
                firstH2Start = h2Matcher.start();
            }
            h2Positions.add(new int[]{h2Matcher.start(), h2Matcher.end()});
            h2Titles.add(h2Matcher.group(1).trim());
        }

        // OVERVIEW: everything before first ## heading
        if (firstH2Start > 0) {
            String overviewContent = stripped.substring(0, firstH2Start).trim();
            if (!overviewContent.isBlank()) {
                chunks.add(new SchemaChunk("overview", ChunkKind.OVERVIEW,
                        "Schema Overview", overviewContent, overviewContent));
            }
        } else if (h2Positions.isEmpty()) {
            // No H2 headings - entire content is overview
            String trimmed = stripped.trim();
            if (!trimmed.isBlank()) {
                chunks.add(new SchemaChunk("overview", ChunkKind.OVERVIEW,
                        "Schema Overview", trimmed, trimmed));
            }
        }

        // TABLE: each ## section
        for (int i = 0; i < h2Positions.size(); i++) {
            int start = h2Positions.get(i)[0];
            int end = (i + 1 < h2Positions.size()) ? h2Positions.get(i + 1)[0] : stripped.length();
            String sectionContent = stripped.substring(start, end).trim();
            String title = h2Titles.get(i);
            String chunkId = "table:" + slugify(title);

            chunks.add(new SchemaChunk(chunkId, ChunkKind.TABLE, title, sectionContent, sectionContent));
        }

        return chunks;
    }

    private List<SchemaChunk> extractEnums(String content) {
        List<SchemaChunk> chunks = new ArrayList<>();

        Matcher h1Matcher = H1_CATEGORICAL.matcher(content);
        if (h1Matcher.find()) {
            int start = h1Matcher.end();
            // Enum section ends at next H1 or end of content
            Pattern nextH1 = Pattern.compile("^# ", Pattern.MULTILINE);
            Matcher nextMatcher = nextH1.matcher(content.substring(start));
            int end = nextMatcher.find() ? start + nextMatcher.start() : content.length();

            String enumContent = content.substring(start, end).trim();
            if (!enumContent.isBlank()) {
                chunks.add(new SchemaChunk("enum:categorical-values", ChunkKind.ENUM,
                        "Known categorical values", enumContent, enumContent));
            }
        }

        return chunks;
    }

    private List<SchemaChunk> extractExamples(String content) {
        List<SchemaChunk> chunks = new ArrayList<>();
        Matcher matcher = EXAMPLE_BLOCK.matcher(content);

        while (matcher.find()) {
            String question = matcher.group(1).trim();
            String sql = matcher.group(2).trim();
            String fullContent = "Question: " + question + "\n\nSQL:\n```sql\n" + sql + "\n```";
            String chunkId = "example:" + slugify(question);

            // embedText = question only (NL signal for semantic matching)
            chunks.add(new SchemaChunk(chunkId, ChunkKind.EXAMPLE, question, fullContent, question));
        }

        return chunks;
    }

    private String stripEnumSection(String content) {
        Matcher h1Matcher = H1_CATEGORICAL.matcher(content);
        if (!h1Matcher.find()) {
            return content;
        }
        int start = h1Matcher.start();
        Pattern nextH1 = Pattern.compile("^# ", Pattern.MULTILINE);
        Matcher nextMatcher = nextH1.matcher(content.substring(h1Matcher.end()));
        int end = nextMatcher.find() ? h1Matcher.end() + nextMatcher.start() : content.length();
        return content.substring(0, start) + content.substring(end);
    }

    static String slugify(String input) {
        String slug = input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > 80) {
            slug = slug.substring(0, 80).replaceAll("-+$", "");
        }
        return slug;
    }
}

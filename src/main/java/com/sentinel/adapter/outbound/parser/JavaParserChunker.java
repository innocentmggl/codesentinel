package com.sentinel.adapter.outbound.parser;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.sentinel.application.port.CodeChunker;
import com.sentinel.common.Texts;
import com.sentinel.domain.code.ChunkKind;
import com.sentinel.domain.code.CodeChunk;
import com.sentinel.domain.code.SourceFile;
import org.springframework.stereotype.Component;

/**
 * Splits Java source into method-level chunks using JavaParser.
 * Files that cannot be parsed yield no chunks instead of failing a whole indexing run.
 */
@Component
class JavaParserChunker implements CodeChunker {

    private static final String ANONYMOUS_OWNER = "<anonymous>";

    private final ParserConfiguration configuration =
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
    private final int maxChunkChars;

    JavaParserChunker(ChunkingProperties properties) {
        this.maxChunkChars = properties.maxChunkChars();
    }

    @Override
    public List<CodeChunk> chunk(SourceFile file) {
        // JavaParser instances are not thread-safe, so every call gets its own.
        return new JavaParser(configuration).parse(file.content()).getResult()
                .map(unit -> Stream.<CallableDeclaration<?>>concat(
                                unit.findAll(MethodDeclaration.class).stream()
                                        .filter(method -> method.getBody().isPresent()),
                                unit.findAll(ConstructorDeclaration.class).stream())
                        .flatMap(callable -> toChunk(file.path(), callable).stream())
                        .toList())
                .orElse(List.of());
    }

    private Optional<CodeChunk> toChunk(String path, CallableDeclaration<?> callable) {
        return callable.getRange().map(range -> new CodeChunk(
                path,
                ownerOf(callable),
                callable.getDeclarationAsString(false, false, true),
                callable instanceof ConstructorDeclaration ? ChunkKind.CONSTRUCTOR : ChunkKind.METHOD,
                range.begin.line,
                range.end.line,
                Texts.truncate(sourceOf(callable), maxChunkChars)));
    }

    /** The declaration exactly as written, which keeps the author's formatting and inline comments. */
    private static String sourceOf(CallableDeclaration<?> callable) {
        return callable.getTokenRange()
                .map(Object::toString)
                .orElseGet(callable::toString);
    }

    private static String ownerOf(CallableDeclaration<?> callable) {
        return callable.getParentNode()
                .filter(parent -> parent instanceof TypeDeclaration<?>)
                .map(parent -> ((TypeDeclaration<?>) parent).getNameAsString())
                .orElse(ANONYMOUS_OWNER);
    }
}
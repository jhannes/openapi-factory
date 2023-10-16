package org.openapifactory.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openapifactory.api.StringUtil.INDENT;

class StringUtilTest {

    @Test
    void itShouldInterpretStringsNormally() {
        assertEquals("hello world!\n", INDENT."""
                hello \{"WORLD".toLowerCase()}!
                """);
    }

    @Test
    void itShouldOmitEmptyOptionals() {
        assertEquals("hello !\n", INDENT."""
                \{Optional.of("hello")} \{Optional.empty()}!
                """);
    }

    @Test
    void itShouldJoinStreams() {
        assertEquals("result: first second third!\n", INDENT."""
                result: \{Stream.of("first ", "second ", "third")}!
                """);
    }
  
    @Test
    void itShouldJoinCollections() {
        assertEquals("result: first second third!\n", INDENT."""
                result: \{List.of("first ", "second ", "third")}!
                """);
    }

    @Test
    void itShouldOmitWholeLineIfEmpty() {
        var title = Optional.of("* Title");
        var summary = Optional.empty();
        assertEquals("""
                /**
                 * Title
                 *
                 */
                /* FILE COMMENT */
                """, INDENT."""
                /**
                 \{title}
                 *
                 \{summary}
                 */
                \{"/* FILE COMMENT */"}
                """);
    }

    @Test
    void itIndentsAllCollectionMembers() {
        var options = new TreeSet<>(Set.of("first", "third", "second"));
        assertEquals("""
                Options:
                    first
                    second
                    third
                 That is all
                """, INDENT."""
                Options:
                    \{options}
                 That \{"is"} all
                """);
    }

    @Test
    void itIndentsAllLines() {
        var entries = List.of(
                "first line",
                "a new line with some\nmore text on it",
                "   two indented\n   lines"
        );
        assertEquals("""
                HEADING
                    first line
                    a new line with some
                    more text on it
                       two indented
                       lines
                FOOTER
                """, INDENT."""
                HEADING
                    \{entries}
                FOOTER
                """);
    }

    @Test
    void itIndentsFirstLines() {
        assertEquals("""
                   hello
                   code
                   world
                FOOTER
                """, INDENT."""
                   \{List.of("hello", "code\nworld")}
                FOOTER
                """);
    }

    @Test
    void itIndentsCodeCorrectly() {
        var functionBody = """
                return await this.fetch(
                    this.basePath + "/pet/locations", params
                );
                """;
        var operation = "getPetLocations";
        var responseType = "PetLocationsDto";

        var result = INDENT."""
            /**
             *
             * @throws {HttpError}
             */
            public async \{operation}(params: RequestCallOptions = {}): Promise<\{ responseType }> {
                \{functionBody}
            }
            """;
        assertEquals("""
                /**
                 *
                 * @throws {HttpError}
                 */
                public async getPetLocations(params: RequestCallOptions = {}): Promise<PetLocationsDto> {
                    return await this.fetch(
                        this.basePath + "/pet/locations", params
                    );
                }
                """, result);
    }

}
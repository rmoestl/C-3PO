package org.c_3po.generation

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Paths

/**
 * Spock unit tests for {@link IgnorablesMatcher}.
 */
class IgnorablesMatcherTest extends Specification {
    @Shared ignorableGlobPatterns = [
            ".git",
            ".idea",
            "README.md",
            "tasks.md",
            "**/*.sass",
            ".gitignore",
            "*.txt",
            "_layouts"
    ]

    @Unroll
    def "test if matcher with absolute base path matches #path to #isMatching" (String path, boolean isMatching) {
        setup:
        def basePath = Paths.get("D:/Programmierung/DVB-T2-Umstellung/development")
        def matcher = IgnorablesMatcher.from(basePath, ignorableGlobPatterns)

        expect:
        isMatching == matcher.matches(Paths.get(path))

        where:
        path | isMatching
        ".git" | true
        "/.git" | false
        "css/.git" | false
        "D:/Programmierung/DVB-T2-Umstellung/development/.git" | true
        "D:/Programmierung/DVB-T2-Umstellung/documentation/.git" | false
        "DVB-T2-Umstellung/development/.git" | false
        ".gitignore" | true
        "css/.gitignore" | false
        "main.sass" | false
        "sass/main.sass" | true
        "sass/vendor/main.sass" | true
        "test.txt" | true
        "notes.txt" | true
    }

    @Unroll
    def "test if matcher with relative base path matches #path to #isMatching" (String path, boolean isMatching) {
        setup:
        def basePath = Paths.get("site")
        def matcher = IgnorablesMatcher.from(basePath, ignorableGlobPatterns)

        expect:
        isMatching == matcher.matches(Paths.get(path))

        where:
        path | isMatching
        "./_layouts" | true
        "_layouts" | true
        "site/_layouts" | true
        "site/site/_layouts" | false
    }

    @Unroll
    def "test if matcher with relative base path preceded by a \"./\" matches #path to #isMatching" (String path, boolean isMatching) {
        setup:
        def basePath = Paths.get("./site")
        def matcher = IgnorablesMatcher.from(basePath, ignorableGlobPatterns)

        expect:
        isMatching == matcher.matches(Paths.get(path))

        where:
        path | isMatching
        "./_layouts" | true
        "_layouts" | true
        "site/_layouts" | true
        "site/site/_layouts" | false
    }
}

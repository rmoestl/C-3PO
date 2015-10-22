package org.c_3po.util

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for {@StringUtils}.
 */
@Unroll
class StringUtilsTest extends Specification {
    def "test that .isBlank returns '#expectedResult' for '#input'"(String input, boolean expectedResult) {
        expect:
        StringUtils.isBlank(input) == expectedResult

        where:
        input | expectedResult
        null | true
        "" | true
        " " | true
        "   " | true
        "hello" | false
        " hello " | false
    }

    def "test that .join ensures that given delimiter is only included once between the parts of the string"() {
        def delimiter = "/"
        def expectedResult = "a/b/c"

        expect:
        StringUtils.trimmedJoin(delimiter, strings as String[]) == expectedResult

        where:

        strings | _
        ["a", "b", "c"] | _
        ["a/", "b/", "c"] | _
        ["a", "b/", "c"] | _
        ["a/", "b", "c"] | _
        ["a/", "/b", "/c"] | _
        ["a/", "b", "/c"] | _
    }
}

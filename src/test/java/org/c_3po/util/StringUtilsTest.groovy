package org.c_3po.util

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for {@StringUtils}.
 */
@Unroll
class StringUtilsTest extends Specification {
    def "test that isBlank returns #expectedResult for '#input'"(String input, boolean expectedResult) {
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
}

package org.c_3po.cmd

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for {@CmdArguments}.
 */
@Unroll
class CmdArgumentsTest extends Specification {
    def "test that .validate returns '#expectedResult' for '#src' and '#dest'"(String src, String dest, boolean expectedResult) {
        def cmdArgs = new CmdArguments(src, dest, false, false, false)

        expect:
        cmdArgs.validate() == expectedResult

        where:
        src | dest | expectedResult
        "" | "" | false
        "." | "." | false
        "./build" | "../development/build" | false
        "." | "../development/build" | true
        "." | "./a-not-existing-folder" | true
        "./a-not-existing-folder" | "." | true
        "./a-not-existing-folder" | "./a-not-existing-folder" | false
    }
}

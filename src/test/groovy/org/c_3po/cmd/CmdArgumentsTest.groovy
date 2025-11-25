package org.c_3po.cmd

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for {@CmdArguments}.
 */
@Unroll
class CmdArgumentsTest extends Specification {
    def ".validate returns '#expectedResult' for src '#src' and dest '#dest'"(String src, String dest, boolean expectedResult) {
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

    def ".validate returns false if new draft is set but new draft dir is missing"() {
        expect:
        !new CmdArguments("./site", "../_build", false, true, "", "", false, "", false, false).validate()
    }

    def ".validate returns false if new draft dir does not exist in source dir"() {
        expect:
        !new CmdArguments("src/test/resources/test-project-src",
                "src/test/resources/test-project-build", false, true, "blogg", "", false, "", false, false).validate()
    }

    def ".parse skips setting new draft dir if value arg starts like a flag"(args) {
        when:
        def cmdArgs = CmdArguments.parse(args as String[])

        then:
        cmdArgs.isNewDraftModeEnabled()
        cmdArgs.getNewDraftDir() == ""

        where:
        args | _
        ["-n", "-a"] | _
        ["-n", "--fingerprint"] | _
    }

    def ".parse skips setting new draft tag if value arg starts like a flag"(args) {
        when:
        def cmdArgs = CmdArguments.parse(args as String[])

        then:
        cmdArgs.getNewDraftTag() == ""

        where:
        args | _
        ["-t", "-src"] | _
        ["-t", "--fingerprinting"] | _
    }
}

package org.c_3po.cmd

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for {@CmdArguments}.
 */
@Unroll
class CmdArgumentsTest extends Specification {
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

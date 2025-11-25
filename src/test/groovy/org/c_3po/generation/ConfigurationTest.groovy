package org.c_3po.generation

import org.c_3po.cmd.CmdArguments
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for {@Configuration}.
 */
@Unroll
class ConfigurationTest extends Specification {
    def ".validate returns '#expectedResult' for src '#src' and dest '#dest'"(String src, String dest, boolean expectedResult) {
        def config = Configuration.deriveFrom(new CmdArguments(src, dest, false, false, false))

        expect:
        config.validate() == expectedResult

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
        !Configuration.deriveFrom(
                new CmdArguments("./site", "../_build", false, true, "", "", false, "", false, false)).validate()
    }

    def ".validate returns false if new draft dir does not exist in source dir"() {
        expect:
        !Configuration.deriveFrom(new CmdArguments("src/test/resources/test-project-src",
                "src/test/resources/test-project-build", false, true, "blogg", "", false, "", false, false)).validate()
    }
}

package org.c_3po.util

import spock.lang.Specification

import java.nio.file.Paths

import static org.c_3po.util.ChecksumCalculator.computeSha1Hash
import static org.c_3po.util.ChecksumCalculator.encodeHexString

class ChecksumCalculatorTest extends Specification {

    def "test that .computeSha1Hash computes the correct sha-1 hash of a given file"() {
        expect:
        encodeHexString(computeSha1Hash(Paths.get(filePath))) == expectedSha1Hash

        where:
        filePath | expectedSha1Hash
        "src/test/resources/test-project-src/css/main.scss" | "e6ce2eaf06d4aa5c64169a225282a19f55ced190"
    }
}

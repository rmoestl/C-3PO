package org.c_3po.generation.meta

import org.c_3po.generation.IFileClassifier
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

class ExtractionFromHTMLSpec extends Specification {
    @Shared IFileClassifier fileClassifier = Stub(IFileClassifier) {
        isHTML(_ as Path) >> true
    }
    @Shared def extractor = MetadataExtractor.getInstance(fileClassifier)

    def 'Extracting the title #rule'(String html, title, String filename) {
        when:
        def metadata = extractFromHTML(html, filename)

        then:
        metadata.title() == title

        where:
        [rule, html, title, filename] << [
            [
                rule : 'gives <h1> precedence over <title>',
                html : '''<html lang="en">
                          <head>
                            <title>BMW M3 Touring</title>
                          </head>
                          <body>
                            <h1>All about the BMW M3 Touring</h1>
                            <p>This car is the dream of...</p>
                          </body>
                          </html>''',
                title: 'All about the BMW M3 Touring'
            ],
            [
                rule : 'falls back to <title> if no <h1> exists',
                html : '''<html lang="en">
                          <head>
                            <title>BMW M3 Touring</title>
                          </head>
                          <body>
                            <p>This car is the dream of...</p>
                          </body>
                          </html>''',
                title: 'BMW M3 Touring'
            ],
            [
                rule    : 'falls back to converting the file name if neither <h1> nor <title> exist',
                html    : '''<html lang="en">
                             <body>
                               <p>This car is the dream of...</p>
                             </body>
                             </html>''',
                title   : 'Bmw m3 touring review',
                filename: 'bmw-m3-touring-review.md'
            ],
            [
                rule    : 'ignores any subsequent <h1> instances if one was already found',
                html    : '''<html lang="en">
                             <body>
                               <h1>All about the BMW M3 Touring</h1>
                               <p>This car is the dream of...</p>
                               <h1>An accidental second first-level heading</h1>
                             </body>
                             </html>''',
                title   : 'All about the BMW M3 Touring'
            ]
        ]
    }

    def 'Extracting the publish date #rule'(String html, String date, String filename) {
        when:
        def metadata = extractFromHTML(html, filename)

        then:
        metadata.publishDate() == LocalDate.parse(date)

        where:
        [rule, html, title, date, filename] << [
            [
                rule    : 'extracts it from OpenGraph article:published_time property',
                html    : '''<html lang="en">
                             <head>
                                <meta property="article:published_time" content="2026-02-04T14:30:00+00:00">
                             </head>
                             <body>
                             </body>
                             </html>''',
                date   : '2026-02-04'
            ],
            [
                rule    : ('falls back to the file\'s last modification date if the OpenGraph '
                    + 'article:published_time property is malformed'),
                html    : '''<html lang="en">
                             <head>
                                <meta property="article:published_time" content="some-odd-garbage">
                             </head>
                             <body></body>
                             </html>''',
                date   : LocalDate.now()
            ],
            [
                rule    : ('falls back to the file\'s last modification date if the OpenGraph '
                    + 'article:published_time property is missing'),
                html    : '''<html lang="en">
                             <body></body>
                             </html>''',
                date   : LocalDate.now()
            ]
        ]
    }

    def extractFromHTML(String contents, String filename = null) {
        extractor.extract(createTmpHTMLFile(contents, filename))
    }

    def createTmpHTMLFile(String contents, String filename) {
        def tmpFilePath = filename != null
                ? Files.createTempDirectory('c3po-testing').resolve(filename)
                : Files.createTempFile('c3po-testing_', '.html')
        Files.writeString(tmpFilePath, contents)
        return tmpFilePath
    }
}

package org.c_3po.generation.meta

import org.c_3po.generation.IFileClassifier
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.stream.Collectors

class ExtractionFromMarkdownSpec extends Specification {
    @Shared IFileClassifier fileClassifier = Stub(IFileClassifier) {
        isMarkdown(_ as Path) >> true
    }
    @Shared def extractor = MetadataExtractor.getInstance(fileClassifier)

    def "Extracting the title #rule"(String md, title, String filename) {
        given:
        md = trimLeadingWhitespaceInLines(md)

        when:
        def metadata = extractFromMarkdown(md, filename)

        then:
        metadata.title() == title

        where:
        [rule, md, title, filename] << [
            [
                rule : 'gives an H1 precedence over a $meta-title',
                md   : '''$meta-title: BMW M3 Touring
                          $meta-description: This is a serious review of the first BMW M3 touring.
                          
                          # All about the BMW M3 Touring
                            
                          This car is the dream of...''',
                title: 'All about the BMW M3 Touring'
            ],
            [
                rule : 'falls back to $meta-title if no H1 exists',
                md   : '''$meta-title: BMW M3 Touring
                          $meta-description: This is a serious review of the first BMW M3 touring.
                          
                          This car is the dream of...''',
                title: 'BMW M3 Touring'
            ],
            [
                rule : 'extracts $meta-title even if it is defined at the end of the file',
                md   : '''This car is the dream of...
                          
                          $meta-description: This is a serious review of the first BMW M3 touring.
                          $meta-title: BMW M3 Touring''',
                title: 'BMW M3 Touring'
            ],
            [
                rule    : 'falls back to converting the filename if no $meta-title and H1 exists',
                md      : '''$meta-description: This is a serious review of the first BMW M3 touring.
            
                             This car is the dream of...''',
                title   : 'Bmw m3 touring review',
                filename: 'bmw-m3-touring-review.md'
            ],
            [
                rule : 'ignores any subsequent H1 instances if one was already found',
                md   : '''# All about the BMW M3 Touring
            
                          ## Interior
                          
                          # Exterior''',
                title: 'All about the BMW M3 Touring'
            ],
            [
                rule : 'a $meta-title does not need to exist',
                md   : '''$meta-description: This is a serious review of the first BMW M3 touring.
            
                          # All about the BMW M3 Touring
                          
                          This car is the dream of...''',
                title: 'All about the BMW M3 Touring'
            ]
        ]
    }

    def "Extracting the publish date #rule"(String md, String date, String filename) {
        given:
        md = trimLeadingWhitespaceInLines(md)

        when:
        def metadata = extractFromMarkdown(md, filename)

        then:
        metadata.publishDate() == LocalDate.parse(date)

        where:
        [rule, md, date, filename] << [
            [
                rule : 'extracts the publish date from $meta-publishDate',
                md   : '''$meta-title: BMW M3 Touring
                          $meta-description: This is a serious review of the first BMW M3 touring.
                          $meta-publishDate: 2026-02-03
                          
                          # All about the BMW M3 Touring
                          
                          This car is the dream of...''',
                date : '2026-02-03'
            ],
            [
                rule: ('extracts the publish date from $meta-publishDate even if it is '
                    + 'defined at the end of the file'),
                md  : '''$meta-title: BMW M3 Touring
                         $meta-description: This is a serious review of the first BMW M3 touring.

                         # All about the BMW M3 Touring

                         This car is the dream of...

                         $meta-publishDate: 2026-02-03''',
                date: '2026-02-03'
            ],
            [
                rule: ('falls back to the file\'s last modification date if '
                    + '$meta-publishDate isn\'t defined'),
                md  : '''$meta-title: BMW M3 Touring
                         $meta-description: This is a serious review of the first BMW M3 touring.

                         # All about the BMW M3 Touring

                         This car is the dream of...''',
                date: LocalDate.now()
            ],
            [
                rule: ('falls back to the file\'s last modification date if '
                    + '$meta-publishDate can\'t be parsed to a date'),
                md  : '''$meta-publishDate: The quick brown fox
                         $meta-title: BMW M3 Touring
                         $meta-description: This is a serious review of the first BMW M3 touring.

                         # All about the BMW M3 Touring

                         This car is the dream of...''',
                date: LocalDate.now()
            ],
            [
                rule: ('is capable to deal with trailing whitespace in $meta-publishDate '
                    + 'meta-publishDate can\'t be parsed to a date'),
                md  : '''$meta-publishDate: 2018-12-14 
                         
                         # All about the BMW M3 Touring''',
                date: '2018-12-14'
            ]
        ]
    }

    def trimLeadingWhitespaceInLines(String s) {
        def lines = s.split("\n")
        lines.toList().stream().map { l -> l.stripLeading() }.collect(Collectors.toList()).join("\n")
    }

    def extractFromMarkdown(String contents, String filename = null) {
        extractor.extract(createTmpMarkdownFile(contents, filename))
    }
    
    def createTmpMarkdownFile(String contents, String filename) {
        def tmpFilePath = filename != null
                ? Files.createTempDirectory('c3po-testing').resolve(filename)
                : Files.createTempFile('c3po-testing_', '.md')
        Files.writeString(tmpFilePath, contents)
        return tmpFilePath
    }
}

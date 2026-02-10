package org.c_3po.generation.meta;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PageTree {
    private static final Comparator<Page> PAGE_COMPARATOR = (page1, page2) -> {
        var comparisonResult = page2.publishDate().compareTo(page1.publishDate());
        return comparisonResult != 0
                ? comparisonResult
                : page1.title().compareTo(page2.title());
    };

    private final Map<Path, Set<Page>> parentPathsToPages = new HashMap<>();

    public void add(Page page) {
        var parentPath = page.url().getParent();
        var pagesOfParent = parentPathsToPages.computeIfAbsent(parentPath, k -> new TreeSet<>(PAGE_COMPARATOR));
        pagesOfParent.add(page);
    }

    /**
     * Get all {@link Page} objects sharing the passed parent path ordered by
     * {@link Page#publishDate()} in descending and {@link Page#title()} in
     * ascending order.
     * Pages in a sub-path of the given parent path are not returned.
     *
     * @param parentPath the parent path
     * @return a list of all pages in the given parent path
     */
    public List<Page> getPagesIn(Path parentPath) {
        Set<Page> pageSet = parentPathsToPages.getOrDefault(parentPath, Collections.emptySet());
        return new ArrayList<>(pageSet);
    }

    public List<Page> getPagesIn(String parentPath) {
        return getPagesIn(Paths.get(parentPath));
    }
}

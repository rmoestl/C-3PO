package org.c_3po.generation.meta;

import java.nio.file.Path;
import java.time.LocalDate;

public record Page(Path url, String title, LocalDate publishDate) { }

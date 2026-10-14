package com.example.adventurebook.support;

import com.example.adventurebook.junit.TafExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Only registers the cross-cutting extension (log context, failure artefacts, once-per-run setup/cleanup).
 * It deliberately creates nothing: a test class declares just the services it uses, e.g.
 * {@code private final BookService books = new BookService();}
 */
@ExtendWith(TafExtension.class)
public abstract class BaseApiTest {
}

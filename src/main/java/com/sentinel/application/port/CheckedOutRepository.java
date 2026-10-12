package com.sentinel.application.port;

import java.nio.file.Path;

public interface CheckedOutRepository extends AutoCloseable {

    Path root();

    /** The repository's own name, used as the default project name. */
    String name();

    @Override
    void close();
}

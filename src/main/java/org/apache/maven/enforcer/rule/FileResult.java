package org.apache.maven.enforcer.rule;

import javax.annotation.Nonnull;
import java.nio.file.Path;

/**
 * Class to hold some information about a file.
 */
class FileResult {

    @Nonnull
    private final Path path;
    @Nonnull
    private final long size;
    @Nonnull
    private final long lastModified;

    private FileResult(@Nonnull final Builder builder) {
        this.path = builder.path;
        this.size = builder.size;
        this.lastModified = builder.lastModified;
    }

    public static class Builder {
        @Nonnull
        private Path path;
        @Nonnull
        private long size = 0;
        @Nonnull
        private long lastModified = 0;

        Builder(@Nonnull final Path value) {
            this.path = value;
        }

        public Builder size(@Nonnull final long value) {
            this.size = value;
            return this;
        }

        public Builder lastModified(@Nonnull final long value) {
            this.lastModified = value;
            return this;
        }

        FileResult build() {
            return new FileResult(this);
        }
    }

    @Nonnull
    public Path getPath() {
        return path;
    }

    @Nonnull
    public long getSize() {
        return size;
    }

    @Nonnull
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public String toString() {
        return "FileResult{" + "path=" + path.toString()
                + ", size=" + size + ", lastModified=" + lastModified + "}";
    }
}

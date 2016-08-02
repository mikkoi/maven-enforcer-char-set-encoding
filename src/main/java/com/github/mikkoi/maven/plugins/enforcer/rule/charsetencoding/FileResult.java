package com.github.mikkoi.maven.plugins.enforcer.rule.charsetencoding;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Internal class for gathering all files.
 */
final class FileResult implements Comparable<FileResult> {

    @Nonnull
    private final Path path;

    private final long lastModified;

    private FileResult(@Nonnull final Builder builder) {
        this.path = builder.path;
        this.lastModified = builder.lastModified;
    }

    @Nonnull
    Path getPath() {
        return path;
    }

    long getLastModified() {
        return lastModified;
    }

    @Override
    public int compareTo(FileResult that) {
        return ComparisonChain.start()
                .compare(this.path.toString(), that.path.toString())
                .compare(this.lastModified, that.lastModified)
                .result();
    }

    /**
     * @return "FileResult{path=s,lastModified=1}"
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper("FileResult")
                .add("path", path.toString())
                .add("lastModified", lastModified)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, lastModified);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return Objects.equals(this, o);
    }

    static class Builder {
        @Nonnull
        private Path path;
        private long lastModified = 0;

        Builder(@Nonnull final Path value) {
            this.path = value;
        }

        Builder lastModified(final long value) {
            this.lastModified = value;
            return this;
        }

        FileResult build() {
            return new FileResult(this);
        }
    }
}

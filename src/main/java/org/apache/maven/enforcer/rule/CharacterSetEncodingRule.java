package org.apache.maven.enforcer.rule;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.google.common.io.ByteStreams;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Checks file encodings to see if they match the parameter
 * or Maven property project.build.sourceEncoding.
 * If file requireEncoding can not be determined it is skipped.
 */
public final class CharacterSetEncodingRule implements EnforcerRule {

    /**
     * Validate files must match this requireEncoding.
     * Default: ${project.builder.sourceEncoding}.
     */
    @Nullable
    private String requireEncoding = null;

    /**
     * Directory to search for files
     */
    @Nullable
    private String directory = null;

    /**
     * Regular Expression to match file names against for filtering in
     */
    @Nullable
    private String includeRegex = null;

    /**
     * Regular Expression to match file names against for filtering out
     * Can be used together with includeRegex.
     * includeRegex will first pick files in,
     * then excludeRegex will filter out files from the selected ones.
     */
    @Nullable
    private String excludeRegex = null;

    public void execute(@Nonnull EnforcerRuleHelper helper)
            throws EnforcerRuleException {
        Log log = helper.getLog();

        @Nonnull
        final String DIRECTORY_DEFAULT = "src";
        @Nonnull
        final String INCLUDE_REGEX_DEFAULT = ".*";
        @Nonnull
        final String EXCLUDE_REGEX_DEFAULT = "";

        try {
            // get the various expressions out of the helper.
            final MavenProject project = (MavenProject) helper.evaluate("${project}");
            final MavenSession session = (MavenSession) helper.evaluate("${session}");
            final String target = helper.evaluate("${project.build.directory}").toString();
            final String artifactId = helper.evaluate("${project.artifactId}").toString();
            final String basedir = helper.evaluate("${project.basedir}").toString();

            // Retrieve any component out of the session directly.
            ArtifactResolver resolver = (ArtifactResolver) helper.getComponent(ArtifactResolver.class);
            RuntimeInformation rti = (RuntimeInformation) helper.getComponent(RuntimeInformation.class);
            log.debug("Retrieved Target Folder: " + target);
            log.debug("Retrieved ArtifactId: " + artifactId);
            log.debug("Retrieved Project: " + project);
            log.debug("Retrieved RuntimeInfo: " + rti);
            log.debug("Retrieved Session: " + session);
            log.debug("Retrieved Resolver: " + resolver);
            log.debug("Retrieved Basedir: " + basedir);
            log.debug("requireEncoding: " + (requireEncoding == null ? "null" : requireEncoding));
            log.debug("directory: " + (directory == null ? "null" : directory));
            log.debug("includeRegex: " + (includeRegex == null ? "null" : includeRegex));
            log.debug("excludeRegex: " + (excludeRegex == null ? "null" : excludeRegex));

            if (this.getRequireEncoding() == null || this.getRequireEncoding().trim().length() == 0) {
                final String sourceEncoding = (String) helper.evaluate("${project.build.sourceEncoding}");
                log.info("No parameter 'requiredEncoding' set. Defaults to property 'project.build.sourceEncoding'.");
                if (sourceEncoding != null && sourceEncoding.trim().length() > 0) {
                    this.setRequireEncoding(sourceEncoding);
                } else {
                    throw new EnforcerRuleException("Missing parameter 'requireEncoding' and property 'project.build.sourceEncoding'.");
                }
            }
            try {
                Charset.forName(this.getRequireEncoding()); //  Charset names are not case-sensitive
            } catch (IllegalCharsetNameException e) {
                throw new EnforcerRuleException("Illegal value (illegal character set name) '" + requireEncoding + "' for parameter 'requireEncoding'.");
            } catch (UnsupportedCharsetException e) {
                throw new EnforcerRuleException("Illegal value (not supported character set) '" + requireEncoding + "' for parameter 'requireEncoding'.");
            } catch (IllegalArgumentException e) {
                throw new EnforcerRuleException("Illegal value (empty) '" + requireEncoding + "' for parameter 'requireEncoding'.");
            }
            if (this.getDirectory() == null || this.getDirectory().trim().length() == 0) {
                log.debug("No parameter 'directory' set. Defaults to '" + DIRECTORY_DEFAULT + "'.");
                this.setDirectory(DIRECTORY_DEFAULT);
            }
            if (this.getIncludeRegex() == null || this.getIncludeRegex().trim().length() == 0) {
                log.debug("No parameter 'includeRegex' set. Defaults to '" + INCLUDE_REGEX_DEFAULT + "'.");
                this.setIncludeRegex(INCLUDE_REGEX_DEFAULT);
            }
            if (this.getExcludeRegex() == null || this.getExcludeRegex().trim().length() == 0) {
                log.debug("No parameter 'excludeRegex' set. Defaults to '" + EXCLUDE_REGEX_DEFAULT + "'.");
                this.setExcludeRegex(EXCLUDE_REGEX_DEFAULT);
            }
            log.debug("requireEncoding: " + this.getRequireEncoding());
            log.debug("directory: " + this.getDirectory());
            log.debug("includeRegex: " + this.getIncludeRegex());
            log.debug("excludeRegex: " + this.getExcludeRegex());

            // Check the existence of the wanted directory:
            final Path dir = Paths.get(basedir, getDirectory());
            log.debug("Get files in dir '" + dir.toString() + "'.");
            if (!dir.toFile().exists()) {
                throw new EnforcerRuleException(
                        "Directory '" + dir.toString() + "' not found."
                                + " Specified by parameter 'directory' (value: '" + this.getDirectory() + "')!"
                );
            }

            // Put all files into this collection:
            Collection<FileResult> allFiles = new ArrayList<>();
            FileVisitor<Path> fileVisitor = new GetEncodingsFileVisitor(
                    log, this.getIncludeRegex(), this.getExcludeRegex(), allFiles
            );
            try {
                Set<FileVisitOption> visitOptions = new LinkedHashSet<>();
                visitOptions.add(FileVisitOption.FOLLOW_LINKS);
                Files.walkFileTree(dir,
                        visitOptions,
                        Integer.MAX_VALUE,
                        fileVisitor
                );
            } catch (Exception e) {
                log.error(e.getCause() + e.getMessage());
            }

            // Copy faulty files to another list.
            Collection<FileResult> faultyFiles = new ArrayList<>();
            log.debug("Moving possible faulty files (faulty encoding) to another list.");
            for (FileResult res : allFiles) {
                log.debug("Checking if file '" + res.getPath().toString() + "' has encoding '" + requireEncoding + "'.");
                boolean hasCorrectEncoding = true;
                try (FileInputStream fileInputStream = new FileInputStream(res.getPath().toFile())) {
                    byte[] bytes = ByteStreams.toByteArray(fileInputStream);
                    Charset.availableCharsets().get(requireEncoding)
                            .newDecoder().decode(ByteBuffer.wrap(bytes));
                } catch (CharacterCodingException e) {
                    hasCorrectEncoding = false;
                } catch (IOException e) {
                    e.printStackTrace();
                    log.error(e.getMessage());
                    hasCorrectEncoding = false;
                }
                if(!hasCorrectEncoding) {
                    log.debug("Moving faulty file: " + res.getPath());
                    FileResult faultyFile = new FileResult.Builder(res.getPath())
                            .lastModified(res.getLastModified())
                            .build();
                    faultyFiles.add(faultyFile);
                }
            }
            log.debug("All faulty files moved.");

            // Report
            if (!faultyFiles.isEmpty()) {
                final StringBuilder builder = new StringBuilder();
                builder.append("Wrong encoding in following files:");
                builder.append(System.getProperty("line.separator"));
                for (FileResult res : faultyFiles) {
                    builder.append(res.getPath());
                    builder.append(System.getProperty("line.separator"));
                }
                throw new EnforcerRuleException(builder.toString());
            }
        } catch (ExpressionEvaluationException e) {
            throw new EnforcerRuleException(
                    "Unable to lookup an expression " + e.getLocalizedMessage(), e
            );
        } catch (ComponentLookupException e) {
            throw new EnforcerRuleException(
                    "Unable to lookup a component " + e.getLocalizedMessage(), e
            );
        }
    }

    /**
     * If your rule is cacheable, you must return a unique id when parameters or conditions
     * change that would cause the result to be different. Multiple cached results are stored
     * based on their id.
     * <p>
     * The easiest way to do this is to return a hash computed from the values of your parameters.
     * <p>
     * If your rule is not cacheable, then the result here is not important, you may return anything.
     */
    @Nullable
    public String getCacheId() {
        // TODO Return a hash from parameters and the complete tree of files and their last change time.
        return "" + this;
    }

    /**
     * This tells the system if the results are cacheable at all. Keep in mind that during
     * forked builds and other things, a given rule may be executed more than once for the same
     * project. This means that even things that change from project to project may still
     * be cacheable in certain instances.
     */
    public boolean isCacheable() {
        return false;
    }

    /**
     * If the rule is cacheable and the same id is found in the cache, the stored results
     * are passed to this method to allow double checking of the results. Most of the time
     * this can be done by generating unique ids, but sometimes the results of objects returned
     * by the helper need to be queried. You may for example, store certain objects in your rule
     * and then query them later.
     */
    public boolean isResultValid(EnforcerRule arg0) {
        return false;
    }

    /**
     * Getters and setters for the parameters (these are filled by Maven)
     */

    @Nullable
    private String getDirectory() {
        return directory;
    }

    private void setDirectory(@Nullable final String directory) {
        this.directory = directory;
    }

    @Nullable
    private String getIncludeRegex() {
        return includeRegex;
    }

    private void setIncludeRegex(@Nullable final String includeRegex) {
        this.includeRegex = includeRegex;
    }

    @Nullable
    private String getExcludeRegex() {
        return excludeRegex;
    }

    private void setExcludeRegex(@Nullable final String excludeRegex) {
        this.excludeRegex = excludeRegex;
    }

    @Nullable
    private String getRequireEncoding() {
        return requireEncoding;
    }

    private void setRequireEncoding(@Nullable final String requireEncoding) {
        this.requireEncoding = requireEncoding;
    }

    private static final class GetEncodingsFileVisitor extends SimpleFileVisitor<Path> {
        @Nonnull
        private final Log log;
        @Nonnull
        private final Collection<FileResult> results;
        @Nonnull
        private String includeRegex;
        @Nonnull
        private String excludeRegex;

        GetEncodingsFileVisitor(
                Log log, String includeRegex, String excludeRegex, Collection<FileResult> results
        ) {
            this.log = log;
            this.includeRegex = includeRegex;
            this.excludeRegex = excludeRegex;
            this.results = results;
        }

        @Override
        public FileVisitResult visitFile(
                Path aFile, BasicFileAttributes aAttrs
        ) throws IOException {
            log.debug("Processing file '" + aFile.toString() + "'.");
            if (includeRegex.length() > 0 && !aFile.toString().matches(includeRegex)) {
                return FileVisitResult.CONTINUE;
            } else if (includeRegex.length() > 0 && aFile.toString().matches(includeRegex)
                    && excludeRegex.length() > 0 && aFile.toString().matches(excludeRegex)) {
                return FileVisitResult.CONTINUE;
            } else if (includeRegex.length() == 0
                    && excludeRegex.length() > 0 && aFile.toString().matches(excludeRegex)) {
                return FileVisitResult.CONTINUE;
            } else {
                File file = aFile.toFile();
                FileResult res = new FileResult.Builder(aFile.toAbsolutePath())
                        .lastModified(file.lastModified())
                        .build();
                results.add(res);
                return FileVisitResult.CONTINUE;
            }
        }

        @Override
        public FileVisitResult preVisitDirectory(
                Path aDir, BasicFileAttributes aAttrs
        ) throws IOException {
            log.debug("Processing directory '" + aDir.toString() + "'.");
            return FileVisitResult.CONTINUE;
        }

    }

}

/**
 * Internal class for gathering all files.
 * Also used for caching the result of the whole test.
 */
class FileResult {

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
    public String toString() {
        return "FileResult{" + "path=" + path.toString()
                + ", lastModified=" + lastModified + "}";
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

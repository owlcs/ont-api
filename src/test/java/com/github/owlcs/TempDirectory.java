/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * {@code TempDirectory} is an ONT-API JUnit Jupiter extension
 * that provides the single temporary directory used in the whole test-suite
 * and controls its content while tests execution.
 * <p>
 * Created by @ssz on 10.10.2020.
 */
public class TempDirectory implements AfterAllCallback {
    /**
     * As a singleton: one working directory for the entire test suite.
     */
    public static final Path DIR = createTempDirectory("junit-ont-api-");
    private static final Logger LOGGER = LoggerFactory.getLogger(TempDirectory.class);

    /**
     * Creates a new empty file in the {@link #DIR junit working directory},
     * using the given prefix and suffix strings to generate its name.
     *
     * @param prefix the prefix string to be used in generating the file's name; may be {@code null}
     * @param suffix the suffix string to be used in generating the file's name; may be {@code null},
     *               in which case "{@code .tmp}" is used
     * @return the path to the newly created file that did not exist before this method was invoked
     * @throws IOException if an I/O error occurs
     */
    public static Path createFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(DIR, prefix, suffix);
    }

    @SuppressWarnings("SameParameterValue")
    private static Path createTempDirectory(String prefix) {
        try {
            Path res = Files.createTempDirectory(prefix);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    deleteAll(res);
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to delete " + res, e);
                }
            }, "The Junit TempDirectory Cleaner."));
            return res;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create temporary dir", e);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void deleteAll(Path root) throws IOException {
        IOException error = new IOException("Can't delete root-dir " + root);
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes a) {
                delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException ex) {
                delete(dir);
                return FileVisitResult.CONTINUE;
            }

            private void delete(Path path) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    error.addSuppressed(exception);
                }
            }
        });
        if (error.getSuppressed().length != 0) {
            throw error;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void clearAll(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes a) {
                try {
                    Files.delete(file);
                } catch (IOException io) {
                    // ignore any exceptions, e.g. https://github.com/owlcs/owlapi/issues/973
                    // (IOException while deleting cannot be a problem of ONT-API)
                    LOGGER.warn("Can't delete file {}: '{}'", file, io.getMessage(), io);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException ex) {
                if (root.equals(dir)) { // do not delete root dir
                    return FileVisitResult.CONTINUE;
                }
                try {
                    Files.delete(dir);
                } catch (IOException io) {
                    // (IOException while deleting cannot be a problem of ONT-API)
                    LOGGER.warn("Can't delete directory {}: '{}'", dir, io.getMessage(), io);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        clearAll(DIR);
    }
}

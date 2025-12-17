/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.common.facades;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Facade for JDK operations that differ between Java SE and Android.
 * <p>
 * All string I/O operations use UTF-8 encoding exclusively for consistency
 * and to prevent data corruption from encoding mismatches.
 * <p>
 * File operations apply restricted permissions (owner-only read/write) on POSIX systems
 * to protect sensitive data like private keys.
 */
public interface JdkFacade {
    String getMyPid();

    Stream<String> getProcessCommandStream();

    void redirectError(ProcessBuilder processBuilder);

    void redirectOutput(ProcessBuilder processBuilder);

    /**
     * Reads the entire content of a file as a UTF-8 encoded string.
     *
     * @param path the path to the file
     * @return the file content as a string
     * @throws IOException if an I/O error occurs
     */
    String readString(Path path) throws IOException;

    /**
     * Writes a string to a file using UTF-8 encoding.
     * On POSIX systems, applies owner-only read/write permissions.
     *
     * @param data the string to write
     * @param path the path to the file
     * @throws IOException if an I/O error occurs
     */
    void writeString(String data, Path path) throws IOException;

    /**
     * Creates a directory and all necessary parent directories.
     * On POSIX systems, applies owner-only read/write/execute permissions.
     *
     * @param path the directory path to create
     * @throws IOException if an I/O error occurs
     */
    void createDirectories(Path path) throws IOException;
}

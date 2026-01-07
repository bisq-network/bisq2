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
import java.net.URI;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Facade for JDK operations that differ between Java SE and Android.
 * <p>
 * All string I/O operations use UTF-8 encoding exclusively for consistency
 * and to prevent data corruption from encoding mismatches.
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
     *
     * @param data the string to write
     * @param path the path to the file
     * @throws IOException if an I/O error occurs
     */
    void writeString(String data, Path path) throws IOException;

    /**
     * Converts a path string, or a sequence of strings that when joined form a path string, to a Path.
     * <p>
     * On Android, this uses {@link java.nio.file.Paths#get(String, String...)} which is compatible with desugaring.
     * On Java SE, this uses {@link Path#of(String, String...)} which is the modern API.
     *
     * @param first the path string or initial part of the path string
     * @param more  additional strings to be joined to form the path string
     * @return the resulting Path
     */
    Path pathOf(String first, String... more);

    /**
     * Converts a URI to a Path.
     * <p>
     * On Android, this uses {@link java.nio.file.Paths#get(URI)} which is compatible with desugaring.
     * On Java SE, this uses {@link Path#of(URI)} which is the modern API.
     *
     * @param uri the URI to convert
     * @return the resulting Path
     */
    Path pathOf(URI uri);
}

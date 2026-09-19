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

package bisq.application;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * Loads every Bisq class on the test classpath, so a test can assert over all of them rather than over a list
 * someone has to remember to update.
 */
final class BisqClasses {
    private BisqClasses() {
    }

    static List<Class<?>> bisqClasses() throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        for (String entry : new TreeSet<>(List.of(System.getProperty("java.class.path")
                .split(File.pathSeparator)))) {
            Path root = Path.of(entry);
            List<String> resources = new ArrayList<>();
            if (Files.isDirectory(root)) {
                try (Stream<Path> files = Files.walk(root)) {
                    // A relative Path prints with the platform separator, so it is normalised to the form jar
                    // entries already use. Without this the checks below silently match nothing on Windows.
                    files.filter(file -> file.toString().endsWith(".class"))
                            .forEach(file -> resources.add(root.relativize(file).toString()
                                    .replace(File.separatorChar, '/')));
                }
            } else if (entry.endsWith(".jar")) {
                try (JarFile jar = new JarFile(root.toFile())) {
                    jar.stream().map(ZipEntry::getName)
                            .filter(name -> name.startsWith("bisq/") && name.endsWith(".class"))
                            .forEach(resources::add);
                }
            }
            for (String resource : resources) {
                if (!resource.startsWith("bisq/")) {
                    continue;
                }
                String name = resource.substring(0, resource.length() - ".class".length()).replace('/', '.');
                try {
                    classes.add(Class.forName(name, false, BisqClasses.class.getClassLoader()));
                } catch (Throwable ignore) {
                    // Classes we cannot load cannot be payload types either.
                }
            }
        }
        return classes;
    }
}

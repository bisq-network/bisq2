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

import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * Finds all concrete types on the test classpath which the network storage can store: implementations of
 * {@link DistributedData} and {@link MailboxMessage}. Tests use it to check all such types, so that a new type
 * cannot be forgotten in a hand-written list.
 */
final class StorableTypes {
    // ConfidentialMessage is the encrypted form of a MailboxMessage. The storage layer stores it inside MailboxData,
    // which carries the class name of the encrypted MailboxMessage.
    private static final Set<Class<?>> NOT_STORED_ALONE = Set.of(ConfidentialMessage.class);

    private StorableTypes() {
    }

    static List<Class<?>> find() throws IOException {
        List<Class<?>> types = new ArrayList<>();
        for (Class<?> clazz : findAll()) {
            boolean isStorable = DistributedData.class.isAssignableFrom(clazz) ||
                    MailboxMessage.class.isAssignableFrom(clazz);
            boolean isConcrete = !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
            if (isStorable && isConcrete && !NOT_STORED_ALONE.contains(clazz)) {
                types.add(clazz);
            }
        }
        return types;
    }

    /**
     * Every Bisq class on the test classpath, storable or not. A test which has to show that a property belongs to
     * the storable types alone needs the types which are not storable as well.
     */
    static List<Class<?>> findAll() throws IOException {
        List<Class<?>> classes = new ArrayList<>();
        for (String classNameEntry : findBisqClassNames()) {
            try {
                classes.add(Class.forName(classNameEntry, false, StorableTypes.class.getClassLoader()));
            } catch (Throwable ignore) {
                // A class which cannot be loaded on this classpath cannot be stored by this application either.
            }
        }
        classes.sort(Comparator.comparing(Class::getName));
        return classes;
    }

    private static List<String> findBisqClassNames() throws IOException {
        List<String> classNames = new ArrayList<>();
        for (String classPathEntry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            Path path = Path.of(classPathEntry);
            List<String> resources = new ArrayList<>();
            if (Files.isDirectory(path)) {
                try (Stream<Path> files = Files.walk(path)) {
                    files.map(file -> path.relativize(file).toString().replace(File.separatorChar, '/'))
                            .forEach(resources::add);
                }
            } else if (classPathEntry.endsWith(".jar")) {
                try (JarFile jarFile = new JarFile(path.toFile())) {
                    jarFile.stream().map(ZipEntry::getName).forEach(resources::add);
                }
            }
            resources.stream()
                    .filter(resource -> resource.startsWith("bisq/") && resource.endsWith(".class"))
                    .map(resource -> resource.substring(0, resource.length() - ".class".length()).replace('/', '.'))
                    .forEach(classNames::add);
        }
        return classNames;
    }
}

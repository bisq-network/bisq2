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

package bisq.application.compat;

import bisq.common.encoding.Hex;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.security.DigestUtil;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helpers of the compatibility chain for the code of v2.1.13. The steps of the chain exchange requests and stores
 * through files in the directory of the system property {@code bisq.compat.exchangeDir}. tools/compat/run-chain.sh
 * sets it. The new version has a class with the same role, CompatSupport, which uses the API of that version.
 */
final class OldCompatSupport {
    static final String EXCHANGE_DIR_PROPERTY = "bisq.compat.exchangeDir";
    static final String OLD_REQUESTS_FILE = "step1-old-requests.properties";
    static final String OLD_DATA_DIR = "step1-old-data-dir";
    static final String NEW_REQUESTS_FILE = "step2-new-requests.properties";
    static final String RELAYED_COPIES_FILE = "step3-relayed-copies.properties";

    private OldCompatSupport() {
    }

    static Path exchangeDir() {
        String value = System.getProperty(EXCHANGE_DIR_PROPERTY);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("System property " + EXCHANGE_DIR_PROPERTY + " is not set");
        }
        return Path.of(value);
    }

    // An empty directory for the persisted data of one node of a step
    static Path newWorkDir(String name) throws IOException {
        Path dir = exchangeDir().resolve("work").resolve(name);
        deleteRecursively(dir);
        Files.createDirectories(dir);
        return dir;
    }

    static Properties read(String fileName) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(exchangeDir().resolve(fileName), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    // Writes the properties sorted by key, so that two runs can be compared. Properties.store writes each entry on one
    // line, with line breaks of values escaped.
    static void write(String fileName, Map<String, String> values, String comment) throws IOException {
        Path path = exchangeDir().resolve(fileName);
        Files.createDirectories(path.getParent());
        Properties properties = new Properties();
        properties.putAll(values);
        StringWriter buffer = new StringWriter();
        properties.store(buffer, null);
        List<String> lines = buffer.toString().lines()
                .filter(line -> !line.startsWith("#"))
                .sorted()
                .collect(Collectors.toList());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("# " + comment + "\n");
            for (String line : lines) {
                writer.write(line + "\n");
            }
        }
    }

    static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    static byte[] fromBase64(String value) {
        return Base64.getDecoder().decode(value);
    }

    static EnvelopePayloadMessage parse(String base64) throws Exception {
        return EnvelopePayloadMessage.fromProto(bisq.network.protobuf.EnvelopePayloadMessage.parseFrom(fromBase64(base64)));
    }

    // The hash of the proof of work payload. Sender and receiver must compute the same payload.
    static String proofOfWorkPayloadHash(EnvelopePayloadMessage message) {
        return Hex.encode(DigestUtil.sha256(message.serializeForHash()));
    }

    static DataStorageResult process(StorageService storageService, EnvelopePayloadMessage message) {
        if (message instanceof AddAuthenticatedDataRequest request) {
            String className = request.getDistributedData().getMetaData().getClassName();
            return storageService.getOrCreateAuthenticatedDataStore(className).join().add(request);
        } else if (message instanceof RemoveAuthenticatedDataRequest request) {
            return storageService.getOrCreateAuthenticatedDataStore(request.getClassName()).join().remove(request);
        } else if (message instanceof RefreshAuthenticatedDataRequest request) {
            return storageService.getOrCreateAuthenticatedDataStore(request.getClassName()).join().refresh(request);
        } else if (message instanceof AddMailboxRequest request) {
            String className = request.getMailboxSequentialData().getMailboxData().getClassName();
            return storageService.getOrCreateMailboxDataStore(className).join().add(request);
        } else if (message instanceof RemoveMailboxRequest request) {
            return storageService.getOrCreateMailboxDataStore(request.getClassName()).join().remove(request);
        }
        throw new IllegalArgumentException("Unexpected message " + message.getClass().getSimpleName());
    }

    // The names of the flags of the result which are true, for example [success] or [sequenceNrInvalid]. The flags have
    // the same names in both versions. The last field of the text of v2.1.13 is the removed data, whose own text can
    // contain commas, so the flags end before it.
    static Set<String> flags(DataStorageResult result) {
        String text = result.toString();
        int start = text.indexOf('{') + 1;
        int removedDataIndex = text.indexOf(", removedData=");
        int end = removedDataIndex >= 0 ? removedDataIndex : text.lastIndexOf('}');
        Set<String> flags = new TreeSet<>();
        for (String token : text.substring(start, end).split(", ")) {
            int index = token.indexOf('=');
            if (index > 0 && token.substring(index + 1).equals("true")) {
                flags.add(token.substring(0, index));
            }
        }
        return flags;
    }

    // A description of each stored entry which does not depend on the version: class name, kind and sequence number
    static List<String> describeStores(StorageService storageService) {
        return storageService.getAllDataRequestMapEntries()
                .map(entry -> describe(entry.getValue()))
                .sorted()
                .collect(Collectors.toList());
    }

    static String describe(Object request) {
        if (request instanceof AddAuthenticatedDataRequest add) {
            return add.getDistributedData().getMetaData().getClassName() + "|add|" + add.getSequenceNumber();
        } else if (request instanceof RemoveAuthenticatedDataRequest remove) {
            return remove.getClassName() + "|remove|" + remove.getSequenceNumber();
        } else if (request instanceof AddMailboxRequest add) {
            return add.getMailboxSequentialData().getMailboxData().getClassName() + "|addMailbox|" + add.getSequenceNumber();
        } else if (request instanceof RemoveMailboxRequest remove) {
            return remove.getClassName() + "|removeMailbox|" + remove.getSequenceNumber();
        }
        return request.getClass().getSimpleName();
    }

    // The data which the applications of the node get, by simple class name
    static List<String> applicationData(DataService dataService) {
        return dataService.getAuthenticatedData()
                .map(data -> data.getDistributedData().getClass().getSimpleName())
                .sorted()
                .collect(Collectors.toList());
    }

    static DataService.Listener recordingListener(List<String> events) {
        return new DataService.Listener() {
            @Override
            public void onAuthorizedDataAdded(AuthorizedData data) {
                events.add("authorizedAdded:" + data.getDistributedData().getClass().getSimpleName());
            }

            @Override
            public void onAuthorizedDataRemoved(AuthorizedData data) {
                events.add("authorizedRemoved:" + data.getDistributedData().getClass().getSimpleName());
            }

            @Override
            public void onAuthenticatedDataAdded(AuthenticatedData data) {
                events.add("added:" + data.getDistributedData().getClass().getSimpleName());
            }

            @Override
            public void onAuthenticatedDataRemoved(AuthenticatedData data) {
                events.add("removed:" + data.getDistributedData().getClass().getSimpleName());
            }

            @Override
            public void onMailboxDataAdded(MailboxData mailboxData) {
                events.add("mailboxAdded");
            }

            @Override
            public void onMailboxDataRemoved(MailboxData mailboxData) {
                events.add("mailboxRemoved");
            }
        };
    }

    static List<String> namesWithPrefix(Properties properties, String prefix) {
        List<String> names = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                names.add(key.substring(prefix.length()));
            }
        }
        names.sort(Comparator.naturalOrder());
        return names;
    }

    static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path file : stream.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.delete(file);
            }
        }
    }

    static void copyRecursively(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : stream.collect(Collectors.toList())) {
                Path destination = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}

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
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.Sequential;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.security.ConfidentialData;
import bisq.security.DigestUtil;
import bisq.security.HybridEncryption;
import bisq.security.keys.KeyGeneration;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Helpers of the compatibility chain for the code of this version. The steps of the chain exchange requests and
 * stores through files in the directory of the system property {@code bisq.compat.exchangeDir}.
 * tools/compat/run-chain.sh sets it. The steps for a released version are in a package of their own, for example
 * v2_1_13. The overlay of the released version has a class with the same role, OldCompatSupport, which uses the API
 * of that version. See tools/compat/README.md.
 */
public final class CompatSupport {
    public static final String EXCHANGE_DIR_PROPERTY = "bisq.compat.exchangeDir";
    public static final String OLD_REQUESTS_FILE = "step1-old-requests.properties";
    public static final String OLD_DATA_DIR = "step1-old-data-dir";
    public static final String NEW_REQUESTS_FILE = "step2-new-requests.properties";
    public static final String RELAYED_COPIES_FILE = "step3-relayed-copies.properties";

    private CompatSupport() {
    }

    public static Path exchangeDir() {
        String value = System.getProperty(EXCHANGE_DIR_PROPERTY);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("System property " + EXCHANGE_DIR_PROPERTY + " is not set");
        }
        return Path.of(value);
    }

    // An empty directory for the persisted data of one node of a step
    public static Path newWorkDir(String name) throws IOException {
        Path dir = exchangeDir().resolve("work").resolve(name);
        deleteRecursively(dir);
        Files.createDirectories(dir);
        return dir;
    }

    public static Properties read(String fileName) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(exchangeDir().resolve(fileName), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    // Writes the properties sorted by key, so that two runs can be compared. Properties.store writes each entry on one
    // line, with line breaks of values escaped.
    public static void write(String fileName, Map<String, String> values, String comment) throws IOException {
        Path path = exchangeDir().resolve(fileName);
        Files.createDirectories(path.getParent());
        Properties properties = new Properties();
        properties.putAll(values);
        StringWriter buffer = new StringWriter();
        properties.store(buffer, null);
        List<String> lines = buffer.toString().lines()
                .filter(line -> !line.startsWith("#"))
                .sorted()
                .toList();
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("# " + comment + "\n");
            for (String line : lines) {
                writer.write(line + "\n");
            }
        }
    }

    public static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] fromBase64(String value) {
        return Base64.getDecoder().decode(value);
    }

    public static EnvelopePayloadMessage parse(String base64) throws Exception {
        return EnvelopePayloadMessage.fromProto(bisq.network.protobuf.EnvelopePayloadMessage.parseFrom(fromBase64(base64)));
    }

    // The hash of the proof of work payload. Sender and receiver must compute the same payload.
    public static String proofOfWorkPayloadHash(EnvelopePayloadMessage message) {
        return Hex.encode(DigestUtil.sha256(message.serializeForHash()));
    }

    public static KeyPair keyPair(Properties properties, String name) throws Exception {
        PrivateKey privateKey = KeyGeneration.generatePrivate(fromBase64(properties.getProperty("key." + name + ".private")));
        PublicKey publicKey = KeyGeneration.generatePublic(fromBase64(properties.getProperty("key." + name + ".public")));
        return new KeyPair(publicKey, privateKey);
    }

    public static DataStorageResult process(StorageService storageService, EnvelopePayloadMessage message) {
        if (message instanceof AddAuthenticatedDataRequest request) {
            return storageService.getOrCreateAuthenticatedDataStore(request.getClassName()).join().add(request);
        } else if (message instanceof RemoveAuthenticatedDataRequest request) {
            return storageService.getOrCreateAuthenticatedDataStore(request.getClassName()).join().remove(request);
        } else if (message instanceof RefreshAuthenticatedDataRequest request) {
            return storageService.getOrCreateAuthenticatedDataStore(request.getClassName()).join().refresh(request);
        } else if (message instanceof AddMailboxRequest request) {
            return storageService.getOrCreateMailboxDataStore(request.getClassName()).join().add(request);
        } else if (message instanceof RemoveMailboxRequest request) {
            return storageService.getOrCreateMailboxDataStore(request.getClassName()).join().remove(request);
        }
        throw new IllegalArgumentException("Unexpected message " + message.getClass().getSimpleName());
    }

    // The names of the flags of the result which are true, for example [success] or [sequenceNrInvalid]. The flags
    // have the same names in both versions.
    public static Set<String> flags(DataStorageResult result) {
        String text = result.toString();
        Set<String> flags = new TreeSet<>();
        for (String token : text.substring(text.indexOf('{') + 1, text.lastIndexOf('}')).split(", ")) {
            int index = token.indexOf('=');
            if (index > 0 && token.substring(index + 1).equals("true")) {
                flags.add(token.substring(0, index));
            }
        }
        return flags;
    }

    // A description of each stored entry which does not depend on the version: class name, kind and sequence number
    public static List<String> describeStores(StorageService storageService) {
        return storageService.getAllDataRequestMapEntries()
                .map(entry -> describe(entry.getValue()))
                .sorted()
                .toList();
    }

    public static String describe(DataRequest request) {
        String kind = switch (request) {
            case AddAuthenticatedDataRequest ignored -> "add";
            case RemoveAuthenticatedDataRequest ignored -> "remove";
            case AddMailboxRequest ignored -> "addMailbox";
            case RemoveMailboxRequest ignored -> "removeMailbox";
            default -> request.getClass().getSimpleName();
        };
        int sequenceNumber = request instanceof Sequential sequential ? sequential.getSequenceNumber() : -1;
        return request.getClassName() + "|" + kind + "|" + sequenceNumber;
    }

    // The data which the applications of the node get, by simple class name
    public static List<String> applicationData(DataService dataService) {
        return dataService.getAuthenticatedData()
                .map(data -> data.getDistributedData().getClass().getSimpleName())
                .sorted()
                .toList();
    }

    public static DataService.Listener recordingListener(List<String> events) {
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

    public static MailboxData mailboxData(String id, KeyPair senderKeyPair, KeyPair receiverKeyPair) throws Exception {
        AckMessage ackMessage = new AckMessage(id);
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(ackMessage.serialize(),
                receiverKeyPair.getPublic(), senderKeyPair);
        // The constructor is package private. ConfidentialMessageService creates the message from the same data.
        Constructor<ConfidentialMessage> constructor = ConfidentialMessage.class.getDeclaredConstructor(ConfidentialData.class, String.class);
        constructor.setAccessible(true);
        ConfidentialMessage confidentialMessage = constructor.newInstance(confidentialData, "compat-receiver-key-id");
        return new MailboxData(AckMessage.META_DATA, confidentialMessage);
    }

    public static List<String> namesWithPrefix(Properties properties, String prefix) {
        List<String> names = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                names.add(key.substring(prefix.length()));
            }
        }
        names.sort(Comparator.naturalOrder());
        return names;
    }

    public static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path file : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(file);
            }
        }
    }

    public static void copyRecursively(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
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

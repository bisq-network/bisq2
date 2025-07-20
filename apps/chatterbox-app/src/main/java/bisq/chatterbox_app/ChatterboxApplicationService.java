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

package bisq.chatterbox_app;

import bisq.common.util.StringUtils;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.security.ConfidentialData;
import bisq.security.HybridEncryption;
import bisq.security.keys.KeyGeneration;
import bisq.seed_node.SeedNodeApplicationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;


/**
 * Creates a seed node that generates noise on the network
 */
@Getter
@Slf4j
public class ChatterboxApplicationService extends SeedNodeApplicationService {

    public ChatterboxApplicationService(String[] args) {
        super("chatterbox", args);
    }

    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private AddMailboxRequest createAddMailboxRequest() throws GeneralSecurityException {
        KeyPair keyPairSender;
        KeyPair keyPairReceiver;
        keyPairSender = KeyGeneration.generateKeyPair();
        keyPairReceiver = KeyGeneration.generateKeyPair();
        var message = generateRandomString(32).getBytes();
        var randomId = StringUtils.createUid();
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(
                message, keyPairReceiver.getPublic(), keyPairSender
        );
        var proto = bisq.network.protobuf.ConfidentialMessage.newBuilder()
                .setConfidentialData(confidentialData.toProto(false))
                .setReceiverKeyId(randomId).build();
        var mailboxData = new MailboxData(new MetaData("chatterbox"), ConfidentialMessage.fromProto(proto));
        return AddMailboxRequest.from(mailboxData, keyPairSender, keyPairReceiver.getPublic());
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return super.initialize().whenComplete((success, throwable) -> {
            if (success) {
                CompletableFuture.runAsync(() -> {
                    try {
                        // wait 10 seconds before doing anything
                        // to allow things to set up and desktops connect
                        Thread.sleep(10 * 1000);
                        // we reuse the same mailbox to not cause storage issues
                        // we just want the network noise
                        var mailboxRequest = createAddMailboxRequest();
                        var dataService = this.getNetworkService().getDataService().get();
                        while (true) {
                            try {
                                IntStream.range(0, 2000).parallel().forEach(i ->
                                        dataService.getBroadcasters().parallelStream().forEach(broadcaster ->
                                                broadcaster.reBroadcast(mailboxRequest)
                                        )
                                );
                                Thread.sleep(60 * 1000);
                            } catch (Exception e) {
                                log.error("Chatterbox Error", e);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        log.error("Chatterbox Error: ", e);

                    } finally {
                        this.shutdown();
                    }
                });
            }
        });
    }
}

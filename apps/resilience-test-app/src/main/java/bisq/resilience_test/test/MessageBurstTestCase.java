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

package bisq.resilience_test.test;

import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.security.ConfidentialData;
import bisq.security.HybridEncryption;
import bisq.security.keys.KeyGeneration;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;
import java.util.Random;

@Slf4j
public class MessageBurstTestCase extends BaseTestCase {

    private final NetworkService networkService;
    private final IdentityService identityService;
    private int messageCount = 1000;
    private boolean sameMessage = true;

    public MessageBurstTestCase(Optional<Config> optionalConfig,
                                NetworkService networkService,
                                IdentityService identityService) {
        super(optionalConfig);
        this.networkService = networkService;
        this.identityService = identityService;
        optionalConfig.ifPresent(config -> {
            if (config.hasPath("messageCount")) {
                messageCount = config.getInt("messageCount");
            }
            if (config.hasPath("sameMessage")) {
                sameMessage = config.getBoolean("sameMessage");
            }
        });
    }

    @Override
    protected void run() {
        try {
            var mailboxRequest = createAddMailboxRequest();
            if (networkService.getDataService().isEmpty()) {
                throw new RuntimeException("networkService.getDataService() not available yet.");
            }
            var dataService = networkService.getDataService().get();

            for (var i = 0; i < messageCount; i++) {
                for (var broadcaster : dataService.getBroadcasters()) {
                    broadcaster.reBroadcast(mailboxRequest);
                    if (!sameMessage) {
                        mailboxRequest = createAddMailboxRequest();
                    }
                }
            }
            log.info("submitted {} messages to be re broadcasted", messageCount);
        } catch (Exception e) {
            log.error("MessageBurst: ", e);
        }
    }

    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private AddMailboxRequest createAddMailboxRequest() throws GeneralSecurityException {
        KeyPair keyPairSender = identityService.getOrCreateDefaultIdentity().getKeyBundle().getKeyPair();
        KeyPair keyPairReceiver;
        keyPairReceiver = KeyGeneration.generateKeyPair();
        var message = generateRandomString(32).getBytes();
        var randomId = "ae6e5c207877909ad4506408dcb62293e19b3fa9"; // StringUtils.createUid();
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(
                message, keyPairReceiver.getPublic(), keyPairSender
        );
        var proto = bisq.network.protobuf.ConfidentialMessage.newBuilder()
                .setConfidentialData(confidentialData.toProto(false))
                .setReceiverKeyId(randomId).build();
        var mailboxData = new MailboxData(new MetaData("resilienceTest"), ConfidentialMessage.fromProto(proto));
        return AddMailboxRequest.from(mailboxData, keyPairSender, keyPairReceiver.getPublic());
    }
}

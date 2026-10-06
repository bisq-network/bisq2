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

package bisq.e2e;

import bisq.api_app.ApiApplicationService;
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.bonded_roles.registration.BondedRoleRegistrationProtocol;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.bonded_roles.security_manager.difficulty_adjustment.AuthorizedDifficultyAdjustmentData;
import bisq.common.encoding.Hex;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import bisq.user.profile.UserProfile;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Publishes and removes authorized data with the first dev key of AuthorizedPubKeys. Nodes in dev mode accept the dev
 * keys as the static keys of security managers and oracle nodes. The same key owns the entries in the network storage,
 * as for all authorized data (docs/specifications/network/storage-ownership.md), except the AuthorizedOracleNode. The
 * commands:
 * <ul>
 *     <li>{@code require <version>}: an emergency alert for the desktop app which requires this version for trading,
 *     with the fields which the security manager tab of the desktop app sets</li>
 *     <li>{@code halt}: an emergency alert for the desktop app which halts trading</li>
 *     <li>{@code ban-mediator <profile ID>}: an alert which bans the role of this mediator, as the security manager tab
 *     of the desktop app publishes it. Needs the command {@code mediator} for this profile before.</li>
 *     <li>{@code remove}: removes the alert which the last of these commands published</li>
 *     <li>{@code difficulty <factor>}: a difficulty adjustment of the security manager</li>
 *     <li>{@code remove-difficulty}: removes the difficulty adjustment</li>
 *     <li>{@code oracle}: the data of a root oracle node with this key, its AuthorizedOracleNode and its role, as an
 *     oracle node publishes them at its start</li>
 *     <li>{@code mediator <profile ID> <registration protocol version>}: the role of a mediator with this user profile,
 *     which this oracle node authorizes, as the oracle node of this checkout publishes it for a registration request
 *     with this protocol version. Needs the command {@code oracle} before.</li>
 *     <li>{@code remove-mediator <profile ID>}: removes the role of this mediator</li>
 *     <li>{@code oracle-legacy} and {@code mediator-legacy <profile ID> <registration protocol version>}: the same data
 *     as {@code oracle} and {@code mediator}, which the key pair of the network ID of the oracle node publishes, as an
 *     oracle node of version 2.1.13 did. The dev key still signs the data.</li>
 *     <li>{@code migrate}: the migration of the publisher of the data of the oracle node of this checkout, which
 *     publishes the data of {@code oracle-legacy} and {@code mediator-legacy} again with the dev key and removes the
 *     entries of the key pair of the network ID (rule 14 of docs/specifications/network/storage-ownership.md). The
 *     command returns when the migration queued its requests, which it broadcasts one at a time.</li>
 * </ul>
 * Each command returns when its broadcast is done.
 */
final class AuthorizedDataPublisher {
    // privatekey1 and publicKey1 of the Javadoc of AuthorizedPubKeys
    private static final String DEV_PRIVATE_KEY = "30818d020100301006072a8648ce3d020106052b8104000a047630740201010420" +
            "5b4479d165652fe5410419b1d03c937956be0e1c4f46e9fbe86c66776529d81ca00706052b8104000aa144034200043dd1f2" +
            "f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e14697055" +
            "67124a02ae2e8afa8e8091";
    private static final String DEV_PUBLIC_KEY = "3056301006072a8648ce3d020106052b8104000a034200043dd1f2f56593e626702" +
            "82c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8" +
            "afa8e8091";
    // Nodes do not check the Bisq 1 signature of a bond. The oracle node checks it when it gets the registration.
    private static final String BOND_SIGNATURE_BASE64 = "H0UyZSBzaWduYXR1cmUgb2YgYSBib25kIHdoaWNoIG5vIG5vZGUgY2hlY2tz";

    private final ApiApplicationService applicationService;
    private final KeyPair devKeyPair;
    // The ID of the dev key, as PubKey.getId() creates it, which serves as the profile ID of the security manager and
    // of the oracle node
    private final String devProfileId;
    private final Map<String, AuthorizedBondedRole> mediatorRolesByProfileId = new HashMap<>();
    private Optional<AuthorizedAlertData> lastAlert = Optional.empty();
    private Optional<AuthorizedDifficultyAdjustmentData> difficultyAdjustment = Optional.empty();
    private Optional<AuthorizedOracleNode> oracleNode = Optional.empty();

    AuthorizedDataPublisher(ApiApplicationService applicationService) {
        this.applicationService = applicationService;
        try {
            devKeyPair = new KeyPair(KeyGeneration.generatePublic(Hex.decode(DEV_PUBLIC_KEY)),
                    KeyGeneration.generatePrivate(Hex.decode(DEV_PRIVATE_KEY)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        devProfileId = Hex.encode(DigestUtil.hash(devKeyPair.getPublic().getEncoded()));
    }

    void execute(String command) {
        String[] parts = command.split(" ");
        switch (parts[0]) {
            case "require" -> {
                checkArgument(parts.length == 2, "require needs a version");
                publishAlert(false, Optional.of(parts[1]));
            }
            case "halt" -> publishAlert(true, Optional.empty());
            case "ban-mediator" -> {
                checkArgument(parts.length == 2, "ban-mediator needs a profile ID");
                publishBanOfMediator(parts[1]);
            }
            case "remove" -> removeAlert();
            case "difficulty" -> {
                checkArgument(parts.length == 2, "difficulty needs a factor");
                publishDifficultyAdjustment(Double.parseDouble(parts[1]));
            }
            case "remove-difficulty" -> removeDifficultyAdjustment();
            case "oracle" -> publishOracleNode(devKeyPair);
            case "oracle-legacy" -> publishOracleNode(networkIdKeyPair());
            case "mediator", "mediator-legacy" -> {
                checkArgument(parts.length == 3, "%s needs a profile ID and a registration protocol version", parts[0]);
                publishMediatorRole(parts[1], Integer.parseInt(parts[2]),
                        parts[0].equals("mediator") ? devKeyPair : networkIdKeyPair());
            }
            case "migrate" -> networkService().migratePublisherOfAuthorizedData(networkIdKeyPair(), devKeyPair);
            case "remove-mediator" -> {
                checkArgument(parts.length == 2, "remove-mediator needs a profile ID");
                removeMediatorRole(parts[1]);
            }
            default -> throw new IllegalArgumentException("Unknown command: " + command);
        }
    }

    private void publishAlert(boolean haltTrading, Optional<String> minVersion) {
        String message = haltTrading ? "Trading is halted" : "Version " + minVersion.orElseThrow() + " is required";
        AuthorizedAlertData alert = new AuthorizedAlertData(UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                AlertType.EMERGENCY,
                Optional.of("E2E emergency"),
                Optional.of(message),
                haltTrading,
                minVersion.isPresent(),
                minVersion,
                Optional.empty(),
                devProfileId,
                true,
                Optional.empty(),
                AppType.DESKTOP);
        networkService().publishAuthorizedData(alert, devKeyPair).join();
        lastAlert = Optional.of(alert);
    }

    private void publishBanOfMediator(String profileId) {
        AuthorizedBondedRole role = mediatorRolesByProfileId.get(profileId);
        checkState(role != null, "No role of mediator %s to ban", profileId);
        AuthorizedAlertData ban = new AuthorizedAlertData(UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                AlertType.BAN,
                Optional.of("E2E ban"),
                Optional.of("The mediator " + profileId + " is banned"),
                false,
                false,
                Optional.empty(),
                Optional.of(role),
                devProfileId,
                true,
                Optional.empty(),
                AppType.DESKTOP);
        networkService().publishAuthorizedData(ban, devKeyPair).join();
        lastAlert = Optional.of(ban);
    }

    private void removeAlert() {
        AuthorizedAlertData alert = lastAlert.orElseThrow(() -> new IllegalStateException("No alert to remove"));
        networkService().removeAuthorizedData(alert, devKeyPair).join();
        lastAlert = Optional.empty();
    }

    private void publishDifficultyAdjustment(double factor) {
        AuthorizedDifficultyAdjustmentData data = new AuthorizedDifficultyAdjustmentData(System.currentTimeMillis(),
                factor,
                devProfileId,
                true);
        networkService().publishAuthorizedData(data, devKeyPair).join();
        difficultyAdjustment = Optional.of(data);
    }

    private void removeDifficultyAdjustment() {
        AuthorizedDifficultyAdjustmentData data = difficultyAdjustment
                .orElseThrow(() -> new IllegalStateException("No difficulty adjustment to remove"));
        networkService().removeAuthorizedData(data, devKeyPair).join();
        difficultyAdjustment = Optional.empty();
    }

    // As OracleNodeService of a root oracle node, whose key is a static key. Nodes of version 2.1.13 accept the
    // AuthorizedOracleNode only from the key of the network ID of the oracle node, so this key publishes it, and the
    // dev key signs it. The publisher of the role of the oracle node is the dev key, or the key of the network ID as
    // at an oracle node of version 2.1.13.
    private void publishOracleNode(KeyPair publisherOfRole) {
        Identity identity = applicationService.getIdentityService().getOrCreateDefaultIdentity();
        NetworkId networkId = identity.getNetworkId();
        String authorizedPublicKey = Hex.encode(devKeyPair.getPublic().getEncoded());
        AuthorizedOracleNode myOracleNode = new AuthorizedOracleNode(networkId,
                devProfileId,
                authorizedPublicKey,
                "e2e-oracle",
                BOND_SIGNATURE_BASE64,
                true);
        AuthorizedBondedRole myRole = new AuthorizedBondedRole(devProfileId,
                authorizedPublicKey,
                BondedRoleType.ORACLE_NODE,
                "e2e-oracle",
                BOND_SIGNATURE_BASE64,
                Optional.of(networkId.getAddressByTransportTypeMap()),
                networkId,
                Optional.of(myOracleNode),
                true);
        publish(myRole, publisherOfRole);
        publish(myOracleNode, identity.getNetworkIdWithKeyPair().getKeyPair());
        oracleNode = Optional.of(myOracleNode);
    }

    // As Bisq1BridgeRequestService.toAuthorizedBondedRole for a registration request of a mediator. A request of
    // version 2.1.13 has no protocol version, which counts as the legacy version.
    private void publishMediatorRole(String profileId, int registrationProtocolVersion, KeyPair publisher) {
        AuthorizedOracleNode myOracleNode = oracleNode
                .orElseThrow(() -> new IllegalStateException("The command oracle must come first"));
        UserProfile mediator = applicationService.getUserService().getUserProfileService().findUserProfile(profileId)
                .orElseThrow(() -> new IllegalStateException("Unknown user profile " + profileId));
        boolean hasProposal = registrationProtocolVersion == BondedRoleRegistrationProtocol.PROPOSAL_KEY_VERSION;
        AuthorizedBondedRole role = new AuthorizedBondedRole(profileId,
                Hex.encode(mediator.getNetworkId().getPubKey().getPublicKey().getEncoded()),
                BondedRoleType.MEDIATOR,
                "e2e-" + mediator.getNickName(),
                BOND_SIGNATURE_BASE64,
                Optional.empty(),
                mediator.getNetworkId(),
                Optional.of(myOracleNode),
                false,
                registrationProtocolVersion,
                hasProposal ? transactionId(profileId, "proposal") : "",
                hasProposal ? transactionId(profileId, "lockup") : "");
        publish(role, publisher);
        mediatorRolesByProfileId.put(profileId, role);
    }

    private void removeMediatorRole(String profileId) {
        AuthorizedBondedRole role = mediatorRolesByProfileId.remove(profileId);
        checkState(role != null, "No role of mediator %s to remove", profileId);
        networkService().removeAuthorizedData(role, devKeyPair).join();
    }

    // The dev key signs the data as its authorized key, and the publisher signs the add request
    private void publish(AuthorizedDistributedData data, KeyPair publisher) {
        networkService().publishAuthorizedData(data, publisher, devKeyPair.getPrivate(), devKeyPair.getPublic())
                .join();
    }

    private KeyPair networkIdKeyPair() {
        return applicationService.getIdentityService().getOrCreateDefaultIdentity().getNetworkIdWithKeyPair()
                .getKeyPair();
    }

    // A Bitcoin transaction ID in its format, 64 hex characters, which differs for each profile and purpose
    private static String transactionId(String profileId, String purpose) {
        return Hex.encode(DigestUtil.sha256((purpose + profileId).getBytes(StandardCharsets.UTF_8)));
    }

    private NetworkService networkService() {
        return applicationService.getNetworkService();
    }
}

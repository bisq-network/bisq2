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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.bonded_roles.bonded_role;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.release.ReleaseNotification;
import bisq.bonded_roles.release.ReleaseNotificationsService;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.bonded_roles.security_manager.difficulty_adjustment.AuthorizedDifficultyAdjustmentData;
import bisq.bonded_roles.security_manager.difficulty_adjustment.DifficultyAdjustmentService;
import bisq.common.encoding.Hex;
import bisq.network.NetworkService;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.persistence.PersistenceService;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static bisq.bonded_roles.registration.BondedRoleRegistrationRequestTest.createNetworkId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Several keys can publish equal authorized data, for example two security managers the same alert: the alert view of
 * the desktop publishes every alert again with the key of the security manager who opens it. A node keeps the data
 * while one publication of it remains which the authorizing role type authorized, as a node which starts does (rule 18
 * of docs/specifications/bonded-roles/authorized-data.md, finding O-3 of the review 2026-10-03).
 * <p>
 * The test uses the real DataService with its stores, the real AuthorizedBondedRolesService and the real services.
 */
class AuthorizedDataPublicationsTest {
    @TempDir
    Path tempDir;

    private DataService dataService;
    private final List<AuthenticatedDataStorageService> stores = new ArrayList<>();
    private AuthorizedBondedRolesService authorizedBondedRolesService;
    private KeyPair securityManagerKeyPair;
    private KeyPair otherSecurityManagerKeyPair;
    private KeyPair releaseManagerKeyPair;
    private KeyPair otherReleaseManagerKeyPair;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(AuthorizedAlertData.class, AuthorizedAlertData.META_DATA);
        StorableTypesRegistry.register(ReleaseNotification.class, ReleaseNotification.META_DATA);
        StorableTypesRegistry.register(AuthorizedDifficultyAdjustmentData.class,
                AuthorizedDifficultyAdjustmentData.META_DATA);
        dataService = new DataService(new PersistenceService(tempDir));
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));
        authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService, false, Set.of(), true);
        // As NetworkService.addDataServiceListener does
        dataService.addListener(authorizedBondedRolesService);

        securityManagerKeyPair = addRole(BondedRoleType.SECURITY_MANAGER, "a");
        otherSecurityManagerKeyPair = addRole(BondedRoleType.SECURITY_MANAGER, "b");
        releaseManagerKeyPair = addRole(BondedRoleType.RELEASE_MANAGER, "c");
        otherReleaseManagerKeyPair = addRole(BondedRoleType.RELEASE_MANAGER, "d");
    }

    // The stores write asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files
    // in it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        stores.forEach(store -> store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join());
        dataService.shutdown();
    }

    @Test
    void alertAndItsBanStayWhileAnotherSecurityManagerPublishesTheAlert() throws GeneralSecurityException {
        AlertService alertService = new AlertService(authorizedBondedRolesService);
        alertService.initialize().join();
        BondedRole bannedRole = new BondedRole(createRole(BondedRoleType.MEDIATOR, "e".repeat(40), "public-key"));
        authorizedBondedRolesService.getBondedRoles().add(bannedRole);
        AuthorizedAlertData banAlert = createBanAlert(bannedRole.getAuthorizedBondedRole());
        AuthorizedData publication = publish(banAlert, securityManagerKeyPair);
        // As the alert view does when another security manager opens it
        AuthorizedData publicationOfOtherSecurityManager = publish(banAlert, otherSecurityManagerKeyPair);
        assertThat(alertService.getAuthorizedAlertDataSet()).containsExactly(banAlert);
        assertThat(bannedRole.isBanned()).isTrue();

        remove(publication, securityManagerKeyPair);

        assertThat(alertService.getAuthorizedAlertDataSet()).containsExactly(banAlert);
        assertThat(bannedRole.isBanned()).isTrue();

        remove(publicationOfOtherSecurityManager, otherSecurityManagerKeyPair);

        assertThat(alertService.getAuthorizedAlertDataSet()).isEmpty();
        assertThat(bannedRole.isBanned()).isFalse();
    }

    // Any key can publish a copy of authorized data without the static flag. Its removal must not remove the alert.
    @Test
    void removalOfACopyOfAnotherKeyKeepsTheAlert() throws GeneralSecurityException {
        AlertService alertService = new AlertService(authorizedBondedRolesService);
        alertService.initialize().join();
        AuthorizedAlertData alert = createAlert();
        publish(alert, securityManagerKeyPair);
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        AuthorizedData copy = publish(alert, otherKeyPair);

        remove(copy, otherKeyPair);

        assertThat(alertService.getAuthorizedAlertDataSet()).containsExactly(alert);
    }

    @Test
    void releaseNotificationStaysWhileAnotherReleaseManagerPublishesIt() throws GeneralSecurityException {
        ReleaseNotificationsService releaseNotificationsService =
                new ReleaseNotificationsService(authorizedBondedRolesService);
        releaseNotificationsService.initialize().join();
        ReleaseNotification releaseNotification = new ReleaseNotification("release-id",
                System.currentTimeMillis(),
                false,
                false,
                "notes",
                "2.1.14",
                "c".repeat(40),
                false,
                AppType.DESKTOP);
        AuthorizedData publication = publish(releaseNotification, releaseManagerKeyPair);
        AuthorizedData publicationOfOtherReleaseManager = publish(releaseNotification, otherReleaseManagerKeyPair);

        remove(publication, releaseManagerKeyPair);

        assertThat(releaseNotificationsService.getReleaseNotifications()).containsExactly(releaseNotification);

        remove(publicationOfOtherReleaseManager, otherReleaseManagerKeyPair);

        assertThat(releaseNotificationsService.getReleaseNotifications()).isEmpty();
    }

    @Test
    void difficultyAdjustmentStaysWhileAnotherSecurityManagerPublishesIt() throws GeneralSecurityException {
        DifficultyAdjustmentService difficultyAdjustmentService =
                new DifficultyAdjustmentService(authorizedBondedRolesService);
        difficultyAdjustmentService.initialize().join();
        AuthorizedDifficultyAdjustmentData difficultyAdjustment = new AuthorizedDifficultyAdjustmentData(
                System.currentTimeMillis(),
                2,
                "a".repeat(40),
                false);
        AuthorizedData publication = publish(difficultyAdjustment, securityManagerKeyPair);
        AuthorizedData publicationOfOtherSecurityManager = publish(difficultyAdjustment, otherSecurityManagerKeyPair);
        assertThat(difficultyAdjustmentService.getMostRecentValueOrDefault().get()).isEqualTo(2);

        remove(publication, securityManagerKeyPair);

        assertThat(difficultyAdjustmentService.getMostRecentValueOrDefault().get()).isEqualTo(2);

        remove(publicationOfOtherSecurityManager, otherSecurityManagerKeyPair);

        assertThat(difficultyAdjustmentService.getMostRecentValueOrDefault().get())
                .isEqualTo(NetworkLoad.DEFAULT_DIFFICULTY_ADJUSTMENT);
    }

    private KeyPair addRole(BondedRoleType bondedRoleType, String profileIdChar) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        authorizedBondedRolesService.getBondedRoles().add(new BondedRole(createRole(bondedRoleType,
                profileIdChar.repeat(40),
                Hex.encode(keyPair.getPublic().getEncoded()))));
        return keyPair;
    }

    // The publisher is the authorized key, which owns authorized data (see
    // docs/specifications/network/storage-ownership.md)
    private AuthorizedData publish(AuthorizedDistributedData data, KeyPair keyPair) throws GeneralSecurityException {
        AuthorizedData authorizedData = new AuthorizedData(data,
                Optional.of(SignatureUtil.sign(data.serializeForHash(), keyPair.getPrivate())),
                keyPair.getPublic());
        AuthenticatedDataStorageService store = getStore(data);
        assertThat(store.add(AddAuthenticatedDataRequest.from(store, authorizedData, keyPair)).isSuccess()).isTrue();
        return authorizedData;
    }

    private void remove(AuthorizedData authorizedData, KeyPair keyPair) throws GeneralSecurityException {
        AuthenticatedDataStorageService store = getStore(authorizedData.getAuthorizedDistributedData());
        assertThat(store.remove(RemoveAuthenticatedDataRequest.from(store, authorizedData, keyPair)).isSuccess())
                .isTrue();
    }

    private AuthenticatedDataStorageService getStore(AuthorizedDistributedData data) {
        AuthenticatedDataStorageService store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(data.getClass().getSimpleName())
                .join();
        if (!stores.contains(store)) {
            stores.add(store);
        }
        return store;
    }

    private static AuthorizedAlertData createAlert() {
        return new AuthorizedAlertData("alert-id",
                System.currentTimeMillis(),
                AlertType.WARN,
                Optional.empty(),
                Optional.of("message"),
                false,
                false,
                Optional.empty(),
                Optional.empty(),
                "a".repeat(40),
                false,
                Optional.empty(),
                AppType.DESKTOP);
    }

    private static AuthorizedAlertData createBanAlert(AuthorizedBondedRole bannedRole) {
        return new AuthorizedAlertData("ban-id",
                System.currentTimeMillis(),
                AlertType.BAN,
                Optional.empty(),
                Optional.empty(),
                false,
                false,
                Optional.empty(),
                Optional.of(bannedRole),
                "a".repeat(40),
                false,
                Optional.empty(),
                AppType.DESKTOP);
    }

    private static AuthorizedBondedRole createRole(BondedRoleType bondedRoleType,
                                                   String profileId,
                                                   String authorizedPublicKey) {
        return new AuthorizedBondedRole(profileId,
                authorizedPublicKey,
                bondedRoleType,
                "user",
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                false);
    }
}

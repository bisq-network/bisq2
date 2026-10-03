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

package bisq.support.moderator;

import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.common.encoding.Hex;
import bisq.network.NetworkService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModeratorReportAdmissionTest {
    private ModeratorService service;
    private UserProfileService userProfileService;
    private UserIdentityService userIdentityService;
    private final UserProfile accused = mock(UserProfile.class);
    private int numSenderKeys;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        Persistence persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), any(ModeratorStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        UserService userService = mock(UserService.class);
        when(accused.getId()).thenReturn("accused of the received reports");
        userIdentityService = mock(UserIdentityService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        userProfileService = mock(UserProfileService.class);
        when(userService.getUserProfileService()).thenReturn(userProfileService);
        BondedRolesService bondedRolesService = mock(BondedRolesService.class);
        when(bondedRolesService.getAuthorizedBondedRolesService()).thenReturn(mock(AuthorizedBondedRolesService.class));
        ChatService chatService = mock(ChatService.class);
        when(chatService.getTwoPartyPrivateChatChannelService()).thenReturn(mock(TwoPartyPrivateChatChannelService.class));

        service = new ModeratorService(new ModeratorService.Config(false),
                persistenceService,
                mock(NetworkService.class),
                userService,
                bondedRolesService,
                chatService);
    }

    @Test
    void reportOfItsSenderIsStored() {
        PublicKey reporterKey = KeyGeneration.generateDefaultEcKeyPair().getPublic();

        service.processReportToModeratorMessage(report(profileId(reporterKey), "report"), reporterKey);

        assertEquals(1, service.getReportToModeratorMessages().size());
    }

    // Otherwise anybody could report in the name of another user
    @Test
    void reportInTheNameOfAnotherUserIsIgnored() {
        PublicKey senderKey = KeyGeneration.generateDefaultEcKeyPair().getPublic();
        String otherReporter = profileId(KeyGeneration.generateDefaultEcKeyPair().getPublic());

        service.processReportToModeratorMessage(report(otherReporter, "report"), senderKey);

        assertTrue(service.getReportToModeratorMessages().isEmpty());
    }

    @Test
    void reportsOfOneReporterAreLimited() {
        KeyPair reporter = KeyGeneration.generateDefaultEcKeyPair();
        String reporterId = profileId(reporter.getPublic());

        for (int i = 0; i < ModeratorService.MAX_REPORTS_PER_REPORTER + 5; i++) {
            service.processReportToModeratorMessage(report(reporterId, "report " + i), reporter.getPublic());
        }

        assertEquals(ModeratorService.MAX_REPORTS_PER_REPORTER, service.getReportToModeratorMessages().size());
    }

    @Test
    void fullStoreDropsReportOfUnknownReporterForReportOfKnownReporter() {
        fillWithReportsOfUnknownReporters(ModeratorService.MAX_REPORTS);
        PublicKey knownReporter = knownSenderKey();
        ReportToModeratorMessage report = report(profileId(knownReporter), "known");

        service.processReportToModeratorMessage(report, knownReporter);

        assertEquals(ModeratorService.MAX_REPORTS, service.getReportToModeratorMessages().size());
        assertTrue(service.getReportToModeratorMessages().contains(report));
    }

    @Test
    void reportOfUnknownReporterDoesNotDropReportOfKnownReporter() {
        PublicKey knownReporter = knownSenderKey();
        String knownReporterId = profileId(knownReporter);
        for (int i = 0; i < ModeratorService.MAX_REPORTS_PER_REPORTER; i++) {
            service.processReportToModeratorMessage(report(knownReporterId, "known " + i), knownReporter);
        }
        for (int i = 0; i < ModeratorService.MAX_REPORTS - ModeratorService.MAX_REPORTS_PER_REPORTER; i++) {
            PublicKey unknownReporter = senderKey();
            service.processReportToModeratorMessage(report(profileId(unknownReporter), "unknown " + i),
                    unknownReporter);
        }

        PublicKey unknownReporter = senderKey();
        service.processReportToModeratorMessage(report(profileId(unknownReporter), "new unknown"), unknownReporter);

        assertEquals(ModeratorService.MAX_REPORTS_PER_REPORTER, service.getReportToModeratorMessages().stream()
                .filter(stored -> stored.getReporterUserProfileId().equals(knownReporterId))
                .count());
    }

    @Test
    void fullStoreDropsReportOfReporterWithMostReports() {
        PublicKey frequentReporter = knownSenderKey();
        String frequentReporterId = profileId(frequentReporter);
        for (int i = 0; i < ModeratorService.MAX_REPORTS_PER_REPORTER; i++) {
            service.processReportToModeratorMessage(report(frequentReporterId, "frequent " + i), frequentReporter);
        }
        for (int i = 0; i < ModeratorService.MAX_REPORTS - ModeratorService.MAX_REPORTS_PER_REPORTER; i++) {
            PublicKey reporter = knownSenderKey();
            service.processReportToModeratorMessage(report(profileId(reporter), "report " + i), reporter);
        }
        PublicKey newReporter = knownSenderKey();
        ReportToModeratorMessage report = report(profileId(newReporter), "new");

        service.processReportToModeratorMessage(report, newReporter);

        assertTrue(service.getReportToModeratorMessages().contains(report));
        assertEquals(ModeratorService.MAX_REPORTS_PER_REPORTER - 1, service.getReportToModeratorMessages().stream()
                .filter(stored -> stored.getReporterUserProfileId().equals(frequentReporterId))
                .count());
    }

    @Test
    void reportWhichWouldBeDroppedFirstIsIgnoredWhenTheStoreIsFull() {
        for (int i = 0; i < ModeratorService.MAX_REPORTS; i++) {
            PublicKey reporter = knownSenderKey();
            service.processReportToModeratorMessage(report(profileId(reporter), "report " + i), reporter);
        }

        PublicKey unknownReporter = senderKey();
        ReportToModeratorMessage report = report(profileId(unknownReporter), "unknown");
        service.processReportToModeratorMessage(report, unknownReporter);

        assertEquals(ModeratorService.MAX_REPORTS, service.getReportToModeratorMessages().size());
        assertFalse(service.getReportToModeratorMessages().contains(report));
    }

    @Test
    void reportReceivedAgainDoesNotDropAnotherReport() {
        PublicKey reporter = knownSenderKey();
        ReportToModeratorMessage report = report(profileId(reporter), "report");
        service.processReportToModeratorMessage(report, reporter);
        fillWithReportsOfKnownReporters(ModeratorService.MAX_REPORTS - 1);
        Set<ReportToModeratorMessage> storedReports = Set.copyOf(service.getReportToModeratorMessages());

        service.processReportToModeratorMessage(report, reporter);

        assertEquals(storedReports, Set.copyOf(service.getReportToModeratorMessages()));
    }

    @Test
    void fullStoreDropsTheOldestReportOfEqualReporters() {
        long now = System.currentTimeMillis();
        PublicKey oldestReporter = knownSenderKey();
        ReportToModeratorMessage oldestReport = report(profileId(oldestReporter), "oldest", now - 2_000);
        service.processReportToModeratorMessage(oldestReport, oldestReporter);
        for (int i = 0; i < ModeratorService.MAX_REPORTS - 1; i++) {
            PublicKey reporter = knownSenderKey();
            service.processReportToModeratorMessage(report(profileId(reporter), "report " + i, now - 1_000), reporter);
        }

        PublicKey newReporter = knownSenderKey();
        service.processReportToModeratorMessage(report(profileId(newReporter), "new", now), newReporter);

        assertEquals(ModeratorService.MAX_REPORTS, service.getReportToModeratorMessages().size());
        assertFalse(service.getReportToModeratorMessages().contains(oldestReport));
    }

    @Test
    void selfReportsAreStoredWithinTheTotalLimit() {
        String moderatorId = profileId(senderKey());
        UserIdentity moderator = mock(UserIdentity.class, RETURNS_DEEP_STUBS);
        when(moderator.getUserProfile().getId()).thenReturn(moderatorId);
        when(userIdentityService.getSelectedUserIdentity()).thenReturn(moderator);
        fillWithReportsOfKnownReporters(ModeratorService.MAX_REPORTS);

        UserProfile accusedOfSelfReport = mock(UserProfile.class);
        when(accusedOfSelfReport.getId()).thenReturn("accused");
        when(userProfileService.findUserProfile("accused")).thenReturn(Optional.of(accusedOfSelfReport));
        service.selfReportRateLimitExceedingUserProfileId("accused");

        assertEquals(ModeratorService.MAX_REPORTS, service.getReportToModeratorMessages().size());
    }

    private void fillWithReportsOfKnownReporters(int numReports) {
        for (int i = 0; i < numReports; i++) {
            PublicKey reporter = knownSenderKey();
            service.processReportToModeratorMessage(report(profileId(reporter), "known " + i), reporter);
        }
    }

    private void fillWithReportsOfUnknownReporters(int numReports) {
        PublicKey reporter = senderKey();
        for (int i = 0; i < numReports; i++) {
            if (i % ModeratorService.MAX_REPORTS_PER_REPORTER == 0) {
                reporter = senderKey();
            }
            service.processReportToModeratorMessage(report(profileId(reporter), "unknown " + i), reporter);
        }
    }

    // The reporter ID is derived from the encoded sender key only, so distinct bytes are enough
    private PublicKey senderKey() {
        PublicKey publicKey = mock(PublicKey.class);
        when(publicKey.getEncoded()).thenReturn(("sender key " + numSenderKeys++).getBytes(StandardCharsets.UTF_8));
        return publicKey;
    }

    private PublicKey knownSenderKey() {
        PublicKey publicKey = senderKey();
        when(userProfileService.findUserProfile(profileId(publicKey))).thenReturn(Optional.of(mock(UserProfile.class)));
        return publicKey;
    }

    private ReportToModeratorMessage report(String reporterUserProfileId, String text) {
        return report(reporterUserProfileId, text, System.currentTimeMillis());
    }

    private ReportToModeratorMessage report(String reporterUserProfileId, String text, long date) {
        return new ReportToModeratorMessage(date,
                reporterUserProfileId,
                accused,
                text,
                ChatChannelDomain.DISCUSSION);
    }

    private static String profileId(PublicKey publicKey) {
        return Hex.encode(DigestUtil.hash(publicKey.getEncoded()));
    }
}

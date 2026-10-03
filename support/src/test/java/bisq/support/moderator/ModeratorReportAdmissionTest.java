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
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModeratorReportAdmissionTest {
    private ModeratorService service;
    private final UserProfile accused = mock(UserProfile.class);

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        Persistence persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), any(ModeratorStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        UserService userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(mock(UserIdentityService.class));
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        when(userService.getUserProfileService()).thenReturn(mock(UserProfileService.class));
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

    private ReportToModeratorMessage report(String reporterUserProfileId, String text) {
        return new ReportToModeratorMessage(System.currentTimeMillis(),
                reporterUserProfileId,
                accused,
                text,
                ChatChannelDomain.DISCUSSION);
    }

    private static String profileId(PublicKey publicKey) {
        return Hex.encode(DigestUtil.hash(publicKey.getEncoded()));
    }
}

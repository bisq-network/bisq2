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

import bisq.common.proto.NetworkStorageWhiteList;
import bisq.common.proto.ProtoResolver;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the two invariants of the resolver registrations which nothing else checks.
 * <p>
 * The whitelist is the set of store keys the P2P storage accepts. A missing entry makes StorageService reject that
 * payload type on every node running the build. Pinning the expected names turns a wrong or forgotten class argument
 * into a build failure. Adding a payload type is expected to fail that test, add the new name to the list.
 * <p>
 * The proto type name and the class passed to addResolver are two hand written spellings of the same type, and the
 * proto type name is the wire contract, so it cannot be derived from the class: renaming a class must not change it.
 * The second test asserts the two agree, with the deliberate exceptions listed explicitly.
 */
class ResolverConfigTest {
    private static final Set<String> EXPECTED_CLASS_NAMES = new TreeSet<>(List.of(
            "AccountTimestamp",
            "AckMessage",
            "AuthorizeAccountAgeRequest",
            "AuthorizeAccountTimestampV1Request",
            "AuthorizeAccountTimestampV2Request",
            "AuthorizeSignedWitnessRequest",
            "AuthorizeTimestampRequest",
            "AuthorizedAccountAgeData",
            "AuthorizedAccountTimestamp",
            "AuthorizedAlertData",
            "AuthorizedBondedReputationData",
            "AuthorizedBondedRole",
            "AuthorizedBurningmanListByBlock",
            "AuthorizedDifficultyAdjustmentData",
            "AuthorizedMarketPriceData",
            "AuthorizedMinRequiredReputationScoreData",
            "AuthorizedOracleNode",
            "AuthorizedProofOfBurnData",
            "AuthorizedSignedWitnessData",
            "AuthorizedTimestampData",
            "BannedUserProfileData",
            "BisqEasyAccountDataMessage",
            "BisqEasyBtcAddressMessage",
            "BisqEasyCancelTradeMessage",
            "BisqEasyConfirmBtcSentMessage",
            "BisqEasyConfirmFiatReceiptMessage",
            "BisqEasyConfirmFiatSentMessage",
            "BisqEasyMediationRequest",
            "BisqEasyMediatorsResponse",
            "BisqEasyOfferbookMessage",
            "BisqEasyOfferbookMessageReaction",
            "BisqEasyOpenTradeMessage",
            "BisqEasyOpenTradeMessageReaction",
            "BisqEasyRejectTradeMessage",
            "BisqEasyReportErrorMessage",
            "BisqEasyTakeOfferRequest",
            "BisqEasyTakeOfferResponse",
            "BondedRoleRegistrationRequest",
            "ChatMessage",
            "ChatMessageReaction",
            "CommonPublicChatMessage",
            "CommonPublicChatMessageReaction",
            "CooperativeClosureMessage_G",
            "MuSigArbitrationRequest",
            "MuSigArbitrationStateChangeMessage",
            "MuSigDisputeCaseDataMessage",
            "MuSigDisputeCasePaymentDetailsRequest",
            "MuSigDisputeCasePaymentDetailsResponse",
            "MuSigMediationRequest",
            "MuSigMediationResultAcceptanceMessage",
            "MuSigMediationStateChangeMessage",
            "MuSigOfferMessage",
            "MuSigOpenTradeMessage",
            "MuSigOpenTradeMessageReaction",
            "MuSigReportErrorMessage",
            "PaymentInitiatedMessage_E",
            "PaymentReceivedMessage_F",
            "ReleaseNotification",
            "ReportToModeratorMessage",
            "SendAccountPayloadAndDepositTxMessage",
            "SendAccountPayloadMessage",
            "SetupTradeMessage_A",
            "SetupTradeMessage_B",
            "SetupTradeMessage_C",
            "SetupTradeMessage_D",
            "TradeMessage",
            "TwoPartyPrivateChatMessage",
            "TwoPartyPrivateChatMessageReaction",
            "UserProfile"    ));

    /**
     * Proto type names which deliberately differ from their class, because the class was renamed after the name was
     * already on the wire. Freezing a name is a conscious decision, so it belongs here rather than in a derivation rule.
     */
    private static final Map<String, String> LEGACY_PROTO_TYPE_NAMES = Map.of(
            "support.MediationRequest", "BisqEasyMediationRequest",
            "support.MediatorsResponse", "BisqEasyMediatorsResponse");

    private static final Pattern REGISTRATION = Pattern.compile("addResolver\\(\"([^\"]+)\",\\s*(\\w+)\\.class");
    private static final Pattern IMPORT = Pattern.compile("^import\\s+([\\w.]+)\\.(\\w+);");

    @Test
    void whiteListMatchesResolverRegistrations() {
        ResolverConfig.config();

        assertEquals(EXPECTED_CLASS_NAMES, new TreeSet<>(NetworkStorageWhiteList.getClassNames()));
    }

    @Test
    void protoTypeNamesMatchRegisteredClasses() throws Exception {
        List<String> source = Files.readAllLines(resolverConfigSource());
        Map<String, String> packageBySimpleName = new HashMap<>();
        for (String line : source) {
            Matcher matcher = IMPORT.matcher(line);
            if (matcher.find()) {
                packageBySimpleName.put(matcher.group(2), matcher.group(1));
            }
        }

        List<String> registrations = new ArrayList<>();
        List<String> mismatches = new ArrayList<>();
        for (String line : source) {
            Matcher matcher = REGISTRATION.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            String protoTypeName = matcher.group(1);
            String simpleName = matcher.group(2);
            registrations.add(protoTypeName);

            String legacyClassName = LEGACY_PROTO_TYPE_NAMES.get(protoTypeName);
            if (legacyClassName != null) {
                if (!legacyClassName.equals(simpleName)) {
                    mismatches.add(protoTypeName + " is frozen for " + legacyClassName + " but is registered with "
                            + simpleName);
                }
            } else {
                Class<?> clazz = Class.forName(packageBySimpleName.get(simpleName) + "." + simpleName);
                String derived = ProtoResolver.getProtoType(clazz);
                if (!protoTypeName.equals(derived)) {
                    mismatches.add(protoTypeName + " is registered with " + simpleName + " whose proto type name is "
                            + derived);
                }
            }
        }

        // Guards against a scan which found nothing, which would let the assertion below pass for the wrong reason.
        assertTrue(registrations.contains("user.UserProfile") && registrations.contains("support.MediationRequest"),
                "Source scan did not reach the known registrations, found " + registrations);
        assertEquals(List.of(), mismatches,
                "Proto type name and class disagree. Fix whichever is wrong, or if the name is frozen on the wire, "
                        + "add it to LEGACY_PROTO_TYPE_NAMES with a comment saying why");
    }

    private static Path resolverConfigSource() {
        Path path = Path.of("").toAbsolutePath();
        while (path != null && !Files.exists(path.resolve("settings.gradle.kts"))) {
            path = path.getParent();
        }
        if (path == null) {
            throw new IllegalStateException("No settings.gradle.kts found above " + Path.of("").toAbsolutePath());
        }
        return path.resolve("application/src/main/java/bisq/application/ResolverConfig.java");
    }
}

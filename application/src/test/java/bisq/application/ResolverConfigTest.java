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
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers the invariants of the resolver registrations which nothing else checks. Each one fails silently in
 * production: the payload is simply refused or cannot be decoded, on every node running the build.
 * <p>
 * The whitelist is the set of store keys the P2P storage accepts, so a missing entry makes StorageService reject that
 * payload type. Adding a payload type is expected to fail that test, add the new name to the list.
 * <p>
 * A registration names one type three times: as the proto type name on the wire, as the class the whitelist is keyed
 * by, and as the owner of the decoder. The wire name is a contract and cannot be derived from the class, since
 * renaming a class must not change it, so the three are checked against each other instead, with the deliberately
 * frozen names listed explicitly.
 * <p>
 * Each registry is pinned separately, because a type dropped from one of the two still leaves that registry unable to
 * decode it while the shared whitelist looks untouched.
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
            "MuSigCustomPayoutPsbtMessage",
            "MuSigDisputeCaseDataMessage",
            "MuSigDisputeCasePaymentDetailsRequest",
            "MuSigDisputeCasePaymentDetailsResponse",
            "MuSigMediationRequest",
            "MuSigMediationResultRejectionMessage",
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
            "UserProfile"));

    /**
     * Proto type names which deliberately differ from their class, because the class was renamed after the name was
     * already on the wire. Freezing a name is a conscious decision, so it belongs here rather than in a derivation rule.
     */
    private static final Map<String, String> LEGACY_PROTO_TYPE_NAMES = Map.of(
            "support.MediationRequest", "BisqEasyMediationRequest",
            "support.MediatorsResponse", "BisqEasyMediatorsResponse");

    /**
     * The proto type names each registry must hold. Pinned per registry rather than as one set, because a type
     * registered in only one of the two still leaves the other unable to decode it while the shared whitelist looks
     * untouched.
     */
    private static final Set<String> DISTRIBUTED_DATA_TYPES = new TreeSet<>(List.of(
            "account.AccountTimestamp",
            "account.AuthorizedAccountTimestamp",
            "bonded_roles.AuthorizedAlertData",
            "bonded_roles.AuthorizedBondedRole",
            "bonded_roles.AuthorizedDifficultyAdjustmentData",
            "bonded_roles.AuthorizedMarketPriceData",
            "bonded_roles.AuthorizedMinRequiredReputationScoreData",
            "bonded_roles.AuthorizedOracleNode",
            "bonded_roles.ReleaseNotification",
            "burningman.AuthorizedBurningmanListByBlock",
            "chat.ChatMessage",
            "chat.ChatMessageReaction",
            "offer.MuSigOfferMessage",
            "user.AuthorizedAccountAgeData",
            "user.AuthorizedBondedReputationData",
            "user.AuthorizedProofOfBurnData",
            "user.AuthorizedSignedWitnessData",
            "user.AuthorizedTimestampData",
            "user.BannedUserProfileData",
            "user.UserProfile"));

    private static final Set<String> NETWORK_MESSAGE_TYPES = new TreeSet<>(List.of(
            "account.AuthorizeAccountTimestampV1Request",
            "account.AuthorizeAccountTimestampV2Request",
            "bonded_roles.BondedRoleRegistrationRequest",
            "chat.ChatMessage",
            "chat.ChatMessageReaction",
            "support.MediationRequest",
            "support.MediatorsResponse",
            "support.MuSigArbitrationRequest",
            "support.MuSigArbitrationStateChangeMessage",
            "support.MuSigDisputeCaseDataMessage",
            "support.MuSigDisputeCasePaymentDetailsRequest",
            "support.MuSigDisputeCasePaymentDetailsResponse",
            "support.MuSigMediationRequest",
            "support.MuSigMediationStateChangeMessage",
            "support.ReportToModeratorMessage",
            "trade.TradeMessage",
            "user.AuthorizeAccountAgeRequest",
            "user.AuthorizeSignedWitnessRequest",
            "user.AuthorizeTimestampRequest"));

    @Test
    void whiteListMatchesResolverRegistrations() {
        ResolverConfig.config();

        assertEquals(EXPECTED_CLASS_NAMES, new TreeSet<>(NetworkStorageWhiteList.getClassNames()));
    }

    @Test
    void everyRegistrationNamesOneTypeConsistently() throws Exception {
        List<Registration> registrations = parseRegistrations();
        Map<String, String> packageBySimpleName = new HashMap<>();
        for (ImportTree importTree : compilationUnit().getImports()) {
            String qualified = importTree.getQualifiedIdentifier().toString();
            packageBySimpleName.put(qualified.substring(qualified.lastIndexOf('.') + 1),
                    qualified.substring(0, qualified.lastIndexOf('.')));
        }

        List<String> violations = new ArrayList<>();
        for (Registration registration : registrations) {
            if (!registration.clazz().equals(registration.resolverOwner())) {
                violations.add(registration.protoTypeName() + " is registered with " + registration.clazz()
                        + " but decodes with " + registration.resolverOwner() + ".getResolver, so the payload type and"
                        + " the decoder disagree");
                continue;
            }
            String legacyClassName = LEGACY_PROTO_TYPE_NAMES.get(registration.protoTypeName());
            if (legacyClassName != null) {
                if (!legacyClassName.equals(registration.clazz())) {
                    violations.add(registration.protoTypeName() + " is frozen for " + legacyClassName
                            + " but is registered with " + registration.clazz());
                }
            } else {
                Class<?> clazz = Class.forName(packageBySimpleName.get(registration.clazz())
                        + "." + registration.clazz());
                String derived = ProtoResolver.getProtoType(clazz);
                if (!registration.protoTypeName().equals(derived)) {
                    violations.add(registration.protoTypeName() + " is registered with " + registration.clazz()
                            + " whose proto type name is " + derived);
                }
            }
        }
        assertEquals(List.of(), violations,
                "Fix whichever argument is wrong, or if a name is frozen on the wire, add it to "
                        + "LEGACY_PROTO_TYPE_NAMES with a comment saying why");
    }

    @Test
    void everyRegistryHoldsExactlyTheExpectedTypes() throws Exception {
        List<Registration> registrations = parseRegistrations();

        assertEquals(DISTRIBUTED_DATA_TYPES, typesRegisteredIn("DistributedDataResolver", registrations));
        assertEquals(NETWORK_MESSAGE_TYPES, typesRegisteredIn("NetworkMessageResolver", registrations));
    }

    private static Set<String> typesRegisteredIn(String registry, List<Registration> registrations) {
        return registrations.stream()
                .filter(registration -> registry.equals(registration.registry()))
                .map(Registration::protoTypeName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static final Set<String> REGISTRIES = Set.of("DistributedDataResolver", "NetworkMessageResolver");

    private record Registration(String registry, String protoTypeName, String clazz, String resolverOwner) {
    }

    /**
     * Parsed with the java compiler rather than matched with a regular expression. A text pattern silently skips the
     * calls it was not written for, which costs coverage without failing anything, and it cannot see the third argument
     * at all.
     * <p>
     * A call to one of the two registries which does not have the expected shape is reported rather than skipped. The
     * pinned type sets catch a registration that disappears from the parse, but not one that was never in them, so a
     * newly added registration written in an unrecognised form would otherwise go unchecked.
     */
    private static List<Registration> parseRegistrations() throws IOException {
        List<Registration> registrations = new ArrayList<>();
        List<String> unrecognised = new ArrayList<>();
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                if (node.getMethodSelect() instanceof MemberSelectTree select
                        && "addResolver".contentEquals(select.getIdentifier())
                        && REGISTRIES.contains(select.getExpression().toString())) {
                    if (node.getArguments().size() == 3
                            && node.getArguments().get(0) instanceof LiteralTree protoTypeName
                            && node.getArguments().get(1) instanceof MemberSelectTree classLiteral
                            && node.getArguments().get(2) instanceof MethodInvocationTree resolverCall
                            && resolverCall.getMethodSelect() instanceof MemberSelectTree resolverSelect) {
                        registrations.add(new Registration(select.getExpression().toString(),
                                protoTypeName.getValue().toString(),
                                classLiteral.getExpression().toString(),
                                resolverSelect.getExpression().toString()));
                    } else {
                        unrecognised.add(node.toString());
                    }
                }
                return super.visitMethodInvocation(node, unused);
            }
        }.scan(compilationUnit(), null);

        assertEquals(List.of(), unrecognised,
                "A registration in this form is not checked by anything. Write it as "
                        + "addResolver(\"proto.TypeName\", TypeName.class, TypeName.getResolver()), or teach "
                        + "parseRegistrations to read the new form");
        return registrations;
    }

    private static CompilationUnitTree compilationUnit() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "No java compiler available, the test needs a JDK rather than a JRE");
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, fileManager, diagnostic -> {
            }, List.of("-proc:none"), null,
                    fileManager.getJavaFileObjectsFromPaths(List.of(resolverConfigSource())));
            return task.parse().iterator().next();
        }
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

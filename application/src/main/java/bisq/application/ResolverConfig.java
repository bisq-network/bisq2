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

import bisq.account.timestamp.AccountTimestamp;
import bisq.account.timestamp.AuthorizeAccountTimestampV1Request;
import bisq.account.timestamp.AuthorizeAccountTimestampV2Request;
import bisq.account.timestamp.AuthorizedAccountTimestamp;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.market_price.AuthorizedMarketPriceData;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.bonded_roles.release.ReleaseNotification;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.bonded_roles.security_manager.difficulty_adjustment.AuthorizedDifficultyAdjustmentData;
import bisq.bonded_roles.security_manager.min_reputation_score.AuthorizedMinRequiredReputationScoreData;
import bisq.burningman.AuthorizedBurningmanListByBlock;
import bisq.chat.ChatMessage;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.chat.reactions.BisqEasyOfferbookMessageReaction;
import bisq.chat.reactions.BisqEasyOpenTradeMessageReaction;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.chat.reactions.CommonPublicChatMessageReaction;
import bisq.chat.reactions.MuSigOpenTradeMessageReaction;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.network.p2p.message.NetworkMessageResolver;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.storage.DistributedDataResolver;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.offer.mu_sig.MuSigOfferMessage;
import bisq.support.arbitration.mu_sig.MuSigArbitrationRequest;
import bisq.support.arbitration.mu_sig.MuSigArbitrationStateChangeMessage;
import bisq.support.mediation.bisq_easy.BisqEasyMediationRequest;
import bisq.support.mediation.bisq_easy.BisqEasyMediatorsResponse;
import bisq.support.dispute.mu_sig.MuSigDisputeCaseDataMessage;
import bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsRequest;
import bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsResponse;
import bisq.support.mediation.mu_sig.MuSigMediationRequest;
import bisq.support.mediation.mu_sig.MuSigMediationStateChangeMessage;
import bisq.support.moderator.ReportToModeratorMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyAccountDataMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyBtcAddressMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyCancelTradeMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmBtcSentMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmFiatReceiptMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmFiatSentMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyRejectTradeMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequest;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferResponse;
import bisq.trade.mu_sig.messages.network.CooperativeClosureMessage_G;
import bisq.trade.mu_sig.messages.network.MuSigCustomPayoutPsbtMessage;
import bisq.trade.mu_sig.messages.network.MuSigMediationResultRejectionMessage;
import bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage;
import bisq.trade.mu_sig.messages.network.PaymentInitiatedMessage_E;
import bisq.trade.mu_sig.messages.network.PaymentReceivedMessage_F;
import bisq.trade.mu_sig.messages.network.SendAccountPayloadAndDepositTxMessage;
import bisq.trade.mu_sig.messages.network.SendAccountPayloadMessage;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_B;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_C;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_D;
import bisq.trade.protocol.messages.TradeMessage;
import bisq.user.banned.BannedUserProfileData;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.data.AuthorizedBondedReputationData;
import bisq.user.reputation.data.AuthorizedProofOfBurnData;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import bisq.user.reputation.data.AuthorizedTimestampData;
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import bisq.user.reputation.requests.AuthorizeTimestampRequest;

public class ResolverConfig {
    public static void config() {
        // Register resolvers for distributedData
        // Abstract classes
        DistributedDataResolver.addResolver("chat.ChatMessage", ChatMessage.class, ChatMessage.getDistributedDataResolver());
        DistributedDataResolver.addResolver("chat.ChatMessageReaction", ChatMessageReaction.class, ChatMessageReaction.getDistributedDataResolver());

        // Final classes
        DistributedDataResolver.addResolver("user.UserProfile", UserProfile.class, UserProfile.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedOracleNode", AuthorizedOracleNode.class, AuthorizedOracleNode.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedBondedRole", AuthorizedBondedRole.class, AuthorizedBondedRole.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedAlertData", AuthorizedAlertData.class, AuthorizedAlertData.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedDifficultyAdjustmentData", AuthorizedDifficultyAdjustmentData.class, AuthorizedDifficultyAdjustmentData.getResolver());
        //noinspection deprecation
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedMinRequiredReputationScoreData", AuthorizedMinRequiredReputationScoreData.class, AuthorizedMinRequiredReputationScoreData.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.ReleaseNotification", ReleaseNotification.class, ReleaseNotification.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedMarketPriceData", AuthorizedMarketPriceData.class, AuthorizedMarketPriceData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedProofOfBurnData", AuthorizedProofOfBurnData.class, AuthorizedProofOfBurnData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedBondedReputationData", AuthorizedBondedReputationData.class, AuthorizedBondedReputationData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedAccountAgeData", AuthorizedAccountAgeData.class, AuthorizedAccountAgeData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedSignedWitnessData", AuthorizedSignedWitnessData.class, AuthorizedSignedWitnessData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedTimestampData", AuthorizedTimestampData.class, AuthorizedTimestampData.getResolver());
        DistributedDataResolver.addResolver("user.BannedUserProfileData", BannedUserProfileData.class, BannedUserProfileData.getResolver());
        DistributedDataResolver.addResolver("offer.MuSigOfferMessage", MuSigOfferMessage.class, MuSigOfferMessage.getResolver());
        DistributedDataResolver.addResolver("burningman.AuthorizedBurningmanListByBlock", AuthorizedBurningmanListByBlock.class, AuthorizedBurningmanListByBlock.getResolver());
        DistributedDataResolver.addResolver("account.AccountTimestamp", AccountTimestamp.class, AccountTimestamp.getResolver());
        DistributedDataResolver.addResolver("account.AuthorizedAccountTimestamp", AuthorizedAccountTimestamp.class, AuthorizedAccountTimestamp.getResolver());

        // Register resolvers for networkMessages 
        // Abstract classes
        NetworkMessageResolver.addResolver("chat.ChatMessage", ChatMessage.class, ChatMessage.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("trade.TradeMessage", TradeMessage.class, TradeMessage.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("chat.ChatMessageReaction", ChatMessageReaction.class, ChatMessageReaction.getNetworkMessageResolver());

        // Final classes
        NetworkMessageResolver.addResolver("user.AuthorizeAccountAgeRequest", AuthorizeAccountAgeRequest.class, AuthorizeAccountAgeRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("user.AuthorizeSignedWitnessRequest", AuthorizeSignedWitnessRequest.class, AuthorizeSignedWitnessRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("user.AuthorizeTimestampRequest", AuthorizeTimestampRequest.class, AuthorizeTimestampRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("bonded_roles.BondedRoleRegistrationRequest", BondedRoleRegistrationRequest.class, BondedRoleRegistrationRequest.getNetworkMessageResolver());
        // Keep proto name for backward compatibility
        NetworkMessageResolver.addResolver("support.MediationRequest", BisqEasyMediationRequest.class, BisqEasyMediationRequest.getNetworkMessageResolver());
        // Keep proto name for backward compatibility
        NetworkMessageResolver.addResolver("support.MediatorsResponse", BisqEasyMediatorsResponse.class, BisqEasyMediatorsResponse.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.MuSigDisputeCaseDataMessage", MuSigDisputeCaseDataMessage.class, MuSigDisputeCaseDataMessage.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.MuSigMediationRequest", MuSigMediationRequest.class, MuSigMediationRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.MuSigMediationStateChangeMessage", MuSigMediationStateChangeMessage.class, MuSigMediationStateChangeMessage.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.MuSigDisputeCasePaymentDetailsRequest", MuSigDisputeCasePaymentDetailsRequest.class, MuSigDisputeCasePaymentDetailsRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.MuSigDisputeCasePaymentDetailsResponse", MuSigDisputeCasePaymentDetailsResponse.class, MuSigDisputeCasePaymentDetailsResponse.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.MuSigArbitrationRequest", MuSigArbitrationRequest.class, MuSigArbitrationRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.MuSigArbitrationStateChangeMessage", MuSigArbitrationStateChangeMessage.class, MuSigArbitrationStateChangeMessage.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.ReportToModeratorMessage", ReportToModeratorMessage.class, ReportToModeratorMessage.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("account.AuthorizeAccountTimestampV1Request", AuthorizeAccountTimestampV1Request.class, AuthorizeAccountTimestampV1Request.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("account.AuthorizeAccountTimestampV2Request", AuthorizeAccountTimestampV2Request.class, AuthorizeAccountTimestampV2Request.getNetworkMessageResolver());


        // The network storage accepts only the types registered here. It takes their MetaData only from the
        // StorableTypesRegistry, never from the network.

        // DistributedData
        StorableTypesRegistry.register(UserProfile.class, UserProfile.META_DATA);
        StorableTypesRegistry.register(AuthorizedOracleNode.class, AuthorizedOracleNode.META_DATA);
        StorableTypesRegistry.register(AuthorizedBondedRole.class, AuthorizedBondedRole.META_DATA);
        StorableTypesRegistry.register(AuthorizedAlertData.class, AuthorizedAlertData.META_DATA);
        StorableTypesRegistry.register(AuthorizedDifficultyAdjustmentData.class, AuthorizedDifficultyAdjustmentData.META_DATA);
        //noinspection deprecation
        StorableTypesRegistry.register(AuthorizedMinRequiredReputationScoreData.class, AuthorizedMinRequiredReputationScoreData.META_DATA);
        StorableTypesRegistry.register(ReleaseNotification.class, ReleaseNotification.META_DATA);
        StorableTypesRegistry.register(AuthorizedMarketPriceData.class, AuthorizedMarketPriceData.META_DATA);
        StorableTypesRegistry.register(AuthorizedProofOfBurnData.class, AuthorizedProofOfBurnData.META_DATA);
        StorableTypesRegistry.register(AuthorizedBondedReputationData.class, AuthorizedBondedReputationData.META_DATA);
        StorableTypesRegistry.register(AuthorizedAccountAgeData.class, AuthorizedAccountAgeData.META_DATA);
        StorableTypesRegistry.register(AuthorizedSignedWitnessData.class, AuthorizedSignedWitnessData.META_DATA);
        StorableTypesRegistry.register(AuthorizedTimestampData.class, AuthorizedTimestampData.META_DATA);
        StorableTypesRegistry.register(BannedUserProfileData.class, BannedUserProfileData.META_DATA);
        StorableTypesRegistry.register(MuSigOfferMessage.class, MuSigOfferMessage.META_DATA);
        StorableTypesRegistry.register(AuthorizedBurningmanListByBlock.class, AuthorizedBurningmanListByBlock.META_DATA);
        StorableTypesRegistry.register(AccountTimestamp.class, AccountTimestamp.META_DATA);
        StorableTypesRegistry.register(AuthorizedAccountTimestamp.class, AuthorizedAccountTimestamp.META_DATA);

        // MailboxMessages
        StorableTypesRegistry.register(AuthorizeAccountAgeRequest.class, AuthorizeAccountAgeRequest.META_DATA);
        StorableTypesRegistry.register(AuthorizeSignedWitnessRequest.class, AuthorizeSignedWitnessRequest.META_DATA);
        StorableTypesRegistry.register(AuthorizeTimestampRequest.class, AuthorizeTimestampRequest.META_DATA);
        StorableTypesRegistry.register(BondedRoleRegistrationRequest.class, BondedRoleRegistrationRequest.META_DATA);
        StorableTypesRegistry.register(BisqEasyMediationRequest.class, BisqEasyMediationRequest.META_DATA);
        StorableTypesRegistry.register(BisqEasyMediatorsResponse.class, BisqEasyMediatorsResponse.META_DATA);
        StorableTypesRegistry.register(MuSigDisputeCaseDataMessage.class, MuSigDisputeCaseDataMessage.META_DATA);
        StorableTypesRegistry.register(MuSigMediationRequest.class, MuSigMediationRequest.META_DATA);
        StorableTypesRegistry.register(MuSigMediationStateChangeMessage.class, MuSigMediationStateChangeMessage.META_DATA);
        StorableTypesRegistry.register(MuSigDisputeCasePaymentDetailsRequest.class, MuSigDisputeCasePaymentDetailsRequest.META_DATA);
        StorableTypesRegistry.register(MuSigDisputeCasePaymentDetailsResponse.class, MuSigDisputeCasePaymentDetailsResponse.META_DATA);
        StorableTypesRegistry.register(MuSigArbitrationRequest.class, MuSigArbitrationRequest.META_DATA);
        StorableTypesRegistry.register(MuSigArbitrationStateChangeMessage.class, MuSigArbitrationStateChangeMessage.META_DATA);
        StorableTypesRegistry.register(ReportToModeratorMessage.class, ReportToModeratorMessage.META_DATA);
        StorableTypesRegistry.register(AuthorizeAccountTimestampV1Request.class, AuthorizeAccountTimestampV1Request.META_DATA);
        StorableTypesRegistry.register(AuthorizeAccountTimestampV2Request.class, AuthorizeAccountTimestampV2Request.META_DATA);

        // ChatMessage subclasses
        StorableTypesRegistry.register(CommonPublicChatMessage.class, CommonPublicChatMessage.META_DATA);
        StorableTypesRegistry.register(TwoPartyPrivateChatMessage.class, TwoPartyPrivateChatMessage.META_DATA);
        StorableTypesRegistry.register(BisqEasyOfferbookMessage.class, BisqEasyOfferbookMessage.META_DATA);
        StorableTypesRegistry.register(BisqEasyOpenTradeMessage.class, BisqEasyOpenTradeMessage.META_DATA);
        StorableTypesRegistry.register(MuSigOpenTradeMessage.class, MuSigOpenTradeMessage.META_DATA);

        // BisqEasyTradeMessage subclasses
        StorableTypesRegistry.register(BisqEasyReportErrorMessage.class, BisqEasyReportErrorMessage.META_DATA);
        StorableTypesRegistry.register(BisqEasyTakeOfferRequest.class, BisqEasyTakeOfferRequest.META_DATA);
        StorableTypesRegistry.register(BisqEasyTakeOfferResponse.class, BisqEasyTakeOfferResponse.META_DATA);
        StorableTypesRegistry.register(BisqEasyCancelTradeMessage.class, BisqEasyCancelTradeMessage.META_DATA);
        StorableTypesRegistry.register(BisqEasyRejectTradeMessage.class, BisqEasyRejectTradeMessage.META_DATA);
        StorableTypesRegistry.register(BisqEasyAccountDataMessage.class, BisqEasyAccountDataMessage.META_DATA);
        StorableTypesRegistry.register(BisqEasyBtcAddressMessage.class, BisqEasyBtcAddressMessage.META_DATA);
        StorableTypesRegistry.register(BisqEasyConfirmFiatSentMessage.class, BisqEasyConfirmFiatSentMessage.META_DATA);
        StorableTypesRegistry.register(BisqEasyConfirmFiatReceiptMessage.class, BisqEasyConfirmFiatReceiptMessage.META_DATA);
        StorableTypesRegistry.register(BisqEasyConfirmBtcSentMessage.class, BisqEasyConfirmBtcSentMessage.META_DATA);

        // MuSigTradeMessage subclasses
        StorableTypesRegistry.register(MuSigReportErrorMessage.class, MuSigReportErrorMessage.META_DATA);
        StorableTypesRegistry.register(SetupTradeMessage_A.class, SetupTradeMessage_A.META_DATA);
        StorableTypesRegistry.register(SetupTradeMessage_B.class, SetupTradeMessage_B.META_DATA);
        StorableTypesRegistry.register(SetupTradeMessage_C.class, SetupTradeMessage_C.META_DATA);
        StorableTypesRegistry.register(SetupTradeMessage_D.class, SetupTradeMessage_D.META_DATA);
        StorableTypesRegistry.register(SendAccountPayloadMessage.class, SendAccountPayloadMessage.META_DATA);
        StorableTypesRegistry.register(SendAccountPayloadAndDepositTxMessage.class, SendAccountPayloadAndDepositTxMessage.META_DATA);
        StorableTypesRegistry.register(PaymentInitiatedMessage_E.class, PaymentInitiatedMessage_E.META_DATA);
        StorableTypesRegistry.register(PaymentReceivedMessage_F.class, PaymentReceivedMessage_F.META_DATA);
        StorableTypesRegistry.register(CooperativeClosureMessage_G.class, CooperativeClosureMessage_G.META_DATA);
        StorableTypesRegistry.register(MuSigCustomPayoutPsbtMessage.class, MuSigCustomPayoutPsbtMessage.META_DATA);
        StorableTypesRegistry.register(MuSigMediationResultRejectionMessage.class, MuSigMediationResultRejectionMessage.META_DATA);

        // ChatMessageReaction subclasses
        StorableTypesRegistry.register(CommonPublicChatMessageReaction.class, CommonPublicChatMessageReaction.META_DATA);
        StorableTypesRegistry.register(TwoPartyPrivateChatMessageReaction.class, TwoPartyPrivateChatMessageReaction.META_DATA);
        StorableTypesRegistry.register(BisqEasyOfferbookMessageReaction.class, BisqEasyOfferbookMessageReaction.META_DATA);
        StorableTypesRegistry.register(BisqEasyOpenTradeMessageReaction.class, BisqEasyOpenTradeMessageReaction.META_DATA);
        StorableTypesRegistry.register(MuSigOpenTradeMessageReaction.class, MuSigOpenTradeMessageReaction.META_DATA);

        // From network module. As it is used as mailbox message we add it here as well.
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
    }
}

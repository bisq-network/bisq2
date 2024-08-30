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

import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.market_price.AuthorizedMarketPriceData;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.bonded_roles.release.ReleaseNotification;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.bonded_roles.security_manager.difficulty_adjustment.AuthorizedDifficultyAdjustmentData;
import bisq.bonded_roles.security_manager.min_reputation_score.AuthorizedMinRequiredReputationScoreData;
import bisq.chat.ChatMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.reactions.*;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.proto.NetworkStorageWhiteList;
import bisq.network.p2p.message.NetworkMessageResolver;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.storage.DistributedDataResolver;
import bisq.offer.OfferMessage;
import bisq.support.mediation.MediationRequest;
import bisq.support.mediation.MediatorsResponse;
import bisq.support.moderator.ReportToModeratorMessage;
import bisq.trade.bisq_easy.protocol.messages.*;
import bisq.trade.protocol.messages.TradeMessage;
import bisq.user.banned.BannedUserProfileData;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.data.*;
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import bisq.user.reputation.requests.AuthorizeTimestampRequest;

public class ResolverConfig {
    public static void config() {
        // Register resolvers for distributedData
        // Abstract classes
        DistributedDataResolver.addResolver("chat.ChatMessage", ChatMessage.getDistributedDataResolver());
        DistributedDataResolver.addResolver("chat.ChatMessageReaction", ChatMessageReaction.getDistributedDataResolver());

        // Final classes
        DistributedDataResolver.addResolver("user.UserProfile", UserProfile.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedOracleNode", AuthorizedOracleNode.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedBondedRole", AuthorizedBondedRole.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedAlertData", AuthorizedAlertData.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedDifficultyAdjustmentData", AuthorizedDifficultyAdjustmentData.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedMinRequiredReputationScoreData", AuthorizedMinRequiredReputationScoreData.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.ReleaseNotification", ReleaseNotification.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedMarketPriceData", AuthorizedMarketPriceData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedProofOfBurnData", AuthorizedProofOfBurnData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedBondedReputationData", AuthorizedBondedReputationData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedAccountAgeData", AuthorizedAccountAgeData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedSignedWitnessData", AuthorizedSignedWitnessData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedTimestampData", AuthorizedTimestampData.getResolver());
        DistributedDataResolver.addResolver("user.BannedUserProfileData", BannedUserProfileData.getResolver());
        DistributedDataResolver.addResolver("offer.OfferMessage", OfferMessage.getResolver());

        // Register resolvers for networkMessages 
        // Abstract classes
        NetworkMessageResolver.addResolver("chat.ChatMessage", ChatMessage.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("trade.TradeMessage", TradeMessage.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("chat.ChatMessageReaction", ChatMessageReaction.getNetworkMessageResolver());

        // Final classes
        NetworkMessageResolver.addResolver("user.AuthorizeAccountAgeRequest", AuthorizeAccountAgeRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("user.AuthorizeSignedWitnessRequest", AuthorizeSignedWitnessRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("user.AuthorizeTimestampRequest", AuthorizeTimestampRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("bonded_roles.BondedRoleRegistrationRequest", BondedRoleRegistrationRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.MediationRequest", MediationRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.MediatorsResponse", MediatorsResponse.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.ReportToModeratorMessage", ReportToModeratorMessage.getNetworkMessageResolver());


        // If the classes added via `addResolver` are not final classes, we need to add manually the subclasses.
        // Otherwise, the className gets added from the `addResolver` method call.

        // ChatMessage subclasses
        NetworkStorageWhiteList.add(CommonPublicChatMessage.class);
        NetworkStorageWhiteList.add(BisqEasyOfferbookMessage.class);
        NetworkStorageWhiteList.add(TwoPartyPrivateChatMessage.class);
        NetworkStorageWhiteList.add(BisqEasyOpenTradeMessage.class);

        // TradeMessage subclasses
        NetworkStorageWhiteList.add(BisqEasyAccountDataMessage.class);
        NetworkStorageWhiteList.add(BisqEasyBtcAddressMessage.class);
        NetworkStorageWhiteList.add(BisqEasyConfirmBtcSentMessage.class);
        NetworkStorageWhiteList.add(BisqEasyConfirmFiatReceiptMessage.class);
        NetworkStorageWhiteList.add(BisqEasyConfirmFiatSentMessage.class);
        NetworkStorageWhiteList.add(BisqEasyCancelTradeMessage.class);
        NetworkStorageWhiteList.add(BisqEasyRejectTradeMessage.class);
        NetworkStorageWhiteList.add(BisqEasyReportErrorMessage.class);
        NetworkStorageWhiteList.add(BisqEasyTakeOfferRequest.class);
        NetworkStorageWhiteList.add(BisqEasyTakeOfferResponse.class);

        // ChatMessageReaction subclasses
        NetworkStorageWhiteList.add(CommonPublicChatMessageReaction.class);
        NetworkStorageWhiteList.add(BisqEasyOfferbookMessageReaction.class);
        NetworkStorageWhiteList.add(TwoPartyPrivateChatMessageReaction.class);
        NetworkStorageWhiteList.add(BisqEasyOpenTradeMessageReaction.class);

        // From network module. As it is used as mailbox message we add it here as well.
        NetworkStorageWhiteList.add(AckMessage.class);
    }
}

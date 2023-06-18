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

import bisq.chat.message.ChatMessage;
import bisq.network.p2p.message.NetworkMessageResolver;
import bisq.network.p2p.services.data.storage.DistributedDataResolver;
import bisq.offer.OfferMessage;
import bisq.oracle.daobridge.model.*;
import bisq.oracle.timestamp.AuthorizeTimestampRequest;
import bisq.oracle.timestamp.AuthorizedTimestampData;
import bisq.support.MediationRequest;
import bisq.support.MediationResponse;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTradeMessage;
import bisq.user.profile.UserProfile;
import bisq.user.role.AuthorizedRoleRegistrationData;

public class ResolverConfig {
    public static void config() {
        // Register resolvers for distributedData 
        DistributedDataResolver.addResolver("chat.ChatMessage", ChatMessage.getDistributedDataResolver());
        DistributedDataResolver.addResolver("user.UserProfile", UserProfile.getResolver());
        DistributedDataResolver.addResolver("offer.OfferMessage", OfferMessage.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedRoleRegistrationData", AuthorizedRoleRegistrationData.getResolver());
        DistributedDataResolver.addResolver("oracle.AuthorizedDaoBridgeServiceProvider", AuthorizedDaoBridgeServiceProvider.getResolver());
        DistributedDataResolver.addResolver("oracle.AuthorizedProofOfBurnData", AuthorizedProofOfBurnData.getResolver());
        DistributedDataResolver.addResolver("oracle.AuthorizedBondedReputationData", AuthorizedBondedReputationData.getResolver());
        DistributedDataResolver.addResolver("oracle.AuthorizedAccountAgeData", AuthorizedAccountAgeData.getResolver());
        DistributedDataResolver.addResolver("oracle.AuthorizedSignedWitnessData", AuthorizedSignedWitnessData.getResolver());
        DistributedDataResolver.addResolver("oracle.AuthorizedTimestampData", AuthorizedTimestampData.getResolver());

        // Register resolvers for networkMessages 
        NetworkMessageResolver.addResolver("chat.ChatMessage",
                ChatMessage.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("oracle.AuthorizeAccountAgeRequest",
                AuthorizeAccountAgeRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("oracle.AuthorizeSignedWitnessRequest",
                AuthorizeSignedWitnessRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("oracle.AuthorizeTimestampRequest",
                AuthorizeTimestampRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.MediationRequest",
                MediationRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.MediationResponse",
                MediationResponse.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("protocol.TradeProtocolMessage",
                BisqEasyTradeMessage.getNetworkMessageResolver());
    }
}
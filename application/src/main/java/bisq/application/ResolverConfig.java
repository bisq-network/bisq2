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
import bisq.oracle.node.AuthorizedOracleNode;
import bisq.oracle.node.bisq1_bridge.data.AuthorizedAccountAgeData;
import bisq.oracle.node.bisq1_bridge.data.AuthorizedBondedReputationData;
import bisq.oracle.node.bisq1_bridge.data.AuthorizedProofOfBurnData;
import bisq.oracle.node.bisq1_bridge.data.AuthorizedSignedWitnessData;
import bisq.oracle.node.bisq1_bridge.requests.AuthorizeAccountAgeRequest;
import bisq.oracle.node.bisq1_bridge.requests.AuthorizeSignedWitnessRequest;
import bisq.oracle.node.timestamp.AuthorizeTimestampRequest;
import bisq.oracle.node.timestamp.AuthorizedTimestampData;
import bisq.support.alert.AuthorizedAlertData;
import bisq.support.mediation.MediationRequest;
import bisq.support.mediation.MediationResponse;
import bisq.trade.protocol.messages.TradeMessage;
import bisq.user.profile.UserProfile;
import bisq.user.role.AuthorizedNodeRegistrationData;
import bisq.user.role.AuthorizedRoleRegistrationData;

public class ResolverConfig {
    public static void config() {
        // Register resolvers for distributedData 
        DistributedDataResolver.addResolver("chat.ChatMessage", ChatMessage.getDistributedDataResolver());
        DistributedDataResolver.addResolver("user.UserProfile", UserProfile.getResolver());
        DistributedDataResolver.addResolver("offer.OfferMessage", OfferMessage.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedRoleRegistrationData", AuthorizedRoleRegistrationData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedNodeRegistrationData", AuthorizedNodeRegistrationData.getResolver());
        DistributedDataResolver.addResolver("oracle.AuthorizedOracleNode", AuthorizedOracleNode.getResolver());
        DistributedDataResolver.addResolver("oracle.AuthorizedProofOfBurnData", AuthorizedProofOfBurnData.getResolver());
        DistributedDataResolver.addResolver("oracle.AuthorizedBondedReputationData", AuthorizedBondedReputationData.getResolver());
        DistributedDataResolver.addResolver("oracle.AuthorizedAccountAgeData", AuthorizedAccountAgeData.getResolver());
        DistributedDataResolver.addResolver("oracle.AuthorizedSignedWitnessData", AuthorizedSignedWitnessData.getResolver());
        DistributedDataResolver.addResolver("oracle.AuthorizedTimestampData", AuthorizedTimestampData.getResolver());
        DistributedDataResolver.addResolver("support.AuthorizedAlertData", AuthorizedAlertData.getResolver());

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
        NetworkMessageResolver.addResolver("trade.TradeMessage",
                TradeMessage.getNetworkMessageResolver());
    }
}
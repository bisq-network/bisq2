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

import bisq.bonded_roles.AuthorizedBondedRole;
import bisq.bonded_roles.AuthorizedOracleNode;
import bisq.bonded_roles.alert.AuthorizedAlertData;
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.chat.message.ChatMessage;
import bisq.network.p2p.message.NetworkMessageResolver;
import bisq.network.p2p.services.data.storage.DistributedDataResolver;
import bisq.offer.OfferMessage;
import bisq.support.mediation.MediationRequest;
import bisq.support.mediation.MediationResponse;
import bisq.trade.protocol.messages.TradeMessage;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.data.*;
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import bisq.user.reputation.requests.AuthorizeTimestampRequest;

public class ResolverConfig {
    public static void config() {
        // Register resolvers for distributedData 
        DistributedDataResolver.addResolver("user.UserProfile", UserProfile.getResolver());
        DistributedDataResolver.addResolver("chat.ChatMessage", ChatMessage.getDistributedDataResolver());
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedOracleNode", AuthorizedOracleNode.getResolver());
        DistributedDataResolver.addResolver("bonded_roles.AuthorizedBondedRole", AuthorizedBondedRole.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedProofOfBurnData", AuthorizedProofOfBurnData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedBondedReputationData", AuthorizedBondedReputationData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedAccountAgeData", AuthorizedAccountAgeData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedSignedWitnessData", AuthorizedSignedWitnessData.getResolver());
        DistributedDataResolver.addResolver("user.AuthorizedTimestampData", AuthorizedTimestampData.getResolver());
        DistributedDataResolver.addResolver("support.AuthorizedAlertData", AuthorizedAlertData.getResolver());
        DistributedDataResolver.addResolver("offer.OfferMessage", OfferMessage.getResolver());

        // Register resolvers for networkMessages 
        NetworkMessageResolver.addResolver("chat.ChatMessage", ChatMessage.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("user.AuthorizeAccountAgeRequest", AuthorizeAccountAgeRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("user.AuthorizeSignedWitnessRequest", AuthorizeSignedWitnessRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("user.AuthorizeTimestampRequest", AuthorizeTimestampRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("bonded_roles.BondedRoleRegistrationRequest", BondedRoleRegistrationRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.MediationRequest", MediationRequest.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("support.MediationResponse", MediationResponse.getNetworkMessageResolver());
        NetworkMessageResolver.addResolver("trade.TradeMessage", TradeMessage.getNetworkMessageResolver());
    }
}
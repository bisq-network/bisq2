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

package bisq.bonded_roles.registration;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.security.DigestUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BondedRoleRegistrationService implements Service {
    private final NetworkService networkService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;

    public BondedRoleRegistrationService(NetworkService networkService, AuthorizedBondedRolesService authorizedBondedRolesService) {
        this.networkService = networkService;
        this.authorizedBondedRolesService = authorizedBondedRolesService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }


    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean requestBondedRoleRegistration(String profileId,
                                                 String authorizedPublicKey,
                                                 BondedRoleType bondedRoleType,
                                                 String bondUserName,
                                                 String signatureBase64,
                                                 Optional<AddressByTransportTypeMap> addressByTransportTypeMap,
                                                 NetworkIdWithKeyPair senderNetworkIdWithKeyPair,
                                                 boolean isCancellationRequest) {
        ObservableSet<AuthorizedOracleNode> authorizedOracleNodes = authorizedBondedRolesService.getAuthorizedOracleNodes();
        if (authorizedOracleNodes.isEmpty()) {
            log.warn("authorizedOracleNodes is empty");
            return false;
        }

        String sendersProfileId = Hex.encode(DigestUtil.hash(senderNetworkIdWithKeyPair.getNetworkId().getPubKey().getPublicKey().getEncoded()));
        checkArgument(profileId.equals(sendersProfileId), "Senders pub key is not matching the profile ID");
        NetworkId networkId = senderNetworkIdWithKeyPair.getNetworkId();
        BondedRoleRegistrationRequest request = new BondedRoleRegistrationRequest(profileId,
                authorizedPublicKey,
                bondedRoleType,
                bondUserName,
                signatureBase64,
                addressByTransportTypeMap,
                networkId,
                isCancellationRequest);
        authorizedOracleNodes.forEach(oracleNode ->
        {
            NetworkId oracleNodeNetworkId = oracleNode.getNetworkId();
            log.info("Send BondedRoleRegistrationRequest to oracleNode {}.\nBondedRoleRegistrationRequest={}", oracleNodeNetworkId, request);
            networkService.confidentialSend(request, oracleNodeNetworkId, senderNetworkIdWithKeyPair);
        });
        return true;
    }
}
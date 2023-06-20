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

package bisq.trade;

import bisq.contract.ContractService;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.offer.OfferService;
import bisq.support.MediationService;
import bisq.support.SupportService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ServiceProvider {
    private final IdentityService identityService;
    private final OfferService offerService;
    private final ContractService contractService;
    private final MediationService mediationService;
    private final NetworkService networkService;

    public ServiceProvider(NetworkService networkService,
                           IdentityService identityService,
                           OfferService offerService,
                           ContractService contractService,
                           SupportService supportService) {
        this.networkService = networkService;
        this.identityService = identityService;
        this.offerService = offerService;
        this.contractService = contractService;
        this.mediationService = supportService.getMediationService();
    }
}
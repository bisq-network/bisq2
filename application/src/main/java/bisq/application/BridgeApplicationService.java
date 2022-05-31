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

import bisq.identity.IdentityService;
import bisq.oracle.daobridge.DaoBridgeService;
import lombok.Getter;

@Getter
public class BridgeApplicationService extends NetworkApplicationService {
    private final IdentityService identityService;
    private final DaoBridgeService daoBridgeService;

    public BridgeApplicationService(String[] args) {
        super(args);

        IdentityService.Config identityServiceConfig = IdentityService.Config.from(getConfig("bisq.identityServiceConfig"));
        identityService = new IdentityService(getPersistenceService(),
                getSecurityService().getKeyPairService(),
                networkService,
                identityServiceConfig);

        daoBridgeService = new DaoBridgeService(networkService,
                identityService,
                getConfig("bisq.oracle.daoBridge"));
    }
}
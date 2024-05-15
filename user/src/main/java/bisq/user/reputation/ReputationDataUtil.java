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

package bisq.user.reputation;

import bisq.network.NetworkService;
import bisq.user.reputation.data.*;

import java.util.Optional;

public class ReputationDataUtil {
    public static void cleanupMap(NetworkService networkService) {
        networkService.getDataService().ifPresent(dataService ->
                dataService.getStorageService().cleanupMap("AuthorizedProofOfBurnData", authorizedDistributedData ->
                        authorizedDistributedData instanceof AuthorizedProofOfBurnData
                                ? Optional.of((AuthorizedProofOfBurnData) authorizedDistributedData)
                                : Optional.empty()));
        networkService.getDataService().ifPresent(dataService ->
                dataService.getStorageService().cleanupMap("AuthorizedBondedReputationData", authorizedDistributedData ->
                        authorizedDistributedData instanceof AuthorizedBondedReputationData
                                ? Optional.of((AuthorizedBondedReputationData) authorizedDistributedData)
                                : Optional.empty()));
        networkService.getDataService().ifPresent(dataService ->
                dataService.getStorageService().cleanupMap("AuthorizedAccountAgeData", authorizedDistributedData ->
                        authorizedDistributedData instanceof AuthorizedAccountAgeData
                                ? Optional.of((AuthorizedAccountAgeData) authorizedDistributedData)
                                : Optional.empty()));
        networkService.getDataService().ifPresent(dataService ->
                dataService.getStorageService().cleanupMap("AuthorizedSignedWitnessData", authorizedDistributedData ->
                        authorizedDistributedData instanceof AuthorizedSignedWitnessData
                                ? Optional.of((AuthorizedSignedWitnessData) authorizedDistributedData)
                                : Optional.empty()));
        networkService.getDataService().ifPresent(dataService ->
                dataService.getStorageService().cleanupMap("AuthorizedTimestampData", authorizedDistributedData ->
                        authorizedDistributedData instanceof AuthorizedTimestampData
                                ? Optional.of((AuthorizedTimestampData) authorizedDistributedData)
                                : Optional.empty()));
    }
}

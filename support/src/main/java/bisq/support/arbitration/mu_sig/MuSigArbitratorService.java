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

package bisq.support.arbitration.mu_sig;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.util.StringUtils;
import bisq.contract.ContractService;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.support.arbitration.ArbitrationCaseState;
import bisq.support.arbitration.ArbitrationPayoutDistributionType;
import bisq.support.mediation.mu_sig.MuSigMediationResultService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Service used by arbitrators.
 */
@Slf4j
public class MuSigArbitratorService extends RateLimitedPersistenceClient<MuSigArbitratorStore> implements Service, ConfidentialMessageService.Listener {
    @Getter
    private final MuSigArbitratorStore persistableStore = new MuSigArbitratorStore();
    @Getter
    private final Persistence<MuSigArbitratorStore> persistence;
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final BannedUserService bannedUserService;

    public MuSigArbitratorService(PersistenceService persistenceService,
                                  NetworkService networkService,
                                  UserService userService,
                                  BondedRolesService bondedRolesService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        bannedUserService = userService.getBannedUserService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.getConfidentialMessageServices().stream()
                .flatMap(service -> service.getProcessedEnvelopePayloadMessages().stream())
                .forEach(this::onMessage);
        networkService.addConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof MuSigArbitrationRequest request) {
            processArbitrationRequest(request);
        }
    }

    public MuSigArbitrationResult createMuSigArbitrationResult(MuSigContract contract,
                                                               ArbitrationPayoutDistributionType arbitrationPayoutDistributionType,
                                                               long buyerPayoutAmount,
                                                               long sellerPayoutAmount,
                                                               Optional<String> summaryNotes) {
        return new MuSigArbitrationResult(ContractService.getContractHash(contract),
                arbitrationPayoutDistributionType,
                buyerPayoutAmount,
                sellerPayoutAmount,
                summaryNotes);
    }

    public ObservableSet<MuSigArbitrationCase> getArbitrationCases() {
        return persistableStore.getMuSigArbitrationCases();
    }

    public void closeArbitrationCase(MuSigArbitrationCase muSigArbitrationCase,
                                     MuSigArbitrationResult muSigArbitrationResult) {
        Optional<MuSigArbitrationResult> existingResult = muSigArbitrationCase.getMuSigArbitrationResult().get();
        if (existingResult.filter(result -> !result.equals(muSigArbitrationResult)).isPresent()) {
            log.warn("Ignoring changed MuSigArbitrationResult for trade {} because result cannot be changed once set.",
                    muSigArbitrationCase.getMuSigArbitrationRequest().getTradeId());
        }

        MuSigArbitrationResult resultToUse = existingResult.orElse(muSigArbitrationResult);
        boolean resultChanged = false;
        if (existingResult.isEmpty() || muSigArbitrationCase.getArbitrationResultSignature().isEmpty()) {
            byte[] arbitrationResultSignature = createArbitrationResultSignature(muSigArbitrationCase, resultToUse);
            resultChanged = muSigArbitrationCase.setSignedMuSigArbitrationResult(resultToUse, arbitrationResultSignature);
        }
        boolean stateChanged = muSigArbitrationCase.setArbitrationCaseState(ArbitrationCaseState.CLOSED);
        if (resultChanged || stateChanged) {
            persist();
            sendArbitrationCaseStateChangeMessage(muSigArbitrationCase, Optional.of(resultToUse));
        }
    }

    public void removeArbitrationCase(MuSigArbitrationCase muSigArbitrationCase) {
        getArbitrationCases().remove(muSigArbitrationCase);
        persist();
    }

    public Optional<UserIdentity> findMyArbitratorUserIdentity(NetworkId arbitratorNetworkId) {
        return findMyArbitratorUserIdentities()
                .filter(userIdentity -> userIdentity.getNetworkIdWithKeyPair().getNetworkId().equals(arbitratorNetworkId))
                .findAny();
    }

    public Stream<UserIdentity> findMyArbitratorUserIdentities() {
        // If we got banned we still want to show the admin UI.
        return authorizedBondedRolesService.getAuthorizedBondedRoleStream(true)
                .filter(data -> data.getBondedRoleType() == BondedRoleType.ARBITRATOR)
                .flatMap(data -> userIdentityService.findUserIdentity(data.getProfileId()).stream());
    }

    private void processArbitrationRequest(MuSigArbitrationRequest muSigArbitrationRequest) {
        if (bannedUserService.isUserProfileBanned(muSigArbitrationRequest.getRequester())) {
            log.warn("Ignoring MuSigArbitrationRequest as requester is banned");
            return;
        }
        if (!hasValidMediatorSignature(muSigArbitrationRequest)) {
            log.warn("Ignoring MuSigArbitrationRequest for trade {} because mediation result signature verification failed.",
                    muSigArbitrationRequest.getTradeId());
            return;
        }

        NetworkId arbitratorNetworkId = muSigArbitrationRequest.getArbitratorNetworkId();
        findMyArbitratorUserIdentity(arbitratorNetworkId).ifPresent(myUserIdentity -> {
            Optional<MuSigArbitrationCase> existingCase = findArbitrationCase(muSigArbitrationRequest.getTradeId());
            if (existingCase.filter(arbitrationCase ->
                    arbitrationCase.getArbitrationCaseState().get() == ArbitrationCaseState.CLOSED).isPresent()) {
                log.warn("Ignoring MuSigArbitrationRequest for trade {} because arbitration case is already closed.",
                        muSigArbitrationRequest.getTradeId());
                return;
            }

            MuSigArbitrationCase arbitrationCase = existingCase.orElseGet(() -> {
                MuSigArbitrationCase newCase = new MuSigArbitrationCase(muSigArbitrationRequest);
                addNewArbitrationCase(newCase);
                return newCase;
            });
            sendArbitrationCaseStateChangeMessage(arbitrationCase, Optional.empty(), myUserIdentity);
        });
    }

    private Optional<MuSigArbitrationCase> findArbitrationCase(String tradeId) {
        return getArbitrationCases().stream()
                .filter(arbitrationCase -> arbitrationCase.getMuSigArbitrationRequest().getTradeId().equals(tradeId))
                .findAny();
    }

    static boolean hasValidMediatorSignature(MuSigArbitrationRequest muSigArbitrationRequest) {
        try {
            if (muSigArbitrationRequest.getContract().getMediator().isEmpty()) {
                return false;
            }
            return MuSigMediationResultService.verifyMediationResult(
                    muSigArbitrationRequest.getMuSigMediationResult(),
                    muSigArbitrationRequest.getMediationResultSignature(),
                    muSigArbitrationRequest.getContract(),
                    muSigArbitrationRequest.getContract().getMediator().orElseThrow().getNetworkId().getPubKey().getPublicKey());
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    private void addNewArbitrationCase(MuSigArbitrationCase muSigArbitrationCase) {
        getArbitrationCases().add(muSigArbitrationCase);
        persist();
    }

    private void sendArbitrationCaseStateChangeMessage(MuSigArbitrationCase muSigArbitrationCase,
                                                       Optional<MuSigArbitrationResult> muSigArbitrationResult) {
        findMyArbitratorUserIdentity(muSigArbitrationCase.getMuSigArbitrationRequest().getArbitratorNetworkId())
                .ifPresent(myUserIdentity ->
                        sendArbitrationCaseStateChangeMessage(muSigArbitrationCase, muSigArbitrationResult, myUserIdentity));
    }

    private void sendArbitrationCaseStateChangeMessage(MuSigArbitrationCase muSigArbitrationCase,
                                                       Optional<MuSigArbitrationResult> muSigArbitrationResult,
                                                       UserIdentity myUserIdentity) {
        MuSigArbitrationRequest muSigArbitrationRequest = muSigArbitrationCase.getMuSigArbitrationRequest();
        NetworkIdWithKeyPair networkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();
        MuSigArbitrationStateChangeMessage message = new MuSigArbitrationStateChangeMessage(
                StringUtils.createUid(),
                muSigArbitrationRequest.getTradeId(),
                muSigArbitrationCase.getArbitrationCaseState().get(),
                muSigArbitrationResult,
                muSigArbitrationCase.getArbitrationResultSignature()
        );

        networkService.confidentialSend(message,
                muSigArbitrationRequest.getRequester().getNetworkId(),
                networkIdWithKeyPair);

        networkService.confidentialSend(message,
                muSigArbitrationRequest.getPeer().getNetworkId(),
                networkIdWithKeyPair);
    }

    private byte[] createArbitrationResultSignature(MuSigArbitrationCase muSigArbitrationCase,
                                                    MuSigArbitrationResult muSigArbitrationResult) {
        MuSigArbitrationRequest arbitrationRequest = muSigArbitrationCase.getMuSigArbitrationRequest();
        return findMyArbitratorUserIdentity(arbitrationRequest.getArbitratorNetworkId())
                .map(myUserIdentity -> {
                    try {
                        return MuSigArbitrationResultService.signArbitrationResult(
                                muSigArbitrationResult,
                                myUserIdentity.getNetworkIdWithKeyPair().getKeyPair());
                    } catch (GeneralSecurityException e) {
                        throw new IllegalStateException("Could not sign MuSigArbitrationStateChangeMessage for trade " + arbitrationRequest.getTradeId(), e);
                    }
                })
                .orElseThrow(() -> new IllegalStateException("Could not sign MuSigArbitrationStateChangeMessage because arbitrator identity was not found."));
    }
}

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

package bisq.support;

import bisq.chat.ChatService;
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.priv.PrivateTradeChannelService;
import bisq.common.application.Service;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.role.AuthorizedRoleRegistrationData;
import bisq.user.role.RoleRegistrationService;
import bisq.user.role.RoleType;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class MediationService implements Service, DataService.Listener, MessageListener {
    private final NetworkService networkService;
    private final Set<AuthorizedRoleRegistrationData> mediators = new CopyOnWriteArraySet<>();
    private final UserIdentityService userIdentityService;
    private final RoleRegistrationService roleRegistrationService;
    private final PrivateTradeChannelService privateTradeChannelService;
    private final TradeChannelSelectionService tradeChannelSelectionService;

    public MediationService(NetworkService networkService,
                            ChatService chatService,
                            UserService userService) {
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        roleRegistrationService = userService.getRoleRegistrationService();
        privateTradeChannelService = chatService.getPrivateTradeChannelService();
        tradeChannelSelectionService = chatService.getTradeChannelSelectionService();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.addMessageListener(this);
        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(service -> service.getAllAuthenticatedPayload().forEach(this::processAuthenticatedData));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        processAuthenticatedData(authenticatedData);
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedRoleRegistrationData) {
            AuthorizedRoleRegistrationData data = (AuthorizedRoleRegistrationData) authenticatedData.getDistributedData();
            if (data.getRoleType() == RoleType.MEDIATOR) {
                mediators.remove(data);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof MediationRequest) {
            processMediationRequest((MediationRequest) networkMessage);
        } else if (networkMessage instanceof MediationResponse) {
            processMediationResponse((MediationResponse) networkMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void requestMediation(UserIdentity myProfile, UserProfile peer, UserProfile mediator) {
        PrivateTradeChannel channel = (PrivateTradeChannel) tradeChannelSelectionService.getSelectedChannel().get();
        MediationRequest networkMessage = new MediationRequest(new ArrayList<>(channel.getChatMessages()),
                myProfile.getUserProfile(),
                peer);
        networkService.confidentialSend(networkMessage, mediator.getNetworkId(), myProfile.getNodeIdAndKeyPair());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void processAuthenticatedData(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedRoleRegistrationData) {
            AuthorizedRoleRegistrationData data = (AuthorizedRoleRegistrationData) authenticatedData.getDistributedData();
            if (data.getRoleType() == RoleType.MEDIATOR) {
                mediators.add(data);
            }
        }
    }

    private void processMediationRequest(MediationRequest mediationRequest) {
        findMyMediatorUserIdentity().ifPresent(myMediatorUserIdentity -> {
            PrivateTradeChannel channel = privateTradeChannelService.mediatorCreatesNewChannel(
                    myMediatorUserIdentity,
                    mediationRequest.getRequester(),
                    mediationRequest.getPeer()
            );
            privateTradeChannelService.setMediationActivated(channel, true);
            mediationRequest.getChatMessages().forEach(channel::addChatMessage);
            tradeChannelSelectionService.selectChannel(channel);

            MediationResponse mediationResponseToRequester = new MediationResponse(channel.getId(), Res.get("bisqEasy.mediation.msgToRequester"));
            networkService.confidentialSend(mediationResponseToRequester, mediationRequest.getRequester().getNetworkId(), myMediatorUserIdentity.getNodeIdAndKeyPair());

            MediationResponse mediationResponseToPeer = new MediationResponse(channel.getId(), Res.get("bisqEasy.mediation.msgToPeer"));
            networkService.confidentialSend(mediationResponseToPeer, mediationRequest.getPeer().getNetworkId(), myMediatorUserIdentity.getNodeIdAndKeyPair());
        });
    }

    private void processMediationResponse(MediationResponse mediationResponse) {
        privateTradeChannelService.findChannelById(mediationResponse.getChannelId()).ifPresent(channel -> {
            // Requester had it activated at request time
            if (!channel.getInMediation().get()) {
                // Peer who has not requested sends their messages as well, so mediator can be sure to get all messages
                privateTradeChannelService.setMediationActivated(channel, true);
                //todo send messages as well
            }
        });
    }

    private Optional<UserIdentity> findMyMediatorUserIdentity() {
        return roleRegistrationService.getMyRegistrations().stream()
                .filter(data -> data.getRoleType() == RoleType.MEDIATOR)
                .flatMap(data -> userIdentityService.findUserIdentity(data.getUserProfile().getId()).stream())
                .findAny();
    }

    public Optional<UserProfile> selectMediator(String myProfileId, String peersProfileId) {
        if (mediators.isEmpty()) {
            return Optional.empty();
        }
        String concat = myProfileId + peersProfileId;
        int index = new BigInteger(concat.getBytes(StandardCharsets.UTF_8)).mod(BigInteger.valueOf(mediators.size())).intValue();
        return Optional.of(new ArrayList<>(mediators).get(index).getUserProfile());
    }
}


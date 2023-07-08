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

package bisq.support.mediation;

import bisq.bonded_roles.registration.BondedRoleRegistrationService;
import bisq.bonded_roles.registration.BondedRoleType;
import bisq.bonded_roles.service.BondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannelService;
import bisq.common.application.Service;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class MediationService implements Service, DataService.Listener, MessageListener {
    private final NetworkService networkService;
    /*  private final Set<AuthorizedRoleRegistrationData> mediators = new CopyOnWriteArraySet<>();*/
    private final UserIdentityService userIdentityService;
    private final UserProfileService userProfileService;
    private final BondedRoleRegistrationService bondedRoleRegistrationService;
    private final BisqEasyPrivateTradeChatChannelService bisqEasyPrivateTradeChatChannelService;

    public MediationService(NetworkService networkService,
                            ChatService chatService,
                            UserService userService, BondedRolesService bondedRolesService) {
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        userProfileService = userService.getUserProfileService();
        bondedRoleRegistrationService = bondedRolesService.getBondedRoleRegistrationService();
        bisqEasyPrivateTradeChatChannelService = chatService.getBisqEasyPrivateTradeChatChannelService();
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
        networkService.removeMessageListener(this);
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
       /* if (authenticatedData.getDistributedData() instanceof AuthorizedRoleRegistrationData) {
            AuthorizedRoleRegistrationData data = (AuthorizedRoleRegistrationData) authenticatedData.getDistributedData();
            if (data.getRoleType() == RoleType.MEDIATOR) {
                mediators.remove(data);
            }
        }*/
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

    public void requestMediation(BisqEasyPrivateTradeChatChannel privateTradeChannel) {
        BisqEasyOffer bisqEasyOffer = privateTradeChannel.getBisqEasyOffer();
        UserIdentity myUserIdentity = privateTradeChannel.getMyUserIdentity();
        UserProfile peer = privateTradeChannel.getPeer();
        UserProfile mediator = privateTradeChannel.getMediator().orElseThrow();
        MediationRequest networkMessage = new MediationRequest(bisqEasyOffer,
                myUserIdentity.getUserProfile(),
                peer,
                new ArrayList<>(privateTradeChannel.getChatMessages()));
        networkService.confidentialSend(networkMessage, mediator.getNetworkId(), myUserIdentity.getNodeIdAndKeyPair());
    }

    public Optional<UserProfile> selectMediator(String makersUserProfileId, String takersUserProfileId) {
        return null; //todo
        /// return selectMediator(mediators, makersUserProfileId, takersUserProfileId);
    }

    // This method can be used for verification when taker provides mediators list.
    // If mediator list was not matching the expected one present in the network it might have been a manipulation attempt.
  /*  public Optional<UserProfile> selectMediator(Set<AuthorizedRoleRegistrationData> mediators, String makersProfileId, String takersProfileId) {
        if (mediators.isEmpty()) {
            return Optional.empty();
        } else if (mediators.size() == 1 && mediators.iterator().hasNext()) {
            return Optional.of(mediators.iterator().next().getUserProfile());
        } else {
            String concat = makersProfileId + takersProfileId;
            int index = new BigInteger(concat.getBytes(StandardCharsets.UTF_8)).mod(BigInteger.valueOf(mediators.size())).intValue();
            return Optional.of(new ArrayList<>(mediators).get(index).getUserProfile());
        }
    }*/

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void processAuthenticatedData(AuthenticatedData authenticatedData) {
       /* if (authenticatedData.getDistributedData() instanceof AuthorizedRoleRegistrationData) {
            AuthorizedRoleRegistrationData data = (AuthorizedRoleRegistrationData) authenticatedData.getDistributedData();
            if (data.getRoleType() == RoleType.MEDIATOR) {
                mediators.add(data);
            }
        }*/
    }

    private void processMediationRequest(MediationRequest mediationRequest) {
        findMyMediatorUserIdentity().ifPresent(myUserIdentity -> {
            BisqEasyOffer bisqEasyOffer = mediationRequest.getBisqEasyOffer();
            BisqEasyPrivateTradeChatChannel channel = bisqEasyPrivateTradeChatChannelService.mediatorFindOrCreatesChannel(
                    bisqEasyOffer,
                    myUserIdentity,
                    mediationRequest.getRequester(),
                    mediationRequest.getPeer()
            );

            bisqEasyPrivateTradeChatChannelService.setIsInMediation(channel, true);

            mediationRequest.getChatMessages().forEach(chatMessage -> bisqEasyPrivateTradeChatChannelService.addMessage(chatMessage, channel));

            NetworkIdWithKeyPair myNodeIdAndKeyPair = myUserIdentity.getNodeIdAndKeyPair();
            NetworkId receiverNetworkId = mediationRequest.getRequester().getNetworkId();
            networkService.confidentialSend(new MediationResponse(bisqEasyOffer),
                    receiverNetworkId,
                    myNodeIdAndKeyPair);
            bisqEasyPrivateTradeChatChannelService.addMediatorsResponseMessage(channel, Res.get("bisqEasy.mediation.message.toRequester"));

            receiverNetworkId = mediationRequest.getPeer().getNetworkId();
            networkService.confidentialSend(new MediationResponse(bisqEasyOffer),
                    receiverNetworkId,
                    myNodeIdAndKeyPair);
            bisqEasyPrivateTradeChatChannelService.addMediatorsResponseMessage(channel, Res.get("bisqEasy.mediation.message.toNonRequester"));
        });
    }

    private void processMediationResponse(MediationResponse mediationResponse) {
        bisqEasyPrivateTradeChatChannelService.findChannel(mediationResponse.getBisqEasyOffer())
                .ifPresent(channel -> {
                    // Requester had it activated at request time
                    if (channel.isInMediation()) {
                        bisqEasyPrivateTradeChatChannelService.addMediatorsResponseMessage(channel, Res.get("bisqEasy.mediation.message.toRequester"));
                    } else {
                        bisqEasyPrivateTradeChatChannelService.setIsInMediation(channel, true);
                        bisqEasyPrivateTradeChatChannelService.addMediatorsResponseMessage(channel, Res.get("bisqEasy.mediation.message.toNonRequester"));

                        //todo
                        // Peer who has not requested sends their messages as well, so mediator can be sure to get all messages
                    }
                });
    }

    private Optional<UserIdentity> findMyMediatorUserIdentity() {
        return bondedRoleRegistrationService.getAuthorizedBondedRoleDataSet().stream()
                .filter(data -> userIdentityService.findUserIdentity(data.getProfileId()).isPresent())
                .filter(data -> data.getBondedRoleType() == BondedRoleType.MEDIATOR)
                .flatMap(data -> userIdentityService.findUserIdentity(data.getProfileId()).stream())
                .findAny();
    }
}


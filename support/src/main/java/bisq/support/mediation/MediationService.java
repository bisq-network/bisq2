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

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.application.Service;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.identity.TorIdentity;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.network.p2p.vo.NetworkIdWithKeyPair;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.security.DigestUtil;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import com.google.common.primitives.Ints;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class MediationService implements Service, MessageListener {
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;
    private final UserProfileService userProfileService;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final BannedUserService bannedUserService;

    public MediationService(NetworkService networkService,
                            ChatService chatService,
                            UserService userService,
                            BondedRolesService bondedRolesService) {
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        userProfileService = userService.getUserProfileService();
        bannedUserService = userService.getBannedUserService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.addMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof MediationRequest) {
            processMediationRequest((MediationRequest) envelopePayloadMessage);
        } else if (envelopePayloadMessage instanceof MediationResponse) {
            processMediationResponse((MediationResponse) envelopePayloadMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void requestMediation(BisqEasyOpenTradeChannel bisqEasyOpenTradeChannel) {
        BisqEasyOffer bisqEasyOffer = bisqEasyOpenTradeChannel.getBisqEasyOffer();
        UserIdentity myUserIdentity = bisqEasyOpenTradeChannel.getMyUserIdentity();
        checkArgument(!bannedUserService.isUserProfileBanned(myUserIdentity.getUserProfile()));

        UserProfile peer = bisqEasyOpenTradeChannel.getPeer();
        UserProfile mediator = bisqEasyOpenTradeChannel.getMediator().orElseThrow();
        MediationRequest networkMessage = new MediationRequest(bisqEasyOpenTradeChannel.getTradeId(),
                bisqEasyOffer,
                myUserIdentity.getUserProfile(),
                peer,
                new ArrayList<>(bisqEasyOpenTradeChannel.getChatMessages()));
        networkService.confidentialSend(networkMessage, mediator.getNetworkId(), myUserIdentity.getNodeIdAndKeyPair(), myUserIdentity.getIdentity().getTorIdentity());
    }

    public Optional<UserProfile> selectMediator(String makersUserProfileId, String takersUserProfileId) {
        Set<AuthorizedBondedRole> mediators = authorizedBondedRolesService.getAuthorizedBondedRoleStream()
                .filter(role -> role.getBondedRoleType() == BondedRoleType.MEDIATOR)
                .collect(Collectors.toSet());
        return selectMediator(mediators, makersUserProfileId, takersUserProfileId);
    }

    // This method can be used for verification when taker provides mediators list.
    // If mediator list was not matching the expected one present in the network it might have been a manipulation attempt.
    public Optional<UserProfile> selectMediator(Set<AuthorizedBondedRole> mediators, String makersProfileId, String takersProfileId) {
        if (mediators.isEmpty()) {
            return Optional.empty();
        }
        int index;
        if (mediators.size() == 1) {
            index = 0;
        } else {
            String combined = makersProfileId + takersProfileId;
            int space = Ints.fromByteArray(DigestUtil.hash(combined.getBytes(StandardCharsets.UTF_8)));
            index = space % mediators.size();
        }
        ArrayList<AuthorizedBondedRole> list = new ArrayList<>(mediators);
        list.sort(Comparator.comparing(AuthorizedBondedRole::getProfileId));
        return userProfileService.findUserProfile(list.get(index).getProfileId());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void processMediationRequest(MediationRequest mediationRequest) {
        if (bannedUserService.isUserProfileBanned(mediationRequest.getRequester())) {
            log.warn("Message ignored as sender is banned");
            return;
        }

        String tradeId = mediationRequest.getTradeId();
        findMyMediatorUserIdentity().ifPresent(myUserIdentity -> {
            BisqEasyOffer bisqEasyOffer = mediationRequest.getBisqEasyOffer();
            BisqEasyOpenTradeChannel channel = bisqEasyOpenTradeChannelService.mediatorFindOrCreatesChannel(
                    tradeId,
                    bisqEasyOffer,
                    myUserIdentity,
                    mediationRequest.getRequester(),
                    mediationRequest.getPeer()
            );

            bisqEasyOpenTradeChannelService.setIsInMediation(channel, true);

            mediationRequest.getChatMessages().forEach(chatMessage -> bisqEasyOpenTradeChannelService.addMessage(chatMessage, channel));

            NetworkIdWithKeyPair networkIdWithKeyPair = myUserIdentity.getNodeIdAndKeyPair();
            TorIdentity myNodeTorIdentity = myUserIdentity.getIdentity().getTorIdentity();
            NetworkId receiverNetworkId = mediationRequest.getRequester().getNetworkId();
            networkService.confidentialSend(new MediationResponse(tradeId, bisqEasyOffer),
                    receiverNetworkId,
                    networkIdWithKeyPair,
                    myNodeTorIdentity);
            bisqEasyOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.get("bisqEasy.mediation.message.toRequester"));

            receiverNetworkId = mediationRequest.getPeer().getNetworkId();
            networkService.confidentialSend(new MediationResponse(tradeId, bisqEasyOffer),
                    receiverNetworkId,
                    networkIdWithKeyPair,
                    myNodeTorIdentity);
            bisqEasyOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.get("bisqEasy.mediation.message.toNonRequester"));
        });
    }

    private void processMediationResponse(MediationResponse mediationResponse) {
        bisqEasyOpenTradeChannelService.findChannel(mediationResponse.getTradeId())
                .ifPresent(channel -> {
                    // Requester had it activated at request time
                    if (channel.isInMediation()) {
                        bisqEasyOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.get("bisqEasy.mediation.message.toRequester"));
                    } else {
                        bisqEasyOpenTradeChannelService.setIsInMediation(channel, true);
                        bisqEasyOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.get("bisqEasy.mediation.message.toNonRequester"));

                        //todo
                        // Peer who has not requested sends their messages as well, so mediator can be sure to get all messages
                    }
                });
    }

    private Optional<UserIdentity> findMyMediatorUserIdentity() {
        return authorizedBondedRolesService.getAuthorizedBondedRoleStream()
                .filter(data -> data.getBondedRoleType() == BondedRoleType.MEDIATOR)
                .flatMap(data -> userIdentityService.findUserIdentity(data.getProfileId()).stream())
                .findAny();
    }
}


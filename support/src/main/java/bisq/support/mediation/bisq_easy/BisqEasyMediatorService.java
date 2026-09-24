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

package bisq.support.mediation.bisq_easy;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatMessageType;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.support.mediation.bisq_easy.BisqEasyMediationContractIdentityChecks.hasMatchingContractMediator;
import static bisq.support.mediation.bisq_easy.BisqEasyMediationContractIdentityChecks.hasMatchingContractParties;
import static bisq.support.mediation.bisq_easy.BisqEasyMediationContractIdentityChecks.isMediatorAParty;

/**
 * Service used by mediators
 */
@Slf4j
public class BisqEasyMediatorService extends RateLimitedPersistenceClient<MediatorStore> implements Service, ConfidentialMessageService.Listener {
    static final int MAX_OPEN_CASES_PER_REQUESTER = 10;
    static final Set<ChatMessageType> FORWARDABLE_MESSAGE_TYPES = EnumSet.of(ChatMessageType.TEXT,
            ChatMessageType.LEAVE,
            ChatMessageType.TAKE_BISQ_EASY_OFFER,
            ChatMessageType.PROTOCOL_LOG_MESSAGE);

    @Getter
    private final MediatorStore persistableStore = new MediatorStore();
    @Getter
    private final Persistence<MediatorStore> persistence;
    private final NetworkService networkService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final BannedUserService bannedUserService;
    private final UnverifiedChatMessageKeysByTrade unverifiedChatMessageKeysByTrade;

    public BisqEasyMediatorService(PersistenceService persistenceService,
                                   NetworkService networkService,
                                   ChatService chatService,
                                   UserService userService,
                                   BondedRolesService bondedRolesService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        bannedUserService = userService.getBannedUserService();
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
        unverifiedChatMessageKeysByTrade = new UnverifiedChatMessageKeysByTrade(getMediationCases());
    }

    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.subscribeToConfidentialMessages(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeConfidentialMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    /* --------------------------------------------------------------------- */
    // ConfidentialMessageService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof BisqEasyMediationRequest) {
            processMediationRequest((BisqEasyMediationRequest) envelopePayloadMessage);
        }
    }

    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public void closeMediationCase(BisqEasyMediationCase bisqEasyMediationCase) {
        if (bisqEasyMediationCase.setClosed(true)) {
            persist();
        }
    }

    public void removeMediationCase(BisqEasyMediationCase bisqEasyMediationCase) {
        getMediationCases().remove(bisqEasyMediationCase);
        persist();
    }

    public void reOpenMediationCase(BisqEasyMediationCase bisqEasyMediationCase) {
        if (bisqEasyMediationCase.setClosed(false)) {
            persist();
        }
    }

    public ObservableSet<BisqEasyMediationCase> getMediationCases() {
        return persistableStore.getBisqEasyMediationCases();
    }

    public Optional<UserIdentity> findMyMediatorUserIdentity(Optional<UserProfile> mediator) {
        return findMyMediatorUserIdentities()
                .filter(e -> mediator.isPresent())
                .filter(userIdentity -> userIdentity.getUserProfile().getId().equals(mediator.orElseThrow().getId()))
                .findAny();
    }

    public Stream<UserIdentity> findMyMediatorUserIdentities() {
        // If we got banned we still want to show the admin UI
        return authorizedBondedRolesService.getAuthorizedBondedRoleStream(true)
                .filter(data -> data.getBondedRoleType() == BondedRoleType.MEDIATOR)
                .flatMap(data -> userIdentityService.findUserIdentity(data.getProfileId()).stream());
    }

    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private synchronized void processMediationRequest(BisqEasyMediationRequest bisqEasyMediationRequest) {
        String tradeId = bisqEasyMediationRequest.getTradeId();
        Optional<BisqEasyOpenTradeChannel> existingChannel = bisqEasyOpenTradeChannelService.findChannelByTradeId(tradeId);
        Optional<UserProfile> optionalRequester = verifyMediationRequest(bisqEasyMediationRequest,
                bannedUserService,
                getMediationCases(),
                existingChannel);
        if (optionalRequester.isEmpty()) {
            return;
        }

        UserProfile requester = optionalRequester.orElseThrow();
        BisqEasyContract contract = bisqEasyMediationRequest.getContract();
        findMyMediatorUserIdentity(contract.getMediator()).ifPresent(myUserIdentity -> {
            UserProfile peer = bisqEasyMediationRequest.getPeer();
            BisqEasyOpenTradeChannel channel = bisqEasyOpenTradeChannelService.mediatorFindOrCreatesChannel(
                    tradeId,
                    contract.getOffer(),
                    myUserIdentity,
                    requester,
                    peer
            );

            bisqEasyOpenTradeChannelService.setIsInMediation(channel, true);

            // We apply the bisqEasyMediationCase after the channel is set up as clients will expect a channel.
            // The other trader's request for the same contract is kept on the existing case.
            BisqEasyMediationCase bisqEasyMediationCase = new BisqEasyMediationCase(bisqEasyMediationRequest);
            getMediationCases().stream()
                    .filter(bisqEasyMediationCase::equals)
                    .findAny()
                    .ifPresentOrElse(existingCase -> retainRequest(existingCase, bisqEasyMediationRequest),
                            () -> addNewMediationCase(bisqEasyMediationCase));

            getMessagesToImport(bisqEasyMediationRequest, channel.getChatMessages())
                    .forEach(chatMessage -> bisqEasyOpenTradeChannelService.addMessage(chatMessage, channel));

            NetworkIdWithKeyPair networkIdWithKeyPair = myUserIdentity.getNetworkIdWithKeyPair();

            // Send to requester
            networkService.confidentialSend(new BisqEasyMediatorsResponse(tradeId, networkIdWithKeyPair.getNetworkId()),
                    requester.getNetworkId(),
                    networkIdWithKeyPair);
            bisqEasyOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.encode("authorizedRole.mediator.message.toRequester"));

            // Send to peer
            networkService.confidentialSend(new BisqEasyMediatorsResponse(tradeId, networkIdWithKeyPair.getNetworkId()),
                    peer.getNetworkId(),
                    networkIdWithKeyPair);
            bisqEasyOpenTradeChannelService.addMediatorsResponseMessage(channel, Res.encode("authorizedRole.mediator.message.toNonRequester"));
        });
    }

    static Optional<UserProfile> verifyMediationRequest(BisqEasyMediationRequest bisqEasyMediationRequest,
                                                        BannedUserService bannedUserService,
                                                        Collection<BisqEasyMediationCase> existingCases,
                                                        Optional<BisqEasyOpenTradeChannel> existingChannel) {
        UserProfile requester = bisqEasyMediationRequest.getRequester();
        if (bannedUserService.isUserProfileBanned(requester)) {
            log.warn("Message ignored as sender is banned");
            return Optional.empty();
        }

        BisqEasyContract contract = bisqEasyMediationRequest.getContract();
        UserProfile peer = bisqEasyMediationRequest.getPeer();
        if (!hasMatchingContractParties(contract, requester, peer)) {
            log.warn("Ignoring BisqEasyMediationRequest for trade {} because requester {} and peer {} are not the two contract parties.",
                    bisqEasyMediationRequest.getTradeId(), requester.getId(), peer.getId());
            return Optional.empty();
        }
        if (!hasMatchingContractMediator(contract.getMediator(), bisqEasyMediationRequest.getReceiver())) {
            log.warn("Ignoring BisqEasyMediationRequest for trade {} because mediator does not match contract mediator.",
                    bisqEasyMediationRequest.getTradeId());
            return Optional.empty();
        }
        if (isMediatorAParty(requester, peer, bisqEasyMediationRequest.getReceiver())) {
            log.warn("Ignoring BisqEasyMediationRequest for trade {} because it names the mediator as a trader.",
                    bisqEasyMediationRequest.getTradeId());
            return Optional.empty();
        }
        if (!hasValidChatMessages(bisqEasyMediationRequest, requester, peer)) {
            return Optional.empty();
        }
        if (!hasTradersOnRecord(bisqEasyMediationRequest, existingCases)) {
            log.warn("Ignoring BisqEasyMediationRequest for trade {} because it names other traders than the cases of that trade.",
                    bisqEasyMediationRequest.getTradeId());
            return Optional.empty();
        }
        if (!isWithinCaseLimits(bisqEasyMediationRequest, existingCases)) {
            return Optional.empty();
        }
        if (existingChannel.isPresent() && !hasMatchingTraders(existingChannel.get(), requester, peer)) {
            log.warn("Ignoring BisqEasyMediationRequest for trade {} because the existing channel has different traders.",
                    bisqEasyMediationRequest.getTradeId());
            return Optional.empty();
        }
        return Optional.of(requester);
    }

    // The traders of a trade are the ones of its first accepted request. The cases are the mediator's record of a
    // trade. The channel is not, as leaving removes it and the mediator view creates it again.
    static boolean hasTradersOnRecord(BisqEasyMediationRequest bisqEasyMediationRequest,
                                      Collection<BisqEasyMediationCase> existingCases) {
        Set<String> traderIds = getTraderIds(bisqEasyMediationRequest);
        return getRequestsForTrade(bisqEasyMediationRequest.getTradeId(), existingCases).stream()
                .allMatch(request -> getTraderIds(request).equals(traderIds));
    }

    // One request per requester and trade. The peer of a known trade may send its own request; it does not take a
    // new open case slot.
    static boolean isWithinCaseLimits(BisqEasyMediationRequest bisqEasyMediationRequest,
                                      Collection<BisqEasyMediationCase> existingCases) {
        String tradeId = bisqEasyMediationRequest.getTradeId();
        String requesterId = bisqEasyMediationRequest.getRequester().getId();
        List<BisqEasyMediationRequest> requestsForTrade = getRequestsForTrade(tradeId, existingCases);
        boolean alreadyRequested = requestsForTrade.stream()
                .anyMatch(request -> request.getRequester().getId().equals(requesterId));
        if (alreadyRequested) {
            log.warn("Ignoring BisqEasyMediationRequest for trade {} because requester {} already requested mediation for it.",
                    tradeId, requesterId);
            return false;
        }
        // Open cases of other trades the requester is on record for, as requester or as the other trader.
        long numOpenCases = existingCases.stream()
                .filter(mediationCase -> !mediationCase.isClosed())
                .filter(mediationCase -> !mediationCase.getBisqEasyMediationRequest().getTradeId().equals(tradeId))
                .filter(mediationCase -> mediationCase.getMediationRequests().stream()
                        .anyMatch(request -> request.getRequester().getId().equals(requesterId)))
                .count();
        if (numOpenCases >= MAX_OPEN_CASES_PER_REQUESTER) {
            log.warn("Ignoring BisqEasyMediationRequest for trade {} because requester {} has {} open cases.",
                    tradeId, requesterId, numOpenCases);
            return false;
        }
        return true;
    }

    // The channel's offer is a display copy from the first accepted request of the trade and is not compared, so
    // that a request with a differing offer opens its own case.
    static boolean hasMatchingTraders(BisqEasyOpenTradeChannel channel,
                                      UserProfile requester,
                                      UserProfile peer) {
        Set<String> traderIds = channel.getTraders().stream()
                .map(UserProfile::getId)
                .collect(Collectors.toSet());
        Set<String> requestTraderIds = Stream.of(requester.getId(), peer.getId())
                .collect(Collectors.toSet());
        return traderIds.equals(requestTraderIds);
    }

    private static Set<String> getTraderIds(BisqEasyMediationRequest request) {
        return Stream.of(request.getRequester().getId(), request.getPeer().getId())
                .collect(Collectors.toSet());
    }

    static List<BisqEasyMediationRequest> getRequestsForTrade(String tradeId,
                                                              Collection<BisqEasyMediationCase> existingCases) {
        return existingCases.stream()
                .filter(mediationCase -> mediationCase.getBisqEasyMediationRequest().getTradeId().equals(tradeId))
                .flatMap(mediationCase -> mediationCase.getMediationRequests().stream())
                .collect(Collectors.toList());
    }

    // A message is unverified when it reached the mediator only inside a request of somebody else than its
    // author. Provenance is bound to the exact message content and author, not to the message ID, so a
    // second message under the same ID cannot clear the mark. The mediator UI marks such messages.
    // Each key is a hash over a message, so the result is kept per trade until the trade's retained requests change.
    public Set<String> getUnverifiedChatMessageKeys(String tradeId) {
        return unverifiedChatMessageKeysByTrade.get(tradeId);
    }

    static Set<String> getUnverifiedChatMessageKeys(String tradeId, Collection<BisqEasyMediationCase> existingCases) {
        return getUnverifiedChatMessageKeys(getRequestsForTrade(tradeId, existingCases));
    }

    static Set<String> getUnverifiedChatMessageKeys(List<BisqEasyMediationRequest> requestsForTrade) {
        Set<String> ownMessageKeys = requestsForTrade.stream()
                .flatMap(request -> request.getChatMessages().stream()
                        .filter(message -> isOwnMessage(message, request)))
                .map(BisqEasyMediatorService::getProvenanceKey)
                .collect(Collectors.toSet());
        return requestsForTrade.stream()
                .flatMap(request -> request.getChatMessages().stream()
                        .filter(message -> !isOwnMessage(message, request)))
                .map(BisqEasyMediatorService::getProvenanceKey)
                .filter(key -> !ownMessageKeys.contains(key))
                .collect(Collectors.toSet());
    }

    // Import by exact message, so a trader's own version of a message is kept next to a differing copy the other
    // trader forwarded under the same ID. Reactions are not imported: nothing shows who added a reaction in a
    // transcript, so it could be attributed to the other trader without a mark. The traders' live reactions are not
    // sent to the mediator.
    static List<BisqEasyOpenTradeMessage> getMessagesToImport(BisqEasyMediationRequest bisqEasyMediationRequest,
                                                              Collection<BisqEasyOpenTradeMessage> channelMessages) {
        Set<String> existingMessageKeys = channelMessages.stream()
                .map(BisqEasyMediatorService::getProvenanceKey)
                .collect(Collectors.toSet());
        return bisqEasyMediationRequest.getChatMessages().stream()
                .map(BisqEasyMediatorService::withoutReactions)
                .filter(chatMessage -> !existingMessageKeys.contains(getProvenanceKey(chatMessage)))
                .toList();
    }

    // Hash of the message without reactions. Reactions get added later in the channel and must not change the key.
    public static String getProvenanceKey(BisqEasyOpenTradeMessage message) {
        return Hex.encode(DigestUtil.hash(withoutReactions(message).serializeForHash()));
    }

    private static BisqEasyOpenTradeMessage withoutReactions(BisqEasyOpenTradeMessage message) {
        return new BisqEasyOpenTradeMessage(message.getTradeId(),
                message.getId(),
                message.getChannelId(),
                message.getSenderUserProfile(),
                message.getReceiverUserProfileId(),
                message.getReceiverNetworkId(),
                message.getText().orElse(null),
                message.getCitation(),
                message.getDate(),
                message.isWasEdited(),
                message.getMediator(),
                message.getChatMessageType(),
                message.getBisqEasyOffer(),
                new HashSet<>());
    }

    private static boolean isOwnMessage(BisqEasyOpenTradeMessage message, BisqEasyMediationRequest request) {
        return message.getSenderUserProfile().getId().equals(request.getRequester().getId());
    }

    private static boolean hasValidChatMessages(BisqEasyMediationRequest bisqEasyMediationRequest,
                                                UserProfile requester,
                                                UserProfile peer) {
        String tradeId = bisqEasyMediationRequest.getTradeId();
        String channelId = BisqEasyOpenTradeChannel.createId(tradeId);
        // Only message types a trader's channel holds. Chat rules warnings and expired messages indicators are
        // local UI notices, their renderers show no provenance.
        Optional<BisqEasyOpenTradeMessage> unexpectedType = bisqEasyMediationRequest.getChatMessages().stream()
                .filter(chatMessage -> !FORWARDABLE_MESSAGE_TYPES.contains(chatMessage.getChatMessageType()))
                .findAny();
        if (unexpectedType.isPresent()) {
            log.warn("Ignoring BisqEasyMediationRequest for trade {} because embedded chat message {} has type {}.",
                    tradeId, unexpectedType.get().getId(), unexpectedType.get().getChatMessageType());
            return false;
        }
        long numDistinctIds = bisqEasyMediationRequest.getChatMessages().stream()
                .map(BisqEasyOpenTradeMessage::getId)
                .distinct()
                .count();
        if (numDistinctIds != bisqEasyMediationRequest.getChatMessages().size()) {
            log.warn("Ignoring BisqEasyMediationRequest for trade {} because embedded chat messages share an ID.", tradeId);
            return false;
        }
        return bisqEasyMediationRequest.getChatMessages().stream()
                .allMatch(chatMessage -> {
                    if (!chatMessage.getTradeId().equals(tradeId)) {
                        log.warn("Ignoring BisqEasyMediationRequest for trade {} because embedded chat message {} has trade ID {}.",
                                tradeId, chatMessage.getId(), chatMessage.getTradeId());
                        return false;
                    }
                    if (!chatMessage.getChannelId().equals(channelId)) {
                        log.warn("Ignoring BisqEasyMediationRequest for trade {} because embedded chat message {} has channel ID {}.",
                                tradeId, chatMessage.getId(), chatMessage.getChannelId());
                        return false;
                    }
                    String senderUserProfileId = chatMessage.getSenderUserProfile().getId();
                    if (!senderUserProfileId.equals(requester.getId()) && !senderUserProfileId.equals(peer.getId())) {
                        log.warn("Ignoring BisqEasyMediationRequest for trade {} because embedded chat message {} has unexpected sender {}.",
                                tradeId, chatMessage.getId(), senderUserProfileId);
                        return false;
                    }
                    return true;
                });
    }

    // Every accepted request is retained before its messages get imported, so that the provenance of each
    // message in the channel is on record. A known case is reached only by the other trader's first request.
    private void retainRequest(BisqEasyMediationCase existingCase, BisqEasyMediationRequest request) {
        if (existingCase.setPeersMediationRequest(request)) {
            persist();
        }
    }

    private void addNewMediationCase(BisqEasyMediationCase bisqEasyMediationCase) {
        getMediationCases().add(bisqEasyMediationCase);
        persist();
    }
}

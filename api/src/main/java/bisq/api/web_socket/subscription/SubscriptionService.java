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

package bisq.api.web_socket.subscription;


import bisq.api.access.permissions.Permission;
import bisq.api.access.permissions.PermissionService;
import bisq.api.chat.common.PublicChatChannels;
import bisq.api.web_socket.domain.BaseWebSocketService;
import bisq.api.web_socket.domain.OpenTradeItemsService;
import bisq.api.web_socket.domain.alert_notifications.AlertNotificationsWebSocketService;
import bisq.api.web_socket.domain.chat.private_chat.PrivateChatChannelsWebSocketService;
import bisq.api.web_socket.domain.chat.private_chat.PrivateChatMessagesWebSocketService;
import bisq.api.web_socket.domain.chat.private_chat.PrivateChatReactionsWebSocketService;
import bisq.api.web_socket.domain.chat.public_chat.PublicChatChannelsWebSocketService;
import bisq.api.web_socket.domain.chat.public_chat.PublicChatMessagesWebSocketService;
import bisq.api.web_socket.domain.chat.public_chat.PublicChatReactionsWebSocketService;
import bisq.api.web_socket.domain.chat.reactions.ChatReactionsWebSocketService;
import bisq.api.web_socket.domain.chat.trade.TradeChatMessagesWebSocketService;
import bisq.api.web_socket.domain.contacts.ContactsWebSocketService;
import bisq.api.web_socket.domain.market_price.MarketPriceWebSocketService;
import bisq.api.web_socket.domain.network.NetworkInfoWebSocketService;
import bisq.api.web_socket.domain.offers.NumOffersWebSocketService;
import bisq.api.web_socket.domain.offers.OffersWebSocketService;
import bisq.api.web_socket.domain.reputation.ReputationWebSocketService;
import bisq.api.web_socket.domain.trade_restricting_alert.TradeRestrictingAlertWebSocketService;
import bisq.api.web_socket.domain.trades.TradePropertiesWebSocketService;
import bisq.api.web_socket.domain.trades.TradesWebSocketService;
import bisq.api.web_socket.domain.user_profile.NumUserProfilesWebSocketService;
import bisq.api.web_socket.util.JsonUtil;
import bisq.api.web_socket.util.WebSocketIdentity;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.websockets.WebSocket;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SubscriptionService implements Service {
    private final SubscriberRepository subscriberRepository;
    private final MarketPriceWebSocketService marketPriceWebSocketService;
    private final NumOffersWebSocketService numOffersWebSocketService;
    private final OffersWebSocketService offersWebSocketService;
    private final TradesWebSocketService tradesWebSocketService;
    private final TradePropertiesWebSocketService tradePropertiesWebSocketService;
    private final TradeChatMessagesWebSocketService tradeChatMessagesWebSocketService;
    private final ChatReactionsWebSocketService chatReactionsWebSocketService;
    private final ReputationWebSocketService reputationWebSocketService;
    private final NumUserProfilesWebSocketService numUserProfilesWebSocketService;
    private final TradeRestrictingAlertWebSocketService tradeRestrictingAlertWebSocketService;
    private final AlertNotificationsWebSocketService alertNotificationsWebSocketService;
    private final NetworkInfoWebSocketService networkInfoWebSocketService;
    private final PrivateChatChannelsWebSocketService privateChatChannelsWebSocketService;
    private final PrivateChatMessagesWebSocketService privateChatMessagesWebSocketService;
    private final PrivateChatReactionsWebSocketService privateChatReactionsWebSocketService;
    private final ContactsWebSocketService contactsWebSocketService;
    private final PublicChatChannelsWebSocketService publicChatChannelsWebSocketService;
    private final PublicChatMessagesWebSocketService publicChatMessagesWebSocketService;
    private final PublicChatReactionsWebSocketService publicChatReactionsWebSocketService;
    private final PermissionService permissionService;
    // Built here rather than injected, like RestApiAuthorizationFilter builds its own: which
    // permission a topic requires is this service's question and nobody else's.
    private final SubscriptionPermissionMapping permissionMapping = new SubscriptionPermissionMapping();
    // The same switch that decides whether RestApiBaseResourceConfig registers the REST
    // authorization filter. A node configured without authorization has no permissions to check
    // against, so subscriptions must not start demanding them.
    private final boolean authorizationRequired;

    public SubscriptionService(BondedRolesService bondedRolesService,
                               AlertNotificationsService alertNotificationsService,
                               ChatService chatService,
                               TradeService tradeService,
                               UserService userService,
                               BisqEasyService bisqEasyService,
                               NetworkService networkService,
                               OpenTradeItemsService openTradeItemsService,
                               PermissionService permissionService,
                               boolean authorizationRequired) {
        this.permissionService = permissionService;
        this.authorizationRequired = authorizationRequired;
        subscriberRepository = new SubscriberRepository();

        marketPriceWebSocketService = new MarketPriceWebSocketService(subscriberRepository, bondedRolesService);
        numOffersWebSocketService = new NumOffersWebSocketService(subscriberRepository, chatService, userService, bisqEasyService);
        offersWebSocketService = new OffersWebSocketService(subscriberRepository, chatService, userService, bondedRolesService);
        tradesWebSocketService = new TradesWebSocketService(subscriberRepository, openTradeItemsService);
        tradePropertiesWebSocketService = new TradePropertiesWebSocketService(subscriberRepository, tradeService);
        tradeChatMessagesWebSocketService = new TradeChatMessagesWebSocketService(subscriberRepository,
                chatService.getBisqEasyOpenTradeChannelService(),
                userService.getUserProfileService());
        chatReactionsWebSocketService = new ChatReactionsWebSocketService(subscriberRepository,
                chatService.getBisqEasyOpenTradeChannelService());
        reputationWebSocketService = new ReputationWebSocketService(subscriberRepository, userService.getReputationService());
        numUserProfilesWebSocketService = new NumUserProfilesWebSocketService(subscriberRepository, userService);
        tradeRestrictingAlertWebSocketService = new TradeRestrictingAlertWebSocketService(subscriberRepository, bondedRolesService.getAlertService());
        alertNotificationsWebSocketService = new AlertNotificationsWebSocketService(subscriberRepository, alertNotificationsService);
        networkInfoWebSocketService = new NetworkInfoWebSocketService(subscriberRepository,
                networkService,
                bondedRolesService.getAuthorizedBondedRolesService());
        privateChatChannelsWebSocketService = new PrivateChatChannelsWebSocketService(subscriberRepository,
                chatService.getTwoPartyPrivateChatChannelService(),
                chatService.getChatNotificationService(),
                userService.getUserProfileService());
        privateChatMessagesWebSocketService = new PrivateChatMessagesWebSocketService(subscriberRepository,
                chatService.getTwoPartyPrivateChatChannelService(),
                userService.getUserProfileService(),
                userService.getBannedUserService());
        privateChatReactionsWebSocketService = new PrivateChatReactionsWebSocketService(subscriberRepository,
                chatService.getTwoPartyPrivateChatChannelService(),
                userService.getBannedUserService());
        contactsWebSocketService = new ContactsWebSocketService(subscriberRepository, userService.getContactListService());
        PublicChatChannels publicChatChannels = new PublicChatChannels(chatService);
        publicChatChannelsWebSocketService = new PublicChatChannelsWebSocketService(subscriberRepository,
                publicChatChannels,
                chatService.getChatNotificationService());
        publicChatMessagesWebSocketService = new PublicChatMessagesWebSocketService(subscriberRepository,
                publicChatChannels,
                userService.getUserProfileService(),
                userService.getBannedUserService());
        publicChatReactionsWebSocketService = new PublicChatReactionsWebSocketService(subscriberRepository,
                publicChatChannels,
                userService.getUserProfileService(),
                userService.getBannedUserService());
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return marketPriceWebSocketService.initialize()
                .thenCompose(e -> numOffersWebSocketService.initialize())
                .thenCompose(e -> offersWebSocketService.initialize())
                .thenCompose(e -> tradesWebSocketService.initialize())
                .thenCompose(e -> tradePropertiesWebSocketService.initialize())
                .thenCompose(e -> tradeChatMessagesWebSocketService.initialize())
                .thenCompose(e -> chatReactionsWebSocketService.initialize())
                .thenCompose(e -> reputationWebSocketService.initialize())
                .thenCompose(e -> numUserProfilesWebSocketService.initialize())
                .thenCompose(e -> tradeRestrictingAlertWebSocketService.initialize())
                .thenCompose(e -> alertNotificationsWebSocketService.initialize())
                .thenCompose(e -> networkInfoWebSocketService.initialize())
                .thenCompose(e -> privateChatChannelsWebSocketService.initialize())
                .thenCompose(e -> privateChatMessagesWebSocketService.initialize())
                .thenCompose(e -> privateChatReactionsWebSocketService.initialize())
                .thenCompose(e -> contactsWebSocketService.initialize())
                .thenCompose(e -> publicChatChannelsWebSocketService.initialize())
                .thenCompose(e -> publicChatMessagesWebSocketService.initialize())
                .thenCompose(e -> publicChatReactionsWebSocketService.initialize());
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return marketPriceWebSocketService.shutdown()
                .thenCompose(e -> numOffersWebSocketService.shutdown())
                .thenCompose(e -> offersWebSocketService.shutdown())
                .thenCompose(e -> tradesWebSocketService.shutdown())
                .thenCompose(e -> tradePropertiesWebSocketService.shutdown())
                .thenCompose(e -> tradeChatMessagesWebSocketService.shutdown())
                .thenCompose(e -> chatReactionsWebSocketService.shutdown())
                .thenCompose(e -> reputationWebSocketService.shutdown())
                .thenCompose(e -> numUserProfilesWebSocketService.shutdown())
                .thenCompose(e -> tradeRestrictingAlertWebSocketService.shutdown())
                .thenCompose(e -> alertNotificationsWebSocketService.shutdown())
                .thenCompose(e -> networkInfoWebSocketService.shutdown())
                .thenCompose(e -> privateChatChannelsWebSocketService.shutdown())
                .thenCompose(e -> privateChatMessagesWebSocketService.shutdown())
                .thenCompose(e -> privateChatReactionsWebSocketService.shutdown())
                .thenCompose(e -> contactsWebSocketService.shutdown())
                .thenCompose(e -> publicChatChannelsWebSocketService.shutdown())
                .thenCompose(e -> publicChatMessagesWebSocketService.shutdown())
                .thenCompose(e -> publicChatReactionsWebSocketService.shutdown());
    }

    public void onConnectionClosed(WebSocket webSocket) {
        subscriberRepository.onConnectionClosed(webSocket);
    }

    public boolean canHandle(String json) {
        return JsonUtil.hasExpectedJsonClassName(SubscriptionRequest.class, json);
    }

    public void onMessage(String json, WebSocket webSocket) {
        SubscriptionRequest.fromJson(json)
                .ifPresent(subscriptionRequest ->
                        subscribe(subscriptionRequest, webSocket));
    }

    private void subscribe(SubscriptionRequest request, WebSocket webSocket) {
        log.info("Received subscription request: {}", request);
        Optional<String> authorizationError = findAuthorizationError(request.getTopic(), webSocket);
        if (authorizationError.isPresent()) {
            refuse(webSocket, request.getRequestId(), authorizationError.get());
            return;
        }
        findWebSocketService(request.getTopic())
                .ifPresent(webSocketService -> {
                    Subscriber subscriber = null;
                    try {
                        webSocketService.validate(request);
                        Optional<String> canonicalParameter = webSocketService.canonicalizeParameter(StringUtils.toOptional(request.getParameter()));
                        subscriber = subscriberRepository.add(request, canonicalParameter, webSocket);
                        // The check above and this add are not one operation. A revocation in between
                        // removes the grant and closes the socket, but closing only moves a Grizzly
                        // socket to CLOSING: until the close frame is flushed it still accepts sends
                        // and the repository has not been swept. Without this re-read the snapshot
                        // below would go out to a client whose grant is already gone, and the entry
                        // would stay until the sweep. The re-read sees the grant gone because the
                        // revocation removes it before it closes the socket. Deliberately not a
                        // socket-state check: isConnected() is true while CLOSING, and once the
                        // socket is CLOSED the send throws and the catch below removes the entry.
                        Optional<String> revokedMeanwhile = findAuthorizationError(request.getTopic(), webSocket);
                        if (revokedMeanwhile.isPresent()) {
                            removeSubscriber(subscriber);
                            refuse(webSocket, request.getRequestId(), revokedMeanwhile.get());
                            return;
                        }
                        Optional<String> jsonPayload = webSocketService.getJsonPayload(canonicalParameter);
                        if (jsonPayload.isPresent()) {
                            sendSubscriptionResponse(webSocket, request.getRequestId(), jsonPayload.get(), null);
                        } else {
                            removeSubscriber(subscriber);
                            sendSubscriptionResponse(webSocket,
                                    request.getRequestId(),
                                    null,
                                    String.format("Unexpected error when subscribing to %s", request.getTopic().name()));
                        }
                    } catch (IllegalArgumentException e) {
                        removeSubscriber(subscriber);
                        sendSubscriptionResponse(webSocket, request.getRequestId(), null, e.getMessage());
                    } catch (Exception e) {
                        removeSubscriber(subscriber);
                        log.error("Unexpected error subscribing {}", request, e);
                        sendSubscriptionResponse(webSocket,
                                request.getRequestId(),
                                null,
                                String.format("Unexpected error when subscribing to %s", request.getTopic().name()));
                    }
                });
    }

    /**
     * The authorization the REST surface gets from {@code RestApiAuthorizationFilter} and this one
     * never had: the same connection carries both, but only REST messages are proxied through
     * JAX-RS, so the filter never sees a subscription and every topic was served to any paired
     * client regardless of what it was granted.
     * <p>
     * Checked before the topic is routed, mirroring a filter running before the resource, and
     * failing closed at every step. The identity comes from the upgrade request rather than the
     * message, because the handshake is the only point in the connection's life where anything
     * could have vouched for it — which is not the same as saying something did; see
     * {@link WebSocketIdentity}.
     * <p>
     * The message repeats the REST vocabulary ({@code permission_not_granted}) so a client can tell
     * a withheld permission from a transport failure and prompt for re-pairing instead of showing a
     * connection error. The contract is that prefix, optionally followed by {@code ": "} and the
     * permission name, so a client has to match on the prefix and never on the whole string. The
     * name is only there when a known client was refused a specific permission — the two identity
     * failures give a bare denial, because naming what an unidentified caller would have needed
     * tells it something it has not earned.
     *
     * @return the error to answer with, or empty when the subscription may proceed
     */
    private Optional<String> findAuthorizationError(Topic topic, WebSocket webSocket) {
        if (!authorizationRequired) {
            return Optional.empty();
        }
        Optional<String> clientId = WebSocketIdentity.findClientId(webSocket);
        if (clientId.isEmpty()) {
            log.warn("Subscription authz failed: connection carries no clientId. topic={}", topic);
            return Optional.of("permission_not_granted");
        }
        Optional<Set<Permission>> granted = permissionService.findPermissions(clientId.get());
        if (granted.isEmpty()) {
            log.warn("Subscription authz failed: no permissions registered for the client. topic={}", topic);
            return Optional.of("permission_not_granted");
        }
        Permission required = permissionMapping.getRequiredPermission(topic);
        if (!permissionService.hasPermission(granted.get(), required)) {
            log.warn("Subscription authz failed: required permission {} not granted. topic={}", required.name(), topic);
            return Optional.of("permission_not_granted: " + required.name());
        }
        return Optional.empty();
    }

    /**
     * A refusal may be answered to a connection that a revocation has just closed: the
     * subscribe frame was already queued when the grant went, on both the check before the add and
     * the one after it. Once the socket has reached CLOSED, Grizzly throws on send (while it is
     * still CLOSING the send goes through and the client simply reads the refusal), and the
     * executor running {@code onMessage} discards its future, so left alone the throw is neither
     * handled nor logged. Here it is the expected outcome, not an error to report.
     */
    private void refuse(WebSocket webSocket, String requestId, String errorMessage) {
        try {
            sendSubscriptionResponse(webSocket, requestId, null, errorMessage);
        } catch (RuntimeException e) {
            log.debug("Could not answer refused subscription {}", requestId, e);
        }
    }

    private void removeSubscriber(Subscriber subscriber) {
        if (subscriber != null) {
            subscriberRepository.remove(subscriber);
        }
    }

    private void sendSubscriptionResponse(WebSocket webSocket,
                                          String requestId,
                                          String payload,
                                          String errorMessage) {
        new SubscriptionResponse(requestId, payload, errorMessage)
                .toJson()
                .ifPresent(json -> {
                    String responseType = errorMessage == null ? "" : " error";
                    // Split by level as in BaseWebSocketService#send, and needed independently of it:
                    // this response goes straight out through the socket below, so the per-event TRACE
                    // there never sees it. The payload is the whole initial snapshot — for private chat
                    // every message of every DM channel — which is also the first thing a client parses,
                    // and so the thing most worth having at TRACE when one fails to.
                    // errorMessage stays at INFO: it is ours, not the user's data.
                    log.info("Send SubscriptionResponse{} for requestId {}. jsonLength={}{}",
                            responseType, requestId, json.length(),
                            errorMessage == null ? "" : ", errorMessage=" + errorMessage);
                    log.trace("SubscriptionResponse payload for requestId {}: {}", requestId, json);
                    webSocket.send(json);
                });
    }

    public void unSubscribe(Topic topic, String subscriberId) {
        subscriberRepository.remove(topic, subscriberId);
    }

    private Optional<BaseWebSocketService> findWebSocketService(Topic topic) {
        switch (topic) {
            case MARKET_PRICE -> {
                return Optional.of(marketPriceWebSocketService);
            }
            case NUM_OFFERS -> {
                return Optional.of(numOffersWebSocketService);
            }
            case OFFERS -> {
                return Optional.of(offersWebSocketService);
            }
            case TRADES -> {
                return Optional.of(tradesWebSocketService);
            }
            case TRADE_PROPERTIES -> {
                return Optional.of(tradePropertiesWebSocketService);
            }
            case TRADE_CHAT_MESSAGES -> {
                return Optional.of(tradeChatMessagesWebSocketService);
            }
            case CHAT_REACTIONS -> {
                return Optional.of(chatReactionsWebSocketService);
            }
            case REPUTATION -> {
                return Optional.of(reputationWebSocketService);
            }
            case NUM_USER_PROFILES -> {
                return Optional.of(numUserProfilesWebSocketService);
            }
            case TRADE_RESTRICTING_ALERT -> {
                return Optional.of(tradeRestrictingAlertWebSocketService);
            }
            case ALERT_NOTIFICATIONS -> {
                return Optional.of(alertNotificationsWebSocketService);
            }
            case NETWORK_INFO -> {
                return Optional.of(networkInfoWebSocketService);
            }
            case PRIVATE_CHAT_CHANNELS -> {
                return Optional.of(privateChatChannelsWebSocketService);
            }
            case PRIVATE_CHAT_MESSAGES -> {
                return Optional.of(privateChatMessagesWebSocketService);
            }
            case PRIVATE_CHAT_REACTIONS -> {
                return Optional.of(privateChatReactionsWebSocketService);
            }
            case CONTACTS -> {
                return Optional.of(contactsWebSocketService);
            }
            case PUBLIC_CHAT_CHANNELS -> {
                return Optional.of(publicChatChannelsWebSocketService);
            }
            case PUBLIC_CHAT_MESSAGES -> {
                return Optional.of(publicChatMessagesWebSocketService);
            }
            case PUBLIC_CHAT_REACTIONS -> {
                return Optional.of(publicChatReactionsWebSocketService);
            }
        }
        log.warn("No WebSocketService for topic {} found", topic);
        return Optional.empty();
    }
}

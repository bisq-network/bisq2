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

package bisq.chat.mu_sig.open_trades;

import bisq.chat.ChatChannelDomain;
import bisq.chat.notifications.ChatChannelNotificationType;
import bisq.chat.priv.PrivateGroupChatChannel;
import bisq.common.observable.Observable;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * PrivateTradeChannel is either a 2 party channel of both traders or
 * a 3 party channel with 2 traders and the mediator or the arbitrator.
 * Depending on the case the fields are differently interpreted.
 * Maybe we should model a group chat channel for a cleaner API.
 */
@ToString(callSuper = true)
@Slf4j
@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class MuSigOpenTradeChannel extends PrivateGroupChatChannel<MuSigOpenTradeMessage> {
    public static String createId(String tradeId) {
        return ChatChannelDomain.MU_SIG_OPEN_TRADES.name().toLowerCase(Locale.ROOT) + "." + tradeId;
    }

    public static MuSigOpenTradeChannel create(String tradeId,
                                               UserIdentity myUserIdentity,
                                               Set<UserProfile> traders,
                                               Optional<UserProfile> mediator,
                                               Optional<UserProfile> arbitrator,
                                               MuSigDisputeAgentType disputeAgentType) {
        return new MuSigOpenTradeChannel(tradeId,
                myUserIdentity,
                traders,
                mediator,
                arbitrator,
                disputeAgentType);
    }

    @EqualsAndHashCode.Include
    private final String tradeId;
    private final Set<UserProfile> traders;
    private final Optional<UserProfile> mediator;
    private final Optional<UserProfile> arbitrator;
    private final Observable<MuSigDisputeAgentType> disputeAgentTypeObservable =
            new Observable<>(MuSigDisputeAgentType.NONE);

    private MuSigOpenTradeChannel(String tradeId,
                                  UserIdentity myUserIdentity,
                                  Set<UserProfile> traders,
                                  Optional<UserProfile> mediator,
                                  Optional<UserProfile> arbitrator,
                                  MuSigDisputeAgentType disputeAgentType) {
        this(createId(tradeId),
                tradeId,
                myUserIdentity,
                traders,
                mediator,
                arbitrator,
                new HashSet<>(),
                disputeAgentType,
                ChatChannelNotificationType.ALL);
    }

    private MuSigOpenTradeChannel(String channelId,
                                 String tradeId,
                                 UserIdentity myUserIdentity,
                                 Set<UserProfile> traders,
                                 Optional<UserProfile> mediator,
                                 Optional<UserProfile> arbitrator,
                                 Set<MuSigOpenTradeMessage> chatMessages,
                                 MuSigDisputeAgentType disputeAgentType,
                                 ChatChannelNotificationType chatChannelNotificationType) {
        super(channelId, ChatChannelDomain.MU_SIG_OPEN_TRADES, myUserIdentity, chatMessages, chatChannelNotificationType);

        this.tradeId = tradeId;
        this.traders = traders;
        this.mediator = mediator;
        this.arbitrator = arbitrator;

        setDisputeAgentType(disputeAgentType);

        traders.forEach(userProfile -> userProfileIdsOfSendingLeaveMessage.add(userProfile.getId()));
    }

    @Override
    public bisq.chat.protobuf.ChatChannel.Builder getBuilder(boolean serializeForHash) {
        bisq.chat.protobuf.MuSigOpenTradeChannel.Builder builder = bisq.chat.protobuf.MuSigOpenTradeChannel.newBuilder()
                .setTradeId(tradeId)
                .setMyUserIdentity(myUserIdentity.toProto(serializeForHash))
                .addAllTraders(getTraders().stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()));
        mediator.ifPresent(mediator -> builder.setMediator(mediator.toProto(serializeForHash)));
        arbitrator.ifPresent(arbitrator -> builder.setArbitrator(arbitrator.toProto(serializeForHash)));
        builder.addAllChatMessages(chatMessages.stream()
                .map(e -> e.toValueProto(serializeForHash))
                .collect(Collectors.toList()));
        builder.setDisputeAgentType(getDisputeAgentType().toProtoEnum());
        return getChatChannelBuilder().setMuSigOpenTradeChannel(builder);
    }

    public static MuSigOpenTradeChannel fromProto(bisq.chat.protobuf.ChatChannel baseProto,
                                                  bisq.chat.protobuf.MuSigOpenTradeChannel proto) {
        return new MuSigOpenTradeChannel(
                baseProto.getId(),
                proto.getTradeId(),
                UserIdentity.fromProto(proto.getMyUserIdentity()),
                proto.getTradersList().stream()
                        .map(UserProfile::fromProto)
                        .collect(Collectors.toSet()),
                proto.hasMediator() ? Optional.of(UserProfile.fromProto(proto.getMediator())) : Optional.empty(),
                proto.hasArbitrator() ? Optional.of(UserProfile.fromProto(proto.getArbitrator())) : Optional.empty(),
                proto.getChatMessagesList().stream()
                        .map(MuSigOpenTradeMessage::fromProto)
                        .collect(Collectors.toSet()),
                MuSigDisputeAgentType.fromProto(proto.getDisputeAgentType()),
                ChatChannelNotificationType.fromProto(baseProto.getChatChannelNotificationType()));
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    @Override
    public String getDisplayString() {
        if (isMediator() || isArbitrator()) {
            checkArgument(traders.size() == 2, "traders.size() need to be 2 but is " + traders.size());
            List<UserProfile> tradersAsList = new ArrayList<>(traders);
            return tradeId + ": " + tradersAsList.get(0).getUserName() + " - " + tradersAsList.get(1).getUserName();
        } else {
            String peer = getPeer().getUserName();
            String optionalDisputeAgentPostfix = switch (getDisputeAgentType()) {
                case MEDIATOR -> mediator
                        .map(userProfile -> ", " + userProfile.getUserName() + " (" + Res.get("muSig.mediator") + ")")
                        .orElse("");
                case ARBITRATOR -> arbitrator
                        .map(userProfile -> ", " + userProfile.getUserName() + " (" + Res.get("muSig.arbitrator") + ")")
                        .orElse("");
                default -> "";
            };
            return tradeId + ": " + peer + optionalDisputeAgentPostfix;
        }
    }

    public boolean isMediator() {
        return mediator.filter(mediator -> mediator.getId().equals(myUserIdentity.getId())).isPresent();
    }

    public boolean isArbitrator() {
        return arbitrator.filter(arbitrator -> arbitrator.getId().equals(myUserIdentity.getId())).isPresent();
    }

    public UserProfile getPeer() {
        checkArgument(!traders.isEmpty(),
                "traders is expected to has at least size 1 at getPeer() in  MuSigOpenTradeChannel");
        return new ArrayList<>(traders).get(0);
    }

    @Override
    public boolean addChatMessage(MuSigOpenTradeMessage chatMessage) {
        boolean changed = super.addChatMessage(chatMessage);
        if (changed) {
            String authorUserProfileId = chatMessage.getAuthorUserProfileId();


            // todo (refactor, low prio) we get called from inside constructor at fromProto. should be redesigned
            // If we received a leave message the user got removed from userProfileIdsOfParticipants
            // In that case we remove them from userProfileIdsOfSendingLeaveMessage as well to avoid sending a 
            // leave message.
            if (!userProfileIdsOfActiveParticipants.contains(authorUserProfileId)) {
                userProfileIdsOfSendingLeaveMessage.remove(authorUserProfileId);
            }
        }
        return changed;
    }


    /* --------------------------------------------------------------------- */
    // Getter, setter
    /* --------------------------------------------------------------------- */

    public Observable<MuSigDisputeAgentType> disputeAgentTypeObservable() {
        return disputeAgentTypeObservable;
    }

    public void setDisputeAgentType(MuSigDisputeAgentType disputeAgentType) {
        disputeAgentTypeObservable.set(disputeAgentType);

        mediator.ifPresent(userProfile -> {
            if (disputeAgentType == MuSigDisputeAgentType.MEDIATOR) {
                userProfileIdsOfSendingLeaveMessage.add(userProfile.getId());
            } else {
                userProfileIdsOfSendingLeaveMessage.remove(userProfile.getId());
            }
        });

        arbitrator.ifPresent(userProfile -> {
            if (disputeAgentType == MuSigDisputeAgentType.ARBITRATOR) {
                userProfileIdsOfSendingLeaveMessage.add(userProfile.getId());
            } else {
                userProfileIdsOfSendingLeaveMessage.remove(userProfile.getId());
            }
        });
    }

    public MuSigDisputeAgentType getDisputeAgentType() {
        return disputeAgentTypeObservable.get();
    }
}

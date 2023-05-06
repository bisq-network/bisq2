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

package bisq.desktop.helpers;

import bisq.chat.trade.channel.PrivateTradeChannelService;
import bisq.chat.trade.channel.PrivateTradeChatChannel;
import bisq.chat.trade.message.PublicTradeChatMessage;
import bisq.chat.trade.message.TradeChatOffer;
import bisq.network.NetworkService;
import bisq.support.MediationService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

public class TakeOfferHelper {
    public static CompletableFuture<NetworkService.SendMessageResult> sendTakeOfferMessage(UserProfileService userProfileService,
                                                                                           UserIdentityService userIdentityService,
                                                                                           MediationService mediationService,
                                                                                           PrivateTradeChannelService privateTradeChannelService,
                                                                                           PublicTradeChatMessage tradeChatMessage) {
        checkArgument(tradeChatMessage.getTradeChatOffer().isPresent(), "tradeChatMessage must contain offer");
        return userProfileService.findUserProfile(tradeChatMessage.getAuthorId())
                .map(makerUserProfile -> {
                    UserIdentity myUserIdentity = userIdentityService.getSelectedUserIdentity().get();
                    TradeChatOffer tradeChatOffer = tradeChatMessage.getTradeChatOffer().get();
                    Optional<UserProfile> mediator = mediationService.takerSelectMediator(makerUserProfile.getId(), myUserIdentity.getUserProfile().getId());
                    PrivateTradeChatChannel privateTradeChannel = privateTradeChannelService.traderFindOrCreatesChannel(tradeChatOffer,
                            myUserIdentity,
                            makerUserProfile,
                            mediator);
                    return privateTradeChannelService.sendTakeOfferMessage(tradeChatMessage, privateTradeChannel);
                })
                .orElse(CompletableFuture.failedFuture(new RuntimeException("makerUserProfile not found from tradeChatMessage.authorId")));
    }
}
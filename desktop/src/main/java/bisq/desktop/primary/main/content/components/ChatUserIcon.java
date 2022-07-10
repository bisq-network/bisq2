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

package bisq.desktop.primary.main.content.components;

import bisq.desktop.components.robohash.RoboHash;
import bisq.identity.PublicUserProfile;
import bisq.social.user.ChatUserService;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatUserIcon extends Pane {
    private final ImageView roboIcon;
    private final ImageView entitlement;
    private final Tooltip tooltip;

    public ChatUserIcon(double size) {
        tooltip = new Tooltip();
        tooltip.setId("proof-of-burn-tooltip");
        roboIcon = new ImageView();
        roboIcon.setFitWidth(size);
        roboIcon.setFitHeight(size);

        entitlement = new ImageView();
        entitlement.setFitWidth(size / 2);
        entitlement.setFitHeight(size / 2);
        entitlement.setX(size * 2 / 3);
        entitlement.setY(size * 2 / 3);
        entitlement.setVisible(false);
        entitlement.setManaged(false);

        getChildren().addAll(roboIcon, entitlement);
    }

    public void setChatUser(PublicUserProfile publicUserProfile, ChatUserService chatUserService) {
        roboIcon.setImage(RoboHash.getImage(publicUserProfile.getPubKeyHash()));

       /* if (chatUser.hasEntitlementType(Role.Type.LIQUIDITY_PROVIDER)) {
            entitlement.setId("chat-trust");

            // We get asynchronous the verified burnInfo results. It is cached in the userProfileService
            // So we should get fast results in most cases.
            chatUserService.findBurnInfoAsync(chatUser.getPubKeyHash(), chatUser.getRoles())
                    .whenComplete((optionalBurnInfo, t) -> {
                        optionalBurnInfo.ifPresent(burnInfo -> {
                            UIThread.run(() -> {
                                entitlement.setVisible(true);
                                entitlement.setManaged(true);
                                Tooltip.install(entitlement, tooltip);
                                tooltip.setText(Res.get("social.chatUser.liquidityProvider.tooltip",
                                        AmountFormatter.formatAmountWithCode(Monetary.from(burnInfo.totalBsqBurned(), "BSQ")),
                                        DateFormatter.formatDateTime(burnInfo.firstBurnDate())));
                            });
                        });
                    });
        } else if (chatUser.hasEntitlementType(Role.Type.CHANNEL_ADMIN)) {
            //trustIconImageView.setId("chat-trust"); //todo define icon
            entitlement.setVisible(true);
            entitlement.setManaged(true);
        } else if (chatUser.hasEntitlementType(Role.Type.CHANNEL_MODERATOR)) {
            //entitlement.setId("chat-trust"); //todo define icon
            entitlement.setVisible(true);
            entitlement.setManaged(true);
        }*/
    }

    public void releaseResources() {
        roboIcon.setImage(null);
        entitlement.setOnMouseEntered(null);
        Tooltip.uninstall(entitlement, tooltip);
    }
}
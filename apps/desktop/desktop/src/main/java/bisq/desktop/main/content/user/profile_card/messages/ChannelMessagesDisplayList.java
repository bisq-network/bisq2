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

package bisq.desktop.main.content.user.profile_card.messages;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookSelectionService;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.pub.PublicChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;

import static bisq.chat.ChatChannelDomain.*;

public class ChannelMessagesDisplayList<M extends PublicChatMessage> {
    private final Controller controller;

    public ChannelMessagesDisplayList(ServiceProvider serviceProvider,
                                      PublicChatChannel<M> publicChatChannel,
                                      UserProfile userProfile) {
        controller = new Controller(serviceProvider, publicChatChannel, userProfile);
    }

    public VBox getRoot() {
        return controller.view.getRoot();
    }

    public ReadOnlyBooleanProperty shouldShowMessageDisplayList() {
        return controller.model.getShouldShow();
    }

    private class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private final PublicChatChannel<M> publicChatChannel;
        private final UserProfile userProfile;
        private final UserProfileService userProfileService;
        private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
        private final BisqEasyOfferbookSelectionService bisqEasyOfferbookChannelSelectionService;
        private final Map<ChatChannelDomain, CommonPublicChatChannelService> commonPublicChatChannelServices;
        private Pin publicMessagesPin;

        private Controller(ServiceProvider serviceProvider,
                           PublicChatChannel<M> publicChatChannel,
                           UserProfile userProfile) {
            model = new Model();
            view = new View(model, this);
            this.publicChatChannel = publicChatChannel;
            this.userProfile = userProfile;
            userProfileService = serviceProvider.getUserService().getUserProfileService();

            ChatService chatService = serviceProvider.getChatService();
            bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
            bisqEasyOfferbookChannelSelectionService = chatService.getBisqEasyOfferbookChannelSelectionService();
            commonPublicChatChannelServices = chatService.getCommonPublicChatChannelServices();
        }

        @Override
        public void onActivate() {
            model.getChannelName().set(publicChatChannel.getDisplayString());
            String iconId = publicChatChannel.getChatChannelDomain() == DISCUSSION ? "bisq-31" : "support-31";
            model.getChannelIconId().set(iconId);
            if (publicChatChannel instanceof BisqEasyOfferbookChannel bisqEasyOfferbookChannel) {
                model.getChannelName().set(bisqEasyOfferbookChannel.getMarket().getFiatCurrencyName());
                model.getMarketCurrencyCode().set(bisqEasyOfferbookChannel.getMarket().getQuoteCurrencyCode());
            }

            publicMessagesPin = publicChatChannel.getChatMessages().addObserver(new CollectionObserver<>() {
                @Override
                public void add(M element) {
                    boolean isBisqEasyOffer = element instanceof BisqEasyOfferbookMessage bisqEasyOfferbookMessage
                            && bisqEasyOfferbookMessage.hasBisqEasyOffer();
                    boolean isAuthor = element.getAuthorUserProfileId().equals(userProfile.getId());
                    if (!isBisqEasyOffer && isAuthor) {
                        UIThread.runOnNextRenderFrame(() -> {
                            boolean elementMissing = model.getChannelMessageItems().stream()
                                    .noneMatch(item -> item.getPublicChatMessage().equals(element));
                            if (elementMissing) {
                                Optional<String> citationAuthorName = Optional.empty();
                                if (element.getCitation().isPresent()) {
                                    String citationAuthorId = element.getCitation().get().getAuthorUserProfileId();
                                    citationAuthorName = Optional.of(controller.getUserName(citationAuthorId));
                                }
                                ChannelMessageItem chatMessageItem = new ChannelMessageItem(element, userProfile, citationAuthorName);
                                model.getChannelMessageItems().add(chatMessageItem);
                                updateShouldShow();
                            }
                        });
                    }
                }

                @Override
                public void remove(Object element) {
                    if (element instanceof PublicChatMessage && ((PublicChatMessage) element).getChatMessageType() == ChatMessageType.TEXT) {
                        UIThread.runOnNextRenderFrame(() -> {
                            PublicChatMessage publicChatMessage = (PublicChatMessage) element;
                            Optional<ChannelMessageItem> toRemove = model.getChannelMessageItems().stream()
                                    .filter(item -> item.getPublicChatMessage().getId().equals(publicChatMessage.getId()))
                                    .findAny();
                            toRemove.ifPresent(item -> {
                                item.dispose();
                                model.getChannelMessageItems().remove(item);
                                updateShouldShow();
                            });
                        });
                    }
                }

                @Override
                public void clear() {
                    UIThread.runOnNextRenderFrame(() -> {
                        model.getChannelMessageItems().forEach(ChannelMessageItem::dispose);
                        model.getChannelMessageItems().clear();
                        updateShouldShow();
                    });
                }
            });

            model.getSortedChannelMessageItems().setComparator(ChannelMessageItem::compareTo);

            updateShouldShow();
        }

        @Override
        public void onDeactivate() {
            publicMessagesPin.unbind();
        }

        private String getUserName(String userProfileId) {
            return userProfileService.findUserProfile(userProfileId)
                    .map(UserProfile::getUserName)
                    .orElse(Res.get("data.na"));
        }

        private void onGoToMessage(PublicChatMessage publicChatMessage) {
            ChatChannelDomain chatChannelDomain = publicChatMessage.getChatChannelDomain();
            Optional<NavigationTarget> navigationTarget = Optional.empty();
            if (chatChannelDomain == DISCUSSION || chatChannelDomain == SUPPORT) {
                navigationTarget = Optional.of(chatChannelDomain == DISCUSSION
                        ? NavigationTarget.CHAT_DISCUSSION
                        : NavigationTarget.SUPPORT_ASSISTANCE);
                commonPublicChatChannelServices.get(chatChannelDomain).findChannel(publicChatMessage.getChannelId())
                        .ifPresent(channel -> channel.getHighlightedMessage().set(publicChatMessage));
            } else if (chatChannelDomain == BISQ_EASY_OFFERBOOK) {
                navigationTarget = Optional.of(NavigationTarget.BISQ_EASY_OFFERBOOK);
                bisqEasyOfferbookChannelService.findChannel(publicChatMessage.getChannelId())
                    .ifPresent(channel -> {
                        bisqEasyOfferbookChannelSelectionService.selectChannel(channel);
                        channel.getHighlightedMessage().set(publicChatMessage);
                });
            }

            navigationTarget.ifPresent(navTarget -> OverlayController.hide(() -> Navigation.navigateTo(navTarget)));
        }

        private void updateShouldShow() {
            model.getShouldShow().set(!model.getChannelMessageItems().isEmpty());
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final StringProperty channelName = new SimpleStringProperty();
        private final StringProperty channelIconId = new SimpleStringProperty();
        private final StringProperty marketCurrencyCode = new SimpleStringProperty();
        private final ObservableList<ChannelMessageItem> channelMessageItems = FXCollections.observableArrayList();
        private final SortedList<ChannelMessageItem> sortedChannelMessageItems = new SortedList<>(channelMessageItems);
        private final BooleanProperty shouldShow = new SimpleBooleanProperty();
    }

    private class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Label headline;
        private final VBox messageListVBox;
        private final ListChangeListener<ChannelMessageItem> listChangeListener;

        private View(Model model, Controller controller) {
            super(new VBox(20), model, controller);

            headline = new Label();
            Region line = getLine();
            HBox headlineHBox = new HBox(10, headline, line);
            HBox.setHgrow(line, Priority.ALWAYS);
            headlineHBox.setPadding(new Insets(0, 20, 0, 20));
            headlineHBox.setAlignment(Pos.CENTER);
            messageListVBox = new VBox(30);

            listChangeListener = change -> updateMessageListVBox();

            root.getChildren().addAll(headlineHBox, messageListVBox);
            root.setPadding(new Insets(20, 0, 20, 0));
        }

        @Override
        protected void onViewAttached() {
            headline.setText(model.getChannelName().get());
            Node image;
            if (model.getMarketCurrencyCode().get() != null) {
                // Bisq Easy market logo
                image = MarketImageComposition.createMarketLogo(model.getMarketCurrencyCode().get());
                image.setCache(true);
                image.setCacheHint(CacheHint.SPEED);

            } else {
                // Discussions and support
                image = ImageUtil.getImageViewById(model.getChannelIconId().get());
            }
            headline.setGraphic(image);
            headline.setGraphicTextGap(10);
            root.visibleProperty().bind(model.getShouldShow());
            root.managedProperty().bind(model.getShouldShow());
            model.getSortedChannelMessageItems().addListener(listChangeListener);
            updateMessageListVBox();
        }

        @Override
        protected void onViewDetached() {
            root.visibleProperty().unbind();
            root.managedProperty().unbind();
            model.getSortedChannelMessageItems().removeListener(listChangeListener);
        }

        private void updateMessageListVBox() {
            clearMessageListVBox();
            model.getSortedChannelMessageItems().forEach(channelMessageItem -> {
                messageListVBox.getChildren().add(new ChannelMessageBox(channelMessageItem));
            });
        }

        private void clearMessageListVBox() {
            messageListVBox.getChildren().forEach(node -> {
                if (node.getClass().equals(ChannelMessageBox.class)) {
                    //noinspection unchecked
                    ((ChannelMessageBox) node).dispose();
                }
            });
            messageListVBox.getChildren().clear();
        }

        private Region getLine() {
            Region line = new Region();
            line.setMinHeight(1);
            line.setMaxHeight(1);
            line.setStyle("-fx-background-color: -bisq-border-color-grey");
            return line;
        }
    }

    private class ChannelMessageBox extends HBox {
        private final ImageView catHashImageView;
        private final BisqMenuItem goToMessageButton;

        private ChannelMessageBox(ChannelMessageItem channelMessageItem) {
            Label dateTimeLabel = new Label(channelMessageItem.getDateTime());
            dateTimeLabel.getStyleClass().addAll("text-fill-grey-dimmed", "font-size-09", "font-light");

            Label textMessageLabel = new Label(channelMessageItem.getMessage());
            textMessageLabel.setPadding(new Insets(10));
            textMessageLabel.getStyleClass().addAll("wrap-text", "text-fill-white", "medium-text", "font-default");
            textMessageLabel.setMinHeight(Label.USE_PREF_SIZE);

            Label citationAuthor = new Label();
            channelMessageItem.getCitationAuthorName().ifPresent(citationAuthor::setText);
            citationAuthor.getStyleClass().add("font-medium");
            citationAuthor.setStyle("-fx-text-fill: -bisq-mid-grey-30");
            Label citationMessage = new Label();
            citationMessage.setMinHeight(Label.USE_PREF_SIZE);
            citationMessage.setWrapText(true);
            citationMessage.setStyle("-fx-fill: -fx-mid-text-color");
            VBox citationMessageVBox = new VBox(citationAuthor, citationMessage);
            citationMessageVBox.setId("chat-message-quote-box-peer-msg");
            citationMessageVBox.setVisible(false);
            citationMessageVBox.setManaged(false);

            channelMessageItem.getCitation().ifPresent(citation -> {
                if (citation.isValid()) {
                    citationMessageVBox.setVisible(true);
                    citationMessageVBox.setManaged(true);
                    citationMessage.setText(citation.getText());
                }
            });

            VBox textMessageVBox = new VBox(10, citationMessageVBox, textMessageLabel);
            HBox.setMargin(textMessageVBox, new Insets(0, 0, 0, -10));

            catHashImageView = new ImageView();
            catHashImageView.setImage(CatHash.getImage(channelMessageItem.getSenderUserProfile(),
                    catHashImageView.getFitWidth()));
            catHashImageView.setFitWidth(30);
            catHashImageView.setFitHeight(catHashImageView.getFitWidth());
            HBox.setMargin(catHashImageView, new Insets(5, 0, 0, 5));

            goToMessageButton = new BisqMenuItem(Res.get("user.profileCard.messages.goToMessage.button"));
            goToMessageButton.getStyleClass().addAll("text-underline", "text-fill-grey-dimmed");
            goToMessageButton.setMaxWidth(BisqMenuItem.USE_PREF_SIZE);
            goToMessageButton.setMinWidth(BisqMenuItem.USE_PREF_SIZE);
            goToMessageButton.setOnAction(e -> controller.onGoToMessage(channelMessageItem.getPublicChatMessage()));
            HBox.setMargin(goToMessageButton, new Insets(0, 0, 0, 20));

            HBox messageBubbleHBox = new HBox(15, catHashImageView, textMessageVBox);
            messageBubbleHBox.setAlignment(Pos.TOP_LEFT);
            messageBubbleHBox.getStyleClass().add("message-bg");
            messageBubbleHBox.setPadding(new Insets(5, 15, 5, 15));

            HBox bubbleAndGoToButtonHBox = new HBox(messageBubbleHBox, Spacer.fillHBox(), goToMessageButton);
            bubbleAndGoToButtonHBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(bubbleAndGoToButtonHBox, Priority.ALWAYS);

            VBox messageBg = new VBox(dateTimeLabel, bubbleAndGoToButtonHBox);
            messageBg.setFillWidth(true);
            HBox.setHgrow(messageBg, Priority.ALWAYS);

            setAlignment(Pos.CENTER_LEFT);
            setFillHeight(true);
            setPadding(new Insets(0, 50, 0, 50));
            getChildren().add(messageBg);

            initialize();
        }

        private void initialize() {
        }

        private void dispose() {
            catHashImageView.setImage(null);
            goToMessageButton.setOnAction(null);
        }
    }
}

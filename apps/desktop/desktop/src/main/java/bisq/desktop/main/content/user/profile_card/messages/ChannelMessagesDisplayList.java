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

import bisq.chat.ChatMessageType;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.pub.PublicChatMessage;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.cathash.CatHash;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.util.Optional;

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

    private class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private final PublicChatChannel<M> publicChatChannel;
        private final UserProfile userProfile;
        private final UserProfileService userProfileService;
        private Pin publicMessagesPin;

        private Controller(ServiceProvider serviceProvider,
                           PublicChatChannel<M> publicChatChannel,
                           UserProfile userProfile) {
            model = new Model();
            view = new View(model, this);
            this.publicChatChannel = publicChatChannel;
            this.userProfile = userProfile;
            userProfileService = serviceProvider.getUserService().getUserProfileService();
        }

        @Override
        public void onActivate() {
            model.getChannelName().set(publicChatChannel.getDisplayString());
            if (publicChatChannel instanceof BisqEasyOfferbookChannel bisqEasyOfferbookChannel) {
                model.getChannelName().set(bisqEasyOfferbookChannel.getMarket().getFiatCurrencyName());
                model.getMarket().set(bisqEasyOfferbookChannel.getMarket());
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
                                ChannelMessageItem chatMessageItem = new ChannelMessageItem(element, userProfile);
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

            updateShouldShow();
        }

        @Override
        public void onDeactivate() {
            publicMessagesPin.unbind();
        }

        String getUserName(String userProfileId) {
            return userProfileService.findUserProfile(userProfileId)
                    .map(UserProfile::getUserName)
                    .orElse(Res.get("data.na"));
        }

        private void updateShouldShow() {
            model.getShouldShow().set(!model.getChannelMessageItems().isEmpty());
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final StringProperty channelName = new SimpleStringProperty();
        private final ObjectProperty<Market> market = new SimpleObjectProperty<>();
        private final ObservableList<ChannelMessageItem> channelMessageItems = FXCollections.observableArrayList();
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
            root.visibleProperty().bind(model.getShouldShow());
            root.managedProperty().bind(model.getShouldShow());
            model.getChannelMessageItems().addListener(listChangeListener);
            updateMessageListVBox();
        }

        @Override
        protected void onViewDetached() {
            root.visibleProperty().unbind();
            root.managedProperty().unbind();
            model.getChannelMessageItems().removeListener(listChangeListener);
        }

        private void updateMessageListVBox() {
            clearMessageListVBox();
            model.getChannelMessageItems().forEach(item -> {
                ChannelMessageBox channelMessageBox = new ChannelMessageBox();
                String citationAuthorId = "";
                if (item.getCitation().isPresent()) {
                    citationAuthorId = item.getCitation().get().getAuthorUserProfileId();
                }
                channelMessageBox.setChannelMessageItem(item, controller.getUserName(citationAuthorId));
                messageListVBox.getChildren().add(channelMessageBox);
            });
        }

        private void clearMessageListVBox() {
            messageListVBox.getChildren().forEach(item -> {
                if (item instanceof ChannelMessageBox channelMessageBox) {
                    channelMessageBox.dispose();
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

    private static class ChannelMessageBox extends HBox {
        private final Label dateTimeLabel, textMessageLabel, citationMessage, citationAuthor;
        private final VBox citationMessageVBox;
        private final ImageView catHashImageView;

        private ChannelMessageBox() {
            dateTimeLabel = new Label();
            dateTimeLabel.getStyleClass().addAll("text-fill-grey-dimmed", "font-size-09", "font-light");

            textMessageLabel = new Label();
            textMessageLabel.setPadding(new Insets(10));
            textMessageLabel.getStyleClass().addAll("wrap-text", "text-fill-white", "medium-text", "font-default");
            textMessageLabel.setMinHeight(Label.USE_PREF_SIZE);

            citationMessage = new Label();
            citationMessage.setMinHeight(Label.USE_PREF_SIZE);
            citationMessage.setWrapText(true);
            citationMessage.setStyle("-fx-fill: -fx-mid-text-color");
            citationAuthor = new Label();
            citationAuthor.getStyleClass().add("font-medium");
            citationAuthor.setStyle("-fx-text-fill: -bisq-mid-grey-30");
            citationMessageVBox = new VBox(citationMessage, citationAuthor);
            citationMessageVBox.setId("chat-message-quote-box-peer-msg");
            citationMessageVBox.setVisible(false);
            citationMessageVBox.setManaged(false);

            VBox textMessageVBox = new VBox(10, citationMessageVBox, textMessageLabel);
            HBox.setMargin(textMessageVBox, new Insets(0, 0, 0, -10));

            catHashImageView = new ImageView();
            catHashImageView.setFitWidth(30);
            catHashImageView.setFitHeight(catHashImageView.getFitWidth());
            HBox.setMargin(catHashImageView, new Insets(5, 0, 0, 5));

            HBox messageBubbleHBox = new HBox(15, catHashImageView, textMessageVBox);
            messageBubbleHBox.setAlignment(Pos.TOP_LEFT);
            messageBubbleHBox.getStyleClass().add("message-bg");
            messageBubbleHBox.setPadding(new Insets(5, 15, 5, 15));

            VBox messageBg = new VBox(dateTimeLabel, messageBubbleHBox);

            setAlignment(Pos.CENTER_LEFT);
            setFillHeight(true);
            setPadding(new Insets(0, 50, 0, 50));
            // TODO: Add goToMessage button
            getChildren().add(messageBg);
        }

        private void setChannelMessageItem(ChannelMessageItem channelMessageItem, String citationAuthorName) {
            dateTimeLabel.setText(channelMessageItem.getDateTime());
            channelMessageItem.getCitation().ifPresent(citation -> {
                if (citation.isValid()) {
                    citationMessageVBox.setVisible(true);
                    citationMessageVBox.setManaged(true);
                    citationMessage.setText(citation.getText());
                    citationAuthor.setText(citationAuthorName);
                }
            });
            catHashImageView.setImage(CatHash.getImage(channelMessageItem.getSenderUserProfile(),
                    catHashImageView.getFitWidth()));
            textMessageLabel.setText(channelMessageItem.getMessage());
        }

        private void dispose() {
            catHashImageView.setImage(null);
        }
    }
}

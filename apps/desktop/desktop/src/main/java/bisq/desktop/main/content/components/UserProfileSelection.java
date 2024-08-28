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

package bisq.desktop.main.content.components;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatMessage;
import bisq.chat.priv.PrivateChatChannel;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.lang.ref.WeakReference;
import java.util.Map;

@Slf4j
public class UserProfileSelection {
    private final Controller controller;

    public UserProfileSelection(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider, 30, false);
    }

    public UserProfileSelection(ServiceProvider serviceProvider, int iconSize, boolean useMaterialStyle) {
        controller = new Controller(serviceProvider, iconSize, useMaterialStyle);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setIsLeftAligned(boolean isLeftAligned) {
        controller.model.getIsLeftAligned().set(isLeftAligned);
    }

    public void setMaxComboBoxWidth(int width) {
        controller.view.setMaxComboBoxWidth(width);
    }

    public void setConverter(StringConverter<ListItem> value) {
        controller.view.setConverter(value);
    }

    public boolean isFocused() {
        return controller.isFocused();
    }

    public ReadOnlyBooleanProperty focusedProperty() {
        return controller.focusedProperty();
    }

    public void requestFocus() {
        controller.requestFocus();
    }

    public void setPrefWidth(double value) {
        controller.setPrefWidth(value);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserIdentityService userIdentityService;
        private final Map<ChatChannelDomain, ChatChannelSelectionService> chatChannelSelectionServices;
        private final ListChangeListener<ListItem> listChangeListener;
        private Pin selectedUserProfilePin, userProfilesPin, chatChannelSelectionPin, navigationPin, isPrivateChannelPin;

        private Controller(ServiceProvider serviceProvider, int iconSize, boolean useMaterialStyle) {
            this.userIdentityService = serviceProvider.getUserService().getUserIdentityService();
            chatChannelSelectionServices = serviceProvider.getChatService().getChatChannelSelectionServices();
            listChangeListener = change -> updateShouldUseComboBox();

            model = new Model();
            view = new View(model, this, iconSize, useMaterialStyle);
        }

        @Override
        public void onActivate() {
            selectedUserProfilePin = FxBindings.subscribe(userIdentityService.getSelectedUserIdentityObservable(),
                    userProfile -> UIThread.run(() -> model.getSelectedUserProfile().set(new ListItem(userProfile))));
            userProfilesPin = FxBindings.<UserIdentity, ListItem>bind(model.getUserProfiles())
                    .map(ListItem::new)
                    .to(userIdentityService.getUserIdentities());

            navigationPin = Navigation.getCurrentNavigationTarget().addObserver(this::navigationTargetChanged);

            model.getUserProfiles().addListener(listChangeListener);
            isPrivateChannelPin = FxBindings.subscribe(model.getIsPrivateChannel(), isPrivate -> updateShouldUseComboBox());
        }

        @Override
        public void onDeactivate() {
            // Need to clear list otherwise we get issues with binding when multiple 
            // instances are used.
            model.getUserProfiles().clear();
            model.getUserProfiles().removeListener(listChangeListener);

            selectedUserProfilePin.unbind();
            userProfilesPin.unbind();
            navigationPin.unbind();
            if (chatChannelSelectionPin != null) {
                chatChannelSelectionPin.unbind();
            }
            isPrivateChannelPin.unbind();
        }

        private void onSelected(ListItem selectedItem) {
            if (selectedItem != null) {
                UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
                // To make sure a different user is never selected for a private channel it's safest to keep this check
                // even though the combobox should be disabled
                if (model.getIsPrivateChannel().get()) {
                    new Popup().warning(Res.get("chat.privateChannel.changeUserProfile.warn",
                                    selectedUserIdentity.getUserProfile().getUserName()))
                            .onClose(() -> {
                                model.getSelectedUserProfile().set(null);
                                model.getSelectedUserProfile().set(new ListItem(selectedUserIdentity));
                            })
                            .show();
                } else {
                    userIdentityService.selectChatUserIdentity(selectedItem.userIdentity);
                }
            }
        }

        private boolean isFocused() {
            return view.getComboBox().isFocused();
        }

        private ReadOnlyBooleanProperty focusedProperty() {
            return view.getComboBox().focusedProperty();
        }

        private void requestFocus() {
            view.getComboBox().requestFocus();
        }

        private void setPrefWidth(double value) {
            view.getComboBox().setPrefWidth(value);
        }

        private void navigationTargetChanged(NavigationTarget navigationTarget) {
            if (chatChannelSelectionPin != null) {
                chatChannelSelectionPin.unbind();
            }
            model.getIsPrivateChannel().set(false);
            switch (navigationTarget) {
                //case BISQ_EASY:
                case BISQ_EASY_OFFERBOOK:
                    selectionServiceChanged(chatChannelSelectionServices.get(ChatChannelDomain.BISQ_EASY_OFFERBOOK));
                    return;
                case BISQ_EASY_OPEN_TRADES:
                    selectionServiceChanged(chatChannelSelectionServices.get(ChatChannelDomain.BISQ_EASY_OPEN_TRADES));
                    return;
                case BISQ_EASY_PRIVATE_CHAT:
                    selectionServiceChanged(chatChannelSelectionServices.get(ChatChannelDomain.BISQ_EASY_PRIVATE_CHAT));
                    return;
                case DISCUSSION:
                    selectionServiceChanged(chatChannelSelectionServices.get(ChatChannelDomain.DISCUSSION));
                    return;
                case EVENTS:
                    selectionServiceChanged(chatChannelSelectionServices.get(ChatChannelDomain.EVENTS));
                    return;
                case SUPPORT:
                    selectionServiceChanged(chatChannelSelectionServices.get(ChatChannelDomain.SUPPORT));
            }
        }

        private void selectionServiceChanged(ChatChannelSelectionService chatChannelSelectionService) {
            chatChannelSelectionPin = chatChannelSelectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);
        }

        private void selectedChannelChanged(ChatChannel<? extends ChatMessage> channel) {
            UIThread.run(() -> model.getIsPrivateChannel().set(channel instanceof PrivateChatChannel));
        }

        private void updateShouldUseComboBox() {
            model.getShouldUseComboBox().set(!model.getIsPrivateChannel().get() && model.getUserProfiles().size() > 1);
        }
    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<ListItem> selectedUserProfile = new SimpleObjectProperty<>();
        private final ObservableList<ListItem> userProfiles = FXCollections.observableArrayList();
        private final DoubleProperty comboBoxWidth = new SimpleDoubleProperty();
        private final BooleanProperty isLeftAligned = new SimpleBooleanProperty();
        private final Observable<Boolean> isPrivateChannel = new Observable<>(false);
        private final Observable<Boolean> shouldUseComboBox = new Observable<>(false);

    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        @Getter
        private final UserProfileComboBox comboBox;
        private final HBox userNameAndIcon;
        private final Label userName;
        private final ImageView catHashImageView;
        private Subscription selectedUserProfilePin, isLeftAlignedPin, comboBoxWidthPin;
        private Pin isComboBoxPin;

        private View(Model model, Controller controller, int iconSize, boolean useMaterialStyle) {
            super(new Pane(), model, controller);

            comboBox = new UserProfileComboBox(model.getUserProfiles(), Res.get("user.userProfile.comboBox.description"),
                    iconSize, useMaterialStyle);
            comboBox.setLayoutY(UserProfileComboBox.Y_OFFSET);

            userName = new Label();
            userName.getStyleClass().add("bisq-text-19");
            catHashImageView = new ImageView();
            catHashImageView.setFitWidth(iconSize);
            catHashImageView.setFitHeight(catHashImageView.getFitWidth());
            userNameAndIcon = new HBox(10, userName, catHashImageView);
            userNameAndIcon.setLayoutY(8);
            userNameAndIcon.setAlignment(Pos.CENTER);
            root.getChildren().setAll(comboBox, userNameAndIcon);
            root.setPrefHeight(60);
        }

        @Override
        protected void onViewAttached() {
            comboBox.setOnChangeConfirmed(e -> controller.onSelected(comboBox.getSelectionModel().getSelectedItem()));
            selectedUserProfilePin = EasyBind.subscribe(model.getSelectedUserProfile(),
                    selected -> {
                        UIThread.runOnNextRenderFrame(() -> comboBox.getSelectionModel().select(selected));
                        if (selected != null) {
                            UserIdentity userIdentity = selected.userIdentity;
                            if (userIdentity != null) {
                                userName.setText(comboBox.getConverter().toString(selected));
                                catHashImageView.setImage(CatHash.getImage(userIdentity.getUserProfile(),
                                        catHashImageView.getFitWidth()));
                            }
                        }
                    });
            isLeftAlignedPin = EasyBind.subscribe(model.getIsLeftAligned(), isLeftAligned -> {
                comboBox.setIsLeftAligned(isLeftAligned);
                if (!isLeftAligned) {
                    HBox.setMargin(userName, new Insets(-2, 0, 0, 0));
                    HBox.setMargin(catHashImageView, new Insets(-1.5, 0, 0, 16));
                    userName.toFront();
                } else {
                    HBox.setMargin(userName, new Insets(-1, 0, 0, 0));
                    HBox.setMargin(catHashImageView, new Insets(0, 17, 0, 0));
                    userName.toBack();
                }
            });
            comboBoxWidthPin = EasyBind.subscribe(model.getComboBoxWidth(), w -> comboBox.setComboBoxWidth(w.doubleValue()));

            isComboBoxPin = FxBindings.subscribe(model.getShouldUseComboBox(), this::updateShouldUseComboBox);
        }

        @Override
        protected void onViewDetached() {
            selectedUserProfilePin.unsubscribe();
            isLeftAlignedPin.unsubscribe();
            comboBoxWidthPin.unsubscribe();
            isComboBoxPin.unbind();
            catHashImageView.setImage(null);
        }

        public void setMaxComboBoxWidth(int width) {
            comboBox.setComboBoxWidth(width);
        }

        public void setConverter(StringConverter<ListItem> value) {
            comboBox.setConverter(value);
        }

        private void updateShouldUseComboBox(boolean isComboBox) {
            comboBox.setManaged(isComboBox);
            comboBox.setVisible(isComboBox);
            userNameAndIcon.setManaged(!isComboBox);
            userNameAndIcon.setVisible(!isComboBox);
        }
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    public static class ListItem {
        @EqualsAndHashCode.Include
        private final UserIdentity userIdentity;

        private ListItem(UserIdentity userIdentity) {
            this.userIdentity = userIdentity;
        }

        @Override
        public String toString() {
            return userIdentity != null ? userIdentity.getUserName() : "";
        }
    }

    private static class UserProfileComboBox extends AutoCompleteComboBox<ListItem> {
        private final static int DEFAULT_COMBO_BOX_WIDTH = 200;
        private final static int Y_OFFSET = 30;
        private final int iconSize;

        private boolean isLeftAligned;

        public UserProfileComboBox(ObservableList<ListItem> items,
                                   String description,
                                   int iconSize,
                                   boolean useMaterialStyle) {
            super(items, description);

            this.iconSize = iconSize;
            ((UserProfileSkin) skin).setIconSize(iconSize);
            ((UserProfileSkin) skin).setUseMaterialStyle(useMaterialStyle);

            setPrefWidth(DEFAULT_COMBO_BOX_WIDTH);

            setCellFactory(param -> new ListCell<>() {
                private ChangeListener<Number> labelWidthListener;
                private final ImageView catHashImageView = new ImageView();
                private final Label label = new Label();
                private final HBox hBox = new HBox(10);

                {
                    label.setMouseTransparent(true);
                    catHashImageView.setFitWidth(iconSize);
                    catHashImageView.setFitHeight(catHashImageView.getFitWidth());
                    setPrefHeight(50);
                    setPadding(new Insets(10, 0, 0, 10));

                    hBox.setPadding(new Insets(0, 10, 0, 10));
                    if (isLeftAligned) {
                        hBox.setAlignment(Pos.CENTER_RIGHT);
                        hBox.getChildren().addAll(label, catHashImageView);
                    } else {
                        hBox.setAlignment(Pos.CENTER_LEFT);
                        hBox.getChildren().addAll(catHashImageView, label);
                    }

                    labelWidthListener = (observable, oldValue, newValue) -> {
                        if (newValue.doubleValue() > 0) {
                            UserProfileSelection.UserProfileComboBox.this.setComboBoxWidth(calculateWidth(label));
                            label.widthProperty().removeListener(labelWidthListener);
                        }
                    };
                }

                @Override
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        catHashImageView.setImage(CatHash.getImage(item.userIdentity.getUserProfile(),
                                catHashImageView.getFitWidth()));
                        label.setText(item.userIdentity.getUserName());
                        label.widthProperty().addListener(labelWidthListener);

                        setGraphic(hBox);
                    } else {
                        setGraphic(null);
                        catHashImageView.setImage(null);
                        label.widthProperty().removeListener(labelWidthListener);
                    }
                }
            });
        }

        private void setIsLeftAligned(boolean isLeftAligned) {
            this.isLeftAligned = isLeftAligned;
            ((UserProfileSkin) skin).setIsLeftAligned(isLeftAligned);
        }

        private void setComboBoxWidth(double width) {
            if (width == 0) {
                width = DEFAULT_COMBO_BOX_WIDTH;
            }
            setPrefWidth(width);
            ((UserProfileSkin) skin).setComboBoxWidth(width);
        }

        private void setMaxComboBoxWidth(double width) {
            if (width == 0) {
                width = DEFAULT_COMBO_BOX_WIDTH;
            }
            setPrefWidth(width);
            ((UserProfileSkin) skin).setMaxComboBoxWidth(width);
        }

        @Override
        protected Skin<?> createDefaultSkin() {
            if (skin == null) {
                skin = new UserProfileSkin(this, description, prompt);
                editor = skin.getMaterialTextField().getTextInputControl();
            }
            return skin;
        }

        private double calculateWidth(Label label) {
            double width = label.getWidth() + iconSize + 80;
            return Math.max(width, UserProfileComboBox.this.getPrefWidth());
        }
    }

    private static class UserProfileSkin extends AutoCompleteComboBox.Skin<ListItem> {
        private final static int ICON_PADDING = 17;
        private final static int ARROW_WIDTH = 10;
        private final static int ARROW_ICON_PADDING = 10;
        private final static int TEXT_PADDING = 6;
        private final Label label;
        private final ImageView catHashImageView;
        private int iconSize;
        private boolean isLeftAligned;

        public UserProfileSkin(ComboBox<ListItem> control, String description, String prompt) {
            super(control, description, prompt);

            int offset = 5;
            arrowX_l = DEFAULT_ARROW_X_L - offset;
            arrowX_m = DEFAULT_ARROW_X_M - offset;
            arrowX_r = DEFAULT_ARROW_X_R - offset;

            catHashImageView = new ImageView();
            catHashImageView.setLayoutY(7);

            label = new Label();

            buttonPane.getChildren().setAll(label, arrow, catHashImageView);
            buttonPane.setCursor(Cursor.HAND);
            buttonPane.setLayoutY(-UserProfileComboBox.Y_OFFSET);

            //todo Fix WeakReference
            control.getSelectionModel().selectedItemProperty().addListener(new WeakReference<>((ChangeListener<ListItem>) (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    UserIdentity userIdentity = newValue.userIdentity;
                    if (userIdentity != null) {
                        catHashImageView.setImage(CatHash.getImage(userIdentity.getUserProfile(),
                                catHashImageView.getFitWidth()));
                        label.setText(control.getConverter().toString(newValue));
                        buttonPane.layout();
                    }
                }
            }).get());

            UserProfileComboBox userProfileComboBox = (UserProfileComboBox) control;
            ChangeListener<Number> userNameLabelWidthListener = (observable, oldValue, newValue) -> {
                if (newValue.doubleValue() > 0) {
                    userProfileComboBox.setComboBoxWidth(userProfileComboBox.calculateWidth(label));
                }
            };
            label.widthProperty().addListener(userNameLabelWidthListener);
        }

        void setIconSize(int iconSize) {
            this.iconSize = iconSize;
            catHashImageView.setFitWidth(iconSize);
            catHashImageView.setFitHeight(catHashImageView.getFitWidth());
        }

        void setUseMaterialStyle(boolean useMaterialStyle) {
            if (useMaterialStyle) {
                arrow.setLayoutY(14);
                label.getStyleClass().add("material-text-field");
                label.setLayoutY(5.5);
            } else {
                arrow.setLayoutY(19);
                label.getStyleClass().add("bisq-text-19");
                label.setLayoutY(14);
            }
        }

        private void setIsLeftAligned(boolean isLeftAligned) {
            this.isLeftAligned = isLeftAligned;
        }

        private void setComboBoxWidth(double width) {
            buttonPane.setPrefWidth(width);
        }

        private void setMaxComboBoxWidth(double width) {
            setComboBoxWidth(width);
            label.setMaxWidth(width - iconSize - 80);
        }

        @Override
        protected void layoutChildren(final double x, final double y, final double w, final double h) {
            if (isLeftAligned) {
                double offset = comboBox.getWidth() - 5;
                arrowX_l = offset - DEFAULT_ARROW_X_L;
                arrowX_m = offset - DEFAULT_ARROW_X_M;
                arrowX_r = offset - DEFAULT_ARROW_X_R;
            }
            super.layoutChildren(x, y, w, h);

            if (isLeftAligned) {
                if (label.getWidth() > 0) {
                    double iconX = buttonPane.getPrefWidth() - ICON_PADDING - iconSize;
                    catHashImageView.setX(iconX);
                    double arrowX = iconX - ARROW_ICON_PADDING - ARROW_WIDTH;
                    arrow.setLayoutX(arrowX);
                    label.setLayoutX(arrowX - TEXT_PADDING - label.getWidth());
                }
            } else {
                if (label.getWidth() > 0) {
                    catHashImageView.setX(ICON_PADDING);
                    arrow.setLayoutX(ICON_PADDING + iconSize + ARROW_ICON_PADDING);
                    label.setLayoutX(ICON_PADDING + iconSize + ARROW_ICON_PADDING + ARROW_WIDTH + TEXT_PADDING);
                }
            }
        }

        @Override
        protected int getRowHeight() {
            return 50;
        }

        @Override
        public Node getDisplayNode() {
            return null;
        }
    }
}

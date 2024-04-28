package bisq.desktop.main.content.chat.message_container.list;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.desktop.common.Transitions;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.list_view.ListViewUtil;
import bisq.desktop.components.list_view.NoSelectionModel;
import bisq.desktop.main.content.chat.ChatUtil;
import bisq.i18n.Res;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class ChatMessagesListView extends bisq.desktop.common.view.View<ChatMessagesListView.CustomStackPane, ChatMessagesListModel, ChatMessagesListController> {
    @Getter
    public static class CustomStackPane extends StackPane {
        private final BooleanProperty layoutChildrenDone = new SimpleBooleanProperty();

        @Override
        protected void layoutChildren() {
            layoutChildrenDone.set(false);
            super.layoutChildren();
            layoutChildrenDone.set(true);
        }
    }

    private final ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> listView;
    private final ImageView scrollDownImageView;
    private final Badge scrollDownBadge;
    private final BisqTooltip scrollDownTooltip;
    private final Label placeholderTitle = new Label("");
    private final Label placeholderDescription = new Label("");
    private final Pane scrollDownBackground;
    private Optional<ScrollBar> scrollBar = Optional.empty();
    private Subscription hasUnreadMessagesPin, showScrolledDownButtonPin;
    private Timeline fadeInScrollDownBadgeTimeline;

    public ChatMessagesListView(ChatMessagesListModel model, ChatMessagesListController controller) {
        super(new CustomStackPane(), model, controller);

        listView = new ListView<>(model.getSortedChatMessages());
        listView.getStyleClass().add("chat-messages-list-view");

        VBox placeholder = ChatUtil.createEmptyChatPlaceholder(placeholderTitle, placeholderDescription);
        listView.setPlaceholder(placeholder);

        listView.setCellFactory(new ChatMessageListCellFactory(controller, model));

        // https://stackoverflow.com/questions/20621752/javafx-make-listview-not-selectable-via-mouse
        listView.setSelectionModel(new NoSelectionModel<>());
        VBox.setVgrow(listView, Priority.ALWAYS);

        scrollDownBackground = new Pane();
        scrollDownBackground.getStyleClass().add("scroll-down-bg");

        scrollDownImageView = new ImageView();
        scrollDownBadge = new Badge(scrollDownImageView);
        scrollDownBadge.setMaxSize(25, 25);
        scrollDownBadge.getStyleClass().add("chat-messages-badge");
        scrollDownBadge.setPosition(Pos.BOTTOM_RIGHT);
        scrollDownBadge.setBadgeInsets(new Insets(20, 10, -2, 55));
        scrollDownBadge.setCursor(Cursor.HAND);

        scrollDownTooltip = new BisqTooltip(Res.get("chat.listView.scrollDown"));
        Tooltip.install(scrollDownBadge, scrollDownTooltip);

        StackPane.setAlignment(scrollDownBadge, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(scrollDownBackground, Pos.BOTTOM_CENTER);
        StackPane.setMargin(scrollDownBackground, new Insets(0, 15, 0, 0));
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(listView, scrollDownBackground, scrollDownBadge);
    }

    public void scrollToChatMessage(ChatMessageListItem<?,?> chatMessageListItem) {
        listView.scrollTo(chatMessageListItem);
    }

    @Override
    protected void onViewAttached() {
        ListViewUtil.findScrollbarAsync(listView, Orientation.VERTICAL, 1000).whenComplete((scrollBar, throwable) -> {
            if (throwable != null) {
                log.error("Find scrollbar failed", throwable);
                return;
            }
            this.scrollBar = scrollBar;
            if (scrollBar.isPresent()) {
                scrollBar.get().valueProperty().bindBidirectional(model.getScrollValue());
                model.getScrollBarVisible().bind(scrollBar.get().visibleProperty());
                controller.onScrollToBottom();
            } else {
                log.error("scrollBar is empty");
            }
        });

        scrollDownBackground.visibleProperty().bind(model.getShowScrolledDownButton());
        scrollDownBackground.managedProperty().bind(model.getShowScrolledDownButton());

        scrollDownBadge.textProperty().bind(model.getNumUnReadMessages());

        scrollDownBadge.setOpacity(0);
        showScrolledDownButtonPin = EasyBind.subscribe(model.getShowScrolledDownButton(), showScrolledDownButton -> {
            if (showScrolledDownButton == null) {
                return;
            }
            if (fadeInScrollDownBadgeTimeline != null) {
                fadeInScrollDownBadgeTimeline.stop();
            }
            if (showScrolledDownButton) {
                fadeInScrollDownBadge();
            } else {
                scrollDownBadge.setOpacity(0);
            }
        });
        hasUnreadMessagesPin = EasyBind.subscribe(model.getHasUnreadMessages(), hasUnreadMessages -> {
            if (hasUnreadMessages) {
                scrollDownImageView.setOpacity(1);
                scrollDownImageView.setId("scroll-down-green");
                scrollDownTooltip.setText(Res.get("chat.listView.scrollDown.newMessages"));
            } else {
                scrollDownImageView.setOpacity(0.5);
                scrollDownImageView.setId("scroll-down-white");
                scrollDownTooltip.setText(Res.get("chat.listView.scrollDown"));
            }
        });
        model.getLayoutChildrenDone().bind(root.getLayoutChildrenDone());

        scrollDownBadge.setOnMouseClicked(e -> controller.onScrollToBottom());

        if (ChatUtil.isCommonChat(model.getChatChannelDomain()) && model.getIsPublicChannel().get()) {
            placeholderTitle.setText(Res.get("chat.messagebox.noChats.placeholder.title"));
            placeholderDescription.setText(Res.get("chat.messagebox.noChats.placeholder.description",
                    model.getSelectedChannel().get().getDisplayString()));
        } else {
            placeholderTitle.setText("");
            placeholderDescription.setText("");
        }
    }

    @Override
    protected void onViewDetached() {
        scrollBar.ifPresent(scrollbar -> scrollbar.valueProperty().unbindBidirectional(model.getScrollValue()));
        model.getScrollBarVisible().unbind();
        scrollDownBackground.visibleProperty().unbind();
        scrollDownBackground.managedProperty().unbind();
        scrollDownBadge.textProperty().unbind();
        model.getLayoutChildrenDone().unbind();
        hasUnreadMessagesPin.unsubscribe();
        showScrolledDownButtonPin.unsubscribe();

        scrollDownBadge.setOnMouseClicked(null);
        if (fadeInScrollDownBadgeTimeline != null) {
            fadeInScrollDownBadgeTimeline.stop();
            fadeInScrollDownBadgeTimeline = null;
            scrollDownBadge.setOpacity(0);
        }
    }

    private void fadeInScrollDownBadge() {
        if (!Transitions.getUseAnimations()) {
            scrollDownBadge.setOpacity(1);
            return;
        }

        fadeInScrollDownBadgeTimeline = new Timeline();
        scrollDownBadge.setOpacity(0);
        ObservableList<KeyFrame> keyFrames = fadeInScrollDownBadgeTimeline.getKeyFrames();
        keyFrames.add(new KeyFrame(Duration.millis(0),
                new KeyValue(scrollDownBadge.opacityProperty(), 0, Interpolator.LINEAR)
        ));
        // Add a delay before starting fade-in to deal with a render delay when adding a
        // list item.
        keyFrames.add(new KeyFrame(Duration.millis(100),
                new KeyValue(scrollDownBadge.opacityProperty(), 0, Interpolator.EASE_OUT)
        ));
        keyFrames.add(new KeyFrame(Duration.millis(400),
                new KeyValue(scrollDownBadge.opacityProperty(), 1, Interpolator.EASE_OUT)
        ));
        fadeInScrollDownBadgeTimeline.play();
    }
}

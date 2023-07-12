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

package bisq.desktop.components.overlay;

import bisq.common.locale.LanguageRepository;
import bisq.common.util.OsUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.common.Browser;
import bisq.desktop.common.Icons;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.components.containers.BisqGridPane;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BusyAnimation;
import bisq.i18n.Res;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import com.google.common.reflect.TypeToken;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public abstract class Overlay<T extends Overlay<T>> {
    protected final static double DEFAULT_WIDTH = 668;

    public static Region primaryStageOwner;
    private static String baseDir;
    public static SettingsService settingsService;
    private static Runnable shutdownHandler;

    public static void init(Region primaryStageOwner,
                            String baseDir,
                            SettingsService settingsService,
                            Runnable shutdownHandler) {
        Overlay.primaryStageOwner = primaryStageOwner;
        Overlay.baseDir = baseDir;
        Overlay.settingsService = settingsService;
        Overlay.shutdownHandler = shutdownHandler;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Enum
    ///////////////////////////////////////////////////////////////////////////////////////////

    public enum AnimationType {
        FadeInAtCenter,
        SlideDownFromCenterTop,
        SlideFromRightTop, // This is used for Notification which is not handled in that class directly but in Notification directly
        ScaleDownToCenter,
        ScaleFromCenter,
        ScaleYFromCenter
    }

    public enum Type {
        UNDEFINED(AnimationType.ScaleFromCenter),

        NOTIFICATION(AnimationType.SlideFromRightTop),

        BACKGROUND_INFO(AnimationType.SlideDownFromCenterTop),
        FEEDBACK(AnimationType.SlideDownFromCenterTop),

        INFORMATION(AnimationType.FadeInAtCenter),
        INSTRUCTION(AnimationType.ScaleFromCenter),
        ATTENTION(AnimationType.ScaleFromCenter),
        CONFIRMATION(AnimationType.ScaleYFromCenter),

        WARNING(AnimationType.ScaleDownToCenter),
        ERROR(AnimationType.ScaleDownToCenter);

        public final AnimationType animationType;
        private final Transitions.Type transitionsType;

        Type(AnimationType animationType) {
            this(animationType, Transitions.Type.MEDIUM_BLUR_STRONG);
        }

        Type(AnimationType animationType, Transitions.Type transitionsType) {
            this.animationType = animationType;
            this.transitionsType = transitionsType;
        }
    }

    private Region owner;
    protected Stage stage;
    @Getter
    protected BisqGridPane gridPane;

    protected double width = DEFAULT_WIDTH;
    protected double buttonDistance = 20;

    protected boolean showReportErrorButtons;
    private boolean showBusyAnimation;
    protected boolean hideCloseButton;
    protected boolean isDisplayed;
    protected boolean disableActionButton;
    protected boolean useBgEffect = true;
    @Getter
    protected BooleanProperty isHiddenProperty = new SimpleBooleanProperty();

    // Used when a priority queue is used for displaying order of popups. Higher numbers mean lower priority
    @Setter
    @Getter
    protected Integer displayOrderPriority = Integer.MAX_VALUE;

    protected boolean useAnimation = true;

    protected Label headlineIcon, headLineLabel, messageLabel;
    protected String headLine, message, closeButtonText, actionButtonText,
            secondaryActionButtonText, dontShowAgainId, dontShowAgainText,
            truncatedMessage;
    private List<String> messageHyperlinks;
    private String headlineStyle;
    protected Button actionButton, secondaryActionButton;
    private HBox buttonBox;
    protected Button closeButton;
    private Region content;

    private HPos buttonAlignment = HPos.RIGHT;

    protected Optional<Runnable> closeHandlerOptional = Optional.empty();
    protected Optional<Runnable> actionHandlerOptional = Optional.empty();
    protected boolean doCloseOnAction = true;
    protected Optional<Runnable> secondaryActionHandlerOptional = Optional.empty();
    protected boolean doCloseOnSecondaryAction = true;
    protected ChangeListener<Number> positionListener;

    protected UIScheduler centerTime;
    protected Type type = Type.UNDEFINED;
    protected AnimationType animationType;
    protected Transitions.Type transitionsType;
    protected int maxChar = 2200;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public Overlay() {
        TypeToken<T> typeToken = new TypeToken<>(getClass()) {
        };
        if (!typeToken.isSupertypeOf(getClass())) {
            throw new RuntimeException("Subclass of Overlay<T> should be castable to T");
        }
        owner = primaryStageOwner;
    }

    public void show(boolean showAgainChecked) {
        if (dontShowAgainId == null || DontShowAgainService.showAgain(dontShowAgainId)) {
            createGridPane();
            if (LanguageRepository.isDefaultLanguageRTL()) {
                getRootContainer().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
            }

            addHeadLine();

            if (showBusyAnimation) {
                addBusyAnimation();
            }

            addContent();
            if (showReportErrorButtons) {
                addReportErrorButtons();
            }

            addButtons();
            addDontShowAgainCheckBox(showAgainChecked);
            applyStyles();
            onShow();
        }
    }

    public void show() {
        this.show(false);
    }

    protected void onShow() {
    }

    public void hide() {
        if (gridPane != null) {
            animateHide();
        }
        isDisplayed = false;
        isHiddenProperty.set(true);
    }

    protected void animateHide() {
        animateHide(() -> {
            if (useBgEffect) {
                removeEffectFromBackground();
            }

            if (stage != null) {
                stage.hide();
            }

            cleanup();
            onHidden();
        });
    }

    protected void onHidden() {
    }

    protected void cleanup() {
        if (centerTime != null) {
            centerTime.stop();
        }

        Scene rootScene = owner.getScene();
        if (rootScene != null) {
            Window window = rootScene.getWindow();
            if (window != null && positionListener != null) {
                window.xProperty().removeListener(positionListener);
                window.yProperty().removeListener(positionListener);
                window.widthProperty().removeListener(positionListener);
            }
        }
    }

    public T onClose(Runnable closeHandler) {
        this.closeHandlerOptional = Optional.of(closeHandler);
        return cast();
    }

    public T onAction(Runnable actionHandler) {
        this.actionHandlerOptional = Optional.of(actionHandler);
        return cast();
    }

    public T doCloseOnAction(boolean doCloseOnAction) {
        this.doCloseOnAction = doCloseOnAction;
        return cast();
    }

    public T doCloseOnSecondaryAction(boolean doCloseOnSecondaryAction) {
        this.doCloseOnSecondaryAction = doCloseOnSecondaryAction;
        return cast();
    }

    public T onSecondaryAction(Runnable secondaryActionHandlerOptional) {
        this.secondaryActionHandlerOptional = Optional.of(secondaryActionHandlerOptional);
        return cast();
    }

    public T headLine(String headLine) {
        this.headLine = headLine;
        return cast();
    }

    public T instruction(String message) {
        type = Type.INSTRUCTION;
        if (headLine == null)
            this.headLine = Res.get("popup.headline.instruction");
        processMessage(message);
        return cast();
    }

    public T attention(String message) {
        type = Type.ATTENTION;
        if (headLine == null)
            this.headLine = Res.get("popup.headline.attention");
        processMessage(message);
        return cast();
    }

    public T backgroundInfo(String message) {
        type = Type.BACKGROUND_INFO;
        if (headLine == null)
            this.headLine = Res.get("popup.headline.backgroundInfo");
        processMessage(message);
        return cast();
    }

    public T feedback(String message) {
        type = Type.FEEDBACK;
        if (headLine == null)
            this.headLine = Res.get("popup.headline.feedback");
        processMessage(message);
        return cast();
    }

    public T animationType(AnimationType animationType) {
        this.animationType = animationType;
        return cast();
    }

    public T transitionsType(Transitions.Type transitionsType) {
        this.transitionsType = transitionsType;
        return cast();
    }

    public T owner(Region owner) {
        this.owner = owner;
        return cast();
    }

    public T useBgEffect(boolean useBgEffect) {
        this.useBgEffect = useBgEffect;
        return cast();
    }

    public T confirmation(String message) {
        type = Type.CONFIRMATION;
        if (headLine == null)
            this.headLine = Res.get("popup.headline.confirmation");
        processMessage(message);
        return cast();
    }

    public T information(String message) {
        type = Type.INFORMATION;
        if (headLine == null)
            this.headLine = Res.get("popup.headline.information");
        processMessage(message);
        return cast();
    }

    public T warning(String message) {
        type = Type.WARNING;

        if (headLine == null)
            this.headLine = Res.get("popup.headline.warning");
        processMessage(message);
        return cast();
    }

    public T error(Throwable throwable) {
        if (throwable.getMessage() != null) {
            return error(throwable.getMessage());
        } else {
            return error(throwable.toString());
        }
    }

    private T error(String message) {
        type = Type.ERROR;
        showReportErrorButtons();
        width = 1100;
        if (headLine == null)
            this.headLine = Res.get("popup.headline.error");
        processMessage(message);
        return cast();
    }

    @SuppressWarnings("UnusedReturnValue")
    public T showReportErrorButtons() {
        this.showReportErrorButtons = true;
        return cast();
    }

    public T message(String message) {
        processMessage(message);
        return cast();
    }

    public T content(Region content) {
        this.content = content;
        return cast();
    }

    public T closeButtonText(String closeButtonText) {
        this.closeButtonText = closeButtonText;
        return cast();
    }

    public T useReportBugButton() {
        this.closeButtonText = Res.get("popup.reportBug");
        this.closeHandlerOptional = Optional.of(() -> Browser.open("https://bisq.network/source/bisq/issues"));
        return cast();
    }

    public T useIUnderstandButton() {
        this.closeButtonText = Res.get("action.iUnderstand");
        return cast();
    }

    public T actionButtonTextWithGoTo(String target) {
        this.actionButtonText = Res.get("action.goTo", Res.get(target));
        return cast();
    }

    public T secondaryActionButtonTextWithGoTo(String target) {
        this.secondaryActionButtonText = Res.get("action.goTo", Res.get(target));
        return cast();
    }

    public T closeButtonTextWithGoTo(String target) {
        this.closeButtonText = Res.get("action.goTo", Res.get(target));
        return cast();
    }

    public T actionButtonText(String actionButtonText) {
        this.actionButtonText = actionButtonText;
        return cast();
    }

    public T secondaryActionButtonText(String secondaryActionButtonText) {
        this.secondaryActionButtonText = secondaryActionButtonText;
        return cast();
    }

    public T useShutDownButton() {
        this.actionButtonText = Res.get("action.shutDown");
        this.actionHandlerOptional = Optional.ofNullable(shutdownHandler);
        return cast();
    }

    public T buttonAlignment(HPos pos) {
        this.buttonAlignment = pos;
        return cast();
    }

    public T width(double width) {
        this.width = width;
        return cast();
    }

    public T maxMessageLength(int maxChar) {
        this.maxChar = maxChar;
        return cast();
    }

    public T showBusyAnimation() {
        this.showBusyAnimation = true;
        return cast();
    }

    public T dontShowAgainId(String key) {
        this.dontShowAgainId = key;
        return cast();
    }

    public T dontShowAgainText(String dontShowAgainText) {
        this.dontShowAgainText = dontShowAgainText;
        return cast();
    }

    public T hideCloseButton() {
        this.hideCloseButton = true;
        return cast();
    }

    public T useAnimation(boolean useAnimation) {
        this.useAnimation = useAnimation;
        return cast();
    }

    public T setHeadlineStyle(String headlineStyle) {
        this.headlineStyle = headlineStyle;
        return cast();
    }

    public T disableActionButton() {
        this.disableActionButton = true;
        return cast();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Protected
    ///////////////////////////////////////////////////////////////////////////////////////////

    protected void createGridPane() {
        gridPane = new BisqGridPane();
        gridPane.setPadding(new Insets(64));
        gridPane.setPrefWidth(width);

        ColumnConstraints columnConstraints1 = new ColumnConstraints();
        columnConstraints1.setHalignment(HPos.RIGHT);
        columnConstraints1.setHgrow(Priority.SOMETIMES);
        ColumnConstraints columnConstraints2 = new ColumnConstraints();
        columnConstraints2.setHgrow(Priority.ALWAYS);
        gridPane.getColumnConstraints().addAll(columnConstraints1, columnConstraints2);
    }

    protected void blurAgain() {
        UIScheduler.run(() -> getTransitionsType().apply(owner)).after(Transitions.DEFAULT_DURATION);
    }

    public void display() {
        Scene rootScene = owner.getScene();
        if (rootScene != null) {
            Scene scene = new Scene(getRootContainer());
            scene.getStylesheets().setAll(rootScene.getStylesheets());
            scene.setFill(Color.TRANSPARENT);

            setupKeyHandler(scene);

            stage = new Stage();
            stage.setScene(scene);
            Window window = rootScene.getWindow();
            setModality();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setOnCloseRequest(event -> {
                event.consume();
                doClose();
            });
            stage.sizeToScene();
            stage.show();

            layout();

            if (useBgEffect) {
                addEffectToBackground();
            }

            // On Linux the owner stage does not move the child stage as it does on Mac
            // So we need to apply centerPopup. Further, with fast movements the handler loses
            // the latest position, with a delay it fixes that.
            // Also, on Mac sometimes the popups are positioned outside the main app, so keep it for all OS
            positionListener = (observable, oldValue, newValue) -> {
                if (stage != null) {
                    layout();

                    if (centerTime != null)
                        centerTime.stop();

                    centerTime = UIScheduler.run(this::layout).after(3000);
                }
            };
            window.xProperty().addListener(positionListener);
            window.yProperty().addListener(positionListener);
            window.widthProperty().addListener(positionListener);

            animateDisplay();
            isDisplayed = true;
        }
    }

    protected Region getRootContainer() {
        return gridPane;
    }

    protected void setupKeyHandler(Scene scene) {
        if (!hideCloseButton) {
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.ENTER) {
                    e.consume();
                    doClose();
                }
            });
        }
    }

    protected void animateDisplay() {
        Region rootContainer = this.getRootContainer();

        rootContainer.setOpacity(0);
        Interpolator interpolator = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
        double duration = getDuration(400);
        Timeline timeline = new Timeline();
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();

        AnimationType animationType = getAnimationType();
        if (animationType == AnimationType.SlideDownFromCenterTop) {
            double startY = -rootContainer.getHeight();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.translateYProperty(), startY, interpolator)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.translateYProperty(), -50, interpolator)
            ));
        } else if (animationType == AnimationType.ScaleFromCenter) {
            double startScale = 0.25;
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), startScale, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), startScale, interpolator)

            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), 1, interpolator)
            ));
        } else if (animationType == AnimationType.ScaleYFromCenter) {
            double startYScale = 0.25;
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), startYScale, interpolator)

            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), 1, interpolator)
            ));
        } else if (animationType == AnimationType.ScaleDownToCenter) {
            double startScale = 1.1;
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), startScale, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), startScale, interpolator)

            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), 1, interpolator)
            ));
        } else if (animationType == AnimationType.FadeInAtCenter) {
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator)

            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator)
            ));
        }

        timeline.play();
    }

    protected void animateHide(Runnable onFinishedHandler) {
        Interpolator interpolator = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
        double duration = getDuration(200);
        Timeline timeline = new Timeline();
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();

        Region rootContainer = getRootContainer();
        AnimationType animationType = getAnimationType();
        if (animationType == AnimationType.SlideDownFromCenterTop) {
            double endY = -rootContainer.getHeight();
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.translateYProperty(), -10, interpolator)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.translateYProperty(), endY, interpolator)
            ));

            timeline.setOnFinished(e -> onFinishedHandler.run());
            timeline.play();
        } else if (animationType == AnimationType.ScaleFromCenter) {
            double endScale = 0.25;
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), 1, interpolator)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), endScale, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), endScale, interpolator)
            ));
        } else if (animationType == AnimationType.ScaleYFromCenter) {
            rootContainer.setRotationAxis(Rotate.X_AXIS);
            rootContainer.getScene().setCamera(new PerspectiveCamera());
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(rootContainer.rotateProperty(), 0, interpolator),
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(rootContainer.rotateProperty(), -90, interpolator),
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator)
            ));
        } else if (animationType == AnimationType.ScaleDownToCenter) {
            double endScale = 0.1;
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), 1, interpolator)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), endScale, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), endScale, interpolator)
            ));
        } else if (animationType == AnimationType.FadeInAtCenter) {
            keyFrames.add(new KeyFrame(Duration.millis(0),
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator)
            ));
            keyFrames.add(new KeyFrame(Duration.millis(duration),
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator)
            ));
        }

        timeline.setOnFinished(e -> onFinishedHandler.run());
        timeline.play();
    }

    protected void layout() {
        Scene rootScene = owner.getScene();
        if (rootScene != null) {
            Window window = rootScene.getWindow();
            double titleBarHeight = window.getHeight() - rootScene.getHeight();

            if (OsUtils.isWindows()) {
                titleBarHeight -= 9;
            }
            double leftDistance = (window.getWidth() - getRootContainer().getWidth()) / 2;
            stage.setX(Math.round(window.getX() + leftDistance));

            AnimationType animationType = getAnimationType();
            if (animationType == AnimationType.SlideDownFromCenterTop)
                stage.setY(Math.round(window.getY() + titleBarHeight));
            else
                stage.setY(Math.round(window.getY() + titleBarHeight + (owner.getHeight() - stage.getHeight()) / 2));
        }
    }

    protected AnimationType getAnimationType() {
        return this.animationType == null ? type.animationType : this.animationType;
    }

    protected Transitions.Type getTransitionsType() {
        return this.transitionsType == null ? type.transitionsType : this.transitionsType;
    }

    protected void addEffectToBackground() {
        getTransitionsType().apply(owner);
    }

    protected void applyStyles() {
        Region rootContainer = getRootContainer();
        rootContainer.getStyleClass().add("overlay-bg");

        if (headLineLabel != null) {
            headlineIcon.setManaged(true);
            headlineIcon.setVisible(true);
            switch (type) {
                case INFORMATION:
                case BACKGROUND_INFO:
                case INSTRUCTION:
                case CONFIRMATION:
                case FEEDBACK:
                case NOTIFICATION:
                case ATTENTION:
                    Icons.getIconForLabel(AwesomeIcon.INFO_SIGN, headlineIcon, "1.8em");
                    headLineLabel.getStyleClass().add("overlay-headline-information");
                    headlineIcon.getStyleClass().add("overlay-icon-information");
                    break;
                case WARNING:
                    Icons.getIconForLabel(AwesomeIcon.WARNING_SIGN, headlineIcon, "1.5em");
                    headLineLabel.getStyleClass().add("overlay-headline-warning");
                    headlineIcon.getStyleClass().add("overlay-icon-warning");
                    break;
                case ERROR:
                    Icons.getIconForLabel(AwesomeIcon.EXCLAMATION_SIGN, headlineIcon, "1.5em");
                    headLineLabel.getStyleClass().add("overlay-headline-error");
                    headlineIcon.getStyleClass().add("overlay-icon-error");
                    break;
                default:
                    headLineLabel.getStyleClass().add("overlay-headline");
            }
        }
    }

    protected void setModality() {
        stage.initOwner(owner.getScene().getWindow());
        stage.initModality(Modality.NONE);
    }

    protected void removeEffectFromBackground() {
        Transitions.removeEffect(owner);
    }

    protected void addHeadLine() {
        if (headLine != null) {
            HBox hBox = new HBox();
            hBox.setSpacing(7);
            headLineLabel = new Label(headLine);
            headlineIcon = new Label();
            headlineIcon.setManaged(false);
            headlineIcon.setVisible(false);
            headlineIcon.setAlignment(Pos.CENTER);
            headlineIcon.setPadding(new Insets(-2, 5, 0, 0));
            headLineLabel.setMouseTransparent(true);

            if (headlineStyle != null)
                headLineLabel.setStyle(headlineStyle);

            hBox.getChildren().addAll(headlineIcon, headLineLabel);
            hBox.setAlignment(Pos.CENTER_LEFT);

            GridPane.setHalignment(hBox, HPos.LEFT);
            GridPane.setRowIndex(hBox, gridPane.getRowCount());
            GridPane.setColumnSpan(hBox, 2);
            gridPane.getChildren().addAll(hBox);
        }
    }

    protected void addContent() {
        if (message != null) {
            messageLabel = new Label(truncatedMessage);
            messageLabel.setMinWidth(width);
            messageLabel.setWrapText(true);
            GridPane.setHalignment(messageLabel, HPos.LEFT);
            GridPane.setHgrow(messageLabel, Priority.ALWAYS);
            GridPane.setMargin(messageLabel, new Insets(3, 0, 0, 0));
            GridPane.setRowIndex(messageLabel, gridPane.getRowCount());
            GridPane.setColumnIndex(messageLabel, 0);
            GridPane.setColumnSpan(messageLabel, 2);
            gridPane.getChildren().add(messageLabel);
            addFooter();
        }
        if (content != null) {
            GridPane.setHalignment(content, HPos.LEFT);
            GridPane.setHgrow(content, Priority.ALWAYS);
            GridPane.setMargin(content, new Insets(3, 0, 0, 0));
            GridPane.setRowIndex(content, gridPane.getRowCount());
            GridPane.setColumnIndex(content, 0);
            GridPane.setColumnSpan(content, 2);
            gridPane.getChildren().add(content);
            addFooter();
        }
    }

    // footer contains optional hyperlinks extracted from the message
    protected void addFooter() {
        if (messageHyperlinks != null && messageHyperlinks.size() > 0) {
            VBox footerBox = new VBox();
            GridPane.setRowIndex(footerBox, gridPane.getRowCount());
            GridPane.setColumnSpan(footerBox, 2);
            GridPane.setMargin(footerBox, new Insets(buttonDistance, 0, 0, 0));
            gridPane.getChildren().add(footerBox);
            for (int i = 0; i < messageHyperlinks.size(); i++) {
                Label label = new Label(String.format("[%d]", i + 1));
                Hyperlink link = new Hyperlink(messageHyperlinks.get(i));
                link.setOnAction(event -> Browser.open(link.getText()));
                HBox hBox = new HBox(label, link);
                hBox.setAlignment(Pos.CENTER_LEFT);
                footerBox.getChildren().addAll(hBox);
            }
        }
    }

    private void addReportErrorButtons() {
        messageLabel.setText(Res.get("popup.reportError", truncatedMessage));

        Button logButton = new Button(Res.get("popup.reportError.log"));
        GridPane.setMargin(logButton, new Insets(20, 0, 0, 0));
        GridPane.setHalignment(logButton, HPos.LEFT);
        GridPane.setRowIndex(logButton, gridPane.getRowCount());
        gridPane.getChildren().add(logButton);
        logButton.setOnAction(event -> OsUtils.open(new File(baseDir, "bisq.log")));

        Button gitHubButton = new Button(Res.get("popup.reportError.gitHub"));
        GridPane.setHalignment(gitHubButton, HPos.RIGHT);
        GridPane.setRowIndex(gitHubButton, gridPane.getRowCount());
        gridPane.getChildren().add(gitHubButton);
        gitHubButton.setOnAction(event -> {
            if (message != null) {
                ClipboardUtil.copyToClipboard(message);
            }
            Browser.open("https://github.com/bisq-network/bisq2/issues");
            hide();
        });
    }

    protected void addBusyAnimation() {
        BusyAnimation busyAnimation = new BusyAnimation();
        GridPane.setHalignment(busyAnimation, HPos.CENTER);
        GridPane.setRowIndex(busyAnimation, gridPane.getRowCount());
        GridPane.setColumnSpan(busyAnimation, 2);
        gridPane.getChildren().add(busyAnimation);
    }

    protected void addDontShowAgainCheckBox(boolean isChecked) {
        if (dontShowAgainId != null) {
            // We might have set it and overridden the default, so we check if it is not set
            if (dontShowAgainText == null) {
                dontShowAgainText = Res.get("action.dontShowAgain");
            }

            CheckBox dontShowAgainCheckBox = new CheckBox(dontShowAgainText);
            HBox.setHgrow(dontShowAgainCheckBox, Priority.NEVER);
            buttonBox.getChildren().add(0, dontShowAgainCheckBox);

            dontShowAgainCheckBox.setSelected(isChecked);
            DontShowAgainService.putDontShowAgain(dontShowAgainId, isChecked);
            dontShowAgainCheckBox.setOnAction(e -> DontShowAgainService.putDontShowAgain(dontShowAgainId, dontShowAgainCheckBox.isSelected()));
        }
    }

    protected void addDontShowAgainCheckBox() {
        this.addDontShowAgainCheckBox(false);
    }

    protected void addButtons() {
        if (!hideCloseButton) {
            closeButton = new Button(closeButtonText == null ? Res.get("action.close") : closeButtonText);
            closeButton.setOnAction(event -> doClose());
            closeButton.setMinWidth(70);
            HBox.setHgrow(closeButton, Priority.SOMETIMES);
        }

        buttonBox = new HBox(10);
        GridPane.setHalignment(buttonBox, buttonAlignment);
        GridPane.setMargin(buttonBox, new Insets(buttonDistance, 0, 0, 0));
        gridPane.add(buttonBox, 0, gridPane.getRowCount(), 2, 1);

        if (actionHandlerOptional.isPresent() || actionButtonText != null) {
            actionButton = new Button(actionButtonText == null ? Res.get("confirmation.ok") : actionButtonText);

            if (!disableActionButton)
                actionButton.setDefaultButton(true);
            else
                actionButton.setDisable(true);

            HBox.setHgrow(actionButton, Priority.SOMETIMES);
            actionButton.setDefaultButton(true);

            if (!disableActionButton) {
                actionButton.setOnAction(event -> {
                    if (doCloseOnAction) {
                        hide();
                    }
                    actionHandlerOptional.ifPresent(Runnable::run);
                });
            }

            if (secondaryActionButtonText != null && secondaryActionHandlerOptional.isPresent()) {
                secondaryActionButton = new Button(secondaryActionButtonText);
                secondaryActionButton.setOnAction(event -> {
                    if (doCloseOnSecondaryAction) {
                        hide();
                    }
                    secondaryActionHandlerOptional.ifPresent(Runnable::run);
                });
            }

        } else if (!hideCloseButton) {
            closeButton.setDefaultButton(true);
        }

        if (buttonAlignment == HPos.RIGHT) {
            buttonBox.getChildren().add(Spacer.fillHBox());
        }

        if (!hideCloseButton && closeButton != null) {
            buttonBox.getChildren().add(closeButton);
        }
        if (secondaryActionButton != null) {
            buttonBox.getChildren().add(secondaryActionButton);
        }
        if (actionButton != null) {
            buttonBox.getChildren().add(actionButton);
        }
    }

    protected void doClose() {
        hide();
        closeHandlerOptional.ifPresent(Runnable::run);
    }

    protected void setTruncatedMessage() {
        if (message != null && message.length() > maxChar) {
            truncatedMessage = StringUtils.truncate(message, maxChar);
        } else {
            truncatedMessage = Objects.requireNonNullElse(message, "");
        }
    }

    private void processMessage(String message) {
        if (messageHyperlinks == null) {
            messageHyperlinks = new ArrayList<>();
        }
        this.message = StringUtils.extractHyperlinks(message, messageHyperlinks);
        setTruncatedMessage();
    }

    protected double getDuration(double duration) {
        return useAnimation && settingsService.getUseAnimations().get() ? duration : 1;
    }

    public boolean isDisplayed() {
        return isDisplayed;
    }

    private T cast() {
        //noinspection unchecked
        return (T) this;
    }

    @Override
    public String toString() {
        return "Popup{" +
                "headLine='" + headLine + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}

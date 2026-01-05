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

import bisq.application.ShutDownHandler;
import bisq.common.application.ApplicationVersion;
import bisq.common.facades.FacadeProvider;
import bisq.common.file.FileMutatorUtils;
import bisq.common.file.FileReaderUtils;
import bisq.common.locale.LanguageRepository;
import bisq.common.platform.OS;
import bisq.common.platform.Platform;
import bisq.common.platform.PlatformUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.Icons;
import bisq.desktop.common.ManagedDuration;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.components.containers.BisqGridPane;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.BusyAnimation;
import bisq.i18n.Res;
import bisq.settings.DontShowAgainKey;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import com.google.common.base.Throwables;
import com.google.common.reflect.TypeToken;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
public abstract class Overlay<T extends Overlay<T>> {
    protected final static double DEFAULT_WIDTH = 668;

    public static Region primaryStageOwner;
    private static Path appDataDirPath;
    public static SettingsService settingsService;
    private static ShutDownHandler shutdownHandler;
    private static DontShowAgainService dontShowAgainService;

    public static void init(ServiceProvider serviceProvider, Region primaryStageOwner) {
        Overlay.primaryStageOwner = primaryStageOwner;
        Overlay.appDataDirPath = serviceProvider.getConfig().getAppDataDirPath();
        Overlay.settingsService = serviceProvider.getSettingsService();
        Overlay.shutdownHandler = serviceProvider.getShutDownHandler();
        Overlay.dontShowAgainService = serviceProvider.getDontShowAgainService();
    }


    /* --------------------------------------------------------------------- */
    // Enum
    /* --------------------------------------------------------------------- */

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

        // NOTIFICATION(AnimationType.SlideFromRightTop),
        // TODO https://github.com/bisq-network/bisq2/issues/1883
        NOTIFICATION(AnimationType.SlideDownFromCenterTop, Transitions.Type.LIGHT_BLUR_LIGHT),

        BACKGROUND_INFO(AnimationType.SlideDownFromCenterTop),
        FEEDBACK(AnimationType.SlideDownFromCenterTop),

        INFORMATION(AnimationType.FadeInAtCenter),
        INSTRUCTION(AnimationType.ScaleFromCenter),
        ATTENTION(AnimationType.ScaleFromCenter),
        CONFIRMATION(AnimationType.ScaleYFromCenter),

        WARNING(AnimationType.ScaleDownToCenter),
        INVALID(AnimationType.SlideDownFromCenterTop, Transitions.Type.LIGHT_BLUR_LIGHT),
        FAILURE(AnimationType.ScaleDownToCenter),
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

    @EqualsAndHashCode.Include
    private final String id;

    private Region owner;
    protected Stage stage;
    @Getter
    protected BisqGridPane gridPane;

    protected double width = DEFAULT_WIDTH;
    protected final double buttonDistance = 20;

    protected boolean showReportErrorButtons;
    private boolean showBusyAnimation;
    protected boolean hideCloseButton;
    protected boolean isDisplayed;
    protected boolean useBgEffect = true;
    @Getter
    protected final BooleanProperty isHiddenProperty = new SimpleBooleanProperty();

    protected Label headlineIcon, headlineLabel, messageLabel;
    protected String headline, message;
    protected String closeButtonText, actionButtonText,
            secondaryActionButtonText, dontShowAgainId, dontShowAgainText,
            truncatedMessage;
    private List<String> messageHyperlinks;
    private String headlineStyle;
    protected Button actionButton, secondaryActionButton;
    private HBox buttonBox;
    protected Button closeButton;
    protected Region content;

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
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Scene> sceneListener = (observable, oldValue, newValue) -> {
        if (oldValue != null && newValue == null) {
            hide();
        }
    };

    public Overlay() {
        id = StringUtils.createUid();
        TypeToken<T> typeToken = new TypeToken<>(getClass()) {
        };
        if (!typeToken.isSupertypeOf(getClass())) {
            throw new RuntimeException("Subclass of Overlay<T> should be castable to T");
        }
        owner = primaryStageOwner;
    }


    /* --------------------------------------------------------------------- */
    // Public API
    /* --------------------------------------------------------------------- */

    public void show(boolean showAgainChecked) {
        if (dontShowAgainId == null || dontShowAgainService.showAgain(dontShowAgainId)) {
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

        if (stage != null && stage.getScene() != null) {
            stage.getScene().setOnKeyPressed(null);
        }
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

    public T headline(String headline) {
        this.headline = headline;
        return cast();
    }

    public T instruction(String message) {
        type = Type.INSTRUCTION;
        if (headline == null)
            this.headline = Res.get("popup.headline.instruction");
        processMessage(message);
        return cast();
    }

    public T attention(String message) {
        type = Type.ATTENTION;
        if (headline == null)
            this.headline = Res.get("popup.headline.attention");
        processMessage(message);
        return cast();
    }

    public T backgroundInfo(String message) {
        type = Type.BACKGROUND_INFO;
        if (headline == null)
            this.headline = Res.get("popup.headline.backgroundInfo");
        processMessage(message);
        return cast();
    }

    public T notify(String message) {
        type = Type.NOTIFICATION;
        processMessage(message);
        return cast();
    }

    public T feedback(String message) {
        type = Type.FEEDBACK;
        if (headline == null)
            this.headline = Res.get("popup.headline.feedback");
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

    public T type(Type type) {
        this.type = type;
        return cast();
    }

    public T confirmation(String message) {
        type = Type.CONFIRMATION;
        if (headline == null)
            this.headline = Res.get("popup.headline.confirmation");
        processMessage(message);
        return cast();
    }

    public T information(String message) {
        type = Type.INFORMATION;
        if (headline == null)
            this.headline = Res.get("popup.headline.information");
        processMessage(message);
        return cast();
    }

    public T warning(String message) {
        type = Type.WARNING;

        if (headline == null)
            this.headline = Res.get("popup.headline.warning");
        processMessage(message);
        return cast();
    }

    public T failure(String header, String errorMessage, String footer) {
        type = Type.FAILURE;
        width = 800;
        if (headline == null) {
            this.headline = Res.get("popup.headline.failure");
        }

        processMessage(header);

        Label footerLabel = new Label(footer);
        footerLabel.getStyleClass().add("overlay-message");
        footerLabel.setWrapText(true);
        footerLabel.setMinWidth(width);

        TextArea textArea = getFailureTextArea(errorMessage, width);
        VBox.setMargin(textArea, new Insets(-15, 0, 5, 0));
        VBox vBox = new VBox(10, textArea, footerLabel);
        content(vBox);

        return cast();
    }

    public T invalid(String message) {
        type = Type.INVALID;

        if (headline == null)
            this.headline = Res.get("popup.headline.invalid");
        processMessage(message);
        return cast();
    }

    public T error(Throwable throwable) {
        return error(Throwables.getStackTraceAsString(throwable));
    }

    public T error(String message) {
        type = Type.ERROR;
        showReportErrorButtons();
        width = 800;
        if (headline == null) {
            this.headline = Res.get("popup.reportBug");
        }

        processMessage(Res.get("popup.reportError"));

        String version = Res.get("version.versionAndCommitHash", ApplicationVersion.getVersion().getVersionAsString(), ApplicationVersion.getBuildCommitShortHash());
        String platformDetails = Platform.getDetails();
        String metaData = Res.get("popup.reportBug.metaData", version, platformDetails);
        TextArea metaDataTextArea = getErrorTextArea(metaData, width);
        metaDataTextArea.setMaxHeight(50);

        String errorReport = Res.get("popup.reportBug.message", message);
        TextArea errorReportTextArea = getErrorTextArea(errorReport, width);

        VBox.setVgrow(metaDataTextArea, Priority.NEVER);
        VBox.setVgrow(errorReportTextArea, Priority.ALWAYS);
        VBox errorReportVBox = new VBox(10, metaDataTextArea, errorReportTextArea);
        content(errorReportVBox);

        return cast();
    }

    private static TextArea getErrorTextArea(String text, double width) {
        TextArea textArea = new TextArea(text);
        textArea.setContextMenu(new ContextMenu());
        textArea.setEditable(false);
        textArea.setPrefWidth(width);
        textArea.setWrapText(true);
        textArea.getStyleClass().addAll("code-block", "error-log");
        return textArea;
    }

    private static TextArea getFailureTextArea(String text, double width) {
        TextArea textArea = new TextArea(text);
        textArea.setPadding(new Insets(5));
        textArea.setMaxHeight(140);
        textArea.setContextMenu(new ContextMenu());
        textArea.setEditable(false);
        textArea.setPrefWidth(width);
        textArea.setWrapText(true);
        textArea.getStyleClass().addAll("code-block", "error-log");
        return textArea;
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
        this.closeHandlerOptional = Optional.of(() -> Browser.open("https://bisq.network/source/bisq2/issues"));
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
        this.actionHandlerOptional = Optional.of(() -> shutdownHandler.shutdown());
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

    public T dontShowAgainId(DontShowAgainKey key) {
        this.dontShowAgainId = key.getKey();
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

    public T setHeadlineStyle(String headlineStyle) {
        this.headlineStyle = headlineStyle;
        return cast();
    }


    /* --------------------------------------------------------------------- */
    // Protected
    /* --------------------------------------------------------------------- */

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
        UIScheduler.run(() -> getTransitionsType().apply(owner)).after(ManagedDuration.getDefaultDurationMillis());
    }

    public void display() {
        // Once our owner gets removed we also want to remove our overlay
        owner.sceneProperty().addListener(new WeakChangeListener<>(sceneListener));

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
        Duration duration = ManagedDuration.millis(400);
        Timeline timeline = new Timeline();
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();

        AnimationType animationType = getAnimationType();
        if (animationType == AnimationType.SlideDownFromCenterTop) {
            double startY = -rootContainer.getHeight();
            keyFrames.add(new KeyFrame(ManagedDuration.ZERO,
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.translateYProperty(), startY, interpolator)
            ));
            keyFrames.add(new KeyFrame(duration,
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.translateYProperty(), -50, interpolator)
            ));
        } else if (animationType == AnimationType.ScaleFromCenter) {
            double startScale = 0.25;
            keyFrames.add(new KeyFrame(ManagedDuration.ZERO,
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), startScale, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), startScale, interpolator)

            ));
            keyFrames.add(new KeyFrame(duration,
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), 1, interpolator)
            ));
        } else if (animationType == AnimationType.ScaleYFromCenter) {
            double startYScale = 0.25;
            keyFrames.add(new KeyFrame(ManagedDuration.ZERO,
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), startYScale, interpolator)

            ));
            keyFrames.add(new KeyFrame(duration,
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), 1, interpolator)
            ));
        } else if (animationType == AnimationType.ScaleDownToCenter) {
            double startScale = 1.1;
            keyFrames.add(new KeyFrame(ManagedDuration.ZERO,
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), startScale, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), startScale, interpolator)

            ));
            keyFrames.add(new KeyFrame(duration,
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), 1, interpolator)
            ));
        } else if (animationType == AnimationType.FadeInAtCenter) {
            keyFrames.add(new KeyFrame(ManagedDuration.ZERO,
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator)

            ));
            keyFrames.add(new KeyFrame(duration,
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator)
            ));
        }

        timeline.play();
    }

    protected void animateHide(Runnable onFinishedHandler) {
        Interpolator interpolator = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
        Duration duration = ManagedDuration.millis(200);
        Timeline timeline = new Timeline();
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();

        Region rootContainer = getRootContainer();
        AnimationType animationType = getAnimationType();
        if (animationType == AnimationType.SlideDownFromCenterTop) {
            double endY = -rootContainer.getHeight();
            keyFrames.add(new KeyFrame(ManagedDuration.ZERO,
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.translateYProperty(), -10, interpolator)
            ));
            keyFrames.add(new KeyFrame(duration,
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.translateYProperty(), endY, interpolator)
            ));

            timeline.setOnFinished(e -> onFinishedHandler.run());
            timeline.play();
        } else if (animationType == AnimationType.ScaleFromCenter) {
            double endScale = 0.25;
            keyFrames.add(new KeyFrame(ManagedDuration.ZERO,
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), 1, interpolator)
            ));
            keyFrames.add(new KeyFrame(duration,
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), endScale, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), endScale, interpolator)
            ));
        } else if (animationType == AnimationType.ScaleYFromCenter) {
            rootContainer.setRotationAxis(Rotate.X_AXIS);
            rootContainer.getScene().setCamera(new PerspectiveCamera());
            keyFrames.add(new KeyFrame(ManagedDuration.ZERO,
                    new KeyValue(rootContainer.rotateProperty(), 0, interpolator),
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator)
            ));
            keyFrames.add(new KeyFrame(duration,
                    new KeyValue(rootContainer.rotateProperty(), -90, interpolator),
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator)
            ));
        } else if (animationType == AnimationType.ScaleDownToCenter) {
            double endScale = 0.1;
            keyFrames.add(new KeyFrame(ManagedDuration.ZERO,
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), 1, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), 1, interpolator)
            ));
            keyFrames.add(new KeyFrame(duration,
                    new KeyValue(rootContainer.opacityProperty(), 0, interpolator),
                    new KeyValue(rootContainer.scaleXProperty(), endScale, interpolator),
                    new KeyValue(rootContainer.scaleYProperty(), endScale, interpolator)
            ));
        } else if (animationType == AnimationType.FadeInAtCenter) {
            keyFrames.add(new KeyFrame(ManagedDuration.ZERO,
                    new KeyValue(rootContainer.opacityProperty(), 1, interpolator)
            ));
            keyFrames.add(new KeyFrame(duration,
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

            if (OS.isWindows()) {
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

        if (headlineLabel != null) {
            headlineIcon.setManaged(true);
            headlineIcon.setVisible(true);
            headlineLabel.getStyleClass().add("overlay-headline");
            switch (type) {
                case NOTIFICATION:
                    headlineIcon.setManaged(false);
                    headlineIcon.setVisible(false);
                    break;
                case INFORMATION:
                case BACKGROUND_INFO:
                case INSTRUCTION:
                case CONFIRMATION:
                case FEEDBACK:
                case ATTENTION:
                    Icons.getIconForLabel(AwesomeIcon.INFO_SIGN, headlineIcon, "1.8em");
                    headlineLabel.getStyleClass().add("overlay-headline-information");
                    headlineIcon.getStyleClass().add("overlay-icon-information");
                    break;
                case WARNING:
                case INVALID:
                    Icons.getIconForLabel(AwesomeIcon.WARNING_SIGN, headlineIcon, "1.5em");
                    headlineLabel.getStyleClass().add("overlay-headline-warning");
                    headlineIcon.getStyleClass().add("overlay-icon-warning");
                    break;
                case FAILURE:
                case ERROR:
                    Icons.getIconForLabel(AwesomeIcon.EXCLAMATION_SIGN, headlineIcon, "1.5em");
                    headlineLabel.getStyleClass().add("overlay-headline-error");
                    headlineIcon.getStyleClass().add("overlay-icon-error");
                    break;
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
        if (headline != null) {
            HBox hBox = new HBox();
            hBox.setSpacing(7);
            headlineLabel = new Label(headline);
            headlineIcon = new Label();
            headlineIcon.setManaged(false);
            headlineIcon.setVisible(false);
            headlineIcon.setAlignment(Pos.CENTER);
            headlineIcon.setPadding(new Insets(0, 5, 0, 0));
            headlineLabel.setMouseTransparent(true);

            if (headlineStyle != null)
                headlineLabel.setStyle(headlineStyle);

            hBox.getChildren().addAll(headlineIcon, headlineLabel);
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
            messageLabel.getStyleClass().add("overlay-message");
            messageLabel.setWrapText(true);
            messageLabel.setMinWidth(width);
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
        if (messageHyperlinks != null && !messageHyperlinks.isEmpty()) {
            VBox footerBox = new VBox();
            GridPane.setRowIndex(footerBox, gridPane.getRowCount());
            GridPane.setColumnSpan(footerBox, 2);
            GridPane.setMargin(footerBox, new Insets(buttonDistance, 0, 0, 0));
            gridPane.getChildren().add(footerBox);
            for (int i = 0; i < messageHyperlinks.size(); i++) {
                Label enumeration = new Label(String.format("[%d]", i + 1));
                enumeration.getStyleClass().add("overlay-message");
                Hyperlink link = new Hyperlink(messageHyperlinks.get(i));
                link.getStyleClass().add("overlay-message");
                link.setOnAction(event -> Browser.open(link.getText()));
                String tooltipText = Browser.hyperLinksGetCopiedWithoutPopup()
                        ? Res.get("popup.hyperlink.copy.tooltip", link.getText())
                        : Res.get("popup.hyperlink.openInBrowser.tooltip", link.getText());
                link.setTooltip(new BisqTooltip(tooltipText));
                HBox hBox = new HBox(5, enumeration, link);
                hBox.setAlignment(Pos.CENTER_LEFT);
                footerBox.getChildren().addAll(hBox);
            }
        }
    }

    private void addReportErrorButtons() {
        Button logButton = new Button(Res.get("popup.reportError.log"));
        logButton.setOnAction(event -> PlatformUtils.open(appDataDirPath.resolve("bisq.log")));

        Button zipLogButton = new Button(Res.get("popup.reportError.zipLogs"));
        zipLogButton.setOnAction(event -> FileChooserUtil.chooseDirectory(getRootContainer().getScene(), appDataDirPath, "")
                .ifPresent(directory -> {
                    // Copy debug log file and replace users home directory with "<HOME_DIR>" to avoid that
                    // private data gets leaked in case the user used their real name as their OS user.
                    Path debugLogPath = Path.of(appDataDirPath + "/tor/").resolve("debug.log");
                    Path debugLogForZipFilePath = Path.of(appDataDirPath + "/tor/").resolve("debug_for_zip.log");
                    try {
                        Files.deleteIfExists(debugLogForZipFilePath);
                        FileMutatorUtils.copyFile(debugLogPath, debugLogForZipFilePath);
                        String logContent = FacadeProvider.getJdkFacade().readString(debugLogForZipFilePath);
                        logContent = StringUtils.maskHomeDirectory(logContent);
                        FacadeProvider.getJdkFacade().writeString(logContent, debugLogForZipFilePath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    URI uri = URI.create("jar:file:" + directory.resolve("bisq2-logs.zip").toUri().getRawPath());
                    Map<String, String> env = Map.of("create", "true");
                    List<Path> logPaths = Arrays.asList(
                            appDataDirPath.resolve("bisq.log"),
                            debugLogForZipFilePath);
                    try (FileSystem zipFileSystem = FileSystems.newFileSystem(uri, env)) {
                        logPaths.forEach(logPath -> {
                            if (Files.isRegularFile(logPath)) {
                                try {
                                    FileMutatorUtils.copyFile(logPath, zipFileSystem.getPath(logPath.getFileName().toString()));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                        PlatformUtils.open(directory);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));

        Button gitHubButton = new Button(Res.get("popup.reportError.gitHub"));
        gitHubButton.setOnAction(event -> {
            if (content instanceof TextArea errorReportTextArea) {
                ClipboardUtil.copyToClipboard(errorReportTextArea.getText());
                Browser.open("https://github.com/bisq-network/bisq2/issues");
            }
            hide();
        });

      /*  buttonBox.getChildren().add(0, Spacer.fillHBox());
        buttonBox.getChildren().add(0, gitHubButton);
        buttonBox.getChildren().add(0, zipLogButton);
        buttonBox.getChildren().add(0, logButton);*/

        HBox buttons = new HBox(10, gitHubButton, zipLogButton, logButton, Spacer.fillHBox());
        GridPane.setHalignment(buttons, buttonAlignment);
        GridPane.setMargin(buttons, new Insets(buttonDistance, 0, 0, 0));
        gridPane.add(buttons, 0, gridPane.getRowCount(), 2, 1);

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
            dontShowAgainService.putDontShowAgain(dontShowAgainId, isChecked);
            dontShowAgainCheckBox.setOnAction(e -> dontShowAgainService.putDontShowAgain(dontShowAgainId, dontShowAgainCheckBox.isSelected()));
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

        boolean useActionButton = actionHandlerOptional.isPresent() || actionButtonText != null;
        if (useActionButton) {
            actionButton = new Button(actionButtonText == null ? Res.get("confirmation.ok") : actionButtonText);

            HBox.setHgrow(actionButton, Priority.SOMETIMES);
            actionButton.setDefaultButton(true);

            actionButton.setOnAction(event -> {
                if (doCloseOnAction) {
                    hide();
                }
                actionHandlerOptional.ifPresent(Runnable::run);
            });
        }
        boolean useSecondaryActionButton = secondaryActionButtonText != null && secondaryActionHandlerOptional.isPresent();
        if (useSecondaryActionButton) {
            secondaryActionButton = new Button(secondaryActionButtonText);
            secondaryActionButton.setOnAction(event -> {
                if (doCloseOnSecondaryAction) {
                    hide();
                }
                secondaryActionHandlerOptional.ifPresent(Runnable::run);
            });
        }
        if (!useActionButton && !useSecondaryActionButton && !hideCloseButton) {
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

    public boolean isDisplayed() {
        return isDisplayed;
    }

    protected abstract T cast();

    @Override
    public String toString() {
        return "Popup{" +
                "headline='" + headline + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}

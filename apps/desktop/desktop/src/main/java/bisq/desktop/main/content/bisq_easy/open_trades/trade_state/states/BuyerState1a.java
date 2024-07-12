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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states;

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.bisq_easy.NavigationTarget;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.observable.Pin;
import bisq.common.util.ExceptionUtil;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.components.controls.validator.SettableErrorValidator;
import bisq.desktop.webcam.WebcamAppModel;
import bisq.desktop.webcam.WebcamAppService;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BuyerState1a extends BaseState {
    private final Controller controller;

    public BuyerState1a(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private final WebcamAppService webcamAppService;
        private Pin qrCodePin, imageRecognizedPin, webcamAppErrorMessagePin, localExceptionPin, webcamAppStatePin, restartSignalReceivedPin;

        private Controller(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);
            webcamAppService = serviceProvider.getWebcamAppService();
        }

        @Override
        protected Model createModel(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            return new Model(bisqEasyTrade, channel);
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();

            BitcoinPaymentRail paymentRail = model.getBisqEasyTrade().getContract().getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail();
            String name = paymentRail.name();
            model.setBitcoinPaymentHeadline(Res.get("bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.headline." + name));
            model.setBitcoinPaymentDescription(Res.get("bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.description." + name));
            model.setBitcoinPaymentPrompt(Res.get("bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.prompt." + name));
            if (paymentRail == BitcoinPaymentRail.MAIN_CHAIN) {
                model.setBitcoinPaymentHelp(Res.get("bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.walletHelp"));
            }
            model.getSendButtonDisabled().bind(model.getBitcoinPaymentData().isEmpty());
            model.getScanQrCodeButtonVisible().set(webcamAppService.isIdle());
            model.getWebcamStateIconId().set("scan-qr-code");
            model.getWebcamStateInfo().set(null);
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();

            model.getSendButtonDisabled().unbind();
            unBindQrCodePins();
            webcamAppService.shutdown();
        }

        private void unBindQrCodePins() {
            if (qrCodePin != null) {
                qrCodePin.unbind();
            }
            if (imageRecognizedPin != null) {
                imageRecognizedPin.unbind();
            }
            if (webcamAppStatePin != null) {
                webcamAppStatePin.unbind();
            }
            if (webcamAppErrorMessagePin != null) {
                webcamAppErrorMessagePin.unbind();
            }
            if (localExceptionPin != null) {
                localExceptionPin.unbind();
            }
            if (restartSignalReceivedPin != null) {
                restartSignalReceivedPin.unbind();
            }
        }

        private void onSend() {
            String name = model.getBisqEasyTrade().getContract().getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail().name();
            String key = "bisqEasy.tradeState.info.buyer.phase1a.tradeLogMessage." + name;
            sendTradeLogMessage(Res.encode(key, model.getChannel().getMyUserIdentity().getUserName(), model.getBitcoinPaymentData().get()));
            bisqEasyTradeService.buyerSendBitcoinPaymentData(model.getBisqEasyTrade(), model.getBitcoinPaymentData().get());
        }

        void onOpenWalletHelp() {
            Navigation.navigateTo(NavigationTarget.WALLET_GUIDE);
        }

        void onScanQrCode() {
            if (webcamAppService.isIdle()) {
                reset();

                WebcamAppModel webcamAppServiceModel = webcamAppService.getModel();
                qrCodePin = webcamAppServiceModel.getQrCode().addObserver(qrCode -> {
                    UIThread.run(() -> {
                        model.getQrCodeDetectedFromWebcam().set(qrCode != null);
                        if (qrCode != null) {
                            model.getBitcoinPaymentData().set(qrCode);
                        }
                    });
                });

                imageRecognizedPin = webcamAppServiceModel.getImageRecognized().addObserver(imageRecognized -> {
                    if (imageRecognized != null) {
                        UIThread.run(() -> {
                            if (imageRecognized) {
                                model.getIsConnectingWebcam().set(false);
                                model.getWebcamStateIconId().set("webcam-state-image-recognized");
                                model.getWebcamStateInfo().set(Res.get("bisqEasy.tradeState.info.buyer.phase1a.scanQrCode.webcamState.imageRecognized"));
                            }
                        });
                    }
                });

                webcamAppStatePin = webcamAppService.getState().addObserver(state -> {
                    if (state != null) {
                        UIThread.run(() -> {
                            boolean isIdle = false;
                            switch (state) {
                                case NEW:
                                    isIdle = true;
                                    model.getIsConnectingWebcam().set(true);
                                    model.getWebcamStateInfo().set(null);
                                    break;
                                case STARTING:
                                    model.getIsConnectingWebcam().set(true);
                                    model.getWebcamStateIconId().set("webcam-state-connecting");
                                    model.getWebcamStateInfo().set(Res.get("bisqEasy.tradeState.info.buyer.phase1a.scanQrCode.webcamState.connecting"));
                                    break;
                                case RUNNING:
                                    break;
                                case STOPPING:
                                    break;
                                case TERMINATED:
                                    isIdle = true;
                                    model.getIsConnectingWebcam().set(false);
                                    model.getWebcamStateInfo().set(null);
                                    break;
                            }
                            model.getScanQrCodeButtonVisible().set(isIdle);
                            model.getWebcamStateVisible().set(!isIdle);
                        });
                    }
                });

                webcamAppErrorMessagePin = webcamAppServiceModel.getWebcamAppErrorMessage().addObserver(webcamAppErrorMessage -> {
                    if (webcamAppErrorMessage != null) {
                        UIThread.run(() -> {
                            model.getIsConnectingWebcam().set(false);
                            model.getWebcamStateInfo().set(Res.get("bisqEasy.tradeState.info.buyer.phase1a.scanQrCode.webcamState.failed"));
                            model.getWebcamErrorMessage().set(webcamAppErrorMessage);
                        });
                    }
                });

                localExceptionPin = webcamAppServiceModel.getLocalException().addObserver(exception -> {
                    if (exception != null) {
                        UIThread.run(() -> {
                            model.getIsConnectingWebcam().set(false);
                            model.getWebcamStateInfo().set(Res.get("bisqEasy.tradeState.info.buyer.phase1a.scanQrCode.webcamState.failed"));
                            model.getWebcamErrorMessage().set(ExceptionUtil.getRootCauseMessage(exception));
                        });
                    }
                });

                restartSignalReceivedPin = webcamAppServiceModel.getRestartSignalReceived().addObserver(restartSignalReceived -> {
                    if (restartSignalReceived != null && restartSignalReceived) {
                        UIScheduler.run(() -> {
                                    onScanQrCode();
                                }
                        ).after(1000);
                    }
                });

                webcamAppService.start();
            }
        }

        private void reset() {
            model.getQrCodeDetectedFromWebcam().set(false);
            model.getImageRecognizedFromWebcam().set(false);
            model.getWebcamErrorMessage().set(null);
            model.getWebcamStateVisible().set(false);
            model.getWebcamStateInfo().set(null);
            model.getIsConnectingWebcam().set(false);
            model.getWebcamStateIconId().set(null);
            model.getScanQrCodeButtonVisible().set(webcamAppService.isIdle());
            unBindQrCodePins();
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        private String bitcoinPaymentHeadline;
        @Setter
        private String bitcoinPaymentDescription;
        @Setter
        private String bitcoinPaymentPrompt;
        @Setter
        private String bitcoinPaymentHelp;
        private final StringProperty bitcoinPaymentData = new SimpleStringProperty();
        private final BooleanProperty sendButtonDisabled = new SimpleBooleanProperty();
        private final ObjectProperty<WebcamAppService.State> isWebcamAppState = new SimpleObjectProperty<>();
        private final StringProperty webcamErrorMessage = new SimpleStringProperty();
        private final BooleanProperty qrCodeDetectedFromWebcam = new SimpleBooleanProperty();
        private final BooleanProperty imageRecognizedFromWebcam = new SimpleBooleanProperty();
        private final BooleanProperty scanQrCodeButtonVisible = new SimpleBooleanProperty();
        private final BooleanProperty webcamStateVisible = new SimpleBooleanProperty();
        private final BooleanProperty isConnectingWebcam = new SimpleBooleanProperty();
        private final StringProperty webcamStateIconId = new SimpleStringProperty();
        private final StringProperty webcamStateInfo = new SimpleStringProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private static final int WEBCAM_STATE_WIDTH = 300;

        private final Button sendButton, walletInfoButton, scanQrCodeButton;
        private final MaterialTextField bitcoinPayment, webcamStateMaterialTextField;
        private final WrappingText bitcoinPaymentHeadline;
        private final ImageView webcamStateIcon;
        private final BisqTooltip scanQrCodeButtonTooltip;
        private final ImageView scanQrCodeButtonIcon;
        private final Timeline webcamStateAnimationTimeline;
        private final Region webcamStateMarkerLine;
        private final SettableErrorValidator webcamStateValidator;
        private Subscription qrCodeDetectedFromWebcamPin, webcamStateVisiblePin;

        private View(Model model, Controller controller) {
            super(model, controller);

            bitcoinPaymentHeadline = FormUtils.getHeadline();
            VBox.setMargin(bitcoinPaymentHeadline, new Insets(5, 0, 0, 0));

            bitcoinPayment = FormUtils.getTextField("", "", true);

            scanQrCodeButtonIcon = ImageUtil.getImageViewById("scan-qr-code");
            scanQrCodeButtonIcon.setOpacity(0.5);

            scanQrCodeButton = new Button(null, scanQrCodeButtonIcon);
            scanQrCodeButton.setStyle("-fx-padding: 0");
            scanQrCodeButton.setPrefHeight(55);
            scanQrCodeButton.setPrefWidth(55);
            scanQrCodeButtonTooltip = new BisqTooltip(Res.get("bisqEasy.tradeState.info.buyer.phase1a.scanQrCode.tooltip"));
            scanQrCodeButton.setTooltip(scanQrCodeButtonTooltip);

            webcamStateIcon = ImageUtil.getImageViewById("scan-qr-code");
            webcamStateIcon.setOpacity(0.6);

            webcamStateMaterialTextField = new MaterialTextField(Res.get("bisqEasy.tradeState.info.buyer.phase1a.scanQrCode.webcamState.description"));
            webcamStateValidator = new SettableErrorValidator();
            webcamStateMaterialTextField.setValidators(webcamStateValidator);
            webcamStateMaterialTextField.getTextInputControl().setEditable(true);
            webcamStateMaterialTextField.showIcon();
            webcamStateMarkerLine = webcamStateMaterialTextField.getSelectionLine();
            BisqIconButton iconButton = webcamStateMaterialTextField.getIconButton();
            iconButton.setMouseTransparent(true);
            // If width/height are not set layout has issues when app is not focussed
            iconButton.setMinWidth(37);
            iconButton.setMinHeight(38);
            iconButton.setPadding(new Insets(40, 0, 0, 0));
            iconButton.setAlignment(Pos.CENTER_LEFT);
            // Hack to tweak layout
            VBox.setMargin(webcamStateIcon, new Insets(1, 0, 0, 0));
            VBox webcamStateIconBox = new VBox(webcamStateIcon);
            iconButton.setIcon(webcamStateIconBox);
            webcamStateMaterialTextField.setPrefWidth(WEBCAM_STATE_WIDTH);

            HBox.setHgrow(bitcoinPayment, Priority.ALWAYS);
            HBox hBox = new HBox(10, bitcoinPayment, webcamStateMaterialTextField, scanQrCodeButton);

            sendButton = new Button(Res.get("bisqEasy.tradeState.info.buyer.phase1a.send"));
            sendButton.setDefaultButton(true);

            walletInfoButton = new Button(Res.get("bisqEasy.tradeState.info.buyer.phase1a.walletHelpButton"));
            walletInfoButton.getStyleClass().add("outlined-button");
            HBox buttons = new HBox(10, sendButton, Spacer.fillHBox(), walletInfoButton);

            VBox.setMargin(buttons, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(bitcoinPaymentHeadline, hBox, buttons);

            webcamStateAnimationTimeline = new Timeline();
            setupWebcamStateAnimation();
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            bitcoinPaymentHeadline.setText(model.getBitcoinPaymentHeadline());
            bitcoinPayment.setDescription(model.getBitcoinPaymentDescription());
            bitcoinPayment.setPromptText(model.getBitcoinPaymentPrompt());
            bitcoinPayment.setHelpText(model.getBitcoinPaymentHelp());

            bitcoinPayment.textProperty().bindBidirectional(model.getBitcoinPaymentData());
            scanQrCodeButton.visibleProperty().bind(model.getScanQrCodeButtonVisible());
            scanQrCodeButton.managedProperty().bind(scanQrCodeButton.visibleProperty());
            webcamStateMaterialTextField.visibleProperty().bind(model.getWebcamStateVisible());
            webcamStateMaterialTextField.managedProperty().bind(webcamStateMaterialTextField.visibleProperty());
            webcamStateMaterialTextField.textProperty().bind(model.getWebcamStateInfo());
            webcamStateIcon.idProperty().bind(model.getWebcamStateIconId());
            sendButton.disableProperty().bind(model.getSendButtonDisabled());
            webcamStateValidator.messageProperty().bind(model.getWebcamErrorMessage());
            webcamStateValidator.invalidProperty().bind(model.getWebcamErrorMessage().isEmpty().not());

            qrCodeDetectedFromWebcamPin = EasyBind.subscribe(model.getQrCodeDetectedFromWebcam(), qrCodeDetectedFromWebcam -> {
                if (qrCodeDetectedFromWebcam != null && qrCodeDetectedFromWebcam) {
                    bitcoinPayment.deselect();
                    UIScheduler.run(() -> {
                        bitcoinPayment.getTextInputControl().requestFocus();
                        bitcoinPayment.deselect();
                    }).after(300);
                }
            });
            webcamStateVisiblePin = EasyBind.subscribe(model.getIsConnectingWebcam(), webcamStateVisible -> {
                if (webcamStateVisible) {
                    webcamStateAnimationTimeline.playFromStart();
                } else {
                    webcamStateAnimationTimeline.stop();
                    webcamStateMaterialTextField.getSelectionLine().setTranslateX(0);
                    webcamStateMaterialTextField.getSelectionLine().setPrefWidth(WEBCAM_STATE_WIDTH);
                }
            });

            sendButton.setOnAction(e -> controller.onSend());
            walletInfoButton.setOnAction(e -> controller.onOpenWalletHelp());
            scanQrCodeButton.setOnAction(e -> controller.onScanQrCode());

            bitcoinPayment.validate();
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            bitcoinPayment.textProperty().unbindBidirectional(model.getBitcoinPaymentData());
            scanQrCodeButton.visibleProperty().unbind();
            scanQrCodeButton.managedProperty().unbind();
            webcamStateMaterialTextField.visibleProperty().unbind();
            webcamStateMaterialTextField.managedProperty().unbind();
            webcamStateMaterialTextField.textProperty().unbind();
            webcamStateIcon.idProperty().unbind();
            sendButton.disableProperty().unbind();
            webcamStateValidator.messageProperty().unbind();
            webcamStateValidator.invalidProperty().unbind();

            qrCodeDetectedFromWebcamPin.unsubscribe();
            webcamStateVisiblePin.unsubscribe();

            sendButton.setOnAction(null);
            walletInfoButton.setOnAction(null);
            scanQrCodeButton.setOnAction(null);

            webcamStateAnimationTimeline.stop();
        }

        private void setupWebcamStateAnimation() {
            webcamStateAnimationTimeline.setCycleCount(Integer.MAX_VALUE);
            ObservableList<KeyFrame> keyFrames = webcamStateAnimationTimeline.getKeyFrames();
            int delay = 0;
            keyFrames.add(new KeyFrame(Duration.millis(delay), new KeyValue(webcamStateMarkerLine.prefWidthProperty(), 0, Interpolator.LINEAR)));
            keyFrames.add(new KeyFrame(Duration.millis(delay), new KeyValue(webcamStateMarkerLine.translateXProperty(), 0, Interpolator.EASE_OUT)));
            delay += 300;
            keyFrames.add(new KeyFrame(Duration.millis(delay), new KeyValue(webcamStateMarkerLine.prefWidthProperty(), WEBCAM_STATE_WIDTH, Interpolator.EASE_OUT)));
            keyFrames.add(new KeyFrame(Duration.millis(delay), new KeyValue(webcamStateMarkerLine.translateXProperty(), 0, Interpolator.EASE_OUT)));
            delay += 600;
            keyFrames.add(new KeyFrame(Duration.millis(delay), new KeyValue(webcamStateMarkerLine.prefWidthProperty(), WEBCAM_STATE_WIDTH, Interpolator.EASE_OUT)));
            keyFrames.add(new KeyFrame(Duration.millis(delay), new KeyValue(webcamStateMarkerLine.translateXProperty(), 0, Interpolator.EASE_OUT)));
            delay += 300;
            keyFrames.add(new KeyFrame(Duration.millis(delay), new KeyValue(webcamStateMarkerLine.prefWidthProperty(), 0, Interpolator.EASE_OUT)));
            keyFrames.add(new KeyFrame(Duration.millis(delay), new KeyValue(webcamStateMarkerLine.translateXProperty(), WEBCAM_STATE_WIDTH, Interpolator.EASE_OUT)));
            delay += 600;
            keyFrames.add(new KeyFrame(Duration.millis(delay), new KeyValue(webcamStateMarkerLine.prefWidthProperty(), 0, Interpolator.EASE_OUT)));
            keyFrames.add(new KeyFrame(Duration.millis(delay), new KeyValue(webcamStateMarkerLine.translateXProperty(), 0, Interpolator.EASE_OUT)));
        }
    }
}

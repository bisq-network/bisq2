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
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.data.Pair;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.qr.QrCodeDisplay;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SellerState3a extends BaseState {
    private final Controller controller;

    public SellerState3a(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);
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
            model.setBitcoinPaymentDescription(Res.get("bisqEasy.tradeState.info.seller.phase3a.bitcoinPayment.description." + name));
            model.setPaymentProofDescription(Res.get("bisqEasy.tradeState.info.seller.phase3a.paymentProof.description." + name));
            model.setPaymentProofPrompt(Res.get("bisqEasy.tradeState.info.seller.phase3a.paymentProof.prompt." + name));

            model.setBitcoinPaymentData(model.getBisqEasyTrade().getBitcoinPaymentData().get());
            if (paymentRail == BitcoinPaymentRail.MAIN_CHAIN) {
                // Typical bitcoin address require size of 29 or a multiple of it
                model.setQrCodeSize(116); //233
                model.getBtcSentButtonDisabled().bind(model.getPaymentProof().isEmpty());
            } else {
                // TypicalLN invoice require size of 65 or a multiple of it
                model.setQrCodeSize(130);
            }
            model.getIsMaximized().set(false);
            model.getMaximizeIconVisible().bind(model.getIsMaximized().not());
            model.getMinimizeIconVisible().bind(model.getIsMaximized());
            model.getQrCodeImage().set(QrCodeDisplay.toImage(model.getBitcoinPaymentData(), model.getQrCodeSize()));
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();

            model.getMaximizeIconVisible().unbind();
            model.getMinimizeIconVisible().unbind();
            model.getBtcSentButtonDisabled().unbind();
        }

        private void onBtcSent() {
            String name = model.getBisqEasyTrade().getContract().getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail().name();
            String proof = Res.get("bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage.paymentProof." + name);
            String userName = model.getChannel().getMyUserIdentity().getUserName();
            if (model.getPaymentProof().get() == null) {
                sendTradeLogMessage(Res.encode("bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage.noProofProvided", userName));
            } else {
                sendTradeLogMessage(Res.encode("bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage",
                        userName, proof, model.getPaymentProof().get()));
            }
            bisqEasyTradeService.sellerConfirmBtcSent(model.getBisqEasyTrade(), Optional.ofNullable(model.getPaymentProof().get()));
        }

        void onToggleQrCodeSize() {
            model.getIsMaximized().set(!model.getIsMaximized().get());
            if (model.getIsMaximized().get()) {
                model.getQrCodeImage().set(QrCodeDisplay.toImage(model.getBitcoinPaymentData(), 2 * model.getQrCodeSize()));
            } else {
                model.getQrCodeImage().set(QrCodeDisplay.toImage(model.getBitcoinPaymentData(), model.getQrCodeSize()));
            }
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        protected String bitcoinPaymentData;
        private final StringProperty paymentProof = new SimpleStringProperty();
        private final BooleanProperty btcSentButtonDisabled = new SimpleBooleanProperty();
        private final BooleanProperty isMaximized = new SimpleBooleanProperty();
        private final BooleanProperty maximizeIconVisible = new SimpleBooleanProperty();
        private final BooleanProperty minimizeIconVisible = new SimpleBooleanProperty();
        private final ObjectProperty<Image> qrCodeImage = new SimpleObjectProperty<>();
        @Setter
        private String bitcoinPaymentDescription;
        @Setter
        private String paymentProofDescription;
        @Setter
        private String paymentProofPrompt;
        @Setter
        private int qrCodeSize = 130;

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {

        private final Button sentButton;
        private final MaterialTextField paymentProof;
        private final WrappingText sendBtcHeadline, fiatReceiptConfirmed;
        private final MaterialTextField baseAmount;
        private final MaterialTextField bitcoinPayment;
        private final Label qrCodeLabel;
        private final ImageView qrCodeImageView, maximizeIcon, minimizeIcon;
        private final VBox qrCodeBox;

        private View(Model model, Controller controller) {
            super(model, controller);

            Pair<WrappingText, HBox> confirmPair = FormUtils.getConfirmInfo();
            fiatReceiptConfirmed = confirmPair.getFirst();
            HBox fiatReceiptConfirmedHBox = confirmPair.getSecond();

            sendBtcHeadline = FormUtils.getHeadline();

            baseAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3a.baseAmount"), "", false);
            bitcoinPayment = FormUtils.getTextField("", "", false);
            paymentProof = FormUtils.getTextField("", "", true);

            VBox textFields = new VBox(10, baseAmount,
                    bitcoinPayment,
                    paymentProof);

            maximizeIcon = ImageUtil.getImageViewById("maximize");
            minimizeIcon = ImageUtil.getImageViewById("minimize");
            minimizeIcon.setVisible(false);
            minimizeIcon.setManaged(false);

            HBox minMaxButtons = new HBox(Spacer.fillHBox(), minimizeIcon, maximizeIcon);
            minMaxButtons.setOpacity(0.4);
            minMaxButtons.setFillHeight(true);
            minMaxButtons.setPadding(new Insets(5, 0, 5, 0));

            qrCodeLabel = new Label();
            qrCodeLabel.setPadding(new Insets(3, 0, 5, 0));
            qrCodeLabel.getStyleClass().add("qr-code-display");
            qrCodeImageView = new ImageView();

            // VBox.setMargin(qrCodeLabel, new Insets(-2, 0, 0, 0));
            qrCodeBox = new VBox(Spacer.fillVBox(), minMaxButtons, Spacer.fillVBox(),
                    qrCodeImageView, Spacer.fillVBox(),
                    qrCodeLabel, Spacer.fillVBox());
            qrCodeBox.setAlignment(Pos.CENTER);
            qrCodeBox.setPadding(new Insets(0, 10, 0, 10));
            qrCodeBox.getStyleClass().add("qr-code-display");

            HBox.setHgrow(textFields, Priority.ALWAYS);
            HBox hBox = new HBox(20, textFields, qrCodeBox);
            sentButton = new Button();
            sentButton.setDefaultButton(true);

            VBox.setMargin(fiatReceiptConfirmedHBox, new Insets(0, 0, 5, 0));
            VBox.setMargin(sentButton, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(
                    fiatReceiptConfirmedHBox,
                    sendBtcHeadline,
                    hBox,
                    sentButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            paymentProof.textProperty().bindBidirectional(model.getPaymentProof());
            sentButton.disableProperty().bind(model.getBtcSentButtonDisabled());
            qrCodeImageView.imageProperty().bind(model.getQrCodeImage());
            maximizeIcon.visibleProperty().bind(model.getMaximizeIconVisible());
            maximizeIcon.managedProperty().bind(model.getMaximizeIconVisible());
            minimizeIcon.visibleProperty().bind(model.getMinimizeIconVisible());
            minimizeIcon.managedProperty().bind(model.getMinimizeIconVisible());

            baseAmount.setText(model.getFormattedBaseAmount());
            bitcoinPayment.setDescription(model.getBitcoinPaymentDescription());
            qrCodeLabel.setText(model.getBitcoinPaymentDescription());
            bitcoinPayment.setText(model.getBitcoinPaymentData());
            paymentProof.setDescription(model.getPaymentProofDescription());
            paymentProof.setPromptText(model.getPaymentProofPrompt());
            sendBtcHeadline.setText(Res.get("bisqEasy.tradeState.info.seller.phase3a.sendBtc", model.getFormattedBaseAmount()));
            fiatReceiptConfirmed.setText(Res.get("bisqEasy.tradeState.info.seller.phase3a.fiatPaymentReceivedCheckBox", model.getFormattedQuoteAmount()));
            sentButton.setText(Res.get("bisqEasy.tradeState.info.seller.phase3a.btcSentButton", model.getFormattedBaseAmount()));

            baseAmount.getIconButton().setOnAction(e -> ClipboardUtil.copyToClipboard(model.getBaseAmount()));
            qrCodeBox.setOnMouseClicked(e -> controller.onToggleQrCodeSize());
            sentButton.setOnAction(e -> controller.onBtcSent());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            paymentProof.textProperty().unbindBidirectional(model.getPaymentProof());
            sentButton.disableProperty().unbind();
            qrCodeImageView.imageProperty().unbind();
            maximizeIcon.visibleProperty().unbind();
            maximizeIcon.managedProperty().unbind();
            minimizeIcon.visibleProperty().unbind();
            minimizeIcon.managedProperty().unbind();

            sentButton.setOnAction(null);
            baseAmount.getIconButton().setOnAction(null);
            qrCodeBox.setOnMouseClicked(null);
        }
    }
}

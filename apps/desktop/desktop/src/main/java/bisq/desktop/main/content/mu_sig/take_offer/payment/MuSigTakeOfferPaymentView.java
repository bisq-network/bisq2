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

package bisq.desktop.main.content.mu_sig.take_offer.payment;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.mu_sig.components.PaymentMethodChipButton;
import bisq.desktop.main.content.mu_sig.create_offer.MuSigCreateOfferView;
import bisq.i18n.Res;
import bisq.account.payment_method.PaymentMethodSpec;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class MuSigTakeOfferPaymentView extends View<StackPane, MuSigTakeOfferPaymentModel, MuSigTakeOfferPaymentController> {
    private final static int FEEDBACK_WIDTH = 700;
    private static final double TWO_COLUMN_WIDTH = 20.75;

    private final GridPane gridPane;
    private final Label headlineLabel, noAccountOverlayHeadline, subtitleLabel, multipleAccountOverlayHeadline;
    private final VBox noAccountOverlay, multipleAccountOverlay, content;
    private final Button noAccountOverlayCloseButton, createAccountButton, multipleAccountOverlayCloseButton;
    private final AutoCompleteComboBox<Account<?, ?>> singlePaymentMethodAccountSelection, accountSelection;
    private final Set<ImageView> closeIcons = new HashSet<>();
    private final List<PaymentMethodChipButton> paymentMethodChipButtons = new ArrayList<>();

    private Subscription paymentMethodWithoutAccountPin, paymentMethodWithMultipleAccountsPin, selectedPaymentMethodPin, selectedTogglePin;

    public MuSigTakeOfferPaymentView(MuSigTakeOfferPaymentModel model,
                                     MuSigTakeOfferPaymentController controller) {
        super(new StackPane(), model, controller);

        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("bisq-easy-trade-wizard-payment-methods-step");

        headlineLabel = new Label();
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        subtitleLabel = new Label();
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().add("bisq-text-3");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(700);

        gridPane = GridPaneUtil.getGridPane(10, 10, new Insets(0));
        gridPane.getStyleClass().add("fiat-methods-grid-pane");

        singlePaymentMethodAccountSelection = createComboBox();

        VBox vBox = new VBox(20, subtitleLabel, gridPane, singlePaymentMethodAccountSelection);
        vBox.setAlignment(Pos.CENTER);

        content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        VBox.setVgrow(headlineLabel, Priority.ALWAYS);
        VBox.setVgrow(vBox, Priority.ALWAYS);
        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, vBox, Spacer.fillVBox());

        // noAccount overlay
        noAccountOverlayHeadline = new Label();
        noAccountOverlayCloseButton = new Button(Res.get("action.close"));
        createAccountButton = new Button(Res.get("muSig.createOffer.paymentMethod.noAccountOverlay.createAccount"));
        noAccountOverlay = new VBox(20);
        configNoAccountOverlay();

        // multipleAccount overlay
        multipleAccountOverlayHeadline = new Label();
        multipleAccountOverlayCloseButton = new Button(Res.get("action.close"));
        multipleAccountOverlay = new VBox(20);
        accountSelection = createComboBox();
        configMultipleAccountOverlay();

        StackPane.setMargin(content, new Insets(40));
        StackPane.setMargin(noAccountOverlay, new Insets(-MuSigCreateOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        StackPane.setMargin(multipleAccountOverlay, new Insets(-MuSigCreateOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(content, noAccountOverlay, multipleAccountOverlay);
    }

    @Override
    protected void onViewAttached() {
        paymentMethodWithoutAccountPin = EasyBind.subscribe(model.getPaymentMethodWithoutAccount(),
                paymentMethod -> {
                    noAccountOverlayCloseButton.setOnAction(null);
                    createAccountButton.setOnAction(null);
                    if (paymentMethod != null) {
                        noAccountOverlay.setVisible(true);
                        noAccountOverlayHeadline.setText(Res.get("muSig.createOffer.paymentMethod.noAccountOverlay.headline", paymentMethod.getShortDisplayString()));
                        noAccountOverlayCloseButton.setOnAction(e -> controller.onCloseNoAccountOverlay(paymentMethod));
                        createAccountButton.setOnAction(e -> controller.onOpenCreateAccountScreen(paymentMethod));
                        root.setOnKeyPressed(controller::onKeyPressedWhileShowingOverlay);
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(noAccountOverlay, 450);
                    } else {
                        root.setOnKeyPressed(null);
                        noAccountOverlay.setVisible(false);
                        Transitions.removeEffect(content);
                    }
                });

        paymentMethodWithMultipleAccountsPin = EasyBind.subscribe(model.getPaymentMethodWithMultipleAccounts(),
                paymentMethod -> {
                    accountSelection.setOnChangeConfirmed(null);
                    multipleAccountOverlayCloseButton.setOnAction(null);
                    if (paymentMethod != null) {
                        multipleAccountOverlay.setVisible(true);
                        multipleAccountOverlayHeadline.setText(Res.get("muSig.createOffer.paymentMethod.multipleAccountOverlay.headline", paymentMethod.getShortDisplayString()));
                        accountSelection.setOnChangeConfirmed(e -> {
                            Account<?, ?> account = accountSelection.getSelectionModel().getSelectedItem();
                            if (account != null) {
                                findPaymentMethodChipButton(paymentMethod)
                                        .ifPresent(button -> button.setAccountName(account.getAccountName()));
                                controller.onSelectAccount(account);
                                UIThread.runOnNextRenderFrame(() -> accountSelection.getSelectionModel().select(null));
                            }
                        });
                        multipleAccountOverlayCloseButton.setOnAction(e -> controller.onCloseMultipleAccountsOverlay());
                        root.setOnKeyPressed(controller::onKeyPressedWhileShowingOverlay);
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(multipleAccountOverlay, 450);
                    } else {
                        root.setOnKeyPressed(null);
                        multipleAccountOverlay.setVisible(false);
                        Transitions.removeEffect(content);
                    }
                });

        selectedPaymentMethodPin = EasyBind.subscribe(model.getSelectedPaymentMethodSpec(),
                paymentMethodSpec -> {
                    if (paymentMethodSpec != null) {
                        updateSelectionsState();
                    }
                });
        selectedTogglePin = EasyBind.subscribe(model.getToggleGroup().selectedToggleProperty(), selectedToggle -> {
            if (selectedToggle == null) {
                paymentMethodChipButtons.forEach(button -> button.setAccountName(null));
            }
        });

        root.setOnMousePressed(e -> root.requestFocus());

        if (model.isSinglePaymentMethod()) {
            singlePaymentMethodAccountSelection.setDescription(model.getSinglePaymentMethodAccountSelectionDescription());
            singlePaymentMethodAccountSelection.getSelectionModel().select(model.getSelectedAccount().get());
            singlePaymentMethodAccountSelection.setOnChangeConfirmed(e -> {
                Account<?, ?> account = singlePaymentMethodAccountSelection.getSelectionModel().getSelectedItem();
                if (account != null) {
                    findPaymentMethodChipButton(account.getPaymentMethod())
                            .ifPresent(button -> button.setAccountName(account.getAccountName()));
                    controller.onSelectAccount(account);
                }
            });
        }
        singlePaymentMethodAccountSelection.setVisible(model.isSinglePaymentMethod());
        singlePaymentMethodAccountSelection.setManaged(model.isSinglePaymentMethod());
        gridPane.setVisible(!model.isSinglePaymentMethod());
        gridPane.setManaged(!model.isSinglePaymentMethod());

        headlineLabel.setText(model.getHeadline());
        subtitleLabel.setText(model.getSubtitle());
        setUpAndFillPaymentMethods();
    }

    @Override
    protected void onViewDetached() {
        paymentMethodWithoutAccountPin.unsubscribe();
        paymentMethodWithMultipleAccountsPin.unsubscribe();
        selectedPaymentMethodPin.unsubscribe();
        selectedTogglePin.unsubscribe();

        paymentMethodChipButtons.forEach(PaymentMethodChipButton::dispose);

        createAccountButton.setOnAction(null);
        noAccountOverlayCloseButton.setOnAction(null);
        multipleAccountOverlayCloseButton.setOnAction(null);
        singlePaymentMethodAccountSelection.setOnChangeConfirmed(null);
        accountSelection.setOnChangeConfirmed(null);

        closeIcons.forEach(imageView -> imageView.setOnMousePressed(null));
        closeIcons.clear();

        root.setOnKeyPressed(null);
        root.setOnMousePressed(null);
    }

    private void setUpAndFillPaymentMethods() {
        closeIcons.forEach(imageView -> imageView.setOnMousePressed(null));
        closeIcons.clear();
        gridPane.getChildren().clear();
        gridPane.getColumnConstraints().clear();
        paymentMethodChipButtons.clear();

        int paymentMethodsCount = model.getSortedPaymentMethods().size();
        int numColumns = paymentMethodsCount < 10 ? 3 : 4;
        GridPaneUtil.setGridPaneMultiColumnsConstraints(gridPane, numColumns);

        int i = 0;
        int col, row;
        for (; i < paymentMethodsCount; ++i) {
            PaymentMethod<?> paymentMethod = model.getSortedPaymentMethods().get(i);

            // enum name or custom name
            PaymentMethodChipButton button = new PaymentMethodChipButton(paymentMethod);
            button.setToggleGroup(model.getToggleGroup());
            PaymentMethodSpec<?> paymentMethodSpec = model.getSelectedPaymentMethodSpec().get();
            boolean isSelected = paymentMethodSpec != null && paymentMethod.equals(paymentMethodSpec.getPaymentMethod());
            button.setSelected(isSelected);
            if (!paymentMethod.getShortDisplayString().equals(paymentMethod.getDisplayString())) {
                button.setTooltip(new BisqTooltip(paymentMethod.getDisplayString()));
            }

            boolean hasAccounts = model.getAccountsByPaymentMethod().containsKey(paymentMethod);
            button.setActive(hasAccounts);
            List<Account<?, ?>> accounts = model.getAccountsByPaymentMethod().get(paymentMethod);
            button.setNumAccounts(hasAccounts ? accounts.size() : 0);

            Account<?, ?> account = model.getSelectedAccount().get();
            if (accounts != null && account != null && accounts.size() > 1 && account.getPaymentMethod().equals(paymentMethod)) {
                button.setAccountName(account.getAccountName());
            } else {
                button.setAccountName(null);
            }

            button.setOnAction(() -> controller.onTogglePaymentMethod(paymentMethod, button.isSelected()));

            col = i % numColumns;
            row = i / numColumns;
            gridPane.add(button, col, row);
            paymentMethodChipButtons.add(button);
        }
    }

    private Optional<PaymentMethodChipButton> findPaymentMethodChipButton(PaymentMethod<?> paymentMethod) {
        return paymentMethodChipButtons.stream()
                .filter(button -> button.getPaymentMethod().equals(paymentMethod))
                .findAny();
    }

    private void updateSelectionsState() {
        paymentMethodChipButtons.forEach(button -> {
            PaymentMethodSpec<?> paymentMethodSpec = model.getSelectedPaymentMethodSpec().get();
            boolean isSelected = paymentMethodSpec != null && button.getPaymentMethod().equals(paymentMethodSpec.getPaymentMethod());
            if (!isSelected) {
                button.setAccountName(null);
            }
        });
    }

    private AutoCompleteComboBox<Account<?, ?>> createComboBox() {
        AutoCompleteComboBox<Account<?, ?>> comboBox = new AutoCompleteComboBox<>(model.getSortedAccountsForPaymentMethod(), Res.get("user.paymentAccounts.selectAccount"));
        comboBox.setPrefWidth(325);
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Account<? extends PaymentMethod<?>, ?> account) {
                return account != null ? account.getAccountName() : "";
            }

            @Override
            public Account<? extends PaymentMethod<?>, ?> fromString(String string) {
                return null;
            }
        });
        return comboBox;
    }

    private void configNoAccountOverlay() {
        VBox overlayContent = new VBox(20);
        overlayContent.setAlignment(Pos.TOP_CENTER);
        overlayContent.getStyleClass().setAll("trade-wizard-feedback-bg");
        overlayContent.setPadding(new Insets(30));
        overlayContent.setMaxWidth(FEEDBACK_WIDTH);

        noAccountOverlay.setVisible(false);
        noAccountOverlay.setAlignment(Pos.TOP_CENTER);

        noAccountOverlayHeadline.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("muSig.createOffer.paymentMethod.noAccountOverlay.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMinWidth(FEEDBACK_WIDTH - 200);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().add("bisq-text-21");

        createAccountButton.setDefaultButton(true);
        HBox buttonsBox = new HBox(10, noAccountOverlayCloseButton, createAccountButton);
        buttonsBox.setAlignment(Pos.CENTER);
        VBox.setMargin(buttonsBox, new Insets(10, 0, 0, 0));
        overlayContent.getChildren().addAll(noAccountOverlayHeadline, subtitleLabel, buttonsBox);
        noAccountOverlay.getChildren().addAll(overlayContent, Spacer.fillVBox());
    }

    private void configMultipleAccountOverlay() {
        VBox overlayContent = new VBox(20);
        overlayContent.setAlignment(Pos.TOP_CENTER);
        overlayContent.getStyleClass().setAll("trade-wizard-feedback-bg");
        overlayContent.setPadding(new Insets(30));
        overlayContent.setMaxWidth(FEEDBACK_WIDTH);

        multipleAccountOverlay.setVisible(false);
        multipleAccountOverlay.setAlignment(Pos.TOP_CENTER);

        multipleAccountOverlayHeadline.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("muSig.createOffer.paymentMethod.multipleAccountOverlay.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMinWidth(FEEDBACK_WIDTH - 200);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().add("bisq-text-21");

        VBox.setMargin(multipleAccountOverlayCloseButton, new Insets(10, 0, 20, 0));
        overlayContent.getChildren().addAll(multipleAccountOverlayHeadline, subtitleLabel, accountSelection, multipleAccountOverlayCloseButton);
        multipleAccountOverlay.getChildren().addAll(overlayContent, Spacer.fillVBox());
    }
}

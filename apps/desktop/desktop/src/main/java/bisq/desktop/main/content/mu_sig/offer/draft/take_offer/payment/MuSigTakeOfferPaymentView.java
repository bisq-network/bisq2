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

package bisq.desktop.main.content.mu_sig.offer.draft.take_offer.payment;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.containers.WizardOverlay;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.mu_sig.offer.draft.components.MuSigPaymentMethodChipButton;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MuSigTakeOfferPaymentView extends View<StackPane, MuSigTakeOfferPaymentModel, MuSigTakeOfferPaymentController> {
    private final GridPane gridPane;
    private final Label headlineLabel, subtitleLabel, noAccountOverlayReasonLabel;
    private final VBox content;
    private final Button noAccountOverlayCloseButton, createAccountButton, multipleAccountsOverlayCloseButton,
            noPaymentMethodSelectedOverlayCloseButton;
    private final AutoCompleteComboBox<Account<?, ?>> singlePaymentMethodAccountSelection, accountSelection;
    private final List<MuSigPaymentMethodChipButton> paymentMethodChipButtons = new ArrayList<>();
    private final WizardOverlay noAccountOverlay, multipleAccountsOverlay, noPaymentMethodSelectedOverlay;
    private Subscription selectedPaymentMethodPin, selectedTogglePin, shouldShowNoAccountOverlayPin,
            shouldShowMultipleAccountsOverlayPin, shouldShowNoPaymentMethodSelectedOverlayPin,
            paymentMethodAdmissibilityPin;

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

        singlePaymentMethodAccountSelection = createComboBox();

        VBox vBox = new VBox(20, subtitleLabel, gridPane, singlePaymentMethodAccountSelection);
        vBox.setAlignment(Pos.CENTER);

        content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        VBox.setVgrow(headlineLabel, Priority.ALWAYS);
        VBox.setVgrow(vBox, Priority.ALWAYS);
        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, vBox, Spacer.fillVBox());

        // noAccount overlay
        noAccountOverlayCloseButton = new Button(Res.get("action.close"));
        createAccountButton = new Button(Res.get("muSig.offer.taker.payment.noAccountOverlay.createAccount"));
        createAccountButton.setDefaultButton(true);
        noAccountOverlayReasonLabel = new Label();
        noAccountOverlay = new WizardOverlay(root)
                .warning()
                .description(createAndGetNoAccountContentBox())
                .buttons(noAccountOverlayCloseButton, createAccountButton)
                .build();

        // multipleAccount overlay
        multipleAccountsOverlayCloseButton = new Button(Res.get("action.close"));
        accountSelection = createComboBox();
        VBox multipleAccountsContentBox = createAndGetContentBox();
        multipleAccountsOverlay = new WizardOverlay(root)
                .warning()
                .description(multipleAccountsContentBox)
                .buttons(multipleAccountsOverlayCloseButton)
                .build();

        // noPaymentMethodSelected overlay
        noPaymentMethodSelectedOverlayCloseButton = new Button(Res.get("action.close"));
        noPaymentMethodSelectedOverlay = new WizardOverlay(root)
                .warning()
                .headlineFromI18nKey("muSig.offer.taker.payment.noPaymentMethodSelectedWizardOverlay.title")
                .descriptionFromI18nKey("muSig.offer.taker.payment.noPaymentMethodSelectedWizardOverlay.subtitle")
                .buttons(noPaymentMethodSelectedOverlayCloseButton)
                .build();

        StackPane.setMargin(content, new Insets(40));
        root.getChildren().addAll(content, noAccountOverlay, multipleAccountsOverlay, noPaymentMethodSelectedOverlay);
    }

    @Override
    protected void onViewAttached() {
        noAccountOverlay.getHeadlineLabel().textProperty().bind(model.getNoAccountOverlayHeadlineText());
        noAccountOverlayReasonLabel.textProperty().bind(model.getNoAccountOverlayReasonText());
        noAccountOverlayReasonLabel.visibleProperty().bind(model.getNoAccountOverlayReasonText().isNotEmpty());
        noAccountOverlayReasonLabel.managedProperty().bind(noAccountOverlayReasonLabel.visibleProperty());
        multipleAccountsOverlay.getHeadlineLabel().textProperty().bind(model.getMultipleAccountsOverlayHeadlineText());

        shouldShowNoAccountOverlayPin = EasyBind.subscribe(model.getShouldShowNoAccountOverlay(), shouldShow ->
                noAccountOverlay.updateOverlayVisibility(content, shouldShow, controller::onKeyPressedWhileShowingNoAccountOverlay));
        shouldShowMultipleAccountsOverlayPin = EasyBind.subscribe(model.getShouldShowMultipleAccountsOverlay(), shouldShow ->
                multipleAccountsOverlay.updateOverlayVisibility(content, shouldShow, controller::onKeyPressedWhileShowingMultipleAccountsOverlay));
        shouldShowNoPaymentMethodSelectedOverlayPin = EasyBind.subscribe(model.getShouldShowNoPaymentMethodSelectedOverlay(), shouldShow ->
                noPaymentMethodSelectedOverlay.updateOverlayVisibility(content, shouldShow, controller::onKeyPressedWhileShowingNoPaymentMethodSelectedOverlay));

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

        paymentMethodAdmissibilityPin = EasyBind.subscribe(model.getPaymentMethodAdmissibilityVersion(), version -> {
            paymentMethodChipButtons.forEach(this::applyAdmissibility);
            applySinglePaymentMethodComboAdmissibility();
            reconcileSelectionVisuals();
        });

        createAccountButton.setOnAction(e -> controller.onOpenCreateAccountScreen());
        noAccountOverlayCloseButton.setOnAction(e -> controller.onCloseNoAccountOverlay());
        multipleAccountsOverlayCloseButton.setOnAction(e -> controller.onCloseMultipleAccountsOverlay());
        noPaymentMethodSelectedOverlayCloseButton.setOnAction(e -> controller.onCloseNoPaymentMethodSelectedOverlay());
        accountSelection.setOnChangeConfirmed(e -> accountSelectionConfirmed());

        root.setOnMousePressed(e -> root.requestFocus());

        if (model.isSinglePaymentMethod()) {
            singlePaymentMethodAccountSelection.setDescription(model.getSinglePaymentMethodAccountSelectionDescription());
            singlePaymentMethodAccountSelection.getSelectionModel().select(model.getSelectedAccount().get());
            singlePaymentMethodAccountSelection.setOnChangeConfirmed(e -> {
                Account<?, ?> account = singlePaymentMethodAccountSelection.getSelectionModel().getSelectedItem();
                if (account != null) {
                    // Admissibility is decided by the controller against the domain; on a
                    // rejected pick the version signal reconciles the label and the combo.
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
        noAccountOverlay.getHeadlineLabel().textProperty().unbind();
        noAccountOverlayReasonLabel.textProperty().unbind();
        noAccountOverlayReasonLabel.visibleProperty().unbind();
        noAccountOverlayReasonLabel.managedProperty().unbind();
        multipleAccountsOverlay.getHeadlineLabel().textProperty().unbind();

        shouldShowNoAccountOverlayPin.unsubscribe();
        shouldShowMultipleAccountsOverlayPin.unsubscribe();
        shouldShowNoPaymentMethodSelectedOverlayPin.unsubscribe();
        selectedPaymentMethodPin.unsubscribe();
        selectedTogglePin.unsubscribe();
        paymentMethodAdmissibilityPin.unsubscribe();

        paymentMethodChipButtons.forEach(MuSigPaymentMethodChipButton::dispose);

        createAccountButton.setOnAction(null);
        noAccountOverlayCloseButton.setOnAction(null);
        multipleAccountsOverlayCloseButton.setOnAction(null);
        noPaymentMethodSelectedOverlayCloseButton.setOnAction(null);
        accountSelection.setOnChangeConfirmed(null);
        singlePaymentMethodAccountSelection.setOnChangeConfirmed(null);

        root.setOnKeyPressed(null);
        root.setOnMousePressed(null);
    }

    private void setUpAndFillPaymentMethods() {
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
            MuSigPaymentMethodChipButton button = new MuSigPaymentMethodChipButton(paymentMethod);
            button.setToggleGroup(model.getToggleGroup());
            PaymentMethodSpec<?> paymentMethodSpec = model.getSelectedPaymentMethodSpec().get();
            boolean isSelected = paymentMethodSpec != null && paymentMethod.equals(paymentMethodSpec.getPaymentMethod());
            button.setSelected(isSelected);
            if (!paymentMethod.getShortDisplayString().equals(paymentMethod.getDisplayString())) {
                button.setTooltip(new BisqTooltip(paymentMethod.getDisplayString()));
            }
            applyAdmissibility(button);
            List<Account<?, ?>> accounts = model.getAccountsByPaymentMethod().get(paymentMethod);
            button.setNumAccounts(accounts != null ? accounts.size() : 0);

            Account<?, ?> account = model.getSelectedAccount().get();
            if (accounts != null && account != null && accounts.size() > 1 && account.getPaymentMethod().equals(paymentMethod)) {
                button.setAccountName(account.getAccountName());
            } else {
                button.setAccountName(null);
            }

            button.setOnAction(() -> {
                // An inadmissible chip is dimmed but stays mouse-interactive so its tooltip
                // shows; the controller rejects the toggle against the domain, and the
                // version signal restores the toggle visuals and account labels.
                controller.onTogglePaymentMethod(paymentMethod, button.isSelected());
            });

            col = i % numColumns;
            row = i / numColumns;
            gridPane.add(button, col, row);
            paymentMethodChipButtons.add(button);
        }
    }

    private Optional<MuSigPaymentMethodChipButton> findPaymentMethodChipButton(PaymentMethod<?> paymentMethod) {
        return paymentMethodChipButtons.stream()
                .filter(button -> button.getPaymentMethod().equals(paymentMethod))
                .findAny();
    }

    private void applyAdmissibility(MuSigPaymentMethodChipButton button) {
        PaymentMethod<?> paymentMethod = button.getPaymentMethod();
        boolean isAdmissible = !model.getInadmissiblePaymentMethods().contains(paymentMethod);
        boolean hasAccounts = model.getAccountsByPaymentMethod().containsKey(paymentMethod);
        button.setActive(hasAccounts && isAdmissible);
        button.setInadmissibleReasonTooltip(isAdmissible
                ? null
                : new BisqTooltip(Res.get("muSig.offer.taker.payment.methodNotAdmissible",
                        paymentMethod.getShortDisplayString())));
    }

    // Single-method offers show the account combo instead of the chip grid, so it has to
    // mirror the chip treatment when the method is inadmissible: dimmed, reason as tooltip,
    // still mouse-interactive (a disabled node would not show the tooltip). Picks are
    // rejected in the change-confirmed handler and in the controller.
    private void applySinglePaymentMethodComboAdmissibility() {
        if (!model.isSinglePaymentMethod() || model.getOfferedPaymentMethods().isEmpty()) {
            return;
        }
        PaymentMethod<?> paymentMethod = model.getOfferedPaymentMethods().get(0);
        boolean isAdmissible = !model.getInadmissiblePaymentMethods().contains(paymentMethod);
        singlePaymentMethodAccountSelection.setOpacity(isAdmissible ? 1 : 0.4);
        singlePaymentMethodAccountSelection.setTooltip(isAdmissible
                ? null
                : new BisqTooltip(Res.get("muSig.offer.taker.payment.methodNotAdmissible",
                        paymentMethod.getShortDisplayString())));
    }

    // The version signal also fires when the controller rejects a selection attempt that
    // this view let through on a stale projection; the selection visuals are re-derived
    // from the model so a rejected attempt cannot leave a toggle, account label or combo
    // showing a selection that was never applied. Idempotent when visuals already match.
    private void reconcileSelectionVisuals() {
        PaymentMethodSpec<?> selectedSpec = model.getSelectedPaymentMethodSpec().get();
        PaymentMethod<?> selectedMethod = selectedSpec != null ? selectedSpec.getPaymentMethod() : null;
        Account<?, ?> selectedAccount = model.getSelectedAccount().get();
        paymentMethodChipButtons.forEach(button -> {
            PaymentMethod<?> paymentMethod = button.getPaymentMethod();
            boolean isSelected = paymentMethod.equals(selectedMethod);
            button.setSelected(isSelected);
            List<Account<?, ?>> accounts = model.getAccountsByPaymentMethod().get(paymentMethod);
            boolean showAccountName = isSelected && selectedAccount != null
                    && accounts != null && accounts.size() > 1
                    && selectedAccount.getPaymentMethod().equals(paymentMethod);
            button.setAccountName(showAccountName ? selectedAccount.getAccountName() : null);
        });
        if (model.isSinglePaymentMethod()) {
            // Deferred: a synchronous programmatic change inside the combo's own
            // change-confirmed handler must be avoided. The model is read at execution
            // time - a selection accepted between the version bump and this frame must
            // not be overwritten by a value captured earlier.
            UIThread.runOnNextRenderFrame(() -> {
                Account<?, ?> currentAccount = model.getSelectedAccount().get();
                if (currentAccount != null) {
                    singlePaymentMethodAccountSelection.getSelectionModel().select(currentAccount);
                } else {
                    singlePaymentMethodAccountSelection.getSelectionModel().clearSelection();
                }
            });
        }
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
        AutoCompleteComboBox<Account<?, ?>> comboBox = new AutoCompleteComboBox<>(model.getSortedAccountsForPaymentMethod(), Res.get("paymentAccounts.selectAccount"));
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

    private VBox createAndGetNoAccountContentBox() {
        Label subtitleLabel = new Label(Res.get("muSig.offer.taker.payment.noAccountOverlay.subTitle"));
        subtitleLabel.setMinWidth(WizardOverlay.OVERLAY_WIDTH - 100);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.getStyleClass().addAll("normal-text", "wrap-text", "text-fill-grey-dimmed");

        noAccountOverlayReasonLabel.setMinWidth(WizardOverlay.OVERLAY_WIDTH - 100);
        noAccountOverlayReasonLabel.setMaxWidth(noAccountOverlayReasonLabel.getMinWidth());
        noAccountOverlayReasonLabel.getStyleClass().addAll("normal-text", "wrap-text", "text-fill-grey-dimmed");

        VBox vBox = new VBox(15, subtitleLabel, noAccountOverlayReasonLabel);
        vBox.setPadding(WizardOverlay.TEXT_CONTENT_PADDING);
        return vBox;
    }

    private VBox createAndGetContentBox() {
        Label subtitleLabel = new Label(Res.get("muSig.offer.taker.payment.multipleAccountOverlay.subTitle"));
        subtitleLabel.setMinWidth(WizardOverlay.OVERLAY_WIDTH - 100);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.getStyleClass().addAll("normal-text", "wrap-text", "text-fill-grey-dimmed");

        VBox vBox = new VBox(20, subtitleLabel, accountSelection);
        vBox.setAlignment(Pos.CENTER);
        vBox.setPadding(WizardOverlay.TEXT_CONTENT_PADDING);
        return vBox;
    }

    private void accountSelectionConfirmed() {
        PaymentMethod<?> paymentMethod = model.getPaymentMethodWithMultipleAccounts().get();
        Account<?, ?> account = accountSelection.getSelectionModel().getSelectedItem();
        if (paymentMethod != null && account != null) {
            // Admissibility is decided by the controller against the domain. On a rejected
            // pick the controller leaves the overlay open and the version signal reconciles
            // the chip label the optimistic set below applied.
            findPaymentMethodChipButton(paymentMethod)
                    .ifPresent(button -> button.setAccountName(account.getAccountName()));
            controller.onSelectAccount(account);
            UIThread.runOnNextRenderFrame(() -> accountSelection.getSelectionModel().clearSelection());
        }
    }
}

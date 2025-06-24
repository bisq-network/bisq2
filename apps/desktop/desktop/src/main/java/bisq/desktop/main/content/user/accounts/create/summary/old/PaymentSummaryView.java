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

package bisq.desktop.main.content.user.accounts.create.summary.old;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.monetary.Monetary;
import bisq.common.util.StringUtils;
import bisq.common.util.TextFormatterUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
public class PaymentSummaryView extends View<VBox, PaymentSummaryModel, PaymentSummaryController> {
    private static final int ICON_SIZE = 20;
    private static final int HORIZONTAL_MARGIN = 50;
    private static final int TOOLTIP_MAX_WIDTH = 500;
    private static final double KEY_COLUMN_WIDTH = 180;
    private static final double VALUE_COLUMN_MAX_WIDTH = 600;
    //UI accountNameField width accommodates exactly 27 chars in the current font
    private static final int MAX_ACCOUNT_NAME_LENGTH = 27;

    private final VBox iconContainer;
    private final Label paymentMethodLabel;
    private final StackPane accountNameContainer;
    private final HBox displayModeBox;
    private final HBox editModeBox;
    private final Label accountNameLabel;
    private final MaterialTextField accountNameField;
    private final Button editButton;
    private final Button saveButton;
    private final Button cancelButton;
    private final HBox riskLimitBox;
    private final Label riskValueLabel;
    private final Label tradeLimitValueLabel;
    protected final VBox contentBox;
    private final ScrollPane contentScrollPane;
    private final VBox headerSeparatorContainer;
    private final GridPane overviewGrid;
    private final VBox overviewSection;
    private final BooleanProperty editMode = new SimpleBooleanProperty(false);
    private ChangeListener<Boolean> editModeListener;
    private ChangeListener<Number> contentScrollPaneWidthListener;
    private ChangeListener<Number> contentBoxHeightListener;
    private ListChangeListener<Node> contentBoxChildrenListener;

    public PaymentSummaryView(PaymentSummaryModel model, PaymentSummaryController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        iconContainer = createIconContainer();
        paymentMethodLabel = createPaymentMethodLabel();

        Label accountNameTitleLabel = new Label(Res.get("user.paymentAccounts.summary.accountName") + ":");
        accountNameTitleLabel.getStyleClass().add("account-summary-label-key");

        accountNameLabel = new Label();
        accountNameLabel.getStyleClass().add("account-summary-subtitle");

        editButton = new Button(Res.get("action.edit"));
        editButton.getStyleClass().add("outlined-button");

        displayModeBox = new HBox(10);
        displayModeBox.setAlignment(Pos.CENTER_LEFT);
        displayModeBox.setPrefHeight(20);
        displayModeBox.setMinHeight(20);
        displayModeBox.setMaxHeight(20);
        displayModeBox.getChildren().addAll(accountNameTitleLabel, accountNameLabel, editButton);

        Label editNameTitleLabel = new Label(Res.get("user.paymentAccounts.summary.accountName") + ":");
        editNameTitleLabel.getStyleClass().add("account-summary-label-key");

        accountNameField = createAccountNameField();

        saveButton = new Button(Res.get("action.save"));
        saveButton.getStyleClass().addAll("outlined-button", "green-button");

        cancelButton = new Button(Res.get("action.cancel"));
        cancelButton.getStyleClass().add("outlined-button");

        editModeBox = new HBox(10);
        editModeBox.setAlignment(Pos.CENTER_LEFT);
        editModeBox.setPrefHeight(20);
        editModeBox.setMinHeight(20);
        editModeBox.setMaxHeight(20);
        editModeBox.getChildren().addAll(editNameTitleLabel, accountNameField, saveButton, cancelButton);

        accountNameContainer = createAccountNameContainer();

        Label riskLabel = createKeyLabel(Res.get("user.paymentAccounts.summary.risk") + ":");
        riskValueLabel = createValueLabel("", false);

        Label tradeLimitLabel = createKeyLabel(Res.get("user.paymentAccounts.summary.tradeLimit") + ":");
        tradeLimitValueLabel = createValueLabel("", false);

        riskLimitBox = new HBox(20);
        riskLimitBox.setAlignment(Pos.CENTER_LEFT);
        riskLimitBox.getChildren().addAll(riskLabel, riskValueLabel, tradeLimitLabel, tradeLimitValueLabel);

        VBox headerContent = createUnifiedHeaderContent();
        headerSeparatorContainer = createHeaderSeparator();

        contentBox = createContentBox();
        contentScrollPane = createContentScrollPane();

        overviewGrid = createSectionGrid();
        overviewSection = createStyledSection(Res.get("user.paymentAccounts.summary.accountDetails"), overviewGrid);

        root.getChildren().addAll(headerContent, headerSeparatorContainer, contentScrollPane, Spacer.fillVBox());
    }

    public void updateAccountName(String name) {
        String displayName = StringUtils.toOptional(name).orElse("");
        accountNameLabel.setText(displayName);
        accountNameField.setText(displayName);
    }

    @Override
    protected void onViewAttached() {
        Optional<PaymentMethod<?>> paymentMethodOpt = model.getPaymentMethod();
        if (paymentMethodOpt.isEmpty()) {
            return;
        }

        PaymentMethod<?> paymentMethod = paymentMethodOpt.get();

        paymentMethodLabel.setText(paymentMethod.getDisplayString());
        setPaymentMethodIcon(paymentMethod.getName());

        String accountName = model.getAccountName().orElse("");
        accountNameLabel.setText(accountName);
        accountNameField.setText(accountName);

        editMode.set(false);

        setupRiskDisplay();
        setupLimitDisplay();

        refreshContentLayout();

        setupEditModeHandling();
    }

    @Override
    protected void onViewDetached() {
        cleanupListeners();
    }

    protected void createMethodSpecificSection() {
        Map<String, String> summaryDetails = model.getSummaryDetails();
        if (summaryDetails == null || summaryDetails.isEmpty()) {
            return;
        }

        contentBox.getChildren().clear();

        populateGridFromMap(overviewGrid, summaryDetails);
        contentBox.getChildren().add(overviewSection);
    }

    protected void addBottomSeparator() {
        Region bottomSeparator = new Region();
        bottomSeparator.setMinHeight(1);
        bottomSeparator.setMaxHeight(1);
        bottomSeparator.getStyleClass().add("account-summary-header-separator");

        HBox separatorContainer = new HBox();
        separatorContainer.setAlignment(Pos.CENTER);
        separatorContainer.setPadding(new Insets(5, 0, 0, 0));

        HBox.setHgrow(bottomSeparator, Priority.ALWAYS);
        bottomSeparator.setMaxWidth(Double.MAX_VALUE);

        separatorContainer.getChildren().add(bottomSeparator);

        contentScrollPaneWidthListener = (obs, oldWidth, newWidth) ->
                Platform.runLater(this::updateSeparatorWidths);
        contentScrollPane.widthProperty().addListener(contentScrollPaneWidthListener);

        contentBoxHeightListener = (obs, oldHeight, newHeight) ->
                Platform.runLater(this::updateSeparatorWidths);
        contentBox.heightProperty().addListener(contentBoxHeightListener);

        contentBox.getChildren().add(separatorContainer);

        Platform.runLater(this::updateSeparatorWidths);
    }

    protected VBox createStyledSection(String title, GridPane contentGrid) {
        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.getStyleClass().add("account-summary-section-header");

        VBox section = new VBox(10);
        section.setAlignment(Pos.TOP_LEFT);
        section.getChildren().addAll(titleLabel, contentGrid);
        section.setPadding(new Insets(15, 0, 15, 0));
        section.getStyleClass().add("account-summary-section");

        section.setPrefHeight(Region.USE_COMPUTED_SIZE);
        section.setMaxHeight(Region.USE_PREF_SIZE);

        return section;
    }

    protected GridPane createSectionGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setAlignment(Pos.TOP_LEFT);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(150);
        col1.setPrefWidth(KEY_COLUMN_WIDTH);
        col1.setMaxWidth(KEY_COLUMN_WIDTH);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.NEVER);
        col2.setMinWidth(200);
        col2.setPrefWidth(VALUE_COLUMN_MAX_WIDTH);
        col2.setMaxWidth(VALUE_COLUMN_MAX_WIDTH);

        grid.getColumnConstraints().addAll(col1, col2);
        return grid;
    }

    protected void populateGridFromMap(GridPane grid, Map<String, String> data) {
        grid.getChildren().clear();
        grid.getRowConstraints().clear();

        if (data == null || data.isEmpty()) {
            return;
        }

        int row = 0;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (StringUtils.isEmpty(value)) {
                continue;
            }

            Label keyLabel = createKeyLabel(key + ":");
            Label valueLabel = createTruncatedValueLabel(key, value);

            RowConstraints rowConstraint = new RowConstraints();

            rowConstraint.setMinHeight(20);
            rowConstraint.setPrefHeight(Region.USE_COMPUTED_SIZE);
            rowConstraint.setMaxHeight(Region.USE_PREF_SIZE);
            rowConstraint.setVgrow(Priority.NEVER);

            grid.getRowConstraints().add(rowConstraint);

            GridPane.setConstraints(keyLabel, 0, row);
            GridPane.setConstraints(valueLabel, 1, row);
            grid.getChildren().addAll(keyLabel, valueLabel);
            row++;
        }

        grid.applyCss();
        grid.autosize();
    }

    protected Label createKeyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("account-summary-label-key");
        label.setAlignment(Pos.TOP_LEFT);
        return label;
    }

    protected Label createValueLabel(String text, boolean wrapText) {
        Label label = new Label(text);
        label.getStyleClass().add("account-summary-label-value");
        label.setAlignment(Pos.CENTER_LEFT);
        label.setWrapText(wrapText);
        label.setMaxHeight(20);
        label.setPrefHeight(20);
        label.setMinHeight(20);
        return label;
    }

    protected Label createTruncatedValueLabel(String key, String value) {
        String cleanValue = value.replace("\n", " ").replace("\r", " ").trim();

        Label label = createValueLabel(cleanValue, true);

        label.setMaxWidth(VALUE_COLUMN_MAX_WIDTH);
        label.setMinWidth(50);
        label.setPrefWidth(VALUE_COLUMN_MAX_WIDTH);

        UIThread.run(() -> {
            Text textNode = new Text(cleanValue);
            textNode.setFont(label.getFont());
            if (textNode.getLayoutBounds().getWidth() > VALUE_COLUMN_MAX_WIDTH) {
                String tooltipText = determineTooltipText(key, value);
                addIntelligentTooltip(label, tooltipText);
            }
        });

        return label;
    }

    private void refreshContentLayout() {
        contentBox.getChildren().clear();

        contentScrollPane.setVvalue(0);
        createMethodSpecificSection();

        addBottomSeparator();

        UIThread.run(() -> {
            contentBox.applyCss();
            contentBox.autosize();

            updateSeparatorWidths();
        });
    }

    private void updateSeparatorWidths() {
        double contentHeight = contentBox.getHeight();
        double viewportHeight = contentScrollPane.getViewportBounds().getHeight();

        boolean scrollbarVisible = contentHeight > viewportHeight && contentHeight > 0;

        if (scrollbarVisible) {
            double scrollbarWidth = contentScrollPane.getWidth() - contentScrollPane.getViewportBounds().getWidth();
            double adjustedMargin = HORIZONTAL_MARGIN + scrollbarWidth;

            headerSeparatorContainer.setPadding(new Insets(0, adjustedMargin, 0, HORIZONTAL_MARGIN));
        } else {
            headerSeparatorContainer.setPadding(new Insets(0, HORIZONTAL_MARGIN, 0, HORIZONTAL_MARGIN));
        }
    }

    private VBox createIconContainer() {
        VBox container = new VBox();
        container.setAlignment(Pos.CENTER);
        container.setPrefWidth(ICON_SIZE);
        container.setPrefHeight(ICON_SIZE);
        container.setMinWidth(ICON_SIZE);
        container.setMinHeight(ICON_SIZE);
        container.setMaxWidth(ICON_SIZE);
        container.setMaxHeight(ICON_SIZE);
        return container;
    }

    private Label createPaymentMethodLabel() {
        Label label = new Label();
        label.getStyleClass().add("account-summary-title");
        return label;
    }

    private MaterialTextField createAccountNameField() {
        MaterialTextField field = new MaterialTextField();
        field.setPrefWidth(300);
       // field.useCompactModeWithHeight(30);

        Predicate<String> validator = TextFormatterUtils.safeWithLength(MAX_ACCOUNT_NAME_LENGTH);

        field.getTextInputControl().setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return validator.test(newText) ? change : null;
        }));

        return field;
    }

    private StackPane createAccountNameContainer() {
        StackPane container = new StackPane();
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPrefHeight(22);
        container.setMinHeight(22);
        container.setMaxHeight(22);
        container.getChildren().addAll(editModeBox, displayModeBox);
        return container;
    }

    private VBox createUnifiedHeaderContent() {
        HBox iconBox = new HBox();
        iconBox.setAlignment(Pos.CENTER_LEFT);
        iconBox.getChildren().add(iconContainer);

        HBox paymentMethodBox = new HBox(10);
        paymentMethodBox.setAlignment(Pos.CENTER_LEFT);
        paymentMethodBox.getChildren().addAll(iconBox, paymentMethodLabel);

        HBox riskLimitContainer = new HBox(15);
        riskLimitContainer.setAlignment(Pos.CENTER_RIGHT);
        riskLimitContainer.getStyleClass().add("risk-limit-container");
        riskLimitContainer.setTranslateY(2);
        riskLimitContainer.getChildren().add(riskLimitBox);

        HBox titleLine = new HBox();
        titleLine.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(paymentMethodBox, Priority.ALWAYS);
        titleLine.getChildren().addAll(paymentMethodBox, riskLimitContainer);

        VBox accountDetailsBox = new VBox(10);
        accountDetailsBox.setAlignment(Pos.TOP_LEFT);
        accountDetailsBox.getChildren().add(accountNameContainer);

        VBox headerContent = new VBox(25);
        headerContent.setAlignment(Pos.TOP_LEFT);
        headerContent.setPadding(new Insets(10, HORIZONTAL_MARGIN, 20, HORIZONTAL_MARGIN));
        headerContent.getChildren().addAll(titleLine, accountDetailsBox);

        return headerContent;
    }

    private VBox createHeaderSeparator() {
        Region headerSeparator = new Region();
        headerSeparator.setMinHeight(1);
        headerSeparator.setMaxHeight(1);
        headerSeparator.getStyleClass().add("account-summary-header-separator");

        VBox separatorContainer = new VBox();
        separatorContainer.setPadding(new Insets(0, HORIZONTAL_MARGIN, 0, HORIZONTAL_MARGIN));
        separatorContainer.getChildren().add(headerSeparator);

        return separatorContainer;
    }

    private VBox createContentBox() {
        VBox box = new VBox();
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(0, HORIZONTAL_MARGIN, 25, HORIZONTAL_MARGIN));

        box.setPrefHeight(Region.USE_COMPUTED_SIZE);
        box.setMaxHeight(Region.USE_PREF_SIZE);
        box.setMinHeight(Region.USE_PREF_SIZE);

        box.setFillWidth(true);

        return box;
    }

    private ScrollPane createContentScrollPane() {
        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);

        scrollPane.setFitToHeight(false);

        scrollPane.setPrefViewportHeight(350);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("account-summary-edge-to-edge");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        contentBoxChildrenListener = change -> Platform.runLater(() -> scrollPane.setVvalue(0));
        contentBox.getChildren().addListener(contentBoxChildrenListener);

        return scrollPane;
    }

    private String determineTooltipText(String key, String value) {
        return model.getFullTextForTooltip(key).orElse(value);
    }

    private void addIntelligentTooltip(Label label, String tooltipText) {
        BisqTooltip tooltip = new BisqTooltip();
        tooltip.setText(tooltipText);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(TOOLTIP_MAX_WIDTH);
        Tooltip.install(label, tooltip);
    }

    private void setPaymentMethodIcon(String paymentMethodName) {
        Optional<PaymentMethod<?>> paymentMethodOpt = model.getPaymentMethod();
        if (paymentMethodOpt.isEmpty()) {
            return;
        }

        PaymentMethod<?> paymentMethod = paymentMethodOpt.get();

        if (paymentMethod instanceof FiatPaymentMethod fiatMethod) {
            Node iconNode;
            if (fiatMethod.isCustomPaymentMethod()) {
                iconNode = BisqEasyViewUtils.getCustomPaymentMethodIcon(fiatMethod.getDisplayString());
            } else {
                iconNode = ImageUtil.getImageViewById(paymentMethodName);
            }

            iconContainer.getChildren().clear();
            if (iconNode instanceof ImageView imageView) {
                imageView.setFitWidth(ICON_SIZE);
                imageView.setFitHeight(ICON_SIZE);
                imageView.setPreserveRatio(true);
            }
            iconContainer.getChildren().add(iconNode);
        }
    }

    private String getRiskCssClass(String riskLevel) {
        return switch (riskLevel) {
            case String s when s.equals(Res.get("user.paymentAccounts.summary.risk.low")) -> "risk-low";
            case String s when s.equals(Res.get("user.paymentAccounts.summary.risk.medium")) -> "risk-medium";
            case String s when s.equals(Res.get("user.paymentAccounts.summary.risk.high")) -> "risk-high";
            default -> "risk-high";
        };
    }

    private void setupRiskDisplay() {
        Optional<String> riskLevelOpt = model.getRiskLevel();

        if (riskLevelOpt.isPresent()) {
            String riskLevel = riskLevelOpt.get();
            riskValueLabel.setText(riskLevel);
            riskValueLabel.getStyleClass().removeAll("risk-high", "risk-medium", "risk-low");
            String cssClass = getRiskCssClass(riskLevel);
            if (!cssClass.isEmpty()) {
                riskValueLabel.getStyleClass().add(cssClass);
            }
        } else {
            riskValueLabel.setText(Res.get("user.paymentAccounts.summary.risk.unknown"));
            riskValueLabel.getStyleClass().add("risk-high");
        }
    }

    private void setupLimitDisplay() {
        Optional<Monetary> tradeLimitOpt = model.getTradeLimit();

        if (tradeLimitOpt.isPresent()) {
            String formattedAmount = AmountFormatter.formatAmountWithCode(tradeLimitOpt.get(), true);
            tradeLimitValueLabel.setText(formattedAmount);
        } else {
            tradeLimitValueLabel.setText(Res.get("data.na"));
        }
    }

    private void setupEditModeHandling() {
        displayModeBox.setVisible(true);
        displayModeBox.setManaged(true);
        editModeBox.setVisible(false);
        editModeBox.setManaged(false);

        editButton.setOnAction(e -> editMode.set(true));
        saveButton.setOnAction(e -> {
            controller.onEditAccountName(accountNameField.getText());
            editMode.set(false);
        });
        cancelButton.setOnAction(e -> {
            accountNameField.setText(accountNameLabel.getText());
            editMode.set(false);
        });

        accountNameField.getTextInputControl().setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> saveButton.fire();
                case ESCAPE -> cancelButton.fire();
            }
        });

        editModeListener = (obs, oldVal, newVal) ->
                handleEditModeTransition(newVal);
        editMode.addListener(editModeListener);
    }

    private void handleEditModeTransition(boolean entering) {
        if (entering) {
            displayModeBox.setVisible(false);
            displayModeBox.setManaged(false);
            editModeBox.setVisible(true);
            editModeBox.setManaged(true);

            accountNameContainer.applyCss();
            accountNameContainer.layout();

            UIThread.run(() -> {
                accountNameField.requestFocus();
               // accountNameField.showSelectionLine(true);
            });
        } else {
            displayModeBox.setVisible(true);
            displayModeBox.setManaged(true);
            editModeBox.setVisible(false);
            editModeBox.setManaged(false);
        }
    }

    private void cleanupListeners() {
        if (editModeListener != null) {
            editMode.removeListener(editModeListener);
            editModeListener = null;
        }

        if (contentScrollPaneWidthListener != null) {
            contentScrollPane.widthProperty().removeListener(contentScrollPaneWidthListener);
            contentScrollPaneWidthListener = null;
        }

        if (contentBoxHeightListener != null) {
            contentBox.heightProperty().removeListener(contentBoxHeightListener);
            contentBoxHeightListener = null;
        }

        if (contentBoxChildrenListener != null) {
            contentBox.getChildren().removeListener(contentBoxChildrenListener);
            contentBoxChildrenListener = null;
        }

        editButton.setOnAction(null);
        saveButton.setOnAction(null);
        cancelButton.setOnAction(null);
        accountNameField.getTextInputControl().setOnKeyPressed(null);
    }
}
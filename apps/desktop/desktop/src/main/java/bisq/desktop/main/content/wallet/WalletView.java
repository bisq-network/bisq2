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

package bisq.desktop.main.content.wallet;

import bisq.desktop.main.content.bisq_easy.offerbook.BisqEasyOfferbookController;
import bisq.desktop.main.content.bisq_easy.offerbook.BisqEasyOfferbookModel;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.main.content.ContentTabView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

@Slf4j
public class WalletView extends ContentTabView<WalletModel, WalletController> {

    private static final Insets CARD_CONTAINER_INSETS = new Insets(25, 0, 50, 0);
    private static final Insets CONTAINER_INSETS = new Insets(50, 20, 20, 20);
    private Button createWalletButton;
    private Button restoreWalletButton;

    private Subscription isWalletInitializedPin;
    private final VBox notInitializedBox;

    public WalletView(WalletModel model, WalletController controller) {
        super(model, controller);

        addTab(Res.get("wallet.dashboard"), NavigationTarget.WALLET_DASHBOARD);
        addTab(Res.get("wallet.send"), NavigationTarget.WALLET_SEND);
        addTab(Res.get("wallet.receive"), NavigationTarget.WALLET_RECEIVE);
        addTab(Res.get("wallet.txs"), NavigationTarget.WALLET_TXS);
        addTab(Res.get("wallet.settings"), NavigationTarget.WALLET_SETTINGS);

        notInitializedBox = createNotInitializedUI();


        /*
        ProtectWalletView overflowBox1 = new ProtectWalletView(model, controller);
        BackupSeedsView overflowBox2 = new BackupSeedsView(model, controller);

        createWalletButton.setOnAction(e -> {
            if(!root.getChildren().contains(overflowBox1.getRoot())) {
                root.getChildren().add(overflowBox1.getRoot());
                notInitializedBox.setEffect(new GaussianBlur(7));
            }
        });

        overflowBox1.setOnClose(() -> {
            root.getChildren().remove(overflowBox1.getRoot());
            notInitializedBox.setEffect(null);
        });

        overflowBox1.setOnNext(() -> {
            root.getChildren().remove(overflowBox1.getRoot());
            root.getChildren().add(overflowBox2.getRoot());
        });
         */


    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        isWalletInitializedPin = EasyBind.subscribe(model.getIsWalletInitialized(), isInitialized -> {
            log.info("Wallet initialization status changed: {}", isInitialized);
            if ((boolean) isInitialized) {
                setContentToTabs();
            } else {
                setContentToNotInitialized();
            }
        });
        // Set initial state
        if (model.getIsWalletInitialized().get()) {
            setContentToTabs();
        } else {
            setContentToNotInitialized();
        }
    }

    private void setContentToTabs() {
        root.getChildren().setAll(topBox, lineAndMarker, scrollPane);
        if (model.getView().get() != null) {
            scrollPane.setContent(model.getView().get().getRoot());
        }
    }

    private void setContentToNotInitialized() {
        root.setPadding(new Insets(40, 40, 20, 40));
        root.getChildren().setAll(notInitializedBox);

        createWalletButton.setOnAction(e -> getController().onCreateWallet());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        if (isWalletInitializedPin != null) {
            isWalletInitializedPin.unsubscribe();
            isWalletInitializedPin = null;
        }
    }

    // TODO: This should be a component
    public VBox createInstructionCard(String step, VBox imageContainer, String caption) {
        Label stepNumber = new Label(step);
        stepNumber.getStyleClass().add("very-large-text");

        Label stepCaption = new Label(caption);

        VBox card = new VBox(10);
        card.getChildren().addAll(stepNumber, imageContainer, stepCaption);
        card.setAlignment(Pos.TOP_CENTER);
        card.getStyleClass().add("bisq-card-bg");
        card.setPadding(new Insets(30, 48, 44, 48));

        return card;
    }

    public ImageView createImageView(String src, int width, int height) {
        Image image = new Image(getClass().getResourceAsStream(src));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        return imageView;
    }


    public HBox createHorizontalStars() {
        String starImageSrc = "/images/icons/stars/star-white@2x.png";
        HBox horizontalStarsContainer = new HBox(5);
        horizontalStarsContainer.setAlignment(Pos.CENTER);
        horizontalStarsContainer.getChildren().addAll(createImageView(starImageSrc, 9, 9), createImageView(starImageSrc, 9, 9), createImageView(starImageSrc, 9, 9), createImageView(starImageSrc, 9, 9), createImageView(starImageSrc, 9, 9));

        return horizontalStarsContainer;
    }

    public VBox createImageContainer(ImageView imageView, HBox horizontalStarsContainer) {
        VBox imageContainer;
        if(horizontalStarsContainer != null) {
            imageContainer = new VBox(10, imageView, horizontalStarsContainer);
        } else {
            imageContainer = new VBox(10, imageView);
        }
        imageContainer.setMargin(imageView, new Insets(0, 0, 0, 7));
        imageContainer.setAlignment(Pos.CENTER);
        VBox.setVgrow(imageContainer, Priority.ALWAYS);

        return imageContainer;
    }

    public VBox createNotInitializedUI() {
        Label headlineLabel = new Label(Res.get("wallet.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-5");

        Label descriptionLabel = new Label(Res.get("wallet.description"));
        descriptionLabel.getStyleClass().add("bisq-text-1");

        String lockImageSrc = "/images/learn/old/security@2x.png";
        ImageView lockImageView1 = createImageView(lockImageSrc, 80, 80);
        ImageView lockImageView2 = createImageView(lockImageSrc, 80, 80);

        String plantImageSrc = "/images/learn/old/security@2x.png";
        ImageView plantImageView = createImageView(plantImageSrc, 80, 80);

        HBox horizontalStarsContainer1 = createHorizontalStars();
        HBox horizontalStarsContainer2 = createHorizontalStars();

        VBox lockImageContainer1 = createImageContainer(lockImageView1, horizontalStarsContainer1);
        VBox lockImageContainer2 = createImageContainer(lockImageView2, horizontalStarsContainer2);
        VBox plantImageContainer = createImageContainer(plantImageView, null);

        VBox card1 = createInstructionCard("1", lockImageContainer1, Res.get("wallet.instruction.caption1"));
        VBox card2 = createInstructionCard("2", plantImageContainer, Res.get("wallet.instruction.caption2"));
        VBox card3 = createInstructionCard("3", lockImageContainer2, Res.get("wallet.instruction.caption3"));

        HBox cardContainer = new HBox(25);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setPadding(CARD_CONTAINER_INSETS);
        cardContainer.getChildren().addAll(card1, card2, card3);

        restoreWalletButton = new Button(Res.get("wallet.button.restoreWallet"));

        createWalletButton = new Button(Res.get("wallet.button.createWallet"));
        createWalletButton.setDefaultButton(true);

        HBox buttons = new HBox(30, restoreWalletButton, createWalletButton);
        buttons.setAlignment(Pos.CENTER);

        VBox vBox = new VBox(20, headlineLabel, descriptionLabel, cardContainer, buttons);
        vBox.setPadding(CONTAINER_INSETS);
        vBox.setAlignment(Pos.CENTER);
        vBox.getStyleClass().add("bisq-common-bg");
        VBox.setVgrow(vBox, Priority.ALWAYS);

        return vBox;

    }

    private WalletModel getModel() {
        return (WalletModel) model;
    }

    private WalletController getController() {
        return (WalletController) controller;
    }
}

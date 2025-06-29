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

package bisq.desktop.overlay.onboarding.create_profile;

import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyBundleService;
import bisq.security.pow.ProofOfWork;
import bisq.user.identity.NymIdGenerator;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class CreateProfileController implements Controller {
    private static final int CURRENT_AVATARS_VERSION = CatHash.currentAvatarsVersion();

    protected final CreateProfileModel model;
    @Getter
    protected final CreateProfileView view;
    protected final UserIdentityService userIdentityService;
    protected final KeyBundleService keyBundleService;
    protected final IdentityService identityService;
    private final OverlayController overlayController;
    protected Optional<CompletableFuture<ProofOfWork>> mintNymProofOfWorkFuture = Optional.empty();
    protected Subscription nickNameSubscription;

    public CreateProfileController(ServiceProvider serviceProvider) {
        keyBundleService = serviceProvider.getSecurityService().getKeyBundleService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        identityService = serviceProvider.getIdentityService();
        overlayController = OverlayController.getInstance();

        model = getGenerateProfileModel();
        view = getGenerateProfileView();
    }

    protected CreateProfileView getGenerateProfileView() {
        return new CreateProfileView(model, this);
    }

    protected CreateProfileModel getGenerateProfileModel() {
        return new CreateProfileModel();
    }

    @Override
    public void onActivate() {
        overlayController.setEnterKeyHandler(this::onCreateUserProfile);
        overlayController.setUseEscapeKeyHandler(false);

        nickNameSubscription = EasyBind.subscribe(model.getNickName(),
                nickName -> model.getCreateProfileButtonDisabled().set(
                        model.getCreateProfileProgress().get() == -1 ||
                                nickName == null ||
                                nickName.trim().isEmpty()));
        onRegenerate();
    }

    @Override
    public void onDeactivate() {
        overlayController.setEnterKeyHandler(null);
        overlayController.setUseEscapeKeyHandler(true);
        if (nickNameSubscription != null) {
            nickNameSubscription.unsubscribe();
        }
        // Does only cancel downstream calls not actual running task
        // We pass the isCanceled flag to stop the running task
        mintNymProofOfWorkFuture.ifPresent(future -> future.cancel(true));
    }

    protected void onCreateUserProfile() {
        if (model.getCreateProfileButtonDisabled().get()) {
            return;
        }

        model.getCreateProfileProgress().set(-1);
        model.getCreateProfileButtonDisabled().set(true);
        model.getReGenerateButtonDisabled().set(true);

        String nickName = model.getNickName().get().trim();
        if (nickName.length() > UserProfile.MAX_LENGTH_NICK_NAME) {
            new Popup().warning(Res.get("onboarding.createProfile.nickName.tooLong", UserProfile.MAX_LENGTH_NICK_NAME)).show();
            return;
        }

        try {
            userIdentityService.createAndPublishNewUserProfile(
                            nickName,
                            model.getKeyPair().orElseThrow(),
                            model.getPubKeyHash().orElseThrow(),
                            model.getProofOfWork().orElseThrow(),
                            CURRENT_AVATARS_VERSION,
                            "",
                            "")
                    .whenComplete((chatUserIdentity, throwable) -> UIThread.run(() -> {
                        if (throwable == null) {
                            model.getCreateProfileProgress().set(0);
                            next();
                        } else {
                            new Popup().error(throwable).show();
                        }
                    }));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    void onRegenerate() {
        generateNewKeyPair();
    }

    void generateNewKeyPair() {
        setPreGenerateState();
        KeyPair keyPair = keyBundleService.generateKeyPair();
        model.setKeyPair(Optional.of(keyPair));
        byte[] pubKeyHash = DigestUtil.hash(keyPair.getPublic().getEncoded());
        model.setPubKeyHash(Optional.of(pubKeyHash));
        mintNymProofOfWorkFuture = Optional.of(createProofOfWork(pubKeyHash));
    }

    private CompletableFuture<ProofOfWork> createProofOfWork(byte[] pubKeyHash) {
        long ts = System.currentTimeMillis();
        return CompletableFuture.supplyAsync(() -> userIdentityService.mintNymProofOfWork(pubKeyHash))
                .thenApply(proofOfWork -> {
                    long powDuration = System.currentTimeMillis() - ts;
                    log.info("Proof of work creation completed after {} ms", powDuration);
                    createSimulatedDelay(powDuration);
                    UIThread.run(() -> {
                        model.setProofOfWork(Optional.of(proofOfWork));
                        byte[] powSolution = proofOfWork.getSolution();
                        String nym = NymIdGenerator.generate(pubKeyHash, powSolution);
                        Image image = CatHash.getImage(pubKeyHash,
                                powSolution,
                                CURRENT_AVATARS_VERSION,
                                CreateProfileModel.CAT_HASH_IMAGE_SIZE);
                        model.getNym().set(nym);
                        model.getCatHashImage().set(image);
                        model.getPowProgress().set(0);
                        model.getCatHashIconVisible().set(true);
                        model.getReGenerateButtonDisabled().set(false);
                    });
                    return proofOfWork;
                });
    }

    private void next() {
        Navigation.navigateTo(NavigationTarget.MAIN);
        UIThread.runOnNextRenderFrame(() -> {
            OverlayController.hide();
            Navigation.navigateTo(NavigationTarget.DASHBOARD);
        });
    }

    private void createSimulatedDelay(long powDuration) {
        try {
            // Proof of work creation for difficulty 65536 takes about 50 ms to 100 ms on a 4 GHz Intel Core i7.
            // Target duration would be 500-2000 ms, but it is hard to find the right difficulty that works 
            // well also for low-end CPUs. So we take a rather safe lower difficulty value and add here some 
            // delay to not have a too fast flicker-effect in the UI when recreating the nym.
            // We add a min delay of 200 ms with some randomness to make the usage of the proof of work more
            // visible. 
            int random = new Random().nextInt(1000);
            // Limit to 200-2000 ms
            Thread.sleep(Math.min(2000, Math.max(200, (200 + random - powDuration))));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void setPreGenerateState() {
        model.getCatHashImage().set(null);
        model.getCatHashIconVisible().set(false);
        model.getReGenerateButtonDisabled().set(true);
        model.getPowProgress().set(-1);
        model.getNym().set(Res.get("onboarding.createProfile.nym.generating"));
    }
}

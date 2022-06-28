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

package bisq.desktop.primary.overlay.onboarding.profile;

import bisq.application.DefaultApplicationService;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.security.DigestUtil;
import bisq.security.KeyPairService;
import bisq.security.pow.ProofOfWorkService;
import bisq.social.user.ChatUserService;
import bisq.social.user.NymIdGenerator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.security.KeyPair;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class GenerateProfileController implements Controller {
    protected final GenerateProfileModel model;
    @Getter
    protected final GenerateProfileView view;
    protected final ChatUserService chatUserService;
    protected final KeyPairService keyPairService;
    protected final ProofOfWorkService proofOfWorkService;
    protected Optional<CompletableFuture<Void>> mintNymProofOfWorkFuture = Optional.empty();
    protected Subscription nickNameSubscription;

    public GenerateProfileController(DefaultApplicationService applicationService) {
        keyPairService = applicationService.getKeyPairService();
        proofOfWorkService = applicationService.getSecurityService().getProofOfWorkService();
        chatUserService = applicationService.getChatUserService();

        model = getGenerateProfileModel();
        view = getGenerateProfileView();
    }

    protected GenerateProfileView getGenerateProfileView() {
        return new GenerateProfileView(model, this);
    }

    protected GenerateProfileModel getGenerateProfileModel() {
        return new GenerateProfileModel();
    }

    @Override
    public void onActivate() {
        nickNameSubscription = EasyBind.subscribe(model.getNickName(),
                nickName -> {
                    TempIdentity tempIdentity = model.getTempIdentity().get();
                    if (tempIdentity != null) {
                        model.getNymId().set(tempIdentity.getProfileId());
                    }

                    model.getCreateProfileButtonDisabled().set(model.getCreateProfileProgress().get() == -1 ||
                            nickName == null || nickName.isEmpty());
                });
        // model.getCreateProfileButtonMouseTransparent().set(true);
        onRegenerate();
    }

    @Override
    public void onDeactivate() {
        if (nickNameSubscription != null) {
            nickNameSubscription.unsubscribe();
        }
        // Does only cancel downstream calls not actual running task
        // We pass the isCanceled flag to stop the running task
        mintNymProofOfWorkFuture.ifPresent(future -> future.cancel(true));
    }

    void onCreateUserProfile() {
        model.getCreateProfileProgress().set(-1);
        TempIdentity tempIdentity = model.getTempIdentity().get();
        chatUserService.createNewInitializedUserProfile(tempIdentity.getProfileId(),
                        model.getNickName().get(),
                        tempIdentity.getTempKeyId(),
                        tempIdentity.getTempKeyPair(),
                        tempIdentity.getProofOfWork(),
                        "",
                        "")
                .thenCompose(chatUserService::publishNewChatUser)
                .thenAccept(chatUserIdentity -> UIThread.run(() -> {
                    Navigation.navigateTo(NavigationTarget.MAIN);
                    UIThread.runOnNextRenderFrame(this::navigateNext);
                    model.getCreateProfileProgress().set(0);
                }));
    }

    protected void navigateNext() {
        OverlayController.hide();
        Navigation.navigateTo(NavigationTarget.DASHBOARD);
    }

    void onRegenerate() {
        KeyPair tempKeyPair = keyPairService.generateKeyPair();
        byte[] pubKeyHash = DigestUtil.hash(tempKeyPair.getPublic().getEncoded());
        model.getRoboHashImage().set(null);
        model.getRoboHashIconVisible().set(false);
        model.getReGenerateButtonMouseTransparent().set(true);
        // model.getCreateProfileButtonDisabled().set(true);
        model.getPowProgress().set(-1);
        model.getNymId().set(Res.get("generateNym.nymId.generating"));
        long ts = System.currentTimeMillis();
        mintNymProofOfWorkFuture = Optional.of(proofOfWorkService.mintNymProofOfWork(pubKeyHash)
                .thenApply(proofOfWork -> {
                    log.info("Proof of work creation completed after {} ms", System.currentTimeMillis() - ts);
                    try {
                        // Proof of work creation for difficulty 65536 takes about 50 ms to 100 ms on a 4 GHz Intel Core i7.
                        // Target duration would be 500-2000 ms, but it is hard to find the right difficulty that works 
                        // well also for low-end CPUs. So we take a rather safe lower difficulty value and add here some 
                        // delay to not have a too fast flicker-effect in the UI when recreating the nym.
                        // We add a min delay of 200 ms with some randomness to make the usage of the proof of work more
                        // visible. 
                        long passed = System.currentTimeMillis() - ts;
                        int random = new Random().nextInt(1000);
                        // Limit to 200-2000 ms
                        Thread.sleep(Math.min(2000, Math.max(200, (200 + random - passed))));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return proofOfWork;
                })
                .thenAccept(proofOfWork -> {
                    UIThread.run(() -> {
                        model.getRoboHashImage().set(RoboHash.getImage(proofOfWork.getPayload()));
                        model.getNymId().set(NymIdGenerator.fromHash(proofOfWork.getPayload()));

                        model.getTempIdentity().set(new TempIdentity(model.getNymId().get(),
                                StringUtils.createUid(),
                                tempKeyPair,
                                proofOfWork));

                        model.getPowProgress().set(0);
                        model.getRoboHashIconVisible().set(true);
                        model.getReGenerateButtonMouseTransparent().set(false);
                        // model.getCreateProfileButtonMouseTransparent().set(false);
                    });
                }));
    }
}

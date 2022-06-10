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
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class CreateProfileController implements Controller {
    private final CreateProfileModel model;
    @Getter
    private final CreateProfileView view;
    private final ChatUserService chatUserService;
    private final KeyPairService keyPairService;
    private final ProofOfWorkService proofOfWorkService;
    private Subscription nickNameSubscription;
    private Optional<CompletableFuture<Void>> mintNymProofOfWorkFuture = Optional.empty();

    public CreateProfileController(DefaultApplicationService applicationService) {
        keyPairService = applicationService.getKeyPairService();
        proofOfWorkService = applicationService.getSecurityService().getProofOfWorkService();
        chatUserService = applicationService.getChatUserService();

        model = new CreateProfileModel();
        view = new CreateProfileView(model, this);
    }

    @Override
    public void onActivate() {
        onCreateTempIdentity();

        nickNameSubscription = EasyBind.subscribe(model.nickName,
                nickName -> model.createProfileButtonDisable.set(nickName == null || nickName.isEmpty() || !model.roboHashIconVisible.get()));
    }

    @Override
    public void onDeactivate() {
        nickNameSubscription.unsubscribe();

        // Does only cancel downstream calls not actual running task
        // We pass the isCanceled flag to stop the running task
        mintNymProofOfWorkFuture.ifPresent(future -> future.cancel(true));
    }

    void onCreateNymProfile() {
        if (model.tempKeyPair != null) {
            String profileId = model.nymId.get();
            chatUserService.createNewInitializedUserProfile(profileId,
                            model.nickName.get(),
                            model.tempKeyId,
                            model.tempKeyPair,
                            model.proofOfWork)
                    .thenCompose(chatUserService::publishNewChatUser)
                    .thenAccept(chatUserIdentity -> UIThread.run(() -> {
                        checkArgument(chatUserIdentity.getIdentity().domainId().equals(profileId));
                        model.createProfileButtonDisable.set(false);
                        Navigation.navigateTo(NavigationTarget.ONBOARDING_BISQ_EASY);
                    }));
        }
    }

    void onCreateTempIdentity() {
        KeyPair tempKeyPair = keyPairService.generateKeyPair();
        byte[] pubKeyHash = DigestUtil.hash(tempKeyPair.getPublic().getEncoded());
        model.roboHashImage.set(null);
        model.roboHashIconVisible.set(false);
        model.createProfileButtonDisable.set(true);
        model.powProgress.set(-1);
        model.nymId.set(Res.get("initNymProfile.nymId.generating"));
        long ts = System.currentTimeMillis();
        mintNymProofOfWorkFuture = Optional.of(proofOfWorkService.mintNymProofOfWork(pubKeyHash)
                .thenAccept(proofOfWork -> {
                    UIThread.run(() -> {
                        log.info("Proof of work creation completed after {} ms", System.currentTimeMillis() - ts);
                        model.proofOfWork = proofOfWork;
                        model.tempKeyId = StringUtils.createUid();
                        model.tempKeyPair = tempKeyPair;

                        model.roboHashImage.set(RoboHash.getImage(proofOfWork.getPayload()));
                        model.nymId.set(NymIdGenerator.fromHash(proofOfWork.getPayload()));

                        model.powProgress.set(0);
                        model.roboHashIconVisible.set(true);
                        model.createProfileButtonDisable.set(model.nickName.get() == null || model.nickName.get().isEmpty());
                        //  boolean result = proofOfWorkService.verify(proofOfWork, itemId, ownerId, difficulty);
                    });
                }));
    }
}

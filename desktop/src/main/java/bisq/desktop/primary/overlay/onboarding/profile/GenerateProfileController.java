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
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.identity.profile.NymIdGenerator;
import bisq.security.DigestUtil;
import bisq.security.KeyPairService;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.ProofOfWorkService;
import bisq.social.user.ChatUserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
public class GenerateProfileController implements Controller {
    protected final GenerateProfileModel model;
    @Getter
    protected final GenerateProfileView view;
    protected final ChatUserService chatUserService;
    protected final KeyPairService keyPairService;
    protected final ProofOfWorkService proofOfWorkService;
    protected final IdentityService identityService;
    protected Optional<CompletableFuture<ProofOfWork>> mintNymProofOfWorkFuture = Optional.empty();
    protected Subscription nickNameSubscription;
    protected final List<Identity> pooledIdentities = new ArrayList<>();
    protected boolean pooledIdentitiesInitialized;

    public GenerateProfileController(DefaultApplicationService applicationService) {
        keyPairService = applicationService.getKeyPairService();
        proofOfWorkService = applicationService.getSecurityService().getProofOfWorkService();
        chatUserService = applicationService.getSocialService().getChatUserService();
        identityService = applicationService.getIdentityService();

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
        if (!pooledIdentitiesInitialized) {
            pooledIdentitiesInitialized = true;
            pooledIdentities.addAll(identityService.getPool());
        }

        nickNameSubscription = EasyBind.subscribe(model.getNickName(),
                nickName -> model.getCreateProfileButtonDisabled().set(
                        model.getCreateProfileProgress().get() == -1 ||
                                nickName == null ||
                                nickName.isEmpty()));
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

    protected void onCreateUserProfile() {
        model.getCreateProfileProgress().set(-1);
        model.getCreateProfileButtonDisabled().set(true);
        model.getReGenerateButtonDisabled().set(true);

        if (model.getKeyPairAndId().isPresent()) {
            KeyPairAndId keyPairAndId = model.getKeyPairAndId().get();
            chatUserService.createAndPublishNewChatUserIdentity(
                            model.getNickName().get(),
                            keyPairAndId.getKeyId(),
                            keyPairAndId.getKeyPair(),
                            model.getProofOfWork().orElseThrow(),
                            "",
                            "")
                    .whenComplete((chatUserIdentity, throwable) -> UIThread.run(() -> {
                        if (throwable == null) {
                            model.getCreateProfileProgress().set(0);
                            close();
                        } else {
                            //todo
                        }
                    }));
        } else if (model.getPooledIdentity().isPresent()) {
            Identity pooledIdentity = model.getPooledIdentity().get();
            chatUserService.createAndPublishNewChatUserIdentity(
                    pooledIdentity,
                    model.getNickName().get(),
                    model.getProofOfWork().orElseThrow(),
                    "",
                    "");
            model.getCreateProfileProgress().set(0);
            close();
        }
    }

    void onRegenerate() {
        // We try first our pooled identities. If user has skipped all those we create new temporary keypair
        if (!pooledIdentities.isEmpty()) {
            Identity pooledIdentity = pooledIdentities.remove(0);
            setPreGenerateState();

            byte[] pubKeyHash = pooledIdentity.getPubKeyHash();
            model.setPubKeyHash(Optional.of(pubKeyHash));
            mintNymProofOfWorkFuture = Optional.of(createProofOfWork(pubKeyHash)
                    .whenComplete((proofOfWork, t) ->
                            UIThread.run(() -> model.setPooledIdentity(Optional.of(pooledIdentity)))));

            runAsync(() -> createSimulatedDelay(0))
                    .whenComplete((__, t) ->
                            UIThread.run(() -> {
                                model.setPooledIdentity(Optional.of(pooledIdentity));
                                String profileId = NymIdGenerator.fromHash(pooledIdentity.getPubKeyHash());
                                applyIdentityData(pooledIdentity.getPubKeyHash(), profileId);
                            }));
        } else {
            generateNewKeyPair();
        }
    }

    void generateNewKeyPair() {
        setPreGenerateState();
        KeyPair keyPair = keyPairService.generateKeyPair();
        byte[] pubKeyHash = DigestUtil.hash(keyPair.getPublic().getEncoded());
        model.setPubKeyHash(Optional.of(pubKeyHash));
        // mintNymProofOfWork is executed on a ForkJoinPool thread
        mintNymProofOfWorkFuture = Optional.of(createProofOfWork(pubKeyHash)
                .whenComplete((proofOfWork, t) ->
                        UIThread.run(() -> {
                            KeyPairAndId keyPairAndId = new KeyPairAndId(model.getProfileId().get(), keyPair);
                            model.setKeyPairAndId(Optional.of(keyPairAndId));
                        })));
    }

    private CompletableFuture<ProofOfWork> createProofOfWork(byte[] pubKeyHash) {
        long ts = System.currentTimeMillis();
        return proofOfWorkService.mintNymProofOfWork(pubKeyHash)
                .thenApply(proofOfWork -> {
                    long powDuration = System.currentTimeMillis() - ts;
                    log.info("Proof of work creation completed after {} ms", powDuration);
                    createSimulatedDelay(powDuration);
                    UIThread.run(() -> {
                        model.setProofOfWork(Optional.of(proofOfWork));
                        String profileId = NymIdGenerator.fromHash(pubKeyHash);
                        applyIdentityData(pubKeyHash, profileId);
                    });
                    return proofOfWork;
                });
    }

    private void close() {
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
        model.getRoboHashImage().set(null);
        model.getRoboHashIconVisible().set(false);
        model.getReGenerateButtonDisabled().set(true);
        model.getPowProgress().set(-1);
        model.getProfileId().set(Res.get("generateNym.nymId.generating"));
    }

    private void applyIdentityData(byte[] pubKeyHash, String profileId) {
        model.getProfileId().set(profileId);
        model.getRoboHashImage().set(RoboHash.getImage(pubKeyHash));
        model.getPowProgress().set(0);
        model.getRoboHashIconVisible().set(true);
        model.getReGenerateButtonDisabled().set(false);
    }
}

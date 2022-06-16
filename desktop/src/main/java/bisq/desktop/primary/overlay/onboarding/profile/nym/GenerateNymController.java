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

package bisq.desktop.primary.overlay.onboarding.profile.nym;

import bisq.application.DefaultApplicationService;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.overlay.onboarding.profile.TempIdentity;
import bisq.i18n.Res;
import bisq.security.DigestUtil;
import bisq.security.KeyPairService;
import bisq.security.pow.ProofOfWorkService;
import bisq.social.user.ChatUserService;
import bisq.social.user.NymIdGenerator;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class GenerateNymController implements Controller {
    private final GenerateNymModel model;
    @Getter
    private final GenerateNymView view;
    private final ChatUserService chatUserService;
    private final KeyPairService keyPairService;
    private final ProofOfWorkService proofOfWorkService;
    private Optional<CompletableFuture<Void>> mintNymProofOfWorkFuture = Optional.empty();

    public GenerateNymController(DefaultApplicationService applicationService) {
        keyPairService = applicationService.getKeyPairService();
        proofOfWorkService = applicationService.getSecurityService().getProofOfWorkService();
        chatUserService = applicationService.getChatUserService();

        model = new GenerateNymModel();
        view = new GenerateNymView(model, this);
    }

    public ReadOnlyObjectProperty<TempIdentity> getTempIdentity() {
        return model.getTempIdentity();
    }

    @Override
    public void onActivate() {
        onCreateTempIdentity();
    }

    @Override
    public void onDeactivate() {
        // Does only cancel downstream calls not actual running task
        // We pass the isCanceled flag to stop the running task
        mintNymProofOfWorkFuture.ifPresent(future -> future.cancel(true));
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.ONBOARDING_ADD_NICKNAME);
    }

    void onCreateTempIdentity() {
        KeyPair tempKeyPair = keyPairService.generateKeyPair();
        byte[] pubKeyHash = DigestUtil.hash(tempKeyPair.getPublic().getEncoded());
        model.getRoboHashImage().set(null);
        model.getRoboHashIconVisible().set(false);
        model.getReGenerateButtonMouseTransparent().set(true);
        model.getCreateProfileButtonMouseTransparent().set(true);
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
                        model.getCreateProfileButtonMouseTransparent().set(false);
                    });
                }));
    }
}

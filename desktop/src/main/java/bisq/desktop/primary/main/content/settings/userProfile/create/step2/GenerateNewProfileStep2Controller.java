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

package bisq.desktop.primary.main.content.settings.userProfile.create.step2;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.onboarding.profile.TempIdentity;
import bisq.identity.Identity;
import bisq.security.pow.ProofOfWork;
import bisq.social.user.ChatUserService;
import bisq.social.user.NymIdGenerator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class GenerateNewProfileStep2Controller implements InitWithDataController<GenerateNewProfileStep2Controller.InitData> {

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class InitData {
        private final Optional<TempIdentity> tempIdentity;
        private final Optional<Identity> pooledIdentity;
        private final String nickName;

        public InitData(Optional<TempIdentity> tempIdentity,
                        Optional<Identity> pooledIdentity,
                        String nickName) {
            this.tempIdentity = tempIdentity;
            this.pooledIdentity = pooledIdentity;
            this.nickName = nickName;
        }
    }

    protected final GenerateNewProfileStep2Model model;
    @Getter
    protected final GenerateNewProfileStep2View view;
    protected final ChatUserService chatUserService;

    public GenerateNewProfileStep2Controller(DefaultApplicationService applicationService) {
        chatUserService = applicationService.getSocialService().getChatUserService();

        model = createModel();
        view = createView();
    }

    protected GenerateNewProfileStep2View createView() {
        return new GenerateNewProfileStep2View(model, this);
    }

    protected GenerateNewProfileStep2Model createModel() {
        return new GenerateNewProfileStep2Model();
    }

    @Override
    public void initWithData(InitData data) {
        model.setTempIdentity(data.getTempIdentity());
        model.setPooledIdentity(data.getPooledIdentity());
        model.getNickName().set(data.getNickName());
        if (data.getTempIdentity().isPresent()) {
            TempIdentity tempIdentity = data.getTempIdentity().get();
            model.getProfileId().set(tempIdentity.getProfileId());
            model.getRoboHashImage().set(RoboHash.getImage(tempIdentity.getProofOfWork().getPayload()));
        } else if (data.getPooledIdentity().isPresent()) {
            Identity pooledIdentity = data.getPooledIdentity().get();
            ProofOfWork proofOfWork = pooledIdentity.getProofOfWork();
            String profileId = NymIdGenerator.fromHash(proofOfWork.getPayload());
            model.getProfileId().set(profileId);
            model.getRoboHashImage().set(RoboHash.getImage(proofOfWork.getPayload()));
        }
    }

    @Override
    public void onActivate() {
        model.getTerms().set("");
        model.getBio().set("");
    }

    @Override
    public void onDeactivate() {
    }

    void onCancel() {
        OverlayController.hide();
    }

   protected void onSave() {
        model.getCreateProfileProgress().set(-1);
        model.getCreateProfileButtonDisabled().set(true);

        if (model.getTempIdentity().isPresent()) {
            TempIdentity tempIdentity = model.getTempIdentity().get();
            chatUserService.createAndPublishNewChatUserIdentity(tempIdentity.getProfileId(),
                            model.getNickName().get(),
                            tempIdentity.getTempKeyId(),
                            tempIdentity.getTempKeyPair(),
                            tempIdentity.getProofOfWork(),
                            model.getTerms().get(),
                            model.getBio().get())
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
            chatUserService.createAndPublishNewChatUserIdentity(model.getProfileId().get(),
                    pooledIdentity,
                    model.getNickName().get(),
                    model.getTerms().get(),
                    model.getBio().get());
            model.getCreateProfileProgress().set(0);
            close();
        }
    }

    protected void close() {
        OverlayController.hide();
    }
}
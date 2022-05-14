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

package bisq.desktop.primary.main.content.newProfilePopup.initNymProfile;

import bisq.application.DefaultApplicationService;
import bisq.common.data.ByteArray;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.main.content.newProfilePopup.NewProfilePopupModel;
import bisq.security.DigestUtil;
import bisq.security.KeyPairService;
import bisq.social.user.ChatUserService;
import bisq.social.user.NymGenerator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class InitNymProfileController implements Controller {
    private final InitNymProfileModel model;
    @Getter
    private final InitNymProfileView view;
    private final ChatUserService chatUserService;
    private final KeyPairService keyPairService;
    private final NewProfilePopupModel popupModel;

    public InitNymProfileController(DefaultApplicationService applicationService, NewProfilePopupModel popupModel) {
        keyPairService = applicationService.getKeyPairService();
        chatUserService = applicationService.getChatUserService();
        
        this.popupModel = popupModel;

        model = new InitNymProfileModel();
        view = new InitNymProfileView(model, this);
    }

    @Override
    public void onActivate() {
        createTempIdentity();
    }

    @Override
    public void onDeactivate() {
    }

    void createNymProfile() {
        model.createProfileInProgress.set(true);
        String profileId = model.profileId.get();
        chatUserService.createNewInitializedUserProfile(profileId,
                        model.profileId.get(),
                        model.tempKeyId,
                        model.tempKeyPair)
                .thenAccept(userProfile -> UIThread.run(() -> {
                    checkArgument(userProfile.getIdentity().domainId().equals(profileId));
                    model.createProfileInProgress.set(false);
                    popupModel.increaseStep();
                }));
    }

    void createTempIdentity() {
        model.tempKeyId = StringUtils.createUid();
        model.tempKeyPair = keyPairService.generateKeyPair();
        byte[] hash = DigestUtil.hash(model.tempKeyPair.getPublic().getEncoded());
        model.uniqueHashId.set(Base64.getEncoder().encodeToString(hash));
        model.roboHashNode.set(RoboHash.getImage(new ByteArray(hash)));
        model.profileId.set(NymGenerator.fromHash(hash));
    }
}

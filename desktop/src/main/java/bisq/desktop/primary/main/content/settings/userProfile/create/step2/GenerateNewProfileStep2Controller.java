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
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.security.pow.ProofOfWorkService;
import bisq.social.user.ChatUserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class GenerateNewProfileStep2Controller implements Controller {
    protected final GenerateNewProfileStep2Model model;
    @Getter
    protected final GenerateNewProfileStep2View view;
    protected final ChatUserService chatUserService;
    protected final ProofOfWorkService proofOfWorkService;

    protected Subscription nickNameSubscription;

    public GenerateNewProfileStep2Controller(DefaultApplicationService applicationService) {

        chatUserService = applicationService.getSocialService().getChatUserService();
        proofOfWorkService = applicationService.getSecurityService().getProofOfWorkService();

        model = getGenerateNewProfileStep2Model();
        view = getGenerateNewProfileStep2View();
    }

    protected GenerateNewProfileStep2View getGenerateNewProfileStep2View() {
        return new GenerateNewProfileStep2View(model, this);
    }

    protected GenerateNewProfileStep2Model getGenerateNewProfileStep2Model() {
        return new GenerateNewProfileStep2Model();
    }

    @Override
    public void onActivate() {
        model.getNickName().set(chatUserService.getSelectedChatUserIdentity().get().getNickName());
        model.getNymId().set(chatUserService.getSelectedChatUserIdentity().get().getChatUser().getNym());
        model.getRoboHashImage().set(RoboHash.getImage(chatUserService.getSelectedChatUserIdentity().get()
                .getChatUser().getProofOfWork().getPayload()));
    }

    @Override
    public void onDeactivate() {
        if (nickNameSubscription != null) {
            nickNameSubscription.unsubscribe();
        }
    }

    public void onCancel() {
        OverlayController.hide();
    }

    public void onSave(String tac, String credo) {
        String credoFromTemp;
        OverlayController.hide();
        System.out.println("this are the rules__" + tac + "__and this is credo__" + credo);
//        MockChatUser existing = chatUserService.getSelectedChatUserIdentity();
//        MockChatUser newUser = new MockChatUser(existing.getNickName(), tac, credo);
//        model.getSelectedChatUser().set(newUser);
//        model.getChatUsers().remove(existing);
//        model.getChatUsers().add(newUser);
//        model.getIsEditable().set(false);
        credoFromTemp = chatUserService.getSelectedChatUserIdentity().get().getNickName();
        System.out.println("nickname from temp" + credoFromTemp);
    }
}
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

package bisq.desktop.main.content.authorized_role.info;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import bisq.presentation.formatters.BooleanFormatter;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import com.google.common.base.Joiner;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

public class RoleInfo {
    @Getter
    private final Controller controller;

    public RoleInfo(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    public Pane getRoot() {
        return controller.getView().getRoot();
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private final UserIdentityService userIdentityService;
        private final AuthorizedBondedRolesService authorizedBondedRolesService;
        private Pin userIdentityPin;

        private Controller(ServiceProvider serviceProvider) {
            userIdentityService = serviceProvider.getUserService().getUserIdentityService();
            authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();
            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            userIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(userIdentity -> UIThread.run(this::onUserIdentity));
        }

        @Override
        public void onDeactivate() {
            userIdentityPin.unbind();
        }

        private void onUserIdentity() {
            UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
            authorizedBondedRolesService.getBondedRoles().stream()
                    .filter(bondedRole -> selectedUserIdentity != null && selectedUserIdentity.getUserProfile().getId().equals(bondedRole.getAuthorizedBondedRole().getProfileId()))
                    .findAny()
                    .ifPresent(bondedRole -> {
                        model.setIsBanned(BooleanFormatter.toYesNo(bondedRole.isBanned()));
                        AuthorizedBondedRole authorizedBondedRole = bondedRole.getAuthorizedBondedRole();
                        String addressByNetworkType = Joiner.on(", ")
                                .join(authorizedBondedRole.getNetworkId().getAddressByNetworkType().entrySet().stream()
                                        .map(e -> e.getKey() + ": " + e.getValue().getFullAddress())
                                        .collect(Collectors.toList()));
                        model.setAddressByNetworkType(addressByNetworkType);
                        model.setProfileId(authorizedBondedRole.getProfileId());
                        model.setAuthorizedPublicKey(authorizedBondedRole.getAuthorizedPublicKey());
                        model.setBondedRoleType(authorizedBondedRole.getBondedRoleType().getDisplayString());
                        model.setBondUserName(authorizedBondedRole.getBondUserName());
                        model.setSignature(authorizedBondedRole.getSignature());
                        model.setAuthorizedOracleNode(authorizedBondedRole.getAuthorizedOracleNode().map(AuthorizedOracleNode::getPublicKeyHash).orElse(Res.get("data.na")));
                        model.setStaticPublicKeysProvided(BooleanFormatter.toYesNo(authorizedBondedRole.isStaticPublicKeysProvided()));
                        model.setAuthorizedPublicKeys(Joiner.on(", ").join(authorizedBondedRole.getAuthorizedPublicKeys()));
                    });
        }
    }

    @Slf4j
    @Getter
    @Setter
    private static class Model implements bisq.desktop.common.view.Model {
        private String profileId;
        private String authorizedPublicKey;
        private String bondedRoleType;
        private String bondUserName;
        private String signature;
        private String addressByNetworkType;
        private String authorizedOracleNode;
        private String staticPublicKeysProvided;
        private String authorizedPublicKeys;
        private String isBanned;

    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final MaterialTextField profileId, authorizedPublicKey, bondedRoleType, bondUserName, signature,
                addressByNetworkType, authorizedOracleNode, staticPublicKeysProvided, isBanned;
        private final MaterialTextArea authorizedPublicKeys;


        private View(Model model, Controller controller) {
            super(new VBox(10), model, controller);

            root.setAlignment(Pos.TOP_LEFT);
            root.setPadding(new Insets(20, 0, 0, 0));

            Label headline = new Label(Res.get("authorizedRole.roleInfo.headline"));
            headline.getStyleClass().add("bisq-text-headline-2");
            root.getChildren().add(headline);

            bondedRoleType = addFields("authorizedRole.roleInfo.bondedRoleType", false);
            bondUserName = addFields("authorizedRole.roleInfo.bondUserName", true);
            profileId = addFields("authorizedRole.roleInfo.profileId", true);
            signature = addFields("authorizedRole.roleInfo.signature", true);
            addressByNetworkType = addFields("authorizedRole.roleInfo.addressByNetworkType", true);
            isBanned = addFields("authorizedRole.roleInfo.isBanned", false);
            authorizedPublicKey = addFields("authorizedRole.roleInfo.authorizedPublicKey", true);
            authorizedOracleNode = addFields("authorizedRole.roleInfo.authorizedOracleNode", true);
            staticPublicKeysProvided = addFields("authorizedRole.roleInfo.staticPublicKeysProvided", false);

            authorizedPublicKeys = new MaterialTextArea(Res.get("authorizedRole.roleInfo.authorizedPublicKeys"));
            authorizedPublicKeys.setFixedHeight(200);
            root.getChildren().add(authorizedPublicKeys);
            authorizedPublicKeys.showCopyIcon();
        }

        private MaterialTextField addFields(String key, boolean showCopyIcon) {
            MaterialTextField field = new MaterialTextField(Res.get(key));
            if (showCopyIcon) {
                field.showCopyIcon();
            }
            root.getChildren().add(field);
            return field;
        }

        @Override
        protected void onViewAttached() {
            profileId.setText(model.getProfileId());
            authorizedPublicKey.setText(model.getAuthorizedPublicKey());
            bondedRoleType.setText(model.getBondedRoleType());
            bondUserName.setText(model.getBondUserName());
            signature.setText(model.getSignature());
            addressByNetworkType.setText(model.getAddressByNetworkType());
            authorizedOracleNode.setText(model.getAuthorizedOracleNode());
            staticPublicKeysProvided.setText(model.getStaticPublicKeysProvided());
            authorizedPublicKeys.setText(model.getAuthorizedPublicKeys());
            isBanned.setText(model.getIsBanned());
        }

        @Override
        protected void onViewDetached() {
        }
    }
}
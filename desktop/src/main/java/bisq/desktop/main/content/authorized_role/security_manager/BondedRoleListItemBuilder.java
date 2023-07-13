package bisq.desktop.main.content.authorized_role.security_manager;

import bisq.bonded_roles.bonded_role.BondedRole;

public class BondedRoleListItemBuilder {
    private BondedRole bondedRole;
    private SecurityManagerController controller;

    public BondedRoleListItemBuilder setBondedRole(BondedRole bondedRole) {
        this.bondedRole = bondedRole;
        return this;
    }

    public BondedRoleListItemBuilder setController(SecurityManagerController controller) {
        this.controller = controller;
        return this;
    }

    public SecurityManagerView.BondedRoleListItem createBondedRoleListItem() {
        return new SecurityManagerView.BondedRoleListItem(bondedRole, controller);
    }
}
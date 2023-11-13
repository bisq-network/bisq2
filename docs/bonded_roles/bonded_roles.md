# Bonded roles

All important roles and network nodes require a BSQ bond to secure the roles.
The Seed nodes and oracle nodes require at least 1 node which is available by default as otherwise it would be a chicken
and egg problem.
The oracle node manages the registration and verification of the other nodes.

## Developer setup (localhost, regtest)

To register different roles and nodes it requires to run those nodes in that order:

- Bitcoind (regtest for testing)
- Bisq 1 seednode
- Bisq 1 desktop app
- Bisq-daonode
- Bisq 2 seed node
- Bisq 2 Oracle node
- Bisq 2 desktop app

Please read the descriptions in the oracle node document for more details about the setup of the oracle node and
daonode.

1. Make the dao proposals for the bonded roles one wants to register.
2. After the voting cycle is over follow the instructions how to register the roles and nodes.
3. Once a role is successfully registered the left navigation shows an additional Authorized role menu item. Inside that
   screen there are tabs for each registered role in case the selected user profile has multiple roles. Nodes do not
   have a management UI.
4. Use the management screens for exploring the role's use cases.

> _Note: The mediator screen is not implemented yet._

Registered roles are listed with details for verification.
<img src="img/list.png" width="1200"/>



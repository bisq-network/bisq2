# Wallet state

Bisq knows four states of the wallet of the daemon ([Wallet of the daemon](daemon-wallet.md)):

- not set up: this installation has not opened or created a wallet,
- locked: Bisq has the record of a wallet, and the daemon has no wallet open,
- open: the daemon has the wallet open,
- unavailable: the daemon cannot be reached, or it does not offer the wallet service.

The wallet screen follows the state: it offers the setup, asks for the password, shows the wallet, or reports that
the daemon is unavailable and offers to try again.

## Wallet record

Bisq records that the wallet exists when the daemon opened or created it, and at no other time. The record survives a
restart.

Without the record the wallet is not set up, and Bisq does not ask the daemon.

Reason: the daemon does not say whether a wallet exists, and opening creates a wallet where none exists. Only the
record tells an installation without a wallet from one with a closed wallet.

A write of the record which failed is repeated at the next opening and when Bisq shuts down.

Reason: without the record, an installation with a wallet would offer the setup again after the next start.

## Learning the state

With the record, the state follows the daemon. Bisq asks for the balance to learn it. An answer means open. The error
"wallet not open" means locked. An unreachable daemon and a daemon without the wallet service mean unavailable.

Reason: the balance request needs an open wallet and changes nothing. The readiness answer is also negative while an
open wallet synchronizes, so it cannot tell a locked wallet from an open one.

Any other error keeps the state, with one exception: with the record, an error never leaves the state at "not set up".
It becomes "unavailable".

Reason: an installation with a wallet must not be offered the setup.

Open point: until the first answer of the daemon after the start, the state is still "not set up", and the wallet
screen offers the setup (issue #5204).

When another wallet request fails because the wallet is not open or the daemon is unavailable, Bisq asks for the state
again. The failure does not set the state itself, and only the answer to the newest question counts.

Reason: the failure of an earlier request can arrive after the wallet was opened. It must not close it on the screen.

A locked wallet is opened with its password. A wrong password leaves it locked.

Open point: if the daemon lost its wallet, for example because it runs with another wallet directory, the state is
still locked, and the password creates a new wallet without the backup step of the setup.

## Refresh

While MuSig is activated, Bisq asks the daemon every 30 seconds:

- with an open wallet for the balance, and for the transactions when the balance or one of its parts changed,
- with an unavailable daemon for the state.

A new round starts no request while the balance or state request of the previous round still runs. A transactions
request which a changed balance started is not waited for. The check of the state with which the refresh starts counts
as such a request. An answer which arrives after the refresh was stopped starts no further request. Without MuSig Bisq
does not ask the daemon on its own.

Reason: the wallet screens exist only with MuSig, and only then a daemon is expected to run.

After a successful send Bisq asks for the balance and the transactions at once.

An answer which is older than the last applied one is dropped. This holds for the balance and for the transactions.

Reason: a slow answer can arrive after a newer one and would put an old balance on the screen.

## Deadline

Every request to the daemon except a send has a deadline of 60 seconds. A request which runs into it fails with its
own error, and the state stays as it is, with the exception for an installation with a wallet above.

Reason: the daemon can be slow to answer while it synchronizes the wallet. A missing answer says nothing about the
state of the wallet.

A send has no deadline.

Reason: once the daemon works on a send, it builds and broadcasts the transaction without a point at which it could
stop. A send which a deadline ended in Bisq could still pay, and a second attempt would pay twice.

## Connection

The connection to the daemon is not encrypted. Bisq therefore accepts only `localhost` or a loopback IP address as the
host of the daemon, and refuses another host at start.

Reason: the password and the seed words cross this connection.

The setting `wallet.mock` replaces the daemon with a stand-in for work on the screens. The host rule does not apply to
it.

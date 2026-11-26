# Wallet of the daemon

Bisq has no Bitcoin wallet of its own. The wallet belongs to the daemon `musigd`, and Bisq is a client of the
wallet service of the daemon. The wire contract of that service is the file
[`bmp_wallet.proto`](https://github.com/bisq-network/bisq-musig/blob/main/rpc/src/main/proto/bmp_wallet.proto) of the
daemon. Bisq holds a copy of the file. A change of the contract is made in the daemon first and then copied.

This specification defines the rules which Bisq follows as a client. They do not apply to the stand-in which
replaces the daemon for work on the screens.

## Open wallet

The wallet must be open before Bisq uses it. Opening needs the wallet password. The first opening creates the
wallet, and its password is then the password of the wallet.

An operation which needs the wallet fails on a closed wallet with the error "wallet not open". Only the question
whether the wallet is ready is answered for a closed wallet.

Reason: an empty answer from a closed wallet would look like a wallet without funds and without transactions.

## Password

The wallet always has a password. Bisq sends the password with each of these requests:

- open or create the wallet,
- read the seed words,
- send bitcoin.

Bisq does not store the password.

Like the other text fields of Bisq, a password field removes spaces and control characters at the start and the end
and invisible format characters, such as a zero width joiner, when the user leaves the field or presses Enter. The
password of the wallet is the cleaned text. A wallet which another client created with a password that starts or ends
with a space, or that contains such a character, cannot be opened from Bisq.

A wrong password fails with its own error. It is distinct from the error of a closed wallet and from the error of a
request which the daemon refuses as invalid, for example a new password which breaks the password rules of the daemon.
For a refused request Bisq passes on the description which the daemon gives.

Bisq never writes the password or the seed words to its log, as for every secret
([Secrets in logs](../security/secrets-in-logs.md)). The log entry of a failed request contains the name of the call,
the status and the description of the daemon, and never the request.

## Balance

The spendable balance is the confirmed balance plus the trusted pending balance. Trusted pending funds are unconfirmed
outputs which the wallet's own transactions pay to its change addresses. The daemon also reports the untrusted pending
balance, which are unconfirmed payments from others, and the immature balance. Neither counts in the spendable balance,
but the daemon can still spend untrusted pending outputs in a send.

Bisq publishes the spendable balance and its parts as one value, one answer at a time, so they always come from the
same answer of the daemon.

Reason: requests for the balance can overlap. If the total and the parts were two values, a total from one answer
could stand next to parts from another.

## Sending

A send names an address and an amount in satoshis. Bisq sends no fee rate, so the daemon applies its default rate.

Reason: the daemon accepts a fee rate of 0 and records the transaction before it broadcasts it, although nodes do not
relay it.

Bisq refuses a negative amount before it sends the request, with the error of a request which the daemon refuses.

Reason: the amount is unsigned in the wire contract. A negative number would reach the daemon as a very large one.

## Receive addresses

The daemon hands out an unused address with each request. Once enough addresses are unused, it creates no further
address and hands out the existing unused addresses again, one after the other.

A note which the user gives to an address is stored by Bisq and stays with the address. When the daemon hands out an
address again, Bisq shows it with its note, and the note cannot be replaced. Bisq stores a note only for an address
which has none, also when the screen still shows an older answer for that address, and it shows the stored note. An
address without a note can get one, whether it is the address shown when the screen opens or one from "create new
address". The label of the address says that it is unused, never that it is new.

Reason: the daemon does not say whether it hands out an address for the first time. An address shown as new with an
empty note invites the user to save a second note, which would replace the first.

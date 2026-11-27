# Sending from the wallet

This specification defines what the send form accepts and how it reports the result. The rules for the request itself
are in [Wallet of the daemon](daemon-wallet.md).

## Input

The form sends only when the address, the amount and the password are valid. Invalid input never reaches the daemon.

- The address is a Bitcoin address or a `bitcoin:` URI. Bisq removes surrounding white space and the URI part, and
  writes a bech32 address in lower case. A URI with a required parameter (`req-`) is invalid, because the form supports
  none. An address of a test network is valid only in the development mode.
- The amount is a positive number of bitcoin with at most 8 decimals.
- The password must not be empty. The form always asks for it.

Reason for the password: the wallet of the daemon always has a password, and the daemon needs it for every send. A
send without a password could only fail.

## One send at a time

While a send runs, the form sends nothing else, and its fields cannot be changed. After a successful send the form
shows the transaction ID and sends nothing until it is opened again.

Reason: a second click during a slow request would pay twice.

A send has no deadline ([Wallet state](wallet-state.md)), so the form waits for the answer of the daemon.

The password leaves the form after a successful send and when the form closes.

## Result

After a successful send the form shows the transaction ID. If the user left the form in the meantime, Bisq shows the
result in a popup, so it is not lost.

A failed send names the cause where the daemon gives one: a wrong password, a locked wallet, an unreachable daemon, a
request which the daemon refused, or the description of the daemon, for example for insufficient funds.

## Unknown outcome

A failed send may have paid when the failure does not tell where it happened. That is the case for a lost connection
and for an internal error of the daemon. Bisq then says that the payment may have been sent, asks the user to check
the transactions first, and clears the amount.

A wrong password, a locked wallet, a refused request, a daemon without the send call and a request which never left
Bisq are known to have sent nothing.

Reason: the daemon may have broadcast the payment before the failure reached Bisq. With the amount still in the form,
a second click would pay a second time.

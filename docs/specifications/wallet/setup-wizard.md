# Wallet setup wizard

The setup wizard creates the wallet of the daemon and lets the user back up the seed words. The rules for the wallet
itself are in [Wallet of the daemon](daemon-wallet.md).

## Password step

The wizard goes on to the backup step only after the daemon opened or created the wallet with the entered password.
While the request runs, the user can neither change the password fields nor move to another step. If the daemon
refuses, the wizard stays on the password step and shows the reason. After the daemon accepted the password, the
fields stay locked with it for the rest of the setup, and going back to the password step and on again sends no new
request.

Reason: the password of the wallet is the one which the daemon received. If the fields could change while the request
runs or afterwards, the text on the screen could differ from the password of the wallet, and the daemon would refuse
any other password for the wallet which exists now.

## Closing

Closing the wizard does not cancel a request. The first way the wizard is closed decides where the user lands: the
close button, Escape and the system close of its window lead to the main screen, the finish of the verification leads
to the wallet. Whatever arrives while the wizard hides, an answer of the daemon, another close, a finish, Next or
Back, changes nothing.

## Seed words

The backup step and the verification step ask the daemon for the seed words with the password which the daemon
accepted in the password step. They do not read the password fields again. The wizard forgets the password when it
closes.

Reason: the wallet keeps the password it was opened with, so the steps after the password step rely only on that
password, whatever the fields show.

The daemon decides the length of the phrase. The wizard takes every length which BIP39 allows: 12, 15, 18, 21 or 24
words. Another number of words counts as a failed request.

When the seed words cannot be loaded, the step says so and shows no words. From the backup step the wizard then goes
back to the password step, from the verification step back to the backup step. While the backup step waits for the
words, it shows no Next button, and Enter does not move on either.

The verification step shows its questions only after the seed words arrived. It asks for six words of the phrase, each
with two wrong choices which are other words of the same phrase.

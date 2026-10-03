# User profile proof of work

A user profile carries a proof of work. Its payload is the hash of the profile's public key. The nym
and the cat hash icon of the profile are derived from the key hash and the solution of this proof.

## Rules

1. A node accepts a user profile from the network only if all of these hold:
   - The payload of the proof of work equals the hash of the profile's public key.
   - The claimed difficulty is at least the minting difficulty `MINT_NYM_DIFFICULTY` (2^16). A
     difficulty which is not a number (NaN) fails this rule.
   - The solution is valid work for the claimed difficulty.
2. A node which creates a profile mints the proof with `MINT_NYM_DIFFICULTY`.
3. A node ignores a profile which breaks rule 1. It does not show it and does not use it for nym
   or nickname decisions.

## Reason

The work makes it expensive to search for a key whose nym and icon look like those of another
user. The verifier checks the work against the difficulty which the profile claims. Without the
minimum in rule 1, a profile could claim a difficulty of 0 and pay about one hash per attempt
instead of about 2^16.

## Compatibility

All released versions mint with `MINT_NYM_DIFFICULTY` (the value is unchanged since 2022). Profiles
of unmodified nodes therefore pass rule 1.

## Limits

The rule applies where a node accepts profiles from the network data store. The network store
itself does not check the proof of work, so it still stores and relays such a profile. Profiles
embedded in other messages, for example in a contract or a mediation request, are not checked
against this rule.

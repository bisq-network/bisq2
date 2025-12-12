## How Bisq avoids impersonation attacks on User Identities

To ensure users can trust identities in public spaces (e.g., Bisq chat channels or Offerbook), Bisq employs **four identity layers** to prevent impersonation.


### Impersonation Attacks
A malicious actor could create a profile mimicking a legitimate user (e.g., a Bisq support agent or a trader with whom one has traded in the past) to deceive others. 


### Identifiers

Bisq employs four layers of identifiers, each balancing security against impersonation with usability for human recognition:

* **Profile ID**
* **Nym (Bot ID)**
* **User profile icon**
* **Nickname**


#### Profile ID
The user profile is based on a cryptographic key pair.
The **Profile ID** is the hex-encoded hash of the public key and practically impossible to brute-force to a collision.

However, the long string is hard for humans to remember. That’s why we provide additional identifiers derived from this secure base.

#### Nym (Bot ID)

The **Nym** consists of three words and a three-digit number, e.g.:

```
Unstoppable-Rainy-Sleepwalker-453
```

It’s built from lists of adverbs, adjectives, and nouns to form a short, semantically valid (but often awkward) phrase. This is much easier to remember and compare than the Profile ID.

The Nym is usually **not** shown in the UI unless multiple nicknames are detected, in which case it helps distinguish users with the same nickname (e.g., multiple “Satoshis”).

Each profile is published to the P2P network and expires after 15 days, ensuring inactive ones are removed.

The Nym is derived from the Profile ID and the solution of a proof-of-work challenge. Brute-forcing the same Nym as another user requires significant computation (details later).
You can think of the Nym like a **family name**—it helps differentiate people but can still have rare collisions.

#### User Profile Icon

Each profile also has a deterministic icon generated from the Profile ID and proof-of-work solution.
The image is built from layered graphical elements, producing a unique visual fingerprint. While icons are easier to recognize than strings, the limited design space means collisions are more likely than with Nyms.

As a complementary identifier, it resembles a passport photo—visually recognizable but not secure on its own.


#### Nickname

Users can freely choose their nickname, which is the most memorable but least secure identifier due to potential collisions (e.g., multiple users choosing “Satoshi”). Like a first name, it’s prone to duplication, making it unreliable for unique identification.

### Layered Identity

Bisq’s layered identity system combines usability and security. By default, users are represented by their nickname and profile icon, which are easy to recognize. If duplicate nicknames are detected, the Nym is displayed alongside the nickname to prevent confusion. This makes impersonation attacks highly impractical. For absolute certainty, users can click the profile icon to view the Profile ID in a details popup, ensuring unambiguous identification.

### Nym generation
The Nym generation process relies on three word lists:
- Adverbs: 4,833 words
- Adjectives: 450 words
- Nouns: 12,591 words
- Number: 0–999 (1,000 possibilities)

The total Nym space is calculated as:

`4832 * 450 * 12591 * 1000 = 27377870400000 (2^44.6)`

This provides approximately 27.38 trillion unique combinations, offering reasonable diversity but insufficient security against brute-force attacks alone.

To enhance security, Bisq initially added a HashCash-based PoW with a difficulty of `2^16 = 65,536`. The Nym is derived from the Profile ID and PoW solution, intending to force attackers to generate both new key pairs and PoW solutions. However, this approach inadvertently weakened security.

### The Problem
The PoW inclusion allows attackers to use a single key pair and iterate through PoW solutions to find a Nym collision, reducing the overall security because PoW hashes are cheaper to create than EC Key pairs for which there are not ASICs known to exist.
An attack requires max. `2^44.6 * 2^16 = 2^60.6 = 1.7 * 10^18 attempts` hash operations. 

Estimated [1] attack times and costs are:

| Hardware                  | Hash Rate (H/s)    | Time (approx)  | Estimated Cost (USD, Cloud) |
|---------------------------|--------------------|----------------|----------------------------|
| CPU (100 MH/s)            | \(10^8\)           | ~70.9 years    | ~$2.72M                    |
| GPU (300 MH/s)            | \(3 \times 10^8\) | ~23.6 years    | ~$1.07M                    |
| Antminer S19 (95 TH/s)    | \(9.5 \times 10^{13}\) | ~39.2 minutes | ~$50                     |
| 1 PH/s Farm               | \(10^{15}\)        | ~3.73 minutes  | ~$50                     |

These low costs for ASICs (~$50) demonstrate that the current PoW makes Nym collisions feasible for determined attackers. 

### Possible Improvements

#### Remove Proof-of-Work
Removing PoW forces attackers to generate `2^44.6` key pairs, which is more computationally intensive and ASIC resistant. Estimated [1] times and costs for ECDSA key generation are:

| Hardware Type               | Keys/sec         | Time Required | Cost (USD, Cloud) |
|-----------------------------|------------------|---------------|-------------------|
| CPU (Single Core)           | 50,000           | ~7.15 years   | ~$1,880           |
| CPU (Multi-core, 32 cores)  | 1.6M             | ~81.5 days    | ~$1,664           |
| GPU (RTX 4090)              | 5M               | ~26.1 days    | ~$626             |
| FPGA                        | 20M              | ~6.52 days    | ~$3,911           |
| Cloud GPU Farm (100 GPUs)   | 500M             | ~6.26 hours   | ~$626             |

At ~$626–$3,911, this is costlier than the PoW-based attack but still not prohibitive for motivated attackers.


#### Use Memory-Hard PoW (EquiHash)
Switching to a memory-hard PoW like EquiHash significantly increases attack costs due to its resistance to ASIC optimization. Estimated [1]  times and costs for `2^60.6` EquiHash operations are:

| Hardware                      | Hash Rate (sol/s) | Time (years) | Cost (USD, Cloud) |
|-------------------------------|-------------------|--------------|-------------------|
| CPU (50 sol/s)                | 50                | ~4,470 years | ~$350B            |
| GPU (500 sol/s)               | 500               | ~447 years   | ~$39.3B           |
| ASIC (10,000 sol/s)           | 10,000            | ~22.4 years  | ~$19.7B           |
| Cloud GPU Farm (10,000 sol/s) | 10,000            | ~22.4 years  | ~$39.3B           |

EquiHash’s high costs make it a cost-prohibitive solution. 

We already have EquiHash integrated, but it hasn’t been activated yet due to challenges in tuning its difficulty.
If the difficulty is set too high, it can degrade the user experience on low-end hardware or mobile devices—important to consider since Bisq also runs on mobile.
That said, this is primarily a concern for the dynamic difficulty used in the P2P network’s DoS protection.
For Nym generation, we use a static, low difficulty, which can be fine-tuned through testing on low-end devices to ensure it remains accessible.

#### Increase PoW Difficulty
Increasing the HashCash difficulty beyond `2^16` could help but risks poor UX on low-end hardware and provides limited security gains compared to EquiHash.

#### Increase Number of Words
Adding more words to the Nym (e.g., four instead of three) marginally increases the Nym space but reduces UX due to longer, less memorable strings.

#### Vanity Prefix on Public Key Hash
Requiring the public key hash to meet a small difficulty (e.g., a specific prefix) forces attackers to generate more keys. However, this impacts UX on low-end devices and offers less security than EquiHash.

### Conclusion
EquiHash as a memory-hard PoW offers the best balance of security and simplicity, making Nym collisions computationally infeasible. The challenge lies in deploying it in a backward-compatible manner to maintain Bisq’s P2P network integrity and minimize disruption. Removing PoW is a viable interim solution, significantly increasing attack costs, but EquiHash remains the most robust long-term approach.

### Deployment Options
We can extend the user profile to include the new **EquiHash PoW** data.
If present, both the existing **HashCash PoW** and the new **EquiHash PoW** will be verified.

When creating a new profile, users will perform **both** PoWs:

* **HashCash PoW** (current method)
* **EquiHash PoW** (new)

Older, non-updated nodes will only recognize and verify the HashCash PoW.
Both PoWs use the **profile ID** as their input.

Updated nodes will verify both PoWs when data is present.
If an updated node receives a profile from an old node (missing EquiHash data), it will simply skip the EquiHash verification.

When a user upgrades, they will create a EquiHash PoW and republish their profile with the EquiHash PoW included.

Adding additionally HashCash to HashCash increases further security as an attacker would need to resolve both PoW solutions.

For **Nym generation**, the EquiHash PoW solution will be added to the input whenever available.
This means updated users gain the extra security immediately. However, this also means Nyms and profile icons will change, which could cause some temporary confusion.
To minimize this, we should communicate the change clearly by utilizing the **in-app update notifications**.

### Mitigating Homograph and Visual Spoofing Attacks
To prevent impersonation through visually deceptive nicknames, we should restrict the allowed character set for nicknames.
This avoids Unicode characters that closely resemble Latin letters, which attackers could exploit to create nicknames that look identical to legitimate ones but differ technically.

Because the system treats these as distinct strings, the Nym would not display for such cases, potentially confusing users or enabling subtle impersonation.

Similarly, ambiguous spacing characters, invisible, and non-printable characters should also be disallowed to avoid hidden manipulations or visual tricks.

Rather than relying solely on exclusion lists—which may miss some problematic characters—we can consider implementing a curated whitelist of explicitly allowed characters for better security.


_[1] Time and cost estimations are done by ChatGpt and cross-checked with other AI tools._
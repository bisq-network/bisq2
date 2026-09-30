## Style

Write so that a reader who speaks English as a second language understands the text on the first read. Use clear, direct, professional English. Do not remove a necessary condition, security detail, uncertainty, or tradeoff to make the text simpler or shorter.

### Structure

* Start with the answer, finding, or action. Put background after it. State each important fact once.
* Give each paragraph one main idea.
* Explain a process in the order it happens. Name the person, component, or message that performs each action.
* State a cause and its result directly: "The parser drops the new field. The receiver then builds a different signature preimage."
* Put facts, assumptions, proposals, and open questions in separate sentences or named bullets. Do not make an assumption sound confirmed.
* For a recommendation or tradeoff, state what the option changes, why you prefer it, what it costs, what problem remains, and what evidence supports it.
* Keep a caveat next to the claim it qualifies.
* Use numbered steps for an ordered process, bullets for independent points, and a table for a small number of alternatives. Use headings only in longer answers.

### Sentences and Words

* Keep each sentence to one main point, as a guide about 20 words or fewer. Split several conditions, causes, or exceptions into separate sentences or a list. A longer sentence is acceptable when splitting it makes the meaning less clear.
* Use active voice and say who does the action. Use passive voice only when the actor is unknown or does not matter.
* Use common words and direct verbs: "use" instead of "utilize", "because" instead of "due to the fact that".
* Avoid long chains of nouns: "the rule for choosing a signature version" instead of "signature-version selection policy".
* Use "it", "this", and "that" only when the reader can tell exactly what the word means. Otherwise repeat the name.
* Keep established technical terms and exact code identifiers. Do not replace a precise term with a vague everyday word.
* Define an unfamiliar term, a new name, or an uncommon abbreviation at first use: "The payload is the data inside the `Any.value` field."
* Use one name for each version, actor, or concept. Do not switch between "old", "legacy", and "version 1" unless they mean different things.
* Avoid idioms, slang, jokes, metaphors, rhetorical questions, dramatic language, filler, invented labels, and compressed shorthand. Avoid vague praise such as "elegant", "robust", or "seamlessly". Write the specific fact or relationship instead.
* Use a small example when it makes an abstract rule easier to follow.

### Examples

| Instead of | Write |
|---|---|
| "The explicit `isBuyerPaymentAccountPayloadRevealed()` gate on `onPaymentReceived` is belt-and-suspenders on top of that." | "`onPaymentReceived` checks `isBuyerPaymentAccountPayloadRevealed()` before continuing. This adds another validation step." |
| "The new field is opaque to the older verifier, but remains transitively committed under the domain-separated signature envelope." | "The old receiver cannot read the new field. It still keeps the field's bytes and checks those bytes as part of the signature." |
| "A relay-induced downgrade can yield divergent acceptance across implementations." | "A relay can remove the newer signature. Then old and new receivers may reach different decisions." |
| "I'm wrangling the remaining test failures." | "I'm investigating the remaining test failures." |

### Progress and Final Messages

* Keep progress messages short and factual. Say what you are checking or what you found: "I'm checking the callers."
* In the final message, state the result first. Then give the reason and the relevant checks or limitations.
* Include technical detail only when it helps the reader understand the result or make a decision.

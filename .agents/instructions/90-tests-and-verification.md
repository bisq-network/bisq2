## Tests and Verification

* Follow the rules in the **Specifications** section regarding the relationship between specifications, implementation, and tests.
* Do not implement questionable behavior solely because an existing test expects it.
* Add or update tests when they provide meaningful verification of intended behavior.
* Prefer tests of domain behavior, invariants, edge cases, and externally observable results over tests tightly coupled to implementation details.
* Avoid excessive mocking where a simpler design can make behavior testable directly.
* When it improves the production design, prefer extracting deterministic logic into small side-effect-free functions or components that can be tested without extensive mocking.
* Do not distort production architecture solely to make tests easier to write.
* If method visibility is widened only to support testing, annotate it with `@VisibleForTesting` where that annotation is available and consistent with project conventions.
* After making changes, run the narrowest useful verification first.
* Run broader checks when the scope, risk, or affected dependencies justify them.
* When verification fails, investigate the underlying cause before changing production behavior or weakening the test.
* Do not delete, disable, loosen, or bypass a valid test merely to get a passing build.
* Report relevant verification that could not be performed and why.

### Tests Which Fail After a Design Change

* A test which fails after a design change is evidence that the change may violate an invariant. Before adapting the test, determine which invariant it protects. If the invariant is valid (for example "a request with an invalid signature is not accepted"), look for a design which keeps the invariant and meets all stated constraints.
* Adapt such a test only after the developer has seen the design option which would keep it passing.

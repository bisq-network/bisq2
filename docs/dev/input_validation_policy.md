# Parameter Validation

## Purpose

Define a clear and consistent policy for validating method parameters without introducing redundant checks or defensive clutter.

---

## Core Principle

Validate inputs at **trust boundaries**, and rely on **contracts internally**.

* **External input → validate**
* **Internal flow → trust**

---

## Trust Boundaries

A trust boundary is any point where data enters a component or module from a less controlled context.

Typical boundaries:

* Public methods
* Constructors
* API endpoints
* UI event handlers
* Cross-module calls

---

## Policy

### 1. Public Methods and Constructors

* Always validate parameters
* Fail fast with clear exceptions
* Enforce invariants at the boundary

```java
public void applySliderValue(TradeAmount tradeAmount, double sliderValue) {
    checkNotNull(tradeAmount, "tradeAmount must not be null");
    checkArgument(sliderValue >= 0 && sliderValue <= 1, "sliderValue must be between 0 and 1");
}
```

---

### 2. Internal Methods (private / package-private)

* Do **not** repeat validation
* Assume inputs satisfy the method contract
* Keep methods focused and readable

```java
private void doApplySliderValue(TradeAmount tradeAmount, double sliderValue) {
    // no validation here
}
```

Redundant checks inside internal flows are discouraged because they:

* add noise
* obscure real invariants
* complicate maintenance

---

### 3. Cross-Class Calls Within the Same Module

* Rely on established contracts
* Do not re-validate if the caller already validated

Exception:

* If the called class is loosely coupled or reused independently, treat it as a boundary

---

### 4. Cross-Module or External Calls

* Treat as a boundary
* Validate before calling if the callee cannot be trusted to enforce constraints

---

## Preferred Check Style

Prefer Guava preconditions for standard parameter validation.

* Use `checkNotNull` for null checks.
* Use `checkArgument` for simple argument constraints expressed as a clear boolean condition.
* Add meaningful error message
* Use `IllegalArgumentException` for more complex validation

```java
public void applySliderValue(TradeAmount tradeAmount, double sliderValue) {
    checkNotNull(tradeAmount, "tradeAmount must not be null");
    checkArgument(sliderValue >= 0 && sliderValue <= 1, "sliderValue must be between 0 and 1");
}
```

---

## Static Utilities

Static utility methods follow the **same trust-boundary rule**.

* Public static utilities are boundaries and must validate their inputs.
* Private or package-private static utilities are internal helpers and should not repeat validation.

```java
public static int parsePositiveInt(String value) {
    checkNotNull(value, "value must not be null");

    int result = Integer.parseInt(value);
    checkArgument(result > 0, "value must be > 0");
    return result;
}
```

```java
private static int multiply(int a, int b) {
    return a * b;
}
```

---

## Recommended Default

* Use **strict validation** for all public APIs and shared utilities
* Avoid validation in private/internal methods

---

## Nullability

* Parameters are non-null by default
* Use Guava `checkNotNull` at boundaries where needed
* Avoid passing `null` internally

See also: [Nullability and Optional](nullability-and-optional.md).

---

## Design Guidelines

### Prefer Strong Types Over Repeated Validation

Instead of repeatedly validating primitives:

```java
void process(long amount);
```

Encapsulate constraints:

```java
void process(Monetary amount);
```

Validation is performed once during object creation.

---

### Fail Fast on Programming Errors

* If a condition indicates a bug, do not defensively guard it everywhere
* Let it fail early

---

## Summary

* Validate at boundaries, not everywhere
* Trust internal flows
* Keep contracts clear and consistent
* Use strict validation for shared utilities
* Prefer `checkNotNull` and `checkArgument` for standard checks
* Use direct `IllegalArgumentException` for complex validation paths
* Avoid redundant checks

## General Code Guidelines

- Use Lombok annotations for `Getter`/`Setter`/`ToString`/`EqualsAndHashCode`. Be cautious with using more advanced
  Lombok features.
- Use the most narrow visibility scope.
- Use clear variable names, not one-letter variables (except in loops). Using the type as variable name is mostly a good
  choice and helps with refactoring and search.
- Set organize imports and reformat code at the commit screen in IDE. It helps to reduce formatting diffs.
- The style convention is to follow the autoformatting rules provided by IntelliJ's IDEA by default. Hence, there is no
  need to customize the IDEA's settings in any way, and you can simply use it as-is out of the box.
- For curly brackets, adopt the "K&R style" for consistency and readability. This means placing the opening brace at the
  end of the method signature line, not on a new line. Example:
    ```
    public ReadOnlyObjectProperty<Market> getMarket() {
        return model.getSelectedMarket();
    }
    ```
- Use curly brackets even in one-liners. It's a common source for bugs when later code gets added, and it improves
  readability.
- Don't use the final keyword in local scope or with arguments, only in class fields.
- Try to use final class fields and avoid nullable values.
- Use Optional if a nullable value is a valid case.
- If nullable fields are used, use the `@Nullable` annotation (see discussion below).
- Nullable values in non-UI code should be avoided as far as possible. In UI code it is unfortunately not that easy as
  JavaFX framework classes often deal with nullable values.
- If you need to write a lot of documentation, ask yourself if instead the method name or variable name could be
  improved to make it more self-explanatory. If the method is too complex, break it up.
- Don't use trivial and boilerplate Javadoc. Use Javadoc only in API level classes or if non-trival background or context is explained.
- If parameters are getting too long, break it up as single param per line.
- When using fluent interface break up in lines at each `.`
- Use separator lines if classes are getting large to logically group methods.
  Use 2 line breaks after the comment separator line.
  Format for comment separators:
    ```
    /* --------------------------------------------------------------------- */
    // Group Name
    /* --------------------------------------------------------------------- */
    ```
- Use Java records only for simple value objects. Converting them later to normal classes is a bit cumbersome.
- Always use `@Override` when overriding methods.
- Ternary operators should be styled for clarity and conciseness. For short conditions that fit in one line, use:
    ```
    return map.containsKey(key) ? map.get(key) : defaultValue;
    ```
  For longer conditions, express them as follows to enhance readability:
    ```
    return priceSpec instanceof FloatPriceSpec
            ? Optional.of((FloatPriceSpec) priceSpec)
            : Optional.empty();
    ```
  Avoid nested ternary operators to maintain code readability.
- In UI classes list fields with the same type in one line to reduce number of lines.


### Modeling Absence with Optional Instead of Null

The official Java recommendation is to use `Optional` primarily for return types, not for parameters or class fields. The rationale is that `Optional` makes the caller–callee contract explicit: a potentially absent return value must be handled consciously by the caller.

However, this argument is limited to return values and does not address the broader issue that nullable values are a primary source of runtime exceptions. Making optionality explicit in the type system helps prevent `NullPointerException`s and improves overall code readability. Using `Optional` consistently for absence achieves this more effectively than relying on implicit nullability.

Relying on nullable values typically leads to excessive boilerplate (defensive null checks) or the use of annotations such as `@Nullable` or `@NonNull`. While useful, these annotations are not enforced by the compiler and therefore provide weaker guarantees than a type-based approach. They also tend to increase noise without fundamentally improving safety.

Our approach is to enforce a project-wide convention:

* All values are non-null by default.
* If absence is a valid state, it must be expressed explicitly using `Optional`.

This eliminates the need for pervasive null checks and makes optionality explicit at the type level. As a result, method contracts become clearer, and both callers and callees can rely on non-null invariants. It also aligns well with the explicit presence semantics used in Protocol Buffers.

We recognize that this approach still relies on discipline: Java does not prevent assigning `null` to an `Optional` or wrapping a `null` value. To mitigate this, additional tooling (e.g., static analysis) and code reviews are required.

Conceptually, this model aligns with languages that provide null-safety at the type level, such as Kotlin, where types are non-null by default and nullable types must be declared explicitly (e.g., using `?`).

In UI code, where frameworks such as JavaFX expose nullable APIs, this rule is applied more flexibly. In such cases, `@Nullable` annotations should be used where appropriate to maintain clarity, and the general non-null-by-default principle should be followed as far as practical.

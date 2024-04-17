## General code guidelines

- Use Lombok annotations for `Getter`/`Setter`/`ToString`/`EqualsAndHashCode`. Be cautious with using more advanced
  Lombok features.
- Use the most narrow visibility scope.
- Use clear variable names, not one letter variables (except in loops). Using the type as variable name is mostly a good
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

- Use curly brackets even in one-liners. It's a common source for bugs when later code gets added and, it improves
  readability.
- Don't use the final keyword in local scope or with arguments, only in class fields.
- Try to use final class fields and avoid nullable values.
- Use Optional if a nullable value is a valid case.
- If nullable fields are used, use the `@Nullable` annotation.
- Nullable values in domain code should be avoided as far as possible. In UI code its unfortunately not that easy as
  JavaFX framework classes often deal with nullable values.
- If you need to write a lot of documentation ask yourself if instead the method name or variable name could be
  improved. If the method is too complex break it up.
- Don't use trivial and boilerplate java doc. Use java doc only in API level classes.
- If parameters are getting too long, break it up as single param per line.
- When using fluent interface break up in lines at each `.`
- Use separator lines if classes gets larger to logically group methods.
- Use Java records only for simple value objects. Converting them later to normal classes is a bit cumbersome.
- Use `@Override` when overriding methods.
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
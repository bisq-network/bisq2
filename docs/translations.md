## How to update/synchronize translations

Communication with Transifex is done via their Transifex Client[^1]

1. Install the Transifex Client
2. Get an API token[^2] from your Transifex user profile. You have to have project maintainer rights to be able to
   manipulate the translation files on transifex. Contact to Transifex maintainer to the get the required permissions.
3. Run `tx pull` to download new translations from the Transifex website or e.g. `tx push -t bisq-2.academyproperties` 
   if you want to overwrite remote translation with your local copy in the `i18n` module directory.
4. If you want to add new translation files to the project run e.g. `tx add src/main/resources/authorized_role.properties`

So besides adding new initial translations via chatGPT the main use case will be to run `tx pull` before a new release
to update all translation files.

[^1]: https://developers.transifex.com/docs/cli
[^2]: https://developers.transifex.com/reference/api-authentication
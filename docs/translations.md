# How to update/synchronize translations

This guide outlines the process for managing translations using the Transifex platform and the Transifex Client (`tx`)[^1].

### One-Time Setup

1.  **Install the Transifex Client**: Follow the instructions on the official documentation.
2.  **Get an API token**: Generate an API token[^2] from your Transifex user profile. You must have "Project Maintainer" rights for the Bisq project to push source files and manage resources.

### Use Case 1: Syncing Existing Translations (e.g., Before a Release)

This is the most common task. It pulls the latest completed translations from Transifex into your local repository.

1.  Navigate to the `i18n` module directory.
2.  Pull all translations:
    ```bash
    tx pull
    ```
3.  Commit the updated `..._<lang>.properties` files to the repository.

### Use Case 2: Adding a New Translatable File

When a new source file (e.g., `new_feature.properties`) is added to the project, it must be properly registered with Transifex and the repository. This is a multi-step process.

**Step 1: Add the New Source File**

Place your new `.properties` file in the `i18n/src/main/resources/` directory.

**Step 2: Update Transifex Configuration**

Manually add a new resource block to the `.tx/config` file in the project's root directory. Copy an existing block and change the resource name and file paths. The resource name in the `[...]` brackets must be unique.

*Example for `new_feature.properties`*:
```ini
[o:bisq:p:bisq-2:r:new_featureproperties]
file_filter            = i18n/src/main/resources/new_feature_<lang>.properties
source_file            = i18n/src/main/resources/new_feature.properties
type                   = UNICODEPROPERTIES
```

**Step 3: Push the New Source File to Transifex**

This command uploads the English source strings to Transifex, making the resource visible and ready for translation. This replaces older workflows that may have used `tx add`.

```bash
tx push --source
# Or using the shorthand:
tx push -s
```

**Step 4: Create Initial Local Files for Each Language**

The `tx pull` command **will not** create local translation files (e.g., `new_feature_de.properties`) if no translations for them exist on Transifex yet. You must create these empty files manually so they can be tracked by Git and populated later.

Navigate to the resources directory and run the following command, replacing `new_feature` with the base name of your new file.

```bash
# In the i18n/src/main/resources/ directory, run this command:
for lang in cs de es it pt_BR pcm ru af_ZA; do
  touch "new_feature_${lang}.properties"
done
```

**Step 5: Commit All Changes**

Commit the following to your Git repository:
1.  The new source file (e.g., `new_feature.properties`).
2.  The updated `.tx/config` file.
3.  All the new empty translation files (e.g., `new_feature_de.properties`, etc.).


[^1]: https://developers.transifex.com/docs/cli
[^2]: https://developers.transifex.com/reference/api-authentication
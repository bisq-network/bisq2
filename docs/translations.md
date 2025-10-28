# How to update/synchronize translations

This guide outlines the process for managing translations using the Transifex platform and the Transifex Client (`tx`)[^1].

## One-Time Setup

1.  **Install the Transifex Client**: Follow the instructions on the official documentation.
2.  **Get an API token**: Generate an API token[^2] from your Transifex user profile. You must have "Project Maintainer" rights for the Bisq project to push source files and manage resources.

## Use Case 1: Syncing Existing Translations (e.g., Before a Release)

This is the most common task. It pulls the latest completed translations from Transifex into your local repository.

1.  Navigate to the `i18n` module directory.
2.  Pull all translations:
    ```bash
    tx pull
    ```
3.  Commit the updated `..._<lang>.properties` files to the repository.

## Use Case 2: Adding a New Translatable File

When a new source file (e.g., `new_feature.properties`) is added to the project, it must be properly registered with Transifex and the repository. This is a multi-step process.

### Step 1: Add the New Source File

Place your new `.properties` file in the `i18n/src/main/resources/` directory.

### Step 2: Update Transifex Configuration

Manually add a new resource block to the `.tx/config` file in the project's root directory. Copy an existing block and change the resource name and file paths. The resource name in the `[...]` brackets must be unique.

*Example for `new_feature.properties`*:
```ini
[o:bisq:p:bisq-2:r:new_featureproperties]
file_filter            = i18n/src/main/resources/new_feature_<lang>.properties
source_file            = i18n/src/main/resources/new_feature.properties
type                   = UNICODEPROPERTIES
```

### Step 3: Push the New Source File to Transifex

This command uploads the English source strings to Transifex, making the resource visible and ready for translation. This replaces older workflows that may have used `tx add`.

```bash
tx push --source
# Or using the shorthand:
tx push -s
```

### Step 4: Create Initial Local Files for Each Language

The `tx pull` command **will not** create local translation files (e.g., `new_feature_de.properties`) if no translations for them exist on Transifex yet. You must create these empty files manually so they can be tracked by Git and populated later.

Navigate to the resources directory and run the following command, replacing `new_feature` with the base name of your new file.

```bash
# In the i18n/src/main/resources/ directory, run this command:
for lang in cs de es it pt_BR pcm ru af_ZA; do
  touch "new_feature_${lang}.properties"
done
```

### Step 5: Commit All Changes

Commit the following to your Git repository:
1.  The new source file (e.g., `new_feature.properties`).
2.  The updated `.tx/config` file.
3.  All the new empty translation files (e.g., `new_feature_de.properties`, etc.).

## Use Case 3: Adding a New Language/Locale

When you want to add a completely new language to Bisq 2 (e.g., French, Japanese, etc.), you need to update both the codebase and Transifex configuration. This is a comprehensive process that involves multiple steps.

### Step 1: Add Language to Transifex

1.  Log in to Transifex at <https://app.transifex.com/>
2.  Navigate to the Bisq 2 project
3.  Go to **Languages** settings
4.  Click **Add Language** and select the target language
5.  Confirm the addition

**Note**: You must have "Project Maintainer" rights to add new languages.

### Step 2: Update LanguageRepository.java

Add the new language code to the `LANGUAGE_TAGS` list in `common/src/main/java/bisq/common/locale/LanguageRepository.java`.

*Example for adding French (fr)*:
```java
public static final List<String> LANGUAGE_TAGS = List.of(
        "en", // English
        "de", // German
        "es", // Spanish
        "fr", // French  <-- Add your new language here
        "it", // Italian
        "pt-BR", // Portuguese (Brazil)
        "cs", // Czech
        "pcm", // Nigerian Pidgin
        "ru", // Russian
        "af-ZA" // Afrikaans
);
```

**Important**: Use the correct language tag format:
- Simple language codes: `fr`, `de`, `ja`, `ko`
- Language with region: `pt-BR`, `af-ZA`, `zh-Hans`
- Use hyphens (`-`), not underscores for region variants

### Step 3: Pull English Source Structure for New Translation Files

After adding the language to Transifex and updating the configuration, pull the English source structure to create properly formatted translation files:

```bash
# Navigate to i18n module
cd i18n

# Pull translation files based on sources (-t flag) for the new language to get English structure
tx pull -t -l fr

# This creates all translation files with:
# - Complete file structure (comments, section headers, empty lines)
# - All translation keys with English values as placeholders
# - Proper formatting matching the source files
```

**Important**: The `-t` flag pulls the source language (English) as a template, creating files with the complete structure. This ensures translation files have proper formatting from the start.

**Note**: File naming uses underscores (`_`) to separate the base name from the language code, even though the language code in Java uses hyphens. For example:
- Language code in Java: `pt-BR`
- Filename: `default_pt_BR.properties`

### Step 4: Translate the Files

Before committing the new translation files, you need to populate them with translations. You have several options:

**Option A: Use AI Translation Tools** (Recommended for initial translations)
```bash
# Use an AI-powered translation service to translate all keys
# This can be done via Claude Code agents or other translation automation tools
# Example: Use Claude Code's translation agents to translate each locale
```

**Option B: Manual Translation via Transifex**
```bash
# Push the source files to Transifex
cd i18n
tx push -s

# Translators can then work in the Transifex web interface
# Pull completed translations when ready
tx pull -l fr
```

**Option C: Manual Translation Locally**
- Open each `*_fr.properties` file and replace English values with French translations
- Preserve all placeholders like `{0}`, `{1}`, etc.
- Maintain multi-line continuation format with `\n\`

**Important**: Whichever method you choose, ensure translations are complete before committing. Files should contain actual translations, not English placeholders.

### Step 5: Verify Translation Files

After running the translation pipeline, verify the files:

```bash
# Check that files have proper structure and translations
ls -lh i18n/src/main/resources/*_fr.properties

# Verify a sample file has translations (not just English)
head -50 i18n/src/main/resources/default_fr.properties
```

### Step 6: Commit All Changes

Commit the following to your Git repository:
1.  Updated `LanguageRepository.java` with the new language code
2.  All 17 new translation files with complete structure and initial translations (e.g., `default_fr.properties`, `chat_fr.properties`, etc.)
3.  Updated `.tx/config` if modified

**Important**: The first commit should include translation files that already have:
- Complete file structure (comments, headers, formatting)
- Initial translations (not English placeholders)
- Proper key ordering matching source files

### Step 7: Test the New Language

1.  Build the application: `./gradlew clean build`
2.  Run the desktop app
3.  Navigate to **Settings → Language**
4.  Verify your new language appears in the dropdown
5.  Select it and restart the application
6.  Verify the UI displays in the new language (or shows translation keys if not yet translated)

**Notes**:
- Empty translation files will fall back to English (the source language)
- The application must be restarted for language changes to take effect
- RTL (Right-to-Left) languages like Arabic, Hebrew, or Persian require additional configuration in `LanguageRepository.RTL_LANGUAGES_CODES`


[^1]: https://developers.transifex.com/docs/cli
[^2]: https://developers.transifex.com/reference/api-authentication
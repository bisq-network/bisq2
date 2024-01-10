## How to update/synchronize translations

Communication with Transifex is done via their Transifex Client[^1]

1. Install the Transifex Client
2. Get an API token[^2] from your Transifex user profile. You have to have project maintainer rights to be able to
   manipulate the translation files on transifex. Contact to Transifex maintainer to the get the required permissions.
3. Run `tx pull` to download new translations from the Transifex website or e.g. `tx push -t bisq-2.academyproperties` 
   if you want to overwrite remote translation with your local copy in the `i18n` module directory.
4. If you want to add new translation files to the project run e.g. `tx add src/main/resources/authorized_role.properties`
5. If you want to update a source file run e.g.`tx push -s bisq-2.academyproperties`

So besides adding new initial translations via chatGPT the main use case will be to run `tx pull` before a new release
to update all translation files.

Here is the prompt I use to kickstart a new translation. Any input of chatGPT prompt engineers to improve the quality
of the translation is highly welcome.

```
You are an export Java programmer and translator. I'll give you a java property file in English and you will translate
 it as a first step into [ENTER YOUR DESIRED LANGUAGE]. You keep all comments (lines starting with a „#“ like 
 „# suppress inspection "UnusedProperty““, „# Dynamic values using DefaultApplicationService.State.name()“, 
 „## Dashboard“) that are already in the file and do NOT translate them. Also keep line breaks like „\n\n“ or „\n“ 
 in the translation. Also don’t translate lines starting with „##“. Also double check that you don’t translate 
 comment lines that start with e.g. „# Dynamically generated in“.  You're a multi-lingual expert. Your task is to 
 rewrite the provided text into [ENTER YOUR DESIRED LANGUAGE]. However, this isn't a mere word-for-word translation task.. You need to adapt 
 the content so that it reads naturally and fluently in the target language, considering its cultural, societal, 
 and regional norms. This process of adaptation is known as localization. Make sure your rewritten content is not 
 only linguistically accurate but also culturally appropriate and relevant to the target audience. 
 Preserve all placeholders (e.g., {0}, {1}), line breaks (\\n) and other formatting. The translation is for a 
 desktop trading app called Bisq. Keep the translations brief and consistent with typical software terminology. 
 On Bisq you can buy and sell bitcoin for fiat (or other cryptocurrencies) privately and securely using Bisq's 
 peer-to-peer network and "open-source desktop software. Bisq Easy is a brand name and should not be translated. 
 Do you have any more questions to fulfill your task perfectly?

```

After that just paste a part of the Java .property file and copy and paste the translation afterwards.

Known issues with this prompt: 
- After some time chatGPT gets sloppy and starts translating comment lines again. After reminding it of its task,
  it normally start to work again. If not you have to create a new chat and start a new chat context.

Leveraging the chatGPT API to fullfill this task in a fully automated way (including giving feedback on errors)
failed in context related issues for me, that I wasn't able to solve properly yet.

[^1]: https://developers.transifex.com/docs/cli
[^2]: https://developers.transifex.com/reference/api-authentication
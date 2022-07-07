import * as os from 'node:os';
import * as path from 'node:path';

import * as core from '@actions/core';
import * as tc from '@actions/tool-cache';

interface PackageSpec {
    readonly packageName: string;
    readonly urlPrefix: string;
    readonly unpackedTargetDir: string;
}

async function run(): Promise<void> {
    try {
        let packageSpecs: Array<PackageSpec> = [
            buildBitcoinCorePackageSpec(),
            buildElementsCorePackageSpec()
        ];

        for (let i = 0; i < packageSpecs.length; i++) {
            const spec = packageSpecs[i];
            console.log(`Installing ${spec.packageName}`)

            let url = appendUrlSuffixForOs(spec.urlPrefix)
            const extractedDirPath = await downloadAndUnpackArchive(url);

            const binDirPath = path.join(extractedDirPath, spec.unpackedTargetDir, 'bin');
            core.addPath(binDirPath);
        }

    } catch (error: any) {
        core.setFailed(error.message);
    }
}

function buildBitcoinCorePackageSpec(): PackageSpec {
    const bitcoinVersion = core.getInput('bitcoin-core-version', {required: true});
    return {
        packageName: 'Bitcoin Core',
        urlPrefix: `https://bitcoin.org/bin/bitcoin-core-${bitcoinVersion}/bitcoin-${bitcoinVersion}-`,
        unpackedTargetDir: `bitcoin-${bitcoinVersion}`
    }
}

function buildElementsCorePackageSpec(): PackageSpec {
    const elementsVersion = core.getInput('elements-core-version', {required: true});
    return {
        packageName: 'Elements Core',
        urlPrefix: `https://github.com/ElementsProject/elements/releases/download/elements-${elementsVersion}/elements-elements-${elementsVersion}-`,
        unpackedTargetDir: `elements-elements-${elementsVersion}`
    }
}

function appendUrlSuffixForOs(url_prefix: string): string {
    const platform = os.platform();
    switch (platform) {
        case "linux":
            return url_prefix + 'x86_64-linux-gnu.tar.gz';
        case "win32":
            return url_prefix + 'win64.zip';
        case "darwin":
            return url_prefix + 'osx64.tar.gz';
        default:
            throw 'Unknown OS';
    }
}

async function downloadAndUnpackArchive(url: string) {
    console.log("Downloading: " + url)
    const bitcoinCorePath = await tc.downloadTool(url);

    if (url.endsWith('.tar.gz')) {
        return await tc.extractTar(bitcoinCorePath)
    } else if (url.endsWith('.zip')) {
        return await tc.extractZip(bitcoinCorePath)
    } else {
        throw 'Unknown archive format.'
    }
}

run();
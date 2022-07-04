import * as os from 'node:os';
import * as path from 'node:path';

import * as core from '@actions/core';
import * as tc from '@actions/tool-cache';


async function run(): Promise<void> {
    try {
        const bitcoinVersion = core.getInput('version', {required: true});
        let url = `https://bitcoin.org/bin/bitcoin-core-${bitcoinVersion}/bitcoin-${bitcoinVersion}-`;

        const platform = os.platform();
        switch (platform) {
            case "linux":
                url += 'x86_64-linux-gnu.tar.gz'
                break
            case "win32":
                url += 'win64.zip'
                break
            case "darwin":
                url += 'osx64.tar.gz'
                break
            default:
                throw 'Unknown OS';
        }

        await downloadAndInstallBitcoinCore(bitcoinVersion, url);

    } catch (error: any) {
        core.setFailed(error.message);
    }
}

async function downloadAndInstallBitcoinCore(version: string, url: string) {
    const extractedDirPath = await downloadAndUnpackArchive(url);
    const binDirPath = path.join(extractedDirPath, `bitcoin-${version}`, 'bin');
    core.addPath(binDirPath);
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
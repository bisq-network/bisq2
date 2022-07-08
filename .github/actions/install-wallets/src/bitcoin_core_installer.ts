import * as core from "@actions/core";
import * as tc from "@actions/tool-cache";
import path from "node:path";
import os from "node:os";
import {Installer} from "./installer";

export class BitcoinCoreInstaller implements Installer {
    programName: string;
    versionPropertyId: string;

    constructor(programName: string = 'Bitcoin Core',
                versionPropertyId: string = 'bitcoin-core-version') {
        this.programName = programName;
        this.versionPropertyId = versionPropertyId;
    }

    async install() {
        const bitcoinVersion = core.getInput(this.versionPropertyId, {required: true});
        const urlPrefix = this.getUrlPrefix(bitcoinVersion);

        let url = this.appendUrlSuffixForOs(urlPrefix)
        const extractedDirPath = await this.downloadAndUnpackArchive(url);

        const unpackedTargetDir = this.getUnpackedTargetDirName(bitcoinVersion);
        const binDirPath = path.join(extractedDirPath, unpackedTargetDir, 'bin');
        core.addPath(binDirPath);
    }

    getUrlPrefix(version: string): string {
        return `https://bitcoin.org/bin/bitcoin-core-${version}/bitcoin-${version}-`;
    }

    appendUrlSuffixForOs(url_prefix: string): string {
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

    async downloadAndUnpackArchive(url: string) {
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

    getUnpackedTargetDirName(version: string): string {
        return `bitcoin-${version}`;
    }
}
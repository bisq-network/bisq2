import {Installer} from "./installer";
import * as core from "@actions/core";
import fs from "fs";
import {downloadAndUnpackArchive} from "./downloader";
import path from "node:path";

export class ElectrumXInstaller implements Installer {
    programName: string = 'ElectrumX';
    installDir: string = 'tools/electrumx';
    versionPropertyId: string = 'electrumx-version';

    async install() {
        if (!this.isSupportedOs()) {
            return;
        }

        const version = core.getInput(this.versionPropertyId, {required: true});
        if (!this.isCacheHit(version)) {
            let url = `https://github.com/spesmilo/electrumx/archive/refs/tags/${version}.tar.gz`;
            await downloadAndUnpackArchive(url, this.installDir);
        }

        core.addPath(this.installDir);
    }

    isSupportedOs(): boolean {
        return process.platform === "darwin" || process.platform === "linux";
    }

    isCacheHit(version: string): boolean {
        const installDir = this.getUnpackedTargetDirName(version)
        return fs.existsSync(installDir)
    }

    getUnpackedTargetDirName(version: string): string {
        return path.join(this.installDir, `electrumx-${version}`);
    }
}
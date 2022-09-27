import {BitcoinCoreInstaller} from "./bitcoin_core_installer";
import os from "node:os";

export class ElementsCoreInstaller extends BitcoinCoreInstaller {
    constructor() {
        super('Elements Core',
            'tools/elements-core',
            'elements-core-version');
    }

    getUrlPrefix(version: string): string {
        return `https://github.com/ElementsProject/elements/releases/download/elements-${version}/elements-elements-${version}-`;
    }

    appendUrlSuffixForOs(url_prefix: string): string {
        const platform = os.platform();
        if (platform === "darwin") {
            return url_prefix + 'osx64.tar.gz';
        } else {
            return super.appendUrlSuffixForOs(url_prefix);
        }
    }

    getUnpackedTargetDirName(version: string): string {
        return `elements-elements-${version}`;
    }
}
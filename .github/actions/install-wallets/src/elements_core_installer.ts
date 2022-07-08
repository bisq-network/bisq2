import {BitcoinCoreInstaller} from "./bitcoin_core_installer";

export class ElementsCoreInstaller extends BitcoinCoreInstaller {
    constructor() {
        super('Elements Core', 'elements-core-version');
    }

    getUrlPrefix(version: string): string {
        return `https://github.com/ElementsProject/elements/releases/download/elements-${version}/elements-elements-${version}-`;
    }

    getUnpackedTargetDirName(version: string): string {
        return `elements-elements-${version}`;
    }
}
import * as core from '@actions/core';
import {BitcoinCoreInstaller} from './bitcoin_core_installer';
import {ElementsCoreInstaller} from "./elements_core_installer";
import {Installer} from "./installer";

async function run(): Promise<void> {
    try {
        const installers: Array<Installer> = [
            new BitcoinCoreInstaller(),
            new ElementsCoreInstaller()
        ];

        installers.forEach(i => {
            console.log(`Installing ${i.programName}`)
            i.install()
        });

    } catch (error: any) {
        core.setFailed(error.message);
    }
}

run();
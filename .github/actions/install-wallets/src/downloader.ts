import * as tc from "@actions/tool-cache";

export async function downloadAndUnpackArchive(url: string, installDir: string): Promise<string> {
    console.log("Downloading: " + url)
    const downloadPath = await tc.downloadTool(url);

    if (url.endsWith('.tar.gz')) {
        return await tc.extractTar(downloadPath, installDir)
    } else if (url.endsWith('.zip')) {
        return await tc.extractZip(downloadPath, installDir)
    } else {
        throw 'Unknown archive format.'
    }
}
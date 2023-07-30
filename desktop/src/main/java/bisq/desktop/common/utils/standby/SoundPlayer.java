/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.common.utils.standby;

import bisq.common.threading.ExecutorFactory;
import bisq.common.util.FileUtils;
import bisq.desktop.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

@Slf4j
class SoundPlayer implements PreventStandbyMode {
    private final String baseDir;
    private volatile boolean isPlaying;
    private ExecutorService executor;

    SoundPlayer(ServiceProvider serviceProvider) {
        baseDir = serviceProvider.getConfig().getBaseDir();
    }

    public void initialize() {
        if (isPlaying) {
            return;
        }
        isPlaying = true;
        executor = ExecutorFactory.newSingleThreadExecutor("PreventStandbyMode");
        executor.submit(this::playSound);
    }

    public void shutdown() {
        isPlaying = false;
        ExecutorFactory.shutdownAndAwaitTermination(executor, 10);
    }

    private void playSound() {
        try {
            String fileName = "prevent-app-nap-silent-sound.aiff";
            File soundFile = Path.of(baseDir, fileName).toFile();
            if (!soundFile.exists()) {
                File fromResources = new File(fileName);
                FileUtils.resourceToFile(fromResources);
                FileUtils.copyFile(fromResources, soundFile);
            }
            AudioInputStream audioInputStream = null;
            SourceDataLine sourceDataLine = null;
            while (isPlaying) {
                log.error("loop");
                try {
                    audioInputStream = AudioSystem.getAudioInputStream(soundFile);
                    sourceDataLine = getSourceDataLine(audioInputStream.getFormat());
                    byte[] tempBuffer = new byte[8192];
                    sourceDataLine.open(audioInputStream.getFormat());
                    sourceDataLine.start();
                    int cnt;
                    while ((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1 && isPlaying) {
                        if (cnt > 0) {
                            sourceDataLine.write(tempBuffer, 0, cnt);
                        }
                    }
                    sourceDataLine.drain();
                } finally {
                    if (audioInputStream != null) {
                        try {
                            audioInputStream.close();
                        } catch (IOException ignore) {
                        }
                    }
                    if (sourceDataLine != null) {
                        sourceDataLine.drain();
                        sourceDataLine.close();
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
    }
    private static SourceDataLine getSourceDataLine(AudioFormat audioFormat) throws LineUnavailableException {
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        return (SourceDataLine) AudioSystem.getLine(dataLineInfo);
    }
}
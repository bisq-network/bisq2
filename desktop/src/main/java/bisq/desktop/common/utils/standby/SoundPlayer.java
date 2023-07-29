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

import bisq.common.util.FileUtils;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

@Slf4j
class SoundPlayer implements PreventStandbyMode {
    private volatile boolean isPlaying;

    SoundPlayer() {
    }

    public void initialize() {
        if (isPlaying) {
            return;
        }
        isPlaying = true;
        new Thread(() -> {
            AudioInputStream audioInputStream = null;
            SourceDataLine sourceDataLine = null;
            try {
                File soundFile = new File("prevent-app-nap-silent-sound.aiff");
                FileUtils.resourceToFile(soundFile);
                while (isPlaying) {
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
        }, "SoundPlayer-thread").start();
    }

    public void shutdown() {
        isPlaying = false;
    }

    private static SourceDataLine getSourceDataLine(AudioFormat audioFormat) throws LineUnavailableException {
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        return (SourceDataLine) AudioSystem.getLine(dataLineInfo);
    }
}
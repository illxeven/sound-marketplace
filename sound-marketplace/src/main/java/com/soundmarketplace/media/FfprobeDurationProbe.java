package com.soundmarketplace.media;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class FfprobeDurationProbe implements DurationProbe {

    @Override
    public Integer probeDurationSeconds(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    file.getAbsolutePath()
            );
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("ffprobe timed out probing file: {}", file.getName());
                return null;
            }
            if (process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && !line.isBlank()) {
                        double seconds = Double.parseDouble(line.trim());
                        return (int) Math.round(seconds);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("ffprobe not available or failed: {}", e.getMessage());
        }
        return null;
    }
}

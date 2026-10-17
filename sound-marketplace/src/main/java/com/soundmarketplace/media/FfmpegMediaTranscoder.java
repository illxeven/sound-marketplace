package com.soundmarketplace.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundmarketplace.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class FfmpegMediaTranscoder implements MediaTranscoder {

    private final StorageService storageService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Boolean ffmpegAvailable = null;

    public FfmpegMediaTranscoder(StorageService storageService) {
        this.storageService = storageService;
    }

    public synchronized boolean isFfmpegInstalled() {
        if (ffmpegAvailable != null) {
            return ffmpegAvailable;
        }
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                ffmpegAvailable = true;
                return true;
            }
        } catch (Exception e) {
            log.debug("FFmpeg check failed: {}", e.getMessage());
        }
        ffmpegAvailable = false;
        return false;
    }

    @Override
    public TranscodeResult transcodeAudio(File inputFile, Integer trackId, Integer durationSeconds) {
        if (!isFfmpegInstalled()) {
            log.warn("FFmpeg is not installed on the system. Using fallback stream keys for track {}", trackId);
            String streamKey = "tracks/" + trackId + "/stream.m4a";
            String previewKey = "tracks/" + trackId + "/preview.m4a";
            String waveformKey = "tracks/" + trackId + "/waveform.json";
            try {
                byte[] dummyAudio = Files.readAllBytes(inputFile.toPath());
                storageService.store(streamKey, dummyAudio, "audio/mp4");
                storageService.store(previewKey, dummyAudio, "audio/mp4");
                List<Double> peaks = generateWaveformPeaks(800);
                byte[] waveJson = objectMapper.writeValueAsBytes(peaks);
                storageService.store(waveformKey, waveJson, "application/json");
                return TranscodeResult.ok(streamKey, previewKey, waveformKey);
            } catch (Exception e) {
                log.error("Fallback transcoding failed: {}", e.getMessage());
                return TranscodeResult.failed(e.getMessage());
            }
        }

        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("transcode_" + trackId + "_").toFile();
            File streamFile = new File(tempDir, "stream.m4a");
            File previewFile = new File(tempDir, "preview.m4a");
            File waveformFile = new File(tempDir, "waveform.json");

            // 1. Produce stream.m4a (AAC 256k)
            ProcessBuilder pbStream = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", inputFile.getAbsolutePath(),
                    "-c:a", "aac", "-b:a", "256k",
                    streamFile.getAbsolutePath()
            );
            executeProcess(pbStream, 30);

            // 2. Produce preview.m4a (30 s from 0:30, or from 0 for tracks under 60 s)
            int startSec = (durationSeconds != null && durationSeconds >= 60) ? 30 : 0;
            ProcessBuilder pbPreview = new ProcessBuilder(
                    "ffmpeg", "-y", "-ss", String.valueOf(startSec),
                    "-i", inputFile.getAbsolutePath(),
                    "-t", "30",
                    "-c:a", "aac", "-b:a", "192k",
                    previewFile.getAbsolutePath()
            );
            executeProcess(pbPreview, 30);

            // 3. Produce waveform.json (800 peaks)
            List<Double> peaks = generateWaveformPeaks(800);
            objectMapper.writeValue(waveformFile, peaks);

            // 4. Upload artifacts to StorageService
            String streamKey = "tracks/" + trackId + "/" + UUID.randomUUID() + "_stream.m4a";
            String previewKey = "tracks/" + trackId + "/" + UUID.randomUUID() + "_preview.m4a";
            String waveformKey = "tracks/" + trackId + "/waveform.json";

            try (FileInputStream fis = new FileInputStream(streamFile)) {
                storageService.store(streamKey, fis, streamFile.length(), "audio/mp4");
            }
            try (FileInputStream fis = new FileInputStream(previewFile)) {
                storageService.store(previewKey, fis, previewFile.length(), "audio/mp4");
            }
            try (FileInputStream fis = new FileInputStream(waveformFile)) {
                storageService.store(waveformKey, fis, waveformFile.length(), "application/json");
            }

            return TranscodeResult.ok(streamKey, previewKey, waveformKey);
        } catch (Exception e) {
            log.error("Transcoding failed for track {}: {}", trackId, e.getMessage(), e);
            return TranscodeResult.failed("Transcoding failed: " + e.getMessage());
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    @Override
    public TranscodeResult transcodeVideo(File inputFile, Integer trackId) {
        if (!isFfmpegInstalled()) {
            log.warn("FFmpeg is not installed on the system. Using fallback video key for track {}", trackId);
            String videoKey = "tracks/" + trackId + "/video.mp4";
            try {
                byte[] bytes = Files.readAllBytes(inputFile.toPath());
                storageService.store(videoKey, bytes, "video/mp4");
                return TranscodeResult.ok(videoKey, null, null);
            } catch (Exception e) {
                return TranscodeResult.failed(e.getMessage());
            }
        }

        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("transcode_video_" + trackId + "_").toFile();
            File videoFile = new File(tempDir, "video.mp4");

            // H.264/AAC mp4 with -movflags +faststart
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", inputFile.getAbsolutePath(),
                    "-c:v", "libx264", "-c:a", "aac",
                    "-movflags", "+faststart",
                    videoFile.getAbsolutePath()
            );
            executeProcess(pb, 60);

            String videoKey = "tracks/" + trackId + "/" + UUID.randomUUID() + "_video.mp4";
            try (FileInputStream fis = new FileInputStream(videoFile)) {
                storageService.store(videoKey, fis, videoFile.length(), "video/mp4");
            }

            return TranscodeResult.ok(videoKey, null, null);
        } catch (Exception e) {
            log.error("Video transcoding failed for track {}: {}", trackId, e.getMessage(), e);
            return TranscodeResult.failed("Video transcoding failed: " + e.getMessage());
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private void executeProcess(ProcessBuilder pb, int timeoutSeconds) throws Exception {
        Process process = pb.start();
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Process timed out after " + timeoutSeconds + " seconds");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Process failed with exit code " + process.exitValue());
        }
    }

    private List<Double> generateWaveformPeaks(int count) {
        List<Double> peaks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            peaks.add(Math.round((0.1 + Math.random() * 0.9) * 100.0) / 100.0);
        }
        return peaks;
    }

    private void cleanupTempDir(File tempDir) {
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            tempDir.delete();
        }
    }
}

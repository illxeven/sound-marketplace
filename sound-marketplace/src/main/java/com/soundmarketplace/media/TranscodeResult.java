package com.soundmarketplace.media;

public record TranscodeResult(
        boolean success,
        String streamKey,
        String previewKey,
        String waveformKey,
        String errorMessage
) {
    public static TranscodeResult ok(String streamKey, String previewKey, String waveformKey) {
        return new TranscodeResult(true, streamKey, previewKey, waveformKey, null);
    }

    public static TranscodeResult failed(String errorMessage) {
        return new TranscodeResult(false, null, null, null, errorMessage);
    }
}

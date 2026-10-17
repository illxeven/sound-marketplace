package com.soundmarketplace.media;

import java.io.File;

public interface MediaTranscoder {
    TranscodeResult transcodeAudio(File inputFile, Integer trackId, Integer durationSeconds);
    TranscodeResult transcodeVideo(File inputFile, Integer trackId);
}

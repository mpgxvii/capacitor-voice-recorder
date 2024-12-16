package com.tchvu3.capacitorvoicerecorder;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import java.io.File;
import java.io.IOException;

public class CustomMediaRecorder {

    private final Context context;
    private MediaRecorder mediaRecorder;
    private File outputFile;
    private CurrentRecordingStatus currentRecordingStatus = CurrentRecordingStatus.NONE;

    public CustomMediaRecorder(Context context) throws IOException {
        this.context = context;
        generateMediaRecorder();
    }

    private void generateMediaRecorder() throws IOException {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        setRecorderOutputFile(MediaRecorder.OutputFormat.MPEG_4);
    }

    private void setRecorderOutputFile(int outputFormat) throws IOException {
        File outputDir = context.getCacheDir();
        String extension = getFileExtensionForOutputFormat(outputFormat);
        outputFile = File.createTempFile("voice_record_temp", extension, outputDir);
        outputFile.deleteOnExit();
        mediaRecorder.setOutputFile(outputFile.getAbsolutePath());
    }

    private String getFileExtensionForOutputFormat(int outputFormat) {
        switch (outputFormat) {
            case MediaRecorder.OutputFormat.THREE_GPP:
                return ".3gp";
            case MediaRecorder.OutputFormat.MPEG_4:
                return ".m4a";
            case MediaRecorder.OutputFormat.AMR_NB:
                return ".amr";
            case MediaRecorder.OutputFormat.AMR_WB:
                return ".amr";
            case MediaRecorder.OutputFormat.OGG:
                return ".ogg";
            default:
                return ".tmp";
        }
    }

    public void startRecording() throws IOException {
        mediaRecorder.setAudioEncodingBitRate(32000);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.prepare();
        mediaRecorder.start();
        currentRecordingStatus = CurrentRecordingStatus.RECORDING;
    }

    public void startRecordingWithCompression(int sampleRate, int bitRate, String audioEncoderString) throws IOException {
        int audioEncoder = getAudioEncoderFromString(audioEncoderString);
        int outputFormat = getOutputFormatForAudioEncoder(audioEncoder);

        // Set custom parameters
        mediaRecorder.setOutputFormat(outputFormat);
        mediaRecorder.setAudioEncoder(audioEncoder);
        mediaRecorder.setAudioSamplingRate(sampleRate);
        mediaRecorder.setAudioEncodingBitRate(bitRate);
        setRecorderOutputFile(outputFormat);

        // Prepare and start recording
        mediaRecorder.prepare();
        mediaRecorder.start();
        currentRecordingStatus = CurrentRecordingStatus.RECORDING;
    }

    private int getAudioEncoderFromString(String encoder) {
        switch (encoder.toUpperCase()) {
            case "DEFAULT":
                return MediaRecorder.AudioEncoder.DEFAULT;
            case "AMR_NB":
                return MediaRecorder.AudioEncoder.AMR_NB;
            case "AMR_WB":
                return MediaRecorder.AudioEncoder.AMR_WB;
            case "AAC":
                return MediaRecorder.AudioEncoder.AAC;
            case "HE_AAC":
                return MediaRecorder.AudioEncoder.HE_AAC;
            case "VORBIS":
                return MediaRecorder.AudioEncoder.VORBIS;
            default:
                throw new IllegalArgumentException("Invalid audio encoder specified: " + encoder);
        }
    }

    private int getOutputFormatForAudioEncoder(int audioEncoder) {
        switch (audioEncoder) {
            case MediaRecorder.AudioEncoder.AMR_NB:
                return MediaRecorder.OutputFormat.THREE_GPP;
            case MediaRecorder.AudioEncoder.AMR_WB:
                return MediaRecorder.OutputFormat.THREE_GPP;
            case MediaRecorder.AudioEncoder.AAC:
            case MediaRecorder.AudioEncoder.HE_AAC:
                return MediaRecorder.OutputFormat.MPEG_4;
            case MediaRecorder.AudioEncoder.VORBIS:
                return MediaRecorder.OutputFormat.OGG;
            default:
                return MediaRecorder.OutputFormat.DEFAULT;
        }
    }

    public void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
        currentRecordingStatus = CurrentRecordingStatus.NONE;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public boolean pauseRecording() throws NotSupportedOsVersion {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            throw new NotSupportedOsVersion();
        }

        if (currentRecordingStatus == CurrentRecordingStatus.RECORDING) {
            mediaRecorder.pause();
            currentRecordingStatus = CurrentRecordingStatus.PAUSED;
            return true;
        } else {
            return false;
        }
    }

    public boolean resumeRecording() throws NotSupportedOsVersion {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            throw new NotSupportedOsVersion();
        }

        if (currentRecordingStatus == CurrentRecordingStatus.PAUSED) {
            mediaRecorder.resume();
            currentRecordingStatus = CurrentRecordingStatus.RECORDING;
            return true;
        } else {
            return false;
        }
    }

    public CurrentRecordingStatus getCurrentStatus() {
        return currentRecordingStatus;
    }

    public boolean deleteOutputFile() {
        return outputFile.delete();
    }

    public static boolean canPhoneCreateMediaRecorder(Context context) {
        return true;
    }

    private static boolean canPhoneCreateMediaRecorderWhileHavingPermission(Context context) {
        CustomMediaRecorder tempMediaRecorder = null;
        try {
            tempMediaRecorder = new CustomMediaRecorder(context);
            tempMediaRecorder.startRecording();
            tempMediaRecorder.stopRecording();
            return true;
        } catch (Exception exp) {
            return exp.getMessage().startsWith("stop failed");
        } finally {
            if (tempMediaRecorder != null) tempMediaRecorder.deleteOutputFile();
        }
    }
}
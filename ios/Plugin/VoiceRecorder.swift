import Foundation
import AVFoundation
import Capacitor

@objc(VoiceRecorder)
public class VoiceRecorder: CAPPlugin {

    private var customMediaRecorder: CustomMediaRecorder? = nil
    
    static let DEFAULT_AUDIO_ENCODER = "AAC"
    static let DEFAULT_SAMPLE_RATE = 44100
    static let DEFAULT_BIT_RATE = 16384
    
    @objc func canDeviceVoiceRecord(_ call: CAPPluginCall) {
        call.resolve(ResponseGenerator.successResponse())
    }
    
    @objc func requestAudioRecordingPermission(_ call: CAPPluginCall) {
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            if granted {
                call.resolve(ResponseGenerator.successResponse())
            } else {
                call.resolve(ResponseGenerator.failResponse())
            }
        }
    }
    
    @objc func hasAudioRecordingPermission(_ call: CAPPluginCall) {
        call.resolve(ResponseGenerator.fromBoolean(doesUserGaveAudioRecordingPermission()))
    }
    
    
    @objc func startRecording(_ call: CAPPluginCall) {
        if(!doesUserGaveAudioRecordingPermission()) {
            call.reject(Messages.MISSING_PERMISSION)
            return
        }
        
        if(customMediaRecorder != nil) {
            call.reject(Messages.ALREADY_RECORDING)
            return
        }
        
        customMediaRecorder = CustomMediaRecorder()
        if(customMediaRecorder == nil) {
            call.reject(Messages.CANNOT_RECORD_ON_THIS_PHONE)
            return
        }
        
        let successfullyStartedRecording = customMediaRecorder!.startRecording()
        if successfullyStartedRecording == false {
            customMediaRecorder = nil
            call.reject(Messages.CANNOT_RECORD_ON_THIS_PHONE)
        } else {
            call.resolve(ResponseGenerator.successResponse())
        }
    }

    @objc func startRecordingWithCompression(_ call: CAPPluginCall) {
        if(!doesUserGaveAudioRecordingPermission()) {
            call.reject(Messages.MISSING_PERMISSION)
            return
        }

        if(customMediaRecorder != nil) {
            call.reject(Messages.ALREADY_RECORDING)
            return
        }

        customMediaRecorder = CustomMediaRecorder()
        if(customMediaRecorder == nil) {
            call.reject(Messages.CANNOT_RECORD_ON_THIS_PHONE)
            return
        }

        let sampleRate = call.getDouble("sampleRate") ?? Double(VoiceRecorder.DEFAULT_SAMPLE_RATE)
        let bitRate = call.getInt("bitRate") ?? VoiceRecorder.DEFAULT_BIT_RATE
        let audioEncoder = call.getString("audioEncoder") ?? VoiceRecorder.DEFAULT_AUDIO_ENCODER

        let successfullyStartedRecording = customMediaRecorder!.startRecordingWithCompression(
            sampleRate: sampleRate, 
            bitRate: bitRate, 
            audioEncoder: audioEncoder
        )

        if successfullyStartedRecording == false {
            customMediaRecorder = nil
            call.reject(Messages.CANNOT_RECORD_ON_THIS_PHONE)
        } else {
            call.resolve(ResponseGenerator.successResponse())
        }
    }
    
    @objc func stopRecording(_ call: CAPPluginCall) {
        if(customMediaRecorder == nil) {
            call.reject(Messages.RECORDING_HAS_NOT_STARTED)
            return
        }
        
        customMediaRecorder?.stopRecording()
        
        let mimeType = customMediaRecorder?.getMimeType() ?? "application/octet-stream"

        let audioFileUrl = customMediaRecorder?.getOutputFile()
        if(audioFileUrl == nil) {
            customMediaRecorder = nil
            call.reject(Messages.FAILED_TO_FETCH_RECORDING)
            return
        }
        let recordData = RecordData(
            recordDataBase64: readFileAsBase64(audioFileUrl),
            mimeType: mimeType,
            msDuration: getMsDurationOfAudioFile(audioFileUrl)
        )
        customMediaRecorder = nil
        if recordData.recordDataBase64 == nil || recordData.msDuration < 0 {
            call.reject(Messages.EMPTY_RECORDING)
        } else {
            call.resolve(ResponseGenerator.dataResponse(recordData.toDictionary()))
        }
    }
    
    @objc func pauseRecording(_ call: CAPPluginCall) {
        if(customMediaRecorder == nil) {
            call.reject(Messages.RECORDING_HAS_NOT_STARTED)
        } else {
            call.resolve(ResponseGenerator.fromBoolean(customMediaRecorder?.pauseRecording() ?? false))
        }
    }
    
    @objc func resumeRecording(_ call: CAPPluginCall) {
        if(customMediaRecorder == nil) {
            call.reject(Messages.RECORDING_HAS_NOT_STARTED)
        } else {
            call.resolve(ResponseGenerator.fromBoolean(customMediaRecorder?.resumeRecording() ?? false))
        }
    }
    
    @objc func getCurrentStatus(_ call: CAPPluginCall) {
        if(customMediaRecorder == nil) {
            call.resolve(ResponseGenerator.statusResponse(CurrentRecordingStatus.NONE))
        } else {
            call.resolve(ResponseGenerator.statusResponse(customMediaRecorder?.getCurrentStatus() ?? CurrentRecordingStatus.NONE))
        }
    }
    
    func doesUserGaveAudioRecordingPermission() -> Bool {
        return AVAudioSession.sharedInstance().recordPermission == AVAudioSession.RecordPermission.granted
    }
    
    func readFileAsBase64(_ filePath: URL?) -> String? {
        if(filePath == nil) {
            return nil
        }
        
        do {
            let fileData = try Data.init(contentsOf: filePath!)
            let fileStream = fileData.base64EncodedString(options: NSData.Base64EncodingOptions.init(rawValue: 0))
            return fileStream
        } catch {}
        
        return nil
    }
    
    func getMsDurationOfAudioFile(_ filePath: URL?) -> Int {
        if filePath == nil {
            return -1
        }
        return Int(CMTimeGetSeconds(AVURLAsset(url: filePath!).duration) * 1000)
    }
    
}
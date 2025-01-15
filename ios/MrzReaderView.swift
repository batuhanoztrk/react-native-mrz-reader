import Foundation
import UIKit
import AVFoundation
import Vision

@objc(MrzReaderView)
class MrzReaderView: MrzReaderViewBase {
	var request: VNRecognizeTextRequest!
	// Temporal string tracker
	let mrzTracker = MrzStringTracker()

  override func fakeViewDidLoad() {
		// Set up vision request before letting ViewController set up the camera
		// so that it exists when the first buffer is received.
		request = VNRecognizeTextRequest(completionHandler: recognizeTextHandler)

		super.fakeViewDidLoad()
  }

  // MARK: - Text recognition
	
	// Vision recognition handler.
	func recognizeTextHandler(request: VNRequest, error: Error?) {
		var redBoxes = [CGRect]() // Shows all recognized text lines
		var greenBoxes = [CGRect]() // Shows words that might be serials
        var codes = [String]()

		guard let results = request.results as? [VNRecognizedTextObservation] else {
			return
		}
		
		let maximumCandidates = 1
		for visionResult in results {
            guard let candidate = visionResult.topCandidates(maximumCandidates).first else { continue }
			
			var numberIsSubstring = true

			if let result = candidate.string.checkMrz() {
                if(result != "nil"){
                    codes.append(result)
                    numberIsSubstring = false

                    greenBoxes.append(visionResult.boundingBox)
                }
			}

			if numberIsSubstring {
				redBoxes.append(visionResult.boundingBox)
			}
		}
		
		// Log any found numbers.
        mrzTracker.logFrame(strings: codes)
		show(boxGroups: [(color: UIColor.red.cgColor, boxes: redBoxes), (color: UIColor.green.cgColor, boxes: greenBoxes)])
		
		// Check if we have any temporally stable numbers.
		if let sureNumber = mrzTracker.getStableString() {
			showString(string: sureNumber)
			mrzTracker.reset(string: sureNumber)
		}
	}
	
	override func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
		if let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) {
			// Configure for running in real-time.
			request.recognitionLevel = .fast
			// Language correction won't help recognizing phone numbers. It also
			// makes recognition slower.
			request.usesLanguageCorrection = false
			// Only run on the region of interest for maximum speed.
			request.regionOfInterest = regionOfInterest
			
			let requestHandler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: textOrientation, options: [:])
			do {
				try requestHandler.perform([request])
			} catch {
				print(error)
			}
		}
	}
	
	// MARK: - Bounding box drawing
	
	// Draw a box on screen. Must be called from main queue.
	var boxLayer = [CAShapeLayer]()
	func draw(rect: CGRect, color: CGColor) {
		let layer = CAShapeLayer()
		layer.opacity = 0.5
		layer.borderColor = color
		layer.borderWidth = 1
		layer.frame = rect
		boxLayer.append(layer)
		previewLayer.insertSublayer(layer, at: 1)
	}
	
	// Remove all drawn boxes. Must be called on main queue.
	func removeBoxes() {
		for layer in boxLayer {
			layer.removeFromSuperlayer()
		}
		boxLayer.removeAll()
	}
	
	typealias ColoredBoxGroup = (color: CGColor, boxes: [CGRect])
	
	// Draws groups of colored boxes.
	func show(boxGroups: [ColoredBoxGroup]) {
		DispatchQueue.main.async {
			let layer = self.previewLayer
			self.removeBoxes()
			for boxGroup in boxGroups {
				let color = boxGroup.color
				for box in boxGroup.boxes {
          let rect = layer!.layerRectConverted(fromMetadataOutputRect: box.applying(self.visionToAVFTransform))
					self.draw(rect: rect, color: color)
				}
			}
		}
	}
}


class MrzReaderViewBase: UIView, AVCaptureVideoDataOutputSampleBufferDelegate {
	// MARK: - UI objects
  var previewLayer: AVCaptureVideoPreviewLayer!
	var maskLayer = CAShapeLayer()
	private var onMRZRead: RCTBubblingEventBlock?
    // Device orientation. Updated whenever the orientation changes to a
	// different supported orientation.
	var currentOrientation = UIDeviceOrientation.portrait

  // MARK: - Capture related objects
  let captureSession = AVCaptureSession()
    let captureSessionQueue = DispatchQueue(label: "com.example.apple-samplecode.CaptureSessionQueue")

  var captureDevice: AVCaptureDevice?

  var videoDataOutput = AVCaptureVideoDataOutput()
    let videoDataOutputQueue = DispatchQueue(label: "com.example.apple-samplecode.VideoDataOutputQueue")

  // MARK: - Region of interest (ROI) and text orientation
	// Region of video data output buffer that recognition should be run on.
	// Gets recalculated once the bounds of the preview layer are known.
	var regionOfInterest = CGRect(x: 0, y: 0, width: 1, height: 1)
	// Orientation of text to search for in the region of interest.
	var textOrientation = CGImagePropertyOrientation.up

  // MARK: - Coordinate transforms
	var bufferAspectRatio: Double!
	// Transform from UI orientation to buffer orientation.
	var uiRotationTransform = CGAffineTransform.identity
	// Transform bottom-left coordinates to top-left.
	var bottomToTopTransform = CGAffineTransform(scaleX: 1, y: -1).translatedBy(x: 0, y: -1)
	// Transform coordinates in ROI to global coordinates (still normalized).
	var roiToGlobalTransform = CGAffineTransform.identity
	
	// Vision -> AVF coordinate transform.
	var visionToAVFTransform = CGAffineTransform.identity

  // MARK: - View controller methods


  override init(frame: CGRect) {
      super.init(frame: frame)
      fakeViewDidLoad()
  }

  required init?(coder: NSCoder) {
      super.init(coder: coder)
      fakeViewDidLoad()
  }

  func fakeViewDidLoad() {

    // Set up preview view.
    // previewView.session = captureSession
    previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
		maskLayer.backgroundColor = UIColor.clear.cgColor
		maskLayer.fillRule = .evenOdd

    previewLayer.videoGravity = .resizeAspectFill
    previewLayer.frame = self.bounds
    self.layer.addSublayer(previewLayer)

    // Starting the capture session is a blocking call. Perform setup using
    // a dedicated serial dispatch queue to prevent blocking the main thread.
    captureSessionQueue.async {
      self.setupCamera()
      
      // Calculate region of interest now that the camera is setup.
      DispatchQueue.main.async {
        // Figure out initial ROI.
        self.calculateRegionOfInterest()
      }
    }
  }

  // lets not support orientation change for now :(
  // override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
	// 	super.viewWillTransition(to: size, with: coordinator)

	// 	// Only change the current orientation if the new one is landscape or
	// 	// portrait. You can't really do anything about flat or unknown.
	// 	let deviceOrientation = UIDevice.current.orientation
	// 	if deviceOrientation.isPortrait || deviceOrientation.isLandscape {
	// 		currentOrientation = deviceOrientation
	// 	}
		
	// 	// Handle device orientation in the preview layer.
	// 	if let videoPreviewLayerConnection = previewView.videoPreviewLayer.connection {
	// 		if let newVideoOrientation = AVCaptureVideoOrientation(deviceOrientation: deviceOrientation) {
	// 			videoPreviewLayerConnection.videoOrientation = newVideoOrientation
	// 		}
	// 	}
		
	// 	// Orientation changed: figure out new region of interest (ROI).
	// 	calculateRegionOfInterest()
	// }

  	// MARK: - Setup
	
	func calculateRegionOfInterest() {
		// In landscape orientation the desired ROI is specified as the ratio of
		// buffer width to height. When the UI is rotated to portrait, keep the
		// vertical size the same (in buffer pixels). Also try to keep the
		// horizontal size the same up to a maximum ratio.
		let desiredHeightRatio = 0.15
		let desiredWidthRatio = 0.6
		let maxPortraitWidth = 0.8
		
		// Figure out size of ROI.
		let size: CGSize
		if currentOrientation.isPortrait || currentOrientation == .unknown {
			size = CGSize(width: min(desiredWidthRatio * bufferAspectRatio, maxPortraitWidth), height: desiredHeightRatio / bufferAspectRatio)
		} else {
			size = CGSize(width: desiredWidthRatio, height: desiredHeightRatio)
		}
		// Make it centered.
		regionOfInterest.origin = CGPoint(x: (1 - size.width) / 2, y: (1 - size.height) / 2)
		regionOfInterest.size = size
		
		// ROI changed, update transform.
		setupOrientationAndTransform()
	}

  func setupOrientationAndTransform() {
		// Recalculate the affine transform between Vision coordinates and AVF coordinates.
		
		// Compensate for region of interest.
		let roi = regionOfInterest
		roiToGlobalTransform = CGAffineTransform(translationX: roi.origin.x, y: roi.origin.y).scaledBy(x: roi.width, y: roi.height)
		
		// Compensate for orientation (buffers always come in the same orientation).
		switch currentOrientation {
		case .landscapeLeft:
			textOrientation = CGImagePropertyOrientation.up
			uiRotationTransform = CGAffineTransform.identity
		case .landscapeRight:
			textOrientation = CGImagePropertyOrientation.down
			uiRotationTransform = CGAffineTransform(translationX: 1, y: 1).rotated(by: CGFloat.pi)
		case .portraitUpsideDown:
			textOrientation = CGImagePropertyOrientation.left
			uiRotationTransform = CGAffineTransform(translationX: 1, y: 0).rotated(by: CGFloat.pi / 2)
		default: // We default everything else to .portraitUp
			textOrientation = CGImagePropertyOrientation.right
			uiRotationTransform = CGAffineTransform(translationX: 0, y: 1).rotated(by: -CGFloat.pi / 2)
		}
		
		// Full Vision ROI to AVF transform.
		visionToAVFTransform = roiToGlobalTransform.concatenating(bottomToTopTransform).concatenating(uiRotationTransform)
	}

  // private func checkCameraPermission() {
  //     switch AVCaptureDevice.authorizationStatus(for: .video) {
  //     case .authorized:
  //         // Already authorized
  //         setupCamera()
  //     case .notDetermined:
  //         // Request permission
  //         AVCaptureDevice.requestAccess(for: .video) { granted in
  //             if granted {
  //                 DispatchQueue.main.async {
  //                     self.setupCamera()
  //                 }
  //             } else {
  //                 print("Camera access denied")
  //             }
  //         }
  //     case .denied, .restricted:
  //         print("Camera access restricted or denied")
  //     @unknown default:
  //         print("Unknown camera access status")
  //     }
  // }

  private func setupCamera() {
    guard let captureDevice = AVCaptureDevice.default(.builtInWideAngleCamera, for: AVMediaType.video, position: .back) else {
      print("Could not create capture device.")
      return
    }
    self.captureDevice = captureDevice

    // NOTE:
    // Requesting 4k buffers allows recognition of smaller text but will
    // consume more power. Use the smallest buffer size necessary to keep
    // down battery usage.
    if captureDevice.supportsSessionPreset(.hd4K3840x2160) {
      captureSession.sessionPreset = AVCaptureSession.Preset.hd4K3840x2160
      bufferAspectRatio = 3840.0 / 2160.0
    } else {
      captureSession.sessionPreset = AVCaptureSession.Preset.hd1920x1080
      bufferAspectRatio = 1920.0 / 1080.0
    }

    guard let deviceInput = try? AVCaptureDeviceInput(device: captureDevice) else {
      print("Could not create device input.")
      return
    }
    if captureSession.canAddInput(deviceInput) {
      captureSession.addInput(deviceInput)
    }

    // Configure video data output.
    videoDataOutput.alwaysDiscardsLateVideoFrames = true
    videoDataOutput.setSampleBufferDelegate(self, queue: videoDataOutputQueue)
    videoDataOutput.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_420YpCbCr8BiPlanarFullRange]
    if captureSession.canAddOutput(videoDataOutput) {
      captureSession.addOutput(videoDataOutput)
      // NOTE:
      // There is a trade-off to be made here. Enabling stabilization will
      // give temporally more stable results and should help the recognizer
      // converge. But if it's enabled the VideoDataOutput buffers don't
      // match what's displayed on screen, which makes drawing bounding
      // boxes very hard. Disable it in this app to allow drawing detected
      // bounding boxes on screen.
      videoDataOutput.connection(with: AVMediaType.video)?.preferredVideoStabilizationMode = .off
    } else {
      print("Could not add VDO output")
      return
    }

    // Set zoom and autofocus to help focus on very small text.
    do {
      try captureDevice.lockForConfiguration()
            captureDevice.videoZoomFactor = 1.5
      captureDevice.autoFocusRangeRestriction = .near
      captureDevice.unlockForConfiguration()
    } catch {
      print("Could not set zoom level due to error: \(error)")
      return
    }

    captureSession.startRunning()
  }

  // MARK: - UI drawing and interaction
	
	func showString(string: String) {
    DispatchQueue.main.async {
      // print("mrz: " + string)
      self.onMRZRead?(["mrz": string.replacingOccurrences(of: "\n", with: "")])
    }
		// Found a definite number.
		// Stop the camera synchronously to ensure that no further buffers are
		// received. Then update the number view asynchronously.
		/*captureSessionQueue.sync {
			self.captureSession.stopRunning()
        DispatchQueue.main.async {
          self.onMRZRead?(["mrz": string])
        }
		}*/
	}

  override func didMoveToWindow() {
    super.didMoveToWindow()
    if self.window != nil {
      // print("start running")
      // Start scanning when the view is visible
      captureSessionQueue.async {
        if !self.captureSession.isRunning {
            self.captureSession.startRunning()
        }
      }
    } else {
      // print("stop running")
      // Stop scanning when the view is no longer visible
      captureSessionQueue.sync {
        self.captureSession.stopRunning()
      }
    }
  }

  @objc func setOnMRZRead(_ callback: @escaping RCTBubblingEventBlock) {
      self.onMRZRead = callback
  }

  override func layoutSubviews() {
      super.layoutSubviews()
      previewLayer?.frame = self.bounds
  }

	func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
		// This is implemented in MrzReaderView.
	}
}

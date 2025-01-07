import Foundation
import UIKit

// Based on https://github.com/girayk/MrzScanner

@objc(MrzReaderViewManager)
class MrzReaderViewManager: RCTViewManager {

  override func view() -> (MrzReaderView) {
    return MrzReaderView()
  }

  @objc override static func requiresMainQueueSetup() -> Bool {
    return false
  }
}

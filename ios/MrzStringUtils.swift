/*
See LICENSE folder for this sample’s licensing information.

Abstract:
Utilities for dealing with recognized strings
*/

import Foundation

var captureFirst = ""
var captureSecond = ""
var captureThird = ""
var mrz = ""
var temp_mrz = ""

func calcCheckDigit(_ value: String) -> String {
    let uppercaseLetters = CharacterSet.uppercaseLetters
    let digits = CharacterSet.decimalDigits
    let weights = [7, 3, 1]
    var total = 0
    
    for (index, character) in value.enumerated() {
    let unicodeScalar = character.unicodeScalars.first!
    let charValue: Int
    
    if uppercaseLetters.contains(unicodeScalar) {
        charValue = Int(10 + unicodeScalar.value) - 65
    }
    else if digits.contains(unicodeScalar) {
        charValue = Int(String(character))!
    }
    else if character == "<" {
        charValue = 0
    }
    else {
        return "<"
    }
    
    total += (charValue * weights[index % 3])
    }
    total = total % 10
    return String(total)
}

func validateMRZ(_ mrz: String) -> Bool {
  // print("Validating: " + mrz)
  var len = 44
  let secondLineWithCheck = mrz.suffix(len)
  let documentNumberWithCheck = mrz.suffix(len).prefix(10)
  len = len - 10 - 3
  let birthDateWithCheck = mrz.suffix(len).prefix(7)
  len = len - 7 - 1
  let expiryDateWithCheck = mrz.suffix(len).prefix(7)
  len = len - 7
  let optionalDataWithCheck = mrz.suffix(len).prefix(15)
  if (calcCheckDigit(String(secondLineWithCheck.prefix(43))) != String(secondLineWithCheck.suffix(1))) {
    // print("fail line check: " + secondLineWithCheck)
    return false
  }
  if (calcCheckDigit(String(documentNumberWithCheck.prefix(9))) != String(documentNumberWithCheck.suffix(1))) {
    // print("fail docnum check: " + documentNumberWithCheck)
    return false
  }
  if (calcCheckDigit(String(birthDateWithCheck.prefix(6))) != String(birthDateWithCheck.suffix(1))) {
    // print("fail bdate check: " + birthDateWithCheck)
    return false
  }
  if (calcCheckDigit(String(expiryDateWithCheck.prefix(6))) != String(expiryDateWithCheck.suffix(1))) {
    // print("fail expdate check: " + expiryDateWithCheck)
    return false
  }
  if (calcCheckDigit(String(optionalDataWithCheck.prefix(14))) != String(optionalDataWithCheck.suffix(1))) {
    // print("fail optdata check: " + optionalDataWithCheck)
    return false
  }
  return true
}

extension String {

	func checkMrz() -> (String)? {
        
    let tdThreeFirstRegex = "P.[A-Z0<]{3}([A-Z0]+<)+<([A-Z0]+<)+<+"
    let tdThreeSecondRegex = "[A-Z0-9]{1,9}<?[0-9O]{1}[A-Z0<]{3}[0-9]{7}(M|F|<)[0-9O]{7}[A-Z0-9<]+"
    let tdThreeMrzRegex = "P.[A-Z0<]{3}([A-Z0]+<)+<([A-Z0]+<)+<+\n[A-Z0-9]{1,9}<?[0-9O]{1}[A-Z0<]{3}[0-9]{7}(M|F|<)[0-9O]{7}[A-Z0-9<]+"

    let tdThreeFirstLine = self.range(of: tdThreeFirstRegex, options: .regularExpression, range: nil, locale: nil)
    let tdThreeSeconddLine = self.range(of: tdThreeSecondRegex, options: .regularExpression, range: nil, locale: nil)
    

    if(tdThreeFirstLine != nil){
      if(self.count == 44){
        captureFirst = self
      }
    }
    
    if(tdThreeSeconddLine != nil){
      if(self.count == 44){
        captureSecond = self
      }
    }
    
    if(captureFirst.count == 44 && captureSecond.count == 44){
      temp_mrz = (captureFirst.stripped + "\n" + captureSecond.stripped).replacingOccurrences(of: " ", with: "<")
      
      let checkMrz = temp_mrz.range(of: tdThreeMrzRegex, options: .regularExpression, range: nil, locale: nil)
      if(checkMrz != nil){
        mrz = temp_mrz
      }
    }

    if(mrz == ""){
      return nil
    }

    if (!validateMRZ(mrz)) {
      return nil
    }
      
		return mrz
	}
    
  var stripped: String {
    let okayChars = Set("ABCDEFGHIJKLKMNOPQRSTUVWXYZ1234567890<")
    return self.filter {okayChars.contains($0) }
  }
}

class MrzStringTracker {
	var frameIndex: Int64 = 0

	typealias StringObservation = (lastSeen: Int64, count: Int64)
	
	// Dictionary of seen strings. Used to get stable recognition before
	// displaying anything.
	var seenStrings = [String: StringObservation]()
	var bestCount = Int64(0)
	var bestString = ""

	func logFrame(strings: [String]) {
		for string in strings {
			if seenStrings[string] == nil {
				seenStrings[string] = (lastSeen: Int64(0), count: Int64(-1))
			}
			seenStrings[string]?.lastSeen = frameIndex
			seenStrings[string]?.count += 1
			// print("Seen \(string) \(seenStrings[string]?.count ?? 0) times")
		}
	
		var obsoleteStrings = [String]()

		// Go through strings and prune any that have not been seen in while.
		// Also find the (non-pruned) string with the greatest count.
		for (string, obs) in seenStrings {
			// Remove previously seen text after 30 frames (~1s).
			if obs.lastSeen < frameIndex - 30 {
				obsoleteStrings.append(string)
			}
			
			// Find the string with the greatest count.
			let count = obs.count
			if !obsoleteStrings.contains(string) && count > bestCount {
				bestCount = Int64(count)
				bestString = string
			}
		}
		// Remove old strings.
		for string in obsoleteStrings {
			seenStrings.removeValue(forKey: string)
		}
		
		frameIndex += 1
	}
	
	func getStableString() -> String? {
		// Require the recognizer to see the same string at least 10 times.
		if bestCount >= 10 {
			return bestString
		} else {
			return nil
		}
	}
	
	func reset(string: String) {
		seenStrings.removeValue(forKey: string)
		bestCount = 0
		bestString = ""
        captureFirst = ""
        captureSecond = ""
        captureThird = ""
        mrz = ""
        temp_mrz = ""
	}
}

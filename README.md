# MRZ Reader Camera for React Native

Reads MRZ field for React Native (Both iOS and Android)

IOS Version only supports TD3 format Passport MRZ (doesn't support id cards), and only supports back camera.
Android Version only reads document number, expiry date, birth date, and fills rest of the fields empty.

## Installation

1. **Install the Plugin**:
   ```sh
   npm install react-native-mrz-reader
   ```
2. **Link Native Modules (if required for versions below React Native 0.60)**:
   ```sh
   npx react-native link react-native-mrz-reader
   ```
3. **iOS Additional Setup**: Modify your Info.plist to include necessary Camera usage descriptions.
   ```xml
   <key>NSCameraUsageDescription</key>
   <string>This app requires Camera access to verify your identity.</string>
   ```
4. **Android Additional Setup**: Add Camera permissions in your AndroidManifest.xml.

   ```xml
   <uses-feature android:name="android.hardware.camera" android:required="false" />
   <uses-permission android:name="android.permission.CAMERA" />
   ```

   Add the following code in your app to request camera permission at runtime:

   ```ts
   import { Platform} from 'react-native';
   import * as Permissions from 'react-native-permissions';

   async function requestCameraPermission() {
     try {
       const granted = await Permissions.request(
         (() => {
           switch(Platform.OS) {
             case 'ios':
               return Permissions.PERMISSIONS.IOS.CAMERA;
             case 'android':
               return Permissions.PERMISSIONS.ANDROID.CAMERA;
             default:
               throw new Error(`Unsupported platform: ${Platform.OS}`);
           }
         })(),
         {
           title: 'Camera Permission',
           message: 'This app needs access to your camera to scan MRZ.',
           buttonNeutral: 'Ask Me Later',
           buttonNegative: 'Cancel',
           buttonPositive: 'OK',
         }
       );
       switch(granted) {
         case Permissions.RESULTS.GRANTED:
         case Permissions.RESULTS.LIMITED:
           console.log('You can use the camera');
           break;
         default:
           console.log('Camera permission denied');
           break;
       }
     } catch (err) {
       console.warn(err);
     }
   }
   ```

**Note**: This plugin is currently only available for Android devices.

## Usage

```tsx
import MrzReader, { CameraSelector, DocType } from 'react-native-mrz-reader';

// ...

<MrzReader
  style={{width: '100%', height: '100%'}}
  docType={DocType.Passport}
  cameraSelector={CameraType.Back}
  onMRZRead={(mrz: string) => {
    console.log(mrz)
  }}
/>
```

## Example

For a detailed example of how to use the NFC Passport Reader, please see the [Example App](example/src/App.tsx).

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)


## Additional

IOS Version is implemented via Vision API by [@corupta](https://github.com/corupta) following the base implementation from [girayk/MrzScanner](https://github.com/girayk/MrzScanner)

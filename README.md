# MRZ Reader Camera for React Native

Reads MRZ field for React Native

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
   import { PermissionsAndroid } from 'react-native';

   async function requestCameraPermission() {
     try {
       const granted = await PermissionsAndroid.request(
         PermissionsAndroid.PERMISSIONS.CAMERA,
         {
           title: 'Camera Permission',
           message: 'This app needs access to your camera to scan MRZ.',
           buttonNeutral: 'Ask Me Later',
           buttonNegative: 'Cancel',
           buttonPositive: 'OK',
         }
       );
       if (granted === PermissionsAndroid.RESULTS.GRANTED) {
         console.log('You can use the camera');
       } else {
         console.log('Camera permission denied');
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

import * as React from 'react';

import { StyleSheet, View, Platform } from 'react-native';
import MrzReaderView, {
  CameraSelector,
  DocType,
} from 'react-native-mrz-reader';
import * as Permissions from 'react-native-permissions';

export default function App() {
  const [isGranted, setIsGranted] = React.useState(false);

  React.useEffect(() => {
    Permissions.request(
      (() => {
        switch (Platform.OS) {
          case 'ios':
            return Permissions.PERMISSIONS.IOS.CAMERA;
          case 'android':
            return Permissions.PERMISSIONS.ANDROID.CAMERA;
          default:
            throw new Error(`Unsupported platform: ${Platform.OS}`);
        }
      })()
    ).then((granted) => {
      switch (granted) {
        case Permissions.RESULTS.GRANTED:
        case Permissions.RESULTS.LIMITED:
          setIsGranted(true);
          break;
        default:
          setIsGranted(false);
          break;
      }
    });
  }, []);

  return (
    <View style={styles.container}>
      {isGranted && (
        <MrzReaderView
          cameraSelector={CameraSelector.Back}
          docType={DocType.Passport}
          style={styles.box}
          onMRZRead={(mrz) => {
            console.log(mrz);
          }}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
  },
  box: {
    width: '100%',
    height: '100%',
  },
});

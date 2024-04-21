import * as React from 'react';

import { StyleSheet, View, PermissionsAndroid } from 'react-native';
import MrzReaderView, { CameraType, DocType } from 'react-native-mrz-reader';

export default function App() {
  const [isGranted, setIsGranted] = React.useState(false);

  React.useEffect(() => {
    PermissionsAndroid.request('android.permission.CAMERA').then((granted) => {
      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        setIsGranted(true);
      } else {
        setIsGranted(false);
      }
    });
  }, []);

  return (
    <View style={styles.container}>
      {isGranted && (
        <MrzReaderView
          cameraType={CameraType.Back}
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

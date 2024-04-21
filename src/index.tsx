import React, { useEffect } from 'react';
import {
  requireNativeComponent,
  UIManager,
  Platform,
  DeviceEventEmitter,
  type ViewStyle,
} from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-mrz-reader' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

export enum CameraType {
  Front = 'front',
  Back = 'back',
}

export enum DocType {
  ID = 'ID_CARD',
  Passport = 'PASSPORT',
}

export type MrzReaderProps = {
  onMRZRead: (mrz: string) => void;
  cameraType?: CameraType;
  docType: DocType;
  style: ViewStyle;
};
const ComponentName = 'MrzReaderView';

const MrzReaderView =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<MrzReaderProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };

const MrzReader = (props: MrzReaderProps) => {
  useEffect(() => {
    DeviceEventEmitter.addListener('onMRZRead', (event) => {
      props.onMRZRead(event);
    });

    return () => {
      DeviceEventEmitter.removeAllListeners('onMRZRead');
    };
  }, [props]);

  return <MrzReaderView {...props} />;
};

export default MrzReader;

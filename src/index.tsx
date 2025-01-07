import React, { useCallback, useEffect } from 'react';
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

export enum CameraSelector {
  Front = 'front',
  Back = 'back',
}

export enum DocType {
  ID = 'ID_CARD',
  Passport = 'PASSPORT',
}

export type MrzReaderProps = {
  onMRZRead: (mrz: string) => void;
  cameraSelector?: CameraSelector;
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
  const { onMRZRead, docType, cameraSelector } = props;
  const onMRZReadIOS = useCallback(
    (event: any) => {
      onMRZRead(event.nativeEvent.mrz);
    },
    [onMRZRead]
  );
  useEffect(() => {
    if (Platform.OS === 'ios') {
      if (docType !== DocType.Passport) {
        throw new Error(
          `Only passport document type is supported on iOS. Received docType: "${docType}"`
        );
      }
    }
  }, [docType]);
  useEffect(() => {
    if (Platform.OS === 'ios') {
      if (cameraSelector && cameraSelector !== CameraSelector.Back) {
        throw new Error(
          `Only back camera is supported on IOS. Received cameraSelector: "${cameraSelector}"`
        );
      }
    }
  }, [cameraSelector]);
  useEffect(() => {
    if (Platform.OS === 'android') {
      DeviceEventEmitter.addListener('onMRZRead', (event) => {
        onMRZRead(event);
      });
    }

    return () => {
      if (Platform.OS === 'android') {
        DeviceEventEmitter.removeAllListeners('onMRZRead');
      }
    };
  }, [onMRZRead]);

  return <MrzReaderView {...props} onMRZRead={onMRZReadIOS} />;
};

export default MrzReader;

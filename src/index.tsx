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

type MrzReaderProps = {
  onMRZRead: (mrz: string) => void;
  style: ViewStyle;
};
const ComponentName = 'MrzReaderView';

const MrzReaderViewBase =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<MrzReaderProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };

const MrzReaderView = (props: MrzReaderProps) => {
  useEffect(() => {
    DeviceEventEmitter.addListener('onMRZRead', (event) => {
      props.onMRZRead(event);
    });

    return () => {
      DeviceEventEmitter.removeAllListeners('onMRZRead');
    };
  }, [props]);

  return <MrzReaderViewBase {...props} />;
};

export default MrzReaderView;

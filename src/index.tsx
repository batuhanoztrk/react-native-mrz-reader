import {
  requireNativeComponent,
  UIManager,
  Platform,
  type ViewStyle,
} from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-mrz-reader' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

type MrzReaderProps = {
  color: string;
  style: ViewStyle;
};

const ComponentName = 'MrzReaderView';

export const MrzReaderView =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<MrzReaderProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };

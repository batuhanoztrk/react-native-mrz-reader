#import <React/RCTView.h>
@interface MrzReaderView : RCTView
@property (nonatomic, copy) RCTDirectEventBlock onMRZRead;
@end
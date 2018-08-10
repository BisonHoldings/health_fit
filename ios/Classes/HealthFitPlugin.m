#import "HealthFitPlugin.h"
#import <health_fit/health_fit-Swift.h>

@implementation HealthFitPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftHealthFitPlugin registerWithRegistrar:registrar];
}
@end

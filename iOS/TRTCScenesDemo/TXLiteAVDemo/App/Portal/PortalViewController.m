
#import "PortalViewController.h"
#import <TRTCCloudDef.h>

#import "AppDelegate.h"
#import "ColorMacro.h"
#import "MainMenuCell.h"
#import "TXLiteAVDemo-Swift.h"

#if DEBUG
#define SdkBusiId (18069)
#else
#define SdkBusiId (18070)
#endif


@interface PortalViewMenuLayout : UICollectionViewFlowLayout

@end

@implementation PortalViewMenuLayout

- (void)prepareLayout
{
    [super prepareLayout];
    
    CGFloat width                = [UIScreen mainScreen].bounds.size.width;
    
    self.sectionInset            = UIEdgeInsetsMake(0, width * 0.08, 0, width * 0.08);
    self.itemSize                = CGSizeMake(width * 0.36, width * 0.24);
    self.minimumLineSpacing      = width * 0.1;
    self.minimumInteritemSpacing = 10.0f;
}

@end

@interface PortalViewController ()<UICollectionViewDelegate, UICollectionViewDataSource,
UIPickerViewDataSource, UIPickerViewDelegate> {
    UIPickerView *  _logPickerView;
    UIView *        _logUploadView;
}

@property (nonatomic, weak) IBOutlet UICollectionView *collectionView;
@property (nonatomic, weak) IBOutlet UILabel *versionLabel;
@property (nonatomic, weak) IBOutlet UILabel *titleLabel;

@property (nonatomic, strong) NSArray<MainMenuItem *> *mainMenuItems;

@property (nonatomic, strong) NSMutableArray *logFilesArray;
@property (nonatomic, strong) TRTCVoiceRoom *voiceRoom;

@end

@implementation PortalViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor whiteColor];
    NSArray *colors = [NSArray arrayWithObjects:(__bridge id)[UIColor colorWithRed:19.0 / 255.0 green:41.0 / 255.0 blue:75.0 / 255.0 alpha:1].CGColor, (__bridge id)[UIColor colorWithRed:5.0 / 255.0 green:12.0 / 255.0 blue:23.0 / 255.0 alpha:1].CGColor, nil];
    CAGradientLayer *gradientLayer = [CAGradientLayer layer];
    gradientLayer.colors = colors;
    gradientLayer.startPoint = CGPointMake(0, 0);
    gradientLayer.endPoint = CGPointMake(1, 1);
    gradientLayer.frame = self.view.bounds;
    [self.view.layer insertSublayer:gradientLayer atIndex:0];
    self.voiceRoom = [TRTCVoiceRoom sharedInstance];
    __weak __typeof(self) wSelf = self;
    self.mainMenuItems = @[
        [[MainMenuItem alloc] initWithIcon:[UIImage imageNamed:@"MenuVoiceRoom"]
                                     title:@"voice chat room"
                                   content:@""
                                  onSelect:^{ [wSelf gotoVoiceRoomView]; }],
    ];
    NSString *version = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleShortVersionString"];
    self.versionLabel.text = [NSString stringWithFormat:@"TRTC v%@(%@)", [TRTCCloud getSDKVersion], version];
    
    UITapGestureRecognizer* tapGesture = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(handleTap:)];
    [self.titleLabel addGestureRecognizer:tapGesture];
    
    UILongPressGestureRecognizer* pressGesture = [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(handleLongPress:)]; //提取SDK日志暗号!
    pressGesture.minimumPressDuration = 2.0;
    pressGesture.numberOfTouchesRequired = 1;
    [self.titleLabel addGestureRecognizer:pressGesture];
    self.titleLabel.userInteractionEnabled = YES;
    [self setUpLogViews];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [self setupToast];
    [self.navigationController setNavigationBarHidden:YES];
    NSString *userID = [[ProfileManager shared] curUserID];
    NSString *userSig = [[ProfileManager shared] curUserSig];
    
    if (![[[V2TIMManager sharedInstance] getLoginUser] isEqual:userID]) {
        [[ProfileManager shared] IMLoginWithUserSig:userSig success:^{
            [self makeToastWithMessage:@"IM login success."];
        } failed:^(NSString * error) {
            [self makeToastWithMessage:@"IM login failed."];
        }];
    }
}

- (void)gotoVoiceRoomView {
    NSString *userID = [[ProfileManager shared] curUserID];
    NSString *userSig = [[ProfileManager shared] curUserSig];
    [self.voiceRoom login:SDKAPPID userId:userID userSig:userSig callback:^(int32_t code, NSString * _Nonnull message) {
        NSLog(@"login voiceroom success.");
    }];
    LoginResultModel *curUser = [[ProfileManager shared] curUserModel];
    [self.voiceRoom setSelfProfile:curUser.name avatarURL:curUser.avatar callback:^(int32_t code, NSString * _Nonnull message) {
        NSLog(@"voiceroom: set self profile success.");

    }];
    TRTCVoiceRoomEnteryControl* container = [[TRTCVoiceRoomEnteryControl alloc] initWithSdkAppId:SDKAPPID userId:userID];
    UIViewController* vc = [container makeEntranceViewController];
    [self.navigationController pushViewController:vc animated:YES];
}

- (IBAction)logout:(id)sender {
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"logout?" message:nil preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:@"Cancel" style:UIAlertActionStyleCancel handler:^(UIAlertAction * _Nonnull action) {
        
    }];
    UIAlertAction *okAction = [UIAlertAction actionWithTitle:@"Confirm" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [[ProfileManager shared] removeLoginCache];
        [[AppUtils shared] showLoginController];
        [[V2TIMManager sharedInstance] logout:^{
            
        } fail:^(int code, NSString *msg) {
            
        }];
    }];
    [alert addAction:cancelAction];
    [alert addAction:okAction];
    [self presentViewController:alert animated:YES completion:nil];
}

#pragma mark - 日志获取
- (void)setUpLogViews {
    _logUploadView = [[UIView alloc] initWithFrame:CGRectMake(0, self.view.bounds.size.height / 2, self.view.bounds.size.width, self.view.bounds.size.height / 2)];
    _logUploadView.backgroundColor = [UIColor whiteColor];
    _logUploadView.hidden = YES;
    UIButton* uploadButton = [UIButton buttonWithType:UIButtonTypeSystem];
    uploadButton.center = CGPointMake(self.view.bounds.size.width / 2, _logUploadView.frame.size.height * 0.9);
    uploadButton.bounds = CGRectMake(0, 0, self.view.bounds.size.width / 3, _logUploadView.frame.size.height * 0.2);
    [uploadButton setTitle:@"share log" forState:UIControlStateNormal];
    [uploadButton addTarget:self action:@selector(onSharedUploadLog:) forControlEvents:UIControlEventTouchUpInside];
    [_logUploadView addSubview:uploadButton];

    _logPickerView = [[UIPickerView alloc] initWithFrame:CGRectMake(0, 0, self.view.frame.size.width, _logUploadView.frame.size.height * 0.8)];
    _logPickerView.dataSource = self;
    _logPickerView.delegate = self;
    [_logUploadView addSubview:_logPickerView];
    [self.view addSubview:_logUploadView];
}

- (void)handleLongPress:(UILongPressGestureRecognizer *)pressRecognizer {
    if (pressRecognizer.state == UIGestureRecognizerStateBegan) {
        NSLog(@"long Press");
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *logDoc = [NSString stringWithFormat:@"%@%@", paths[0], @"/log"];
        NSFileManager *fileManager = [NSFileManager defaultManager];
        NSArray* fileArray = [fileManager contentsOfDirectoryAtPath:logDoc error:nil];
        fileArray = [fileArray sortedArrayUsingComparator:^NSComparisonResult(id  _Nonnull obj1, id  _Nonnull obj2) {
            NSString* file1 = (NSString*)obj1;
            NSString* file2 = (NSString*)obj2;
            return [file1 compare:file2] == NSOrderedDescending;
        }];
        self.logFilesArray = [NSMutableArray new];
        for (NSString* logName in fileArray) {
            if ([logName hasSuffix:@"xlog"]) {
                [self.logFilesArray addObject:logName];
            }
        }
        
        _logUploadView.alpha = 0.1;
        UIView *logUploadView = _logUploadView;
        [UIView animateWithDuration:0.5 animations:^{
            logUploadView.hidden = NO;
            logUploadView.alpha = 1;
        }];
        [_logPickerView reloadAllComponents];
    }
}

- (IBAction)onSharedUploadLog:(UIButton *)sender {
    NSInteger row = [_logPickerView selectedRowInComponent:0];
    if (row < self.logFilesArray.count) {
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *logDoc = [NSString stringWithFormat:@"%@%@", paths[0], @"/log"];
        NSString* logPath = [logDoc stringByAppendingPathComponent:self.logFilesArray[row]];
        NSURL *shareobj = [NSURL fileURLWithPath:logPath];
        UIActivityViewController *activityView = [[UIActivityViewController alloc] initWithActivityItems:@[shareobj] applicationActivities:nil];
        UIView *logUploadView = _logUploadView;
        [self presentViewController:activityView animated:YES completion:^{
            logUploadView.hidden = YES;
        }];
    }
}

- (void)handleTap:(UITapGestureRecognizer*)tapRecognizer {
    if (!_logUploadView.hidden) {
        _logUploadView.hidden = YES;
    }
}

#pragma mark - UIPickerViewDatasource & UIPickerViewDelegate
- (NSInteger)numberOfComponentsInPickerView:(UIPickerView *)pickerView {
    return 1;
}

- (NSInteger)pickerView:(UIPickerView *)pickerView numberOfRowsInComponent:(NSInteger)component {
    return self.logFilesArray.count;
}

- (NSString*)pickerView:(UIPickerView *)pickerView titleForRow:(NSInteger)row forComponent:(NSInteger)component {
    if (row < self.logFilesArray.count) {
        return (NSString*)self.logFilesArray[row];
    }
    return nil;
}


#pragma mark - UICollectionViewDelegate

- (void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath *)indexPath
{
    self.mainMenuItems[indexPath.row].selectBlock();
}

#pragma mark - UICollectionViewDataSource

- (NSInteger)collectionView:(UICollectionView *)collectionView numberOfItemsInSection:(NSInteger)section
{
    return self.mainMenuItems.count;
}

- (UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath
{
    MainMenuCell *cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"MainMenuCell" forIndexPath:indexPath];
    cell.item = self.mainMenuItems[indexPath.row];
    return cell;
}

- (NSInteger)numberOfSectionsInCollectionView:(UICollectionView *)collectionView
{
    return 1;
}

@end

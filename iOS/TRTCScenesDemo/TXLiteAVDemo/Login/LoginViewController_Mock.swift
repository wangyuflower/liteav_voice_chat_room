//
//  LoginViewController.swift
//  trtcScenesDemo
//
//  Created by xcoderliu on 12/16/19.
//  Copyright © 2019 xcoderliu. All rights reserved.
//

import UIKit
import RxSwift
import NVActivityIndicatorView
import Toast_Swift

class LoginViewController: UIViewController {
    let disposeBag = DisposeBag()
    let loading = NVActivityIndicatorView(frame: CGRect(x: 0, y: 0, width: 100, height: 60),
                                          type: .ballBeat,
                                          color: .appTint)
    
    /// verify code countdown time
    @objc dynamic var verifyTime: UInt32 = 0
    
    
    /// trtc title
    let trtcTitle: UILabel = {
        let title = UILabel()
        title.textColor = .appTint
        title.font = UIFont.boldSystemFont(ofSize: 30)
        title.text = "Tencent Cloud TRTC"
        title.textAlignment = .center
        return title
    }()
    
    /// 手机号
    let phoneTip: UILabel = {
        let tip = UILabel()
        tip.text = "UserID"
        tip.textColor = .white
        tip.font = UIFont.systemFont(ofSize: 16)
        return tip
    }()
    
    /// Login Button
    lazy var loginButton: UIButton = {
        let sign = UIButton()
        sign.backgroundColor = .appTint
        sign.setTitle("Login", for: .normal)
        sign.setTitleColor(.white, for: .normal)
        sign.layer.cornerRadius = 4
        return sign
    }()
    
    let versionTip: UILabel = {
        let tip = UILabel()
        tip.textAlignment = .center
        tip.font = UIFont.systemFont(ofSize: 14)
        tip.textColor = UIColor(hex: "525252")
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString")
        let sdvVersionStr = TXLiveBase.getSDKVersionStr()
        tip.text = "Tencent Cloud TRTC v\(version ?? "0")(\(sdvVersionStr ?? "0"))"
        tip.adjustsFontSizeToFitWidth = true
        return tip
    }()
    
    let bottomTip: UILabel = {
        let tip = UILabel()
        tip.textAlignment = .center
        tip.font = UIFont.systemFont(ofSize: 14)
        tip.textColor = UIColor(hex: "525252")
        tip.text = ""
        tip.adjustsFontSizeToFitWidth = true
        return tip
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        setupUI()
    }
    
    deinit {
        debugPrint("deinit \(self)")
    }
    

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

}

package com.tencent.liteav.trtcvoiceroom.ui.room;

import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.Group;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ToastUtils;
import com.squareup.picasso.Picasso;
import com.tencent.liteav.trtcvoiceroom.R;
import com.tencent.liteav.trtcvoiceroom.model.TRTCVoiceRoom;
import com.tencent.liteav.trtcvoiceroom.model.TRTCVoiceRoomCallback;
import com.tencent.liteav.trtcvoiceroom.model.TRTCVoiceRoomDef;
import com.tencent.liteav.trtcvoiceroom.model.TRTCVoiceRoomDelegate;
import com.tencent.liteav.trtcvoiceroom.ui.base.VoiceRoomSeatEntity;
import com.tencent.liteav.trtcvoiceroom.ui.widget.ConfirmDialogFragment;
import com.tencent.liteav.trtcvoiceroom.ui.widget.InputTextMsgDialog;
import com.tencent.liteav.trtcvoiceroom.ui.widget.SelectMemberView;
import com.tencent.liteav.trtcvoiceroom.ui.widget.msg.MsgEntity;
import com.tencent.liteav.trtcvoiceroom.ui.widget.msg.MsgListAdapter;
import com.tencent.trtc.TRTCCloudDef;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.tencent.liteav.trtcvoiceroom.model.TRTCVoiceRoomDef.SeatInfo.STATUS_CLOSE;
import static com.tencent.liteav.trtcvoiceroom.model.TRTCVoiceRoomDef.SeatInfo.STATUS_UNUSED;
import static com.tencent.liteav.trtcvoiceroom.model.TRTCVoiceRoomDef.SeatInfo.STATUS_USED;

public class VoiceRoomBaseActivity extends AppCompatActivity implements VoiceRoomSeatAdapter.OnItemClickListener, TRTCVoiceRoomDelegate, InputTextMsgDialog.OnTextSendListener, MsgListAdapter.OnItemClickListener {
    protected static final String TAG = VoiceRoomBaseActivity.class.getName();

    protected static final int    MAX_SEAT_SIZE           = 7;
    protected static final String VOICEROOM_ROOM_ID       = "room_id";
    protected static final String VOICEROOM_ROOM_NAME     = "room_name";
    protected static final String VOICEROOM_USER_NAME     = "user_name";
    protected static final String VOICEROOM_USER_ID       = "user_id";
    protected static final String VOICEROOM_USER_SIG      = "user_sig";
    protected static final String VOICEROOM_NEED_REQUEST  = "need_request";
    protected static final String VOICEROOM_SEAT_COUNT    = "seat_count";
    protected static final String VOICEROOM_AUDIO_QUALITY = "audio_quality";
    protected static final String VOICEROOM_USER_AVATAR   = "user_avatar";
    protected static final String VOICEROOM_ROOM_COVER    = "room_cover";

    protected String        mSelfUserId;     //进房用户ID
    protected int           mCurrentRole;    //用户当前角色
    protected Set<String>   mSeatUserSet; //在座位上的主播集合
    protected TRTCVoiceRoom mTRTCVoiceRoom;

    protected List<VoiceRoomSeatEntity> mVoiceRoomSeatEntityList;
    protected VoiceRoomSeatAdapter      mVoiceRoomSeatAdapter;
    protected Toolbar                   mToolbar;
    protected TextView                  mToolbarTitle;
    protected Group                     mGroupBottomTool;
    protected CircleImageView           mImgHead;
    protected TextView                  mTvName;
    protected RecyclerView              mRvSeat;
    protected RecyclerView              mRvImMsg;
    protected View                      mToolBarView;
    protected AppCompatImageButton      mBtnMsg;
    protected AppCompatImageButton      mBtnMic;
    protected AppCompatImageButton      mBtnAudio;
    protected SelectMemberView          mViewSelectMember;
    protected InputTextMsgDialog        mInputTextMsgDialog;
    protected int                       mRoomId;
    protected String                    mRoomName;
    protected String                    mUserName;
    protected String                    mUserAvatar;
    protected String                    mRoomCover;
    protected String                    mMainSeatUserId;
    protected boolean                   mNeedRequest;
    protected int                       mAudioQuality;
    protected List<MsgEntity>           mMsgEntityList;
    protected MsgListAdapter            mMsgListAdapter;
    protected ConfirmDialogFragment     mConfirmDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 应用运行时，保持不锁屏、全屏化
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.trtcvoiceroom_activity_main);
        initView();
        initData();
        initListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void initListener() {
        mBtnMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkButtonPermission()) {
                    boolean currentMode = !mBtnMic.isSelected();
                    mBtnMic.setSelected(currentMode);
                    if (currentMode) {
                        mTRTCVoiceRoom.startMicrophone();
                        ToastUtils.showLong(R.string.trtcvoiceroom_enable_mic);
                    } else {
                        mTRTCVoiceRoom.stopMicrophone();
                        ToastUtils.showLong(R.string.trtcvoiceroom_disable_mic);
                    }
                }
            }
        });
        mBtnAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean currentMode = !mBtnAudio.isSelected();
                mBtnAudio.setSelected(currentMode);
                mTRTCVoiceRoom.muteAllRemoteAudio(!currentMode);
                if (currentMode) {
                    ToastUtils.showLong(R.string.trtcvoiceroom_unmuted);
                } else {
                    ToastUtils.showLong(R.string.trtcvoiceroom_muted);
                }
            }
        });
        mBtnMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputMsgDialog();
            }
        });
    }

    /**
     * 判断是否为主播，有操作按钮的权限
     *
     * @return 是否有权限
     */
    protected boolean checkButtonPermission() {
        boolean hasPermission = (mCurrentRole == TRTCCloudDef.TRTCRoleAnchor);
        if (!hasPermission) {
            ToastUtils.showLong(R.string.trtcvoiceroom_host_only);
        }
        return hasPermission;
    }

    protected void initData() {
        Intent intent = getIntent();
        mRoomId = intent.getIntExtra(VOICEROOM_ROOM_ID, 0);
        mRoomName = intent.getStringExtra(VOICEROOM_ROOM_NAME);
        mUserName = intent.getStringExtra(VOICEROOM_USER_NAME);
        mSelfUserId = intent.getStringExtra(VOICEROOM_USER_ID);
        mNeedRequest = intent.getBooleanExtra(VOICEROOM_NEED_REQUEST, false);
        mUserAvatar = intent.getStringExtra(VOICEROOM_USER_AVATAR);
        mRoomCover = intent.getStringExtra(VOICEROOM_ROOM_COVER);
        mAudioQuality = intent.getIntExtra(VOICEROOM_AUDIO_QUALITY, TRTCCloudDef.TRTC_AUDIO_QUALITY_DEFAULT);
        //        mSeatCount = intent.getIntExtra(VOICEROOM_SEAT_COUNT);
        mTRTCVoiceRoom = TRTCVoiceRoom.sharedInstance(this);
        mTRTCVoiceRoom.setDelegate(this);
    }

    protected void initView() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbarTitle = (TextView) findViewById(R.id.toolbar_title);
        mGroupBottomTool = (Group) findViewById(R.id.group_bottom_tool);
        mImgHead = (CircleImageView) findViewById(R.id.img_head);
        mTvName = (TextView) findViewById(R.id.tv_name);
        mRvSeat = (RecyclerView) findViewById(R.id.rv_seat);
        mRvImMsg = (RecyclerView) findViewById(R.id.rv_im_msg);
        mToolBarView = findViewById(R.id.tool_bar_view);
        mBtnMsg = (AppCompatImageButton) findViewById(R.id.btn_msg);
        mBtnMic = (AppCompatImageButton) findViewById(R.id.btn_mic);
        mBtnAudio = (AppCompatImageButton) findViewById(R.id.btn_audio);
        mViewSelectMember = new SelectMemberView(this);
        mConfirmDialogFragment = new ConfirmDialogFragment();
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        mInputTextMsgDialog = new InputTextMsgDialog(this, R.style.TRTCVoiceRoomInputDialog);
        mInputTextMsgDialog.setmOnTextSendListener(this);
        mMsgEntityList = new ArrayList<>();
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mMsgListAdapter = new MsgListAdapter(this, mMsgEntityList, this);
        mRvImMsg.setLayoutManager(new LinearLayoutManager(this));
        mRvImMsg.setAdapter(mMsgListAdapter);

        mVoiceRoomSeatEntityList = new ArrayList<>();
        for (int i = 0; i < MAX_SEAT_SIZE - 1; i++) {
            mVoiceRoomSeatEntityList.add(new VoiceRoomSeatEntity());
        }
        mVoiceRoomSeatAdapter = new VoiceRoomSeatAdapter(this, mVoiceRoomSeatEntityList, this);
        mRvSeat.setLayoutManager(gridLayoutManager);
        mRvSeat.setAdapter(mVoiceRoomSeatAdapter);
    }

    /**
     *     /////////////////////////////////////////////////////////////////////////////////
     *     //
     *     //                      send text massage
     *     //
     *     /////////////////////////////////////////////////////////////////////////////////
     */
    /**
     * 发消息弹出框
     */
    private void showInputMsgDialog() {
        WindowManager              windowManager = getWindowManager();
        Display                    display       = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams lp            = mInputTextMsgDialog.getWindow().getAttributes();
        lp.width = display.getWidth(); //设置宽度
        mInputTextMsgDialog.getWindow().setAttributes(lp);
        mInputTextMsgDialog.setCancelable(true);
        mInputTextMsgDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mInputTextMsgDialog.show();
    }

    @Override
    public void onTextSend(String msg) {
        if (msg.length() == 0) {
            return;
        }
        byte[] byte_num = msg.getBytes(StandardCharsets.UTF_8);
        if (byte_num.length > 160) {
            Toast.makeText(this, R.string.trtcvoiceroom_input_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        //消息回显
        MsgEntity entity = new MsgEntity();
        entity.userName = getString(R.string.trtcvoiceroom_me);
        entity.content = msg;
        entity.userId = mSelfUserId;
        entity.type = MsgEntity.TYPE_NORMAL;
        showImMsg(entity);

        mTRTCVoiceRoom.sendRoomTextMsg(msg, new TRTCVoiceRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
                if (code == 0) {
                    ToastUtils.showShort(R.string.trtcvoiceroom_send_success);
                } else {
                    ToastUtils.showShort(getString(R.string.trtcvoiceroom_send_failed) + msg);
                }
            }
        });
    }

    private void showImMsg(final MsgEntity entity) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMsgEntityList.size() > 1000) {
                    while (mMsgEntityList.size() > 900) {
                        mMsgEntityList.remove(0);
                    }
                }
                mMsgEntityList.add(entity);
                mMsgListAdapter.notifyDataSetChanged();
                mRvImMsg.smoothScrollToPosition(mMsgListAdapter.getItemCount());
            }
        });
    }

    public void resetSeatView() {
        mSeatUserSet.clear();
        for (VoiceRoomSeatEntity entity : mVoiceRoomSeatEntityList) {
            entity.isUsed = false;
        }
        mVoiceRoomSeatAdapter.notifyDataSetChanged();
    }

    /**
     * 座位上点击按钮的反馈
     *
     * @param position
     */
    @Override
    public void onItemClick(int position) {
    }

    @Override
    public void onError(int code, String message) {

    }

    @Override
    public void onWarning(int code, String message) {

    }

    @Override
    public void onDebugLog(String message) {

    }

    @Override
    public void onRoomDestroy(String roomId) {

    }

    @Override
    public void onRoomInfoChange(TRTCVoiceRoomDef.RoomInfo roomInfo) {
        mNeedRequest = roomInfo.needRequest;
        mRoomName = roomInfo.roomName;
        mToolbarTitle.setText(getString(R.string.trtcvoiceroom_main_title, roomInfo.roomName, roomInfo.roomId));
    }

    @Override
    public void onSeatListChange(final List<TRTCVoiceRoomDef.SeatInfo> seatInfoList) {
        //先刷一遍界面
        final List<String> userids = new ArrayList<>();
        for (int i = 0; i < seatInfoList.size(); i++) {
            TRTCVoiceRoomDef.SeatInfo newSeatInfo = seatInfoList.get(i);
            // 底层返回的第一个座位是主播哦！特殊处理一下
            if (i == 0) {
                if (mMainSeatUserId == null || !mMainSeatUserId.equals(newSeatInfo.userId)) {
                    //主播上线啦
                    mMainSeatUserId = newSeatInfo.userId;
                    userids.add(newSeatInfo.userId);
                    mTvName.setText(R.string.trtcvoiceroom_fetching_host_info);
                }
                continue;
            }
            // 接下来是座位区域的列表
            VoiceRoomSeatEntity oldSeatEntity = mVoiceRoomSeatEntityList.get(i - 1);
            if (newSeatInfo.userId != null && !newSeatInfo.userId.equals(oldSeatEntity.userId)) {
                //userId相同，可以不用重新获取信息了
                //但是如果有新的userId进来，那么应该去拿一下主播的详细信息
                userids.add(newSeatInfo.userId);
            }
            oldSeatEntity.userId = newSeatInfo.userId;
            // 座位的状态更新一下
            switch (newSeatInfo.status) {
                case STATUS_UNUSED:
                    oldSeatEntity.isUsed = false;
                    oldSeatEntity.isClose = false;
                    break;
                case STATUS_CLOSE:
                    oldSeatEntity.isUsed = false;
                    oldSeatEntity.isClose = true;
                    break;
                case STATUS_USED:
                    oldSeatEntity.isUsed = true;
                    oldSeatEntity.isClose = false;
                    break;
                default:
                    break;
            }
            oldSeatEntity.isMute = newSeatInfo.mute;
        }
        mVoiceRoomSeatAdapter.notifyDataSetChanged();

        //所有的userId拿到手，开始去搜索详细信息了
        mTRTCVoiceRoom.getUserInfoList(userids, new TRTCVoiceRoomCallback.UserListCallback() {
            @Override
            public void onCallback(int code, String msg, List<TRTCVoiceRoomDef.UserInfo> list) {
                // 解析所有人的userinfo
                Map<String, TRTCVoiceRoomDef.UserInfo> map = new HashMap<>();
                for (TRTCVoiceRoomDef.UserInfo userInfo : list) {
                    map.put(userInfo.userId, userInfo);
                }
                for (int i = 0; i < seatInfoList.size(); i++) {
                    TRTCVoiceRoomDef.SeatInfo newSeatInfo = seatInfoList.get(i);
                    TRTCVoiceRoomDef.UserInfo userInfo    = map.get(newSeatInfo.userId);
                    if (userInfo == null) {
                        continue;
                    }
                    // 底层返回的第一个座位是房主哦！特殊处理一下
                    if (i == 0) {
                        if (newSeatInfo.status == STATUS_USED) {
                            //主播上线啦
                            if (!TextUtils.isEmpty(userInfo.userAvatar)) {
                                Picasso.get().load(userInfo.userAvatar).into(mImgHead);
                            } else {
                                mImgHead.setImageResource(R.drawable.trtcvoiceroom_ic_head);
                            }
                            if (TextUtils.isEmpty(userInfo.userName)) {
                                mTvName.setText(userInfo.userId);
                            } else {
                                mTvName.setText(userInfo.userName);
                            }
                        } else {
                            mTvName.setText(R.string.trtcvoiceroom_host_offline);
                        }
                    } else {
                        // 接下来是座位区域的列表
                        VoiceRoomSeatEntity seatEntity = mVoiceRoomSeatEntityList.get(i - 1);
                        if (userInfo.userId.equals(seatEntity.userId)) {
                            seatEntity.userName = userInfo.userName;
                            seatEntity.userAvatar = userInfo.userAvatar;
                        }
                    }
                }
                mVoiceRoomSeatAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onAnchorEnterSeat(int index, TRTCVoiceRoomDef.UserInfo user) {
        if (index != 0) {
            // 房主上麦就别提醒了
            showNotifyMsg(getString(R.string.trtcvoiceroom_anchor_enter_seat, user.userName, index));
        }
    }

    @Override
    public void onAnchorLeaveSeat(int index, TRTCVoiceRoomDef.UserInfo user) {
        if (index != 0) {
            // 房主上麦就别提醒了
            showNotifyMsg(getString(R.string.trtcvoiceroom_anchor_leave_seat, user.userName, index));
        }
    }

    @Override
    public void onSeatMute(int index, boolean isMute) {
        if (isMute) {
            showNotifyMsg(getString(R.string.trtcvoiceroom_mute_seat, index));
        } else {
            showNotifyMsg(getString(R.string.trtcvoiceroom_unmute_seat, index));
        }
    }

    @Override
    public void onSeatClose(int index, boolean isClose) {
        String lockSeatStr = getString(R.string.trtcvoiceroom_anchor_close_seat, index);
        String unlockSeatStr = getString(R.string.trtcvoiceroom_anchor_open_seat, index);
        showNotifyMsg(isClose ? lockSeatStr : unlockSeatStr);
    }

    @Override
    public void onAudienceEnter(TRTCVoiceRoomDef.UserInfo userInfo) {
        showNotifyMsg(getString(R.string.trtcvoiceroom_enter_room_hint, userInfo.userName));
    }

    @Override
    public void onAudienceExit(TRTCVoiceRoomDef.UserInfo userInfo) {
        showNotifyMsg(getString(R.string.trtcvoiceroom_anchor_exit_room, userInfo.userName));
    }

    @Override
    public void onUserVolumeUpdate(ArrayList<TRTCCloudDef.TRTCVolumeInfo> userVolumes, int totalVolume) {
        Map<String, Integer> volumeMap = new HashMap<>();
        for (TRTCCloudDef.TRTCVolumeInfo info : userVolumes) {
            if (info.userId != null) {
                volumeMap.put(info.userId, info.volume);
            }
        }
        for (VoiceRoomSeatEntity entity : mVoiceRoomSeatEntityList) {
            if (entity.isUsed && volumeMap.get(entity.userId) != null) {
                int volume = volumeMap.get(entity.userId);
                if (volume > 20) {
                    entity.isTalk = true;
                } else {
                    entity.isTalk = false;
                }
            } else {
                entity.isTalk = false;
            }
        }
        mVoiceRoomSeatAdapter.notifyDataSetChanged();
        // main seat anchor
        Integer mainSeatVol = volumeMap.get(mMainSeatUserId);
        if (mainSeatVol != null && mainSeatVol > 20) {
            mImgHead.setBorderColor(getResources().getColor(R.color.trtcvoiceroom_color_head_talk));
        } else {
            mImgHead.setBorderColor(getResources().getColor(R.color.trtcvoiceroom_color_head_not_talk));
        }
    }

    @Override
    public void onRecvRoomTextMsg(String message, TRTCVoiceRoomDef.UserInfo userInfo) {
        MsgEntity msgEntity = new MsgEntity();
        msgEntity.userId = userInfo.userId;
        msgEntity.userName = userInfo.userName;
        msgEntity.content = message;
        msgEntity.type = MsgEntity.TYPE_NORMAL;
        showImMsg(msgEntity);
    }

    @Override
    public void onRecvRoomCustomMsg(String cmd, String message, TRTCVoiceRoomDef.UserInfo userInfo) {

    }

    @Override
    public void onReceiveNewInvitation(String id, String inviter, String cmd, String content) {

    }

    @Override
    public void onInviteeAccepted(String id, String invitee) {

    }

    @Override
    public void onInviteeRejected(String id, String invitee) {

    }

    @Override
    public void onInvitationCancelled(String id, String invitee) {

    }

    @Override
    public void onAgreeClick(int position) {

    }

    protected int changeSeatIndexToModelIndex(int srcSeatIndex) {
        return srcSeatIndex + 1;
    }

    protected void showNotifyMsg(String msg) {
        MsgEntity msgEntity = new MsgEntity();
        msgEntity.type = MsgEntity.TYPE_NORMAL;
        msgEntity.content = msg;
        mMsgEntityList.add(msgEntity);
        mMsgListAdapter.notifyDataSetChanged();
        mRvImMsg.smoothScrollToPosition(mMsgListAdapter.getItemCount());
    }
}
package com.tencent.liteav.trtcvoiceroom.ui.room;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.blankj.utilcode.util.ToastUtils;
import com.tencent.liteav.trtcvoiceroom.R;
import com.tencent.liteav.trtcvoiceroom.model.TRTCVoiceRoomCallback;
import com.tencent.liteav.trtcvoiceroom.model.TRTCVoiceRoomDef;
import com.tencent.liteav.trtcvoiceroom.ui.base.VoiceRoomSeatEntity;
import com.tencent.liteav.trtcvoiceroom.ui.list.TCConstants;
import com.tencent.liteav.trtcvoiceroom.ui.widget.CommonBottomDialog;
import com.tencent.liteav.trtcvoiceroom.ui.widget.ConfirmDialogFragment;
import com.tencent.trtc.TRTCCloudDef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 观众界面
 *
 * @author guanyifeng
 */
public class VoiceRoomAudienceActivity extends VoiceRoomBaseActivity {
    private Map<String, Integer> mInvitationSeatMap;
    private String               mOwnerId;
    private boolean              mIsSeatInitSuccess;
    private int                  mSelfSeatIndex;

    public static void enterRoom(Context context, int roomId, String userId, int audioQuality) {
        Intent starter = new Intent(context, VoiceRoomAudienceActivity.class);
        starter.putExtra(VOICEROOM_ROOM_ID, roomId);
        starter.putExtra(VOICEROOM_USER_ID, userId);
        starter.putExtra(VOICEROOM_AUDIO_QUALITY, audioQuality);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAudience();
    }

    private void initAudience() {
        mInvitationSeatMap = new HashMap<>();
        mVoiceRoomSeatAdapter.setEmptyText(getString(R.string.trtcvoiceroom_hands_up));
        mVoiceRoomSeatAdapter.notifyDataSetChanged();
        // 开始进房哦
        enterRoom();
        mBtnMsg.setActivated(true);
        mBtnMsg.setSelected(true);
        mBtnAudio.setActivated(true);
        mBtnAudio.setSelected(true);

        refreshView();
    }

    private void refreshView() {
        if (mCurrentRole == TRTCCloudDef.TRTCRoleAnchor) {
            mBtnMic.setActivated(true);
            mBtnEffect.setActivated(true);
            mBtnMic.setSelected(true);
            mBtnEffect.setSelected(true);
        } else {
            mBtnEffect.setActivated(false);
            mBtnMic.setActivated(false);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exitRoom();
    }

    private void exitRoom() {
        mTRTCVoiceRoom.exitRoom(new TRTCVoiceRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void enterRoom() {
        mIsSeatInitSuccess = false;
        mSelfSeatIndex = -1;
        mCurrentRole = TRTCCloudDef.TRTCRoleAudience;
        mTRTCVoiceRoom.enterRoom(mRoomId, new TRTCVoiceRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
                if (code == 0) {
                    ToastUtils.showShort(R.string.trtcvoiceroom_enter_room_success);
                    mTRTCVoiceRoom.setAudioQuality(mAudioQuality);
                } else {
                    ToastUtils.showShort(R.string.trtcvoiceroom_enter_room_failed, code, msg);
                    finish();
                }
            }
        });
    }

    @Override
    public void onSeatListChange(List<TRTCVoiceRoomDef.SeatInfo> seatInfoList) {
        super.onSeatListChange(seatInfoList);
        mIsSeatInitSuccess = true;
    }

    /**
     * 点击麦位列表观众端的操作
     *
     * @param itemPos
     */
    @Override
    public void onItemClick(final int itemPos) {
        if (!mIsSeatInitSuccess) {
            ToastUtils.showLong(R.string.trtcvoiceroom_still_loading);
            return;
        }
        // 判断座位有没有人
        VoiceRoomSeatEntity entity = mVoiceRoomSeatEntityList.get(itemPos);
        if (entity.isUsed) {
            if (mSelfUserId.equals(entity.userId)) {
                // 点击自己的头像，弹出下麦的对话框
                final CommonBottomDialog dialog = new CommonBottomDialog(this);
                dialog.setButton(new CommonBottomDialog.OnButtonClickListener() {
                    @Override
                    public void onClick(int position, String text) {
                        if (position == 0) {
                            leaveSeat();
                        }
                        dialog.dismiss();
                    }
                }, getString(R.string.trtcvoiceroom_leave_seat_hint));
                dialog.show();
            } else {
                // 点击其他人可以toast展示一下
                ToastUtils.showShort(entity.userName);
            }
            Log.d(TAG, "onItemClick: " + entity.userId);
        } else if (!entity.isClose) {
            // 没人弹出申请上麦
            final CommonBottomDialog dialog = new CommonBottomDialog(this);
            dialog.setButton(new CommonBottomDialog.OnButtonClickListener() {
                @Override
                public void onClick(int position, String text) {
                    if (position == 0) {
                        // 发送请求之前再次判断一下这个座位有没有人
                        VoiceRoomSeatEntity seatEntity = mVoiceRoomSeatEntityList.get(itemPos);
                        if (seatEntity.isUsed) {
                            ToastUtils.showShort(R.string.trtcvoiceroom_seat_used);
                            return;
                        }
                        startTakeSeat(itemPos);
                    }
                    dialog.dismiss();
                }
            }, getString(R.string.trtcvoiceroom_request_seat_hint));
            dialog.show();
        } else {
            ToastUtils.showShort(R.string.trtcvoiceroom_seat_locked);
        }
    }

    private void leaveSeat() {
        mTRTCVoiceRoom.leaveSeat(new TRTCVoiceRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
                if (code == 0) {
                    ToastUtils.showShort(R.string.trtcvoiceroom_leave_success);
                } else {
                    ToastUtils.showShort(code + "--" + msg);
                }
            }
        });
    }

    private void startTakeSeat(int itemPos) {
        if (mCurrentRole == TRTCCloudDef.TRTCRoleAnchor) {
            ToastUtils.showShort(R.string.trtcvoiceroom_ur_host);
            return;
        }
        if (mNeedRequest) {
            //需要申请上麦
            if (mOwnerId == null) {
                ToastUtils.showShort(R.string.trtcvoiceroom_still_loading);
                return;
            }
            String inviteId = mTRTCVoiceRoom.sendInvitation(TCConstants.CMD_REQUEST_TAKE_SEAT, mOwnerId, String.valueOf(changeSeatIndexToModelIndex(itemPos)), new TRTCVoiceRoomCallback.ActionCallback() {
                @Override
                public void onCallback(int code, String msg) {
                }
            });
            mInvitationSeatMap.put(inviteId, itemPos);
        } else {
            //不需要的情况下自动上麦
            mTRTCVoiceRoom.enterSeat(changeSeatIndexToModelIndex(itemPos), new TRTCVoiceRoomCallback.ActionCallback() {
                @Override
                public void onCallback(int code, String msg) {
                    if (code == 0) {

                    }
                }
            });
        }
    }

    private void recvPickSeat(final String id, String cmd, final String content) {
        //这里收到了主播抱麦的邀请
        if (mConfirmDialogFragment != null && mConfirmDialogFragment.isAdded()) {
            mConfirmDialogFragment.dismiss();
        }
        mConfirmDialogFragment = new ConfirmDialogFragment();
        int seatIndex = Integer.parseInt(content);
        mConfirmDialogFragment.setMessage(getString(R.string.trtcvoiceroom_host_invite, seatIndex));
        mConfirmDialogFragment.setNegativeClickListener(new ConfirmDialogFragment.NegativeClickListener() {
            @Override
            public void onClick() {
                mTRTCVoiceRoom.rejectInvitation(id, new TRTCVoiceRoomCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        Log.d(TAG, "rejectInvitation callback:" + code);
                        ToastUtils.showShort(R.string.trtcvoiceroom_reject_reqeust);
                    }
                });
                mConfirmDialogFragment.dismiss();
            }
        });
        mConfirmDialogFragment.setPositiveClickListener(new ConfirmDialogFragment.PositiveClickListener() {
            @Override
            public void onClick() {
                //同意上麦，回复接受
                mTRTCVoiceRoom.acceptInvitation(id, new TRTCVoiceRoomCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        if (code != 0) {
                            ToastUtils.showShort(R.string.trtcvoiceroom_accept_failed, code);
                        }
                        Log.d(TAG, "acceptInvitation callback:" + code);
                    }
                });
                mConfirmDialogFragment.dismiss();
            }
        });
        mConfirmDialogFragment.show(getFragmentManager(), "confirm_fragment" + seatIndex);
    }

    @Override
    public void onRoomInfoChange(TRTCVoiceRoomDef.RoomInfo roomInfo) {
        super.onRoomInfoChange(roomInfo);
        mOwnerId = roomInfo.ownerId;
    }

    @Override
    public void onReceiveNewInvitation(final String id, String inviter, String cmd, final String content) {
        super.onReceiveNewInvitation(id, inviter, cmd, content);
        if (cmd.equals(TCConstants.CMD_PICK_UP_SEAT)) {
            recvPickSeat(id, cmd, content);
        }
    }

    @Override
    public void onInviteeAccepted(String id, String invitee) {
        super.onInviteeAccepted(id, invitee);
        Integer seatIndex = mInvitationSeatMap.remove(id);
        if (seatIndex != null) {
            VoiceRoomSeatEntity entity = mVoiceRoomSeatEntityList.get(seatIndex);
            if (!entity.isUsed) {
                mTRTCVoiceRoom.enterSeat(changeSeatIndexToModelIndex(seatIndex), new TRTCVoiceRoomCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        if (code == 0) {

                        }
                    }
                });
            }
        }
    }

    @Override
    public void onSeatMute(int index, boolean isMute) {
        super.onSeatMute(index, isMute);
    }

    @Override
    public void onAnchorEnterSeat(int index, TRTCVoiceRoomDef.UserInfo user) {
        super.onAnchorEnterSeat(index, user);
        if (user.userId.equals(mSelfUserId)) {
            mCurrentRole = TRTCCloudDef.TRTCRoleAnchor;
            mSelfSeatIndex = index;
            refreshView();
        }
    }

    @Override
    public void onAnchorLeaveSeat(int index, TRTCVoiceRoomDef.UserInfo user) {
        super.onAnchorLeaveSeat(index, user);
        if (user.userId.equals(mSelfUserId)) {
            mCurrentRole = TRTCCloudDef.TRTCRoleAudience;
            mSelfSeatIndex = -1;
            if (mAnchorAudioPanel != null) {
                mAnchorAudioPanel.reset();
            }
            refreshView();
        }
    }

    @Override
    public void onRoomDestroy(String roomId) {
        super.onRoomDestroy(roomId);
        ToastUtils.showLong(R.string.trtcvoiceroom_dismiss_room);
        mTRTCVoiceRoom.exitRoom(null);
        finish();
    }
}

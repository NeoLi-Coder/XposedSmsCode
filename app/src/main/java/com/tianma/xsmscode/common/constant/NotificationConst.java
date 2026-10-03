package com.tianma.xsmscode.common.constant;

import com.github.tianma8023.xposed.smscode.BuildConfig;

/**
 * Notification Constants
 */
public interface NotificationConst {

    String CHANNEL_ID_FOREGROUND_SERVICE = "foreground_service";

    String CHANNEL_ID_SMSCODE_NOTIFICATION = BuildConfig.APPLICATION_ID + ".smscode_notification";
    String GROUP_KEY_SMSCODE_NOTIFICATION = BuildConfig.APPLICATION_ID + ".group_key_smscode_notification";

}

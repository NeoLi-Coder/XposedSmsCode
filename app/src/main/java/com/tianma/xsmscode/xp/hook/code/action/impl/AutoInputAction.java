package com.tianma.xsmscode.xp.hook.code.action.impl;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.common.utils.XSPUtils;
import com.tianma.xsmscode.data.db.DBProvider;
import com.tianma.xsmscode.data.db.entity.AppInfo;
import com.tianma.xsmscode.data.db.entity.AppInfoDao;
import com.tianma.xsmscode.data.db.entity.SmsMsg;
import com.tianma.xsmscode.feature.store.EntityStoreManager;
import com.tianma.xsmscode.feature.store.EntityType;
import com.tianma.xsmscode.xp.hook.code.action.CallableAction;
import com.tianma.xsmscode.xp.hook.code.helper.InputHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import com.github.tianma8023.xposed.smscode.BuildConfig;

import de.robv.android.xposed.XSharedPreferences;

/**
 * 自动输入验证码
 */
public class AutoInputAction extends CallableAction {

    public AutoInputAction(Context pluginContext, Context phoneContext, SmsMsg smsMsg, XSharedPreferences xsp) {
        super(pluginContext, phoneContext, smsMsg, xsp);
    }

    @Override
    public Bundle action() {
        xsp.reload();
        if (XSPUtils.isEnabled(xsp) && XSPUtils.autoInputCodeEnabled(xsp)) {
            prepareAutoInputCode(mSmsMsg.getSmsCode());
        }
        return null;
    }

    private void prepareAutoInputCode(String code) {
        KeyguardManager keyguard = (KeyguardManager) mPhoneContext.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguard != null && keyguard.isKeyguardLocked()) {
            XLog.d("Device locked, skip automatic input");
            return;
        }
        if (!autoInputBlockedHere()) {
            autoInputCode(code);
        }
    }

    // auto-input
    private void autoInputCode(String code) {
        try {
            InputHelper.sendText(code);
            XLog.d("Auto input code succeed");
        } catch (Throwable throwable) {
            XLog.e("Error occurs when auto input code", throwable);
        }
    }

    // 是否屏蔽自动输入
    private boolean autoInputBlockedHere() {
        boolean result = false;
        try {
            List<String> blockedAppList = new ArrayList<>();
            try {
                Uri appInfoUri = DBProvider.APP_INFO_URI;
                ContentResolver resolver = mPluginContext.getContentResolver();

                final String packageColumn = AppInfoDao.Properties.PackageName.columnName;
                final String blockedColumn = AppInfoDao.Properties.Blocked.columnName;

                String[] projection = {packageColumn,};
                String selection = blockedColumn + " = ?";
                String[] selectionArgs = {String.valueOf(1)};
                Cursor cursor = resolver.query(appInfoUri, projection, selection, selectionArgs, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        blockedAppList.add(cursor.getString(cursor.getColumnIndexOrThrow(packageColumn)));
                    }
                    cursor.close();
                }
                XLog.d("Get blocked apps by content provider");
            } catch (Exception e) {
                List<AppInfo> appInfoList = EntityStoreManager
                        .loadEntitiesFromFile(EntityType.BLOCKED_APP, AppInfo.class);
                for (AppInfo appInfo : appInfoList) {
                    blockedAppList.add(appInfo.getPackageName());
                }
                XLog.d("Get blocked apps from file");
            }

            if (blockedAppList.isEmpty()) {
                return false;
            }

            List<ActivityManager.RunningTaskInfo> runningTasks = getRunningTasks(mPhoneContext);
            String topPkgPrimary = null;
            if (runningTasks != null && runningTasks.size() > 0) {
                topPkgPrimary = runningTasks.get(0).topActivity == null ? null : runningTasks.get(0).topActivity.getPackageName();
                XLog.d("topPackagePrimary: %s", topPkgPrimary);
            }

            if (topPkgPrimary != null) return blockedAppList.contains(topPkgPrimary);

            List<ActivityManager.RunningAppProcessInfo> appProcesses = getRunningAppProcesses(mPhoneContext);
            Set<String> foregroundPackages = new HashSet<>();
            if (appProcesses != null) {
                for (ActivityManager.RunningAppProcessInfo process : appProcesses) {
                    if (process.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) continue;
                    if (blockedAppList.contains(process.processName)) return true;
                    if (process.pkgList != null) {
                        for (String packageName : process.pkgList) {
                            if (blockedAppList.contains(packageName)) return true;
                            if (!packageName.equals(mPhoneContext.getPackageName())
                                    && !packageName.equals(BuildConfig.APPLICATION_ID)
                                    && !packageName.equals("com.android.systemui")) foregroundPackages.add(packageName);
                        }
                    }
                }
            }
            // 已配置屏蔽名单却无法确认唯一前台应用时，不向未知输入框发送按键。
            result = foregroundPackages.size() != 1;
        } catch (Throwable t) {
            XLog.e("Cannot resolve auto-input target", t);
            result = true;
        }
        return result;
    }

    private List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return am == null ? null : am.getRunningAppProcesses();
    }

    @SuppressWarnings("deprecation")
    private List<ActivityManager.RunningTaskInfo> getRunningTasks(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return am == null ? null : am.getRunningTasks(10);
    }
}

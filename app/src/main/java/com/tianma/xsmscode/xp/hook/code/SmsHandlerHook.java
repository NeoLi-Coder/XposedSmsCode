package com.tianma.xsmscode.xp.hook.code;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Telephony;

import com.github.tianma8023.xposed.smscode.BuildConfig;
import com.github.tianma8023.xposed.smscode.R;
import com.tianma.xsmscode.common.constant.NotificationConst;
import com.tianma.xsmscode.common.utils.NotificationUtils;
import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.xp.helper.XposedWrapper;
import com.tianma.xsmscode.xp.hook.BaseHook;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hook class com.android.internal.telephony.InBoundSmsHandler
 */
public class SmsHandlerHook extends BaseHook {

    public static final String ANDROID_PHONE_PACKAGE = "com.android.phone";

    private static final String TELEPHONY_PACKAGE = "com.android.internal.telephony";
    private static final String SMS_HANDLER_CLASS = TELEPHONY_PACKAGE + ".InboundSmsHandler";
    private static final String SMSCODE_PACKAGE = BuildConfig.APPLICATION_ID;

    // Host App Context
    private Context mPhoneContext;
    // Plugin App Context
    private Context mPluginContext;

    @Override
    public void onLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (ANDROID_PHONE_PACKAGE.equals(lpparam.packageName) || "com.xiaomi.phone".equals(lpparam.packageName)) {
            XLog.i("SmsCode initializing");
            printDeviceInfo();
            try {
                hookSmsHandler(lpparam.classLoader);
            } catch (Throwable e) {
                XLog.e("Failed to hook SmsHandler", e);
            }
            XLog.i("SmsCode initialize completely");
        }
    }

    @SuppressWarnings("deprecation")
    private static void printDeviceInfo() {
        XLog.i("Phone manufacturer: %s", Build.MANUFACTURER);
        XLog.i("Phone model: %s", Build.MODEL);
        XLog.i("Android version: %s", Build.VERSION.RELEASE);
        int xposedVersion;
        try {
            xposedVersion = XposedBridge.getXposedVersion();
        } catch (Throwable e) {
            xposedVersion = XposedBridge.XPOSED_BRIDGE_VERSION;
        }
        XLog.i("Xposed bridge version: %d", xposedVersion);
        XLog.i("SmsCode version: %s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
    }

    private void hookSmsHandler(ClassLoader classloader) {
        hookConstructor(classloader);
        hookDispatchIntent(classloader);
    }

    private void hookConstructor(ClassLoader classloader) {
        Class<?> handler = XposedWrapper.findClass(SMS_HANDLER_CLASS, classloader);
        if (handler != null) XposedBridge.hookAllConstructors(handler, new ConstructorHook());
    }

    private void hookDispatchIntent(ClassLoader classloader) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hookDispatchIntent29(classloader);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hookDispatchIntent23(classloader);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hookDispatchIntent21(classloader);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hookDispatchIntent19(classloader);
        }
    }

    // Android K
    private void hookDispatchIntent19(ClassLoader classloader) {
        XLog.d("Hooking dispatchIntent() for Android v19+");
        XposedHelpers.findAndHookMethod(SMS_HANDLER_CLASS, classloader, "dispatchIntent",
                /*         intent */ Intent.class,
                /*     permission */ String.class,
                /*          appOp */ int.class,
                /* resultReceiver */ BroadcastReceiver.class,
                new DispatchIntentHook(3));
    }

    // Android L+
    private void hookDispatchIntent21(ClassLoader classloader) {
        XLog.d("Hooking dispatchIntent() for Android v21+");
        XposedHelpers.findAndHookMethod(SMS_HANDLER_CLASS, classloader, "dispatchIntent",
                /*         intent */ Intent.class,
                /*     permission */ String.class,
                /*          appOp */ int.class,
                /* resultReceiver */ BroadcastReceiver.class,
                /*           user */ UserHandle.class,
                new DispatchIntentHook(3));
    }

    // Android M+
    private void hookDispatchIntent23(ClassLoader classloader) {
        XLog.d("Hooking dispatchIntent() for Android v23+");
        XposedHelpers.findAndHookMethod(SMS_HANDLER_CLASS, classloader, "dispatchIntent",
                /*         intent */ Intent.class,
                /*     permission */ String.class,
                /*          appOp */ int.class,
                /*           opts */ Bundle.class,
                /* resultReceiver */ BroadcastReceiver.class,
                /*           user */ UserHandle.class,
                new DispatchIntentHook(4));
    }

    // Android 10+
    private void hookDispatchIntent29(ClassLoader classLoader) {
        XLog.d("Hooking dispatchIntent() for Android v29+");
        // 实际上这是一个通用的方式，不再使用精确匹配来找到对应的 Method，而使用模糊搜索的方式
        // 但是之前分 API 匹配的逻辑在以往 Android 版本的系统之中已经验证通过，故而保留原有逻辑

        Class<?> inboundSmsHandlerClass = XposedWrapper.findClass(SMS_HANDLER_CLASS, classLoader);
        if (inboundSmsHandlerClass == null) {
            XLog.e("Class: %s cannot found", SMS_HANDLER_CLASS);
            return;
        }

        int hooked = 0;
        for (Method method : inboundSmsHandlerClass.getDeclaredMethods()) {
            if (!"dispatchIntent".equals(method.getName()) || method.getReturnType() != void.class) continue;
            int intentIndex = -1;
            int receiverIndex = -1;
            Class<?>[] types = method.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                if (Intent.class.isAssignableFrom(types[i])) intentIndex = i;
                if (BroadcastReceiver.class.isAssignableFrom(types[i])) receiverIndex = i;
            }
            if (intentIndex >= 0) {
                XposedWrapper.hookMethod(method, new DispatchIntentHook(intentIndex, receiverIndex));
                hooked++;
            }
        }
        XLog.i("Installed %d SMS dispatch hooks", hooked);
    }

    private class ConstructorHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                afterConstructorHandler(param);
            } catch (Throwable e) {
                XLog.e("Error occurred in constructor hook", e);
                // 模块错误不能中断电话服务原有逻辑。
            }
        }
    }

    private void afterConstructorHandler(XC_MethodHook.MethodHookParam param) {
        if (mPhoneContext == null) {
            for (Object argument : param.args) {
                if (argument instanceof Context) {
                    mPhoneContext = (Context) argument;
                    break;
                }
            }
        }
        initializeContext(param.thisObject);
    }

    private void initializeContext(Object handler) {
        if (mPhoneContext == null) {
            try {
                Object context = XposedHelpers.getObjectField(handler, "mContext");
                if (context instanceof Context) mPhoneContext = (Context) context;
            } catch (Throwable e) {
                XLog.e("Cannot recover phone context", e);
            }
        }
        if (mPhoneContext == null) return;
        long identity = Binder.clearCallingIdentity();
        try {
            if (mPluginContext == null) {
                mPluginContext = mPhoneContext.createPackageContext(SMSCODE_PACKAGE, Context.CONTEXT_IGNORE_SECURITY);
                initNotificationChannel();
            }
            // 允许用户在模块运行后打开通知开关；接收器在进程内只注册一次。
            CopyCodeReceiver.registerMe(mPhoneContext);
        } catch (Throwable e) {
            XLog.e("Initialize SMS runtime context failed", e);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = NotificationConst.CHANNEL_ID_SMSCODE_NOTIFICATION;
            String channelName = getPluginContext().getString(R.string.channel_name_smscode_notification);
            NotificationUtils.createNotificationChannel(mPhoneContext,
                    channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            XLog.d("Init notification channel succeed");
        }
    }

    private final ThreadLocal<Integer> dispatchDepth = new ThreadLocal<Integer>() {
        @Override protected Integer initialValue() { return 0; }
    };

    private class DispatchIntentHook extends XC_MethodHook {
        private final int mReceiverIndex;
        private final int mIntentIndex;

        DispatchIntentHook(int receiverIndex) {
            this(0, receiverIndex);
        }

        DispatchIntentHook(int intentIndex, int receiverIndex) {
            mIntentIndex = intentIndex;
            mReceiverIndex = receiverIndex;
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                Intent intent = (Intent) param.args[mIntentIndex];
                if (intent == null || !Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) return;
                int depth = dispatchDepth.get();
                dispatchDepth.set(depth + 1);
                param.setObjectExtra("smscode_dispatch", true);
                if (depth == 0) beforeDispatchIntentHandler(param, intent, mReceiverIndex);
            } catch (Throwable e) {
                XLog.e("Error occurred in dispatchIntent() hook, ", e);
                // 模块错误不能中断电话服务原有逻辑。
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            if (Boolean.TRUE.equals(param.getObjectExtra("smscode_dispatch"))) {
                int depth = dispatchDepth.get() - 1;
                if (depth == 0) dispatchDepth.remove();
                else dispatchDepth.set(depth);
            }
        }
    }

    private void beforeDispatchIntentHandler(XC_MethodHook.MethodHookParam param, Intent intent, int receiverIndex) {
        String action = intent.getAction();

        // We only care about the initial SMS_DELIVER intent,
        // the rest are irrelevant
        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(action)) {
            return;
        }

        initializeContext(param.thisObject);
        if (mPhoneContext == null || mPluginContext == null) return;
        ParseResult parseResult = new CodeWorker(getPluginContext(), mPhoneContext, intent).parse();
        if (parseResult != null) {// parse succeed
            if (parseResult.isBlockSms() && receiverIndex >= 0 && param.args[receiverIndex] != null) {
                XLog.d("Blocking code SMS...");
                if (deleteRawTableAndSendMessage(param.thisObject, param.args[receiverIndex])) param.setResult(null);
            }
        }
    }

    private static final int EVENT_BROADCAST_COMPLETE = 3;

    private boolean deleteRawTableAndSendMessage(Object inboundSmsHandler, Object smsReceiver) {
        long token = Binder.clearCallingIdentity();
        try {
            deleteFromRawTable(inboundSmsHandler, smsReceiver);
        } catch (Throwable e) {
            XLog.e("Error occurs when delete SMS data from raw table", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }

        sendEventBroadcastComplete(inboundSmsHandler);
        return true;
    }

    private void sendEventBroadcastComplete(Object inboundSmsHandler) {
        XLog.d("Send event(EVENT_BROADCAST_COMPLETE)");
        XposedHelpers.callMethod(inboundSmsHandler, "sendMessage", EVENT_BROADCAST_COMPLETE);
    }

    private void deleteFromRawTable(Object inboundSmsHandler, Object smsReceiver) throws ReflectiveOperationException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            deleteFromRawTable24(inboundSmsHandler, smsReceiver);
        } else {
            deleteFromRawTable19(inboundSmsHandler, smsReceiver);
        }
    }

    private void deleteFromRawTable19(Object inboundSmsHandler, Object smsReceiver) throws ReflectiveOperationException {
        XLog.d("Delete raw SMS data from database on Android 19+");
        Object deleteWhere = XposedHelpers.getObjectField(smsReceiver, "mDeleteWhere");
        Object deleteWhereArgs = XposedHelpers.getObjectField(smsReceiver, "mDeleteWhereArgs");

        callDeclaredMethod(SMS_HANDLER_CLASS, inboundSmsHandler, "deleteFromRawTable",
                /* String deleteWhere       */ deleteWhere,
                /* String[] deleteWhereArgs */ deleteWhereArgs);
    }

    private void deleteFromRawTable24(Object inboundSmsHandler, Object smsReceiver) throws ReflectiveOperationException {
        XLog.d("Delete raw SMS data from database on Android 24+");
        Object deleteWhere = XposedHelpers.getObjectField(smsReceiver, "mDeleteWhere");
        Object deleteWhereArgs = XposedHelpers.getObjectField(smsReceiver, "mDeleteWhereArgs");
        final int MARK_DELETED = 2;

        callDeclaredMethod(SMS_HANDLER_CLASS, inboundSmsHandler, "deleteFromRawTable",
                /* String deleteWhere       */ deleteWhere,
                /* String[] deleteWhereArgs */ deleteWhereArgs,
                /* int deleteType           */ MARK_DELETED);
    }

    private static Object callDeclaredMethod(String className, Object obj, String methodName, Object... args) throws InvocationTargetException, IllegalAccessException {
        // XposedHelpers#callMethod() 方法，不能反射调用 private 的方法
        // 而本方法可以反射调用指定类的 private 方法
        Class<?> clz = XposedHelpers.findClass(className, obj.getClass().getClassLoader());
        Method method = XposedHelpers.findMethodBestMatch(clz, methodName, args);
        return method.invoke(obj, args);
    }

    private Context getPluginContext() {
        if (mPluginContext == null) {
            try {
                mPluginContext = mPhoneContext.createPackageContext(SMSCODE_PACKAGE,
                        Context.CONTEXT_IGNORE_SECURITY);
            } catch (Exception e) {
                XLog.e("Create plugin context failed: %s", e);
            }
        }
        return mPluginContext;
    }

}

package com.tianma.xsmscode.common.utils;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;

public class ClipboardUtils {

    private ClipboardUtils() {
    }

    public static void copyToClipboard(Context context, String text) {
        if (context == null || text == null) return;
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) {
            XLog.e("Copy failed, clipboard manager is null");
            return;
        }
        ClipData clipData = ClipData.newPlainText("Copy text", text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
            clipData.getDescription().setExtras(extras);
        }
        cm.setPrimaryClip(clipData);
        XLog.i("Copy to clipboard succeed");
    }

    public static void clearClipboard(Context context) {
        if (context == null) return;
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) {
            XLog.e("Clear failed, clipboard manager is null");
            return;
        }
        if(cm.hasPrimaryClip()) {
            ClipDescription cd = cm.getPrimaryClipDescription();
            if (cd != null) {
                if (cd.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    cm.setPrimaryClip(ClipData.newPlainText("Copy text", ""));
                    XLog.i("Clear clipboard succeed");
                }
            }
        }
    }

}

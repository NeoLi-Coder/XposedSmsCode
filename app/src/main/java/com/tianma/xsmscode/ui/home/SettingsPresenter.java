package com.tianma.xsmscode.ui.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.tianma.xsmscode.common.constant.Const;
import com.tianma.xsmscode.common.utils.SmsCodeUtils;
import com.tianma.xsmscode.common.utils.StorageUtils;
import com.tianma.xsmscode.common.utils.Utils;

import java.io.File;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SettingsPresenter implements SettingsContract.Presenter {

    private SettingsContract.View mView;
    private Context mContext;

    private CompositeDisposable mCompositeDisposable;

    @Inject
    SettingsPresenter() {
        mCompositeDisposable = new CompositeDisposable();
    }

    @Inject
    @Override
    public void onAttach(Context context, SettingsContract.View view) {
        mContext = context;
        mView = view;
    }

    @Override
    public void onDetach() {
        mView = null;
        if (mCompositeDisposable.size() > 0) {
            mCompositeDisposable.clear();
        }
    }

    @Override
    public void setPreferenceWorldWritable(String preferencesName) {
        // 旧版逻辑，Xposed or 太极阳
        // dataDir: /data/data/<package_name>/
        // spDir: /data/data/<package_name>/shared_prefs/
        // spFile: /data/data/<package_name>/shared_prefs/<preferences_name>.xml
        File prefsFile = StorageUtils.getSharedPreferencesFile(mContext, preferencesName);
        StorageUtils.setFileWorldWritable(prefsFile, 2);

        // 新版(2.2.8)起，AndroidManifest.xml 配置了 xposedsharedprefs meta 数据，
        // EdXposed 或者 LSPosed 会 Hook SP的路径 (/data/misc/{UUID}/{packageName}/prefs/{prefName}.xml)
        // 所以上面的 prefsFile 不会存在，具体参考: https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences
    }

    @Override
    public void hideOrShowLauncherIcon(boolean hide) {
        PackageManager pm = mContext.getPackageManager();
        ComponentName launcherCN = new ComponentName(mContext, Const.HOME_ACTIVITY_ALIAS);
        int state = hide ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        if (pm.getComponentEnabledSetting(launcherCN) != state) {
            pm.setComponentEnabledSetting(launcherCN, state, PackageManager.DONT_KILL_APP);
        }
    }

    @Override
    public void performSmsCodeTest(String msgBody) {
        Disposable disposable = Observable
                .create((ObservableOnSubscribe<String>) emitter -> {
                    String code = TextUtils.isEmpty(msgBody) ? "" :
                            SmsCodeUtils.parseSmsCodeIfExists(mContext, msgBody, false);
                    emitter.onNext(code);
                    emitter.onComplete();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(code -> mView.showSmsCodeTestResult(code),
                        throwable -> mView.showSmsCodeTestResult(""));
        mCompositeDisposable.add(disposable);
    }

    @Override
    public void showSourceProject() {
        Utils.showWebPage(mContext, Const.PROJECT_SOURCE_CODE_URL);
    }

    @Override
    public void setInternalFilesWritable() {
        // dataDir or external dataDir
        // filesDir or external filesDir
        StorageUtils.setFileWorldWritable(StorageUtils.getFilesDir(), 1);
    }

}

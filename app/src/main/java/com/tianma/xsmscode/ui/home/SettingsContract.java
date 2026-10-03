package com.tianma.xsmscode.ui.home;

import com.tianma.xsmscode.common.mvp.BasePresenter;
import com.tianma.xsmscode.common.mvp.BaseView;

public interface SettingsContract {

    interface View extends BaseView {

        void showSmsCodeTestResult(String code);

        void showPrivacyPolicy();
    }

    interface Presenter extends BasePresenter<View> {

        void setPreferenceWorldWritable(String preferencesName);

        void hideOrShowLauncherIcon(boolean hide);

        void performSmsCodeTest(String msgBody);

        void showSourceProject();

        void setInternalFilesWritable();
    }

}

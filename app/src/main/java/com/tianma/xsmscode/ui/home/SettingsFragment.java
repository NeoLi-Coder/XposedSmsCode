package com.tianma.xsmscode.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Rect;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.tianma8023.xposed.smscode.BuildConfig;
import com.github.tianma8023.xposed.smscode.R;
import com.jaredrummler.cyanea.prefs.CyaneaSettingsActivity;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.common.preference.ResetEditPreference;
import com.tianma.xsmscode.common.preference.ResetEditPreferenceDialogFragCompat;
import com.tianma.xsmscode.common.utils.ModuleUtils;
import com.tianma.xsmscode.common.utils.SPUtils;
import com.tianma.xsmscode.common.utils.SnackbarHelper;
import com.tianma.xsmscode.common.utils.XLog;
import com.tianma.xsmscode.ui.app.base.BasePreferenceFragment;
import com.tianma.xsmscode.ui.block.AppBlockActivity;
import com.tianma.xsmscode.ui.rule.CodeRulesActivity;

import java.util.Objects;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import dagger.android.support.AndroidSupportInjection;

/**
 * 首选项Fragment
 */
public class SettingsFragment extends BasePreferenceFragment implements
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener,
        HasAndroidInjector,
        SettingsContract.View {

    private HomeActivity mActivity;

    @Inject
    DispatchingAndroidInjector<Object> androidInjector;

    @Inject
    SettingsContract.Presenter mPresenter;

    public SettingsFragment() {
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return androidInjector;
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @NonNull
    @Override
    public <T extends Preference> T findPreference(@NonNull CharSequence key) {
        return Objects.requireNonNull(super.findPreference(key));
    }

    @Override
    protected void doOnCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);

        // general group
        if (!ModuleUtils.isModuleEnabled()) {
            Preference enablePref = findPreference(PrefConst.KEY_ENABLE);
            enablePref.setSummary(R.string.pref_enable_summary_alt);
        }

        findPreference(PrefConst.KEY_HIDE_LAUNCHER_ICON).setOnPreferenceChangeListener(this);
        findPreference(PrefConst.KEY_CHOOSE_THEME).setOnPreferenceClickListener(this);
        // general group end

        // SMS code group
        EditTextPreference autoInputDelayPref = findPreference(PrefConst.KEY_AUTO_INPUT_CODE_DELAY);
        autoInputDelayPref.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setSelection(editText.getText().length());
        });
        showAutoInputDelaySummary(autoInputDelayPref, autoInputDelayPref.getText());
        autoInputDelayPref.setOnPreferenceChangeListener(this);

        findPreference(PrefConst.KEY_SMSCODE_KEYWORDS).setOnPreferenceChangeListener(this);
        findPreference(PrefConst.KEY_APP_BLOCK_ENTRY).setOnPreferenceClickListener(this);
        // SMS code group end

        // experimental group
        // experimental group end

        // code rule group
        findPreference(PrefConst.KEY_CODE_RULES).setOnPreferenceClickListener(this);
        findPreference(PrefConst.KEY_SMSCODE_TEST).setOnPreferenceClickListener(this);
        // code rule group end

        // code records group
        Preference recordsEntryPref = findPreference(PrefConst.KEY_ENTRY_CODE_RECORDS);
        recordsEntryPref.setOnPreferenceClickListener(this);
        initRecordEntryPreference(recordsEntryPref);
        // code records group end

        // others group
        findPreference(PrefConst.KEY_VERBOSE_LOG_MODE).setOnPreferenceChangeListener(this);
        // others group end

        // about group
        // version info preference
        Preference versionPref = findPreference(PrefConst.KEY_VERSION);
        versionPref.setSelectable(false);
        showVersionInfo(versionPref);
        findPreference(PrefConst.KEY_SOURCE_CODE).setOnPreferenceClickListener(this);
        findPreference(PrefConst.KEY_PRIVACY_POLICY).setOnPreferenceClickListener(this);
        // about group end
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mActivity = (HomeActivity) requireActivity();
        mPresenter.onAttach(mActivity, this);
        setDivider(null);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        getListView().setPadding(padding, 0, padding, padding);
        getListView().setClipToPadding(false);
        // 保留 Preference 的依赖和持久化机制，仅绘制分组卡片背景。
        getListView().addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override public void getItemOffsets(@NonNull Rect outRect, @NonNull View child,
                                                  @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                if (child.findViewById(android.R.id.widget_frame) != null) {
                    child.setBackgroundResource(R.drawable.preference_row_background);
                    outRect.bottom = (int) (4 * getResources().getDisplayMetrics().density);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        String preferencesName = getPreferenceManager().getSharedPreferencesName();
        mPresenter.setPreferenceWorldWritable(preferencesName);
        mPresenter.setInternalFilesWritable();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (PrefConst.KEY_CHOOSE_THEME.equals(key)) {
            Intent intent = new Intent(mActivity, CyaneaSettingsActivity.class);
            startActivity(intent);
        } else if (PrefConst.KEY_CODE_RULES.equals(key)) {
            CodeRulesActivity.startToMe(mActivity);
        } else if (PrefConst.KEY_SMSCODE_TEST.equals(key)) {
            showSmsCodeTestDialog();
        } else if (PrefConst.KEY_SOURCE_CODE.equals(key)) {
            mPresenter.showSourceProject();
        } else if (PrefConst.KEY_ENTRY_CODE_RECORDS.equals(key)) {
            mActivity.selectTab(R.id.tab_records);
        } else if (PrefConst.KEY_APP_BLOCK_ENTRY.equals(key)) {
            AppBlockActivity.startMe(mActivity);
        } else if(PrefConst.KEY_PRIVACY_POLICY.equals(key)) {
            showPrivacyPolicy();
        } else {
            return false;
        }
        return true;
    }

    private void showVersionInfo(Preference preference) {
        String summary = getString(R.string.pref_version_summary, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        preference.setSummary(summary);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (PrefConst.KEY_HIDE_LAUNCHER_ICON.equals(key)) {
            mPresenter.hideOrShowLauncherIcon((Boolean) newValue);
        } else if (PrefConst.KEY_VERBOSE_LOG_MODE.equals(key)) {
            onVerboseLogModeSwitched((Boolean) newValue);
        } else if (PrefConst.KEY_SMSCODE_KEYWORDS.equals(key)) {
            try {
                if (TextUtils.isEmpty((String) newValue)) throw new PatternSyntaxException("empty", "", 0);
                Pattern.compile((String) newValue);
            } catch (PatternSyntaxException e) {
                SnackbarHelper.makeLong(getListView(), R.string.invalid_regex_hint).show();
                return false;
            }
        } else if(PrefConst.KEY_AUTO_INPUT_CODE_DELAY.equals(key)) {
            return onAutoInputDelayPrefChanged(preference, newValue);
        } else {
            return false;
        }
        return true;
    }

    private void onVerboseLogModeSwitched(boolean on) {
        XLog.setLogLevel(on ? Log.VERBOSE : BuildConfig.LOG_LEVEL);
    }

    private void showSmsCodeTestDialog() {
        new MaterialDialog.Builder(mActivity)
                .title(R.string.pref_smscode_test_title)
                .input(R.string.sms_content_hint, 0, true,
                        (dialog, input) -> mPresenter.performSmsCodeTest(input.toString()))
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                .negativeText(R.string.cancel)
                .show();
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        boolean handled = false;
        if (preference instanceof ResetEditPreference) {
            DialogFragment dialogFragment =
                    ResetEditPreferenceDialogFragCompat.newInstance(preference.getKey());

            FragmentManager fm = getFragmentManager();
            if (fm != null) {
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(fm, "android.support.v7.preference.PreferenceFragment.DIALOG");
                handled = true;
            }
        }
        if (!handled) {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private void initRecordEntryPreference(Preference preference) {
        String summary = getString(R.string.pref_entry_code_records_summary, PrefConst.MAX_SMS_RECORDS_COUNT_DEFAULT);
        preference.setSummary(summary);
    }

    @Override
    public void showSmsCodeTestResult(String code) {
        String text = TextUtils.isEmpty(code) ? getString(R.string.cannot_parse_smscode)
                : getString(R.string.current_sms_code, code);
        SnackbarHelper.makeLong(getListView(), text).show();
    }

    @Override
    public void showPrivacyPolicy() {
        mActivity.showPrivacyPolicy();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSwitches(getPreferenceScreen());
    }

    private void refreshSwitches(PreferenceGroup group) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            if (preference instanceof TwoStatePreference) {
                TwoStatePreference control = (TwoStatePreference) preference;
                control.setChecked(getPreferenceManager().getSharedPreferences()
                        .getBoolean(control.getKey(), control.isChecked()));
            } else if (preference instanceof PreferenceGroup) {
                refreshSwitches((PreferenceGroup) preference);
            }
        }
    }

    @Override
    public void onDestroyView() {
        mPresenter.onDetach();
        super.onDestroyView();
    }

    private boolean onAutoInputDelayPrefChanged(Preference preference, Object newValue) {
        if (newValue instanceof String) {
            String value = (String) newValue;
            try {
                long seconds = Long.parseLong(value);
                if (seconds < 0 || seconds > Long.MAX_VALUE / 1000) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                SnackbarHelper.makeLong(getListView(), R.string.invalid_delay_hint).show();
                return false;
            }
            showAutoInputDelaySummary(preference, value);
            return true;
        } else {
            return false;
        }
    }

    private void showAutoInputDelaySummary(Preference preference, String value) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        String summary = context.getString(R.string.pref_auto_input_code_delay_summary, value);
        preference.setSummary(summary);
    }
}

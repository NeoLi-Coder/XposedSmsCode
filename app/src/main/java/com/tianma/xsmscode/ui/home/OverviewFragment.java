package com.tianma.xsmscode.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.tianma8023.xposed.smscode.BuildConfig;
import com.github.tianma8023.xposed.smscode.R;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.common.utils.ModuleUtils;
import com.tianma.xsmscode.common.utils.PackageUtils;
import com.tianma.xsmscode.ui.rule.CodeRulesActivity;

public class OverviewFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences preferences;
    private boolean refreshing;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        preferences = requireContext().getSharedPreferences(PrefConst.PREF_NAME, Context.MODE_PRIVATE);
        view.findViewById(R.id.overview_activation).setOnClickListener(v -> {
            if (!PackageUtils.openModuleInLsposed(requireContext())) {
                new MaterialDialog.Builder(requireContext())
                        .title(R.string.module_activation_title)
                        .content(getString(R.string.module_manager_open_failed, getString(R.string.app_name)))
                        .positiveText(R.string.i_know).show();
            }
        });
        bindSwitch(view, R.id.overview_enable, PrefConst.KEY_ENABLE);
        bindSwitch(view, R.id.overview_auto_input, PrefConst.KEY_ENABLE_AUTO_INPUT_CODE);
        bindSwitch(view, R.id.overview_notification, PrefConst.KEY_SHOW_CODE_NOTIFICATION);
        bindSwitch(view, R.id.overview_clipboard, PrefConst.KEY_COPY_TO_CLIPBOARD);
        view.findViewById(R.id.overview_records).setOnClickListener(v -> home().selectTab(R.id.tab_records));
        view.findViewById(R.id.overview_settings).setOnClickListener(v -> home().selectTab(R.id.tab_settings));
        view.findViewById(R.id.overview_rules).setOnClickListener(v -> CodeRulesActivity.startToMe(requireContext()));
        ((TextView) view.findViewById(R.id.overview_version)).setText(
                getString(R.string.overview_version_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        ((TextView) view.findViewById(R.id.overview_device)).setText(
                getString(R.string.overview_device_format, Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE));
    }

    private HomeActivity home() { return (HomeActivity) requireActivity(); }

    private void bindSwitch(View view, int id, String key) {
        ((SwitchCompat) view.findViewById(id)).setOnCheckedChangeListener((button, checked) -> {
            if (!refreshing) preferences.edit().putBoolean(key, checked).apply();
        });
    }

    private void refresh() {
        View view = getView();
        if (view == null) return;
        boolean enabled = preferences.getBoolean(PrefConst.KEY_ENABLE, true);
        boolean activated = ModuleUtils.isModuleEnabled();
        ((TextView) view.findViewById(R.id.overview_status)).setText(activated
                ? R.string.module_status_active : R.string.module_status_inactive);
        ((TextView) view.findViewById(R.id.overview_status_hint)).setText(activated
                ? R.string.overview_active_hint : R.string.overview_inactive_hint);
        View activation = view.findViewById(R.id.overview_activation);
        activation.setClickable(!activated);
        activation.setFocusable(!activated);
        ((TextView) view.findViewById(R.id.overview_status)).setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, 0, activated ? 0 : R.drawable.ic_chevron, 0);
        refreshing = true;
        ((SwitchCompat) view.findViewById(R.id.overview_enable)).setChecked(enabled);
        setSwitch(view, R.id.overview_auto_input, PrefConst.KEY_ENABLE_AUTO_INPUT_CODE, true, enabled);
        setSwitch(view, R.id.overview_notification, PrefConst.KEY_SHOW_CODE_NOTIFICATION, true, enabled);
        setSwitch(view, R.id.overview_clipboard, PrefConst.KEY_COPY_TO_CLIPBOARD, false, enabled);
        refreshing = false;
    }

    private void setSwitch(View view, int id, String key, boolean defaultValue, boolean enabled) {
        SwitchCompat control = view.findViewById(id);
        control.setChecked(preferences.getBoolean(key, defaultValue));
        control.setEnabled(enabled);
    }

    @Override
    public void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(this);
        refresh();
    }

    @Override
    public void onPause() {
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) { refresh(); }
}

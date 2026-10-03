package com.tianma.xsmscode.ui.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.core.graphics.ColorUtils;
import android.util.TypedValue;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.tianma8023.xposed.smscode.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.common.fragment.backpress.BackPressEventDispatchHelper;
import com.tianma.xsmscode.common.utils.PackageUtils;
import com.tianma.xsmscode.common.utils.SPUtils;
import com.tianma.xsmscode.common.utils.StorageUtils;
import com.tianma.xsmscode.ui.app.base.BaseActivity;
import com.tianma.xsmscode.ui.faq.FaqFragment;
import com.tianma.xsmscode.ui.record.CodeRecordFragment;

public class HomeActivity extends BaseActivity {
    private static final String KEY_TAB = "home_tab";
    private Toolbar toolbar;
    private int foregroundColor;
    private BottomNavigationView navigation;
    private int selectedTab = R.id.tab_overview;
    private MaterialDialog privacyDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        toolbar = findViewById(R.id.toolbar);
        navigation = findViewById(R.id.home_navigation);
        setSupportActionBar(toolbar);
        TypedValue background = new TypedValue();
        TypedValue foreground = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorBackground, background, true);
        getTheme().resolveAttribute(android.R.attr.textColorPrimary, foreground, true);
        foregroundColor = foreground.resourceId != 0
                ? androidx.core.content.ContextCompat.getColor(this, foreground.resourceId) : foreground.data;
        findViewById(R.id.home_shell).setBackgroundColor(ColorUtils.blendARGB(background.data, foregroundColor, 0.035f));
        getExternalFilesDir("");
        shareXposedPreferences();
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(KEY_TAB, R.id.tab_overview);
        }
        navigation.setSelectedItemId(selectedTab);
        navigation.setOnItemSelectedListener(item -> {
            displayTab(item.getItemId());
            return true;
        });
        // 恢复 FAQ 时保留 FragmentManager 已恢复的可见页面。
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) displayTab(selectedTab);
        refreshChrome();
        if (!SPUtils.isPrivacyPolicyAccepted(this)) {
            showPrivacyPolicy();
        }
    }

    public void selectTab(int tab) {
        if (navigation.getSelectedItemId() == tab) displayTab(tab);
        else navigation.setSelectedItemId(tab);
    }

    private void displayTab(int tab) {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        selectedTab = tab;
        String tag = "home:" + tab;
        Fragment target = manager.findFragmentByTag(tag);
        FragmentTransaction transaction = manager.beginTransaction();
        for (Fragment fragment : manager.getFragments()) {
            if (!fragment.isHidden() && fragment != target) {
                transaction.hide(fragment).setMaxLifecycle(fragment, Lifecycle.State.STARTED);
            }
        }
        if (target == null) {
            if (tab == R.id.tab_records) {
                target = CodeRecordFragment.newInstance();
            } else if (tab == R.id.tab_settings) {
                target = SettingsFragment.newInstance();
            } else {
                target = new OverviewFragment();
            }
            transaction.add(R.id.home_content, target, tag);
        } else {
            transaction.show(target);
        }
        transaction.setMaxLifecycle(target, Lifecycle.State.RESUMED)
                .setPrimaryNavigationFragment(target).commitNow();
        refreshChrome();
        invalidateOptionsMenu();
    }

    private void refreshChrome() {
        boolean faq = getSupportFragmentManager().getBackStackEntryCount() > 0;
        navigation.setVisibility(faq ? View.GONE : View.VISIBLE);
        toolbar.setTitle(faq ? R.string.action_home_faq_title : selectedTab == R.id.tab_records
                ? R.string.smscode_records : selectedTab == R.id.tab_settings
                ? R.string.tab_settings_title : R.string.app_name);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(faq);
        tintToolbarIcons();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_TAB, selectedTab);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        FragmentManager manager = getSupportFragmentManager();
        if (manager.getBackStackEntryCount() > 0) {
            manager.popBackStackImmediate();
            refreshChrome();
            invalidateOptionsMenu();
        } else if (!BackPressEventDispatchHelper.dispatchBackPressedEvent(this)) {
            if (selectedTab != R.id.tab_overview) selectTab(R.id.tab_overview);
            else super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            getMenuInflater().inflate(R.menu.menu_home, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        for (int i = 0; i < menu.size(); i++) {
            android.graphics.drawable.Drawable icon = menu.getItem(i).getIcon();
            if (icon != null) icon.mutate().setTint(foregroundColor);
        }
        tintToolbarIcons();
        return true;
    }

    private void tintToolbarIcons() {
        toolbar.setTitleTextColor(foregroundColor);
        if (toolbar.getNavigationIcon() != null) toolbar.getNavigationIcon().mutate().setTint(foregroundColor);
        if (toolbar.getOverflowIcon() != null) toolbar.getOverflowIcon().mutate().setTint(foregroundColor);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_home_faq) {
            Fragment current = getSupportFragmentManager().getPrimaryNavigationFragment();
            getSupportFragmentManager().beginTransaction().hide(current)
                    .add(R.id.home_content, FaqFragment.newInstance(), "faq")
                    .addToBackStack("faq").commit();
            getSupportFragmentManager().executePendingTransactions();
            refreshChrome();
            invalidateOptionsMenu();
            return true;
        } else if (item.getItemId() == R.id.action_taichi_users_notice) {
            onTaichiUsersNoticeSelected();
            return true;
        } else if (item.getItemId() == R.id.action_edxposed_users_notice) {
            onEdxposedUsersNoticeSelected();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void onTaichiUsersNoticeSelected() {
        new MaterialDialog.Builder(this).title(R.string.taichi_users_notice)
                .content(R.string.taichi_users_notice_content)
                .negativeText(R.string.add_apps_in_taichi)
                .onNegative((dialog, which) -> PackageUtils.startAddAppsInTaiChi(this))
                .positiveText(R.string.check_module_in_taichi)
                .onPositive((dialog, which) -> PackageUtils.startCheckModuleInTaiChi(this)).show();
    }

    void onEdxposedUsersNoticeSelected() {
        new MaterialDialog.Builder(this).title(R.string.edxposed_users_notice)
                .content(R.string.edxposed_users_notice_content).positiveText(R.string.i_know).show();
    }

    void showPrivacyPolicy() {
        if (privacyDialog != null && privacyDialog.isShowing()) return;
        privacyDialog = new MaterialDialog.Builder(this).title(R.string.privacy_dialog_title)
                .content(R.string.privacy_dialog_content).positiveText(R.string.privacy_dialog_confirm)
                .onPositive((dialog, which) -> SPUtils.setPrivacyPolicyAccepted(this, true))
                .negativeText(R.string.privacy_dialog_cancel)
                .onNegative((dialog, which) -> { SPUtils.setPrivacyPolicyAccepted(this, false); finish(); })
                .cancelable(false).canceledOnTouchOutside(false).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        StorageUtils.setFileWorldWritable(StorageUtils.getSharedPreferencesFile(this, PrefConst.PREF_NAME), 2);
        StorageUtils.setFileWorldWritable(StorageUtils.getFilesDir(), 1);
    }

    @Override
    protected void onDestroy() {
        if (privacyDialog != null) privacyDialog.dismiss();
        super.onDestroy();
    }

    @SuppressLint("WorldReadableFiles")
    private void shareXposedPreferences() {
        try {
            getSharedPreferences(PrefConst.PREF_NAME, Context.MODE_WORLD_READABLE);
        } catch (SecurityException ignored) {
            // 框架未启用时不支持跨进程首选项；界面仍应正常打开。
        }
    }
}

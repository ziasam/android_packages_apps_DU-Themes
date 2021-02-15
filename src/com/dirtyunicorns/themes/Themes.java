/*
 * Copyright (C) 2019-2020 The Dirty Unicorns Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dirtyunicorns.themes;

import static android.os.UserHandle.USER_SYSTEM;
import static com.dirtyunicorns.themes.utils.duUtils.getScheduledStartThemeSummary;
import static com.dirtyunicorns.themes.utils.duUtils.getScheduledStartThemeTime;
import static com.dirtyunicorns.themes.utils.duUtils.getThemeSchedule;
import static com.dirtyunicorns.themes.utils.duUtils.handleBackgrounds;
import static com.dirtyunicorns.themes.utils.duUtils.handleOverlays;
import static com.dirtyunicorns.themes.utils.duUtils.isLiveWallpaper;
import static com.dirtyunicorns.themes.utils.duUtils.threeButtonNavbarEnabled;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.UiModeManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.du.Utils;
import com.android.internal.util.du.ThemesUtils;

import com.dirtyunicorns.themes.db.ThemeDatabase;

import java.util.Calendar;

public class Themes extends PreferenceFragment implements ThemesListener {

    private static final String TAG = "Themes";

    private static final String PREF_BACKUP_THEMES = "backup_themes";
    private static final String PREF_RESTORE_THEMES = "restore_themes";
    private static final String PREF_WP_PREVIEW = "wp_preview";
    private static final String PREF_THEME_SCHEDULE = "theme_schedule";
    private static final String PREF_THEME_ACCENT_PICKER = "theme_accent_picker";
    private static final String PREF_SWITCH_STYLE = "switch_style";
    public static final String PREF_THEME_ACCENT_COLOR = "theme_accent_color";
    private static final String PREF_THEME_NAVBAR_PICKER = "theme_navbar_picker";
    public static final String PREF_THEME_NAVBAR_STYLE = "theme_navbar_style";
    private static final String PREF_THEME_QSSTYLE_PICKER = "theme_qsstyle_picker";
    public static final String PREF_THEME_QSTILE_STYLE = "theme_qstile_style";
    public static final String PREF_ADAPTIVE_ICON_SHAPE = "adapative_icon_shape";
    public static final String PREF_FONT_PICKER = "font_picker";
    public static final String PREF_STATUSBAR_ICONS = "statusbar_icons";
    public static final String PREF_THEME_SWITCH = "theme_switch";
    private static final String BRIGHTNESS_SLIDER_STYLE = "brightness_slider_style";
    private static final String PREF_PANEL_BG = "panel_bg";
    private static final String PREF_QS_SHAPE = "qs_shape";
    private static final String PREF_ROUNDED_CORNER = "rounded_ui";

    private int mBackupLimit = 10;
    private static boolean mUseSharedPrefListener;
    private String[] mAccentName;
    private String[] mNavbarName;
    private String[] mQSStyleName;

    private Context mContext;
    private IOverlayManager mOverlayManager;
    private SharedPreferences mSharedPreferences;
    private ThemeDatabase mThemeDatabase;
    private UiModeManager mUiModeManager;

    private ListPreference mAdaptiveIconShape;
    private ListPreference mFontPicker;
    private ListPreference mStatusbarIcons;
    private ListPreference mThemeSwitch;
    private ListPreference mSwitchStyle;
    private Preference mAccentPicker;
    private Preference mBackupThemes;
    private Preference mNavbarPicker;
    private Preference mQSStylePicker;
    private Preference mRestoreThemes;
    private Preference mThemeSchedule;
    private CustomPreference mWpPreview;
    private ListPreference mBrightnessSliderStyle;
    private ListPreference mPanelBg;
    private ListPreference mQsShape;
    private ListPreference mRoundedUi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        addPreferencesFromResource(R.xml.themes);

        mContext = getActivity();
        PreferenceScreen prefSet = getPreferenceScreen();

        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setHasOptionsMenu(true);

        mThemeDatabase = new ThemeDatabase(mContext);

        // Shared preferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPrefListener);

        // Theme services
        mUiModeManager = getContext().getSystemService(UiModeManager.class);
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));

        // Accent summary
        mAccentName = getResources().getStringArray(R.array.accent_name);

        // Navbar summary
        mNavbarName = getResources().getStringArray(R.array.navbar_name);

        // QSStyle summary
        mQSStyleName = getResources().getStringArray(R.array.qsstyle_name);

        // Wallpaper preview
        mWpPreview = (CustomPreference) findPreference(PREF_WP_PREVIEW);

        // Theme schedule
        mThemeSchedule = (Preference) findPreference(PREF_THEME_SCHEDULE);
        assert mThemeSchedule != null;
        mThemeSchedule.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), Schedule.class);
                startActivity(intent);
                return true;
            }
        });

        // Accent picker
        mAccentPicker = (Preference) findPreference(PREF_THEME_ACCENT_PICKER);
        assert mAccentPicker != null;
        mAccentPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentManager manager = getFragmentManager();
                Fragment frag = manager.findFragmentByTag(AccentPicker.TAG_ACCENT_PICKER);
                if (frag != null) {
                    manager.beginTransaction().remove(frag).commit();
                }
                AccentPicker accentPickerFragment = new AccentPicker();
                accentPickerFragment.show(manager, AccentPicker.TAG_ACCENT_PICKER);
                return true;
            }
        });

        // Navbar picker
        mNavbarPicker = (Preference) findPreference(PREF_THEME_NAVBAR_PICKER);
        if (threeButtonNavbarEnabled(mContext)) {
            assert mNavbarPicker != null;
            mNavbarPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    FragmentManager manager = getFragmentManager();
                    Fragment frag = manager.findFragmentByTag(NavbarPicker.TAG_NAVBAR_PICKER);
                    if (frag != null) {
                        manager.beginTransaction().remove(frag).commit();
                    }
                    NavbarPicker navbarPickerFragment = new NavbarPicker();
                    navbarPickerFragment.show(manager, NavbarPicker.TAG_NAVBAR_PICKER);
                    return true;
                }
            });
        } else {
            prefSet.removePreference(mNavbarPicker);
        }

        // QSStyle picker
        mQSStylePicker = (Preference) findPreference(PREF_THEME_QSSTYLE_PICKER);
        assert mQSStylePicker != null;
        mQSStylePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentManager manager = getFragmentManager();
                Fragment frag = manager.findFragmentByTag(QSStylePicker.TAG_QSSTYLE_PICKER);
                if (frag != null) {
                    manager.beginTransaction().remove(frag).commit();
                }
                QSStylePicker qsStylePickerFragment = new QSStylePicker();
                qsStylePickerFragment.show(manager, QSStylePicker.TAG_QSSTYLE_PICKER);
                return true;
            }
        });

        // Themes backup
        mBackupThemes = (Preference) findPreference(PREF_BACKUP_THEMES);
        assert mBackupThemes != null;
        mBackupThemes.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (isLiveWallpaper(getActivity())) {
                    new AlertDialog.Builder(getActivity(), R.style.AccentDialogTheme)
                            .setTitle(getContext().getString(R.string.theme_backup_dialog_title))
                            .setMessage(getContext().getString(R.string.theme_backup_dialog_message))
                            .setCancelable(false)
                            .setPositiveButton(getContext().getString(android.R.string.ok),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            FragmentManager manager = getFragmentManager();
                                            Fragment frag = manager.findFragmentByTag(BackupThemes.TAG_BACKUP_THEMES);
                                            if (frag != null) {
                                                manager.beginTransaction().remove(frag).commit();
                                            }
                                            BackupThemes backupThemesFragment = new BackupThemes(Themes.this);
                                            backupThemesFragment.show(manager, BackupThemes.TAG_BACKUP_THEMES);
                                        }
                                    })
                            .setNegativeButton(getContext().getString(android.R.string.cancel),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }).show();

                } else {
                    FragmentManager manager = getFragmentManager();
                    Fragment frag = manager.findFragmentByTag(BackupThemes.TAG_BACKUP_THEMES);
                    if (frag != null) {
                        manager.beginTransaction().remove(frag).commit();
                    }
                    BackupThemes backupThemesFragment = new BackupThemes(Themes.this);
                    backupThemesFragment.show(manager, BackupThemes.TAG_BACKUP_THEMES);
                }
                return true;
            }
        });

        // Themes restore
        mRestoreThemes = (Preference) findPreference(PREF_RESTORE_THEMES);
        assert mRestoreThemes != null;
        mRestoreThemes.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(mContext, RestoreThemes.class);
                if (intent != null) {
                    setSharedPrefListener(true);
                    startActivity(intent);
                }
                return true;
            }
        });

        // Accent
        String accentName = getOverlayName(ThemesUtils.ACCENTS);
        if (accentName != null) {
            mSharedPreferences.edit().putString("theme_accent_color", accentName).apply();
        }

        // Navbar
        String navbarName = getOverlayName(ThemesUtils.NAVBAR_STYLES);
        if (navbarName != null) {
            mSharedPreferences.edit().putString("theme_navbar_style", navbarName).apply();
        }

        // QSStyles
        String qsStyleName = getOverlayName(ThemesUtils.QS_TILE_THEMES);
        if (qsStyleName != null) {
            mSharedPreferences.edit().putString("theme_qstile_style", qsStyleName).apply();
        }
        // Themes
        mThemeSwitch = (ListPreference) findPreference(PREF_THEME_SWITCH);
        if (Utils.isThemeEnabled("com.android.theme.transparentclear.system")) {
            mThemeSwitch.setValue("9");
        } else if (Utils.isThemeEnabled("com.android.theme.materialocean.system")) {
            mThemeSwitch.setValue("8");
        } else if (Utils.isThemeEnabled("com.android.theme.darkgrey.system")) {
            mThemeSwitch.setValue("7");
        } else if (Utils.isThemeEnabled("com.android.theme.bakedgreen.system")) {
            mThemeSwitch.setValue("6");
        } else if (Utils.isThemeEnabled("com.android.theme.chocox.system")) {
            mThemeSwitch.setValue("5");
        } else if (Utils.isThemeEnabled("com.android.theme.solarizeddark.system")) {
            mThemeSwitch.setValue("4");
        } else if (Utils.isThemeEnabled("com.android.theme.pitchblack.system")) {
            mThemeSwitch.setValue("3");
        } else if (mUiModeManager != null) {
            if (mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES) {
                mThemeSwitch.setValue("2");
            } else {
                mThemeSwitch.setValue("1");
            }
        }
        mThemeSwitch.setSummary(mThemeSwitch.getEntry());

        mPanelBg = (ListPreference) findPreference(PREF_PANEL_BG);
        int mPanelValue = getOverlayPosition(ThemesUtils.PANEL_BG_STYLE);
        if (mPanelValue != -1) {
            mPanelBg.setValue(String.valueOf(mPanelValue + 2));
        } else {
            mPanelBg.setValue("1");
        }
        mPanelBg.setSummary(mPanelBg.getEntry());

        mRoundedUi = (ListPreference) findPreference(PREF_ROUNDED_CORNER);
        int roundedValue = getOverlayPosition(ThemesUtils.UI_RADIUS);
        if (roundedValue != -1) {
            mRoundedUi.setValue(String.valueOf(roundedValue + 2));
        } else {
            mRoundedUi.setValue("1");
        }
        mRoundedUi.setSummary(mRoundedUi.getEntry());

        // Font picker
        mFontPicker = (ListPreference) findPreference(PREF_FONT_PICKER);
        int fontPickerValue = getOverlayPosition(ThemesUtils.FONTS);
        if (fontPickerValue != -1) {
            mFontPicker.setValue(String.valueOf(fontPickerValue + 2));
        } else {
            mFontPicker.setValue("1");
        }
        mFontPicker.setSummary(mFontPicker.getEntry());

        // Adaptive icon shape
        mAdaptiveIconShape = (ListPreference) findPreference(PREF_ADAPTIVE_ICON_SHAPE);
        int iconShapeValue = getOverlayPosition(ThemesUtils.ADAPTIVE_ICON_SHAPE);
        if (iconShapeValue != -1) {
            mAdaptiveIconShape.setValue(String.valueOf(iconShapeValue + 2));
        } else {
            mAdaptiveIconShape.setValue("1");
        }
        mAdaptiveIconShape.setSummary(mAdaptiveIconShape.getEntry());

        // Statusbar icons
        mStatusbarIcons = (ListPreference) findPreference(PREF_STATUSBAR_ICONS);
        int sbIconsValue = getOverlayPosition(ThemesUtils.STATUSBAR_ICONS);
        if (sbIconsValue != -1) {
            mStatusbarIcons.setValue(String.valueOf(sbIconsValue + 2));
        } else {
            mStatusbarIcons.setValue("1");
        }
        mStatusbarIcons.setSummary(mStatusbarIcons.getEntry());

        // Switch Style
        mSwitchStyle = (ListPreference) findPreference(PREF_SWITCH_STYLE);
        int switchStyleValue = getOverlayPosition(ThemesUtils.SWITCH_STYLE);
        if (switchStyleValue != -1) {
            mSwitchStyle.setValue(String.valueOf(switchStyleValue + 2));
        } else {
            mSwitchStyle.setValue("1");
        }
        mSwitchStyle.setSummary(mSwitchStyle.getEntry());

        setWallpaperPreview();
        updateAccentSummary();
        updateNavbarSummary();
        updateQSStyleSummary();
        updateThemeScheduleSummary();
        updateBackupPref();
        updateRestorePref();
    }

    private void setWallpaperPreview() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getActivity());
        Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        mWpPreview.setIcon(wallpaperDrawable);
    }

    private void updateBackupPref() {
        mBackupThemes.setEnabled(getThemeCount() < mBackupLimit ? true : false);
        if (getThemeCount() == mBackupLimit) {
            mBackupThemes.setSummary(R.string.theme_backup_reach_limit_summary);
        } else {
            mBackupThemes.setSummary(R.string.theme_backup_summary);
        }
    }

    private void updateRestorePref() {
        mRestoreThemes.setEnabled(getThemeCount() > 0 ? true : false);
        if (getThemeCount() == 0) {
            mRestoreThemes.setSummary(R.string.theme_restore_no_backup_summary);
        } else {
            mRestoreThemes.setSummary(R.string.theme_restore_summary);
        }
    }

    private int getThemeCount() {
        int count = mThemeDatabase.getThemeDbUtilsCount();
        return count;
    }

    private int getOverlayPosition(String[] overlays) {
        int position = -1;
        for (int i = 0; i < overlays.length; i++) {
            String overlay = overlays[i];
            if (Utils.isThemeEnabled(overlay)) {
                position = i;
            }
        }
        return position;
    }

    private String getOverlayName(String[] overlays) {
        String overlayName = null;
        for (int i = 0; i < overlays.length; i++) {
            String overlay = overlays[i];
            if (Utils.isThemeEnabled(overlay)) {
                overlayName = overlay;
            }
        }
        return overlayName;
    }

    public OnSharedPreferenceChangeListener mSharedPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
            class FontPicker extends AsyncTask<Void, Void, Void> {

                protected Void doInBackground(Void... param) {
                    return null;
                }

                protected void onPostExecute(Void param) {
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    String fontType = sharedPreferences.getString(PREF_FONT_PICKER, "1");
                    String overlayName = getOverlayName(ThemesUtils.FONTS);
                    int fontTypeValue = Integer.parseInt(fontType);
                    if (overlayName != null) {
                        handleOverlays(overlayName, false, mOverlayManager);
                    }
                    if (fontTypeValue > 1) {
                        handleOverlays(ThemesUtils.FONTS[fontTypeValue - 2],
                            true, mOverlayManager);
                    }
                    mFontPicker.setSummary(mFontPicker.getEntry());
                }
            }

            if (key.equals(PREF_THEME_ACCENT_COLOR)) {
                String accentColor = sharedPreferences.getString(PREF_THEME_ACCENT_COLOR, "default");
                String overlayName = getOverlayName(ThemesUtils.ACCENTS);
                if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                }
                if (accentColor != "default") {
                    handleOverlays(accentColor, true, mOverlayManager);
                }
                updateAccentSummary();
            }

            if (key.equals(PREF_THEME_NAVBAR_STYLE)) {
                String navbarStyle = sharedPreferences.getString(PREF_THEME_NAVBAR_STYLE, "default");
                String overlayName = getOverlayName(ThemesUtils.NAVBAR_STYLES);
                if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                }
                if (navbarStyle != "default") {
                    handleOverlays(navbarStyle, true, mOverlayManager);
                }
                updateNavbarSummary();
            }

            if (key.equals(PREF_THEME_QSTILE_STYLE)) {
                String qsStyle = sharedPreferences.getString(PREF_THEME_QSTILE_STYLE, "com.android.systemui.qstile.default");
                String overlayName = getOverlayName(ThemesUtils.QS_TILE_THEMES);
                if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                }
                if (qsStyle != "default") {
                    handleOverlays(qsStyle, true, mOverlayManager);
                }
                updateQSStyleSummary();
            }

            if (key.equals(PREF_FONT_PICKER)) {
                new FontPicker().execute();
            }

            if (key.equals(PREF_ADAPTIVE_ICON_SHAPE)) {
                String adapativeIconShape = sharedPreferences.getString(PREF_ADAPTIVE_ICON_SHAPE, "1");
                String overlayName = getOverlayName(ThemesUtils.ADAPTIVE_ICON_SHAPE);
                int adapativeIconShapeValue = Integer.parseInt(adapativeIconShape);
                if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                }
                if (adapativeIconShapeValue > 1) {
                    handleOverlays(ThemesUtils.ADAPTIVE_ICON_SHAPE[adapativeIconShapeValue - 2],
                        true, mOverlayManager);
                }
                mAdaptiveIconShape.setSummary(mAdaptiveIconShape.getEntry());
            }

            if (key.equals(PREF_STATUSBAR_ICONS)) {
                String statusbarIcons = sharedPreferences.getString(PREF_STATUSBAR_ICONS, "1");
                String overlayName = getOverlayName(ThemesUtils.STATUSBAR_ICONS);
                int statusbarIconsValue = Integer.parseInt(statusbarIcons);
                if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                }
                if (statusbarIconsValue > 1) {
                    handleOverlays(ThemesUtils.STATUSBAR_ICONS[statusbarIconsValue - 2],
                        true, mOverlayManager);
                }
                mStatusbarIcons.setSummary(mStatusbarIcons.getEntry());
            }

            if (key.equals(PREF_SWITCH_STYLE)) {
                String switchStyle = sharedPreferences.getString(PREF_SWITCH_STYLE, "1");
                String overlayName = getOverlayName(ThemesUtils.SWITCH_STYLE);
                int switchStyleValue = Integer.parseInt(switchStyle);
                if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                }
                if (switchStyleValue > 1) {
                    handleOverlays(ThemesUtils.SWITCH_STYLE[switchStyleValue - 2],
                            true, mOverlayManager);
                }
                mSwitchStyle.setSummary(mSwitchStyle.getEntry());
            }

            if (key.equals(PREF_ROUNDED_CORNER)) {
            String roundedStyle = sharedPreferences.getString(PREF_ROUNDED_CORNER, "1");
            int roundedValue = Integer.parseInt(rounded);
            String overlayName = getOverlayName(ThemesUtils.UI_RADIUS);
                if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                }
                if (roundedValue > 1) {
                    handleOverlays(ThemesUtils.UI_RADIUS[roundedValue -2],
                            true, mOverlayManager);
                }
            mRoundedUi.setSummary(mRoundedUi.getEntry());
            }

            if (key.equals(PREF_THEME_SWITCH)) {
                String themeSwitch = sharedPreferences.getString(PREF_THEME_SWITCH, "1");
                switch (themeSwitch) {
                    case "1":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_NO,
                                ThemesUtils.TRANSPARENT_CLEAR, mOverlayManager);
                        break;
                    case "2":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.TRANSPARENT_CLEAR, mOverlayManager);
                        break;
                    case "3":
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.TRANSPARENT_CLEAR, mOverlayManager);
                        break;
                    case "4":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.TRANSPARENT_CLEAR, mOverlayManager);
                        break;
                    case "5":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.TRANSPARENT_CLEAR, mOverlayManager);
                        break;
                    case "6":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.TRANSPARENT_CLEAR, mOverlayManager);
                        break;
                    case "7":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.TRANSPARENT_CLEAR, mOverlayManager);
                        break;
                    case "8":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.TRANSPARENT_CLEAR, mOverlayManager);
                        break;
                    case "9":
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.PITCH_BLACK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.SOLARIZED_DARK, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.CHOCO_X, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.BAKED_GREEN, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.DARK_GREY, mOverlayManager);
                        handleBackgrounds(false, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.MATERIAL_OCEAN, mOverlayManager);
                        handleBackgrounds(true, mContext, UiModeManager.MODE_NIGHT_YES,
                                ThemesUtils.TRANSPARENT_CLEAR, mOverlayManager);
                        break;
                }
                mThemeSwitch.setSummary(mThemeSwitch.getEntry());
            }

            if (key.equals(PREF_PANEL_BG)) {
                String panelBg = sharedPreferences.getString(PREF_PANEL_BG, "1");
                int panelBgValue = Integer.parseInt(panelBg);
                  String overlayName = getOverlayName(ThemesUtils.PANEL_BG_STYLE);
                if (overlayName != null) {
                    handleOverlays(overlayName, false, mOverlayManager);
                }
                if (panelBgValue > 1) {
                	Utils.showSystemUiRestartDialog(getContext());
                    handleOverlays(ThemesUtils.PANEL_BG_STYLE[panelBgValue -2],
                            true, mOverlayManager);
                }
            mPanelBg.setSummary(mPanelBg.getEntry());
           }
        }
    };

    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    public static void setSharedPrefListener(boolean listener) {
        mUseSharedPrefListener = listener;
    }

    @Override
    public void onCloseBackupDialog(DialogFragment dialog) {
        updateBackupPref();
        updateRestorePref();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPrefListener);
        setWallpaperPreview();
        updateBackupPref();
        updateRestorePref();
        updateThemeScheduleSummary();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mUseSharedPrefListener) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPrefListener);
        }
        updateThemeScheduleSummary();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!mUseSharedPrefListener) {
            mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPrefListener);
        }
        updateThemeScheduleSummary();
    }

    private void updateThemeScheduleSummary() {
        if (mThemeSchedule != null) {
            if (getThemeSchedule(mSharedPreferences).equals("1")) {
                mThemeSchedule.setSummary(mContext.getString(R.string.theme_schedule_summary));
            } else {
                if (!Calendar.getInstance().before(getScheduledStartThemeTime(mSharedPreferences))) {
                    mThemeSchedule.setSummary(getScheduledStartThemeSummary(mSharedPreferences, mContext)
                            + " " + mContext.getString(R.string.theme_schedule_dyn_summary));
                }
            }
        }
    }

    private void updateAccentSummary() {
        if (mAccentPicker != null) {
            int value = getOverlayPosition(ThemesUtils.ACCENTS);
            if (value != -1) {
                mAccentPicker.setSummary(mAccentName[value]);
            } else {
                mAccentPicker.setSummary(mContext.getString(R.string.theme_accent_picker_default));
            }
        }
    }

    private void updateNavbarSummary() {
        if (mNavbarPicker != null) {
            int value = getOverlayPosition(ThemesUtils.NAVBAR_STYLES);
            if (value != -1) {
                mNavbarPicker.setSummary(mNavbarName[value]);
            } else {
                mNavbarPicker.setSummary(R.string.theme_accent_picker_default);
            }
        }
    }

    private void updateQSStyleSummary() {
        if (mQSStylePicker != null) {
            int value = getOverlayPosition(ThemesUtils.QS_TILE_THEMES);
            if (value != -1) {
                mQSStylePicker.setSummary(mQSStyleName[value]);
            } else {
                mQSStylePicker.setSummary(R.string.theme_accent_picker_default);
            }
            // Update preview
            mWpPreview.setResources();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.themes_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            case R.id.themes_reset:
                resetThemes();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void resetThemes() {
        new AlertDialog.Builder(getActivity(), R.style.AccentDialogTheme)
                .setTitle(mContext.getString(R.string.theme_reset_dialog_title))
                .setMessage(mContext.getString(R.string.theme_reset_dialog_message))
                .setCancelable(false)
                .setPositiveButton(getContext().getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new resetThemes().execute();
                            }
                        })
                .setNegativeButton(getContext().getString(android.R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
    }

    class resetThemes extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... param) {
            mSharedPreferences.edit()
            // Accents
            .remove(PREF_THEME_ACCENT_COLOR)
            // NavBar
            .remove(PREF_THEME_NAVBAR_STYLE)
            // QSStyle
            .remove(PREF_THEME_QSTILE_STYLE)
            // Fonts
            .remove(PREF_FONT_PICKER)
            // Adapative icons
            .remove(PREF_ADAPTIVE_ICON_SHAPE)
            // Statusbar icons
            .remove(PREF_STATUSBAR_ICONS)
            // Themes
            .remove(PREF_THEME_SWITCH)
            // Switch
	    .remove(PREF_SWITCH_STYLE)
            .apply();

            return null;
        }

        protected void onPostExecute(Void param) {
            Toast.makeText(mContext, mContext.getString(R.string.theme_reset_toast), Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
    }
}

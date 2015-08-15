package thomas.jonathan.notey;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.webkit.WebView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.jenzz.materialpreference.Preference;

import de.psdev.licensesdialog.LicensesDialog;

public class About extends ActionBarActivity {
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeStuffBeforeSetContentView();
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    private void themeStuffBeforeSetContentView(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        MainActivity.themeColor = sharedPreferences.getString("pref_theme_color", "md_blue_500");
        MainActivity.darkTheme = sharedPreferences.getBoolean("pref_theme_dark", false);

        //set light/dark theme
        if(MainActivity.darkTheme) {
            super.setTheme(getResources().getIdentifier("MySettingsThemeDark_" + MainActivity.themeColor + "_Accent_" + MainActivity.accentColor, "style", getPackageName()));
        }
        else {
            super.setTheme(getResources().getIdentifier("MySettingsTheme_" + MainActivity.themeColor + "_Accent_" + MainActivity.accentColor, "style", getPackageName()));
        }
    }

    @Override //close the activity when pressing the back arrow
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.about);

            Preference openSourceLic = (Preference) findPreference("pref_open_source_licenses");
            Preference changelogPref = (Preference) findPreference("pref_changelog");
            Preference translationsPref = (Preference) findPreference("pref_translations");
            Preference githubPref = (Preference) findPreference("pref_github");
            Preference contactPref = (Preference) findPreference("pref_contact");
            Preference ratePref = (Preference) findPreference("pref_rate");
//            Preference proPref = (Preference) findPreference("pref_pro");

            //dialog pop-up for 'open sources' preference
            new AlertDialog.Builder(getActivity());
            openSourceLic.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    new LicensesDialog.Builder(getActivity())
                        .setNotices(R.raw.opensourcelicenses)
                        .setTitle(R.string.opensource)
                        .setCloseText(R.string.ok)
                        .build()
                        .show();
                    return false;
                }
            });

            //changelog dialog pop-up
            new AlertDialog.Builder(getActivity());
            changelogPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    MaterialDialog md = new MaterialDialog.Builder(getActivity())
                            .title(getResources().getString(R.string.changelog))
                            .customView(R.layout.webview_dialog_layout, false)
                            .positiveText(getResources().getString(R.string.ok))
                            .typeface(Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-LIGHT.TTF"))
                            .build();
                    WebView webView = (WebView) md.getCustomView().findViewById(R.id.pro_features_webview);
                    webView.loadUrl(MainActivity.darkTheme ? "file:///android_asset/NoteyChangelogDark.html" : "file:///android_asset/NoteyChangelog.html");
                    md.show();
                    return false;
                }
            });

            //translations dialog pop-up
            new AlertDialog.Builder(getActivity());
            translationsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    MaterialDialog md = new MaterialDialog.Builder(getActivity())
                            .title(getResources().getString(R.string.translations))
                            .customView(R.layout.webview_dialog_layout, false)
                            .positiveText(getResources().getString(R.string.ok))
                            .typeface(Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-LIGHT.TTF"))
                            .build();
                    WebView webView = (WebView) md.getCustomView().findViewById(R.id.pro_features_webview);
                    webView.loadUrl(MainActivity.darkTheme ? "file:///android_asset/TranslationsThankYouDark.html" : "file:///android_asset/TranslationsThankYou.html");
                    md.show();

                    return false;
                }
            });

            //'github' preference selection. send the user to my github
            githubPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    Uri uri = Uri.parse(getResources().getString(R.string.github_url));
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });

            //'Contact' preference selection. send an email intent, setting the subject and the recipient
            contactPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("message/rfc822");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{getResources().getString(R.string.email)});
                    i.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name));
                    try {
                        startActivity(Intent.createChooser(i, getResources().getString(R.string.sendemail)));
                    } catch (android.content.ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });

            //'Rate' - go to the play store listing
            ratePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    Uri uri = Uri.parse(getResources().getString(R.string.playstore_url));
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });

            //Pro - popup to show the pro features
//            new AlertDialog.Builder(getActivity());
//            proPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//                public boolean onPreferenceClick(android.preference.Preference preference) {
//                    MaterialDialog md = new MaterialDialog.Builder(getActivity())
//                            .title(getResources().getString(R.string.pro_features))
//                            .customView(R.layout.webview_dialog_layout, false)
//                            .positiveText(getResources().getString(R.string.ok))
//                            .typeface(Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getActivity().getAssets(), "ROBOTO-LIGHT.TTF"))
//                            .build();
//                    WebView webView = (WebView) md.getCustomView().findViewById(R.id.pro_features_webview);
//                    webView.loadUrl(MainActivity.darkTheme ? "file:///android_asset/ProFeaturesInfoDark.html" : "file:///android_asset/ProFeaturesInfo.html");
//                    md.show();
//
//                    return false;
//                }
//            });
//
//            if (MainActivity.proVersionEnabled) {
//                proPref.setSummary(getString(R.string.yes_a_pro));
//            } else {
//                proPref.setSummary(getString(R.string.not_a_pro));
//            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
    }
}

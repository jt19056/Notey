package thomas.jonathan.notey;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import de.psdev.licensesdialog.LicensesDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;
import uk.co.chrisjenx.calligraphy.CalligraphyTypefaceSpan;

public class About extends PreferenceActivity {
    private int numTaps = 0;
    private EditText hiddenEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeStuffBeforeSetContentView();
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about);
        //show action bar
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        //defualt font
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("assets/ROBOTO-REGULAR.ttf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );

        //set action bar font
        SpannableString s = new SpannableString(getString(R.string.about));
        s.setSpan(new CalligraphyTypefaceSpan(Typeface.createFromAsset(getAssets(), "ROBOTO-BOLD.ttf")), 0, s.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getActionBar().setTitle(s);

        Preference openSourceLic = findPreference("pref_open_source_licenses");
        Preference changelogPref = findPreference("pref_changelog");
        Preference translationsPref = findPreference("pref_translations");
        Preference githubPref = findPreference("pref_github");
        Preference contactPref = findPreference("pref_contact");
        Preference ratePref = findPreference("pref_rate");
        Preference proPref = findPreference("pref_pro");
        Preference verNumPref = findPreference("pref_ver_num");

        //dialog pop-up for 'open sources' preference
        new AlertDialog.Builder(this);
        openSourceLic.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                new LicensesDialog.Builder(About.this).setNotices(R.raw.opensourcelicenses).setTitle(R.string.opensource).setCloseText(R.string.ok).build().show();
                return false;
            }
        });

        //changelog dialog pop-up
        new AlertDialog.Builder(this);
        changelogPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog md = new MaterialDialog.Builder(About.this)
                        .title(getResources().getString(R.string.changelog))
                        .customView(R.layout.webview_dialog_layout, false)
                        .positiveText(getResources().getString(R.string.ok))
                        .typeface(Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getAssets(), "ROBOTO-LIGHT.TTF"))
                        .build();
                WebView webView = (WebView) md.getCustomView().findViewById(R.id.pro_features_webview);
                webView.loadUrl("file:///android_asset/NoteyChangelog.html");
                md.show();

                return false;
            }
        });

        //translations dialog pop-up
        new AlertDialog.Builder(this);
        translationsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                MaterialDialog md = new MaterialDialog.Builder(About.this)
                        .title(getResources().getString(R.string.translations))
                        .content(R.string.translations_thank_you)
                        .positiveText(getResources().getString(R.string.dismiss))
                        .typeface(Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"))
                        .build();
                md.show();

                return false;
            }
        });

        //'github' preference selection. send the user to my github
        githubPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
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
            public boolean onPreferenceClick(Preference preference) {
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
            public boolean onPreferenceClick(Preference preference) {
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

        if (MainActivity.proVersionEnabled) {
            proPref.setSummary(getString(R.string.yes_a_pro));
        } else {
            proPref.setSummary(getString(R.string.not_a_pro));
        }
    }

    private void themeStuffBeforeSetContentView(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        MainActivity.themeColor = sharedPreferences.getString("pref_theme_color", "md_blue_500");
        MainActivity.darkTheme = sharedPreferences.getBoolean("pref_theme_dark", false);

        //set light/dark theme
        if(MainActivity.darkTheme) {
            super.setTheme(getResources().getIdentifier("MySettingsThemeDark_" + MainActivity.themeColor, "style", getPackageName()));
        }
        else {
            super.setTheme(getResources().getIdentifier("MySettingsTheme_" + MainActivity.themeColor, "style", getPackageName()));
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

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}

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
        s.setSpan(new CalligraphyTypefaceSpan(Typeface.createFromAsset(getAssets(), "ROBOTO-LIGHT.TTF")), 0, s.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getActionBar().setTitle(s);

        Preference openSourceLic = findPreference("pref_open_source_licenses");
        Preference changelogPref = findPreference("pref_changelog");
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
            /* old dialog to show licenses.  keeping for now just in case i want to revert back to it*/
//                MaterialDialog md = new MaterialDialog.Builder(About.this)
//                        .title(getResources().getString(R.string.opensource))
//                        .customView(R.layout.webview_dialog_layout, false)
//                        .positiveText(getResources().getString(R.string.ok))
//                        .typeface(Typeface.createFromAsset(getAssets(), "ROBOTO-REGULAR.ttf"), Typeface.createFromAsset(getAssets(), "ROBOTO-LIGHT.TTF"))
//                        .build();
//                WebView webView = (WebView) md.getCustomView().findViewById(R.id.pro_features_webview);
//                webView.loadUrl("file:///android_asset/opensourcelicenses.html");
//                md.show();
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

        //'Contact' preference selection. send an email intent, setting the subject and the recipient
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

        // tap the version num x amount of times to activate the hidden password screen.  typing in the correct password will enable pro
        verNumPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if (!MainActivity.proVersionEnabled) //don't bother doing any of this if pro version is already enabled
                    numTaps++;
                if (numTaps == 15) {
                    MaterialDialog hiddenDialog = new MaterialDialog.Builder(About.this)
                            .title("")
                            .customView(R.layout.hidden_pro_upgrade_screen, true)
                            .positiveText(getString(R.string.ok))
                            .positiveColorRes(R.color.blue_500)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog hiddenDialog) {
                                    String s = hiddenEditText.getText().toString();
                                    if (s != null && s.equals("20mAlteSe02")) {
                                        Toast.makeText(getApplicationContext(), "Pro version enabled :)", Toast.LENGTH_SHORT).show();
                                        MainActivity.proVersionEnabled = true;
                                        SharedPreferences.Editor spe = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                                        spe.putBoolean("proVersionEnabled", MainActivity.proVersionEnabled);
                                        spe.apply();
                                        Log.d("AboutActivity", "Saved data: proVersionEnabled = " + String.valueOf(MainActivity.proVersionEnabled));

                                        MainActivity.setUpProGUI();
                                        MainActivity.justTurnedPro = true;
                                    }
                                }
                            })
                            .build();
                    hiddenEditText = (EditText) hiddenDialog.getCustomView().findViewById(R.id.hiddenEditText);

                    hiddenDialog.show();
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

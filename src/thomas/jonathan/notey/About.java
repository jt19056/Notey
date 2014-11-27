package thomas.jonathan.notey;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.Html;
import android.view.MenuItem;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class About extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.about);

        //create and add the action bar at the top
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        Preference openSourceLic = findPreference("pref_open_source_licenses");
        Preference changelogPref = findPreference("pref_changelog");
        Preference contactPref = findPreference("pref_contact");
        Preference ratePref = findPreference("pref_rate");

        //dialog pop-up for 'open sources' preference
        final AlertDialog.Builder openSourceLicDialog = new AlertDialog.Builder(this);
        openSourceLic.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                AssetManager am = getAssets();
                InputStream inputStream;
                try {
                    inputStream = am.open("OpenSourceLicenses.txt");
                    openSourceLicDialog
                            .setTitle(getResources().getString(R.string.opensource))
                            .setMessage(Html.fromHtml(readOpenSourceFile(inputStream))) //get the text file
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //no code, close dialog
                                }
                            });

                    AlertDialog dialog = openSourceLicDialog.create();
                    dialog.show();
                    Button b = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if(b != null)
                        b.setTextColor(getResources().getColor(R.color.material_blue));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return false;
            }
        });

        //changelog dialog pop-up
        final AlertDialog.Builder changelogDialog = new AlertDialog.Builder(this);
        changelogPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                AssetManager am = getAssets();
                InputStream inputStream;
                try {
                    inputStream = am.open("NoteyChangelog.txt");
                    changelogDialog
                            .setTitle(getResources().getString(R.string.changelog))
                            .setMessage(Html.fromHtml(readChangelogFile(inputStream)))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue with close
                                }
                            });

                    AlertDialog dialog = changelogDialog.create();
                    dialog.show();
                    Button b = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if(b != null)
                        b.setTextColor(getResources().getColor(R.color.material_blue));
                } catch (IOException e) {
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
                i.putExtra(Intent.EXTRA_EMAIL  , new String[]{getResources().getString(R.string.email)});
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
    }

    private String readOpenSourceFile(InputStream inputStream) throws IOException {
        StringBuilder buf=new StringBuilder();
        BufferedReader in= new BufferedReader(new InputStreamReader(inputStream));
        String str;

        //read the text file. bold the text after the bullet point
        while ((str=in.readLine()) != null) {
            if(str.contains("•")){
                String tempString = str;
                str = "<b>" + tempString + "</b>";
            }
            buf.append(str);
            buf.append("<br></br>");
        }

        in.close();
        return buf.toString();
    }

    private String readChangelogFile(InputStream inputStream) throws IOException {
        StringBuilder buf=new StringBuilder();
        BufferedReader in= new BufferedReader(new InputStreamReader(inputStream));
        String str;

        //read the text file. bold the version nums
        while ((str=in.readLine()) != null) {
            if(str.contains("v1") || str.contains("v2")){
                String tempString = str;
                str = "<b>" + tempString + "</b>";
            }
            buf.append(str);
            buf.append("<br></br>");
        }

        in.close();
        return buf.toString();
    }

    @Override //close the activity when pressing the back arrow
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

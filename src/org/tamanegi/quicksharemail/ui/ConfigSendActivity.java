package org.tamanegi.quicksharemail.ui;

import org.tamanegi.quicksharemail.R;
import org.tamanegi.quicksharemail.content.MessageDB;
import org.tamanegi.quicksharemail.content.SendSetting;
import org.tamanegi.quicksharemail.service.SenderService;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

public class ConfigSendActivity extends PreferenceActivity
{
    private static final String TAG = "QuickShareMail";

    private static final long MAX_TCP_PORT_NUMBER = 65535;

    private static final String GMAIL_SERVER = "smtp.gmail.com";
    private static final String GMAIL_PORT = "587";
    private static final String GMAIL_SEC = "starttls";

    private SendSetting setting;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_send);
        setting = new SendSetting(this);

        setupSummary();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // check remain count gt 0
        MessageDB message_db = new MessageDB(this);
        int rest_cnt = 0;
        try {
            rest_cnt = message_db.getRestCount();
        }
        finally{
            message_db.close();
        }

        if(setting.checkValid() && rest_cnt > 0) {
            // request start send, if settings are changed to valid
            startService(new Intent(SenderService.ACTION_ENQUEUE, null,
                                    getApplicationContext(),
                                    SenderService.class));
        }
    }

    private void setupSummary()
    {
        setupMailFromSummary("mail_from", R.string.summary_def_pref_mail_from);

        setupTextSummary("smtp_server", R.string.summary_def_pref_smtp_server);
        setupNumberSummary("smtp_port",
                           SendSetting.DEFAULT_SMTP_PORT,
                           MAX_TCP_PORT_NUMBER,
                           R.string.msg_pref_smtp_port);
        setupListSummary("smtp_sec", SendSetting.DEFAULT_SMTP_SEC);

        setupTextSummary("smtp_user", R.string.summary_def_pref_smtp_user);
    }

    private void setupTextSummary(String key, int def_id)
    {
        EditTextPreference preference =
            (EditTextPreference)findPreference(key);

        preference.setOnPreferenceChangeListener(
            new UpdateSummaryOnChangeText(def_id));

        String text = preference.getText();
        if(text == null || text.length() < 1) {
            preference.setSummary(def_id);
        }
        else {
            preference.setSummary(text);
        }
    }

    private void setupMailFromSummary(String key, int def_id)
    {
        EditTextPreference preference =
            (EditTextPreference)findPreference(key);

        preference.setOnPreferenceChangeListener(
            new UpdateSummaryOnChangeMailFrom(def_id));

        String text = preference.getText();
        if(text == null || text.length() < 1) {
            preference.setSummary(def_id);
        }
        else {
            preference.setSummary(text);
        }
    }

    private void setupNumberSummary(String key, long def_val, long max,
                                    int warn_id)
    {
        EditTextPreference preference = (EditTextPreference)findPreference(key);

        preference.setOnPreferenceChangeListener(
            new UpdateSummaryOnChangeNumber(max, warn_id));

        String text = preference.getText();
        long val;
        try {
            val = Long.parseLong(text);
            if(val < 1 || val > max) {
                Log.i(TAG, "setup: out of range: " + text);
                val = def_val;
            }
        }
        catch(NumberFormatException e) {
            Log.i(TAG, "setup: not number: " + text);
            val = def_val;
        }

        preference.setText(String.valueOf(val));
        preference.setSummary(String.valueOf(val));
    }

    private void setupListSummary(String key, String def_val)
    {
        ListPreference preference = (ListPreference)findPreference(key);

        preference.setOnPreferenceChangeListener(
            new UpdateSummaryOnChangeList());

        int idx = preference.findIndexOfValue(preference.getValue());
        if(idx < 0) {
            idx = preference.findIndexOfValue(def_val);
        }

        preference.setValue(preference.getEntryValues()[idx].toString());
        preference.setSummary(preference.getEntries()[idx]);
    }

    private void showGmailSetup(final String addr)
    {
        new AlertDialog.Builder(this)
            .setTitle(R.string.title_pref_smtp_ask_gmail)
            .setMessage(R.string.msg_pref_smtp_ask_gmail)
            .setPositiveButton(
                android.R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        dialog.dismiss();
                        setGmailDefault(addr);
                        ((EditTextRemPreference)
                         findPreference("smtp_pass")).performClick();
                    }
                })
            .setNegativeButton(
                android.R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        dialog.cancel();
                    }
                })
            .show();
    }

    private void setGmailDefault(String addr)
    {
        EditTextPreference smtp_server =
            (EditTextPreference)findPreference("smtp_server");
        smtp_server.setText(GMAIL_SERVER);
        smtp_server.setSummary(GMAIL_SERVER);

        EditTextPreference smtp_port =
            (EditTextPreference)findPreference("smtp_port");
        smtp_port.setText(GMAIL_PORT);
        smtp_port.setSummary(GMAIL_PORT);

        ListPreference smtp_sec = (ListPreference)findPreference("smtp_sec");
        int idx = smtp_sec.findIndexOfValue(GMAIL_SEC);
        smtp_sec.setValue(smtp_sec.getEntryValues()[idx].toString());
        smtp_sec.setSummary(smtp_sec.getEntries()[idx]);

        CheckBoxPreference smtp_auth =
            (CheckBoxPreference)findPreference("smtp_auth");
        smtp_auth.setChecked(true);

        EditTextPreference smtp_user =
            (EditTextPreference)findPreference("smtp_user");
        smtp_user.setText(addr);
        smtp_user.setSummary(addr);
    }

    private void showWarnMessage(int str_id)
    {
        Toast.makeText(this, str_id, Toast.LENGTH_LONG).show();
    }

    private class UpdateSummaryOnChangeText
        implements Preference.OnPreferenceChangeListener
    {
        private int def_id;

        private UpdateSummaryOnChangeText(int def_id)
        {
            this.def_id = def_id;
        }

        public boolean onPreferenceChange(Preference preference,
                                          Object newValue)
        {
            String str = newValue.toString();

            if(str.length() < 1) {
                preference.setSummary(def_id);
            }
            else {
                preference.setSummary(str);
            }
            return true;
        }
    }

    private class UpdateSummaryOnChangeMailFrom
        extends UpdateSummaryOnChangeText
    {
        private UpdateSummaryOnChangeMailFrom(int def_id)
        {
            super(def_id);
        }

        public boolean onPreferenceChange(Preference preference,
                                          Object newValue)
        {
            if(super.onPreferenceChange(preference, newValue)) {
                if(newValue != null &&
                   newValue.toString().indexOf("@gmail.com") >= 0) {
                    showGmailSetup(newValue.toString());
                }

                return true;
            }
            else {
                return false;
            }
        }
    }

    private class UpdateSummaryOnChangeNumber
        implements Preference.OnPreferenceChangeListener
    {
        private long max;
        private int warn_id;

        private UpdateSummaryOnChangeNumber(long max, int warn_id)
        {
            this.max = max;
            this.warn_id = warn_id;
        }

        public boolean onPreferenceChange(Preference preference,
                                          Object newValue)
        {
            long val;

            try {
                val = Long.parseLong(newValue.toString());
                if(val < 1 || val > max) {
                    Log.w(TAG, "pref: out of range: " + newValue);
                    showWarnMessage(warn_id);
                    return false;
                }
            }
            catch(NumberFormatException e) {
                Log.w(TAG, "pref: not number: " + newValue);
                showWarnMessage(warn_id);
                return false;
            }

            ((EditTextPreference)preference).setText(String.valueOf(val));
            preference.setSummary(String.valueOf(val));
            return false;
        }
    }

    private class UpdateSummaryOnChangeList
        implements Preference.OnPreferenceChangeListener
    {
        public boolean onPreferenceChange(Preference preference,
                                          Object newValue)
        {
            ListPreference pref = (ListPreference)preference;
            int idx = pref.findIndexOfValue(newValue.toString());
            pref.setSummary(pref.getEntries()[idx]);
            return true;
        }
    }
}

package org.tamanegi.quicksharemail.ui;

import org.tamanegi.quicksharemail.R;
import org.tamanegi.quicksharemail.content.SendToContent;
import org.tamanegi.quicksharemail.content.SendToDB;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ConfigSendToDetailActivity extends ListActivity
{
    public static String EXTRA_INFO_ID = "info_id";

    private SendToDB sendto_db;

    private long info_id;
    private SendToContent detailinfo = null;
    private AddressAdapter adapter;

    private LayoutInflater inflater;
    private CheckBox enable_check;
    private TextView label_summary;
    private TextView type_summary;
    private TextView subject_summary;
    private TextView priority_summary;
    private CheckBox alternate_check;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        info_id = getIntent().getLongExtra(EXTRA_INFO_ID, -1);
        sendto_db = new SendToDB(this);

        // setup ListView
        ListView list = getListView();
        list.setItemsCanFocus(true);

        inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);

        // headers
        enable_check =
            setupCheckItemHeader(R.string.title_pref_send_to_detail_enable,
                                 R.string.summary_pref_send_to_detail_enable,
                                 new EnableOnClickListener());
        label_summary =
            setupTextItemHeader(R.string.title_pref_send_to_detail_label,
                                new LabelOnClickListener());
        type_summary =
            setupTextItemHeader(R.string.title_pref_send_to_detail_type,
                                new TypeOnClickListener());
        subject_summary =
            setupTextItemHeader(R.string.title_pref_send_to_detail_subject,
                                new SubjectOnClickListener());
        priority_summary =
            setupTextItemHeader(R.string.title_pref_send_to_detail_priority,
                                new PriorityOnClickListener());
        alternate_check =
            setupCheckItemHeader(R.string.title_pref_send_to_detail_alternate,
                                 R.string.summary_pref_send_to_detail_alternate,
                                 new AlternateOnClickListener());

        // address separator
        setupTextViewHeader(R.string.title_pref_send_to_detail_address);

        // add-new footer
        setupTextViewFooter(R.string.title_pref_send_to_detail_add,
                            new AddOnClickListener());

        // adapter
        adapter = new AddressAdapter();
        setListAdapter(adapter);

        // for context menu
        registerForContextMenu(list);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(info_id < 0) {
            // for add-new
            showEditText(
                null,
                R.string.title_pref_send_to_detail_label,
                R.layout.dialog_edittext,
                new OnTextEditedListener() {
                    public void onTextEdited(CharSequence text) {
                        if(text.length() < 1) {
                            showWarnToast(
                                R.string.msg_send_to_detail_label_zero_len);
                            finish();
                            return;
                        }

                        detailinfo = new SendToContent();
                        detailinfo.setLabel(text.toString());
                        detailinfo.setSubjectFormat(
                            getString(R.string.initial_subject_format));
                        detailinfo.setAllowType("*/*");
                        detailinfo.setPriority(50);
                        detailinfo.setEnable(false);
                        detailinfo.setAlternate(false);
                        detailinfo.setAddress(new String[0]);

                        info_id = sendto_db.addSendinfo(detailinfo);

                        updateData();
                    }
                },
                new OnTextEditedListener() {
                    public void onTextEdited(CharSequence text) {
                        finish();
                    }
                });
        }
        else {
            // update
            updateData();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        sendto_db.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.config_send_to_detail_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId()) {
        case R.id.menu_add:
            // add new
            showEditAddress(-1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuinfo)
    {
        AdapterView.AdapterContextMenuInfo minfo =
            (AdapterView.AdapterContextMenuInfo)menuinfo;
        getMenuInflater().inflate(R.menu.config_send_to_detail_context, menu);
        menu.setHeaderTitle(detailinfo.getAddress((int)minfo.id));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo minfo =
            (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

        switch(item.getItemId()) {
        case R.id.menu_edit:
            // start edit
            showEditAddress((int)minfo.id);
            return true;

        case R.id.menu_delete:
            // confirm to delete
            showDeleteConfirm((int)minfo.id);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    private CheckBox setupCheckItemHeader(int title_id, int summary_id,
                                          View.OnClickListener listener)
    {
        View view = inflater.inflate(R.layout.list_checkitem, null);
        view.setOnClickListener(listener);

        ((TextView)view.findViewById(R.id.list_item_title)).setText(title_id);
        ((TextView)
         view.findViewById(R.id.list_item_summary)).setText(summary_id);
        CheckBox check = (CheckBox)view.findViewById(R.id.list_item_checkbox);

        getListView().addHeaderView(view, null, true);

        return check;
    }

    private TextView setupTextItemHeader(int title_id,
                                         View.OnClickListener listener)
    {
        View view = inflater.inflate(R.layout.list_textitem, null);
        view.setOnClickListener(listener);

        ((TextView)view.findViewById(R.id.list_item_title)).setText(title_id);
        TextView summary = (TextView)view.findViewById(R.id.list_item_summary);

        getListView().addHeaderView(view, null, true);

        return summary;
    }

    private void setupTextViewHeader(int title_id)
    {
        TextView view =
            (TextView)inflater.inflate(R.layout.list_separator, null);
        view.setText(title_id);

        getListView().addHeaderView(view, null, false);
    }

    private void setupTextViewFooter(int title_id,
                                     View.OnClickListener listener)
    {
        TextView view = (TextView)inflater.inflate(R.layout.list_additem, null);
        view.setOnClickListener(listener);
        view.setText(title_id);

        getListView().addFooterView(view, null, true);
    }

    private void updateData()
    {
        // update content data
        retrieveData();

        // update headers
        enable_check.setChecked(detailinfo.isEnable());
        label_summary.setText(detailinfo.getLabel());
        type_summary.setText(detailinfo.getAllowType());
        subject_summary.setText(detailinfo.getSubjectFormat());
        priority_summary.setText(String.valueOf(detailinfo.getPriority()));
        alternate_check.setChecked(detailinfo.isAlternate());

        // update list
        adapter.notifyDataSetChanged();
    }

    private void retrieveData()
    {
        detailinfo = sendto_db.getSendinfoById(info_id);
    }

    private void showEditText(CharSequence init_text,
                              int title_id,
                              int view_id,
                              final OnTextEditedListener on_ok,
                              final OnTextEditedListener on_cancel)
    {
        View view = inflater.inflate(view_id, null);
        final TextView text_view =
            (TextView)view.findViewById(R.id.dialog_edittext);

        if(init_text != null) {
            text_view.setText(init_text);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(title_id)
            .setView(view)
            .setPositiveButton(
                android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        dialog.dismiss();
                        if(on_ok != null) {
                            on_ok.onTextEdited(text_view.getText());
                        }
                    }
                })
            .setNegativeButton(
                android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        dialog.dismiss();
                        if(on_cancel != null) {
                            on_cancel.onTextEdited(text_view.getText());
                        }
                    }
                })
            .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                })
            .create();

        dialog.show();
    }

    private void showWarnToast(int str_id)
    {
        Toast.makeText(this, str_id, Toast.LENGTH_LONG).show();
    }

    private void showWarnMessage(int msg_id)
    {
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.title_pref_send_to)
            .setMessage(msg_id)
            .setPositiveButton(
                android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        dialog.dismiss();
                    }
                })
            .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                })
            .show();
    }

    private void showEditAddress(final int position)
    {
        showEditText(
            (position < 0 ? null : detailinfo.getAddress(position)),
            (position < 0 ?
             R.string.title_pref_send_to_detail_add :
             R.string.title_pref_send_to_detail_edit),
            R.layout.dialog_editaddress,
            new OnTextEditedListener() {
                public void onTextEdited(CharSequence text) {
                    if(text.length() < 1) {
                        showWarnToast(
                            R.string.msg_send_to_detail_address_zero_len);
                        return;
                    }

                    if(position < 0) {
                        // add address
                        int cnt = detailinfo.getAddressCount();
                        String[] addrs = new String[cnt + 1];

                        for(int i = 0; i < cnt; i++) {
                            addrs[i] = detailinfo.getAddress(i);
                        }
                        addrs[cnt] = text.toString();

                        detailinfo.setAddress(addrs);
                    }
                    else {
                        // update address
                        detailinfo.setAddress(position, text.toString());
                    }

                    sendto_db.updateSendinfo(detailinfo);
                    updateData();
                }
            },
            null);
    }

    private void showDeleteConfirm(final int position)
    {
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.title_pref_send_to_detail_delete)
            .setMessage(
                String.format(getString(R.string.msg_send_to_detail_delete),
                              detailinfo.getAddress(position)))
            .setPositiveButton(
                android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        dialog.dismiss();

                        // delete address
                        int cnt = detailinfo.getAddressCount();
                        String[] addrs = new String[cnt - 1];

                        for(int i = 0; i < position; i++) {
                            addrs[i] = detailinfo.getAddress(i);
                        }
                        for(int i = position + 1; i < cnt; i++) {
                            addrs[i - 1] = detailinfo.getAddress(i);
                        }

                        if(cnt == 1) {
                            detailinfo.setEnable(false);
                        }

                        detailinfo.setAddress(addrs);
                        sendto_db.updateSendinfo(detailinfo);
                        updateData();
                    }
                })
            .setNegativeButton(
                android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        dialog.dismiss();
                    }
                })
            .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                })
            .show();
    }

    private class EnableOnClickListener implements View.OnClickListener
    {
        public void onClick(View v)
        {
            boolean val = (! enable_check.isChecked());

            if(detailinfo.getAddressCount() < 1 && val) {
                showWarnMessage(R.string.msg_send_to_no_address);
                return;
            }

            // invert check
            enable_check.setChecked(val);

            detailinfo.setEnable(val);
            sendto_db.updateSendinfo(detailinfo);
            updateData();
        }
    }

    private class LabelOnClickListener
        implements View.OnClickListener, OnTextEditedListener
    {
        public void onClick(View v)
        {
            showEditText(detailinfo.getLabel(),
                         R.string.title_pref_send_to_detail_label,
                         R.layout.dialog_edittext,
                         this, null);
        }

        public void onTextEdited(CharSequence text)
        {
            if(text.length() < 1) {
                showWarnToast(R.string.msg_send_to_detail_label_zero_len);
                return;
            }

            detailinfo.setLabel(text.toString());
            sendto_db.updateSendinfo(detailinfo);
            updateData();
        }
    }

    private class TypeOnClickListener
        implements View.OnClickListener, OnTextEditedListener
    {
        public void onClick(View v)
        {
            showEditText(detailinfo.getAllowType(),
                         R.string.title_pref_send_to_detail_type,
                         R.layout.dialog_edittext,
                         this, null);
        }

        public void onTextEdited(CharSequence text)
        {
            detailinfo.setAllowType(text.toString());
            sendto_db.updateSendinfo(detailinfo);
            updateData();
        }
    }

    private class SubjectOnClickListener
        implements View.OnClickListener, OnTextEditedListener
    {
        public void onClick(View v)
        {
            // todo: show format template?
            showEditText(detailinfo.getSubjectFormat(),
                         R.string.title_pref_send_to_detail_subject,
                         R.layout.dialog_edittext,
                         this, null);
        }

        public void onTextEdited(CharSequence text)
        {
            detailinfo.setSubjectFormat(text.toString());
            sendto_db.updateSendinfo(detailinfo);
            updateData();
        }
    }

    private class PriorityOnClickListener
        implements View.OnClickListener, OnTextEditedListener
    {
        public void onClick(View v)
        {
            showEditText(String.valueOf(detailinfo.getPriority()),
                         R.string.title_pref_send_to_detail_priority,
                         R.layout.dialog_editnumber,
                         this, null);
        }

        public void onTextEdited(CharSequence text)
        {
            int val;
            try {
                val = Integer.parseInt(text.toString());
            }
            catch(NumberFormatException e) {
                showWarnToast(R.string.msg_send_to_detail_prority_invalid);
                return;
            }

            detailinfo.setPriority(val);
            sendto_db.updateSendinfo(detailinfo);
            updateData();
        }
    }

    private class AlternateOnClickListener implements View.OnClickListener
    {
        public void onClick(View v)
        {
            boolean val = (! alternate_check.isChecked());

            // invert check
            alternate_check.setChecked(val);

            detailinfo.setAlternate(val);
            sendto_db.updateSendinfo(detailinfo);
            updateData();
        }
    }

    private class AddOnClickListener implements View.OnClickListener
    {
        public void onClick(View v)
        {
            showEditAddress(-1);
        }
    }

    private class AddressItemOnClickListener
        implements View.OnClickListener, View.OnLongClickListener
    {
        private int position;

        private AddressItemOnClickListener(int position)
        {
            this.position = position;
        }

        public void onClick(View v)
        {
            // start edit
            showEditAddress(position);
        }

        public boolean onLongClick(View v)
        {
            // show context menu
            v.showContextMenu();

            return true;
        }
    }

    private class AddressAdapter extends BaseAdapter
    {
        public int getCount()
        {
            return (detailinfo != null ? detailinfo.getAddressCount() : 0);
        }

        public Object getItem(int position)
        {
            return detailinfo.getAddress(position);
        }

        public long getItemId(int position)
        {
            return position;
        }

        public View getView(int position, View conv_view, ViewGroup parent)
        {
            View view;
            if(conv_view == null) {
                view = inflater.inflate(R.layout.list_textitem, parent, false);
            }
            else {
                view = conv_view;
            }

            AddressItemOnClickListener listener =
                new AddressItemOnClickListener(position);
            view.setOnClickListener(listener);
            view.setOnLongClickListener(listener);

            ((TextView)view.findViewById(R.id.list_item_title)).
                setText(detailinfo.getAddress(position));
            view.findViewById(R.id.list_item_summary).setVisibility(View.GONE);

            return view;
        }
    }

    private interface OnTextEditedListener
    {
        public void onTextEdited(CharSequence text);
    }
}

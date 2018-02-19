package org.wordpress.android.ui.reader;

import android.Manifest;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.reader.adapters.ReaderUserAdapter;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.WPPermissionUtils;

import java.util.ArrayList;
import java.util.List;

public class ReaderContactsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private RecyclerView mRecycler;
    private ReaderUserAdapter mAdapter;
    private static final String[] permissionList = { Manifest.permission.READ_CONTACTS };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_contacts);
        mRecycler = findViewById(R.id.recycler);
        mRecycler.setAdapter(getAdapter());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        boolean hasPermission = PermissionUtils.checkPermissions(this, permissionList);
        if (hasPermission) {
            loadUsers();
        } else {
            showSoftAskView(true);
        }
    }

    private ReaderUserAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new ReaderUserAdapter(this);
        }
        return mAdapter;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSoftAskView(boolean show) {
        View softAskContainer = findViewById(R.id.container_soft_ask);
        if (!show) {
            if (softAskContainer.getVisibility() == View.VISIBLE) {
                AniUtils.fadeOut(softAskContainer, AniUtils.Duration.MEDIUM);
            }
            return;
        }

        final boolean isAlwaysDenied = WPPermissionUtils.isPermissionAlwaysDenied(
                this, Manifest.permission.READ_CONTACTS);

        TextView txtLabel = findViewById(R.id.text_soft_ask_label);
        String appName = "<strong>" + getString(R.string.app_name) + "</strong>";
        String label;
        if (isAlwaysDenied) {
            String permissionName = "<strong>"
                    + WPPermissionUtils.getPermissionName(this, Manifest.permission.READ_CONTACTS)
                    + "</strong>";
            label = String.format(
                    getString(R.string.reader_label_contacts_soft_ask_denied), appName, permissionName);
        } else {
            label = String.format(getString(R.string.reader_label_contacts_soft_ask), appName);
        }
        txtLabel.setText(Html.fromHtml(label));

        TextView txtAllow = softAskContainer.findViewById(R.id.text_soft_ask_allow);
        int allowId = isAlwaysDenied ?
                R.string.button_edit_permissions : R.string.photo_picker_soft_ask_allow;
        txtAllow.setText(allowId);
        txtAllow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAlwaysDenied) {
                    WPPermissionUtils.showAppSettings(ReaderContactsActivity.this);
                } else {
                    requestPermission();
                }
            }
        });

        softAskContainer.setVisibility(View.VISIBLE);
    }

    private void requestPermission() {
        requestPermissions(permissionList, WPPermissionUtils.CONTACTS_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        boolean allGranted = WPPermissionUtils.setPermissionListAsked(
                this, requestCode, permissions, grantResults, true);
        if (allGranted && requestCode == WPPermissionUtils.CONTACTS_PERMISSION_REQUEST_CODE) {
            showSoftAskView(false);
            loadUsers();
        }
    }

    private void loadUsers() {
        List<String> emailList = getContactEmails();
        ReaderUserList userList = new ReaderUserList();
        for (String email: emailList) {
            ReaderUser user = new ReaderUser();
            user.setUserName(email);
            userList.add(user);
        }
        getAdapter().setUsers(userList);
    }

    /*
     * returns a list of email addresses from the device's address book
     */
    private List<String> getContactEmails() {
        ArrayList<String> emailList = new ArrayList<>();
        try {
            ContentResolver cr = getContentResolver();
            String[] PROJECTION = new String[] {
                    ContactsContract.RawContacts._ID,
                    ContactsContract.CommonDataKinds.Email.DATA};
            String filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''";
            Cursor cur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, PROJECTION, filter, null, null);
            if (cur != null && cur.moveToFirst()) {
                do {
                    // TODO: should we ensure unique emails?
                    String email = cur.getString(1);
                    emailList.add(email);
                } while (cur.moveToNext());
                cur.close();
            }
        } catch (SecurityException e) {
            // no permission
            AppLog.e(AppLog.T.UTILS, e);
        }
        return emailList;
    }
}

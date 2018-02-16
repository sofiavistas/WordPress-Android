package org.wordpress.android.ui.contacts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

public class ContactUtils {

    /*
     * returns a list of email addreses from the device's address book - note that the caller
     * should first ensure the app has permission to access contacts
     */
    public static List<String> getContactEmails(@NonNull Context context) {
        ArrayList<String> emails = new ArrayList<String>();
        ContentResolver cr = context.getContentResolver();
        try {
            Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (cur.getCount() > 0) {
                while (cur.moveToNext()) {
                    String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                    Cursor cur1 = cr.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (cur1.moveToNext()) {
                        String email = cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                        if (!TextUtils.isEmpty(email)) {
                            emails.add(email);
                        }
                    }
                    cur1.close();
                }
            }
        } catch (SecurityException e) {
            // no permission
            AppLog.e(AppLog.T.UTILS, e);
        }
        return emails;
    }
}

package org.wordpress.android.ui.contacts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;

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
        try {
            ContentResolver cr = context.getContentResolver();
            String[] PROJECTION = new String[] {
                    ContactsContract.RawContacts._ID,
                    ContactsContract.CommonDataKinds.Email.DATA};
            String filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''";
            Cursor cur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, PROJECTION, filter, null, null);
            if (cur.moveToFirst()) {
                do {
                    String email = cur.getString(1);
                    emails.add(email);
                } while (cur.moveToNext());
            }

            cur.close();
        } catch (SecurityException e) {
            // no permission
            AppLog.e(AppLog.T.UTILS, e);
        }
        return emails;
    }
}

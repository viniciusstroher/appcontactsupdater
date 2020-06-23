package br.rcx.updatercontacts.services;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;

public class ContactService {

    //falta adicionar nos contatos
    public static void addContact(ContentResolver ctx, String phoneNumber,String groupId,boolean replace) throws OperationApplicationException, RemoteException {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        int rawContactID = ops.size();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, phoneNumber.replace(" ",""))
                .build());

        String phoneReplace = phoneNumber;
        if(replace){
            phoneReplace = phoneNumber.replace("-","");
        }
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneReplace)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        //String groupId = "6"; //my contacts group
        //grupo
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                .withValue(ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE,
                        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
                        groupId).build());

        ctx.applyBatch(ContactsContract.AUTHORITY, ops);

    }

    public static String getContactDisplayNameByNumber(ContentResolver ctx, String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number.replace(" ","")));
        String name = null;

        ContentResolver contentResolver = ctx;
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }

    public static String getContactIdByNumber(ContentResolver ctx, String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number.replace(" ","")));
        String name = null;

        ContentResolver contentResolver = ctx;
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.CONTACT_ID }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.CONTACT_ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }

    public static JSONArray getPhoneNumbers(ContentResolver ctx,String contactId) throws JSONException {
        JSONArray r = new JSONArray();
        ContentResolver cr = ctx;
        ArrayList<String> phoneNumber = new ArrayList<String>();
        // IF HAS NUMBER THEN GET ALL
        Cursor pCur = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{contactId}, null);

        String number = null;
        int x = 0;
        while (pCur.moveToNext()){
            number = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            r.put(x,number);
            x++;
        }
        pCur.close();
        return r;
    }

    public static String hasWhatsapp(ContentResolver ctx,String contactID) {
        String rowContactId = null;
        boolean hasWhatsApp;

        String[] projection = new String[]{ContactsContract.RawContacts._ID};
        String selection = ContactsContract.Data.CONTACT_ID + " = ? AND account_type IN (?,?)";
        String[] selectionArgs = new String[]{contactID, "com.whatsapp","com.whatsapp.w4b"};
        Cursor cursor = ctx.query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, selectionArgs, null);
        if (cursor != null) {
            hasWhatsApp = cursor.moveToNext();
            if (hasWhatsApp) {
                rowContactId = cursor.getString(0);
            }
            cursor.close();
        }
        return rowContactId;
    }

    public static long getGroupIdFor(ContentResolver ctx,Long contactId){
        Uri uri = ContactsContract.Data.CONTENT_URI;
        String where = String.format(
                "%s = ? AND %s = ?",
                ContactsContract.RawContacts.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID);

        String[] whereParams = new String[] {
                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                Long.toString(contactId),
        };

        String[] selectColumns = new String[]{
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
        };

        Cursor groupIdCursor = ctx.query(
                uri,
                selectColumns,
                where,
                whereParams,
                null);
        try{
            if (groupIdCursor.moveToFirst()) {
                return groupIdCursor.getLong(0);
            }
            return Long.MIN_VALUE; // Has no group ...
        }finally{
            groupIdCursor.close();
        }
    }

    public static void deleteContactById(ContentResolver ctx,String localContactId)
    {
        ContentResolver cr = ctx;
        String rawWhere = ContactsContract.Contacts._ID + " = ? ";
        String[] whereArgs1 = new String[]{localContactId};
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, rawWhere, whereArgs1, null);

        if(cur != null && cur.getCount() > 0) {
            while (cur.moveToNext()) {
                try{
                    String lookupKey = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
                    cr.delete(uri, null, null);

                }
                catch(Exception e)
                {
                    System.out.println(e.getStackTrace());
                }
            }
        }

        if(cur != null){
            cur.close();
        }
    }
}

package ch.epfl.ndubois.whoisit;

import android.accounts.Account;
import android.app.DownloadManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.StringBuilderPrinter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by NicDub on 25.02.2017.
 */

public class RetrieveContactInfoTask extends AsyncTask<String, Void, String> {

    private Context _ctx;
    private Account _account = new Account("WhoIsIt", "ch.epfl.ndubois.whoisit.account");


    public RetrieveContactInfoTask(Context ctx)
    {
        _ctx = ctx;
    }

    private String downloadUrl(String urlPath)
    {
        StringBuilder out = new StringBuilder();
        try {
            URL url = new URL(urlPath);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null)
                out.append(inputLine);

            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toString();
    }

    class Contact
    {
        public String Firstname;
        public String Lastname;
        public List<String> Units;
    }

    protected String doInBackground(String... phoneNumbers)
    {

        SharedPreferences sharedPref = _ctx.getSharedPreferences(_ctx.getPackageName() + "_prefs", Context.MODE_PRIVATE);
        String apiUrl = sharedPref.getString(_ctx.getString(R.string.pref_apiUrl), null);

        if (apiUrl == null) return null;

        String jsonStr = downloadUrl(apiUrl + "?p=" + phoneNumbers[0].substring(1));

        ArrayList<Contact> contacts = new ArrayList<>();

        try {
            JSONArray jsonContacts = new JSONArray(jsonStr);
            // looping through All Contacts
            for (int i = 0; i < jsonContacts.length(); i++) {
                JSONObject c = jsonContacts.getJSONObject(i);
                Contact contact = new Contact();
                contact.Firstname = c.getString("Firstname");
                contact.Lastname = c.getString("Lastname");

                contact.Units = new ArrayList<>();
                JSONArray jsonUnits = c.getJSONArray("Units");
                for (int j = 0; j < jsonUnits.length(); j++) {
                    contact.Units.add(jsonUnits.getString(j));
                }
                contacts.add(contact);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Exit if no contact found
        if (contacts.size() == 0) return null;

        // Actually use only the first contact
        Contact contact = contacts.get(0);

        String s = findContact(phoneNumbers[0]);
        if (s != null) {
            updateContact(s, contact, phoneNumbers[0]);
        }
        else {
            addContact(phoneNumbers[0], contact);
        }
        return contactGetDisplayName(contacts) + " " + contactGetCompany(contact);
    }

    private String contactGetCompany(Contact contact)
    {
        return TextUtils.join(", ", contact.Units);
    }

    private String contactGetDisplayName(ArrayList<Contact> contacts)
    {
        ArrayList<String> names = new ArrayList<>();
        for (Iterator<Contact> i = contacts.iterator(); i.hasNext(); ) {
            Contact contact = i.next();
            names.add(contact.Firstname + " " + contact.Lastname);
        }
        return TextUtils.join(", ", names);
    }

    protected void onPostExecute(String contactInfo) {
        Toast.makeText(_ctx, contactInfo, Toast.LENGTH_LONG).show();
        Toast.makeText(_ctx, contactInfo, Toast.LENGTH_LONG).show();
   }


    private void addContact(String number, Contact contact)
    {
        String company = contactGetCompany(contact);

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ops.add(ContentProviderOperation.newInsert(
                ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, "ch.epfl.ndubois.whoisit.account")
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, "WhoIsIt")
                .build());

            //------------------------------------------------------ Names
            ops.add(ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.Lastname)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.Firstname)
                    .build());

        //------------------------------------------------------ Work Numbers
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
                .build());


        //------------------------------------------------------ Organization
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                .build());

        // Asking the Contact provider to create a new contact
        try {
            _ctx.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(_ctx, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public String findContact(String number)
    {
        ContentResolver contentResolver = _ctx.getContentResolver();
        Uri uri = ContactsContract.Data.CONTENT_URI;
        String[] projection = new String[]
                {
                        ContactsContract.Data.CONTACT_ID
                };
        String selection = ContactsContract.Data.MIMETYPE + " = ? AND " +
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = ? AND " +
                ContactsContract.RawContacts.ACCOUNT_TYPE + " = ?  AND " +// "ch.epfl.ndubois.whoisit.account")
                ContactsContract.RawContacts.ACCOUNT_NAME + " = ? "; // "WhoIsIt")


        String[] selectionArguments = {
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                number,
                _account.type,
                _account.name
        };

        try {
            Cursor cursor = contentResolver.query(uri, projection, selection, selectionArguments, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    return cursor.getString(0);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }


    public void updateContact(String contactId, Contact contact, String number)
    {
        String company = contactGetCompany(contact);
        try
        {
            ContentResolver contentResolver = _ctx.getContentResolver();
            String selection =
                    ContactsContract.Data.CONTACT_ID + " = ? AND " +
                    ContactsContract.Data.MIMETYPE + " = ?";

            String[] selectionArguments = {
                    contactId,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE};

            String[] selectionArgumentsCompany = {
                    contactId,
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE};


            ArrayList<android.content.ContentProviderOperation> ops = new ArrayList<>();

            ops.add(android.content.ContentProviderOperation.newUpdate(android.provider.ContactsContract.Data.CONTENT_URI)
                    .withSelection(selection, selectionArguments)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.Lastname)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.Firstname)
                    .build());

            ops.add(android.content.ContentProviderOperation.newUpdate(android.provider.ContactsContract.Data.CONTENT_URI)
                    .withSelection(selection, selectionArgumentsCompany)
                    .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                    .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                    .build());

            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
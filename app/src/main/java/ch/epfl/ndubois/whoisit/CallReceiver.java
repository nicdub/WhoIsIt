package ch.epfl.ndubois.whoisit;

/**
 * Created by NicDub on 22.02.2017.
 */
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.provider.ContactsContract;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

public class CallReceiver extends PhonecallReceiver {

//    private String urlLookupPhoneNumber(Context ctx, String phoneNumber) {
//        if (!phoneNumber.matches("^\\+[0-9]+$")) return null;// throw new IllegalArgumentException("Wrong phone format");
//
//        new RetrieveContactInfoTask(ctx).execute(phoneNumber);
//        return null;
//    }

    @Override
    protected void onIncomingCallReceived(Context ctx, String number, Date start)
    {
        if (!number.matches("^\\+[0-9]+$")) return;

        new RetrieveContactInfoTask(ctx).execute(number);

    }

    @Override
    protected void onIncomingCallAnswered(Context ctx, String number, Date start)
    {
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end)
    {
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start)
    {
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end)
    {
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start)
    {
    }
}

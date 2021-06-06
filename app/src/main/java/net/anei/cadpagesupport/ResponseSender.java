package net.anei.cadpagesupport;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;

/*
This class handles old requests to initiate phone calls, send text messages, or initiate MMS content
downloads.  The initiate phone call function quit working properly in Andriod 10 because it requires
that we start an activity from what may be a background state.  New versions of Cadpage route
these requests to ResponseSenderService which does not have this problem, but we keep this to
service old versions of Cadpage.

Also, ResponseSenderService routes the status reports from the SMS send function back to this receiver
to be logged.

 */
public class ResponseSender extends BroadcastReceiver {

  private static final String CALL_PHONE = "net.anei.cadpagesupport.CALL_PHONE";
  private static final String SEND_SMS = "net.anei.cadpagesupport.SendSMS";
  private static final String MMS_DOWNLOAD = "net.anei.cadpagesupport.MMS_DOWNLOAD";

  @Override
  public void onReceive(Context context, Intent intent) {

    if (CALL_PHONE.equals(intent.getAction())) {
      String phone = intent.getStringExtra("phone");
      ResponseSenderService.callPhone(context, phone);
    }
    else if (SEND_SMS.equals(intent.getAction())) {
      String target = intent.getStringExtra("target");
      String message = intent.getStringExtra("message");
      if (target == null || message == null) return;

      ResponseSenderService.sendSMS(context, target, message);
    }
    else if (MMS_DOWNLOAD.equals(intent.getAction())) {
      Uri downloadUri = intent.getData();
      String content = intent.getStringExtra("content_uri");
      String downloadStr = intent.getStringExtra("download_uri");
      int subscriptionId = intent.getIntExtra("subscription_id", -1);
      PendingIntent pIntent = intent.getParcelableExtra("report_intent");
      if (content == null || downloadUri == null) return;
      ResponseSenderService.mmsDownload(context, content, downloadUri, subscriptionId, pIntent);
    }

    else {
      reportResult(intent);
    }
  }

  private void reportResult(Intent intent) {
    if (intent == null) return;
    String action = intent.getAction();
    if (action != null) {
      int pt = action.lastIndexOf('.');
      if (pt >= 0) action = action.substring(pt + 1);
    }
    String status;
    switch (getResultCode()) {

      case Activity.RESULT_OK:
        status = "OK";
        break;

      case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
        status = "Generic failure";
        break;

      case SmsManager.RESULT_ERROR_NO_SERVICE:
        status = "No service";
        break;

      case SmsManager.RESULT_ERROR_NULL_PDU:
        status = "Null PDU";
        break;

      case SmsManager.RESULT_ERROR_RADIO_OFF:
        status = "Radio off";
        break;

      case Activity.RESULT_CANCELED:
        status = "Canceled";
        break;

      default:
        status = "" + getResultCode();
    }
    Log.v("SMS " + action + " status:" + status);
  }
}


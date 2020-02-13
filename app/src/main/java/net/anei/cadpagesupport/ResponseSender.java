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

public class ResponseSender extends BroadcastReceiver {

  private static final String CALL_PHONE = "net.anei.cadpagesupport.CALL_PHONE";
  private static final String SEND_SMS = "net.anei.cadpagesupport.SendSMS";
  private static final String MMS_DOWNLOAD = "net.anei.cadpagesupport.MMS_DOWNLOAD";
  private static final String SMS_SENT = "net.anei.cadpagesupport.ResponseSender.SMS_SENT";
  private static final String SMS_DELIVERED = "net.anei.cadpagesupport.ResponseSender.SMS_DELIVERED";

  @Override
  public void onReceive(Context context, Intent intent) {

    if (CALL_PHONE.equals(intent.getAction())) {
      String phone = intent.getStringExtra("phone");
      callPhone(context, phone);
    }
    else if (SEND_SMS.equals(intent.getAction())) {
      String target = intent.getStringExtra("target");
      String message = intent.getStringExtra("message");
      if (target == null || message == null) return;

      sendSMS(context, target, message);
    }
    else if (MMS_DOWNLOAD.equals(intent.getAction())) {
      Uri downloadUri = intent.getData();
      String content = intent.getStringExtra("content_uri");
      String downloadStr = intent.getStringExtra("download_uri");
      int subscriptionId = intent.getIntExtra("subscription_id", -1);
      PendingIntent pIntent = intent.getParcelableExtra("report_intent");
      if (content == null || downloadUri == null) return;
      mmsDownload(context, content, downloadUri, subscriptionId, pIntent);
    }

    else {
      reportResult(intent);
    }
  }

  private void callPhone(Context context, String phone) {

    String action = Intent.ACTION_CALL;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
      if (context.checkSelfPermission("android.permission.CALL_PHONE") != PackageManager.PERMISSION_GRANTED) {
        action = Intent.ACTION_DIAL;
      }
    }
    try {
      String urlPhone = "tel:" + phone;
      Intent intent = new Intent(action);
      intent.setData(Uri.parse(urlPhone));
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
    } catch (Exception e) {
      Log.v("SMSPopupActivity: Phone call failed" + e.getMessage());
    }
  }

  /**
   * Send SMS response message
   * @param context current context
   * @param target target phone number or address
   * @param message message to be sent
   */
  private void sendSMS(Context context, String target, String message){

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
      if (context.checkSelfPermission("android.permission.SEND_SMS") != PackageManager.PERMISSION_GRANTED) {
        Log.e("sendSMS aborted - SEND_SMS permission has not been granted");
        return;
      }
    }

    Intent sendIntent = new Intent(SMS_SENT);
    sendIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
    sendIntent.setClassName("net.anei.cadpagesupport", "net.anei.cadpagesupport.ResponseSender");
    PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, sendIntent, 0);
    Intent deliverIntent = new Intent(SMS_DELIVERED);
    deliverIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
    deliverIntent.setClassName("net.anei.cadpagesupport", "net.anei.cadpagesupport.ResponseSender");
    PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0, deliverIntent, 0);

    // The send logic apparently isn't as bulletproof as we like.  It sometimes
    // throws a null pointer exception on the other side of an RPC.  We can't
    // do much about it.
    SmsManager sms = SmsManager.getDefault();
    try {
      sms.sendTextMessage(target, null, message, sentPI, deliveredPI);
    } catch (NullPointerException ex) {

      Log.e(ex);
    }
  }

  private void mmsDownload(Context context, String content, Uri downloadUri, int subscriptionId, PendingIntent pIntent) {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
      Log.e("MMS download not supported before Lollipop");
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
      if (context.checkSelfPermission("android.permission.RECEIVE_MMS") != PackageManager.PERMISSION_GRANTED) {
        Log.e("Download MMS aborted - RECEIVE_MMS permission has not been granted");
        return;
      }
    }

    SmsManager smsManager;
    if (subscriptionId != -1) {
      smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
    } else {
      smsManager = SmsManager.getDefault();
    }

    final Bundle configOverrides = smsManager.getCarrierConfigValues();

    if (TextUtils.isEmpty(configOverrides.getString(SmsManager.MMS_CONFIG_USER_AGENT))) {
      configOverrides.remove(SmsManager.MMS_CONFIG_USER_AGENT);
    }

    if (TextUtils.isEmpty(configOverrides.getString(SmsManager.MMS_CONFIG_UA_PROF_URL))) {
      configOverrides.remove(SmsManager.MMS_CONFIG_UA_PROF_URL);
    }

    smsManager.downloadMultimediaMessage(context,
        content,
        downloadUri,
        configOverrides,
        pIntent);
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


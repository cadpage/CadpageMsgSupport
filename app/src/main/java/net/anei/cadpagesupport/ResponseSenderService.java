package net.anei.cadpagesupport;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.text.TextUtils;

public class ResponseSenderService extends Service {

  private static final String SMS_SENT = "net.anei.cadpagesupport.ResponseSender.SMS_SENT";
  private static final String SMS_DELIVERED = "net.anei.cadpagesupport.ResponseSender.SMS_DELIVERED";

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  private final IResponseSenderService.Stub mBinder = new IResponseSenderService.Stub() {

    @Override
    public void callPhone(String phone) {
      ResponseSenderService.callPhone(ResponseSenderService.this, phone);
    }

    @Override
    public void sendSMS(String target, String message){
      ResponseSenderService.sendSMS(ResponseSenderService.this, target, message);
    }

    @Override
    public void mmsDownload(String content, Uri downloadUri, int subscriptionId, PendingIntent pIntent) {
      ResponseSenderService.mmsDownload(ResponseSenderService.this, content, downloadUri, subscriptionId, pIntent);
    }
  };

  public static void callPhone(Context context, String phone) {

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
  public static void sendSMS(Context context, String target, String message){

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

  public static void mmsDownload(Context context, String content, Uri downloadUri, int subscriptionId, PendingIntent pIntent) {

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

}

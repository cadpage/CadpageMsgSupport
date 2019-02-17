package net.anei.cadpagesupport;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

public class ResponseSender extends BroadcastReceiver {

  private static final String SEND_SMS = "net.anei.cadpagesupport.SendSMS";
  private static final String SMS_SENT = "net.anei.cadpagesupport.ResponseSender.SMS_SENT";
  private static final String SMS_DELIVERED = "net.anei.cadpagesupport.ResponseSender.SMS_DELIVERED";

  @Override
  public void onReceive(Context context, Intent intent) {

    if (SEND_SMS.equals(intent.getAction())) {
      String target = intent.getStringExtra("target");
      String message = intent.getStringExtra("message");
      if (target == null || message == null) return;

      sendSMS(context, target, message);
    }

    else {
      reportResult(intent);
    }

  }

  /**
   * Send SMS response message
   * @param context curent context
   * @param target target phone number or address
   * @param message message to be sent
   */
  private void sendSMS(Context context, String target, String message){

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


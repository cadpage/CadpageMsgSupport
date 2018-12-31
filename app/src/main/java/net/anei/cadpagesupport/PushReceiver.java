package net.anei.cadpagesupport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receives Intent.WAP_PUSH_RECEIVED_ACTION intents and starts the
 * TransactionService by passing the push-data to it.
 */
public class PushReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {

    // Pass intent on to Cadpage
    intent.setAction(intent.getAction().replace("android.provider", "net.anei.cadpage"));
    intent.setClassName("net.anei.cadpage", "net.anei.cadpage.PushReceiver");
    context.sendBroadcast(intent);
  }
}


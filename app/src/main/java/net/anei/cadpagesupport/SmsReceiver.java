package net.anei.cadpagesupport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SmsReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {

    // Pass intent on to Cadpage
    intent.setAction(intent.getAction().replace("android.provider", "net.anei.cadpage"));
    intent.setClassName("net.anei.cadpage", "net.anei.cadpage.SmsReceiver");
    context.sendBroadcast(intent);
  }
}


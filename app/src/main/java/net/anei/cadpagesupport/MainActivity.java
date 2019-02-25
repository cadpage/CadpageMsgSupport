package net.anei.cadpagesupport;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

  private boolean needPhoneSupport;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Otherwise handle the button that requests the required permissions
    findViewById(R.id.req_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        checkPermissions(true);
      }
    });

    process(getIntent());
  }

  @Override
  public void onNewIntent(Intent intent) {
    setIntent(intent);
    process(intent);
  }

  private void process(Intent intent) {

    boolean launchedFromCadpage = intent.getBooleanExtra("net.anei.cadpage.LAUNCH", false);
    needPhoneSupport = intent.getBooleanExtra("net.anei.cadpage.CALL_PHONE", false);

    // If we were not launched from Cadpage, do nothing except launch Cadpage and terminate
    // Cadpage will immediately turn around and ask us to set up the correct permissions
    if (!launchedFromCadpage) {
      launchCadpage();
      finish();
    }

    // If we were launched from Cadpage,
    // our only purpose is to ensure that the required permissions have been granted
    // If that is the case, we can quietly shut down.
    if (checkPermissions(true)) {
      Log.v("Cadpage message support enabled");
      finish();
    }
  }

  private static final int PERM_REQ_ID = 3421;

  // Required permissions
  private static final String[] REQ_PERMISSIONS = new String[]{
    "android.permission.READ_SMS",
    "android.permission.RECEIVE_SMS",
    "android.permission.RECEIVE_MMS",
    "android.permission.SEND_SMS"
  };

  /**
   * See if all required permissions have been granted
   * @param request true if user should be asked to grant missing permissions
   * @return true if they have, false otherwise
   */
  private boolean checkPermissions(boolean request) {

    // If run time permissions are not implemented, the answer is always yes
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

    // Build a list of ungranted permissions
    List<String> missingPerms = new ArrayList<>();
    for (String perm : REQ_PERMISSIONS) {
      if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) missingPerms.add(perm);
    }

    if (needPhoneSupport) {
      String perm = "android.permission.CALL_PHONE";
      if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) missingPerms.add(perm);
    }

    if (missingPerms.isEmpty()) return true;

    if (request) requestPermissions(missingPerms.toArray(new String[0]), PERM_REQ_ID);
    return false;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (checkPermissions(false)) {
      Log.v("Cadpage message support enabled");
      finish();
    }
  }

  /**
   * Launch the real Cadpage app
   */
  private void launchCadpage() {
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.setClassName("net.anei.cadpage", "net.anei.cadpage.CadPageActivity");
    try {
      startActivity(intent);
    } catch (Exception ex) {
      Log.e(ex);
    }
  }
}

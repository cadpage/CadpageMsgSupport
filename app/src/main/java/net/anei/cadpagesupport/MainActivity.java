package net.anei.cadpagesupport;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class MainActivity extends Activity {

  private boolean needPhoneSupport;
  private boolean suppressBatteryOptimization;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Otherwise handle the button that requests the required permissions
    findViewById(R.id.req_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (checkPermissions(true))
          checkBatteryOptimization(true);
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
    suppressBatteryOptimization = intent.getBooleanExtra("new.anei.cadpage.SUPPRESS_BATTERY_OPT", false);

    // If we were not launched from Cadpage, do nothing except launch Cadpage and terminate
    // Cadpage will immediately turn around and ask us to set up the correct permissions
    if (!launchedFromCadpage) {
      launchCadpage();
      finish();
    }

    // We can be launched from cadpage for two seperate purposes.  The first will be to
    // check if the necessary permission have been granted
    boolean good;
    if (!suppressBatteryOptimization) {
      good = checkPermissions(true);
      if (good) Log.v("Cadpage permissions OK");
    }

    // The second is to confirm that battery optimization has been disabled
    else {
      good = checkBatteryOptimization(true);
      if (good) Log.v("Cadpage battery optimization OK");
    }

    // If everything is kosher, shut everything down
    if (good) finish();
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
   * @return true if everything is OK
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

  /**
   * Check if battery optimization has been disabled
   * @param request true if user should be prompted to disable optimization
   * @return true if everything is OK
   */

  private boolean checkBatteryOptimization(boolean request) {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;

    PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
    String packageName = getPackageName();
    if (pm.isIgnoringBatteryOptimizations(getPackageName())) return true;

    if (request) {
      @SuppressLint({"InlinedApi", "BatteryLife"})
      Intent newIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + packageName));
      ContentQuery.dumpIntent(newIntent);
      startActivity(newIntent);
    }
    return false;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    // If we were invoked to check permissions, see if that has been taken care of.  If so, we
    // can shut things down
    if (!suppressBatteryOptimization && checkPermissions(false)) {
      Log.v("Cadpage message support enabled");
      finish();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    // If we were invoked to confirm battery optimization has been disabled, see if it has
    // and if so we can shut down
  if (suppressBatteryOptimization && checkBatteryOptimization(false)) {
      Log.v("Cadpage battery optimization disabled");
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

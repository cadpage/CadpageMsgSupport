package net.anei.cadpagesupport;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  private boolean launchedFromCadpage;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Intent intent = getIntent();
    launchedFromCadpage = intent.getBooleanExtra("net.anei.cadpage.LAUNCH", false);

    // Our only purpose is to ensure that the required permissions have been granted
    // If that is the case, we can quietly shut down.  If we were not started from Cadpage, start
    // Cadpage ourselves
    if (checkPermissions(true)) {
      Log.v("Cadpage message support enabled");
      if (!launchedFromCadpage) launchCadpage();
      finish();
    }

    // Otherwise handle the button that requests the required permissions
    ((Button)findViewById(R.id.req_button)).setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v) {
        checkPermissions(true);
      }
    });
  }

  @Override
  protected void onStart() {

    super.onStart();
  }

  private static final int PERM_REQ_ID = 3421;

  // Required permissions
  private static final String[] REQ_PERMISSIONS = new String[]{
    "android.permission.READ_SMS",
    "android.permission.RECEIVE_SMS",
    "android.permission.RECEIVE_MMS"
  };

  /**
   * See if all required permissions have been granted
   * @param request true if user should be asked to grant missing permissions
   * @return true if they have, false otherwise
   */
  @TargetApi(23)
  private boolean checkPermissions(boolean request) {

    // If run time permissions are not implemented, the answer is always yes
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

    // Build a list of ungranted permissions
    List<String> missingPerms = new ArrayList<>();
    for (String perm : REQ_PERMISSIONS) {
      if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) missingPerms.add(perm);
    }

    if (missingPerms.isEmpty()) return true;

    if (request) requestPermissions(REQ_PERMISSIONS, PERM_REQ_ID);
    return false;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (checkPermissions(false)) {
      Log.v("Cadpage message support enabled");
      if (!launchedFromCadpage) launchCadpage();
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

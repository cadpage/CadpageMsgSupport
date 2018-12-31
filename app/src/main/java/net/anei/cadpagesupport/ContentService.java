package net.anei.cadpagesupport;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

public class ContentService extends Service {

  private ContentResolver qr;

  @Override
  public void onCreate() {
    super.onCreate();
    qr = getContentResolver();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  private final IContentService.Stub mBinder = new IContentService.Stub() {

    @Override
    public ContentCursor query(String url, String[] projection, String selection, String[] selectArgs, String sortOrder) {
      Uri uri = Uri.parse(url);
      return new ContentCursor(qr.query(uri, projection, selection, selectArgs, sortOrder));
    }
  };
}

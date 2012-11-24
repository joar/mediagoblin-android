/*
 *  This file is part of MediaGoblin for Android (MGA).
 *  Copyright (C) 2012, MediaGoblin for Android Contributors;
 *  see AUTHORS.
 *
 *  MGA is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MGA is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MGA.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.mediagoblin.android;

import java.io.IOException;

import org.json.JSONObject;
import org.mediagoblin.android.client.PostEntry;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class SendImage extends Activity {

    private static final String TAG = "GMG:Send";
    private AccountManager mAM;
    private Account mgAccount;
    private String token;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sendimage);

        mAM = AccountManager.get(this); // "this" references the current Context

        findViewById(R.id.upload_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        attemptUpload();
                    }
                });

    }

    private void attemptUpload() {
        Bundle options = new Bundle();

        Account[] accounts = mAM.getAccountsByType(Constants.ACCOUNT_TYPE);
        Log.d(TAG, accounts.toString());

        if (accounts.length >= 1) {
            mgAccount = accounts[0];
        }

        mAM.getAuthToken(mgAccount, Constants.AUTHTOKEN_TYPE, options, this,
                new OnTokenAcquired(), new Handler(new OnError()));
    }

    private void uploadComplete(JSONObject result) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, R.string.upload_success, duration);
        toast.show();

        this.finish();
    }

    private class OnError implements Callback {

        @Override
        public boolean handleMessage(Message arg0) {
            // TODO Auto-generated method stub
            Log.e(TAG, arg0.toString());
            return false;
        }

    }

    private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            // Get the result of the operation from the AccountManagerFuture.
            Bundle bundle;
            try {
                bundle = result.getResult();

                // The token is a named value in the bundle. The name of the value
                // is stored in the constant AccountManager.KEY_AUTHTOKEN.
                token = bundle.getString(AccountManager.KEY_AUTHTOKEN);

                Log.d(TAG, "Got Token!!!" + token);

                Intent intent = getIntent();
                if (Intent.ACTION_SEND.equals(intent.getAction())) {
                    Bundle extras = intent.getExtras();
                    if (extras.containsKey(Intent.EXTRA_STREAM)) {
                        Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
                        String scheme = uri.getScheme();
                        if (scheme.equals("content")) {
                            ContentResolver contentResolver = getContentResolver();
                            Cursor cursor = contentResolver.query(uri, null, null,
                                    null, null);
                            cursor.moveToFirst();
                            String filePath = cursor
                                    .getString(cursor
                                            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA));

                            Bundle mediaInfo = new Bundle();
                            mediaInfo.putString(PostEntry.KEY_FILE_PATH, filePath);
                            mediaInfo.putString(PostEntry.KEY_TITLE,
                                    ((TextView)findViewById(R.id.editTitle)).getText().toString());
                            mediaInfo.putString(PostEntry.KEY_DESCRIPTION,
                                    ((TextView)findViewById(R.id.editDescription)).getText().toString());

                            new UploadImage(mAM.getUserData(mgAccount, Constants.KEY_SERVER), token).execute(mediaInfo);
                        }
                    }
                }

            } catch (OperationCanceledException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    private class UploadImage extends PostEntry {

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);

            uploadComplete(result);
        }

        public UploadImage(String server, String token) {
            super(server, token);
        }

    }

}

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

import org.json.JSONException;
import org.json.JSONObject;
import org.mediagoblin.android.client.ApiTest;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String TAG = "GMG:Main";
    private static final int CREATE_ACCOUNT = 1;
    private AccountManager mAM;
    private Account mgAccount;
    private String token;

    private TextView mHello;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHello = (TextView) findViewById(R.id.hello);
        mAM = AccountManager.get(this); // "this" references the current Context
        Account[] accounts = mAM.getAccountsByType(Constants.ACCOUNT_TYPE);
        Log.d(TAG, accounts.toString());

        if (accounts.length < 1) {
            // launch the register Activity
            Intent i = new Intent(this, LoginActivity.class);
            startActivityForResult(i, CREATE_ACCOUNT);
        } else {
            updateMainView();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_ACCOUNT) {
            // we don't care if they made one, just wanted to give the opportunity
            updateMainView();
        }
    }

    private void updateMainView() {
        Bundle options = new Bundle();
        Account[] accounts = mAM.getAccountsByType(Constants.ACCOUNT_TYPE);

        if (accounts.length > 0) {
            mgAccount = accounts[0];
        }

        if (mgAccount != null) {
            mAM.getAuthToken(mgAccount, Constants.AUTHTOKEN_TYPE, options,
                    this, new OnTokenAcquired(), new Handler(new OnError()));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    private class OnError implements Callback {

        @Override
        public boolean handleMessage(Message arg0) {
            // TODO Auto-generated method stub
            Log.e(TAG, arg0.toString());
            return false;
        }

    }

    private class GetUsername extends ApiTest {

        public GetUsername(String server, String token) {
            super(server, token);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            // TODO Auto-generated method stub
            try {
                Log.d(TAG, result.toString());
                mHello.setText(result.getString("username"));
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                mHello.setText("Houston, we have a problem.");
            }
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

                Log.d(TAG, "Received token: " + token);

                new GetUsername(mAM.getUserData(mgAccount, Constants.KEY_SERVER), token).execute();

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

}

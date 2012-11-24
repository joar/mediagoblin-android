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

import org.mediagoblin.android.client.NetworkUtilities;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends AccountAuthenticatorActivity {


    private static final String TAG = "GMG";

    private RetrieveTokenTask mAuthTask = null;
    private Context mContext;

    // Values for email and password at the time of the login attempt.
    private String mServer;
    private String mAccessCode;

    // UI references.
    private EditText mServerEdit;
    private View mLoginFormView;
    private View mLoginStatusView;
    private View mOAuthView;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        setContentView(R.layout.activity_login);

        // Set up the login form.
        mServerEdit = (EditText) findViewById(R.id.server);

        mOAuthView = findViewById(R.id.oauth_view);
        mWebView = (WebView) findViewById(R.id.webView);
        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);

        findViewById(R.id.sign_in_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        attemptLogin();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_login, menu);
        return true;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mServerEdit.setError(null);

        // Store values at the time of the login attempt.
        mServer = mServerEdit.getText().toString();

        mLoginFormView.setVisibility(View.GONE);
        mOAuthView.setVisibility(View.VISIBLE);

        // set up webview for OAuth2 login
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if ( url.startsWith(NetworkUtilities.REDIRECT_URI) ) {
                    Log.i(TAG, url);

                    // extract OAuth2 access_token appended in url
                    if ( url.indexOf("code=") != -1 ) {
                        Log.i(TAG, url);
                        mAccessCode = extractAccessCode(url);

                        // store in default SharedPreferences
                        showProgress(true);
                        mAuthTask = new RetrieveTokenTask();
                        mAuthTask.execute((Void) null);
                    }

                    // don't go to redirectUri
                    return true;
                }

         // load the webpage from url (login and grant access)
         return super.shouldOverrideUrlLoading(view, url); // return false;
         }
        });

        // do OAuth2 login
        String authorizationUri = mReturnAuthorizationRequestUri();
        mWebView.loadUrl(authorizationUri);
    }

    private String mReturnAuthorizationRequestUri() {
        StringBuilder sb = new StringBuilder();
        sb.append(mServer);
        sb.append(NetworkUtilities.PATH_OAUTH_AUTHORIZE);
        sb.append("?response_type=token");
        sb.append("&client_id="+NetworkUtilities.CLIENT_ID);
        sb.append("&redirect_uri="+NetworkUtilities.REDIRECT_URI);
        Log.d(TAG, sb.toString());
        return sb.toString();
    }

    private String extractAccessCode(String url) {

        Uri uri=Uri.parse(url);
        return uri.getQueryParameter("code");
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(
                    android.R.integer.config_shortAnimTime);

            mLoginStatusView.setVisibility(View.VISIBLE);
            mLoginStatusView.animate().setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginStatusView.setVisibility(show ? View.VISIBLE
                                    : View.GONE);
                        }
                    });

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE
                                    : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private class RetrieveTokenTask extends AsyncTask<Void, Void, Boolean> {


        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            String authToken;

            authToken = NetworkUtilities.getAuthToken(mServer, mAccessCode);

            if (authToken != null) {
                // create the Account in the account manager
                Log.i(TAG, authToken);
                final Account account = new Account(mServer, Constants.ACCOUNT_TYPE);
                Bundle user_data = new Bundle();
                user_data.putString(Constants.KEY_SERVER, mServer);
                // XXX store refresh token here
                AccountManager.get(mContext).addAccountExplicitly(account, authToken, user_data);

                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                Context context = getApplicationContext();
                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(context, R.string.account_creation_success, duration);
                toast.show();

                finish();
            } else {
                // TODO: Handle failure to get token
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

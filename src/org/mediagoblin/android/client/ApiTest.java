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

package org.mediagoblin.android.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Asynchronously call the test method of the MG API.
 *
 * @author nathan
 *
 */
public class ApiTest extends AsyncTask<Void, Void, JSONObject> {

    private static final String TAG = "GMG:ApiTest";

    public static final String API_BASE = "/api";
    public static final String API_TEST = "/test";

    public static final String PARAM_ACCESS_TOKEN = "access_token";

    String mServer;
    String mToken;

    public ApiTest(String server, String token) {
        mServer = server;
        mToken = token;
    }

    @Override
    protected JSONObject doInBackground(Void... params) {

        InputStream is;

        Uri.Builder uri_builder = Uri.parse(mServer + API_BASE + API_TEST).buildUpon();
        uri_builder.appendQueryParameter(PARAM_ACCESS_TOKEN, mToken);

        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) new URL(uri_builder.toString())
                    .openConnection();

            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response_code = conn.getResponseCode();
            Log.d(TAG, "The response is: " + response_code);
            is = conn.getInputStream();

            // parse token_response as JSON to get the token out
            String str_response = NetworkUtilities.readStreamToString(is, 500);
            Log.d("GMG", str_response);
            JSONObject response = new JSONObject(str_response);
            return response;
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}

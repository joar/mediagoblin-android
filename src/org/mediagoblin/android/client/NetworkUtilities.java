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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.util.Log;

/**
 * Provides utility methods for communicating with the server.
 */
final public class NetworkUtilities {

    private static final String PATH_OAUTH_ACCESS_TOKEN = "/oauth-2/access_token";
    public static final String PATH_OAUTH_AUTHORIZE  = "/oauth-2/authorize";

    private static final String TAG = "GMG:NetworkUtilities";

    public static final String PARAM_CODE = "code";
    public static final String PARAM_CLIENT_SECRET = "client_secret";
    public static final String PARAM_CLIENT_ID = "client_id";

    public static final String REDIRECT_URI    = "https://localhost-mediagoblin-app";
    public static final String CLIENT_ID = "c0ec891a-510f-423e-9009-50252f340b33";
    public static final String CLIENT_SECRET = "$2a$12$QhEaSCo0R8xzoH1CBFmJjeCUBvQLMYO0K4ldPhBsHdpnhbPSb3zPG";

    /** Timeout (in ms) we specify for each http request */
    public static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;


    private NetworkUtilities() {
    }

    /**
     * Configures the httpClient to connect to the URL provided.
     */
    public static HttpClient getHttpClient() {
        HttpClient httpClient = new DefaultHttpClient();
        final HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        return httpClient;
    }

    private static URL getRequestURL(String server, String uri, ArrayList<NameValuePair> params) throws MalformedURLException {

        Uri server_uri = Uri.parse(server + uri);
        Uri.Builder request_builder = server_uri.buildUpon();

        for (NameValuePair item: params) {
            request_builder.appendQueryParameter(item.getName(), item.getValue());
        }
        request_builder.appendQueryParameter(PARAM_CLIENT_ID, CLIENT_ID);

        return new URL(request_builder.toString());
    }

    public static String getAuthToken(String server, String code) {

        String token=null;

        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_CODE, code));
        params.add(new BasicNameValuePair(PARAM_CLIENT_SECRET, CLIENT_SECRET));

        InputStream is = null;
        URL auth_token_url;
        try {
            auth_token_url = getRequestURL(server, PATH_OAUTH_ACCESS_TOKEN,
                    params);
            Log.i(TAG, auth_token_url.toString());

            HttpURLConnection conn = (HttpURLConnection) auth_token_url
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
            String str_response = readStreamToString(is, 500);
            Log.d(TAG, str_response);
            JSONObject response = new JSONObject(str_response);
            token = response.getString("access_token");

        } catch (MalformedURLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        Log.i(TAG, token);
        return token;
    }

    static String readStreamToString(InputStream stream) throws IOException, UnsupportedEncodingException {
        return readStreamToString(stream, 1024);
    }

    static String readStreamToString(InputStream stream, int block_size)
            throws IOException, UnsupportedEncodingException {

        InputStream in = stream;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] pBuffer = new byte[block_size];

        try {
            for (;;) {
                int res = in.read(pBuffer);
                if (res == -1) {
                    break;
                }
                if (res > 0) {
                    if (out != null) {
                        out.write(pBuffer, 0, res);
                    }
                }
            }
            out.close();
            in.close();
            in = null;
            return out.toString();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable t) {
                    /* Ignore me */
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable t) {
                    /* Ignore me */
                }
            }
        }

    }


}

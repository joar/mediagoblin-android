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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

/**
 * Asynchronously call the post entry method of the MG API.
 *
 * @author nathan
 *
 */
public class PostEntry extends AsyncTask<Bundle, Void, JSONObject> {

    private static final String TAG = "GMG";
    public static final String KEY_FILE_PATH = "filepath";
    public static final String KEY_TITLE = "title";
    public static final String KEY_DESCRIPTION = "description";

    public static final String API_BASE = "/api";
    public static final String API_POST_ENTRY = "/submit";

    public static final String PARAM_ACCESS_TOKEN = "access_token";

    public static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

    String mServer;
    String mToken;

    public PostEntry(String server, String token) {
        mServer = server;
        mToken = token;
    }

    /**
     * Fire the task to post the media entry. The media information is encoded
     * in Bundles. Each Bundle MUST contain the KEY_FILE_PATH pointing to the
     * resource. Bundles MAY contain KEY_TITLE and KEY_DESCRIPTION for passing
     * additional metadata.
     *
     */
    @Override
    protected JSONObject doInBackground(Bundle... mediaInfoBundles) {

        Bundle mediaInfo = mediaInfoBundles[0];
        HttpURLConnection connection = null;

        Uri.Builder uri_builder = Uri
                .parse(mServer + API_BASE + API_POST_ENTRY).buildUpon();
        uri_builder.appendQueryParameter(PARAM_ACCESS_TOKEN, mToken);

        String charset = "UTF-8";

        File binaryFile = new File(mediaInfo.getString(KEY_FILE_PATH));

        // Semi-random value to act as the boundary
        String boundary = Long.toHexString(System.currentTimeMillis());
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.
        PrintWriter writer = null;

        try {
            URL url = new URL(uri_builder.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + boundary);

            OutputStream output = connection.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(output, charset),
                    true); // true = autoFlush, important!

            // Send metadata
            if (mediaInfo.containsKey(KEY_TITLE)) {
                writer.append("--" + boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"title\"")
                        .append(CRLF);
                writer.append("Content-Type: text/plain; charset=" + charset)
                        .append(CRLF);
                writer.append(CRLF);
                writer.append(mediaInfo.getString(KEY_TITLE)).append(CRLF)
                        .flush();
            }
            if (mediaInfo.containsKey(KEY_DESCRIPTION)) {
                writer.append("--" + boundary).append(CRLF);
                writer.append(
                        "Content-Disposition: form-data; name=\"description\"")
                        .append(CRLF);
                writer.append("Content-Type: text/plain; charset=" + charset)
                        .append(CRLF);
                writer.append(CRLF);
                writer.append(mediaInfo.getString(KEY_DESCRIPTION))
                        .append(CRLF).flush();
            }

            // Send binary file.
            writer.append("--" + boundary).append(CRLF);
            writer.append(
                    "Content-Disposition: form-data; name=\"file\"; filename=\""
                            + binaryFile.getName() + "\"").append(CRLF);
            writer.append(
                    "Content-Type: "
                            + URLConnection.guessContentTypeFromName(binaryFile
                                    .getName())).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF).flush();
            InputStream input = null;
            try {
                input = new FileInputStream(binaryFile);
                byte[] buffer = new byte[1024];
                for (int length = 0; (length = input.read(buffer)) > 0;) {
                    output.write(buffer, 0, length);
                }
                output.flush(); // Important! Output cannot be closed. Close of
                                // writer will close output as well.
            } finally {
                if (input != null) try {
                    input.close();
                } catch (IOException logOrIgnore) {
                }
            }
            writer.append(CRLF).flush(); // CRLF is important! It indicates end
                                         // of binary boundary.

            // End of multipart/form-data.
            writer.append("--" + boundary + "--").append(CRLF);
            writer.close();

            // read the response
            int serverResponseCode = connection.getResponseCode();
            String serverResponseMessage = connection.getResponseMessage();
            Log.d(TAG, Integer.toString(serverResponseCode));
            Log.d(TAG, serverResponseMessage);

            InputStream is = connection.getInputStream();

            // parse token_response as JSON to get the token out
            String str_response = NetworkUtilities.readStreamToString(is);
            Log.d(TAG, str_response);
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
        } finally {
            // if (writer != null) writer.close();
        }

        return null;
    }

}

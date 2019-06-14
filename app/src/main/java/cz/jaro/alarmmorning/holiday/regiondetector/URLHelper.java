package cz.jaro.alarmmorning.holiday.regiondetector;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Helper class for URLs.
 */
public class URLHelper {
    private static final String TAG = URLHelper.class.getSimpleName();

    /**
     * Tests whether an URL exists.
     *
     * @param URLString URL
     * @return true if the URL exists (and has content)
     * @throws IOException On Input/Output exception
     */
    public static boolean exists(String URLString) throws IOException {
        URL url = new URL(URLString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("HEAD");
        urlConnection.connect();
        int code = urlConnection.getResponseCode();

        return code == 200;
    }

    /**
     * Donwloads the content at an URL.
     *
     * @param URLString URL.
     * @return Content.
     * @throws IOException If there is a problem reading the stream.
     */
    public static String readURL(String URLString) throws IOException {
        StringBuilder res = new StringBuilder();

        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(URLString);

            urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = urlConnection.getInputStream();

            InputStreamReader isw = new InputStreamReader(in);

            int data;
            while ((data = isw.read()) != -1) {
                char ch = (char) data;
                res.append(ch);
            }
            in.close();
        } catch (IOException e) {
            Log.w(TAG, "Cannot read stream", e);
            throw e;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        Log.v(TAG, "The URL " + URLString + " returned content: " + res);
        return res.toString();
    }

}

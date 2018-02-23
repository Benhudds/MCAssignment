package common;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

// "Static" type class to read JSON from a URL
public final class URLStringReader {

    // Private constructor to prevent the class from being instantiated
    private URLStringReader() {
    }

    // Method to read JSON from a URL
    public static String readUrl(String urlString) throws IOException {
        BufferedReader reader = null;

        try {
            // Get the URL object and a buffer to read the string into
            URL url = new URL(urlString);
            StringBuffer buffer = new StringBuffer();

            // Get the stream from the URL
            reader = new BufferedReader(new InputStreamReader(url.openStream()));

            // Temp variables to read into
            // Reads 1024 characters at a time
            int read;
            char[] chars = new char[1024];

            // Loop through the buffered readers stream until there are no more characters
            while ((read = reader.read(chars)) != -1) {
                buffer.append(chars, 0, read);
            }

            // Return the JSON string
            return buffer.toString();
        } catch (IOException ex) {
            Log.e("URL", ex.getMessage());
            return null;
        } finally {
            // Close the buffered reader
            if (reader != null) {
                reader.close();
            }
        }
    }
}

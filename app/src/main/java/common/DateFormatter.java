package common;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


// "Static" type class for generating dates from strings
public final class DateFormatter {

    // Private constructor to prevent the class from being instantiated
    private DateFormatter() {
    }

    // Method to generate a java Date object from a string
    // Returns null if not possible
    public static Date getDate(String string) {
        Date date = null;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            date = dateFormat.parse(string);
        } catch (ParseException ex) {
        }

        return date;
    }

    // Method to generate a string from a java Data object
    public static String getString(Date date) {
        String string;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        string = dateFormat.format(date);

        return string;
    }
}

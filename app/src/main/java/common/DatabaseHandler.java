package common;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.ben.currencyconvertor.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// DatabaseHandler inherited form SQLiteOpenHelper
// Creates a locally stored database that is copied from a database asset
// Removes upgrade functionality so shouldn't be used outside of application
public final class DatabaseHandler extends SQLiteOpenHelper {

    // Database name data member
    private String databaseName;

    // Database path data member
    private String databasePath;

    // SQLite database data member
    private SQLiteDatabase database;

    // Static instance of the current object
    private static DatabaseHandler instance = null;

    // Getter for the static instance of the current object
    // If the instance is null, then it is generated
    // Implemented as such so that SQLite connections are not leaked
    public static DatabaseHandler getInstance(Context context, String dbPath, String dbName) {
        if(instance != null) {
            return instance;
        }

        return new DatabaseHandler(context, dbPath, dbName);
    }

    // Private constructor that creates the local database if it does not exist
    // Otherwise, it gets a readable copy of the database
    private DatabaseHandler(Context context, String dbPath, String dbName) {
        super(context, dbName, null, 1);
        this.databaseName = dbName;
        this.databasePath = dbPath;

        if (!this.checkDataBase(context)) {
            try {
                this.createDataBase(context);
            } catch (IOException ex) {
                Log.e(context.getString(R.string.app_name), ex.getMessage());
                return;
            }
        }

        this.database = this.getWritableDatabase();

        instance = this;
    }

    // Method to check if the local database exists
    private boolean checkDataBase(Context context) {
        File dbFile = context.getDatabasePath(this.databaseName);
        return dbFile.exists();
    }

    // Method to create the local database
    // Throws IOException
    private void createDataBase(Context context) throws IOException {

        // Creates an empty database then copies the asset database contents to the local one
        this.getWritableDatabase();
        this.copyDataBase(context);
    }

    // Method to copy the contents of the asset database to the local database
    private void copyDataBase(Context context) throws IOException {

        // Open asset database
        InputStream myInput = context.getAssets().open(this.databaseName);

        // Path to the just created empty db
        String outFileName = this.databasePath + this.databaseName;

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

    }

    // Method to return a cursor for the given query
    public Cursor query(String query) {
        return this.database.rawQuery(query, null);
    }

    // Method to execute a query on the database
    public void exexQuery(String query) {
        this.database.execSQL(query);
    }

    // Overriden close method
    @Override
    public synchronized void close() {
        if (this.database != null) {
            this.database.close();
        }

        super.close();
    }

    // Overriden onCreate method
    // DO NOT USE
    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    // Overriden onUpgrade method
    // DO NOT USE
    @Override
    public void onUpgrade(SQLiteDatabase db, int a, int b) {
    }
}
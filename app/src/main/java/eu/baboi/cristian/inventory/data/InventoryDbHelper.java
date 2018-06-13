package eu.baboi.cristian.inventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Formatter;

import eu.baboi.cristian.inventory.data.InventoryContract.InventoryEntry;



public class InventoryDbHelper extends SQLiteOpenHelper{
    private static final String DATABASE_NAME = "inventory.db";
    private static final int DATABASE_VERSION = 16;

    // The constructor
    public  InventoryDbHelper(Context context){
        super(context,context.getDatabasePath(DATABASE_NAME).getAbsolutePath(),null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Formatter f = new Formatter();
        f.format(InventoryEntry.CREATE_TABLE_SQL_TEMPLATE,
                InventoryEntry.TABLE_NAME.trim(),
                InventoryEntry._ID.trim(),
                InventoryEntry.COLUMN_INVENTORY_PRODUCT.trim(),
                InventoryEntry.COLUMN_INVENTORY_PRICE.trim(),
                InventoryEntry.COLUMN_INVENTORY_QUANTITY.trim(),
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER.trim(),
                InventoryEntry.COLUMN_INVENTORY_PHONE.trim());
        try {
            db.execSQL(f.toString());// create table with constraints for ensuring database integrity
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String SAVE_DATA = "create table %s as select * from %s";
        final String DROP_TABLE = "drop table %s";
        final String RESTORE_DATA = "insert into %s select * from %s";
        final String TEMPORARY_TABLE = "copy_of_data";
        final String DATA_TABLE = InventoryEntry.TABLE_NAME.trim();

        db.beginTransaction();
        try {

            // Save the data to a temporary table
            db.execSQL(String.format(SAVE_DATA, TEMPORARY_TABLE, DATA_TABLE));

            // Drop the original table
            db.execSQL(String.format(DROP_TABLE, DATA_TABLE));

            // Create the table again
            onCreate(db);

            // Restore the data
            db.execSQL(String.format(RESTORE_DATA, DATA_TABLE, TEMPORARY_TABLE));

            // Drop temporary table
            db.execSQL(String.format(DROP_TABLE, TEMPORARY_TABLE));

            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }
}



package eu.baboi.cristian.inventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Formatter;

import eu.baboi.cristian.inventory.data.InventoryContract.InventoryEntry;


public class InventoryDbHelper extends SQLiteOpenHelper{
    private static final String DATABASE_NAME = "inventory.db";
    private static final int DATABASE_VERSION = 4;

    // The constructor
    public  InventoryDbHelper(Context context){
        super(context,context.getDatabasePath(DATABASE_NAME).getAbsolutePath(),null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_INVENTORY_TABLE_TEMPLATE = "CREATE TABLE %s(%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT UNIQUE NOT NULL, %s REAL NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL)";
        Formatter f = new Formatter();
        f = f.format(SQL_CREATE_INVENTORY_TABLE_TEMPLATE,
                InventoryEntry.TABLE_NAME,
                InventoryEntry._ID,
                InventoryEntry.COLUMN_INVENTORY_PRODUCT,
                InventoryEntry.COLUMN_INVENTORY_PRICE,
                InventoryEntry.COLUMN_INVENTORY_QUANTITY,
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER,
                InventoryEntry.COLUMN_INVENTORY_PHONE);
        db.execSQL(f.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}



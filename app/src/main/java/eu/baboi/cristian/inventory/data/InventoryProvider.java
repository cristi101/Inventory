package eu.baboi.cristian.inventory.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import eu.baboi.cristian.inventory.data.InventoryContract.InventoryEntry;

public class InventoryProvider extends ContentProvider {
    private static final int INVENTORY = 0;
    private static final int INVENTORY_ID = 1;
    private static final int SELL_ID = 2;
    private static final int BUY_ID = 3;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private InventoryDbHelper dbHelper;
    private SQLiteStatement sell = null;
    private SQLiteStatement buy = null;

    // Add paths to matcher
    static {
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY, INVENTORY);
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, String.format("%s/#", InventoryContract.PATH_INVENTORY), INVENTORY_ID);
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, String.format("%s/#/#", InventoryContract.PATH_SELL_INVENTORY), SELL_ID);
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, String.format("%s/#/#", InventoryContract.PATH_BUY_INVENTORY), BUY_ID);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    // return new selection string for _ID
    private String idSelection() {
        return InventoryEntry._ID + "=?";
    }

    // return new Args array for _ID
    private String[] idArg(Uri uri) {
        return new String[]{String.valueOf(ContentUris.parseId(uri))};
    }

    // return the content resolver or throw exception
    private ContentResolver getContentResolver() throws IllegalStateException {
        Context context = getContext();
        if (context == null) throw new IllegalStateException("null Context in InventoryProvider!");
        ContentResolver resolver = context.getContentResolver();
        if (resolver == null)
            throw new IllegalStateException("null ContentResolver in InventoryProvider!");
        return resolver;
    }

    // The following content provider methods throw:
    //   - SQLiteConstraintException on database constraint violation
    //   - IllegalArgumentException on invalid uri
    //   - IllegalStateException on null Context or ContentProvider
    // They return null or 0 in any other case and print a stack trace in case of other SQLiteException

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) throws SQLiteConstraintException, IllegalArgumentException, IllegalStateException {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            final int match = sUriMatcher.match(uri);
            switch (match) {
                case INVENTORY:
                    cursor = db.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                    break;
                case INVENTORY_ID:
                    cursor = db.query(InventoryEntry.TABLE_NAME, projection, idSelection(), idArg(uri), null, null, sortOrder);
                    break;
                default:
                    throw new IllegalArgumentException("Cannot query unknown URI " + uri);
            }
        } catch (SQLiteConstraintException e) {
            throw e; // throw an exception on database constraint violation
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        if (cursor != null)
            cursor.setNotificationUri(getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) throws IllegalArgumentException {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return InventoryEntry.CONTENT_LIST_TYPE;
            case INVENTORY_ID:
                return InventoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) throws SQLiteConstraintException, IllegalArgumentException, IllegalStateException {
        Uri uriID = null;
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            final int match = sUriMatcher.match(uri);
            switch (match) {
                case INVENTORY:
                    long id = db.insertOrThrow(InventoryEntry.TABLE_NAME, null, values);
                    if (id != -1) {
                        getContentResolver().notifyChange(uri, null);
                        uriID = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Insertion is not supported for " + uri);
            }
        } catch (SQLiteConstraintException e) {
            throw e; // throw an exception on database constraint violation
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return uriID;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) throws SQLiteConstraintException, IllegalArgumentException, IllegalStateException {
        int count = 0;
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            final int match = sUriMatcher.match(uri);
            switch (match) {
                case INVENTORY:
                    count = db.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                    break;
                case INVENTORY_ID:
                    count = db.delete(InventoryEntry.TABLE_NAME, idSelection(), idArg(uri));
                    break;
                default:
                    throw new IllegalArgumentException("Deletion is not supported for URI " + uri);
            }
        } catch (SQLiteConstraintException e) {
            throw e; // throw an exception on database constraint violation
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        if (count > 0)
            getContentResolver().notifyChange(uri, null);
        return count;
    }

    // record to hold BUY/SELL uri parts
    private class UriData {
        Uri uri;// item uri
        long id;
        long quantity;
    }

    // get BUY/SELL uri parts
    private void splitUri(Uri uri, UriData data) {
        List<String> segments = uri.getPathSegments();
        if (segments.size() != 4) throw new IllegalArgumentException("Wrong URI format " + uri);

        Uri.Builder builder = new Uri.Builder();
        builder.scheme(uri.getScheme()).authority(uri.getAuthority());
        builder.appendPath(segments.get(0)).appendPath(segments.get(3));
        data.uri = builder.build();

        data.id = Long.parseLong(segments.get(3));
        data.quantity = Long.parseLong(segments.get(2));
    }


    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) throws SQLiteConstraintException, IllegalArgumentException, IllegalStateException {
        int count = 0;
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            final int match = sUriMatcher.match(uri);
            switch (match) {
                case INVENTORY:
                    count = db.update(InventoryEntry.TABLE_NAME, values, selection, selectionArgs);
                    break;
                case INVENTORY_ID:
                    count = db.update(InventoryEntry.TABLE_NAME, values, idSelection(), idArg(uri));
                    break;
                case SELL_ID:
                    if (sell == null) sell = db.compileStatement(InventoryEntry.SELL_SQL);
                    UriData sellData = new UriData();
                    splitUri(uri, sellData);
                    sell.bindLong(1, sellData.quantity);
                    sell.bindLong(2, sellData.id);
                    count = sell.executeUpdateDelete();
                    uri = sellData.uri;
                    break;
                case BUY_ID:
                    if (buy == null) buy = db.compileStatement(InventoryEntry.BUY_SQL);
                    UriData buyData = new UriData();
                    splitUri(uri, buyData);
                    buy.bindLong(1, buyData.quantity);
                    buy.bindLong(2, buyData.id);
                    count = buy.executeUpdateDelete();
                    uri = buyData.uri;
                    break;
                default:
                    throw new IllegalArgumentException("Update is not supported for URI " + uri);
            }
        } catch (SQLiteConstraintException e) {
            throw e; // throw an exception on database constraint violation
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        if (count > 0)
            getContentResolver().notifyChange(uri, null);
        return count;
    }
}

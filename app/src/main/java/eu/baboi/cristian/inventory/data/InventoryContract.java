package eu.baboi.cristian.inventory.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class InventoryContract {
    public static final String CONTENT_AUTHORITY = "eu.baboi.cristian.inventory";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // paths
    public static final String PATH_INVENTORY = "inventory";
    public static final String PATH_SELL_INVENTORY = String.format("%s/sell", PATH_INVENTORY);
    public static final String PATH_BUY_INVENTORY = String.format("%s/buy", PATH_INVENTORY);

    private InventoryContract() {
    }

    public static final class InventoryEntry implements BaseColumns {
        // content types
        public static final String CONTENT_LIST_TYPE = String.format("%s/%s/%s", ContentResolver.CURSOR_DIR_BASE_TYPE, CONTENT_AUTHORITY, PATH_INVENTORY);
        public static final String CONTENT_ITEM_TYPE = String.format("%s/%s/%s", ContentResolver.CURSOR_ITEM_BASE_TYPE, CONTENT_AUTHORITY, PATH_INVENTORY);

        // uri
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_INVENTORY);
        public static final Uri SELL_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_SELL_INVENTORY);
        public static final Uri BUY_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_BUY_INVENTORY);

        // names
        public static final String TABLE_NAME = "inventory";
        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_INVENTORY_PRODUCT = "product";
        public final static String COLUMN_INVENTORY_PRICE = "price";
        public final static String COLUMN_INVENTORY_QUANTITY = "quantity";
        public final static String COLUMN_INVENTORY_SUPPLIER = "supplier";
        public final static String COLUMN_INVENTORY_PHONE = "phone";

        // SQL templates
        public final static String SELL_SQL = String.format("update %1$s set %2$s=%2$s-? where %3$s=?", TABLE_NAME, COLUMN_INVENTORY_QUANTITY, _ID);
        public final static String BUY_SQL = String.format("update %1$s set %2$s=%2$s+? where %3$s=?", TABLE_NAME, COLUMN_INVENTORY_QUANTITY, _ID);
        public final static String CREATE_TABLE_SQL_TEMPLATE =
                "CREATE TABLE %1$s(" +
                        "%2$s INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "%3$s TEXT UNIQUE NOT NULL CONSTRAINT %3$S_IS_NOT_EMPTY CHECK(trim(%3$s)!=''), " +
                        "%4$s REAL        NOT NULL CONSTRAINT %4$S_IS_POSITIVE  CHECK(%4$s >= 0), " +
                        "%5$s INTEGER     NOT NULL CONSTRAINT %5$S_IS_POSITIVE  CHECK(%5$s >= 0), " +
                        "%6$s TEXT        NOT NULL CONSTRAINT %6$S_IS_NOT_EMPTY CHECK(trim(%6$s)!=''), " +
                        "%7$s TEXT        NOT NULL CONSTRAINT %7$S_IS_NOT_EMPTY CHECK(trim(%7$s)!=''))";
    }
}


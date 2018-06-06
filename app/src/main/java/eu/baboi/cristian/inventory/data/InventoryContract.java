package eu.baboi.cristian.inventory.data;

import android.provider.BaseColumns;

public final class InventoryContract {
    private InventoryContract(){}

    public static final class InventoryEntry implements BaseColumns{
        public static final String TABLE_NAME="inventory";
        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_INVENTORY_PRODUCT ="product";
        public final static String COLUMN_INVENTORY_PRICE ="price";
        public final static String COLUMN_INVENTORY_QUANTITY ="quantity";
        public final static String COLUMN_INVENTORY_SUPPLIER ="supplier";
        public final static String COLUMN_INVENTORY_PHONE ="phone";
    }
}


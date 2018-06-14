package eu.baboi.cristian.inventory;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import eu.baboi.cristian.inventory.data.InventoryContract.InventoryEntry;

public class InventoryCursorAdapter extends CursorAdapter {

    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // get the data
        final Long id = cursor.getLong(0);
        final String product = cursor.getString(1);
        final Double price = cursor.getDouble(2);
        final Long quantity = cursor.getLong(3);

        // find the views
        final TextView productView = view.findViewById(R.id.product);
        final TextView priceView = view.findViewById(R.id.price);
        final TextView quantityView = view.findViewById(R.id.quantity);
        final ImageView saleView = view.findViewById(R.id.sell);
        saleView.setOnClickListener(new Listener(id));

        // set the views
        productView.setText(product);
        priceView.setText(String.format("%,.2f", price));
        quantityView.setText(String.format("%,d", quantity));
    }

    // sale button handling
    private static class Listener implements View.OnClickListener {
        long mId;

        Listener(long id) {
            mId = id;
        }

        @Override
        public void onClick(View v) {
            int count = 0;
            final Context context = v.getContext();
            final ContentResolver resolver = context.getContentResolver();
            try { // on UI thread
                count = resolver.update(Uri.parse(String.format("%s/%d/%d", InventoryEntry.SELL_URI, 1, mId)), null, null, null);
            } catch (SQLiteConstraintException e) {
                Toast.makeText(context, context.getString(R.string.sell_item_error, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                return;
            }
            if (count == 0)
                Toast.makeText(context, R.string.database_error, Toast.LENGTH_LONG).show();
        }
    }

}

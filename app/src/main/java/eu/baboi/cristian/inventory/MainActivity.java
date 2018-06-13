package eu.baboi.cristian.inventory;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;

import eu.baboi.cristian.inventory.data.InventoryContract.InventoryEntry;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {
    private CursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // there is no data at first
        mAdapter = new InventoryCursorAdapter(this, null);

        // list view setup
        ListView list = findViewById(R.id.list);
        list.setEmptyView(findViewById(R.id.empty));
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(this);

        // start loader
        getLoaderManager().initLoader(0, null, this);
    }

    // user event handling

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                add();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        view(id);
    }

    // Cursor binding to the list

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != 0) return null;
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_INVENTORY_PRODUCT,
                InventoryEntry.COLUMN_INVENTORY_PRICE,
                InventoryEntry.COLUMN_INVENTORY_QUANTITY
        };
        return new CursorLoader(this, InventoryEntry.CONTENT_URI, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    // user actions
    private void add() {
        Intent intent = new Intent(this, ViewActivity.class);
        startActivity(intent);
    }

    private void view(long id) {
        Intent intent = new Intent(this, ViewActivity.class);
        intent.setAction(Intent.ACTION_EDIT);
        intent.setData(ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id));
        startActivity(intent);
    }
}

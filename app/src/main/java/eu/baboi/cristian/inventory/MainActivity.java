package eu.baboi.cristian.inventory;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import eu.baboi.cristian.inventory.data.InventoryDbHelper;

public class MainActivity extends AppCompatActivity {
    private TextView text;
    private InventoryDbHelper helper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = findViewById(R.id.text);
        helper = new InventoryDbHelper(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            insertData();
        } catch (SQLException e) {
        }

        Cursor cursor=null;
        try{
            cursor = queryData();
            StringBuilder data = new StringBuilder();
            while (cursor.moveToNext()){
                String product = cursor.getString(0);
                String price = cursor.getString(1);
                String quantity = cursor.getString(2);
                String supplier = cursor.getString(3);
                String phone = cursor.getString(4);

                data.append("\n product:");data.append(product);
                data.append("\n   price:");data.append(price);
                data.append("\nquantity:");data.append(quantity);
                data.append("\nsupplier:");data.append(supplier);
                data.append("\n   phone:");data.append(phone);
                data.append("\n------------------------------");

                Log.e("DATA  product",product);
                Log.e("DATA    price",price);
                Log.e("DATA quantity",quantity);
                Log.e("DATA supplier",supplier);
                Log.e("DATA    phone",phone);
                Log.e("DATA","------------------------------");
            }
            text.setText(data.toString());
        } finally {
            if(cursor!=null)
            cursor.close();
        }
    }

    private void insertData(){
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("delete from inventory");
        db.execSQL("insert into inventory(product,price,quantity,supplier,phone) values ('bicycle',99.99,18,'Golden Bicycles','123456789')");
        db.execSQL("insert into inventory(product,price,quantity,supplier,phone) values ('ball',49.99, 100,'Balls for everyone','987654321')");
    }

    private Cursor queryData(){
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.rawQuery("select product, price, quantity, supplier, phone from inventory",null);
    }
}

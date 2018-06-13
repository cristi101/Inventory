package eu.baboi.cristian.inventory;

import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import eu.baboi.cristian.inventory.data.InventoryContract.InventoryEntry;

public class ViewActivity extends AppCompatActivity implements TextWatcher, LoaderManager.LoaderCallbacks<Cursor> {
    // String keys for saving & restoring state
    private static final String URI = "URI";
    private static final String CHANGED = "CHANGED";
    private static final String MODE = "MODE";

    // local state
    private Uri mUri;
    private boolean dataChanged = false;
    private boolean editMode = false;

    private Locale locale;
    private KeyboardView keyboardView;

    // references to user interface fields
    private EditText productView;
    private EditText priceView;
    private EditText quantityView;
    private EditText supplierView;
    private EditText phoneView;
    private ImageView buyView;
    private ImageView sellView;
    private ImageView callView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_activity);

        // setup the views
        productView = findViewById(R.id.product);
        priceView = findViewById(R.id.price);
        quantityView = findViewById(R.id.quantity);
        supplierView = findViewById(R.id.supplier);
        phoneView = findViewById(R.id.phone);

        setupDirtyFlag();

        setupLocales();
        setupKeyboard();
        setupFieldValidation();

        setupButtons();

        hideCustomKeyboard(keyboardView);
        hideKeyboard(productView);

        // init form
        if (savedInstanceState == null) {// initial state
            // setup the form
            Intent intent = getIntent();
            mUri = intent.getData();
            editMode = false;
            restoreMode();
        } else {// after rotation
            mUri = savedInstanceState.getParcelable(URI);
            editMode = savedInstanceState.getBoolean(MODE);
            restoreMode();
            dataChanged = savedInstanceState.getBoolean(CHANGED);
        }
    }

    // save local state on rotation
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(URI, mUri);
        outState.putBoolean(MODE, editMode);
        outState.putBoolean(CHANGED, dataChanged);
    }

    // Field validation classes

    // Check that text fields are not empty
    private static class TextValidation implements View.OnFocusChangeListener, TextView.OnEditorActionListener {

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            EditText field = (EditText) v;
            if (!hasFocus) {
                toValue(field);
                hideKeyboard(v);
            } else showKeyboard(v);
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            EditText field = (EditText) v;
            toValue(field);
            return false;
        }

        // return the String value of the field if valid and set the error if not
        static String toValue(EditText field) {
            String data = field.getText().toString().trim();
            if (TextUtils.isEmpty(data)) {
                field.setError(field.getContext().getString(R.string.not_empty));
                return null;
            }
            return data;
        }
    }

    // Check that price field is a positive fractional number
    private static class PriceValidation implements View.OnClickListener, View.OnTouchListener, View.OnFocusChangeListener, TextView.OnEditorActionListener {
        private static Locale mLocale;
        private KeyboardView kv;

        PriceValidation(Locale locale, KeyboardView keyboardView) {
            mLocale = locale;
            kv = keyboardView;
        }


        public boolean onTouch(View v, MotionEvent event) {
            EditText edittext = (EditText) v;
            int inType = edittext.getInputType();       // Backup the input type
            edittext.setInputType(InputType.TYPE_NULL); // Disable standard keyboard
            edittext.onTouchEvent(event);               // Call native handler
            edittext.setInputType(inType);              // Restore input type
            return true; // Consume touch event
        }


        @Override
        public void onClick(View v) {
            showCustomKeyboard(kv, v);
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            EditText field = (EditText) v;
            if (!hasFocus) {
                hideCustomKeyboard(kv);
                toValue(field);
            } else showCustomKeyboard(kv, v);
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            EditText field = (EditText) v;
            toValue(field);
            return false;
        }

        // try conversion to number and return null if not successful
        private static Number toNumber(String data) {
            Number result = null;
            ParsePosition pos = new ParsePosition(0);
            NumberFormat nf = NumberFormat.getNumberInstance(mLocale);
            nf.setGroupingUsed(true);

            result = nf.parse(data, pos);
            if (pos.getErrorIndex() != -1 || pos.getIndex() != data.length()) {
                result = null;
            }
            return result;
        }

        // set the given value to the field
        static void setValue(EditText field, double value) {
            NumberFormat nf = NumberFormat.getNumberInstance(mLocale);
            nf.setGroupingUsed(true);
            field.setText(nf.format(value));
        }

        // return the Number value of the field if valid and set the error if not
        static Number toValue(EditText field) {
            String data = field.getText().toString().trim();
            Number value = toNumber(data);
            if (value == null) field.setError(field.getContext().getString(R.string.is_number));
            else {
                double doubleValue = value.doubleValue();
                if (doubleValue < 0) {
                    field.setError(field.getContext().getString(R.string.is_positive));
                    return null;
                } else setValue(field, doubleValue);
            }
            return value;
        }
    }

    private static class QuantityValidation implements View.OnClickListener, View.OnTouchListener, View.OnFocusChangeListener, TextView.OnEditorActionListener {
        private static Locale mLocale;
        private KeyboardView kv;

        QuantityValidation(Locale locale, KeyboardView keyboardView) {
            mLocale = locale;
            kv = keyboardView;
        }

        public boolean onTouch(View v, MotionEvent event) {
            EditText edittext = (EditText) v;
            int inType = edittext.getInputType();       // Backup the input type
            edittext.setInputType(InputType.TYPE_NULL); // Disable standard keyboard
            edittext.onTouchEvent(event);               // Call native handler
            edittext.setInputType(inType);              // Restore input type
            return true; // Consume touch event
        }

        @Override
        public void onClick(View v) {
            showCustomKeyboard(kv, v);
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            EditText field = (EditText) v;
            if (!hasFocus) {
                hideCustomKeyboard(kv);
                toValue(field);
            } else showCustomKeyboard(kv, v);
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            EditText field = (EditText) v;
            toValue(field);
            return false;
        }

        // try conversion to number and return null if not successful
        private static Number toNumber(String data) {
            Number result = null;
            ParsePosition pos = new ParsePosition(0);

            NumberFormat nf = NumberFormat.getIntegerInstance(mLocale);
            nf.setGroupingUsed(true);

            result = nf.parse(data, pos);
            if (pos.getErrorIndex() != -1 || pos.getIndex() != data.length()) {
                result = null;
            }
            return result;
        }

        // set the given value to the field
        static void setValue(EditText field, long value) {
            NumberFormat nf = NumberFormat.getIntegerInstance(mLocale);
            nf.setGroupingUsed(true);
            field.setText(nf.format(value));
        }

        // return the Number value of the field if valid and set the error if not
        static Number toValue(EditText field) {
            String data = field.getText().toString().trim();
            Number value = toNumber(data);
            if (value == null) field.setError(field.getContext().getString(R.string.is_number));
            else {
                long longValue = value.longValue();
                if (longValue < 0) {
                    field.setError(field.getContext().getString(R.string.is_positive));
                    return null;
                } else setValue(field, longValue);
            }
            return value;
        }
    }


    // setup methods


    private void setupFieldValidation() {
        TextValidation textValidation = new TextValidation();
        PriceValidation priceValidation = new PriceValidation(locale, keyboardView);
        QuantityValidation quantityValidation = new QuantityValidation(locale, keyboardView);

        productView.setOnFocusChangeListener(textValidation);
        productView.setOnEditorActionListener(textValidation);
        priceView.setOnFocusChangeListener(priceValidation);
        priceView.setOnEditorActionListener(priceValidation);
        priceView.setOnClickListener(priceValidation);
        priceView.setOnTouchListener(priceValidation);
        quantityView.setOnFocusChangeListener(quantityValidation);
        quantityView.setOnEditorActionListener(quantityValidation);
        quantityView.setOnClickListener(quantityValidation);
        quantityView.setOnTouchListener(quantityValidation);
        supplierView.setOnFocusChangeListener(textValidation);
        supplierView.setOnEditorActionListener(textValidation);
        phoneView.setOnFocusChangeListener(textValidation);
        phoneView.setOnEditorActionListener(textValidation);
    }

    // setup the buttons
    private void setupButtons() {
        buyView = findViewById(R.id.buy);
        sellView = findViewById(R.id.sell);
        callView = findViewById(R.id.call);
        buyView.setOnClickListener(new Listener(0));
        sellView.setOnClickListener(new Listener(1));
        callView.setOnClickListener(new Listener(2));
    }

    // Keep dataChanged updated
    private void setupDirtyFlag() {
        // setup dirty flag maintenance
        productView.addTextChangedListener(this);
        priceView.addTextChangedListener(this);
        quantityView.addTextChangedListener(this);
        supplierView.addTextChangedListener(this);
        phoneView.addTextChangedListener(this);
    }



    // state setup

    private void setupLocales() {
        locale = Locale.getDefault();

        productView.setTextLocale(locale);
        priceView.setTextLocale(locale);
        quantityView.setTextLocale(locale);
        supplierView.setTextLocale(locale);
        phoneView.setTextLocale(locale);
    }

    // Custom numeric keyboard routines

    // Source https://inducesmile.com/android/how-to-create-an-android-custom-keyboard-application/

    private class CustomKeyboard implements KeyboardView.OnKeyboardActionListener {


        @Override
        public void onKey(int primaryCode, int[] keyCodes) {
            View v = getCurrentFocus();
            View next;
            int flagsDown = KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE;
            int flagsUp = KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE;
            int scancode = 0;
            switch (primaryCode) {
                case -1:
                    next = v.focusSearch(View.FOCUS_DOWN);
                    next.requestFocusFromTouch();
                    break;
                case -2:
                    next = v.focusSearch(View.FOCUS_UP);
                    next.requestFocusFromTouch();
                    break;
                case KeyEvent.KEYCODE_ENTER:
                default:// dispatch the key event
                    long eventTime = SystemClock.uptimeMillis();
                    KeyEvent eventDown = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, primaryCode, 0, 0, 0, scancode, flagsDown, InputDevice.SOURCE_KEYBOARD);
                    KeyEvent eventUp = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, primaryCode, 0, 0, 0, scancode, flagsUp, InputDevice.SOURCE_KEYBOARD);

                    dispatchKeyEvent(eventDown);
                    dispatchKeyEvent(eventUp);
            }
        }

        @Override
        public void onPress(int primaryCode) {

        }

        @Override
        public void onRelease(int primaryCode) {

        }

        @Override
        public void onText(CharSequence text) {

        }

        @Override
        public void swipeLeft() {
            hideCustomKeyboard(keyboardView);
        }

        @Override
        public void swipeRight() {
            hideCustomKeyboard(keyboardView);
        }

        @Override
        public void swipeDown() {
            hideCustomKeyboard(keyboardView);
        }

        @Override
        public void swipeUp() {
            hideCustomKeyboard(keyboardView);
        }
    }

    private void setupKeyboard() {

        keyboardView = findViewById(R.id.keyboard_view);
        Keyboard kb = new Keyboard(this, R.xml.keyboard);
        keyboardView.setKeyboard(kb);
        keyboardView.setOnKeyboardActionListener(new CustomKeyboard());

    }

    // custom keyboard actions
    private static void hideCustomKeyboard(KeyboardView keyboardView) {
        keyboardView.setVisibility(View.GONE);
        keyboardView.setEnabled(false);
    }

    private static void showCustomKeyboard(KeyboardView keyboardView, View v) {
        keyboardView.setVisibility(View.VISIBLE);
        keyboardView.setEnabled(true);
        if (v != null) {
            hideKeyboard(v);
        }
    }

    private boolean isCustomKeyboardVisible() {
        return keyboardView.getVisibility() == View.VISIBLE;
    }

    // soft keyboard actions
    private static void showKeyboard(View v) {
        Context context = v.getContext();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(v, 0);
    }

    private static void hideKeyboard(View v) {
        Context context = v.getContext();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }


    //  needs mUri, editMode set
    private void restoreMode() {//first entry point
        if (mUri == null) addSetup();
        else {
            //if in view/edit mode, also bind form to data
            getLoaderManager().initLoader(0, null, this);
            if (editMode) {
                editSetup();
            } else {
                viewSetup();
            }
        }
    }

    private void enableFields(boolean enabled) {
        // enable the edit fields
        productView.setEnabled(enabled);
        priceView.setEnabled(enabled);
        quantityView.setEnabled(enabled);
        supplierView.setEnabled(enabled);
        phoneView.setEnabled(enabled);

        productView.setFocusableInTouchMode(enabled);
        priceView.setFocusableInTouchMode(enabled);
        quantityView.setFocusableInTouchMode(enabled);
        supplierView.setFocusableInTouchMode(enabled);
        phoneView.setFocusableInTouchMode(enabled);
    }

    private void enableButtons(boolean enabled) {
        buyView.setEnabled(enabled);
        buyView.setVisibility(enabled ? View.VISIBLE : View.GONE);
        sellView.setEnabled(enabled);
        sellView.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }


    // clear error messages
    private void clearErrors() {
        productView.setError(null, null);
        priceView.setError(null, null);
        quantityView.setError(null, null);
        supplierView.setError(null, null);
        phoneView.setError(null, null);
    }

    // state initialization routines

    private void addSetup() {
        clearErrors();
        // move to first field
        productView.requestFocusFromTouch();
        setTitle(R.string.add_title);
        enableFields(true);// enable the edit fields
        enableButtons(false);// disable the buy/sell buttons
        editMode = false;
        dataChanged = false;
        invalidateOptionsMenu(); // refresh the menu
    }

    private void clearSelection() {
        View v = getCurrentFocus();
        if (v instanceof EditText) {
            ((EditText) v).setSelection(0, 0);
        }
    }
    private void viewSetup() {
        clearSelection();
        clearErrors();

        //hide the keyboards in view mode
        hideKeyboard(productView);
        hideCustomKeyboard(keyboardView);

        setTitle(R.string.view_title);
        enableFields(false);// disable the edit fields
        enableButtons(true);// enable the buy/sell buttons
        editMode = false;
        dataChanged = false;
        invalidateOptionsMenu(); // refresh the menu
    }

    private void editSetup() {
        clearErrors();

        // move to first field
        productView.requestFocusFromTouch();
        setTitle(R.string.edit_title);
        enableFields(true);// enable the edit fields
        enableButtons(false);// disable the buy/sell buttons
        editMode = true;
        dataChanged = false;
        invalidateOptionsMenu();//refresh the menu
    }


    // Watch for changes in the form fields
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (editMode || mUri == null) dataChanged = true;// mark form as dirty
    }


    // Button handling
    private class Listener implements View.OnClickListener {
        private int which;

        Listener(int which) {
            this.which = which;
        }

        @Override
        public void onClick(View v) {
            switch (which) {
                case 0:
                    buy();
                    break;
                case 1:
                    sell();
                    break;
                case 2:
                    call();
                    break;
            }
        }
    }

    // Menu handling

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.view_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItem;
        if (mUri == null) { // add mode
            menuItem = menu.findItem(R.id.delete);
            menuItem.setVisible(false);
            menuItem = menu.findItem(R.id.edit);
            menuItem.setVisible(false);
            menuItem = menu.findItem(R.id.save);
            menuItem.setVisible(false);
            menuItem = menu.findItem(R.id.add);
            menuItem.setVisible(true);
        } else {
            if (editMode) { //edit mode
                menuItem = menu.findItem(R.id.delete);
                menuItem.setVisible(false);
                menuItem = menu.findItem(R.id.edit);
                menuItem.setVisible(false);
                menuItem = menu.findItem(R.id.save);
                menuItem.setVisible(true);
                menuItem = menu.findItem(R.id.add);
                menuItem.setVisible(false);

            } else { // view mode
                menuItem = menu.findItem(R.id.delete);
                menuItem.setVisible(true);
                menuItem = menu.findItem(R.id.edit);
                menuItem.setVisible(true);
                menuItem = menu.findItem(R.id.save);
                menuItem.setVisible(false);
                menuItem = menu.findItem(R.id.add);
                menuItem.setVisible(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:// insert data & go in view mode
                if (save()) {// second entry point
                    // bind form to data
                    getLoaderManager().initLoader(0, null, this);
                    viewSetup();
                }
                break;
            case R.id.delete:// delete record & go to main screen
                showConfirmationDialog(R.string.delete_dialog_msg, R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        delete();
                        finish();
                    }
                }, R.string.cancel);
                break;
            case R.id.save:// update record & go in view mode
                if (save()) viewSetup();
                break;
            case R.id.edit:// go in edit mode
                edit();
                break;
            case android.R.id.home://must use navigateUpFromSameTask instead of finish

                if (!dataChanged) {
                    if (editMode) viewSetup();//no need to discard changes
                    else NavUtils.navigateUpFromSameTask(this);
                    return true;
                }

                // Show a dialog that notifies the user they have unsaved changes
                showConfirmationDialog(R.string.unsaved_changes_dialog_msg, R.string.discard, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        if (dialog != null)
                            dialog.dismiss();

                        if (editMode) {
                            getContentResolver().notifyChange(mUri, null);// discard changes
                            viewSetup();// go into view mode
                        } else NavUtils.navigateUpFromSameTask(ViewActivity.this);
                    }
                }, R.string.keep_editing);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    // back button handling

    @Override
    public void onBackPressed() {// must use finish

        if (isCustomKeyboardVisible()) {
            hideCustomKeyboard(keyboardView);
            return;
        }

        if (!dataChanged) {
            if (editMode) viewSetup();// no need to discard changes
            else super.onBackPressed();
            return;
        }

        // Show dialog that there are unsaved changes
        showConfirmationDialog(R.string.unsaved_changes_dialog_msg, R.string.discard, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                if (dialog != null)
                    dialog.dismiss();
                if (editMode) {
                    getContentResolver().notifyChange(mUri, null);// discard changes
                    viewSetup();//go into view mode
                } else finish();
            }
        }, R.string.keep_editing);
    }

    // User actions

    private void buy() {
        if (mUri == null) return;
        int count = 0;
        final ContentResolver resolver = getContentResolver();
        final String message = getString(R.string.buy_item_error);
        try { // on UI thread
            count = resolver.update(Uri.parse(String.format("%s/%d/%d", InventoryEntry.BUY_URI, 1, ContentUris.parseId(mUri))), null, null, null);
        } catch (SQLiteConstraintException e) {
            count = 1;
            Toast.makeText(this, String.format(message, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }
        if (count == 0)
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
    }

    private void sell() {
        if (mUri == null) return;
        int count = 0;
        final ContentResolver resolver = getContentResolver();
        final String message = getString(R.string.sell_item_error);
        try { // on UI thread
            count = resolver.update(Uri.parse(String.format("%s/%d/%d", InventoryEntry.SELL_URI, 1, ContentUris.parseId(mUri))), null, null, null);
        } catch (SQLiteConstraintException e) {
            count = 1;
            Toast.makeText(this, String.format(message, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }
        if (count == 0)
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
    }

    private void call() {
        Intent intent = new Intent();
        intent.setData(Uri.parse(String.format("tel:%s", phoneView.getText().toString().trim())));
        intent.setAction(Intent.ACTION_DIAL);
        startActivity(intent);
    }

    private void edit() {
        editSetup();
    }


    private void delete() {
        if (mUri == null) return;
        final ContentResolver resolver = getContentResolver();
        final String message = getString(R.string.validation_error);
        int count = 0;
        try {
            count = resolver.delete(mUri, null, null);
        } catch (SQLiteConstraintException e) {
            count = -1;
            Toast.makeText(this, String.format(message, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }
        if (count == 0) Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_LONG).show();
        else if (count > 0) Toast.makeText(this, R.string.delete_success, Toast.LENGTH_LONG).show();
    }

    private boolean save() {
        // get the data from fields
        final String product = TextValidation.toValue(productView);
        final Number price = PriceValidation.toValue(priceView);
        final Number quantity = QuantityValidation.toValue(quantityView);
        final String supplier = TextValidation.toValue(supplierView);
        final String phone = TextValidation.toValue(phoneView);
        final Object[] data = {product, price, quantity, supplier, phone};
        final EditText[] fields = {productView, priceView, quantityView, supplierView, phoneView};
        final int[] indices = {0, 1, 2, 3, 4};

        // check if any field value is null
        for (int i : indices) {
            if (data[i] == null) {
                fields[i].requestFocusFromTouch();
                Toast.makeText(this, R.string.invalid_data, Toast.LENGTH_LONG).show();
                return false;
            }
        }

        // save data
        final ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_INVENTORY_PRODUCT, product);
        values.put(InventoryEntry.COLUMN_INVENTORY_PRICE, price.doubleValue());
        values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, quantity.longValue());
        values.put(InventoryEntry.COLUMN_INVENTORY_SUPPLIER, supplier);
        values.put(InventoryEntry.COLUMN_INVENTORY_PHONE, phone);

        if (mUri != null) {
            int count = 0;
            try {
                count = resolver.update(mUri, values, null, null);
            } catch (SQLiteConstraintException e) {
                Toast.makeText(this, String.format(getString(R.string.validation_error), e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                return false;
            }
            if (count == 0)
                Toast.makeText(this, getString(R.string.update_failed), Toast.LENGTH_LONG).show();
            else {
                Toast.makeText(this, getString(R.string.update_success), Toast.LENGTH_LONG).show();
                return true;
            }
        } else {
            try {
                mUri = resolver.insert(InventoryEntry.CONTENT_URI, values);
            } catch (SQLiteConstraintException e) {
                Toast.makeText(this, String.format(getString(R.string.validation_error), e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
                return false;
            }
            if (mUri == null)
                Toast.makeText(this, R.string.insert_failed, Toast.LENGTH_LONG).show();
            else {
                Toast.makeText(this, R.string.insert_success, Toast.LENGTH_LONG).show();
                return true;
            }
        }
        return false;
    }

    // Cursor binding to the form

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != 0) return null;
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_INVENTORY_PRODUCT,
                InventoryEntry.COLUMN_INVENTORY_PRICE,
                InventoryEntry.COLUMN_INVENTORY_QUANTITY,
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER,
                InventoryEntry.COLUMN_INVENTORY_PHONE
        };
        return new CursorLoader(this, mUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {

            // get data from cursor
            final String product = data.getString(1);
            final Double price = data.getDouble(2);
            final Long quantity = data.getLong(3);
            final String supplier = data.getString(4);
            final String phone = data.getString(5);

            // set the values to fields
            productView.setText(product.trim());
            PriceValidation.setValue(priceView, price);
            QuantityValidation.setValue(quantityView, quantity);
            supplierView.setText(supplier.trim());
            phoneView.setText(phone.trim());
            clearErrors();//must clear errors after we restore the data from database
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        productView.setText(null);
        priceView.setText(null);
        quantityView.setText(null);
        supplierView.setText(null);
        phoneView.setText(null);
        clearErrors();
    }

    // show a confirmation dialog
    private void showConfirmationDialog(int message, int positive, DialogInterface.OnClickListener positiveListener, int negative) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton(positive, positiveListener);
        builder.setNegativeButton(negative, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //dismiss the dialog on negative action
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}

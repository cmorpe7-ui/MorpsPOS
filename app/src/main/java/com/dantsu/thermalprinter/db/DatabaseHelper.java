package com.dantsu.thermalprinter.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dantsu.thermalprinter.MainActivity;
import com.dantsu.thermalprinter.model.Product;
import com.dantsu.thermalprinter.model.SaleTransaction;
import com.dantsu.thermalprinter.model.SaleTransactionItem;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "morps_pos.db";
    private static final int DATABASE_VERSION = 2;

    // PRODUCTS TABLE
    public static final String TABLE_PRODUCTS = "products";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_COST = "cost";
    public static final String COLUMN_STOCK = "stock";
    public static final String COLUMN_MIN_THRESHOLD = "min_threshold";

    // SALES TABLE
    public static final String TABLE_SALES = "sales";
    public static final String COLUMN_SALE_ID = "id";
    public static final String COLUMN_RECEIPT_NO = "receipt_no";
    public static final String COLUMN_DATE_TIME = "date_time";
    public static final String COLUMN_DATE_ONLY = "date_only";
    public static final String COLUMN_TOTAL_AMOUNT = "total_amount";
    public static final String COLUMN_PAYMENT_MODE = "payment_mode";
    public static final String COLUMN_AMOUNT_PAID = "amount_paid";
    public static final String COLUMN_CHANGE_AMOUNT = "change_amount";
    public static final String COLUMN_CUSTOMER_NOTE = "customer_note";
    public static final String COLUMN_IS_SETTLED = "is_settled";

    // SALE ITEMS TABLE
    public static final String TABLE_SALE_ITEMS = "sale_items";
    public static final String COLUMN_ITEM_ID = "id";
    public static final String COLUMN_ITEM_SALE_ID = "sale_id";
    public static final String COLUMN_ITEM_NAME = "product_name";
    public static final String COLUMN_ITEM_UNIT_PRICE = "unit_price";
    public static final String COLUMN_ITEM_QUANTITY = "quantity";
    public static final String COLUMN_ITEM_SUBTOTAL = "subtotal";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createProductsTable = "CREATE TABLE " + TABLE_PRODUCTS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT NOT NULL, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_PRICE + " REAL NOT NULL, " +
                COLUMN_COST + " REAL NOT NULL DEFAULT 0.0, " +
                COLUMN_STOCK + " INTEGER NOT NULL DEFAULT 0, " +
                COLUMN_MIN_THRESHOLD + " INTEGER NOT NULL DEFAULT 5" +
                ");";
        db.execSQL(createProductsTable);

        createSalesTables(db);
        seedDefaultProducts(db);
    }

    private void createSalesTables(SQLiteDatabase db) {
        String createSalesTable = "CREATE TABLE IF NOT EXISTS " + TABLE_SALES + " (" +
                COLUMN_SALE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_RECEIPT_NO + " TEXT NOT NULL UNIQUE, " +
                COLUMN_DATE_TIME + " TEXT NOT NULL, " +
                COLUMN_DATE_ONLY + " TEXT NOT NULL, " +
                COLUMN_TOTAL_AMOUNT + " REAL NOT NULL, " +
                COLUMN_PAYMENT_MODE + " TEXT NOT NULL, " +
                COLUMN_AMOUNT_PAID + " REAL NOT NULL, " +
                COLUMN_CHANGE_AMOUNT + " REAL NOT NULL, " +
                COLUMN_CUSTOMER_NOTE + " TEXT, " +
                COLUMN_IS_SETTLED + " INTEGER DEFAULT 1" +
                ");";
        db.execSQL(createSalesTable);

        String createSaleItemsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_SALE_ITEMS + " (" +
                COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ITEM_SALE_ID + " INTEGER NOT NULL, " +
                COLUMN_ITEM_NAME + " TEXT NOT NULL, " +
                COLUMN_ITEM_UNIT_PRICE + " REAL NOT NULL, " +
                COLUMN_ITEM_QUANTITY + " INTEGER NOT NULL, " +
                COLUMN_ITEM_SUBTOTAL + " REAL NOT NULL" +
                ");";
        db.execSQL(createSaleItemsTable);
    }

    private void seedDefaultProducts(SQLiteDatabase db) {
        insertSeed(db, "Pancit Canton", "Noodles", 18.00, 14.50, 36, 8);
        insertSeed(db, "Coke Mismo", "Beverages", 20.00, 16.00, 24, 6);
        insertSeed(db, "Nescafe 3-in-1", "Coffee", 15.00, 12.00, 40, 10);
        insertSeed(db, "Kopiko Blanca", "Coffee", 15.00, 12.00, 35, 10);
        insertSeed(db, "Egg (pc)", "Poultry", 9.00, 7.50, 60, 15);
        insertSeed(db, "Rice 1kg", "Grains", 52.00, 46.00, 25, 5);
        insertSeed(db, "Chips / Snack", "Snacks", 15.00, 12.00, 18, 5);
        insertSeed(db, "Bath Soap", "Hygiene", 25.00, 20.00, 12, 4);
        insertSeed(db, "Canned Sardines", "Canned", 24.00, 19.50, 20, 5);
        insertSeed(db, "Bottled Water", "Beverages", 15.00, 10.00, 30, 8);
        insertSeed(db, "Cigarettes", "Tobacco", 10.00, 7.50, 50, 10);
        insertSeed(db, "Candy / Sweets", "Sweets", 2.00, 1.20, 100, 20);
    }

    private void insertSeed(SQLiteDatabase db, String name, String category, double price, double cost, int stock, int minThreshold) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_NAME, name);
        cv.put(COLUMN_CATEGORY, category);
        cv.put(COLUMN_PRICE, price);
        cv.put(COLUMN_COST, cost);
        cv.put(COLUMN_STOCK, stock);
        cv.put(COLUMN_MIN_THRESHOLD, minThreshold);
        db.insert(TABLE_PRODUCTS, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createSalesTables(db);
        }
    }

    /*==============================================================================================
    ======================================PRODUCTS METHODS==========================================
    ==============================================================================================*/

    public long addProduct(Product p) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_NAME, p.getName());
        cv.put(COLUMN_CATEGORY, p.getCategory());
        cv.put(COLUMN_PRICE, p.getPrice());
        cv.put(COLUMN_COST, p.getCost());
        cv.put(COLUMN_STOCK, p.getStock());
        cv.put(COLUMN_MIN_THRESHOLD, p.getMinThreshold());
        return db.insert(TABLE_PRODUCTS, null, cv);
    }

    public int updateProduct(Product p) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_NAME, p.getName());
        cv.put(COLUMN_CATEGORY, p.getCategory());
        cv.put(COLUMN_PRICE, p.getPrice());
        cv.put(COLUMN_COST, p.getCost());
        cv.put(COLUMN_STOCK, p.getStock());
        cv.put(COLUMN_MIN_THRESHOLD, p.getMinThreshold());
        return db.update(TABLE_PRODUCTS, cv, COLUMN_ID + " = ?", new String[]{String.valueOf(p.getId())});
    }

    public int deleteProduct(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_PRODUCTS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public int adjustStock(long id, int delta) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COLUMN_STOCK}, COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int currentStock = cursor.getInt(0);
            cursor.close();
            int newStock = Math.max(0, currentStock + delta);
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_STOCK, newStock);
            return db.update(TABLE_PRODUCTS, cv, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        }
        if (cursor != null) cursor.close();
        return 0;
    }

    public boolean deductStockByName(String name, int quantitySold) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COLUMN_ID, COLUMN_STOCK}, "LOWER(" + COLUMN_NAME + ") = ?", new String[]{name.toLowerCase().trim()}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            long id = cursor.getLong(0);
            int currentStock = cursor.getInt(1);
            cursor.close();
            int newStock = Math.max(0, currentStock - quantitySold);
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_STOCK, newStock);
            db.update(TABLE_PRODUCTS, cv, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
            return true;
        }
        if (cursor != null) cursor.close();
        return false;
    }

    public List<Product> getAllProducts(String searchQuery, String categoryFilter) {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder selection = new StringBuilder();
        List<String> args = new ArrayList<>();

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            selection.append(COLUMN_NAME).append(" LIKE ?");
            args.add("%" + searchQuery.trim() + "%");
        }

        if (categoryFilter != null && !categoryFilter.equalsIgnoreCase("All") && !categoryFilter.trim().isEmpty()) {
            if (selection.length() > 0) selection.append(" AND ");
            selection.append(COLUMN_CATEGORY).append(" = ?");
            args.add(categoryFilter.trim());
        }

        String whereClause = selection.length() > 0 ? selection.toString() : null;
        String[] whereArgs = args.isEmpty() ? null : args.toArray(new String[0]);

        Cursor c = db.query(TABLE_PRODUCTS, null, whereClause, whereArgs, null, null, COLUMN_NAME + " ASC");
        if (c != null && c.moveToFirst()) {
            do {
                list.add(cursorToProduct(c));
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    public List<String> getCategories() {
        List<String> list = new ArrayList<>();
        list.add("All");
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT DISTINCT " + COLUMN_CATEGORY + " FROM " + TABLE_PRODUCTS + " ORDER BY " + COLUMN_CATEGORY + " ASC", null);
        if (c != null && c.moveToFirst()) {
            do {
                String cat = c.getString(0);
                if (cat != null && !cat.trim().isEmpty() && !list.contains(cat)) {
                    list.add(cat);
                }
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    public int getLowStockCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS + " WHERE " + COLUMN_STOCK + " <= " + COLUMN_MIN_THRESHOLD, null);
        int count = 0;
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        return count;
    }

    public int getTotalStockCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(" + COLUMN_STOCK + ") FROM " + TABLE_PRODUCTS, null);
        int count = 0;
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        return count;
    }

    public int getProductCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS, null);
        int count = 0;
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        return count;
    }

    public int getTotalStockUnits() {
        return getTotalStockCount();
    }

    public double getTotalInventoryValue() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(" + COLUMN_STOCK + " * " + COLUMN_PRICE + ") FROM " + TABLE_PRODUCTS, null);
        double val = 0.0;
        if (c != null && c.moveToFirst()) {
            val = c.getDouble(0);
            c.close();
        }
        return val;
    }

    private Product cursorToProduct(Cursor c) {
        long id = c.getLong(c.getColumnIndexOrThrow(COLUMN_ID));
        String name = c.getString(c.getColumnIndexOrThrow(COLUMN_NAME));
        String category = c.getString(c.getColumnIndexOrThrow(COLUMN_CATEGORY));
        double price = c.getDouble(c.getColumnIndexOrThrow(COLUMN_PRICE));
        double cost = c.getDouble(c.getColumnIndexOrThrow(COLUMN_COST));
        int stock = c.getInt(c.getColumnIndexOrThrow(COLUMN_STOCK));
        int minThreshold = c.getInt(c.getColumnIndexOrThrow(COLUMN_MIN_THRESHOLD));
        return new Product(id, name, category, price, cost, stock, minThreshold);
    }

    /*==============================================================================================
    ======================================SALES & TRANSACTIONS======================================
    ==============================================================================================*/

    public long recordSale(SaleTransaction sale, List<MainActivity.CartItem> cartItems) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long saleId = -1;
        try {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_RECEIPT_NO, sale.getReceiptNo());
            cv.put(COLUMN_DATE_TIME, sale.getDateTime());
            cv.put(COLUMN_DATE_ONLY, sale.getDateOnly());
            cv.put(COLUMN_TOTAL_AMOUNT, sale.getTotalAmount());
            cv.put(COLUMN_PAYMENT_MODE, sale.getPaymentMode());
            cv.put(COLUMN_AMOUNT_PAID, sale.getAmountPaid());
            cv.put(COLUMN_CHANGE_AMOUNT, sale.getChangeAmount());
            cv.put(COLUMN_CUSTOMER_NOTE, sale.getCustomerNote());
            cv.put(COLUMN_IS_SETTLED, sale.isSettled() ? 1 : 0);

            saleId = db.insert(TABLE_SALES, null, cv);

            if (saleId != -1 && cartItems != null) {
                for (MainActivity.CartItem item : cartItems) {
                    ContentValues itemCv = new ContentValues();
                    itemCv.put(COLUMN_ITEM_SALE_ID, saleId);
                    itemCv.put(COLUMN_ITEM_NAME, item.getName());
                    itemCv.put(COLUMN_ITEM_UNIT_PRICE, item.getUnitPrice());
                    itemCv.put(COLUMN_ITEM_QUANTITY, item.getQuantity());
                    itemCv.put(COLUMN_ITEM_SUBTOTAL, item.getTotalPrice());
                    db.insert(TABLE_SALE_ITEMS, null, itemCv);

                    // Deduct stock
                    deductStockByName(item.getName(), item.getQuantity());
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return saleId;
    }

    public List<SaleTransaction> getAllSales(boolean creditOnly) {
        List<SaleTransaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = creditOnly ? (COLUMN_PAYMENT_MODE + " = 'CREDIT' AND " + COLUMN_IS_SETTLED + " = 0") : null;
        Cursor c = db.query(TABLE_SALES, null, selection, null, null, null, COLUMN_SALE_ID + " DESC");

        if (c != null && c.moveToFirst()) {
            do {
                SaleTransaction st = new SaleTransaction(
                        c.getLong(c.getColumnIndexOrThrow(COLUMN_SALE_ID)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_RECEIPT_NO)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_DATE_TIME)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_DATE_ONLY)),
                        c.getDouble(c.getColumnIndexOrThrow(COLUMN_TOTAL_AMOUNT)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_PAYMENT_MODE)),
                        c.getDouble(c.getColumnIndexOrThrow(COLUMN_AMOUNT_PAID)),
                        c.getDouble(c.getColumnIndexOrThrow(COLUMN_CHANGE_AMOUNT)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_CUSTOMER_NOTE)),
                        c.getInt(c.getColumnIndexOrThrow(COLUMN_IS_SETTLED)) == 1
                );
                st.setItems(getItemsForSale(st.getId()));
                list.add(st);
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    public List<SaleTransactionItem> getItemsForSale(long saleId) {
        List<SaleTransactionItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_SALE_ITEMS, null, COLUMN_ITEM_SALE_ID + " = ?", new String[]{String.valueOf(saleId)}, null, null, COLUMN_ITEM_ID + " ASC");

        if (c != null && c.moveToFirst()) {
            do {
                list.add(new SaleTransactionItem(
                        c.getLong(c.getColumnIndexOrThrow(COLUMN_ITEM_ID)),
                        c.getLong(c.getColumnIndexOrThrow(COLUMN_ITEM_SALE_ID)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_ITEM_NAME)),
                        c.getDouble(c.getColumnIndexOrThrow(COLUMN_ITEM_UNIT_PRICE)),
                        c.getInt(c.getColumnIndexOrThrow(COLUMN_ITEM_QUANTITY)),
                        c.getDouble(c.getColumnIndexOrThrow(COLUMN_ITEM_SUBTOTAL))
                ));
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    public void settleCredit(long saleId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_IS_SETTLED, 1);
        db.update(TABLE_SALES, cv, COLUMN_SALE_ID + " = ?", new String[]{String.valueOf(saleId)});
    }

    public double getTodaySalesTotal(String dateOnly) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(" + COLUMN_TOTAL_AMOUNT + ") FROM " + TABLE_SALES + " WHERE " + COLUMN_DATE_ONLY + " = ?", new String[]{dateOnly});
        double total = 0.0;
        if (c != null && c.moveToFirst()) {
            total = c.getDouble(0);
            c.close();
        }
        return total;
    }

    public double getTodayCashTotal(String dateOnly) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(" + COLUMN_TOTAL_AMOUNT + ") FROM " + TABLE_SALES + " WHERE " + COLUMN_DATE_ONLY + " = ? AND " + COLUMN_PAYMENT_MODE + " = 'CASH'", new String[]{dateOnly});
        double total = 0.0;
        if (c != null && c.moveToFirst()) {
            total = c.getDouble(0);
            c.close();
        }
        return total;
    }

    public double getTodayGcashTotal(String dateOnly) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(" + COLUMN_TOTAL_AMOUNT + ") FROM " + TABLE_SALES + " WHERE " + COLUMN_DATE_ONLY + " = ? AND " + COLUMN_PAYMENT_MODE + " = 'GCASH'", new String[]{dateOnly});
        double total = 0.0;
        if (c != null && c.moveToFirst()) {
            total = c.getDouble(0);
            c.close();
        }
        return total;
    }

    public double getUnpaidCreditTotal() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(" + COLUMN_TOTAL_AMOUNT + ") FROM " + TABLE_SALES + " WHERE " + COLUMN_PAYMENT_MODE + " = 'CREDIT' AND " + COLUMN_IS_SETTLED + " = 0", null);
        double total = 0.0;
        if (c != null && c.moveToFirst()) {
            total = c.getDouble(0);
            c.close();
        }
        return total;
    }

    public int getTodayOrderCount(String dateOnly) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SALES + " WHERE " + COLUMN_DATE_ONLY + " = ?", new String[]{dateOnly});
        int count = 0;
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        return count;
    }
}

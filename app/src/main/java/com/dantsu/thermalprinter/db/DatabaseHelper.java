package com.dantsu.thermalprinter.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dantsu.thermalprinter.MainActivity;
import com.dantsu.thermalprinter.model.GcashTransaction;
import com.dantsu.thermalprinter.model.Product;
import com.dantsu.thermalprinter.model.SaleTransaction;
import com.dantsu.thermalprinter.model.SaleTransactionItem;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "morps_pos.db";
    private static final int DATABASE_VERSION = 4;

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

    // GCASH TRANSACTIONS TABLE
    public static final String TABLE_GCASH = "gcash_transactions";
    public static final String COLUMN_GCASH_ID = "id";
    public static final String COLUMN_GCASH_TYPE = "type"; // CASH_IN, CASH_OUT
    public static final String COLUMN_GCASH_REF_NO = "ref_no";
    public static final String COLUMN_GCASH_CUSTOMER_NUMBER = "customer_number";
    public static final String COLUMN_GCASH_CUSTOMER_NAME = "customer_name";
    public static final String COLUMN_GCASH_AMOUNT = "amount";
    public static final String COLUMN_GCASH_FEE = "fee";
    public static final String COLUMN_GCASH_TOTAL_CASH = "total_cash_handled";
    public static final String COLUMN_GCASH_DATE_TIME = "date_time";
    public static final String COLUMN_GCASH_DATE_ONLY = "date_only";
    public static final String COLUMN_GCASH_NOTE = "note";
    public static final String COLUMN_GCASH_STATUS = "status"; // COMPLETED, VOIDED

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        if (instance != null) {
            try {
                instance.close();
            } catch (Exception ignored) {}
            instance = null;
        }
    }

    public void checkpoint() {
        try {
            SQLiteDatabase db = getWritableDatabase();
            if (db != null && db.isOpen()) {
                Cursor c = db.rawQuery("PRAGMA wal_checkpoint(FULL);", null);
                if (c != null) {
                    c.close();
                }
            }
        } catch (Exception ignored) {}
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
        createGcashTables(db);
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

    private void createGcashTables(SQLiteDatabase db) {
        String createGcashTable = "CREATE TABLE IF NOT EXISTS " + TABLE_GCASH + " (" +
                COLUMN_GCASH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_GCASH_TYPE + " TEXT NOT NULL, " +
                COLUMN_GCASH_REF_NO + " TEXT, " +
                COLUMN_GCASH_CUSTOMER_NUMBER + " TEXT, " +
                COLUMN_GCASH_CUSTOMER_NAME + " TEXT, " +
                COLUMN_GCASH_AMOUNT + " REAL NOT NULL, " +
                COLUMN_GCASH_FEE + " REAL NOT NULL DEFAULT 0.0, " +
                COLUMN_GCASH_TOTAL_CASH + " REAL NOT NULL, " +
                COLUMN_GCASH_DATE_TIME + " TEXT NOT NULL, " +
                COLUMN_GCASH_DATE_ONLY + " TEXT NOT NULL, " +
                COLUMN_GCASH_NOTE + " TEXT, " +
                COLUMN_GCASH_STATUS + " TEXT NOT NULL DEFAULT 'COMPLETED'" +
                ");";
        db.execSQL(createGcashTable);
    }

    private void seedDefaultProducts(SQLiteDatabase db) {
        // NOODLES
        insertSeed(db, "Lucky Me! Pancit Canton Kalamansi", "Noodles", 18.00, 14.50, 48, 10);
        insertSeed(db, "Lucky Me! Pancit Canton Chilimansi", "Noodles", 18.00, 14.50, 48, 10);
        insertSeed(db, "Lucky Me! Pancit Canton Original", "Noodles", 18.00, 14.50, 36, 8);
        insertSeed(db, "Lucky Me! Pancit Canton Sweet & Spicy", "Noodles", 18.00, 14.50, 36, 8);
        insertSeed(db, "Lucky Me! Pancit Canton Hot Chili", "Noodles", 18.00, 14.50, 36, 8);
        insertSeed(db, "Lucky Me! Chicken Mami", "Noodles", 16.00, 12.50, 36, 8);
        insertSeed(db, "Lucky Me! Beef Mami", "Noodles", 16.00, 12.50, 36, 8);
        insertSeed(db, "Payless Pancit Canton Extra Big", "Noodles", 22.00, 17.50, 24, 6);

        // COFFEE & DRINK MIXES
        insertSeed(db, "Kopiko Blanca Twin Pack", "Coffee", 16.00, 12.50, 50, 12);
        insertSeed(db, "Kopiko Brown Twin Pack", "Coffee", 16.00, 12.50, 50, 12);
        insertSeed(db, "Kopiko Black Twin Pack", "Coffee", 16.00, 12.50, 40, 10);
        insertSeed(db, "Nescafe 3-in-1 Original Twin Pack", "Coffee", 16.00, 12.50, 45, 10);
        insertSeed(db, "Nescafe Classic Sachet", "Coffee", 6.00, 4.80, 60, 15);
        insertSeed(db, "Milo Sachet 24g", "Coffee", 12.00, 9.50, 40, 10);
        insertSeed(db, "Bear Brand Powdered Milk Sachet", "Coffee", 15.00, 12.00, 50, 12);
        insertSeed(db, "Tang Orange Juice Sachet", "Coffee", 22.00, 18.00, 30, 6);

        // BEVERAGES
        insertSeed(db, "Coke Mismo 290ml", "Beverages", 20.00, 16.00, 36, 8);
        insertSeed(db, "Sprite Mismo 290ml", "Beverages", 20.00, 16.00, 24, 6);
        insertSeed(db, "Royal Tru-Orange Mismo 290ml", "Beverages", 20.00, 16.00, 24, 6);
        insertSeed(db, "Mountain Dew 290ml", "Beverages", 20.00, 16.00, 24, 6);
        insertSeed(db, "Coke 1.5L", "Beverages", 80.00, 68.00, 15, 4);
        insertSeed(db, "Nature's Spring Bottled Water 500ml", "Beverages", 15.00, 9.50, 36, 8);
        insertSeed(db, "C2 Green Tea Apple 355ml", "Beverages", 18.00, 14.00, 24, 6);
        insertSeed(db, "Red Horse Beer 500ml", "Beverages", 65.00, 54.00, 24, 6);
        insertSeed(db, "San Miguel Pale Pilsen 330ml", "Beverages", 55.00, 45.00, 24, 6);

        // CANNED GOODS
        insertSeed(db, "555 Sardines Tomato Sauce", "Canned Goods", 24.00, 19.50, 30, 6);
        insertSeed(db, "Mega Sardines Green (Chili)", "Canned Goods", 26.00, 21.00, 30, 6);
        insertSeed(db, "Ligo Sardines Red", "Canned Goods", 26.00, 21.50, 24, 6);
        insertSeed(db, "Argentina Corned Beef 150g", "Canned Goods", 42.00, 35.00, 24, 5);
        insertSeed(db, "CDO Karne Norte 150g", "Canned Goods", 32.00, 26.00, 24, 5);
        insertSeed(db, "Century Tuna Flakes in Oil 155g", "Canned Goods", 42.00, 34.50, 24, 5);
        insertSeed(db, "CDO Meat Loaf 150g", "Canned Goods", 26.00, 21.00, 20, 4);
        insertSeed(db, "555 Fried Sardines Escabeche", "Canned Goods", 30.00, 24.50, 20, 4);
        insertSeed(db, "Purefoods Corned Beef 150g", "Canned Goods", 75.00, 64.00, 15, 3);
        insertSeed(db, "San Marino Corned Tuna 150g", "Canned Goods", 45.00, 37.00, 20, 4);

        // SNACKS & BISCUITS
        insertSeed(db, "Piattos Cheese 40g", "Snacks", 18.00, 14.50, 25, 5);
        insertSeed(db, "Piattos Sour Cream 40g", "Snacks", 18.00, 14.50, 20, 5);
        insertSeed(db, "Nova Country Cheddar 40g", "Snacks", 18.00, 14.50, 20, 5);
        insertSeed(db, "Chippy Barbecue 27g", "Snacks", 10.00, 7.50, 30, 6);
        insertSeed(db, "Oishi Prawn Crackers 30g", "Snacks", 10.00, 7.50, 25, 5);
        insertSeed(db, "Clover Chips Cheese", "Snacks", 10.00, 7.50, 25, 5);
        insertSeed(db, "Ding Dong Snack Mix 100g", "Snacks", 22.00, 17.50, 20, 4);
        insertSeed(db, "SkyFlakes Crackers Sachet", "Snacks", 8.00, 6.20, 50, 10);
        insertSeed(db, "Rebisco Crackers Sachet", "Snacks", 8.00, 6.20, 40, 8);
        insertSeed(db, "Fudgee Barr Chocolate", "Snacks", 10.00, 7.80, 36, 8);
        insertSeed(db, "Choc-Nut 10s", "Snacks", 20.00, 15.00, 20, 4);

        // CONDIMENTS & COOKING
        insertSeed(db, "Silver Swan Soy Sauce Pouch 200ml", "Condiments", 14.00, 10.50, 25, 5);
        insertSeed(db, "Datu Puti Vinegar Pouch 200ml", "Condiments", 12.00, 9.00, 25, 5);
        insertSeed(db, "Mang Tomas All-Around Sarsa 325g", "Condiments", 38.00, 31.00, 15, 3);
        insertSeed(db, "UFC Banana Ketchup Pouch 200g", "Condiments", 16.00, 12.50, 20, 4);
        insertSeed(db, "Magic Sarap Sachet 8g", "Condiments", 6.00, 4.80, 80, 20);
        insertSeed(db, "Ajinomoto MSG 11g", "Condiments", 5.00, 3.80, 60, 15);
        insertSeed(db, "Knorr Pork Cubes Twin", "Condiments", 14.00, 11.00, 40, 8);
        insertSeed(db, "Knorr Chicken Cubes Twin", "Condiments", 14.00, 11.00, 40, 8);
        insertSeed(db, "Mantika (Cooking Oil) Tingi", "Condiments", 15.00, 11.00, 30, 6);
        insertSeed(db, "Asin (Rock Salt) Pack", "Condiments", 10.00, 6.00, 25, 5);
        insertSeed(db, "White Sugar 1/4 kg", "Condiments", 24.00, 19.00, 25, 5);
        insertSeed(db, "Brown Sugar 1/4 kg", "Condiments", 22.00, 17.50, 25, 5);

        // GRAINS & POULTRY
        insertSeed(db, "Sinandomeng Rice 1kg", "Grains", 54.00, 48.00, 50, 10);
        insertSeed(db, "Dinorado Special Rice 1kg", "Grains", 60.00, 53.00, 30, 8);
        insertSeed(db, "Fresh Egg Large (pc)", "Poultry", 9.50, 7.80, 90, 20);
        insertSeed(db, "Fresh Egg Medium (pc)", "Poultry", 8.50, 7.00, 90, 20);

        // PERSONAL CARE & HYGIENE
        insertSeed(db, "Safeguard White Soap 60g", "Personal Care", 26.00, 21.00, 24, 5);
        insertSeed(db, "Palmolive Shampoo Sachet", "Personal Care", 8.00, 6.20, 60, 15);
        insertSeed(db, "Sunsilk Shampoo Sachet Green", "Personal Care", 8.00, 6.20, 60, 15);
        insertSeed(db, "Head & Shoulders Sachet", "Personal Care", 9.00, 7.00, 50, 10);
        insertSeed(db, "Colgate Toothpaste Sachet", "Personal Care", 12.00, 9.50, 40, 8);
        insertSeed(db, "Modess Regular Pads 8s", "Personal Care", 48.00, 39.00, 15, 4);

        // LAUNDRY & HOUSEHOLD
        insertSeed(db, "Surf Powder Blossom Fresh Sachet", "Laundry", 8.00, 6.20, 60, 15);
        insertSeed(db, "Ariel Detergent Powder Sachet", "Laundry", 14.00, 11.00, 40, 10);
        insertSeed(db, "Champion Detergent Bar (pc)", "Laundry", 20.00, 16.00, 24, 6);
        insertSeed(db, "Downy Sunrise Fresh Sachet", "Laundry", 10.00, 7.80, 60, 15);
        insertSeed(db, "Joy Dishwashing Liquid Sachet", "Laundry", 12.00, 9.50, 40, 10);
        insertSeed(db, "Zonrox Bleach Original 100ml", "Laundry", 14.00, 10.50, 20, 5);
        insertSeed(db, "Posporo (Guitar Matches)", "Household", 4.00, 2.50, 50, 10);
        insertSeed(db, "Kandila (Esperma #3 Candle)", "Household", 6.00, 4.00, 40, 8);

        // TOBACCO & CANDIES
        insertSeed(db, "Marlboro Red (stick)", "Tobacco", 10.00, 8.00, 100, 20);
        insertSeed(db, "Marlboro Gold (stick)", "Tobacco", 10.00, 8.00, 100, 20);
        insertSeed(db, "Winston Red (stick)", "Tobacco", 8.00, 6.50, 80, 15);
        insertSeed(db, "Fortune Blue (stick)", "Tobacco", 7.00, 5.50, 80, 15);
        insertSeed(db, "Maxx Menthol Candy (pc)", "Candies", 2.00, 1.20, 150, 30);
        insertSeed(db, "Mentos Mint Candy (pc)", "Candies", 2.00, 1.20, 100, 25);
        insertSeed(db, "Kopiko Coffee Candy (pc)", "Candies", 2.00, 1.20, 150, 30);
    }

    public void ensureDefaultProductsSeeded() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            seedDefaultProducts(db);
        } catch (Exception ignored) {}
    }

    private void insertSeed(SQLiteDatabase db, String name, String category, double price, double cost, int stock, int minThreshold) {
        Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COLUMN_ID}, "LOWER(" + COLUMN_NAME + ") = ?", new String[]{name.toLowerCase().trim()}, null, null, null);
        if (cursor != null) {
            boolean exists = cursor.moveToFirst();
            cursor.close();
            if (exists) return;
        }

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
        if (oldVersion < 3) {
            createGcashTables(db);
        }
        if (oldVersion < 4) {
            seedDefaultProducts(db);
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

    public void deductStock(SQLiteDatabase db, long productId, String name, int quantitySold) {
        if (productId > 0) {
            Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COLUMN_STOCK}, COLUMN_ID + " = ?", new String[]{String.valueOf(productId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int currentStock = cursor.getInt(0);
                cursor.close();
                int newStock = Math.max(0, currentStock - quantitySold);
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_STOCK, newStock);
                db.update(TABLE_PRODUCTS, cv, COLUMN_ID + " = ?", new String[]{String.valueOf(productId)});
                return;
            }
            if (cursor != null) cursor.close();
        }
        if (name != null && !name.trim().isEmpty()) {
            Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COLUMN_ID, COLUMN_STOCK}, "LOWER(" + COLUMN_NAME + ") = ?", new String[]{name.toLowerCase().trim()}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                int currentStock = cursor.getInt(1);
                cursor.close();
                int newStock = Math.max(0, currentStock - quantitySold);
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_STOCK, newStock);
                db.update(TABLE_PRODUCTS, cv, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
            }
            if (cursor != null) cursor.close();
        }
    }

    public Product getProduct(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_PRODUCTS, null, COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        if (c != null && c.moveToFirst()) {
            Product p = cursorToProduct(c);
            c.close();
            return p;
        }
        if (c != null) c.close();
        return null;
    }

    public int getProductStock(long id, String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        if (id > 0) {
            Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COLUMN_STOCK}, COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        return cursor.getInt(0);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        if (name != null && !name.trim().isEmpty()) {
            Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COLUMN_STOCK}, "LOWER(" + COLUMN_NAME + ") = ?", new String[]{name.toLowerCase().trim()}, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        return cursor.getInt(0);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return -1;
    }

    public List<Product> getAllProducts(String searchQuery, String categoryFilter) {
        return getAllProducts(searchQuery, categoryFilter, "ALL");
    }

    public List<Product> getAllProducts(String searchQuery, String categoryFilter, String stockFilter) {
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

        if ("LOW".equalsIgnoreCase(stockFilter)) {
            if (selection.length() > 0) selection.append(" AND ");
            selection.append(COLUMN_STOCK).append(" > 0 AND ").append(COLUMN_STOCK).append(" <= ").append(COLUMN_MIN_THRESHOLD);
        } else if ("OUT".equalsIgnoreCase(stockFilter)) {
            if (selection.length() > 0) selection.append(" AND ");
            selection.append(COLUMN_STOCK).append(" <= 0");
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

                    // Deduct stock using active db transaction
                    deductStock(db, item.getProductId(), item.getName(), item.getQuantity());
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return saleId;
    }

    public List<SaleTransaction> getAllSales(boolean creditOnly) {
        return getAllSales(creditOnly, null);
    }

    public List<SaleTransaction> getAllSales(boolean creditOnly, String searchQuery) {
        List<SaleTransaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder selection = new StringBuilder();
        List<String> args = new ArrayList<>();

        if (creditOnly) {
            selection.append(COLUMN_PAYMENT_MODE).append(" = 'CREDIT' AND ").append(COLUMN_IS_SETTLED).append(" = 0");
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            if (selection.length() > 0) selection.append(" AND ");
            selection.append("(").append(COLUMN_RECEIPT_NO).append(" LIKE ? OR ").append(COLUMN_CUSTOMER_NOTE).append(" LIKE ?)");
            String q = "%" + searchQuery.trim() + "%";
            args.add(q);
            args.add(q);
        }

        String whereClause = selection.length() > 0 ? selection.toString() : null;
        String[] whereArgs = args.isEmpty() ? null : args.toArray(new String[0]);

        Cursor c = db.query(TABLE_SALES, null, whereClause, whereArgs, null, null, COLUMN_SALE_ID + " DESC");

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
        Cursor c = db.rawQuery("SELECT SUM(" + COLUMN_TOTAL_AMOUNT + ") FROM " + TABLE_SALES + " WHERE " + COLUMN_DATE_ONLY + " = ? AND " + COLUMN_PAYMENT_MODE + " != 'VOID'", new String[]{dateOnly});
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
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SALES + " WHERE " + COLUMN_DATE_ONLY + " = ? AND " + COLUMN_PAYMENT_MODE + " != 'VOID'", new String[]{dateOnly});
        int count = 0;
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        return count;
    }

    /*==============================================================================================
    ================================ADVANCED SALES & PROFIT ANALYTICS===============================
    ==============================================================================================*/

    public static class SalesStats {
        public double grossSales = 0.0;
        public double totalCost = 0.0;
        public double netProfit = 0.0;
        public double profitMarginPercent = 0.0;
        public double cashTotal = 0.0;
        public double gcashTotal = 0.0;
        public double unpaidCreditTotal = 0.0;
        public int orderCount = 0;
    }

    public static class TopSellingItem {
        private final String name;
        private final int quantity;
        private final double revenue;

        public TopSellingItem(String name, int quantity, double revenue) {
            this.name = name;
            this.quantity = quantity;
            this.revenue = revenue;
        }

        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public double getRevenue() { return revenue; }
        public double getTotalRevenue() { return revenue; }
    }

    public static class CustomerDebtor {
        private final String customerName;
        private final double totalOwed;
        private final int orderCount;
        private final String lastDate;

        public CustomerDebtor(String customerName, double totalOwed, int orderCount, String lastDate) {
            this.customerName = customerName;
            this.totalOwed = totalOwed;
            this.orderCount = orderCount;
            this.lastDate = lastDate;
        }

        public String getCustomerName() { return customerName; }
        public double getTotalOwed() { return totalOwed; }
        public int getOrderCount() { return orderCount; }
        public String getLastDate() { return lastDate; }
    }

    public SalesStats getSalesStats(String startDate, String endDate) {
        SalesStats stats = new SalesStats();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder whereClause = new StringBuilder(" WHERE " + COLUMN_PAYMENT_MODE + " != 'VOID'");
        List<String> args = new ArrayList<>();
        if (startDate != null && endDate != null) {
            whereClause.append(" AND " + COLUMN_DATE_ONLY + " >= ? AND " + COLUMN_DATE_ONLY + " <= ?");
            args.add(startDate);
            args.add(endDate);
        }

        String[] argArray = args.isEmpty() ? null : args.toArray(new String[0]);

        // 1. Query Sales Totals & Breakdowns
        String salesSql = "SELECT " +
                "COUNT(*), " +
                "SUM(" + COLUMN_TOTAL_AMOUNT + "), " +
                "SUM(CASE WHEN " + COLUMN_PAYMENT_MODE + " = 'CASH' THEN " + COLUMN_TOTAL_AMOUNT + " ELSE 0 END), " +
                "SUM(CASE WHEN " + COLUMN_PAYMENT_MODE + " = 'GCASH' THEN " + COLUMN_TOTAL_AMOUNT + " ELSE 0 END), " +
                "SUM(CASE WHEN " + COLUMN_PAYMENT_MODE + " = 'CREDIT' AND " + COLUMN_IS_SETTLED + " = 0 THEN " + COLUMN_TOTAL_AMOUNT + " ELSE 0 END) " +
                "FROM " + TABLE_SALES + whereClause.toString();

        Cursor c = db.rawQuery(salesSql, argArray);
        if (c != null && c.moveToFirst()) {
            stats.orderCount = c.getInt(0);
            stats.grossSales = c.getDouble(1);
            stats.cashTotal = c.getDouble(2);
            stats.gcashTotal = c.getDouble(3);
            stats.unpaidCreditTotal = c.getDouble(4);
            c.close();
        }

        // 2. Query Total Cost of Goods Sold from sale_items joined with products
        StringBuilder costSql = new StringBuilder("SELECT SUM(si." + COLUMN_ITEM_QUANTITY + " * IFNULL(p." + COLUMN_COST + ", 0)) " +
                "FROM " + TABLE_SALE_ITEMS + " si " +
                "JOIN " + TABLE_SALES + " s ON si." + COLUMN_ITEM_SALE_ID + " = s." + COLUMN_SALE_ID + " " +
                "LEFT JOIN " + TABLE_PRODUCTS + " p ON si." + COLUMN_ITEM_NAME + " = p." + COLUMN_NAME + " " +
                "WHERE s." + COLUMN_PAYMENT_MODE + " != 'VOID'");

        if (startDate != null && endDate != null) {
            costSql.append(" AND s." + COLUMN_DATE_ONLY + " >= ? AND s." + COLUMN_DATE_ONLY + " <= ?");
        }

        Cursor cc = db.rawQuery(costSql.toString(), argArray);
        if (cc != null && cc.moveToFirst()) {
            stats.totalCost = cc.getDouble(0);
            cc.close();
        }

        stats.netProfit = Math.max(0.0, stats.grossSales - stats.totalCost);
        stats.profitMarginPercent = stats.grossSales > 0 ? (stats.netProfit / stats.grossSales) * 100.0 : 0.0;

        return stats;
    }

    public List<TopSellingItem> getTopSellingItems(String startDate, String endDate, int limit) {
        List<TopSellingItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder sql = new StringBuilder("SELECT si." + COLUMN_ITEM_NAME + ", " +
                "SUM(si." + COLUMN_ITEM_QUANTITY + ") AS total_qty, " +
                "SUM(si." + COLUMN_ITEM_SUBTOTAL + ") AS total_rev " +
                "FROM " + TABLE_SALE_ITEMS + " si " +
                "JOIN " + TABLE_SALES + " s ON si." + COLUMN_ITEM_SALE_ID + " = s." + COLUMN_SALE_ID + " " +
                "WHERE s." + COLUMN_PAYMENT_MODE + " != 'VOID'");

        List<String> args = new ArrayList<>();
        if (startDate != null && endDate != null) {
            sql.append(" AND s." + COLUMN_DATE_ONLY + " >= ? AND s." + COLUMN_DATE_ONLY + " <= ?");
            args.add(startDate);
            args.add(endDate);
        }

        sql.append(" GROUP BY si." + COLUMN_ITEM_NAME + " ORDER BY total_qty DESC LIMIT " + limit);

        Cursor c = db.rawQuery(sql.toString(), args.isEmpty() ? null : args.toArray(new String[0]));
        if (c != null && c.moveToFirst()) {
            do {
                list.add(new TopSellingItem(
                        c.getString(0),
                        c.getInt(1),
                        c.getDouble(2)
                ));
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    public List<CustomerDebtor> getCustomerDebtors() {
        List<CustomerDebtor> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT " +
                "CASE WHEN " + COLUMN_CUSTOMER_NOTE + " IS NULL OR TRIM(" + COLUMN_CUSTOMER_NOTE + ") = '' THEN 'Unnamed Customer' ELSE TRIM(" + COLUMN_CUSTOMER_NOTE + ") END AS debtor, " +
                "SUM(" + COLUMN_TOTAL_AMOUNT + ") AS total_debt, " +
                "COUNT(*) AS order_cnt, " +
                "MAX(" + COLUMN_DATE_TIME + ") AS last_dt " +
                "FROM " + TABLE_SALES + " " +
                "WHERE " + COLUMN_PAYMENT_MODE + " = 'CREDIT' AND " + COLUMN_IS_SETTLED + " = 0 " +
                "GROUP BY debtor " +
                "ORDER BY total_debt DESC";

        Cursor c = db.rawQuery(sql, null);
        if (c != null && c.moveToFirst()) {
            do {
                list.add(new CustomerDebtor(
                        c.getString(0),
                        c.getDouble(1),
                        c.getInt(2),
                        c.getString(3)
                ));
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    public int settleAllCustomerCredit(String customerName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_IS_SETTLED, 1);

        if ("Unnamed Customer".equalsIgnoreCase(customerName)) {
            return db.update(TABLE_SALES, cv,
                    COLUMN_PAYMENT_MODE + " = 'CREDIT' AND " + COLUMN_IS_SETTLED + " = 0 AND (" + COLUMN_CUSTOMER_NOTE + " IS NULL OR TRIM(" + COLUMN_CUSTOMER_NOTE + ") = '')",
                    null);
        } else {
            return db.update(TABLE_SALES, cv,
                    COLUMN_PAYMENT_MODE + " = 'CREDIT' AND " + COLUMN_IS_SETTLED + " = 0 AND TRIM(" + COLUMN_CUSTOMER_NOTE + ") = ?",
                    new String[]{customerName.trim()});
        }
    }

    public boolean voidSale(long saleId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Get all items in this transaction to return stock back to inventory
            List<SaleTransactionItem> items = getItemsForSale(saleId);
            for (SaleTransactionItem item : items) {
                db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET " + COLUMN_STOCK + " = " + COLUMN_STOCK + " + ? WHERE " + COLUMN_NAME + " = ?",
                        new Object[]{item.getQuantity(), item.getItemName()});
            }

            // 2. Mark the transaction as VOIDED
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_PAYMENT_MODE, "VOID");
            cv.put(COLUMN_TOTAL_AMOUNT, 0.0);
            cv.put(COLUMN_CUSTOMER_NOTE, "[VOIDED]");
            db.update(TABLE_SALES, cv, COLUMN_SALE_ID + " = ?", new String[]{String.valueOf(saleId)});

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public boolean deleteSale(long saleId, boolean restockInventory) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            if (restockInventory) {
                Cursor c = db.query(TABLE_SALES, new String[]{COLUMN_PAYMENT_MODE}, COLUMN_SALE_ID + " = ?", new String[]{String.valueOf(saleId)}, null, null, null);
                boolean isAlreadyVoid = false;
                if (c != null && c.moveToFirst()) {
                    isAlreadyVoid = "VOID".equalsIgnoreCase(c.getString(0));
                    c.close();
                }

                if (!isAlreadyVoid) {
                    List<SaleTransactionItem> items = getItemsForSale(saleId);
                    for (SaleTransactionItem item : items) {
                        db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET " + COLUMN_STOCK + " = " + COLUMN_STOCK + " + ? WHERE " + COLUMN_NAME + " = ?",
                                new Object[]{item.getQuantity(), item.getItemName()});
                    }
                }
            }

            db.delete(TABLE_SALE_ITEMS, COLUMN_ITEM_SALE_ID + " = ?", new String[]{String.valueOf(saleId)});
            int deleted = db.delete(TABLE_SALES, COLUMN_SALE_ID + " = ?", new String[]{String.valueOf(saleId)});

            db.setTransactionSuccessful();
            return deleted > 0;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public int deleteAllVoidedSales() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            List<Long> voidIds = new ArrayList<>();
            Cursor c = db.query(TABLE_SALES, new String[]{COLUMN_SALE_ID}, COLUMN_PAYMENT_MODE + " = 'VOID'", null, null, null, null);
            if (c != null && c.moveToFirst()) {
                do {
                    voidIds.add(c.getLong(0));
                } while (c.moveToNext());
                c.close();
            }

            for (Long id : voidIds) {
                db.delete(TABLE_SALE_ITEMS, COLUMN_ITEM_SALE_ID + " = ?", new String[]{String.valueOf(id)});
            }

            int count = db.delete(TABLE_SALES, COLUMN_PAYMENT_MODE + " = 'VOID'", null);
            db.setTransactionSuccessful();
            return count;
        } catch (Exception e) {
            return 0;
        } finally {
            db.endTransaction();
        }
    }

    public int clearSalesHistory(String startDate, String endDate, boolean restock) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            StringBuilder whereClause = new StringBuilder();
            List<String> args = new ArrayList<>();

            if (startDate != null && endDate != null) {
                whereClause.append(COLUMN_DATE_ONLY).append(" >= ? AND ").append(COLUMN_DATE_ONLY).append(" <= ?");
                args.add(startDate);
                args.add(endDate);
            }

            String whereStr = whereClause.length() > 0 ? whereClause.toString() : null;
            String[] argArray = args.isEmpty() ? null : args.toArray(new String[0]);

            List<Long> targetSaleIds = new ArrayList<>();
            Cursor c = db.query(TABLE_SALES, new String[]{COLUMN_SALE_ID, COLUMN_PAYMENT_MODE}, whereStr, argArray, null, null, null);
            if (c != null && c.moveToFirst()) {
                do {
                    long sId = c.getLong(0);
                    String pMode = c.getString(1);
                    targetSaleIds.add(sId);

                    if (restock && !"VOID".equalsIgnoreCase(pMode)) {
                        List<SaleTransactionItem> items = getItemsForSale(sId);
                        for (SaleTransactionItem it : items) {
                            db.execSQL("UPDATE " + TABLE_PRODUCTS + " SET " + COLUMN_STOCK + " = " + COLUMN_STOCK + " + ? WHERE " + COLUMN_NAME + " = ?",
                                    new Object[]{it.getQuantity(), it.getItemName()});
                        }
                    }
                } while (c.moveToNext());
                c.close();
            }

            for (Long id : targetSaleIds) {
                db.delete(TABLE_SALE_ITEMS, COLUMN_ITEM_SALE_ID + " = ?", new String[]{String.valueOf(id)});
            }

            int count = db.delete(TABLE_SALES, whereStr, argArray);
            db.setTransactionSuccessful();
            return count;
        } catch (Exception e) {
            return 0;
        } finally {
            db.endTransaction();
        }
    }

    public List<SaleTransaction> getFilteredSales(String startDate, String endDate, String filterMode, String searchQuery) {
        List<SaleTransaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder selection = new StringBuilder();
        List<String> args = new ArrayList<>();

        if ("CASH".equalsIgnoreCase(filterMode)) {
            selection.append(COLUMN_PAYMENT_MODE).append(" = 'CASH'");
        } else if ("GCASH".equalsIgnoreCase(filterMode)) {
            selection.append(COLUMN_PAYMENT_MODE).append(" = 'GCASH'");
        } else if ("CREDIT_UNPAID".equalsIgnoreCase(filterMode)) {
            selection.append(COLUMN_PAYMENT_MODE).append(" = 'CREDIT' AND ").append(COLUMN_IS_SETTLED).append(" = 0");
        } else if ("CREDIT_PAID".equalsIgnoreCase(filterMode)) {
            selection.append(COLUMN_PAYMENT_MODE).append(" = 'CREDIT' AND ").append(COLUMN_IS_SETTLED).append(" = 1");
        } else if ("VOID".equalsIgnoreCase(filterMode)) {
            selection.append(COLUMN_PAYMENT_MODE).append(" = 'VOID'");
        } else {
            // ALL (exclude void by default unless searched)
            selection.append(COLUMN_PAYMENT_MODE).append(" != 'VOID'");
        }

        if (startDate != null && endDate != null) {
            if (selection.length() > 0) selection.append(" AND ");
            selection.append(COLUMN_DATE_ONLY).append(" >= ? AND ").append(COLUMN_DATE_ONLY).append(" <= ?");
            args.add(startDate);
            args.add(endDate);
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            if (selection.length() > 0) selection.append(" AND ");
            selection.append("(").append(COLUMN_RECEIPT_NO).append(" LIKE ? OR ").append(COLUMN_CUSTOMER_NOTE).append(" LIKE ?)");
            String q = "%" + searchQuery.trim() + "%";
            args.add(q);
            args.add(q);
        }

        String whereClause = selection.length() > 0 ? selection.toString() : null;
        String[] whereArgs = args.isEmpty() ? null : args.toArray(new String[0]);

        Cursor c = db.query(TABLE_SALES, null, whereClause, whereArgs, null, null, COLUMN_SALE_ID + " DESC");

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

    /*==============================================================================================
    ======================================GCASH INVENTORY & LEDGER METHODS==========================
    ==============================================================================================*/

    public static class GcashStats {
        public double totalCashInAmount = 0.0;
        public int countCashIn = 0;
        public double totalCashOutAmount = 0.0;
        public int countCashOut = 0;
        public double totalFeesEarned = 0.0;
        public double netCashDrawerFlow = 0.0; // (Cash In + Fees) - Cash Out
        public double netGcashWalletFlow = 0.0; // Cash Out received - Cash In sent out
    }

    public long addGcashTransaction(GcashTransaction tx) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_GCASH_TYPE, tx.getType());
        cv.put(COLUMN_GCASH_REF_NO, tx.getRefNo());
        cv.put(COLUMN_GCASH_CUSTOMER_NUMBER, tx.getCustomerNumber());
        cv.put(COLUMN_GCASH_CUSTOMER_NAME, tx.getCustomerName());
        cv.put(COLUMN_GCASH_AMOUNT, tx.getAmount());
        cv.put(COLUMN_GCASH_FEE, tx.getFee());
        cv.put(COLUMN_GCASH_TOTAL_CASH, tx.getTotalCashHandled());
        cv.put(COLUMN_GCASH_DATE_TIME, tx.getDateTime());
        cv.put(COLUMN_GCASH_DATE_ONLY, tx.getDateOnly());
        cv.put(COLUMN_GCASH_NOTE, tx.getNote());
        cv.put(COLUMN_GCASH_STATUS, tx.getStatus());
        return db.insert(TABLE_GCASH, null, cv);
    }

    public List<GcashTransaction> getGcashTransactions(String startDate, String endDate, String typeFilter, String searchQuery) {
        List<GcashTransaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder selection = new StringBuilder();
        List<String> args = new ArrayList<>();

        if ("CASH_IN".equalsIgnoreCase(typeFilter)) {
            selection.append(COLUMN_GCASH_TYPE).append(" = 'CASH_IN'");
        } else if ("CASH_OUT".equalsIgnoreCase(typeFilter)) {
            selection.append(COLUMN_GCASH_TYPE).append(" = 'CASH_OUT'");
        }

        if (startDate != null && endDate != null) {
            if (selection.length() > 0) selection.append(" AND ");
            selection.append(COLUMN_GCASH_DATE_ONLY).append(" >= ? AND ").append(COLUMN_GCASH_DATE_ONLY).append(" <= ?");
            args.add(startDate);
            args.add(endDate);
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            if (selection.length() > 0) selection.append(" AND ");
            selection.append("(")
                    .append(COLUMN_GCASH_REF_NO).append(" LIKE ? OR ")
                    .append(COLUMN_GCASH_CUSTOMER_NUMBER).append(" LIKE ? OR ")
                    .append(COLUMN_GCASH_CUSTOMER_NAME).append(" LIKE ? OR ")
                    .append(COLUMN_GCASH_NOTE).append(" LIKE ?)");
            String q = "%" + searchQuery.trim() + "%";
            args.add(q);
            args.add(q);
            args.add(q);
            args.add(q);
        }

        String whereClause = selection.length() > 0 ? selection.toString() : null;
        String[] whereArgs = args.isEmpty() ? null : args.toArray(new String[0]);

        Cursor c = db.query(TABLE_GCASH, null, whereClause, whereArgs, null, null, COLUMN_GCASH_ID + " DESC");
        if (c != null && c.moveToFirst()) {
            do {
                GcashTransaction tx = new GcashTransaction(
                        c.getLong(c.getColumnIndexOrThrow(COLUMN_GCASH_ID)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_GCASH_TYPE)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_GCASH_REF_NO)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_GCASH_CUSTOMER_NUMBER)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_GCASH_CUSTOMER_NAME)),
                        c.getDouble(c.getColumnIndexOrThrow(COLUMN_GCASH_AMOUNT)),
                        c.getDouble(c.getColumnIndexOrThrow(COLUMN_GCASH_FEE)),
                        c.getDouble(c.getColumnIndexOrThrow(COLUMN_GCASH_TOTAL_CASH)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_GCASH_DATE_TIME)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_GCASH_DATE_ONLY)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_GCASH_NOTE)),
                        c.getString(c.getColumnIndexOrThrow(COLUMN_GCASH_STATUS))
                );
                list.add(tx);
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    public GcashStats getGcashStats(String startDate, String endDate) {
        GcashStats stats = new GcashStats();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder query = new StringBuilder("SELECT " +
                COLUMN_GCASH_TYPE + ", " +
                "COUNT(*) as cnt, " +
                "SUM(" + COLUMN_GCASH_AMOUNT + ") as total_amt, " +
                "SUM(" + COLUMN_GCASH_FEE + ") as total_fee, " +
                "SUM(" + COLUMN_GCASH_TOTAL_CASH + ") as total_cash " +
                "FROM " + TABLE_GCASH + " " +
                "WHERE " + COLUMN_GCASH_STATUS + " != 'VOIDED'");

        List<String> args = new ArrayList<>();
        if (startDate != null && endDate != null) {
            query.append(" AND ").append(COLUMN_GCASH_DATE_ONLY).append(" >= ? AND ").append(COLUMN_GCASH_DATE_ONLY).append(" <= ?");
            args.add(startDate);
            args.add(endDate);
        }
        query.append(" GROUP BY ").append(COLUMN_GCASH_TYPE);

        Cursor c = db.rawQuery(query.toString(), args.isEmpty() ? null : args.toArray(new String[0]));
        if (c != null && c.moveToFirst()) {
            do {
                String type = c.getString(c.getColumnIndexOrThrow(COLUMN_GCASH_TYPE));
                int cnt = c.getInt(c.getColumnIndexOrThrow("cnt"));
                double amt = c.getDouble(c.getColumnIndexOrThrow("total_amt"));
                double fee = c.getDouble(c.getColumnIndexOrThrow("total_fee"));

                if ("CASH_IN".equalsIgnoreCase(type)) {
                    stats.countCashIn = cnt;
                    stats.totalCashInAmount = amt;
                } else if ("CASH_OUT".equalsIgnoreCase(type)) {
                    stats.countCashOut = cnt;
                    stats.totalCashOutAmount = amt;
                }
                stats.totalFeesEarned += fee;
            } while (c.moveToNext());
            c.close();
        }

        stats.netCashDrawerFlow = (stats.totalCashInAmount + stats.totalFeesEarned) - stats.totalCashOutAmount;
        stats.netGcashWalletFlow = stats.totalCashOutAmount - stats.totalCashInAmount;

        return stats;
    }

    public boolean voidGcashTransaction(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_GCASH_STATUS, GcashTransaction.STATUS_VOIDED);
        int rows = db.update(TABLE_GCASH, cv, COLUMN_GCASH_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public boolean deleteGcashTransaction(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_GCASH, COLUMN_GCASH_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public int deleteAllVoidedGcashTransactions() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_GCASH, COLUMN_GCASH_STATUS + " = 'VOIDED'", null);
    }

    public int clearGcashHistory(String startDate, String endDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (startDate != null && endDate != null) {
            return db.delete(TABLE_GCASH, COLUMN_GCASH_DATE_ONLY + " >= ? AND " + COLUMN_GCASH_DATE_ONLY + " <= ?", new String[]{startDate, endDate});
        } else {
            return db.delete(TABLE_GCASH, null, null);
        }
    }

    public String generateNextGcashSlipNo() {
        SQLiteDatabase db = this.getReadableDatabase();
        long nextId = 1001;
        Cursor c = db.rawQuery("SELECT MAX(" + COLUMN_GCASH_ID + ") FROM " + TABLE_GCASH, null);
        if (c != null && c.moveToFirst()) {
            if (!c.isNull(0)) {
                nextId = 1001 + c.getLong(0);
            }
            c.close();
        }
        return "GC-" + nextId;
    }

    public static class GcashCustomerRecord {
        public final String customerNumber;
        public final String customerName;
        public final String lastDateTime;
        public final int transactionCount;

        public GcashCustomerRecord(String customerNumber, String customerName, String lastDateTime, int transactionCount) {
            this.customerNumber = customerNumber;
            this.customerName = customerName;
            this.lastDateTime = lastDateTime;
            this.transactionCount = transactionCount;
        }
    }

    public List<GcashCustomerRecord> getFrequentGcashCustomers(int limit) {
        List<GcashCustomerRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_GCASH_CUSTOMER_NUMBER + ", " +
                "MAX(" + COLUMN_GCASH_CUSTOMER_NAME + ") as cust_name, " +
                "MAX(" + COLUMN_GCASH_DATE_TIME + ") as last_dt, " +
                "COUNT(*) as tx_count " +
                "FROM " + TABLE_GCASH + " " +
                "WHERE " + COLUMN_GCASH_CUSTOMER_NUMBER + " IS NOT NULL AND length(" + COLUMN_GCASH_CUSTOMER_NUMBER + ") >= 10 " +
                "GROUP BY " + COLUMN_GCASH_CUSTOMER_NUMBER + " " +
                "ORDER BY last_dt DESC " +
                "LIMIT " + limit;
        Cursor c = db.rawQuery(query, null);
        if (c != null) {
            while (c.moveToNext()) {
                String num = c.getString(0);
                String name = c.getString(1);
                String dt = c.getString(2);
                int count = c.getInt(3);
                if (name == null) name = "";
                list.add(new GcashCustomerRecord(num, name, dt, count));
            }
            c.close();
        }
        return list;
    }
}

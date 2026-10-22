package com.dantsu.thermalprinter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.thermalprinter.async.AsyncBluetoothEscPosPrint;
import com.dantsu.thermalprinter.async.AsyncEscPosPrint;
import com.dantsu.thermalprinter.async.AsyncEscPosPrinter;
import com.dantsu.thermalprinter.async.AsyncUsbEscPosPrint;
import com.dantsu.thermalprinter.db.DatabaseHelper;
import com.dantsu.thermalprinter.model.Product;
import com.dantsu.thermalprinter.model.SaleTransaction;
import com.dantsu.thermalprinter.model.SaleTransactionItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static class CartItem {
        private final String name;
        private final double unitPrice;
        private int quantity;

        public CartItem(String name, double unitPrice, int quantity) {
            this.name = name;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        public String getName() {
            return name;
        }

        public double getUnitPrice() {
            return unitPrice;
        }

        public int getQuantity() {
            return quantity;
        }

        public void increment() {
            this.quantity++;
        }

        public boolean decrement() {
            this.quantity--;
            return this.quantity > 0;
        }

        public double getTotalPrice() {
            return unitPrice * quantity;
        }
    }

    /**
     * Stores and manages receipt design customization preferences.
     */
    public static class ReceiptDesignConfig {
        public String storeName = "MORPS POS";
        public String subheader = "Sari-Sari Store & General Mdse.";
        public String address = "Brgy. San Antonio • Pasig City";
        public String contact = "Phone: 0917-000-0000";
        public String receiptTitle = "ORDER RECEIPT";
        public boolean isHeaderBig = true;
        public String dividerStyle = "="; // "=", "-", "*"
        public String footer1 = "Thank You for Your Purchase!";
        public String footer2 = "Please Come Again!";
        public String policy = "No Return, No Exchange";
        public boolean showDate = true;
        public boolean showReceiptNo = true;

        public void load(Context context) {
            SharedPreferences prefs = context.getSharedPreferences("MorpsPOSPrefs", Context.MODE_PRIVATE);
            storeName = prefs.getString("storeName", "MORPS POS");
            subheader = prefs.getString("subheader", "Sari-Sari Store & General Mdse.");
            address = prefs.getString("address", "Brgy. San Antonio • Pasig City");
            contact = prefs.getString("contact", "Phone: 0917-000-0000");
            receiptTitle = prefs.getString("receiptTitle", "ORDER RECEIPT");
            isHeaderBig = prefs.getBoolean("isHeaderBig", true);
            dividerStyle = prefs.getString("dividerStyle", "=");
            footer1 = prefs.getString("footer1", "Thank You for Your Purchase!");
            footer2 = prefs.getString("footer2", "Please Come Again!");
            policy = prefs.getString("policy", "No Return, No Exchange");
            showDate = prefs.getBoolean("showDate", true);
            showReceiptNo = prefs.getBoolean("showReceiptNo", true);
        }

        public void save(Context context) {
            SharedPreferences.Editor editor = context.getSharedPreferences("MorpsPOSPrefs", Context.MODE_PRIVATE).edit();
            editor.putString("storeName", storeName);
            editor.putString("subheader", subheader);
            editor.putString("address", address);
            editor.putString("contact", contact);
            editor.putString("receiptTitle", receiptTitle);
            editor.putBoolean("isHeaderBig", isHeaderBig);
            editor.putString("dividerStyle", dividerStyle);
            editor.putString("footer1", footer1);
            editor.putString("footer2", footer2);
            editor.putString("policy", policy);
            editor.putBoolean("showDate", showDate);
            editor.putBoolean("showReceiptNo", showReceiptNo);
            editor.apply();
        }

        public void reset() {
            storeName = "MORPS POS";
            subheader = "Sari-Sari Store & General Mdse.";
            address = "Brgy. San Antonio • Pasig City";
            contact = "Phone: 0917-000-0000";
            receiptTitle = "ORDER RECEIPT";
            isHeaderBig = true;
            dividerStyle = "=";
            footer1 = "Thank You for Your Purchase!";
            footer2 = "Please Come Again!";
            policy = "No Return, No Exchange";
            showDate = true;
            showReceiptNo = true;
        }

        public String getDivider(int length) {
            char ch = (dividerStyle == null || dividerStyle.isEmpty()) ? '=' : dividerStyle.charAt(0);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                sb.append(ch);
            }
            return sb.toString();
        }
    }

    private final List<CartItem> cartItems = new ArrayList<>();
    private final ReceiptDesignConfig receiptDesign = new ReceiptDesignConfig();
    private DatabaseHelper dbHelper;
    private int receiptCounter = 1001;

    // Active Category Filter for POS
    private String selectedPosCategory = "All";

    // Navigation Tabs
    private static final int TAB_POS = 0;
    private static final int TAB_INVENTORY = 1;
    private static final int TAB_SALES = 2;
    private int currentTab = TAB_POS;

    // Views - Header & Navigation
    private TextView textMainTitle;
    private TextView textMainSubtitle;
    private TextView textPrinterName;
    private Button buttonBrowsePrinter;
    private Button tabPos;
    private Button tabInventory;
    private Button tabSales;
    private LinearLayout layoutPosView;
    private LinearLayout layoutInventoryView;
    private LinearLayout layoutSalesView;

    // Views - POS Tab
    private TextView textPosLowStockAlert;
    private EditText editPosSearch;
    private LinearLayout layoutPosCategories;
    private LinearLayout layoutPosProductsContainer;
    private LinearLayout layoutCartItems;
    private TextView textEmptyCart;
    private TextView textCartTotal;
    private RadioGroup radioGroupPayment;
    private LinearLayout layoutCashSection;
    private EditText editCashTendered;
    private TextView textChange;
    private EditText editCustomerNote;
    private EditText editCustomName;
    private EditText editCustomPrice;

    // Views - Inventory Tab
    private TextView textInvMetricProducts;
    private TextView textInvMetricStock;
    private TextView textInvMetricValue;
    private TextView bannerInvLowStock;
    private EditText editInventorySearch;
    private LinearLayout layoutInventoryItemsContainer;

    // Views - Sales Tab
    private TextView textSalesTodayDate;
    private TextView textSalesTodayTotal;
    private TextView textSalesTodayOrders;
    private TextView textSalesTodayCash;
    private TextView textSalesTodayGcash;
    private TextView textSalesUnpaidCredit;
    private Button btnPrintZReport;
    private RadioGroup radioGroupSalesFilter;
    private TextView textSalesEmpty;
    private LinearLayout layoutSalesItemsContainer;

    private BluetoothConnection selectedBluetoothDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = DatabaseHelper.getInstance(this);
        receiptDesign.load(this);

        // Keep receipt numbers aligned with previous sales
        try {
            int totalSales = dbHelper.getAllSales(false).size();
            receiptCounter = 1001 + totalSales;
        } catch (Exception ignored) {}

        initViews();
        applyStoreDesignToUI();
        setupTabs();
        setupPosCatalog();
        setupPaymentWatcher();
        setupQuickCashChips();
        setupPrinterButtons();
        setupInventoryEvents();
        setupSalesEvents();

        updateCartDisplay();
        refreshAllData();
    }

    private void initViews() {
        textMainTitle = findViewById(R.id.text_main_title);
        textMainSubtitle = findViewById(R.id.text_main_subtitle);
        textPrinterName = findViewById(R.id.text_printer_name);
        buttonBrowsePrinter = findViewById(R.id.button_bluetooth_browse);

        tabPos = findViewById(R.id.tab_pos);
        tabInventory = findViewById(R.id.tab_inventory);
        tabSales = findViewById(R.id.tab_sales);
        layoutPosView = findViewById(R.id.layout_pos_view);
        layoutInventoryView = findViewById(R.id.layout_inventory_view);
        layoutSalesView = findViewById(R.id.layout_sales_view);

        // POS Elements
        textPosLowStockAlert = findViewById(R.id.text_pos_low_stock_alert);
        editPosSearch = findViewById(R.id.edit_pos_search);
        layoutPosCategories = findViewById(R.id.layout_pos_categories);
        layoutPosProductsContainer = findViewById(R.id.layout_pos_products_container);
        layoutCartItems = findViewById(R.id.layout_cart_items);
        textEmptyCart = findViewById(R.id.text_empty_cart);
        textCartTotal = findViewById(R.id.text_cart_total);
        radioGroupPayment = findViewById(R.id.radiogroup_payment);
        layoutCashSection = findViewById(R.id.layout_cash_section);
        editCashTendered = findViewById(R.id.edittext_cash_tendered);
        textChange = findViewById(R.id.text_change);
        editCustomerNote = findViewById(R.id.edittext_customer_note);
        editCustomName = findViewById(R.id.edittext_custom_name);
        editCustomPrice = findViewById(R.id.edittext_custom_price);

        // Inventory Elements
        textInvMetricProducts = findViewById(R.id.text_inv_metric_products);
        textInvMetricStock = findViewById(R.id.text_inv_metric_stock);
        textInvMetricValue = findViewById(R.id.text_inv_metric_value);
        bannerInvLowStock = findViewById(R.id.banner_inv_low_stock);
        editInventorySearch = findViewById(R.id.edit_inventory_search);
        layoutInventoryItemsContainer = findViewById(R.id.layout_inventory_items_container);

        // Sales Elements
        textSalesTodayDate = findViewById(R.id.text_sales_today_date);
        textSalesTodayTotal = findViewById(R.id.text_sales_today_total);
        textSalesTodayOrders = findViewById(R.id.text_sales_today_orders);
        textSalesTodayCash = findViewById(R.id.text_sales_today_cash);
        textSalesTodayGcash = findViewById(R.id.text_sales_today_gcash);
        textSalesUnpaidCredit = findViewById(R.id.text_sales_unpaid_credit);
        btnPrintZReport = findViewById(R.id.btn_print_z_report);
        radioGroupSalesFilter = findViewById(R.id.radiogroup_sales_filter);
        textSalesEmpty = findViewById(R.id.text_sales_empty);
        layoutSalesItemsContainer = findViewById(R.id.layout_sales_items_container);

        findViewById(R.id.button_clear_cart).setOnClickListener(v -> clearCart());
        findViewById(R.id.button_new_sale).setOnClickListener(v -> completeSaleAndReset());
        findViewById(R.id.button_customize_receipt).setOnClickListener(v -> openReceiptDesignDialog());
        findViewById(R.id.button_preview_receipt).setOnClickListener(v -> openReceiptPreviewDialog());

        findViewById(R.id.button_add_custom).setOnClickListener(v -> {
            String name = editCustomName.getText().toString().trim();
            String priceStr = editCustomPrice.getText().toString().trim();

            if (name.isEmpty()) {
                name = "Item";
            }

            if (priceStr.isEmpty()) {
                Toast.makeText(this, "Please enter a price", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                addItemToCart(name, price);
                editCustomName.setText("");
                editCustomPrice.setText("");
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price format", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyStoreDesignToUI() {
        if (textMainTitle != null && receiptDesign.storeName != null && !receiptDesign.storeName.isEmpty()) {
            textMainTitle.setText(receiptDesign.storeName + " 🛒");
        }
        if (textMainSubtitle != null) {
            String sub = receiptDesign.subheader.isEmpty() ? "Sari-Sari Store POS" : receiptDesign.subheader;
            textMainSubtitle.setText(sub + " • 58mm Thermal");
        }
    }

    private void setupTabs() {
        tabPos.setOnClickListener(v -> switchTab(TAB_POS));
        tabInventory.setOnClickListener(v -> switchTab(TAB_INVENTORY));
        tabSales.setOnClickListener(v -> switchTab(TAB_SALES));
    }

    private void switchTab(int tab) {
        currentTab = tab;

        tabPos.setBackgroundColor(Color.TRANSPARENT);
        tabPos.setTextColor(Color.parseColor("#1E293B"));
        tabInventory.setBackgroundColor(Color.TRANSPARENT);
        tabInventory.setTextColor(Color.parseColor("#1E293B"));
        tabSales.setBackgroundColor(Color.TRANSPARENT);
        tabSales.setTextColor(Color.parseColor("#1E293B"));

        layoutPosView.setVisibility(View.GONE);
        layoutInventoryView.setVisibility(View.GONE);
        layoutSalesView.setVisibility(View.GONE);

        if (tab == TAB_POS) {
            tabPos.setBackgroundResource(R.drawable.button_primary);
            tabPos.setTextColor(Color.WHITE);
            layoutPosView.setVisibility(View.VISIBLE);
            renderPosProducts();
        } else if (tab == TAB_INVENTORY) {
            tabInventory.setBackgroundResource(R.drawable.button_primary);
            tabInventory.setTextColor(Color.WHITE);
            layoutInventoryView.setVisibility(View.VISIBLE);
            renderInventory();
        } else if (tab == TAB_SALES) {
            tabSales.setBackgroundResource(R.drawable.button_primary);
            tabSales.setTextColor(Color.WHITE);
            layoutSalesView.setVisibility(View.VISIBLE);
            renderSales();
        }
    }

    private void refreshAllData() {
        renderPosCategories();
        renderPosProducts();
        renderInventory();
        renderSales();
    }

    /*==============================================================================================
    ======================================POS CATALOG SECTION=======================================
    ==============================================================================================*/

    private void setupPosCatalog() {
        editPosSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderPosProducts();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void renderPosCategories() {
        layoutPosCategories.removeAllViews();
        List<String> categories = dbHelper.getCategories();

        for (String cat : categories) {
            Button btnCat = new Button(this);
            btnCat.setText(cat);
            btnCat.setTextSize(11);
            btnCat.setAllCaps(false);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    80
            );
            lp.setMargins(0, 0, 10, 0);
            btnCat.setLayoutParams(lp);

            if (cat.equalsIgnoreCase(selectedPosCategory)) {
                btnCat.setBackgroundResource(R.drawable.button_primary);
                btnCat.setTextColor(Color.WHITE);
                btnCat.setTypeface(null, Typeface.BOLD);
            } else {
                btnCat.setBackgroundResource(R.drawable.button_chip);
                btnCat.setTextColor(Color.parseColor("#1E293B"));
                btnCat.setTypeface(null, Typeface.NORMAL);
            }

            btnCat.setOnClickListener(v -> {
                selectedPosCategory = cat;
                renderPosCategories();
                renderPosProducts();
            });

            layoutPosCategories.addView(btnCat);
        }
    }

    private void renderPosProducts() {
        layoutPosProductsContainer.removeAllViews();
        String search = editPosSearch.getText().toString().trim();
        List<Product> products = dbHelper.getAllProducts(search, selectedPosCategory);

        int lowStockCount = dbHelper.getLowStockCount();
        if (lowStockCount > 0) {
            textPosLowStockAlert.setVisibility(View.VISIBLE);
            textPosLowStockAlert.setText("⚠️ " + lowStockCount + " Low Stock");
            textPosLowStockAlert.setOnClickListener(v -> switchTab(TAB_INVENTORY));
        } else {
            textPosLowStockAlert.setVisibility(View.GONE);
        }

        if (products.isEmpty()) {
            TextView emptyTxt = new TextView(this);
            emptyTxt.setText("No products found. Add new items in the Inventory tab.");
            emptyTxt.setTextColor(Color.parseColor("#64748B"));
            emptyTxt.setTextSize(13);
            emptyTxt.setGravity(Gravity.CENTER);
            emptyTxt.setPadding(0, 24, 0, 24);
            layoutPosProductsContainer.addView(emptyTxt);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Product p : products) {
            View itemView = inflater.inflate(R.layout.item_pos_product, layoutPosProductsContainer, false);

            TextView textName = itemView.findViewById(R.id.text_pos_product_name);
            TextView textCat = itemView.findViewById(R.id.text_pos_product_category);
            TextView textStock = itemView.findViewById(R.id.text_pos_stock_indicator);
            TextView textPrice = itemView.findViewById(R.id.text_pos_product_price);
            Button btnAdd = itemView.findViewById(R.id.btn_pos_add);

            textName.setText(p.getName());
            textCat.setText(p.getCategory());
            textPrice.setText(String.format(Locale.US, "₱%.2f", p.getPrice()));

            if (p.isOutOfStock()) {
                textStock.setText("Out of Stock");
                textStock.setBackgroundResource(R.drawable.badge_red);
                textStock.setTextColor(Color.parseColor("#991B1B"));
                itemView.setAlpha(0.6f);
            } else if (p.isLowStock()) {
                textStock.setText("Stock: " + p.getStock() + " (Low)");
                textStock.setBackgroundResource(R.drawable.badge_orange);
                textStock.setTextColor(Color.parseColor("#92400E"));
                itemView.setAlpha(1.0f);
            } else {
                textStock.setText("Stock: " + p.getStock());
                textStock.setBackgroundResource(R.drawable.badge_green);
                textStock.setTextColor(Color.parseColor("#166534"));
                itemView.setAlpha(1.0f);
            }

            View.OnClickListener addListener = v -> {
                if (p.isOutOfStock()) {
                    Toast.makeText(this, p.getName() + " is OUT OF STOCK!", Toast.LENGTH_SHORT).show();
                    return;
                }
                addItemToCart(p.getName(), p.getPrice());
            };

            itemView.setOnClickListener(addListener);
            btnAdd.setOnClickListener(addListener);

            layoutPosProductsContainer.addView(itemView);
        }
    }

    private void addItemToCart(String name, double price) {
        for (CartItem item : cartItems) {
            if (item.getName().equalsIgnoreCase(name) && Math.abs(item.getUnitPrice() - price) < 0.001) {
                item.increment();
                updateCartDisplay();
                return;
            }
        }
        cartItems.add(new CartItem(name, price, 1));
        updateCartDisplay();
    }

    private void clearCart() {
        cartItems.clear();
        updateCartDisplay();
    }

    private void completeSaleAndReset() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is already empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        double total = calculateTotal();
        String paymentMode = "CASH";
        int checkedId = radioGroupPayment.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_gcash) paymentMode = "GCASH";
        else if (checkedId == R.id.radio_utang) paymentMode = "CREDIT";

        double cashTendered = total;
        double change = 0.0;
        boolean isPaid = !paymentMode.equals("CREDIT");

        if (paymentMode.equals("CASH")) {
            try {
                String s = editCashTendered.getText().toString().trim();
                if (!s.isEmpty()) {
                    cashTendered = Double.parseDouble(s);
                    change = Math.max(0.0, cashTendered - total);
                }
            } catch (Exception ignored) {}
        } else if (paymentMode.equals("CREDIT")) {
            cashTendered = 0.0;
            change = 0.0;
        }

        String customerNote = editCustomerNote.getText().toString().trim();
        SimpleDateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date now = new Date();
        String dateTime = dtFormat.format(now);
        String dateOnly = dFormat.format(now);
        String receiptNo = "MORPS-" + receiptCounter;

        SaleTransaction sale = new SaleTransaction(
            receiptNo,
            dateTime,
            dateOnly,
            total,
            paymentMode,
            cashTendered,
            change,
            customerNote,
            isPaid
        );

        // Record sale and deduct inventory stock in single atomic transaction
        dbHelper.recordSale(sale, cartItems);

        clearCart();
        editCashTendered.setText("");
        editCustomerNote.setText("");
        receiptCounter++;
        ((RadioButton) findViewById(R.id.radio_cash)).setChecked(true);

        // Refresh dynamic inventory, catalog, and sales history
        refreshAllData();

        Toast.makeText(this, "Sale recorded (" + receiptNo + ")! Inventory updated.", Toast.LENGTH_SHORT).show();
    }

    private double calculateTotal() {
        double total = 0.0;
        for (CartItem item : cartItems) {
            total += item.getTotalPrice();
        }
        return total;
    }

    private void updateCartDisplay() {
        layoutCartItems.removeAllViews();
        double total = calculateTotal();

        if (cartItems.isEmpty()) {
            textEmptyCart.setVisibility(View.VISIBLE);
        } else {
            textEmptyCart.setVisibility(View.GONE);

            for (int i = 0; i < cartItems.size(); i++) {
                final CartItem item = cartItems.get(i);
                final int itemIndex = i;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, 8, 0, 8);

                LinearLayout infoCol = new LinearLayout(this);
                infoCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.8f);
                infoCol.setLayoutParams(infoParams);

                TextView textName = new TextView(this);
                textName.setText(item.getName());
                textName.setTextSize(13);
                textName.setTextColor(Color.parseColor("#1E293B"));
                textName.setTypeface(null, Typeface.BOLD);

                TextView textPrice = new TextView(this);
                textPrice.setText(String.format(Locale.US, "₱%.2f each", item.getUnitPrice()));
                textPrice.setTextSize(11);
                textPrice.setTextColor(Color.parseColor("#64748B"));

                infoCol.addView(textName);
                infoCol.addView(textPrice);

                Button btnMinus = new Button(this);
                btnMinus.setText("-");
                btnMinus.setTextSize(14);
                btnMinus.setTextColor(Color.parseColor("#EF4444"));
                btnMinus.setBackgroundColor(Color.TRANSPARENT);
                btnMinus.setLayoutParams(new LinearLayout.LayoutParams(90, 90));
                btnMinus.setOnClickListener(v -> {
                    if (!item.decrement()) {
                        cartItems.remove(itemIndex);
                    }
                    updateCartDisplay();
                });

                TextView textQty = new TextView(this);
                textQty.setText(String.valueOf(item.getQuantity()));
                textQty.setTextSize(14);
                textQty.setTypeface(null, Typeface.BOLD);
                textQty.setGravity(Gravity.CENTER);
                textQty.setLayoutParams(new LinearLayout.LayoutParams(70, LinearLayout.LayoutParams.WRAP_CONTENT));

                Button btnPlus = new Button(this);
                btnPlus.setText("+");
                btnPlus.setTextSize(14);
                btnPlus.setTextColor(Color.parseColor("#1B5E20"));
                btnPlus.setBackgroundColor(Color.TRANSPARENT);
                btnPlus.setLayoutParams(new LinearLayout.LayoutParams(90, 90));
                btnPlus.setOnClickListener(v -> {
                    item.increment();
                    updateCartDisplay();
                });

                TextView textSubtotal = new TextView(this);
                textSubtotal.setText(String.format(Locale.US, "₱%.2f", item.getTotalPrice()));
                textSubtotal.setTextSize(13);
                textSubtotal.setTypeface(null, Typeface.BOLD);
                textSubtotal.setGravity(Gravity.END);
                textSubtotal.setTextColor(Color.parseColor("#1B5E20"));
                textSubtotal.setLayoutParams(new LinearLayout.LayoutParams(160, LinearLayout.LayoutParams.WRAP_CONTENT));

                row.addView(infoCol);
                row.addView(btnMinus);
                row.addView(textQty);
                row.addView(btnPlus);
                row.addView(textSubtotal);

                layoutCartItems.addView(row);
            }
        }

        textCartTotal.setText(String.format(Locale.US, "₱ %.2f", total));
        recalculateChange();
    }

    private void setupPaymentWatcher() {
        radioGroupPayment.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_cash) {
                layoutCashSection.setVisibility(View.VISIBLE);
                editCustomerNote.setHint("Customer Name / Note (Optional)");
            } else if (checkedId == R.id.radio_gcash) {
                layoutCashSection.setVisibility(View.GONE);
                editCustomerNote.setHint("GCash Reference # / Customer Name");
            } else if (checkedId == R.id.radio_utang) {
                layoutCashSection.setVisibility(View.GONE);
                editCustomerNote.setHint("Customer Name (Credit / Due)");
            }
            recalculateChange();
        });

        editCashTendered.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateChange();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupQuickCashChips() {
        findViewById(R.id.chip_cash_exact).setOnClickListener(v -> {
            double total = calculateTotal();
            editCashTendered.setText(String.format(Locale.US, "%.2f", total));
        });
        findViewById(R.id.chip_cash_50).setOnClickListener(v -> editCashTendered.setText("50"));
        findViewById(R.id.chip_cash_100).setOnClickListener(v -> editCashTendered.setText("100"));
        findViewById(R.id.chip_cash_500).setOnClickListener(v -> editCashTendered.setText("500"));
        findViewById(R.id.chip_cash_1000).setOnClickListener(v -> editCashTendered.setText("1000"));
    }

    private void recalculateChange() {
        double total = calculateTotal();
        int paymentModeId = radioGroupPayment.getCheckedRadioButtonId();

        if (paymentModeId != R.id.radio_cash) {
            textChange.setText("Change: ₱ 0.00");
            textChange.setTextColor(Color.parseColor("#1B5E20"));
            return;
        }

        String cashStr = editCashTendered.getText().toString().trim();
        if (cashStr.isEmpty()) {
            textChange.setText("Change: ₱ 0.00");
            textChange.setTextColor(Color.parseColor("#64748B"));
            return;
        }

        try {
            double cash = Double.parseDouble(cashStr);
            double change = cash - total;
            if (change < 0) {
                textChange.setText(String.format(Locale.US, "Short: ₱ %.2f", Math.abs(change)));
                textChange.setTextColor(Color.parseColor("#EF4444"));
            } else {
                textChange.setText(String.format(Locale.US, "Change: ₱ %.2f", change));
                textChange.setTextColor(Color.parseColor("#1B5E20"));
            }
        } catch (NumberFormatException e) {
            textChange.setText("Change: ₱ 0.00");
        }
    }

    /*==============================================================================================
    ==================================INVENTORY MANAGEMENT SECTION==================================
    ==============================================================================================*/

    private void setupInventoryEvents() {
        findViewById(R.id.btn_inventory_add).setOnClickListener(v -> openAddEditProductDialog(null));

        editInventorySearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderInventory();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void renderInventory() {
        layoutInventoryItemsContainer.removeAllViews();
        String query = editInventorySearch.getText().toString().trim();
        List<Product> products = dbHelper.getAllProducts(query, null);

        // Update Overview Metrics
        textInvMetricProducts.setText(String.valueOf(products.size()));
        textInvMetricStock.setText(String.valueOf(dbHelper.getTotalStockCount()));
        textInvMetricValue.setText(String.format(Locale.US, "₱%.2f", dbHelper.getTotalInventoryValue()));

        int lowStockCount = dbHelper.getLowStockCount();
        if (lowStockCount > 0) {
            bannerInvLowStock.setVisibility(View.VISIBLE);
            bannerInvLowStock.setText("⚠️ " + lowStockCount + " items are low in stock! Tap +Restock to add.");
        } else {
            bannerInvLowStock.setVisibility(View.GONE);
        }

        if (products.isEmpty()) {
            TextView emptyTxt = new TextView(this);
            emptyTxt.setText("No products found in inventory. Tap '+ Add Item' above.");
            emptyTxt.setTextColor(Color.parseColor("#64748B"));
            emptyTxt.setTextSize(13);
            emptyTxt.setGravity(Gravity.CENTER);
            emptyTxt.setPadding(0, 32, 0, 32);
            layoutInventoryItemsContainer.addView(emptyTxt);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Product p : products) {
            View itemView = inflater.inflate(R.layout.item_inventory_product, layoutInventoryItemsContainer, false);

            TextView textName = itemView.findViewById(R.id.text_inv_item_name);
            TextView textCat = itemView.findViewById(R.id.text_inv_item_category);
            TextView textStockBadge = itemView.findViewById(R.id.text_inv_stock_badge);
            TextView textPrice = itemView.findViewById(R.id.text_inv_item_price);
            TextView textCost = itemView.findViewById(R.id.text_inv_item_cost);
            Button btnEdit = itemView.findViewById(R.id.btn_inv_edit);
            Button btnRestock5 = itemView.findViewById(R.id.btn_inv_restock_5);
            Button btnRestock10 = itemView.findViewById(R.id.btn_inv_restock_10);

            textName.setText(p.getName());
            textCat.setText(p.getCategory());
            textPrice.setText(String.format(Locale.US, "Selling: ₱%.2f", p.getPrice()));

            double profit = p.getPrice() - p.getCost();
            textCost.setText(String.format(Locale.US, "Cost: ₱%.2f • Margin: +₱%.2f", p.getCost(), profit));

            if (p.isOutOfStock()) {
                textStockBadge.setText("Out of Stock (0)");
                textStockBadge.setBackgroundResource(R.drawable.badge_red);
                textStockBadge.setTextColor(Color.parseColor("#991B1B"));
            } else if (p.isLowStock()) {
                textStockBadge.setText("Low Stock: " + p.getStock());
                textStockBadge.setBackgroundResource(R.drawable.badge_orange);
                textStockBadge.setTextColor(Color.parseColor("#92400E"));
            } else {
                textStockBadge.setText("In Stock: " + p.getStock());
                textStockBadge.setBackgroundResource(R.drawable.badge_green);
                textStockBadge.setTextColor(Color.parseColor("#166534"));
            }

            btnRestock5.setOnClickListener(v -> {
                dbHelper.adjustStock(p.getId(), 5);
                Toast.makeText(this, "Added +5 to " + p.getName(), Toast.LENGTH_SHORT).show();
                renderInventory();
                renderPosProducts();
            });

            btnRestock10.setOnClickListener(v -> {
                dbHelper.adjustStock(p.getId(), 10);
                Toast.makeText(this, "Added +10 to " + p.getName(), Toast.LENGTH_SHORT).show();
                renderInventory();
                renderPosProducts();
            });

            btnEdit.setOnClickListener(v -> openAddEditProductDialog(p));

            layoutInventoryItemsContainer.addView(itemView);
        }
    }

    private void openAddEditProductDialog(Product existingProduct) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_product, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView textTitle = dialogView.findViewById(R.id.text_dialog_product_title);
        EditText editName = dialogView.findViewById(R.id.edit_product_name);
        EditText editCat = dialogView.findViewById(R.id.edit_product_category);
        EditText editPrice = dialogView.findViewById(R.id.edit_product_price);
        EditText editCost = dialogView.findViewById(R.id.edit_product_cost);
        EditText editStock = dialogView.findViewById(R.id.edit_product_stock);
        EditText editMinThreshold = dialogView.findViewById(R.id.edit_product_min_threshold);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete_product);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_product);
        Button btnSave = dialogView.findViewById(R.id.btn_save_product);

        final boolean isEdit = existingProduct != null;

        if (isEdit) {
            textTitle.setText("✏️ Edit Product");
            editName.setText(existingProduct.getName());
            editCat.setText(existingProduct.getCategory());
            editPrice.setText(String.format(Locale.US, "%.2f", existingProduct.getPrice()));
            editCost.setText(String.format(Locale.US, "%.2f", existingProduct.getCost()));
            editStock.setText(String.valueOf(existingProduct.getStock()));
            editMinThreshold.setText(String.valueOf(existingProduct.getMinThreshold()));
            btnDelete.setVisibility(View.VISIBLE);

            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Product")
                        .setMessage("Are you sure you want to delete '" + existingProduct.getName() + "'?")
                        .setPositiveButton("Delete", (d, which) -> {
                            dbHelper.deleteProduct(existingProduct.getId());
                            Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            refreshAllData();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String cat = editCat.getText().toString().trim();
            String priceStr = editPrice.getText().toString().trim();
            String costStr = editCost.getText().toString().trim();
            String stockStr = editStock.getText().toString().trim();
            String minStr = editMinThreshold.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter product name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (priceStr.isEmpty()) {
                Toast.makeText(this, "Please enter selling price", Toast.LENGTH_SHORT).show();
                return;
            }

            double price;
            double cost = 0.0;
            int stock = 0;
            int minThreshold = 5;

            try {
                price = Double.parseDouble(priceStr);
                if (!costStr.isEmpty()) cost = Double.parseDouble(costStr);
                if (!stockStr.isEmpty()) stock = Integer.parseInt(stockStr);
                if (!minStr.isEmpty()) minThreshold = Integer.parseInt(minStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please check number inputs", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEdit) {
                existingProduct.setName(name);
                existingProduct.setCategory(cat);
                existingProduct.setPrice(price);
                existingProduct.setCost(cost);
                existingProduct.setStock(stock);
                existingProduct.setMinThreshold(minThreshold);
                dbHelper.updateProduct(existingProduct);
                Toast.makeText(this, "Product updated!", Toast.LENGTH_SHORT).show();
            } else {
                Product newProd = new Product(name, cat, price, cost, stock, minThreshold);
                dbHelper.addProduct(newProd);
                Toast.makeText(this, "Product added to inventory!", Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
            refreshAllData();
        });

        dialog.show();
    }

    /*==============================================================================================
    ================================SALES & BUSINESS REPORTS SECTION================================
    ==============================================================================================*/

    private void setupSalesEvents() {
        if (btnPrintZReport != null) {
            btnPrintZReport.setOnClickListener(v -> onPrintZReportClicked());
        }
        if (radioGroupSalesFilter != null) {
            radioGroupSalesFilter.setOnCheckedChangeListener((group, checkedId) -> renderSales());
        }
    }

    private void renderSales() {
        if (layoutSalesItemsContainer == null) return;
        layoutSalesItemsContainer.removeAllViews();

        SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String today = dFormat.format(new Date());

        // Update Today's Summary Metrics
        double todayTotal = dbHelper.getTodaySalesTotal(today);
        int todayOrders = dbHelper.getTodayOrderCount(today);
        double todayCash = dbHelper.getTodayCashTotal(today);
        double todayGcash = dbHelper.getTodayGcashTotal(today);
        double unpaidCredit = dbHelper.getUnpaidCreditTotal();

        if (textSalesTodayDate != null) textSalesTodayDate.setText(today);
        if (textSalesTodayTotal != null) textSalesTodayTotal.setText(String.format(Locale.US, "₱%.2f", todayTotal));
        if (textSalesTodayOrders != null) textSalesTodayOrders.setText(String.valueOf(todayOrders));
        if (textSalesTodayCash != null) textSalesTodayCash.setText(String.format(Locale.US, "₱%.2f", todayCash));
        if (textSalesTodayGcash != null) textSalesTodayGcash.setText(String.format(Locale.US, "₱%.2f", todayGcash));
        if (textSalesUnpaidCredit != null) textSalesUnpaidCredit.setText(String.format(Locale.US, "₱%.2f", unpaidCredit));

        boolean creditOnly = radioGroupSalesFilter != null && radioGroupSalesFilter.getCheckedRadioButtonId() == R.id.radio_filter_unpaid_credit;
        List<SaleTransaction> sales = dbHelper.getAllSales(creditOnly);

        if (sales.isEmpty()) {
            if (textSalesEmpty != null) {
                textSalesEmpty.setVisibility(View.VISIBLE);
                textSalesEmpty.setText(creditOnly ? "No outstanding unpaid credit transactions." : "No transactions recorded yet.");
            }
            return;
        }

        if (textSalesEmpty != null) textSalesEmpty.setVisibility(View.GONE);

        for (SaleTransaction sale : sales) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_sale_transaction, layoutSalesItemsContainer, false);

            TextView textReceiptNo = itemView.findViewById(R.id.text_sale_receipt_no);
            TextView textDateTime = itemView.findViewById(R.id.text_sale_date_time);
            TextView textBadge = itemView.findViewById(R.id.text_sale_payment_badge);
            TextView textSummary = itemView.findViewById(R.id.text_sale_items_summary);
            TextView textNote = itemView.findViewById(R.id.text_sale_customer_note);
            TextView textTotal = itemView.findViewById(R.id.text_sale_total_amount);
            Button btnSettle = itemView.findViewById(R.id.btn_settle_credit);
            Button btnReprint = itemView.findViewById(R.id.btn_reprint_receipt);

            textReceiptNo.setText(sale.getReceiptNo());
            textDateTime.setText(sale.getDateTime());
            textTotal.setText(String.format(Locale.US, "₱%.2f", sale.getTotalAmount()));

            // Format Payment Badge
            String mode = sale.getPaymentMode();
            if ("CREDIT".equalsIgnoreCase(mode)) {
                if (sale.isSettled()) {
                    textBadge.setText("CREDIT (PAID)");
                    textBadge.setBackgroundResource(R.drawable.badge_green);
                    textBadge.setTextColor(Color.parseColor("#166534"));
                    btnSettle.setVisibility(View.GONE);
                } else {
                    textBadge.setText("UNPAID CREDIT");
                    textBadge.setBackgroundResource(R.drawable.badge_red);
                    textBadge.setTextColor(Color.parseColor("#991B1B"));
                    btnSettle.setVisibility(View.VISIBLE);
                }
            } else if ("GCASH".equalsIgnoreCase(mode)) {
                textBadge.setText("GCASH");
                textBadge.setBackgroundResource(R.drawable.badge_green);
                textBadge.setTextColor(Color.parseColor("#166534"));
                btnSettle.setVisibility(View.GONE);
            } else {
                textBadge.setText("CASH");
                textBadge.setBackgroundResource(R.drawable.badge_green);
                textBadge.setTextColor(Color.parseColor("#166534"));
                btnSettle.setVisibility(View.GONE);
            }

            // Items summary
            List<SaleTransactionItem> items = sale.getItems();
            if (items == null || items.isEmpty()) {
                textSummary.setText("Item sale");
            } else {
                StringBuilder isb = new StringBuilder();
                for (int i = 0; i < items.size(); i++) {
                    SaleTransactionItem it = items.get(i);
                    if (i > 0) isb.append(", ");
                    isb.append(it.getQuantity()).append("x ").append(it.getItemName());
                }
                textSummary.setText(isb.toString());
            }

            // Customer Note
            String note = sale.getCustomerNote();
            if (note != null && !note.trim().isEmpty()) {
                textNote.setVisibility(View.VISIBLE);
                textNote.setText("Note: " + note.trim());
            } else {
                textNote.setVisibility(View.GONE);
            }

            // Settle Credit Click
            btnSettle.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("Settle Store Credit")
                    .setMessage("Mark " + sale.getReceiptNo() + " (" + String.format(Locale.US, "₱%.2f", sale.getTotalAmount()) + ") as PAID?")
                    .setPositiveButton("Confirm Payment", (dialog, which) -> {
                        dbHelper.settleCredit(sale.getId());
                        Toast.makeText(this, "Credit marked as settled!", Toast.LENGTH_SHORT).show();
                        renderSales();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });

            // Re-print Receipt Click
            btnReprint.setOnClickListener(v -> onReprintClicked(sale));

            layoutSalesItemsContainer.addView(itemView);
        }
    }

    private void onReprintClicked(SaleTransaction sale) {
        String[] options = {"👁️ Preview Receipt Copy", "🖨️ Print via Bluetooth", "🔌 Print via USB"};
        new AlertDialog.Builder(this)
            .setTitle("Re-Print Receipt: " + sale.getReceiptNo())
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    previewPastReceiptDialog(sale);
                } else if (which == 1) {
                    printPastReceiptBluetooth(sale);
                } else if (which == 2) {
                    printPastReceiptUsb(sale);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void previewPastReceiptDialog(SaleTransaction sale) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_receipt_preview, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView textPreview = dialogView.findViewById(R.id.text_receipt_preview_content);
        textPreview.setText(generatePastReceiptPreviewText(sale));

        dialogView.findViewById(R.id.btn_close_preview).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_print_from_preview).setOnClickListener(v -> {
            dialog.dismiss();
            printPastReceiptBluetooth(sale);
        });

        dialog.show();
    }

    private void printPastReceiptBluetooth(SaleTransaction sale) {
        this.checkBluetoothPermissions(() -> {
            new AsyncBluetoothEscPosPrint(
                this,
                new AsyncEscPosPrint.OnPrintFinished() {
                    @Override
                    public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                        Log.e("AsyncPrint", "Print error code: " + codeException);
                        Toast.makeText(MainActivity.this, "Unable to print! Make sure Bluetooth and printer are on.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                        Toast.makeText(MainActivity.this, "Receipt re-printed successfully!", Toast.LENGTH_SHORT).show();
                    }
                }
            ).execute(this.getPastReceiptPrinter(selectedBluetoothDevice, sale));
        });
    }

    private void printPastReceiptUsb(SaleTransaction sale) {
        UsbConnection usbConnection = UsbPrintersConnections.selectFirstConnected(this);
        UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);

        if (usbConnection == null || usbManager == null) {
            new AlertDialog.Builder(this)
                .setTitle("USB Connection")
                .setMessage("No USB printer found. Use USB OTG cable or Bluetooth.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        try {
            new AsyncUsbEscPosPrint(
                this,
                new AsyncEscPosPrint.OnPrintFinished() {
                    @Override
                    public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                        Toast.makeText(MainActivity.this, "USB Print Error!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                        Toast.makeText(MainActivity.this, "Receipt re-printed via USB!", Toast.LENGTH_SHORT).show();
                    }
                }
            ).execute(getPastReceiptPrinter(usbConnection, sale));
        } catch (Exception e) {
            Toast.makeText(this, "USB Print failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String generatePastReceiptPreviewText(SaleTransaction sale) {
        final int W = 32;
        String divider = receiptDesign.getDivider(W);
        StringBuilder sb = new StringBuilder();

        sb.append(centerText(receiptDesign.storeName, W)).append("\n");
        if (!receiptDesign.subheader.isEmpty()) {
            sb.append(centerText(receiptDesign.subheader, W)).append("\n");
        }
        if (!receiptDesign.address.isEmpty()) {
            sb.append(centerText(receiptDesign.address, W)).append("\n");
        }
        if (!receiptDesign.contact.isEmpty()) {
            sb.append(centerText(receiptDesign.contact, W)).append("\n");
        }

        sb.append(divider).append("\n");
        sb.append(centerText("** RE-PRINT / COPY **", W)).append("\n");
        if (receiptDesign.showDate) {
            sb.append(leftRightText("Date: " + sale.getDateTime(), "", W)).append("\n");
        }
        if (receiptDesign.showReceiptNo) {
            sb.append(leftRightText("Receipt #: " + sale.getReceiptNo(), "", W)).append("\n");
        }

        sb.append(divider).append("\n");
        sb.append(leftRightText("ITEM", "PRICE", W)).append("\n");

        List<SaleTransactionItem> items = sale.getItems();
        if (items != null) {
            for (SaleTransactionItem item : items) {
                String name = item.getItemName();
                if (name.length() > 18) name = name.substring(0, 16) + "..";
                sb.append(leftRightText(item.getQuantity() + "x " + name, String.format(Locale.US, "%.2f", item.getSubtotal()), W)).append("\n");
            }
        }

        sb.append(divider).append("\n");
        sb.append(leftRightText("TOTAL AMOUNT:", "PHP " + String.format(Locale.US, "%.2f", sale.getTotalAmount()), W)).append("\n");
        sb.append(leftRightText("Payment Method:", sale.getPaymentMode(), W)).append("\n");

        if ("CASH".equalsIgnoreCase(sale.getPaymentMode())) {
            sb.append(leftRightText("Cash Tendered:", "PHP " + String.format(Locale.US, "%.2f", sale.getAmountPaid()), W)).append("\n");
            sb.append(leftRightText("Change:", "PHP " + String.format(Locale.US, "%.2f", sale.getChangeAmount()), W)).append("\n");
        } else if ("CREDIT".equalsIgnoreCase(sale.getPaymentMode())) {
            String status = sale.isSettled() ? "PAID / SETTLED" : "OUTSTANDING";
            sb.append(leftRightText("Credit Status:", status, W)).append("\n");
        }

        String note = sale.getCustomerNote();
        if (note != null && !note.isEmpty()) {
            sb.append("Customer/Note: ").append(note).append("\n");
        }

        sb.append(divider).append("\n");
        if (!receiptDesign.footer1.isEmpty()) sb.append(centerText(receiptDesign.footer1, W)).append("\n");
        if (!receiptDesign.footer2.isEmpty()) sb.append(centerText(receiptDesign.footer2, W)).append("\n");
        if (!receiptDesign.policy.isEmpty()) sb.append(centerText("(" + receiptDesign.policy + ")", W)).append("\n");

        return sb.toString();
    }

    public AsyncEscPosPrinter getPastReceiptPrinter(DeviceConnection printerConnection, SaleTransaction sale) {
        final int W = 32;
        String divider = configDivider(receiptDesign.dividerStyle, W);
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 48f, 32);

        StringBuilder receipt = new StringBuilder();

        if (receiptDesign.isHeaderBig) {
            receipt.append("[C]<b><font size='big'>").append(receiptDesign.storeName).append("</font></b>\n");
        } else {
            receipt.append("[C]<b>").append(receiptDesign.storeName).append("</b>\n");
        }

        if (!receiptDesign.subheader.isEmpty()) {
            receipt.append("[C]").append(receiptDesign.subheader).append("\n");
        }
        if (!receiptDesign.address.isEmpty()) {
            receipt.append("[C]").append(receiptDesign.address).append("\n");
        }
        if (!receiptDesign.contact.isEmpty()) {
            receipt.append("[C]").append(receiptDesign.contact).append("\n");
        }

        receipt.append("[C]").append(divider).append("\n");
        receipt.append("[C]<b>** RE-PRINT / COPY **</b>\n");
        if (receiptDesign.showDate) {
            receipt.append("[L]Date: ").append(sale.getDateTime()).append("\n");
        }
        if (receiptDesign.showReceiptNo) {
            receipt.append("[L]Receipt #: ").append(sale.getReceiptNo()).append("\n");
        }

        receipt.append("[C]").append(divider).append("\n");
        receipt.append("[L]<b>ITEM</b>[R]<b>PRICE</b>\n");

        List<SaleTransactionItem> items = sale.getItems();
        if (items != null) {
            for (SaleTransactionItem item : items) {
                String itemName = item.getItemName();
                if (itemName.length() > 18) itemName = itemName.substring(0, 16) + "..";
                receipt.append("[L]").append(item.getQuantity()).append("x ").append(itemName)
                    .append("[R]").append(String.format(Locale.US, "%.2f", item.getSubtotal())).append("\n");
            }
        }

        receipt.append("[C]").append(divider).append("\n");
        receipt.append("[L]<b>TOTAL AMOUNT:</b>[R]<b>PHP ").append(String.format(Locale.US, "%.2f", sale.getTotalAmount())).append("</b>\n");
        receipt.append("[L]Payment Method:[R]").append(sale.getPaymentMode()).append("\n");

        if ("CASH".equalsIgnoreCase(sale.getPaymentMode())) {
            receipt.append("[L]Cash Tendered:[R]PHP ").append(String.format(Locale.US, "%.2f", sale.getAmountPaid())).append("\n")
                .append("[L]Change:[R]PHP ").append(String.format(Locale.US, "%.2f", sale.getChangeAmount())).append("\n");
        } else if ("CREDIT".equalsIgnoreCase(sale.getPaymentMode())) {
            String status = sale.isSettled() ? "PAID / SETTLED" : "OUTSTANDING";
            receipt.append("[L]Credit Status:[R]").append(status).append("\n");
        }

        String note = sale.getCustomerNote();
        if (note != null && !note.isEmpty()) {
            receipt.append("[L]Customer/Note: ").append(note).append("\n");
        }

        receipt.append("[C]").append(divider).append("\n");
        if (!receiptDesign.footer1.isEmpty()) receipt.append("[C]").append(receiptDesign.footer1).append("\n");
        if (!receiptDesign.footer2.isEmpty()) receipt.append("[C]").append(receiptDesign.footer2).append("\n");
        if (!receiptDesign.policy.isEmpty()) receipt.append("[C]<font size='small'>(").append(receiptDesign.policy).append(")</font>\n");
        receipt.append("\n\n");

        return printer.addTextToPrint(receipt.toString());
    }

    private void onPrintZReportClicked() {
        SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String today = dFormat.format(new Date());

        String[] options = {"👁️ View Z-Report on Screen", "🖨️ Print to Bluetooth 58mm", "🔌 Print to USB 58mm"};
        new AlertDialog.Builder(this)
            .setTitle("Daily Z-Report (" + today + ")")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    showZReportPreviewDialog(today);
                } else if (which == 1) {
                    printZReportBluetooth(today);
                } else if (which == 2) {
                    printZReportUsb(today);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showZReportPreviewDialog(String dateOnly) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_receipt_preview, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView textPreview = dialogView.findViewById(R.id.text_receipt_preview_content);
        textPreview.setText(generateZReportText(dateOnly));

        dialogView.findViewById(R.id.btn_close_preview).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_print_from_preview).setOnClickListener(v -> {
            dialog.dismiss();
            printZReportBluetooth(dateOnly);
        });

        dialog.show();
    }

    private void printZReportBluetooth(String dateOnly) {
        this.checkBluetoothPermissions(() -> {
            new AsyncBluetoothEscPosPrint(
                this,
                new AsyncEscPosPrint.OnPrintFinished() {
                    @Override
                    public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                        Log.e("AsyncPrint", "Print error code: " + codeException);
                        Toast.makeText(MainActivity.this, "Unable to print Z-Report! Make sure Bluetooth and printer are on.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                        Toast.makeText(MainActivity.this, "Daily Z-Report printed successfully!", Toast.LENGTH_SHORT).show();
                    }
                }
            ).execute(this.getZReportPrinter(selectedBluetoothDevice, dateOnly));
        });
    }

    private void printZReportUsb(String dateOnly) {
        UsbConnection usbConnection = UsbPrintersConnections.selectFirstConnected(this);
        UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);

        if (usbConnection == null || usbManager == null) {
            new AlertDialog.Builder(this)
                .setTitle("USB Connection")
                .setMessage("No USB printer found. Use USB OTG cable or Bluetooth.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        try {
            new AsyncUsbEscPosPrint(
                this,
                new AsyncEscPosPrint.OnPrintFinished() {
                    @Override
                    public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                        Toast.makeText(MainActivity.this, "USB Print Error!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                        Toast.makeText(MainActivity.this, "Daily Z-Report printed via USB!", Toast.LENGTH_SHORT).show();
                    }
                }
            ).execute(getZReportPrinter(usbConnection, dateOnly));
        } catch (Exception e) {
            Toast.makeText(this, "USB Print failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String generateZReportText(String dateOnly) {
        final int W = 32;
        String divider = receiptDesign.getDivider(W);
        StringBuilder sb = new StringBuilder();

        sb.append(centerText(receiptDesign.storeName, W)).append("\n");
        sb.append(centerText("DAILY Z-READING REPORT", W)).append("\n");
        sb.append(divider).append("\n");

        SimpleDateFormat fullFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        sb.append(leftRightText("Printed: " + fullFmt.format(new Date()), "", W)).append("\n");
        sb.append(leftRightText("Business Date: " + dateOnly, "", W)).append("\n");
        sb.append(divider).append("\n");

        double totalSales = dbHelper.getTodaySalesTotal(dateOnly);
        int orderCount = dbHelper.getTodayOrderCount(dateOnly);
        double cashTotal = dbHelper.getTodayCashTotal(dateOnly);
        double gcashTotal = dbHelper.getTodayGcashTotal(dateOnly);
        double unpaidCredit = dbHelper.getUnpaidCreditTotal();

        sb.append(leftRightText("DAILY SUMMARY", "", W)).append("\n");
        sb.append(leftRightText("Gross Sales:", String.format(Locale.US, "PHP %.2f", totalSales), W)).append("\n");
        sb.append(leftRightText("Transactions:", String.valueOf(orderCount), W)).append("\n");
        sb.append(divider).append("\n");

        sb.append(leftRightText("PAYMENT BREAKDOWN", "", W)).append("\n");
        sb.append(leftRightText("Cash Received:", String.format(Locale.US, "PHP %.2f", cashTotal), W)).append("\n");
        sb.append(leftRightText("GCash Payments:", String.format(Locale.US, "PHP %.2f", gcashTotal), W)).append("\n");
        sb.append(leftRightText("Unpaid Store Credit:", String.format(Locale.US, "PHP %.2f", unpaidCredit), W)).append("\n");
        sb.append(divider).append("\n");

        int prodCount = dbHelper.getProductCount();
        int stockUnits = dbHelper.getTotalStockUnits();
        double invValue = dbHelper.getTotalInventoryValue();

        sb.append(leftRightText("INVENTORY SNAPSHOT", "", W)).append("\n");
        sb.append(leftRightText("Total Products:", String.valueOf(prodCount), W)).append("\n");
        sb.append(leftRightText("In-Stock Units:", String.valueOf(stockUnits), W)).append("\n");
        sb.append(leftRightText("Current Stock Value:", String.format(Locale.US, "PHP %.2f", invValue), W)).append("\n");
        sb.append(divider).append("\n");

        sb.append(centerText("End of Daily Z-Report", W)).append("\n");
        sb.append(centerText("Morps POS System", W)).append("\n");

        return sb.toString();
    }

    public AsyncEscPosPrinter getZReportPrinter(DeviceConnection printerConnection, String dateOnly) {
        final int W = 32;
        String divider = configDivider(receiptDesign.dividerStyle, W);
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 48f, 32);

        StringBuilder sb = new StringBuilder();
        sb.append("[C]<b><font size='big'>").append(receiptDesign.storeName).append("</font></b>\n");
        sb.append("[C]<b>DAILY Z-READING REPORT</b>\n");
        sb.append("[C]").append(divider).append("\n");

        SimpleDateFormat fullFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        sb.append("[L]Printed: ").append(fullFmt.format(new Date())).append("\n");
        sb.append("[L]Business Date: ").append(dateOnly).append("\n");
        sb.append("[C]").append(divider).append("\n");

        double totalSales = dbHelper.getTodaySalesTotal(dateOnly);
        int orderCount = dbHelper.getTodayOrderCount(dateOnly);
        double cashTotal = dbHelper.getTodayCashTotal(dateOnly);
        double gcashTotal = dbHelper.getTodayGcashTotal(dateOnly);
        double unpaidCredit = dbHelper.getUnpaidCreditTotal();

        sb.append("[L]<b>DAILY SUMMARY</b>\n");
        sb.append("[L]Gross Sales:[R]<b>PHP ").append(String.format(Locale.US, "%.2f", totalSales)).append("</b>\n");
        sb.append("[L]Transactions:[R]").append(orderCount).append("\n");
        sb.append("[C]").append(divider).append("\n");

        sb.append("[L]<b>PAYMENT BREAKDOWN</b>\n");
        sb.append("[L]Cash Received:[R]PHP ").append(String.format(Locale.US, "%.2f", cashTotal)).append("\n");
        sb.append("[L]GCash Payments:[R]PHP ").append(String.format(Locale.US, "%.2f", gcashTotal)).append("\n");
        sb.append("[L]Unpaid Store Credit:[R]PHP ").append(String.format(Locale.US, "%.2f", unpaidCredit)).append("\n");
        sb.append("[C]").append(divider).append("\n");

        int prodCount = dbHelper.getProductCount();
        int stockUnits = dbHelper.getTotalStockUnits();
        double invValue = dbHelper.getTotalInventoryValue();

        sb.append("[L]<b>INVENTORY SNAPSHOT</b>\n");
        sb.append("[L]Total Products:[R]").append(prodCount).append("\n");
        sb.append("[L]In-Stock Units:[R]").append(stockUnits).append("\n");
        sb.append("[L]Stock Value:[R]PHP ").append(String.format(Locale.US, "%.2f", invValue)).append("\n");
        sb.append("[C]").append(divider).append("\n");

        sb.append("[C]End of Daily Z-Report\n");
        sb.append("[C]Morps POS System\n\n\n");

        return printer.addTextToPrint(sb.toString());
    }

    private void setupPrinterButtons() {
        buttonBrowsePrinter.setOnClickListener(v -> browseBluetoothDevice());
        findViewById(R.id.button_bluetooth).setOnClickListener(v -> printBluetooth());
        findViewById(R.id.button_usb).setOnClickListener(v -> printUsb());
    }

    /*==============================================================================================
    ================================RECEIPT CUSTOMIZATION EDITOR====================================
    ==============================================================================================*/

    private void openReceiptDesignDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_customize_receipt, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText editStoreName = dialogView.findViewById(R.id.edit_design_store_name);
        EditText editSubheader = dialogView.findViewById(R.id.edit_design_subheader);
        EditText editAddress = dialogView.findViewById(R.id.edit_design_address);
        EditText editContact = dialogView.findViewById(R.id.edit_design_contact);
        EditText editReceiptTitle = dialogView.findViewById(R.id.edit_design_receipt_title);
        RadioGroup radioGroupHeaderSize = dialogView.findViewById(R.id.radiogroup_header_size);
        RadioGroup radioGroupDivider = dialogView.findViewById(R.id.radiogroup_divider);
        EditText editFooter1 = dialogView.findViewById(R.id.edit_design_footer_1);
        EditText editFooter2 = dialogView.findViewById(R.id.edit_design_footer_2);
        EditText editPolicy = dialogView.findViewById(R.id.edit_design_policy);
        CheckBox checkShowDate = dialogView.findViewById(R.id.check_design_show_date);
        CheckBox checkShowReceiptNo = dialogView.findViewById(R.id.check_design_show_receipt_no);
        TextView textLivePreview = dialogView.findViewById(R.id.text_design_live_preview);

        // Pre-fill with current config
        editStoreName.setText(receiptDesign.storeName);
        editSubheader.setText(receiptDesign.subheader);
        editAddress.setText(receiptDesign.address);
        editContact.setText(receiptDesign.contact);
        editReceiptTitle.setText(receiptDesign.receiptTitle);
        if (receiptDesign.isHeaderBig) {
            ((RadioButton) dialogView.findViewById(R.id.radio_header_big)).setChecked(true);
        } else {
            ((RadioButton) dialogView.findViewById(R.id.radio_header_normal)).setChecked(true);
        }

        if ("-".equals(receiptDesign.dividerStyle)) {
            ((RadioButton) dialogView.findViewById(R.id.radio_divider_dash)).setChecked(true);
        } else if ("*".equals(receiptDesign.dividerStyle)) {
            ((RadioButton) dialogView.findViewById(R.id.radio_divider_star)).setChecked(true);
        } else {
            ((RadioButton) dialogView.findViewById(R.id.radio_divider_double)).setChecked(true);
        }

        editFooter1.setText(receiptDesign.footer1);
        editFooter2.setText(receiptDesign.footer2);
        editPolicy.setText(receiptDesign.policy);
        checkShowDate.setChecked(receiptDesign.showDate);
        checkShowReceiptNo.setChecked(receiptDesign.showReceiptNo);

        // Live preview updater callback
        Runnable updateLivePreview = () -> {
            ReceiptDesignConfig temp = new ReceiptDesignConfig();
            temp.storeName = editStoreName.getText().toString().trim();
            temp.subheader = editSubheader.getText().toString().trim();
            temp.address = editAddress.getText().toString().trim();
            temp.contact = editContact.getText().toString().trim();
            temp.receiptTitle = editReceiptTitle.getText().toString().trim();
            temp.isHeaderBig = radioGroupHeaderSize.getCheckedRadioButtonId() == R.id.radio_header_big;

            int dividerId = radioGroupDivider.getCheckedRadioButtonId();
            if (dividerId == R.id.radio_divider_dash) temp.dividerStyle = "-";
            else if (dividerId == R.id.radio_divider_star) temp.dividerStyle = "*";
            else temp.dividerStyle = "=";

            temp.footer1 = editFooter1.getText().toString().trim();
            temp.footer2 = editFooter2.getText().toString().trim();
            temp.policy = editPolicy.getText().toString().trim();
            temp.showDate = checkShowDate.isChecked();
            temp.showReceiptNo = checkShowReceiptNo.isChecked();

            textLivePreview.setText(generatePreviewText(temp, true));
        };

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLivePreview.run();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        editStoreName.addTextChangedListener(watcher);
        editSubheader.addTextChangedListener(watcher);
        editAddress.addTextChangedListener(watcher);
        editContact.addTextChangedListener(watcher);
        editReceiptTitle.addTextChangedListener(watcher);
        editFooter1.addTextChangedListener(watcher);
        editFooter2.addTextChangedListener(watcher);
        editPolicy.addTextChangedListener(watcher);

        radioGroupHeaderSize.setOnCheckedChangeListener((group, id) -> updateLivePreview.run());
        radioGroupDivider.setOnCheckedChangeListener((group, id) -> updateLivePreview.run());
        checkShowDate.setOnCheckedChangeListener((buttonView, isChecked) -> updateLivePreview.run());
        checkShowReceiptNo.setOnCheckedChangeListener((buttonView, isChecked) -> updateLivePreview.run());

        // Initialize preview
        updateLivePreview.run();

        // Save action
        dialogView.findViewById(R.id.btn_save_receipt_design).setOnClickListener(v -> {
            receiptDesign.storeName = editStoreName.getText().toString().trim();
            if (receiptDesign.storeName.isEmpty()) receiptDesign.storeName = "MORPS POS";

            receiptDesign.subheader = editSubheader.getText().toString().trim();
            receiptDesign.address = editAddress.getText().toString().trim();
            receiptDesign.contact = editContact.getText().toString().trim();
            receiptDesign.receiptTitle = editReceiptTitle.getText().toString().trim();
            if (receiptDesign.receiptTitle.isEmpty()) receiptDesign.receiptTitle = "ORDER RECEIPT";

            receiptDesign.isHeaderBig = radioGroupHeaderSize.getCheckedRadioButtonId() == R.id.radio_header_big;

            int dividerId = radioGroupDivider.getCheckedRadioButtonId();
            if (dividerId == R.id.radio_divider_dash) receiptDesign.dividerStyle = "-";
            else if (dividerId == R.id.radio_divider_star) receiptDesign.dividerStyle = "*";
            else receiptDesign.dividerStyle = "=";

            receiptDesign.footer1 = editFooter1.getText().toString().trim();
            receiptDesign.footer2 = editFooter2.getText().toString().trim();
            receiptDesign.policy = editPolicy.getText().toString().trim();
            receiptDesign.showDate = checkShowDate.isChecked();
            receiptDesign.showReceiptNo = checkShowReceiptNo.isChecked();

            receiptDesign.save(this);
            applyStoreDesignToUI();
            Toast.makeText(this, "Receipt design saved successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // Reset action
        dialogView.findViewById(R.id.btn_reset_receipt_design).setOnClickListener(v -> {
            receiptDesign.reset();
            editStoreName.setText(receiptDesign.storeName);
            editSubheader.setText(receiptDesign.subheader);
            editAddress.setText(receiptDesign.address);
            editContact.setText(receiptDesign.contact);
            editReceiptTitle.setText(receiptDesign.receiptTitle);
            ((RadioButton) dialogView.findViewById(R.id.radio_header_big)).setChecked(true);
            ((RadioButton) dialogView.findViewById(R.id.radio_divider_double)).setChecked(true);
            editFooter1.setText(receiptDesign.footer1);
            editFooter2.setText(receiptDesign.footer2);
            editPolicy.setText(receiptDesign.policy);
            checkShowDate.setChecked(true);
            checkShowReceiptNo.setChecked(true);
            updateLivePreview.run();
            Toast.makeText(this, "Receipt design reset to default", Toast.LENGTH_SHORT).show();
        });

        dialogView.findViewById(R.id.btn_cancel_receipt_design).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openReceiptPreviewDialog() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty! Please add items first.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_receipt_preview, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView textPreview = dialogView.findViewById(R.id.text_receipt_preview_content);
        textPreview.setText(generatePreviewText(receiptDesign, false));

        dialogView.findViewById(R.id.btn_close_preview).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_print_from_preview).setOnClickListener(v -> {
            dialog.dismiss();
            printBluetooth();
        });

        dialog.show();
    }

    /**
     * Generates plain monospace 32-column text for on-screen receipt preview.
     */
    private String generatePreviewText(ReceiptDesignConfig config, boolean isSample) {
        final int W = 32;
        String divider = config.getDivider(W);
        StringBuilder sb = new StringBuilder();

        sb.append(centerText(config.storeName, W)).append("\n");
        if (!config.subheader.isEmpty()) {
            sb.append(centerText(config.subheader, W)).append("\n");
        }
        if (!config.address.isEmpty()) {
            sb.append(centerText(config.address, W)).append("\n");
        }
        if (!config.contact.isEmpty()) {
            sb.append(centerText(config.contact, W)).append("\n");
        }

        sb.append(divider).append("\n");
        if (!config.receiptTitle.isEmpty()) {
            sb.append(centerText(config.receiptTitle, W)).append("\n");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        if (config.showDate) {
            sb.append(leftRightText("Date: " + dateFormat.format(new Date()), "", W)).append("\n");
        }
        if (config.showReceiptNo) {
            sb.append(leftRightText("Receipt #: MORPS-" + receiptCounter, "", W)).append("\n");
        }

        sb.append(divider).append("\n");
        sb.append(leftRightText("ITEM", "PRICE", W)).append("\n");

        if (isSample || cartItems.isEmpty()) {
            sb.append(leftRightText("1x Pancit Canton", "18.00", W)).append("\n");
            sb.append(leftRightText("2x Coke Mismo", "40.00", W)).append("\n");
            sb.append(divider).append("\n");
            sb.append(leftRightText("TOTAL AMOUNT:", "PHP 58.00", W)).append("\n");
            sb.append(leftRightText("Payment Method:", "CASH", W)).append("\n");
            sb.append(leftRightText("Cash Tendered:", "PHP 100.00", W)).append("\n");
            sb.append(leftRightText("Change:", "PHP 42.00", W)).append("\n");
        } else {
            double total = calculateTotal();
            for (CartItem item : cartItems) {
                String name = item.getName();
                if (name.length() > 18) name = name.substring(0, 16) + "..";
                sb.append(leftRightText(item.getQuantity() + "x " + name, String.format(Locale.US, "%.2f", item.getTotalPrice()), W)).append("\n");
            }
            sb.append(divider).append("\n");
            sb.append(leftRightText("TOTAL AMOUNT:", "PHP " + String.format(Locale.US, "%.2f", total), W)).append("\n");

            String paymentMode = "CASH";
            int checkedId = radioGroupPayment.getCheckedRadioButtonId();
            if (checkedId == R.id.radio_gcash) paymentMode = "GCASH";
            else if (checkedId == R.id.radio_utang) paymentMode = "CREDIT";

            sb.append(leftRightText("Payment Method:", paymentMode, W)).append("\n");

            if ("CASH".equals(paymentMode)) {
                double cash = total;
                try {
                    String s = editCashTendered.getText().toString().trim();
                    if (!s.isEmpty()) cash = Double.parseDouble(s);
                } catch (Exception ignored) {}
                double change = Math.max(0.0, cash - total);
                sb.append(leftRightText("Cash Tendered:", "PHP " + String.format(Locale.US, "%.2f", cash), W)).append("\n");
                sb.append(leftRightText("Change:", "PHP " + String.format(Locale.US, "%.2f", change), W)).append("\n");
            } else if ("CREDIT".equals(paymentMode)) {
                sb.append(leftRightText("Credit Balance:", "PHP " + String.format(Locale.US, "%.2f", total), W)).append("\n");
            }

            String note = editCustomerNote.getText().toString().trim();
            if (!note.isEmpty()) {
                sb.append("Customer/Note: ").append(note).append("\n");
            }
        }

        sb.append(divider).append("\n");
        if (!config.footer1.isEmpty()) sb.append(centerText(config.footer1, W)).append("\n");
        if (!config.footer2.isEmpty()) sb.append(centerText(config.footer2, W)).append("\n");
        if (!config.policy.isEmpty()) sb.append(centerText("(" + config.policy + ")", W)).append("\n");

        return sb.toString();
    }

    private String centerText(String text, int width) {
        if (text == null) return "";
        if (text.length() >= width) return text.substring(0, width);
        int pad = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pad; i++) sb.append(" ");
        sb.append(text);
        return sb.toString();
    }

    private String leftRightText(String left, String right, int width) {
        if (left == null) left = "";
        if (right == null) right = "";
        int spaceCount = width - left.length() - right.length();
        if (spaceCount <= 0) {
            return left + " " + right;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        for (int i = 0; i < spaceCount; i++) sb.append(" ");
        sb.append(right);
        return sb.toString();
    }

    /*==============================================================================================
    ======================================BLUETOOTH PART============================================
    ==============================================================================================*/

    public interface OnBluetoothPermissionsGranted {
        void onPermissionsGranted();
    }

    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    public static final int PERMISSION_BLUETOOTH_SCAN = 4;

    public OnBluetoothPermissionsGranted onBluetoothPermissionsGranted;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            this.checkBluetoothPermissions(this.onBluetoothPermissionsGranted);
        }
    }

    public void checkBluetoothPermissions(OnBluetoothPermissionsGranted onBluetoothPermissionsGranted) {
        this.onBluetoothPermissionsGranted = onBluetoothPermissionsGranted;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MainActivity.PERMISSION_BLUETOOTH);
        } else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, MainActivity.PERMISSION_BLUETOOTH_ADMIN);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, MainActivity.PERMISSION_BLUETOOTH_CONNECT);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, MainActivity.PERMISSION_BLUETOOTH_SCAN);
        } else if (this.onBluetoothPermissionsGranted != null) {
            this.onBluetoothPermissionsGranted.onPermissionsGranted();
        }
    }

    public void browseBluetoothDevice() {
        this.checkBluetoothPermissions(() -> {
            final BluetoothConnection[] bluetoothDevicesList = (new BluetoothPrintersConnections()).getList();

            if (bluetoothDevicesList != null && bluetoothDevicesList.length > 0) {
                final String[] items = new String[bluetoothDevicesList.length + 1];
                items[0] = "Default printer (First paired)";
                int i = 0;
                for (BluetoothConnection device : bluetoothDevicesList) {
                    items[++i] = device.getDevice().getName();
                }

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Select Bluetooth 58mm Printer");
                alertDialog.setItems(
                    items,
                    (dialogInterface, i1) -> {
                        int index = i1 - 1;
                        if (index == -1) {
                            selectedBluetoothDevice = null;
                            textPrinterName.setText("🖨️ Printer: Default Bluetooth");
                        } else {
                            selectedBluetoothDevice = bluetoothDevicesList[index];
                            textPrinterName.setText("🖨️ Printer: " + items[i1]);
                        }
                    }
                );

                AlertDialog alert = alertDialog.create();
                alert.setCanceledOnTouchOutside(true);
                alert.show();
            } else {
                Toast.makeText(this, "No paired Bluetooth printers found in phone settings.", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void printBluetooth() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty! Please add items first.", Toast.LENGTH_SHORT).show();
            return;
        }

        this.checkBluetoothPermissions(() -> {
            new AsyncBluetoothEscPosPrint(
                this,
                new AsyncEscPosPrint.OnPrintFinished() {
                    @Override
                    public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                        Log.e("AsyncPrint", "Print error code: " + codeException);
                        Toast.makeText(MainActivity.this, "Unable to print! Make sure Bluetooth and printer are on.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                        Toast.makeText(MainActivity.this, "Receipt printed successfully!", Toast.LENGTH_SHORT).show();
                    }
                }
            ).execute(this.getReceiptPrinter(selectedBluetoothDevice));
        });
    }

    /*==============================================================================================
    ===========================================USB PART=============================================
    ==============================================================================================*/

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MainActivity.ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbManager != null && usbDevice != null) {
                            new AsyncUsbEscPosPrint(
                                context,
                                new AsyncEscPosPrint.OnPrintFinished() {
                                    @Override
                                    public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                                        Toast.makeText(MainActivity.this, "USB Print Error!", Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                                        Toast.makeText(MainActivity.this, "Receipt printed via USB!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            ).execute(getReceiptPrinter(new UsbConnection(usbManager, usbDevice)));
                        }
                    }
                }
            }
        }
    };

    public void printUsb() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty! Please add items first.", Toast.LENGTH_SHORT).show();
            return;
        }

        UsbConnection usbConnection = UsbPrintersConnections.selectFirstConnected(this);
        UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);

        if (usbConnection == null || usbManager == null) {
            new AlertDialog.Builder(this)
                .setTitle("USB Connection")
                .setMessage("No USB printer found. Use USB OTG cable or Bluetooth.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        PendingIntent permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            new Intent(MainActivity.ACTION_USB_PERMISSION),
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
        );
        IntentFilter filter = new IntentFilter(MainActivity.ACTION_USB_PERMISSION);
        registerReceiver(this.usbReceiver, filter);
        usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);
    }

    /*==============================================================================================
    ===================================SARI-SARI 58mm RECEIPT=======================================
    ==============================================================================================*/

    /**
     * Generates ESC/POS formatted receipt tailored for 58mm thermal printers (32 chars per line).
     * Applies the store owner's customized receipt design.
     */
    @SuppressLint("SimpleDateFormat")
    public AsyncEscPosPrinter getReceiptPrinter(DeviceConnection printerConnection) {
        final int W = 32;
        String divider = configDivider(receiptDesign.dividerStyle, W);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        String dateStr = dateFormat.format(new Date());

        double total = calculateTotal();

        // Payment Mode
        String paymentMode = "CASH";
        double amountPaid = total;
        double change = 0.0;
        int checkedPaymentId = radioGroupPayment.getCheckedRadioButtonId();

        if (checkedPaymentId == R.id.radio_gcash) {
            paymentMode = "GCASH";
        } else if (checkedPaymentId == R.id.radio_utang) {
            paymentMode = "CREDIT";
            amountPaid = 0.0;
        } else {
            String cashStr = editCashTendered.getText().toString().trim();
            if (!cashStr.isEmpty()) {
                try {
                    amountPaid = Double.parseDouble(cashStr);
                    change = Math.max(0.0, amountPaid - total);
                } catch (NumberFormatException ignored) {}
            }
        }

        String note = editCustomerNote.getText().toString().trim();

        // Build item rows: format "[L]qty x ItemName[R]Subtotal\n"
        StringBuilder itemsBuilder = new StringBuilder();
        for (CartItem item : cartItems) {
            String itemName = item.getName();
            if (itemName.length() > 18) {
                itemName = itemName.substring(0, 16) + "..";
            }
            itemsBuilder.append("[L]").append(item.getQuantity()).append("x ").append(itemName)
                .append("[R]").append(String.format(Locale.US, "%.2f", item.getTotalPrice()))
                .append("\n");
        }

        // Standard 58mm printer: 203 DPI, 48mm width, 32 characters per line
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 48f, 32);

        StringBuilder receipt = new StringBuilder();

        // Header: Big or Normal font
        if (receiptDesign.isHeaderBig) {
            receipt.append("[C]<b><font size='big'>").append(receiptDesign.storeName).append("</font></b>\n");
        } else {
            receipt.append("[C]<b>").append(receiptDesign.storeName).append("</b>\n");
        }

        if (!receiptDesign.subheader.isEmpty()) {
            receipt.append("[C]").append(receiptDesign.subheader).append("\n");
        }
        if (!receiptDesign.address.isEmpty()) {
            receipt.append("[C]").append(receiptDesign.address).append("\n");
        }
        if (!receiptDesign.contact.isEmpty()) {
            receipt.append("[C]").append(receiptDesign.contact).append("\n");
        }

        receipt.append("[C]").append(divider).append("\n");

        if (!receiptDesign.receiptTitle.isEmpty()) {
            receipt.append("[C]<u>").append(receiptDesign.receiptTitle).append("</u>\n");
        }

        if (receiptDesign.showDate) {
            receipt.append("[L]Date: ").append(dateStr).append("\n");
        }
        if (receiptDesign.showReceiptNo) {
            receipt.append("[L]Receipt #: MORPS-").append(receiptCounter).append("\n");
        }

        receipt.append("[C]").append(divider).append("\n");
        receipt.append("[L]<b>ITEM</b>[R]<b>PRICE</b>\n");
        receipt.append(itemsBuilder);
        receipt.append("[C]").append(divider).append("\n");
        receipt.append("[L]<b>TOTAL AMOUNT:</b>[R]<b>PHP ").append(String.format(Locale.US, "%.2f", total)).append("</b>\n");
        receipt.append("[L]Payment Method:[R]").append(paymentMode).append("\n");

        if (paymentMode.equals("CASH")) {
            receipt.append("[L]Cash Tendered:[R]PHP ").append(String.format(Locale.US, "%.2f", amountPaid)).append("\n")
                .append("[L]Change:[R]PHP ").append(String.format(Locale.US, "%.2f", change)).append("\n");
        } else if (paymentMode.equals("CREDIT")) {
            receipt.append("[L]Credit Balance:[R]PHP ").append(String.format(Locale.US, "%.2f", total)).append("\n");
        }

        if (!note.isEmpty()) {
            receipt.append("[L]Customer/Note: ").append(note).append("\n");
        }

        receipt.append("[C]").append(divider).append("\n");
        if (!receiptDesign.footer1.isEmpty()) {
            receipt.append("[C]").append(receiptDesign.footer1).append("\n");
        }
        if (!receiptDesign.footer2.isEmpty()) {
            receipt.append("[C]").append(receiptDesign.footer2).append("\n");
        }
        if (!receiptDesign.policy.isEmpty()) {
            receipt.append("[C]<font size='small'>(").append(receiptDesign.policy).append(")</font>\n");
        }
        receipt.append("\n\n");

        return printer.addTextToPrint(receipt.toString());
    }

    private String configDivider(String style, int len) {
        char ch = (style == null || style.isEmpty()) ? '=' : style.charAt(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(ch);
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(this.usbReceiver);
        } catch (Exception ignored) {}
    }
}

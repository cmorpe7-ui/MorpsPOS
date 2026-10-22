package com.dantsu.thermalprinter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import android.os.Build;
import android.provider.Settings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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
import com.dantsu.thermalprinter.model.GcashTransaction;
import com.dantsu.thermalprinter.model.Product;
import com.dantsu.thermalprinter.model.SaleTransaction;
import com.dantsu.thermalprinter.model.SaleTransactionItem;
import com.dantsu.thermalprinter.util.DataValidator;
import com.dantsu.thermalprinter.util.GcashAutoFillParser;
import android.content.ClipboardManager;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static class CartItem {
        private final long productId;
        private final String name;
        private final double unitPrice;
        private int quantity;

        public CartItem(long productId, String name, double unitPrice, int quantity) {
            this.productId = productId;
            this.name = name;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        public CartItem(String name, double unitPrice, int quantity) {
            this(-1, name, unitPrice, quantity);
        }

        public long getProductId() {
            return productId;
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

        public void setQuantity(int quantity) {
            this.quantity = Math.max(1, quantity);
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
        public String receiptNoLabel = "Receipt #:";
        public String receiptPrefix = "MORPS-";
        public boolean isHeaderBig = true;
        public String dividerStyle = "="; // "=", "-", "*"
        public String footer1 = "Thank You for Your Purchase!";
        public String footer2 = "Please Come Again!";
        public String policy = "No Return, No Exchange";
        public boolean showDate = true;
        public boolean showReceiptNo = true;
        public boolean enableGcash = true;
        public boolean enableStoreCredit = true;
        public boolean promptToPrint = true;
        public boolean autoPrintOnCheckout = false;
        public int paperWidthMm = 58;
        public int charactersPerLine = 32;
        public boolean enableGcashQrOnReceipt = false;
        public String gcashQrPrintMode = "ALWAYS";
        public static final String GCASH_QR_FILENAME = "gcash_qr.png";
        public boolean enablePinSecurity = false;
        public String securityPin = "1234";
        public String updateSourceUrl = "https://api.github.com/repos/cmorpe7-ui/MorpsPOS/releases/latest";

        public void load(Context context) {
            SharedPreferences prefs = context.getSharedPreferences("MorpsPOSPrefs", Context.MODE_PRIVATE);
            storeName = prefs.getString("storeName", "MORPS POS");
            subheader = prefs.getString("subheader", "Sari-Sari Store & General Mdse.");
            address = prefs.getString("address", "Brgy. San Antonio • Pasig City");
            contact = prefs.getString("contact", "Phone: 0917-000-0000");
            receiptTitle = prefs.getString("receiptTitle", "ORDER RECEIPT");
            receiptNoLabel = prefs.getString("receiptNoLabel", "Receipt #:");
            receiptPrefix = prefs.getString("receiptPrefix", "MORPS-");
            isHeaderBig = prefs.getBoolean("isHeaderBig", true);
            dividerStyle = prefs.getString("dividerStyle", "=");
            footer1 = prefs.getString("footer1", "Thank You for Your Purchase!");
            footer2 = prefs.getString("footer2", "Please Come Again!");
            policy = prefs.getString("policy", "No Return, No Exchange");
            showDate = prefs.getBoolean("showDate", true);
            showReceiptNo = prefs.getBoolean("showReceiptNo", true);
            enableGcash = prefs.getBoolean("enableGcash", true);
            enableStoreCredit = prefs.getBoolean("enableStoreCredit", true);
            promptToPrint = prefs.getBoolean("promptToPrint", true);
            autoPrintOnCheckout = prefs.getBoolean("autoPrintOnCheckout", false);
            paperWidthMm = prefs.getInt("paperWidthMm", 58);
            charactersPerLine = prefs.getInt("charactersPerLine", 32);
            enableGcashQrOnReceipt = prefs.getBoolean("enableGcashQrOnReceipt", false);
            gcashQrPrintMode = prefs.getString("gcashQrPrintMode", "ALWAYS");
            enablePinSecurity = prefs.getBoolean("enablePinSecurity", false);
            securityPin = prefs.getString("securityPin", "1234");
            updateSourceUrl = prefs.getString("updateSourceUrl", "https://api.github.com/repos/cmorpe7-ui/MorpsPOS/releases/latest");
        }

        public void save(Context context) {
            SharedPreferences.Editor editor = context.getSharedPreferences("MorpsPOSPrefs", Context.MODE_PRIVATE).edit();
            editor.putString("storeName", storeName);
            editor.putString("subheader", subheader);
            editor.putString("address", address);
            editor.putString("contact", contact);
            editor.putString("receiptTitle", receiptTitle);
            editor.putString("receiptNoLabel", receiptNoLabel);
            editor.putString("receiptPrefix", receiptPrefix);
            editor.putBoolean("isHeaderBig", isHeaderBig);
            editor.putString("dividerStyle", dividerStyle);
            editor.putString("footer1", footer1);
            editor.putString("footer2", footer2);
            editor.putString("policy", policy);
            editor.putBoolean("showDate", showDate);
            editor.putBoolean("showReceiptNo", showReceiptNo);
            editor.putBoolean("enableGcash", enableGcash);
            editor.putBoolean("enableStoreCredit", enableStoreCredit);
            editor.putBoolean("promptToPrint", promptToPrint);
            editor.putBoolean("autoPrintOnCheckout", autoPrintOnCheckout);
            editor.putInt("paperWidthMm", paperWidthMm);
            editor.putInt("charactersPerLine", charactersPerLine);
            editor.putBoolean("enableGcashQrOnReceipt", enableGcashQrOnReceipt);
            editor.putString("gcashQrPrintMode", gcashQrPrintMode);
            editor.putBoolean("enablePinSecurity", enablePinSecurity);
            editor.putString("securityPin", securityPin);
            editor.putString("updateSourceUrl", updateSourceUrl);
            editor.apply();
        }

        public void reset() {
            storeName = "MORPS POS";
            subheader = "Sari-Sari Store & General Mdse.";
            address = "Brgy. San Antonio • Pasig City";
            contact = "Phone: 0917-000-0000";
            receiptTitle = "ORDER RECEIPT";
            receiptNoLabel = "Receipt #:";
            receiptPrefix = "MORPS-";
            isHeaderBig = true;
            dividerStyle = "=";
            footer1 = "Thank You for Your Purchase!";
            footer2 = "Please Come Again!";
            policy = "No Return, No Exchange";
            showDate = true;
            showReceiptNo = true;
            enableGcash = true;
            enableStoreCredit = true;
            promptToPrint = true;
            autoPrintOnCheckout = false;
            paperWidthMm = 58;
            charactersPerLine = 32;
            enableGcashQrOnReceipt = false;
            gcashQrPrintMode = "ALWAYS";
            enablePinSecurity = false;
            securityPin = "1234";
            updateSourceUrl = "https://api.github.com/repos/cmorpe7-ui/MorpsPOS/releases/latest";
        }

        public boolean hasGcashQrImage(Context context) {
            File file = new File(context.getFilesDir(), GCASH_QR_FILENAME);
            return file.exists() && file.length() > 0;
        }

        public String getEffectiveReceiptNoLabel() {
            if (receiptNoLabel == null || receiptNoLabel.trim().isEmpty()) {
                return "Receipt #:";
            }
            return receiptNoLabel.trim();
        }

        public String getEffectiveReceiptPrefix() {
            if (receiptPrefix == null) {
                return "MORPS-";
            }
            return receiptPrefix;
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
    private final List<CartItem> parkedCartItems = new ArrayList<>();
    private final ReceiptDesignConfig receiptDesign = new ReceiptDesignConfig();
    private DatabaseHelper dbHelper;
    private int receiptCounter = 1001;

    // Parked Cart UI Views
    private View bannerParkedCart;
    private TextView textParkedCartSummary;
    private Button btnHoldCart;
    private Button btnResumeParkedCart;

    // Inventory Filtering
    private String selectedInventoryStockFilter = "ALL";
    private Button chipInvFilterAll;
    private Button chipInvFilterLow;
    private Button chipInvFilterOut;

    // Settings GCash QR Dialog references
    private ImageView currentSettingsQrPreview;
    private Button currentSettingsRemoveQrBtn;
    private AlertDialog currentSettingsDialog;

    // AI Recognition & Camera Constants
    public static final int PERMISSION_CAMERA_REQUEST = 5;
    public static final int REQUEST_CAMERA_PRODUCT = 201;
    public static final int REQUEST_GALLERY_PRODUCT = 202;
    public static final int REQUEST_PICK_GCASH_QR = 203;
    public static final int REQUEST_RESTORE_DB = 204;
    public static final int REQUEST_INSTALL_LOCAL_APK = 205;
    public static final int REQUEST_CAMERA_GCASH_SCAN = 206;
    public static final int REQUEST_GALLERY_GCASH_SCAN = 207;

    private String currentProductPhotoPath;
    private Uri currentProductPhotoUri;
    private AlertDialog currentProductDialog;
    private View currentProductDialogView;
    private Product currentEditingProduct;

    private String currentGcashPhotoPath;
    private Uri currentGcashPhotoUri;
    private AlertDialog currentGcashDialog;
    private View currentGcashDialogView;

    // Active Category Filter for POS
    private String selectedPosCategory = "All";

    // Navigation Tabs
    private static final int TAB_POS = 0;
    private static final int TAB_INVENTORY = 1;
    private static final int TAB_SALES = 2;
    private static final int TAB_GCASH = 3;
    private int currentTab = TAB_POS;

    // Views - Header & Navigation
    private TextView textMainTitle;
    private TextView textMainSubtitle;
    private TextView textPrinterName;
    private Button buttonBrowsePrinter;
    private Button tabPos;
    private Button tabInventory;
    private Button tabSales;
    private Button tabGcash;
    private LinearLayout layoutPosView;
    private LinearLayout layoutInventoryView;
    private LinearLayout layoutSalesView;
    private LinearLayout layoutGcashView;

    // Views - GCash Tab
    private TextView textGcashDigitalBalance;
    private TextView textGcashDigitalFlow;
    private TextView textGcashFeesProfit;
    private TextView textGcashDrawerFlow;
    private Button btnGcashAdjustFloat;
    private Button btnGcashNewCashIn;
    private Button btnGcashNewCashOut;
    private Button btnGcashExportCsv;
    private Button btnGcashManageHistory;
    private Button chipGcashToday, chipGcashYesterday, chipGcash7days, chipGcash30days, chipGcashAlltime;
    private Button filterGcashAll, filterGcashIn, filterGcashOut;
    private EditText editGcashSearch;
    private TextView textGcashStatCashIn;
    private TextView textGcashStatCashOut;
    private TextView textGcashStatFees;
    private TextView textGcashEmpty;
    private LinearLayout layoutGcashItemsContainer;

    // State - GCash Tab
    private SalesPeriod selectedGcashPeriod = SalesPeriod.TODAY;
    private String selectedGcashFilterType = "ALL";

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

    // Views - Sales Tab (Period Chips, Net Profit, Share Bar, Top Sellers, Utang Ledger, Filters)
    public enum SalesPeriod {
        TODAY, YESTERDAY, DAYS_7, THIS_MONTH, ALL_TIME
    }
    private SalesPeriod selectedSalesPeriod = SalesPeriod.TODAY;
    private String selectedSalesFilterMode = "ALL"; // "ALL", "CASH", "GCASH", "CREDIT_UNPAID"

    private Button chipSalesToday;
    private Button chipSalesYesterday;
    private Button chipSales7Days;
    private Button chipSales30Days;
    private Button chipSalesAllTime;

    private TextView textSalesTodayDate;
    private TextView textSalesTodayTotal;
    private TextView textSalesNetProfit;
    private TextView textSalesTodayOrders;
    private LinearLayout layoutSalesShareBar;
    private View barSegmentCash;
    private View barSegmentGcash;
    private View barSegmentCredit;
    private TextView textSalesTodayCash;
    private TextView textSalesTodayGcash;
    private TextView textSalesUnpaidCredit;

    // Sales - GCash Remittance & Drawer Reconciliation
    private TextView textSalesGcashFees;
    private TextView textSalesGcashTxCount;
    private TextView textSalesGcashCashin;
    private TextView textSalesGcashCashout;
    private TextView textSalesExpectedDrawerCash;
    private TextView textSalesDrawerCalcBreakdown;
    private TextView btnGotoGcashTab;

    private LinearLayout layoutSalesTopSellersCard;
    private TextView textTopSellersPeriod;
    private LinearLayout containerTopSellersItems;

    private LinearLayout layoutDebtorsSummaryContainer;
    private LinearLayout containerDebtorsItems;

    private Button btnPrintZReport;
    private Button btnSalesExport;
    private Button btnSalesManageHistory;
    private EditText editSalesSearch;
    private Button filterSalesAll;
    private Button filterSalesCash;
    private Button filterSalesGcash;
    private Button filterSalesUtang;
    private TextView textSalesEmpty;
    private LinearLayout layoutSalesItemsContainer;

    private BluetoothConnection selectedBluetoothDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = DatabaseHelper.getInstance(this);
        dbHelper.ensureDefaultProductsSeeded();
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
        autoReconnectSavedPrinter();
        setupInventoryEvents();
        setupSalesEvents();
        setupGcashEvents();

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
        tabGcash = findViewById(R.id.tab_gcash);
        layoutPosView = findViewById(R.id.layout_pos_view);
        layoutInventoryView = findViewById(R.id.layout_inventory_view);
        layoutSalesView = findViewById(R.id.layout_sales_view);
        layoutGcashView = findViewById(R.id.layout_gcash_view);

        // GCash Elements
        textGcashDigitalBalance = findViewById(R.id.text_gcash_digital_balance);
        textGcashDigitalFlow = findViewById(R.id.text_gcash_digital_flow);
        textGcashFeesProfit = findViewById(R.id.text_gcash_fees_profit);
        textGcashDrawerFlow = findViewById(R.id.text_gcash_drawer_flow);
        btnGcashAdjustFloat = findViewById(R.id.btn_gcash_adjust_float);
        btnGcashNewCashIn = findViewById(R.id.btn_gcash_new_cash_in);
        btnGcashNewCashOut = findViewById(R.id.btn_gcash_new_cash_out);
        btnGcashExportCsv = findViewById(R.id.btn_gcash_export_csv);
        btnGcashManageHistory = findViewById(R.id.btn_gcash_manage_history);
        chipGcashToday = findViewById(R.id.chip_gcash_today);
        chipGcashYesterday = findViewById(R.id.chip_gcash_yesterday);
        chipGcash7days = findViewById(R.id.chip_gcash_7days);
        chipGcash30days = findViewById(R.id.chip_gcash_30days);
        chipGcashAlltime = findViewById(R.id.chip_gcash_alltime);
        filterGcashAll = findViewById(R.id.filter_gcash_all);
        filterGcashIn = findViewById(R.id.filter_gcash_in);
        filterGcashOut = findViewById(R.id.filter_gcash_out);
        editGcashSearch = findViewById(R.id.edit_gcash_search);
        textGcashStatCashIn = findViewById(R.id.text_gcash_stat_cash_in);
        textGcashStatCashOut = findViewById(R.id.text_gcash_stat_cash_out);
        textGcashStatFees = findViewById(R.id.text_gcash_stat_fees);
        textGcashEmpty = findViewById(R.id.text_gcash_empty);
        layoutGcashItemsContainer = findViewById(R.id.layout_gcash_items_container);

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
        chipSalesToday = findViewById(R.id.chip_sales_today);
        chipSalesYesterday = findViewById(R.id.chip_sales_yesterday);
        chipSales7Days = findViewById(R.id.chip_sales_7days);
        chipSales30Days = findViewById(R.id.chip_sales_30days);
        chipSalesAllTime = findViewById(R.id.chip_sales_all_time);

        textSalesTodayDate = findViewById(R.id.text_sales_today_date);
        textSalesTodayTotal = findViewById(R.id.text_sales_today_total);
        textSalesNetProfit = findViewById(R.id.text_sales_net_profit);
        textSalesTodayOrders = findViewById(R.id.text_sales_today_orders);

        layoutSalesShareBar = findViewById(R.id.layout_sales_share_bar);
        barSegmentCash = findViewById(R.id.bar_segment_cash);
        barSegmentGcash = findViewById(R.id.bar_segment_gcash);
        barSegmentCredit = findViewById(R.id.bar_segment_credit);

        textSalesTodayCash = findViewById(R.id.text_sales_today_cash);
        textSalesTodayGcash = findViewById(R.id.text_sales_today_gcash);
        textSalesUnpaidCredit = findViewById(R.id.text_sales_unpaid_credit);

        textSalesGcashFees = findViewById(R.id.text_sales_gcash_fees);
        textSalesGcashTxCount = findViewById(R.id.text_sales_gcash_tx_count);
        textSalesGcashCashin = findViewById(R.id.text_sales_gcash_cashin);
        textSalesGcashCashout = findViewById(R.id.text_sales_gcash_cashout);
        textSalesExpectedDrawerCash = findViewById(R.id.text_sales_expected_drawer_cash);
        textSalesDrawerCalcBreakdown = findViewById(R.id.text_sales_drawer_calc_breakdown);
        btnGotoGcashTab = findViewById(R.id.btn_goto_gcash_tab);
        if (btnGotoGcashTab != null) {
            btnGotoGcashTab.setOnClickListener(v -> switchTab(TAB_GCASH));
        }

        layoutSalesTopSellersCard = findViewById(R.id.layout_sales_top_sellers_card);
        textTopSellersPeriod = findViewById(R.id.text_top_sellers_period);
        containerTopSellersItems = findViewById(R.id.container_top_sellers_items);

        layoutDebtorsSummaryContainer = findViewById(R.id.layout_debtors_summary_container);
        containerDebtorsItems = findViewById(R.id.container_debtors_items);

        btnPrintZReport = findViewById(R.id.btn_print_z_report);
        btnSalesExport = findViewById(R.id.btn_sales_export);
        btnSalesManageHistory = findViewById(R.id.btn_sales_manage_history);
        editSalesSearch = findViewById(R.id.edit_sales_search);

        filterSalesAll = findViewById(R.id.filter_sales_all);
        filterSalesCash = findViewById(R.id.filter_sales_cash);
        filterSalesGcash = findViewById(R.id.filter_sales_gcash);
        filterSalesUtang = findViewById(R.id.filter_sales_utang);

        textSalesEmpty = findViewById(R.id.text_sales_empty);
        layoutSalesItemsContainer = findViewById(R.id.layout_sales_items_container);

        View btnEditPm = findViewById(R.id.btn_edit_payment_methods);
        if (btnEditPm != null) {
            btnEditPm.setOnClickListener(v -> openPaymentMethodsDialog());
        }

        // Parked cart bindings
        bannerParkedCart = findViewById(R.id.banner_parked_cart);
        textParkedCartSummary = findViewById(R.id.text_parked_cart_summary);
        btnHoldCart = findViewById(R.id.button_hold_cart);
        btnResumeParkedCart = findViewById(R.id.btn_resume_parked_cart);
        if (btnHoldCart != null) btnHoldCart.setOnClickListener(v -> holdCart());
        if (btnResumeParkedCart != null) btnResumeParkedCart.setOnClickListener(v -> resumeParkedCart());


        // Inventory Stock Filter chips
        chipInvFilterAll = findViewById(R.id.chip_inv_filter_all);
        chipInvFilterLow = findViewById(R.id.chip_inv_filter_low);
        chipInvFilterOut = findViewById(R.id.chip_inv_filter_out);
        setupInventoryFilterChips();

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
        applyPaymentMethodVisibility();
    }

    private void applyPaymentMethodVisibility() {
        RadioButton radioGcash = findViewById(R.id.radio_gcash);
        RadioButton radioUtang = findViewById(R.id.radio_utang);
        RadioButton radioCash = findViewById(R.id.radio_cash);

        if (radioGcash != null) {
            radioGcash.setVisibility(receiptDesign.enableGcash ? View.VISIBLE : View.GONE);
        }
        if (radioUtang != null) {
            radioUtang.setVisibility(receiptDesign.enableStoreCredit ? View.VISIBLE : View.GONE);
        }

        if (radioGroupPayment != null) {
            int checkedId = radioGroupPayment.getCheckedRadioButtonId();
            if ((checkedId == R.id.radio_gcash && !receiptDesign.enableGcash) ||
                (checkedId == R.id.radio_utang && !receiptDesign.enableStoreCredit)) {
                if (radioCash != null) {
                    radioCash.setChecked(true);
                }
            }
        }
    }

    private void openPaymentMethodsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_methods, null);
        builder.setView(dialogView);

        CheckBox checkGcash = dialogView.findViewById(R.id.check_pm_gcash);
        CheckBox checkCredit = dialogView.findViewById(R.id.check_pm_credit);

        checkGcash.setChecked(receiptDesign.enableGcash);
        checkCredit.setChecked(receiptDesign.enableStoreCredit);

        builder.setPositiveButton("Save", (dialog, which) -> {
            receiptDesign.enableGcash = checkGcash.isChecked();
            receiptDesign.enableStoreCredit = checkCredit.isChecked();
            receiptDesign.save(this);
            applyPaymentMethodVisibility();
            Toast.makeText(this, "Payment methods updated!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void setupTabs() {
        tabPos.setOnClickListener(v -> switchTab(TAB_POS));
        tabInventory.setOnClickListener(v -> switchTab(TAB_INVENTORY));
        tabSales.setOnClickListener(v -> switchTab(TAB_SALES));
        tabGcash.setOnClickListener(v -> switchTab(TAB_GCASH));
    }

    private void switchTab(int tab) {
        currentTab = tab;

        int inactiveColor = ContextCompat.getColor(this, R.color.colorSlate600);
        int activeColor = ContextCompat.getColor(this, R.color.colorOnPrimary);

        tabPos.setBackgroundResource(R.drawable.tab_pill_inactive);
        tabPos.setTextColor(inactiveColor);
        tabInventory.setBackgroundResource(R.drawable.tab_pill_inactive);
        tabInventory.setTextColor(inactiveColor);
        tabSales.setBackgroundResource(R.drawable.tab_pill_inactive);
        tabSales.setTextColor(inactiveColor);
        tabGcash.setBackgroundResource(R.drawable.tab_pill_inactive);
        tabGcash.setTextColor(inactiveColor);

        layoutPosView.setVisibility(View.GONE);
        layoutInventoryView.setVisibility(View.GONE);
        layoutSalesView.setVisibility(View.GONE);
        layoutGcashView.setVisibility(View.GONE);

        if (tab == TAB_POS) {
            tabPos.setBackgroundResource(R.drawable.tab_pill_active);
            tabPos.setTextColor(activeColor);
            layoutPosView.setVisibility(View.VISIBLE);
            renderPosProducts();
        } else if (tab == TAB_INVENTORY) {
            tabInventory.setBackgroundResource(R.drawable.tab_pill_active);
            tabInventory.setTextColor(activeColor);
            layoutInventoryView.setVisibility(View.VISIBLE);
            renderInventory();
        } else if (tab == TAB_SALES) {
            tabSales.setBackgroundResource(R.drawable.tab_pill_active);
            tabSales.setTextColor(activeColor);
            layoutSalesView.setVisibility(View.VISIBLE);
            renderSales();
        } else if (tab == TAB_GCASH) {
            tabGcash.setBackgroundResource(R.drawable.tab_pill_active);
            tabGcash.setTextColor(activeColor);
            layoutGcashView.setVisibility(View.VISIBLE);
            renderGcash();
        }
    }

    private void refreshAllData() {
        renderPosCategories();
        renderPosProducts();
        renderInventory();
        renderSales();
        renderGcash();
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
        float density = getResources().getDisplayMetrics().density;
        int heightInPx = (int) (48 * density);
        int marginEndInPx = (int) (8 * density);
        int padH = (int) (14 * density);

        for (String cat : categories) {
            Button btnCat = new Button(this);
            btnCat.setText(cat);
            btnCat.setTextSize(12);
            btnCat.setAllCaps(false);
            btnCat.setPadding(padH, 0, padH, 0);
            btnCat.setMinimumHeight(heightInPx);
            btnCat.setMinimumWidth(heightInPx);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    heightInPx
            );
            lp.setMargins(0, 0, marginEndInPx, 0);
            btnCat.setLayoutParams(lp);

            if (cat.equalsIgnoreCase(selectedPosCategory)) {
                btnCat.setBackgroundResource(R.drawable.tab_pill_active);
                btnCat.setTextColor(ContextCompat.getColor(this, R.color.colorOnPrimary));
                btnCat.setTypeface(null, Typeface.BOLD);
            } else {
                btnCat.setBackgroundResource(R.drawable.button_chip);
                btnCat.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
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
            emptyTxt.setTextColor(ContextCompat.getColor(this, R.color.colorTextMuted));
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

            int inCart = getCartQuantity(p.getId(), p.getName());
            int remainingStock = Math.max(0, p.getStock() - inCart);

            if (p.isOutOfStock()) {
                textStock.setText("Out of Stock");
                textStock.setBackgroundResource(R.drawable.badge_red);
                textStock.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeRedText));
                itemView.setAlpha(0.6f);
            } else if (remainingStock == 0) {
                textStock.setText("All in Cart (" + p.getStock() + ")");
                textStock.setBackgroundResource(R.drawable.badge_orange);
                textStock.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeOrangeText));
                itemView.setAlpha(0.75f);
            } else if (inCart > 0) {
                textStock.setText("Stock: " + remainingStock + " left (" + inCart + " in cart)");
                textStock.setBackgroundResource(R.drawable.badge_green);
                textStock.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeGreenText));
                itemView.setAlpha(1.0f);
            } else if (p.isLowStock()) {
                textStock.setText("Stock: " + p.getStock() + " (Low)");
                textStock.setBackgroundResource(R.drawable.badge_orange);
                textStock.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeOrangeText));
                itemView.setAlpha(1.0f);
            } else {
                textStock.setText("Stock: " + p.getStock());
                textStock.setBackgroundResource(R.drawable.badge_green);
                textStock.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeGreenText));
                itemView.setAlpha(1.0f);
            }

            View.OnClickListener addListener = v -> {
                int currentDbStock = dbHelper.getProductStock(p.getId(), p.getName());
                if (currentDbStock <= 0) {
                    Toast.makeText(this, p.getName() + " is OUT OF STOCK!", Toast.LENGTH_SHORT).show();
                    return;
                }
                int currentInCart = getCartQuantity(p.getId(), p.getName());
                if (currentInCart >= currentDbStock) {
                    Toast.makeText(this, "Cannot add more! Only " + currentDbStock + " " + p.getName() + " in stock (all already in cart).", Toast.LENGTH_SHORT).show();
                    return;
                }
                addItemToCart(p.getId(), p.getName(), p.getPrice());
            };

            itemView.setOnClickListener(addListener);
            btnAdd.setOnClickListener(addListener);

            layoutPosProductsContainer.addView(itemView);
        }
    }

    private int getCartQuantity(long productId, String name) {
        for (CartItem item : cartItems) {
            if ((productId > 0 && item.getProductId() == productId) ||
                (productId <= 0 && item.getName().equalsIgnoreCase(name))) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    private boolean addItemToCart(long productId, String name, double price) {
        int stock = dbHelper.getProductStock(productId, name);
        for (CartItem item : cartItems) {
            if ((productId > 0 && item.getProductId() == productId) ||
                (productId <= 0 && item.getName().equalsIgnoreCase(name) && Math.abs(item.getUnitPrice() - price) < 0.001)) {
                if (stock >= 0 && item.getQuantity() >= stock) {
                    Toast.makeText(this, "Cannot add more! Only " + stock + " " + item.getName() + " in stock.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                item.increment();
                updateCartDisplay();
                return true;
            }
        }
        if (stock == 0) {
            Toast.makeText(this, name + " is OUT OF STOCK!", Toast.LENGTH_SHORT).show();
            return false;
        }
        cartItems.add(new CartItem(productId, name, price, 1));
        updateCartDisplay();
        return true;
    }

    private void addItemToCart(String name, double price) {
        addItemToCart(-1, name, price);
    }

    private void clearCart() {
        cartItems.clear();
        updateCartDisplay();
    }

    private void completeSaleAndReset() {
        completeSaleAndReset(true);
    }

    private void completeSaleAndReset(boolean promptToPrint) {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is already empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verify inventory stock for all cart items before proceeding
        for (CartItem item : cartItems) {
            int currentStock = dbHelper.getProductStock(item.getProductId(), item.getName());
            if (currentStock >= 0 && item.getQuantity() > currentStock) {
                new AlertDialog.Builder(this)
                    .setTitle("⚠️ Stock Insufficient")
                    .setMessage("Cannot complete sale!\n\nItem \"" + item.getName() + "\" has " + item.getQuantity() + " ordered in cart, but only " + currentStock + " available in stock.\n\nPlease adjust the quantity to " + currentStock + " or less.")
                    .setPositiveButton("Adjust to " + currentStock, (d, w) -> {
                        if (currentStock <= 0) {
                            cartItems.remove(item);
                        } else {
                            item.setQuantity(currentStock);
                        }
                        updateCartDisplay();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                return;
            }
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
            String s = editCashTendered.getText().toString().trim();
            if (!s.isEmpty()) {
                try {
                    cashTendered = Double.parseDouble(s);
                    if (cashTendered < total) {
                        new AlertDialog.Builder(this)
                            .setTitle("⚠️ Insufficient Cash Tendered")
                            .setMessage("Total sale amount is ₱" + String.format(Locale.US, "%,.2f", total) +
                                    ", but cash tendered entered is only ₱" + String.format(Locale.US, "%,.2f", cashTendered) +
                                    ".\n\nPlease enter the correct payment amount.")
                            .setPositiveButton("Fix Amount", (d, w) -> editCashTendered.requestFocus())
                            .show();
                        return;
                    }
                    change = Math.max(0.0, cashTendered - total);
                } catch (Exception e) {
                    Toast.makeText(this, "Please enter a valid numeric cash amount.", Toast.LENGTH_SHORT).show();
                    editCashTendered.requestFocus();
                    return;
                }
            }
        } else if (paymentMode.equals("CREDIT")) {
            cashTendered = 0.0;
            change = 0.0;
        }

        String customerNote = editCustomerNote.getText().toString().trim();

        if (paymentMode.equals("CREDIT") && customerNote.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("⚠️ Debtor Name Required")
                .setMessage("Please enter the customer name / contact in the note field for Store Credit (Utang) so this balance can be recorded and collected.")
                .setPositiveButton("Enter Name", (d, w) -> editCustomerNote.requestFocus())
                .show();
            return;
        }

        SimpleDateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date now = new Date();
        String dateTime = dtFormat.format(now);
        String dateOnly = dFormat.format(now);
        String receiptNo = receiptDesign.getEffectiveReceiptPrefix() + receiptCounter;

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
        long saleId = dbHelper.recordSale(sale, cartItems);
        sale.setId(saleId);
        sale.setItems(dbHelper.getItemsForSale(saleId));

        clearCart();
        editCashTendered.setText("");
        editCustomerNote.setText("");
        receiptCounter++;
        ((RadioButton) findViewById(R.id.radio_cash)).setChecked(true);

        // Refresh dynamic inventory, catalog, and sales history
        refreshAllData();

        Toast.makeText(this, "Sale recorded (" + receiptNo + ")! Inventory updated.", Toast.LENGTH_SHORT).show();

        if (receiptDesign.autoPrintOnCheckout && selectedBluetoothDevice != null) {
            Toast.makeText(this, "Auto-printing receipt (" + receiptNo + ")...", Toast.LENGTH_SHORT).show();
            printPastReceiptBluetooth(sale);
        } else if (promptToPrint && receiptDesign.promptToPrint) {
            new AlertDialog.Builder(this)
                .setTitle("Sale Recorded (" + receiptNo + ")")
                .setMessage("Total: ₱" + String.format(Locale.US, "%.2f", sale.getTotalAmount()) + "\nWould you like to print the receipt now?")
                .setPositiveButton("Print Bluetooth", (dialog, which) -> printPastReceiptBluetooth(sale))
                .setNeutralButton("Print USB", (dialog, which) -> printPastReceiptUsb(sale))
                .setNegativeButton("Done", null)
                .show();
        }
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

        // 1. Update Parked Cart Banner
        if (bannerParkedCart != null) {
            if (parkedCartItems.isEmpty()) {
                bannerParkedCart.setVisibility(View.GONE);
            } else {
                bannerParkedCart.setVisibility(View.VISIBLE);
                double parkedTotal = 0.0;
                int parkedUnits = 0;
                for (CartItem pItem : parkedCartItems) {
                    parkedTotal += pItem.getTotalPrice();
                    parkedUnits += pItem.getQuantity();
                }
                if (textParkedCartSummary != null) {
                    textParkedCartSummary.setText("⏸️ " + parkedCartItems.size() + " items (" + parkedUnits + " units • ₱" + String.format(Locale.US, "%.2f", parkedTotal) + ") on hold");
                }
            }
        }

        // 2. Render Cart Items
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
                textName.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
                textName.setTypeface(null, Typeface.BOLD);

                TextView textPrice = new TextView(this);
                textPrice.setText(String.format(Locale.US, "₱%.2f each", item.getUnitPrice()));
                textPrice.setTextSize(11);
                textPrice.setTextColor(ContextCompat.getColor(this, R.color.colorTextMuted));

                infoCol.addView(textName);
                infoCol.addView(textPrice);

                float density = getResources().getDisplayMetrics().density;
                int btnSize = (int) (48 * density);
                int qtyWidth = (int) (36 * density);
                int subtotalWidth = (int) (76 * density);

                Button btnMinus = new Button(this);
                btnMinus.setText("-");
                btnMinus.setTextSize(16);
                btnMinus.setTextColor(ContextCompat.getColor(this, R.color.colorDanger));
                btnMinus.setBackgroundResource(R.drawable.button_tonal);
                btnMinus.setMinimumHeight(btnSize);
                btnMinus.setMinimumWidth(btnSize);
                btnMinus.setPadding(0, 0, 0, 0);
                btnMinus.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
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
                textQty.setLayoutParams(new LinearLayout.LayoutParams(qtyWidth, LinearLayout.LayoutParams.WRAP_CONTENT));
                textQty.setOnClickListener(v -> promptSetQuantity(item));

                Button btnPlus = new Button(this);
                btnPlus.setText("+");
                btnPlus.setTextSize(16);
                btnPlus.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
                btnPlus.setBackgroundResource(R.drawable.button_tonal);
                btnPlus.setMinimumHeight(btnSize);
                btnPlus.setMinimumWidth(btnSize);
                btnPlus.setPadding(0, 0, 0, 0);
                btnPlus.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
                btnPlus.setOnClickListener(v -> {
                    int stock = dbHelper.getProductStock(item.getProductId(), item.getName());
                    if (stock >= 0 && item.getQuantity() >= stock) {
                        Toast.makeText(this, "Cannot add more! Only " + stock + " " + item.getName() + " in stock.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    item.increment();
                    updateCartDisplay();
                });

                TextView textSubtotal = new TextView(this);
                textSubtotal.setText(String.format(Locale.US, "₱%.2f", item.getTotalPrice()));
                textSubtotal.setTextSize(13);
                textSubtotal.setTypeface(null, Typeface.BOLD);
                textSubtotal.setGravity(Gravity.END);
                textSubtotal.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
                textSubtotal.setLayoutParams(new LinearLayout.LayoutParams(subtotalWidth, LinearLayout.LayoutParams.WRAP_CONTENT));

                row.addView(infoCol);
                row.addView(btnMinus);
                row.addView(textQty);
                row.addView(btnPlus);
                row.addView(textSubtotal);

                layoutCartItems.addView(row);
            }
        }

        // 3. Update Cart Total
        double total = calculateTotal();
        textCartTotal.setText(String.format(Locale.US, "₱ %.2f", total));
        recalculateChange();
        renderPosProducts();
    }

    private void promptSetQuantity(CartItem item) {
        int stock = dbHelper.getProductStock(item.getProductId(), item.getName());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Quantity: " + item.getName());
        if (stock >= 0) {
            builder.setMessage("Available Inventory Stock: " + stock);
        }

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(item.getQuantity()));
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        builder.setPositiveButton("Set", (d, w) -> {
            String valStr = input.getText().toString().trim();
            if (!valStr.isEmpty()) {
                try {
                    int val = Integer.parseInt(valStr);
                    if (val <= 0) {
                        cartItems.remove(item);
                        updateCartDisplay();
                        return;
                    }
                    if (stock >= 0 && val > stock) {
                        Toast.makeText(this, "Cannot order " + val + "! Only " + stock + " in stock. Quantity capped to " + stock + ".", Toast.LENGTH_LONG).show();
                        item.setQuantity(stock);
                        updateCartDisplay();
                        return;
                    }
                    item.setQuantity(val);
                    updateCartDisplay();
                } catch (NumberFormatException ignored) {}
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void holdCart() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty. Nothing to put on hold.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!parkedCartItems.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("Replace Parked Order?")
                .setMessage("An existing order is already on hold. Holding this cart will overwrite it. Proceed?")
                .setPositiveButton("Overwrite & Hold", (d, w) -> {
                    parkedCartItems.clear();
                    parkedCartItems.addAll(cartItems);
                    cartItems.clear();
                    updateCartDisplay();
                    Toast.makeText(this, "Order placed on hold! Ready for next customer.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
            return;
        }

        parkedCartItems.addAll(cartItems);
        cartItems.clear();
        updateCartDisplay();
        Toast.makeText(this, "Order placed on hold! Ready for next customer.", Toast.LENGTH_SHORT).show();
    }

    private void resumeParkedCart() {
        if (parkedCartItems.isEmpty()) {
            Toast.makeText(this, "No parked order on hold.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cartItems.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("Resume Parked Order")
                .setMessage("You have active items in your current cart. Would you like to merge with the parked order or replace current cart?")
                .setPositiveButton("Merge Both", (d, w) -> {
                    for (CartItem pItem : parkedCartItems) {
                        boolean found = false;
                        int stock = dbHelper.getProductStock(pItem.getProductId(), pItem.getName());
                        for (CartItem cItem : cartItems) {
                            if (cItem.getProductId() > 0 && cItem.getProductId() == pItem.getProductId()) {
                                int newQty = cItem.getQuantity() + pItem.getQuantity();
                                if (stock >= 0 && newQty > stock) newQty = stock;
                                cItem.setQuantity(newQty);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            if (stock >= 0 && pItem.getQuantity() > stock) {
                                pItem.setQuantity(stock);
                            }
                            if (pItem.getQuantity() > 0) {
                                cartItems.add(pItem);
                            }
                        }
                    }
                    parkedCartItems.clear();
                    updateCartDisplay();
                    Toast.makeText(this, "Orders merged successfully!", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Replace Current Cart", (d, w) -> {
                    cartItems.clear();
                    for (CartItem pItem : parkedCartItems) {
                        int stock = dbHelper.getProductStock(pItem.getProductId(), pItem.getName());
                        if (stock >= 0 && pItem.getQuantity() > stock) {
                            pItem.setQuantity(stock);
                        }
                        if (pItem.getQuantity() > 0) {
                            cartItems.add(pItem);
                        }
                    }
                    parkedCartItems.clear();
                    updateCartDisplay();
                    Toast.makeText(this, "Parked order restored!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
            return;
        }

        for (CartItem pItem : parkedCartItems) {
            int stock = dbHelper.getProductStock(pItem.getProductId(), pItem.getName());
            if (stock >= 0 && pItem.getQuantity() > stock) {
                pItem.setQuantity(stock);
            }
            if (pItem.getQuantity() > 0) {
                cartItems.add(pItem);
            }
        }
        parkedCartItems.clear();
        updateCartDisplay();
        Toast.makeText(this, "Parked order restored to cart!", Toast.LENGTH_SHORT).show();
    }


    private void setupPaymentWatcher() {
        if (radioGroupPayment != null) {
            radioGroupPayment.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.radio_cash) {
                    if (layoutCashSection != null) layoutCashSection.setVisibility(View.VISIBLE);
                } else {
                    if (layoutCashSection != null) layoutCashSection.setVisibility(View.GONE);
                }
                recalculateChange();
            });
        }
        if (editCashTendered != null) {
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
    }

    private void setupQuickCashChips() {
        findViewById(R.id.chip_cash_exact).setOnClickListener(v -> {
            double total = calculateTotal();
            editCashTendered.setText(String.format(Locale.US, "%.2f", total));
        });
        findViewById(R.id.chip_cash_50).setOnClickListener(v -> editCashTendered.setText("50"));
        findViewById(R.id.chip_cash_100).setOnClickListener(v -> editCashTendered.setText("100"));
        View chip200 = findViewById(R.id.chip_cash_200);
        if (chip200 != null) {
            chip200.setOnClickListener(v -> editCashTendered.setText("200"));
        }
        findViewById(R.id.chip_cash_500).setOnClickListener(v -> editCashTendered.setText("500"));
        findViewById(R.id.chip_cash_1000).setOnClickListener(v -> editCashTendered.setText("1000"));
    }

    private void recalculateChange() {
        double total = calculateTotal();
        int paymentModeId = radioGroupPayment.getCheckedRadioButtonId();

        if (paymentModeId != R.id.radio_cash) {
            textChange.setText("Change: ₱ 0.00");
            textChange.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            return;
        }

        String cashStr = editCashTendered.getText().toString().trim();
        if (cashStr.isEmpty()) {
            textChange.setText("Change: ₱ 0.00");
            textChange.setTextColor(ContextCompat.getColor(this, R.color.colorTextMuted));
            return;
        }

        try {
            double cash = Double.parseDouble(cashStr);
            double change = cash - total;
            if (change < 0) {
                textChange.setText(String.format(Locale.US, "Short: ₱ %.2f", Math.abs(change)));
                textChange.setTextColor(ContextCompat.getColor(this, R.color.colorDanger));
            } else {
                textChange.setText(String.format(Locale.US, "Change: ₱ %.2f", change));
                textChange.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            }
        } catch (NumberFormatException e) {
            textChange.setText("Change: ₱ 0.00");
        }
    }

    /*==============================================================================================
    ==================================INVENTORY MANAGEMENT SECTION==================================
    ==============================================================================================*/

    private void setupInventoryEvents() {
        findViewById(R.id.btn_inventory_add).setOnClickListener(v -> requireOwnerPin("Add New Product", () -> openAddEditProductDialog(null)));
        View btnScanAdd = findViewById(R.id.btn_inventory_scan_add);
        if (btnScanAdd != null) {
            btnScanAdd.setOnClickListener(v -> requireOwnerPin("Scan Product with AI", () -> promptProductPhotoCapture(null, null, null)));
        }
        View btnInvExport = findViewById(R.id.btn_inventory_export);
        if (btnInvExport != null) {
            btnInvExport.setOnClickListener(v -> exportInventoryToCsv());
        }

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

    private void setupInventoryFilterChips() {
        if (chipInvFilterAll != null) {
            chipInvFilterAll.setOnClickListener(v -> setInventoryFilter("ALL"));
        }
        if (chipInvFilterLow != null) {
            chipInvFilterLow.setOnClickListener(v -> setInventoryFilter("LOW"));
        }
        if (chipInvFilterOut != null) {
            chipInvFilterOut.setOnClickListener(v -> setInventoryFilter("OUT"));
        }
    }

    private void setInventoryFilter(String filter) {
        selectedInventoryStockFilter = filter;
        updateInventoryFilterUI();
        renderInventory();
    }

    private void updateInventoryFilterUI() {
        int activeBg = R.drawable.tab_pill_active;
        int inactiveBg = R.drawable.button_chip;
        int activeTxt = ContextCompat.getColor(this, R.color.colorOnPrimary);
        int inactiveTxt = ContextCompat.getColor(this, R.color.colorTextDark);

        if (chipInvFilterAll != null) {
            boolean sel = "ALL".equalsIgnoreCase(selectedInventoryStockFilter);
            chipInvFilterAll.setBackgroundResource(sel ? activeBg : inactiveBg);
            chipInvFilterAll.setTextColor(sel ? activeTxt : inactiveTxt);
        }
        if (chipInvFilterLow != null) {
            boolean sel = "LOW".equalsIgnoreCase(selectedInventoryStockFilter);
            chipInvFilterLow.setBackgroundResource(sel ? activeBg : inactiveBg);
            chipInvFilterLow.setTextColor(sel ? activeTxt : inactiveTxt);
        }
        if (chipInvFilterOut != null) {
            boolean sel = "OUT".equalsIgnoreCase(selectedInventoryStockFilter);
            chipInvFilterOut.setBackgroundResource(sel ? activeBg : inactiveBg);
            chipInvFilterOut.setTextColor(sel ? activeTxt : inactiveTxt);
        }
    }

    private void renderInventory() {
        layoutInventoryItemsContainer.removeAllViews();
        updateInventoryFilterUI();

        String query = editInventorySearch.getText().toString().trim();
        List<Product> products = dbHelper.getAllProducts(query, null, selectedInventoryStockFilter);

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
            String emptyMsg = "No products found.";
            if ("LOW".equalsIgnoreCase(selectedInventoryStockFilter)) {
                emptyMsg = "No products are low in stock right now! Great job.";
            } else if ("OUT".equalsIgnoreCase(selectedInventoryStockFilter)) {
                emptyMsg = "No out-of-stock products right now!";
            } else {
                emptyMsg = "No products found in inventory. Tap '+ Add Item' above.";
            }
            emptyTxt.setText(emptyMsg);
            emptyTxt.setTextColor(ContextCompat.getColor(this, R.color.colorTextMuted));
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
            Button btnRestockMore = itemView.findViewById(R.id.btn_inv_restock_more);

            textName.setText(p.getName());
            textCat.setText(p.getCategory());
            textPrice.setText(String.format(Locale.US, "Selling: ₱%.2f", p.getPrice()));

            double profit = p.getPrice() - p.getCost();
            textCost.setText(String.format(Locale.US, "Cost: ₱%.2f • Margin: +₱%.2f", p.getCost(), profit));

            if (p.isOutOfStock()) {
                textStockBadge.setText("Out of Stock (0)");
                textStockBadge.setBackgroundResource(R.drawable.badge_red);
                textStockBadge.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeRedText));
            } else if (p.isLowStock()) {
                textStockBadge.setText("Low Stock: " + p.getStock());
                textStockBadge.setBackgroundResource(R.drawable.badge_orange);
                textStockBadge.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeOrangeText));
            } else {
                textStockBadge.setText("In Stock: " + p.getStock());
                textStockBadge.setBackgroundResource(R.drawable.badge_green);
                textStockBadge.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeGreenText));
            }

            btnRestock5.setOnClickListener(v -> adjustProductStockQuick(p, 5));
            btnRestock10.setOnClickListener(v -> adjustProductStockQuick(p, 10));
            if (btnRestockMore != null) {
                btnRestockMore.setOnClickListener(v -> openQuickRestockDialog(p));
            }

            btnEdit.setOnClickListener(v -> requireOwnerPin("Edit Product & Price", () -> openAddEditProductDialog(p)));

            layoutInventoryItemsContainer.addView(itemView);
        }
    }

    private void openQuickRestockDialog(Product p) {
        String[] options = {
            "➕ Add 1 Piece (+1)",
            "📦 Add Half Dozen (+6)",
            "📦 Add One Dozen (+12)",
            "🏢 Add Full Case (+24)",
            "✏️ Add Custom Amount..."
        };

        new AlertDialog.Builder(this)
            .setTitle("Restock " + p.getName() + " (Current: " + p.getStock() + ")")
            .setItems(options, (dialog, which) -> {
                if (which == 0) adjustProductStockQuick(p, 1);
                else if (which == 1) adjustProductStockQuick(p, 6);
                else if (which == 2) adjustProductStockQuick(p, 12);
                else if (which == 3) adjustProductStockQuick(p, 24);
                else if (which == 4) promptCustomRestockInput(p);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void adjustProductStockQuick(Product p, int amount) {
        dbHelper.adjustStock(p.getId(), amount);
        Toast.makeText(this, "Added +" + amount + " to " + p.getName() + "! Stock: " + (p.getStock() + amount), Toast.LENGTH_SHORT).show();
        renderInventory();
        renderPosProducts();
    }

    private void promptCustomRestockInput(Product p) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Restock " + p.getName());

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter quantity to add (e.g. 50)");
        builder.setView(input);

        builder.setPositiveButton("Add Stock", (d, w) -> {
            String valStr = input.getText().toString().trim();
            if (!valStr.isEmpty()) {
                try {
                    int qty = Integer.parseInt(valStr);
                    if (qty > 0) {
                        adjustProductStockQuick(p, qty);
                        return;
                    }
                } catch (Exception ignored) {}
            }
            Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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
                requireOwnerPin("Delete Product", () -> {
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
                editName.requestFocus();
                return;
            }

            if (priceStr.isEmpty()) {
                Toast.makeText(this, "Please enter selling price", Toast.LENGTH_SHORT).show();
                editPrice.requestFocus();
                return;
            }

            double price;
            double cost = 0.0;
            int stock = 0;
            int minThreshold = 5;

            try {
                price = Double.parseDouble(priceStr);
                if (!DataValidator.isValidPrice(price)) {
                    Toast.makeText(this, "Selling price must be greater than zero", Toast.LENGTH_SHORT).show();
                    editPrice.requestFocus();
                    return;
                }
                if (!costStr.isEmpty()) {
                    cost = Double.parseDouble(costStr);
                    if (!DataValidator.isValidCost(cost)) {
                        Toast.makeText(this, "Supplier cost cannot be negative", Toast.LENGTH_SHORT).show();
                        editCost.requestFocus();
                        return;
                    }
                }
                if (!stockStr.isEmpty()) {
                    stock = Integer.parseInt(stockStr);
                    if (!DataValidator.isValidStock(stock)) {
                        Toast.makeText(this, "Stock quantity cannot be negative", Toast.LENGTH_SHORT).show();
                        editStock.requestFocus();
                        return;
                    }
                }
                if (!minStr.isEmpty()) {
                    minThreshold = Integer.parseInt(minStr);
                    if (minThreshold < 0) {
                        Toast.makeText(this, "Low stock threshold cannot be negative", Toast.LENGTH_SHORT).show();
                        editMinThreshold.requestFocus();
                        return;
                    }
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please check numeric inputs for price/cost/stock", Toast.LENGTH_SHORT).show();
                return;
            }

            // Margin check: Selling below cost?
            if (cost > 0 && price < cost) {
                double loss = cost - price;
                final double finalPrice = price;
                final double finalCost = cost;
                final int finalStock = stock;
                final int finalMinThreshold = minThreshold;
                new AlertDialog.Builder(this)
                    .setTitle("⚠️ Selling Below Cost Warning")
                    .setMessage("Selling price (₱" + String.format(Locale.US, "%.2f", finalPrice) +
                            ") is lower than supplier cost (₱" + String.format(Locale.US, "%.2f", finalCost) +
                            ").\n\nYou will lose ₱" + String.format(Locale.US, "%.2f", loss) + " on each sale.\n\nSave anyway?")
                    .setPositiveButton("Save Anyway", (d, w) -> {
                        commitSaveProduct(dialog, isEdit, existingProduct, name, cat, finalPrice, finalCost, finalStock, finalMinThreshold);
                    })
                    .setNegativeButton("Adjust Price", (d, w) -> editPrice.requestFocus())
                    .show();
                return;
            }

            commitSaveProduct(dialog, isEdit, existingProduct, name, cat, price, cost, stock, minThreshold);
        });

        dialog.show();

        currentProductDialog = dialog;
        currentProductDialogView = dialogView;
        currentEditingProduct = existingProduct;

        dialog.setOnDismissListener(d -> {
            if (currentProductDialog == dialog) {
                currentProductDialog = null;
                currentProductDialogView = null;
                currentEditingProduct = null;
            }
        });

        View btnAiScan = dialogView.findViewById(R.id.btn_ai_scan_product);
        if (btnAiScan != null) {
            btnAiScan.setOnClickListener(v -> promptProductPhotoCapture(existingProduct, dialogView, dialog));
        }
        View btnAiRetake = dialogView.findViewById(R.id.btn_ai_retake);
        if (btnAiRetake != null) {
            btnAiRetake.setOnClickListener(v -> promptProductPhotoCapture(existingProduct, dialogView, dialog));
        }
    }

    private void commitSaveProduct(AlertDialog dialog, boolean isEdit, Product existingProduct, String name, String cat, double price, double cost, int stock, int minThreshold) {
        if (isEdit && existingProduct != null) {
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

        if (dialog != null) dialog.dismiss();
        refreshAllData();
    }

    /*==============================================================================================
    ==================================AI PRODUCT RECOGNITION (ML KIT)==============================
    ==============================================================================================*/

    private void promptProductPhotoCapture(Product product, View dialogView, AlertDialog dialog) {
        currentEditingProduct = product;
        currentProductDialogView = dialogView;
        currentProductDialog = dialog;

        String[] options = {"📸 Take Photo with Camera", "🖼️ Choose from Gallery", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("AI Product Photo Recognition")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndLaunch();
                    } else if (which == 1) {
                        launchGalleryPick();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQUEST);
        } else {
            launchCameraCapture();
        }
    }

    private void launchCameraCapture() {
        try {
            File cacheDir = new File(getCacheDir(), "camera");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File photoFile = new File(cacheDir, "prod_" + System.currentTimeMillis() + ".jpg");
            currentProductPhotoPath = photoFile.getAbsolutePath();
            currentProductPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentProductPhotoUri);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_CAMERA_PRODUCT);
            } else {
                Toast.makeText(this, "No camera application found on device.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error setting up camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGalleryPick() {
        try {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickIntent.setType("image/*");
            startActivityForResult(pickIntent, REQUEST_GALLERY_PRODUCT);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap loadScaledBitmap(String imagePath, int maxDim) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            int scale = 1;
            while (options.outWidth / scale > maxDim || options.outHeight / scale > maxDim) {
                scale *= 2;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = scale;
            Bitmap raw = BitmapFactory.decodeFile(imagePath, options);
            if (raw == null) return null;

            // Check EXIF rotation so photo orientation is right-side up
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotate = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotate = 90;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotate = 180;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotate = 270;

            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotate);
                return Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), matrix, true);
            }
            return raw;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap loadScaledBitmapFromUri(Uri uri, int maxDim) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            if (is != null) is.close();

            int scale = 1;
            while (options.outWidth / scale > maxDim || options.outHeight / scale > maxDim) {
                scale *= 2;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = scale;
            is = getContentResolver().openInputStream(uri);
            Bitmap raw = BitmapFactory.decodeStream(is, null, options);
            if (is != null) is.close();
            return raw;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Executes Google ML Kit on-device Vision: OCR Text Recognition + Visual Image Labeling.
     */
    private void runOnDeviceProductRecognition(Bitmap bitmap) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("🤖 Analyzing product photo with Google ML Kit...");
        progress.setCancelable(false);
        progress.show();

        try {
            InputImage inputImage = InputImage.fromBitmap(bitmap, 0);

            TextRecognizer textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            ImageLabeler imageLabeler = ImageLabeling.getClient(
                    new ImageLabelerOptions.Builder().setConfidenceThreshold(0.40f).build()
            );

            Task<Text> textTask = textRecognizer.process(inputImage);
            Task<List<ImageLabel>> labelTask = imageLabeler.process(inputImage);

            Tasks.whenAllComplete(textTask, labelTask).addOnCompleteListener(task -> {
                progress.dismiss();
                Text visionText = textTask.isSuccessful() ? textTask.getResult() : null;
                List<ImageLabel> labels = labelTask.isSuccessful() ? labelTask.getResult() : null;

                analyzeAndDisplayRecognitionResult(bitmap, visionText, labels);
            });
        } catch (Exception e) {
            progress.dismiss();
            e.printStackTrace();
            Toast.makeText(this, "ML Kit analysis error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void analyzeAndDisplayRecognitionResult(Bitmap bitmap, Text visionText, List<ImageLabel> labels) {
        List<String> snippets = extractCandidateProductNames(visionText, labels);
        String suggestedName = pickBestProductName(snippets);
        String allRecognizedText = (visionText != null) ? visionText.getText() : "";
        String suggestedCategory = inferProductCategory(allRecognizedText, labels);

        // If product dialog is already visible, update it in place
        if (currentProductDialog != null && currentProductDialog.isShowing() && currentProductDialogView != null) {
            populateDialogWithAIRecognition(currentProductDialogView, bitmap, suggestedName, suggestedCategory, snippets);
        } else {
            // Open product dialog pre-filled with AI recognized details
            openAddEditProductDialogWithAI(currentEditingProduct, suggestedName, suggestedCategory, bitmap, snippets);
        }
    }

    private void openAddEditProductDialogWithAI(Product existingProduct, String suggestedName, String suggestedCategory, Bitmap bitmap, List<String> snippets) {
        openAddEditProductDialog(existingProduct);
        if (currentProductDialogView != null) {
            populateDialogWithAIRecognition(currentProductDialogView, bitmap, suggestedName, suggestedCategory, snippets);
        }
    }

    private void populateDialogWithAIRecognition(View dialogView, Bitmap bitmap, String suggestedName, String suggestedCategory, List<String> snippets) {
        EditText editName = dialogView.findViewById(R.id.edit_product_name);
        EditText editCat = dialogView.findViewById(R.id.edit_product_category);
        EditText editPrice = dialogView.findViewById(R.id.edit_product_price);
        LinearLayout layoutResult = dialogView.findViewById(R.id.layout_ai_recognition_result);
        ImageView imgPreview = dialogView.findViewById(R.id.image_ai_preview);
        TextView textSummary = dialogView.findViewById(R.id.text_ai_detected_summary);
        LinearLayout layoutChips = dialogView.findViewById(R.id.layout_ai_text_chips);

        if (editName != null && !suggestedName.isEmpty()) {
            editName.setText(suggestedName);
        }
        if (editCat != null && !suggestedCategory.isEmpty()) {
            editCat.setText(suggestedCategory);
        }
        if (layoutResult != null) {
            layoutResult.setVisibility(View.VISIBLE);
        }
        if (imgPreview != null && bitmap != null) {
            imgPreview.setImageBitmap(bitmap);
        }
        if (textSummary != null) {
            textSummary.setText("Detected: " + suggestedName + " • " + suggestedCategory);
        }

        if (layoutChips != null) {
            layoutChips.removeAllViews();
            float density = getResources().getDisplayMetrics().density;
            int chipTarget48 = (int) (48 * density);
            for (String snippet : snippets) {
                Button chip = new Button(this);
                chip.setText(snippet);
                chip.setBackgroundResource(R.drawable.button_tonal);
                chip.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
                chip.setTextSize(12f);
                chip.setAllCaps(false);
                chip.setMinimumHeight(chipTarget48);
                chip.setMinimumWidth(chipTarget48);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        chipTarget48
                );
                params.setMargins(0, 0, (int) (8 * density), 0);
                chip.setLayoutParams(params);
                chip.setPadding((int) (14 * density), 0, (int) (14 * density), 0);
                chip.setOnClickListener(v -> {
                    if (editName != null) {
                        editName.setText(snippet);
                        Toast.makeText(this, "Set product name: " + snippet, Toast.LENGTH_SHORT).show();
                    }
                });
                layoutChips.addView(chip);
            }
        }

        if (editPrice != null) {
            editPrice.requestFocus();
        }
        Toast.makeText(this, "✨ Product recognized! Please check details and enter price.", Toast.LENGTH_LONG).show();
    }

    private String inferProductCategory(String text, List<ImageLabel> labels) {
        String lowerText = (text != null ? text.toLowerCase(Locale.US) : "");
        StringBuilder labelStr = new StringBuilder();
        if (labels != null) {
            for (ImageLabel lbl : labels) {
                labelStr.append(lbl.getText().toLowerCase(Locale.US)).append(" ");
            }
        }
        String combined = lowerText + " " + labelStr.toString();

        if (combined.contains("coke") || combined.contains("cola") || combined.contains("pepsi") ||
            combined.contains("sprite") || combined.contains("drink") || combined.contains("beverage") ||
            combined.contains("water") || combined.contains("juice") || combined.contains("tea") ||
            combined.contains("coffee") || combined.contains("beer") || combined.contains("liquor") ||
            combined.contains("milk") || combined.contains("soft drink") || combined.contains("bottle")) {
            return "Beverages";
        }
        if (combined.contains("chip") || combined.contains("biscuit") || combined.contains("cookie") ||
            combined.contains("snack") || combined.contains("cracker") || combined.contains("candy") ||
            combined.contains("chocolate") || combined.contains("wafer") || combined.contains("peanut") ||
            combined.contains("corn") || combined.contains("popcorn") || combined.contains("piattos")) {
            return "Snacks";
        }
        if (combined.contains("can") || combined.contains("tin") || combined.contains("sardine") ||
            combined.contains("tuna") || combined.contains("corned beef") || combined.contains("meatloaf") ||
            combined.contains("sausage") || combined.contains("canned")) {
            return "Canned Goods";
        }
        if (combined.contains("noodle") || combined.contains("pancit") || combined.contains("canton") ||
            combined.contains("ramen") || combined.contains("pasta") || combined.contains("spaghetti") ||
            combined.contains("macaroni") || combined.contains("rice") || combined.contains("grain")) {
            return "Noodles & Grains";
        }
        if (combined.contains("sauce") || combined.contains("soy") || combined.contains("vinegar") ||
            combined.contains("oil") || combined.contains("seasoning") || combined.contains("magic sarap") ||
            combined.contains("salt") || combined.contains("sugar") || combined.contains("ketchup")) {
            return "Condiments";
        }
        if (combined.contains("soap") || combined.contains("shampoo") || combined.contains("toothpaste") ||
            combined.contains("conditioner") || combined.contains("lotion") || combined.contains("deodorant") ||
            combined.contains("bath") || combined.contains("personal care") || combined.contains("cosmetic") ||
            combined.contains("safeguard")) {
            return "Personal Care";
        }
        if (combined.contains("detergent") || combined.contains("bleach") || combined.contains("cleaner") ||
            combined.contains("dishwashing") || combined.contains("powder") || combined.contains("fabric") ||
            combined.contains("household") || combined.contains("surf") || combined.contains("tide") || combined.contains("ariel")) {
            return "Household";
        }

        // Visual labels fallback
        if (labels != null && !labels.isEmpty()) {
            for (ImageLabel lbl : labels) {
                String top = lbl.getText();
                if (top.equalsIgnoreCase("Food") || top.equalsIgnoreCase("Dish") || top.equalsIgnoreCase("Snack")) return "Snacks";
                if (top.equalsIgnoreCase("Beverage") || top.equalsIgnoreCase("Drink")) return "Beverages";
            }
        }

        return "General";
    }

    private List<String> extractCandidateProductNames(Text visionText, List<ImageLabel> labels) {
        List<String> candidates = new ArrayList<>();
        if (visionText != null) {
            for (Text.TextBlock block : visionText.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    String raw = line.getText().trim();
                    String clean = raw.replaceAll("[^a-zA-Z0-9\\s\\-\\.]", " ").replaceAll("\\s+", " ").trim();
                    if (clean.length() >= 3 && clean.length() <= 35) {
                        String lower = clean.toLowerCase(Locale.US);
                        // Filter out barcodes, dates, nutrition facts, instructions
                        if (lower.matches(".*\\d{6,}.*")) continue;
                        if (lower.contains("exp") || lower.contains("mfg") || lower.contains("batch") ||
                            lower.contains("best before") || lower.contains("nutrition") || lower.contains("calories") ||
                            lower.contains("ingredients") || lower.contains("serving") || lower.contains("net wt") ||
                            lower.contains("manufactured") || lower.contains("tel") || lower.contains("philippines")) {
                            continue;
                        }
                        if (!candidates.contains(clean)) {
                            candidates.add(clean);
                        }
                    }
                }
            }
        }

        // Add top visual labels as secondary options
        if (labels != null) {
            for (ImageLabel lbl : labels) {
                String lblText = lbl.getText();
                if (!lblText.equalsIgnoreCase("Font") &&
                    !lblText.equalsIgnoreCase("Rectangle") &&
                    !lblText.equalsIgnoreCase("Circle") &&
                    !lblText.equalsIgnoreCase("Parallel") &&
                    !lblText.equalsIgnoreCase("Material property") &&
                    !candidates.contains(lblText)) {
                    candidates.add(lblText);
                }
                if (candidates.size() >= 8) break;
            }
        }

        return candidates;
    }

    private String pickBestProductName(List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "New Product";
        }
        for (String c : candidates) {
            if (c.length() >= 4 && c.length() <= 30 && c.matches(".*[a-zA-Z]{3,}.*")) {
                return toTitleCase(c);
            }
        }
        return toTitleCase(candidates.get(0));
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c) || c == '-' || c == '.') {
                nextUpper = true;
                sb.append(c);
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString().trim();
    }

    /*==============================================================================================
    ================================SALES & BUSINESS REPORTS SECTION================================
    ==============================================================================================*/

    private void setupSalesEvents() {
        if (chipSalesToday != null) chipSalesToday.setOnClickListener(v -> selectSalesPeriod(SalesPeriod.TODAY));
        if (chipSalesYesterday != null) chipSalesYesterday.setOnClickListener(v -> selectSalesPeriod(SalesPeriod.YESTERDAY));
        if (chipSales7Days != null) chipSales7Days.setOnClickListener(v -> selectSalesPeriod(SalesPeriod.DAYS_7));
        if (chipSales30Days != null) chipSales30Days.setOnClickListener(v -> selectSalesPeriod(SalesPeriod.THIS_MONTH));
        if (chipSalesAllTime != null) chipSalesAllTime.setOnClickListener(v -> selectSalesPeriod(SalesPeriod.ALL_TIME));

        if (filterSalesAll != null) filterSalesAll.setOnClickListener(v -> selectSalesFilterMode("ALL"));
        if (filterSalesCash != null) filterSalesCash.setOnClickListener(v -> selectSalesFilterMode("CASH"));
        if (filterSalesGcash != null) filterSalesGcash.setOnClickListener(v -> selectSalesFilterMode("GCASH"));
        if (filterSalesUtang != null) filterSalesUtang.setOnClickListener(v -> selectSalesFilterMode("CREDIT_UNPAID"));

        if (btnPrintZReport != null) {
            btnPrintZReport.setOnClickListener(v -> onPrintZReportClicked());
        }
        if (btnSalesExport != null) {
            btnSalesExport.setOnClickListener(v -> exportSalesToCsv());
        }
        if (btnSalesManageHistory != null) {
            btnSalesManageHistory.setOnClickListener(v -> requireOwnerPin("Manage Sales History", () -> openManageSalesHistoryDialog()));
        }
        if (editSalesSearch != null) {
            editSalesSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    renderSales();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void selectSalesPeriod(SalesPeriod period) {
        selectedSalesPeriod = period;
        renderSales();
    }

    private void selectSalesFilterMode(String mode) {
        selectedSalesFilterMode = mode;
        renderSales();
    }

    private void updatePeriodChipsUI() {
        Button[] chips = {chipSalesToday, chipSalesYesterday, chipSales7Days, chipSales30Days, chipSalesAllTime};
        SalesPeriod[] periods = {SalesPeriod.TODAY, SalesPeriod.YESTERDAY, SalesPeriod.DAYS_7, SalesPeriod.THIS_MONTH, SalesPeriod.ALL_TIME};
        int activeColor = ContextCompat.getColor(this, R.color.colorOnPrimary);
        int inactiveColor = ContextCompat.getColor(this, R.color.colorSlate600);

        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null) continue;
            boolean isActive = (selectedSalesPeriod == periods[i]);
            chips[i].setBackgroundResource(isActive ? R.drawable.tab_pill_active : R.drawable.tab_pill_inactive);
            chips[i].setTextColor(isActive ? activeColor : inactiveColor);
        }
    }

    private void updatePaymentFilterUI() {
        Button[] filters = {filterSalesAll, filterSalesCash, filterSalesGcash, filterSalesUtang};
        String[] modes = {"ALL", "CASH", "GCASH", "CREDIT_UNPAID"};
        int activeColor = ContextCompat.getColor(this, R.color.colorOnPrimary);
        int inactiveColor = ContextCompat.getColor(this, R.color.colorSlate600);

        for (int i = 0; i < filters.length; i++) {
            if (filters[i] == null) continue;
            boolean isActive = selectedSalesFilterMode.equals(modes[i]);
            filters[i].setBackgroundResource(isActive ? R.drawable.tab_pill_active : R.drawable.tab_pill_inactive);
            filters[i].setTextColor(isActive ? activeColor : inactiveColor);
        }
    }

    private String[] getSelectedPeriodDateRange() {
        SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        String today = dFormat.format(cal.getTime());

        switch (selectedSalesPeriod) {
            case TODAY:
                return new String[]{today, today};
            case YESTERDAY:
                cal.add(Calendar.DAY_OF_YEAR, -1);
                String yest = dFormat.format(cal.getTime());
                return new String[]{yest, yest};
            case DAYS_7:
                cal.add(Calendar.DAY_OF_YEAR, -6);
                String start7 = dFormat.format(cal.getTime());
                return new String[]{start7, today};
            case THIS_MONTH:
                cal.set(Calendar.DAY_OF_MONTH, 1);
                String startMonth = dFormat.format(cal.getTime());
                return new String[]{startMonth, today};
            case ALL_TIME:
            default:
                return new String[]{null, null};
        }
    }

    private String getPeriodLabel() {
        switch (selectedSalesPeriod) {
            case TODAY: return "Today";
            case YESTERDAY: return "Yesterday";
            case DAYS_7: return "7 Days";
            case THIS_MONTH: return "This Month";
            case ALL_TIME: return "All Time";
            default: return "Period";
        }
    }

    private View createTopSellerRow(int rank, String name, int qty, double revenue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 8, 0, 8);

        TextView textRank = new TextView(this);
        textRank.setText("#" + rank);
        textRank.setTextSize(12);
        textRank.setTypeface(null, Typeface.BOLD);
        textRank.setTextColor(ContextCompat.getColor(this, R.color.colorTextMuted));
        textRank.setPadding(0, 0, 10, 0);

        TextView textName = new TextView(this);
        textName.setText(name);
        textName.setTextSize(13);
        textName.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
        textName.setTypeface(null, Typeface.BOLD);
        textName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView textQty = new TextView(this);
        textQty.setText(qty + " sold");
        textQty.setTextSize(11);
        textQty.setTextColor(ContextCompat.getColor(this, R.color.colorTextMuted));
        textQty.setPadding(8, 0, 12, 0);

        TextView textRev = new TextView(this);
        textRev.setText(String.format(Locale.US, "₱%.2f", revenue));
        textRev.setTextSize(12);
        textRev.setTypeface(null, Typeface.BOLD);
        textRev.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));

        row.addView(textRank);
        row.addView(textName);
        row.addView(textQty);
        row.addView(textRev);

        return row;
    }

    private View createDebtorRow(DatabaseHelper.CustomerDebtor debtor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 8, 0, 8);

        LinearLayout infoCol = new LinearLayout(this);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView textName = new TextView(this);
        textName.setText(debtor.getCustomerName());
        textName.setTextSize(13);
        textName.setTypeface(null, Typeface.BOLD);
        textName.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));

        TextView textDetails = new TextView(this);
        textDetails.setText(debtor.getOrderCount() + " unpaid slip(s) • Last: " + debtor.getLastDate());
        textDetails.setTextSize(11);
        textDetails.setTextColor(ContextCompat.getColor(this, R.color.colorTextMuted));

        infoCol.addView(textName);
        infoCol.addView(textDetails);

        TextView textAmount = new TextView(this);
        textAmount.setText(String.format(Locale.US, "₱%.2f", debtor.getTotalOwed()));
        textAmount.setTextSize(13);
        textAmount.setTypeface(null, Typeface.BOLD);
        textAmount.setTextColor(ContextCompat.getColor(this, R.color.colorDanger));
        textAmount.setPadding(8, 0, 10, 0);

        int target48 = (int) (48 * getResources().getDisplayMetrics().density);
        Button btnSettleAll = new Button(this);
        btnSettleAll.setText("Settle All");
        btnSettleAll.setTextSize(12);
        btnSettleAll.setTypeface(null, Typeface.BOLD);
        btnSettleAll.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        btnSettleAll.setBackgroundResource(R.drawable.button_tonal);
        btnSettleAll.setPadding(14, 0, 14, 0);
        btnSettleAll.setMinimumHeight(target48);
        btnSettleAll.setMinimumWidth(target48);
        btnSettleAll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                target48
        ));

        btnSettleAll.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Settle All Utang: " + debtor.getCustomerName())
                .setMessage("Total Balance: " + String.format(Locale.US, "₱%.2f", debtor.getTotalOwed()) +
                        "\nNumber of unpaid transactions: " + debtor.getOrderCount() +
                        "\n\nMark all outstanding debt as settled/paid?")
                .setPositiveButton("Confirm Settle", (dialog, which) -> {
                    int updated = dbHelper.settleAllCustomerCredit(debtor.getCustomerName());
                    Toast.makeText(this, "Cleared " + updated + " transaction(s) for " + debtor.getCustomerName() + "!", Toast.LENGTH_SHORT).show();
                    renderSales();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        row.addView(infoCol);
        row.addView(textAmount);
        row.addView(btnSettleAll);

        return row;
    }

    private void confirmVoidSale(SaleTransaction sale) {
        new AlertDialog.Builder(this)
            .setTitle("🚫 Void Transaction: " + sale.getReceiptNo())
            .setMessage("Are you sure you want to VOID this transaction (" + String.format(Locale.US, "₱%.2f", sale.getTotalAmount()) + ")?" +
                    "\n\n⚠️ This will cancel the sale and automatically return all items back into inventory stock.")
            .setPositiveButton("Confirm Void", (dialog, which) -> {
                boolean success = dbHelper.voidSale(sale.getId());
                if (success) {
                    Toast.makeText(this, "Transaction " + sale.getReceiptNo() + " VOIDED! Stock returned to inventory.", Toast.LENGTH_LONG).show();
                    refreshAllData();
                } else {
                    Toast.makeText(this, "Failed to void transaction.", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void confirmDeleteSale(SaleTransaction sale) {
        requireOwnerPin("Delete Sale Record", () -> {
            boolean isVoid = "VOID".equalsIgnoreCase(sale.getPaymentMode());

            if (isVoid) {
                new AlertDialog.Builder(this)
                    .setTitle("🗑️ Delete Voided Record: " + sale.getReceiptNo())
                    .setMessage("Permanently remove this voided transaction record?\n\n(Inventory stock was already returned when voided.)")
                    .setPositiveButton("Delete Permanently", (dialog, which) -> {
                        boolean success = dbHelper.deleteSale(sale.getId(), false);
                        if (success) {
                            Toast.makeText(this, "Voided transaction " + sale.getReceiptNo() + " deleted.", Toast.LENGTH_SHORT).show();
                            renderSales();
                        } else {
                            Toast.makeText(this, "Failed to delete record.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            } else {
                new AlertDialog.Builder(this)
                    .setTitle("🗑️ Delete Transaction: " + sale.getReceiptNo())
                    .setMessage("Total: ₱" + String.format(Locale.US, "%.2f", sale.getTotalAmount()) + " • " + sale.getDateTime() +
                            "\nPayment: " + sale.getPaymentMode() +
                            "\n\nWould you like to return the sold items back into inventory stock before deleting this record?")
                    .setPositiveButton("Restock & Delete", (dialog, which) -> {
                        boolean success = dbHelper.deleteSale(sale.getId(), true);
                        if (success) {
                            Toast.makeText(this, "Transaction " + sale.getReceiptNo() + " deleted! Stock returned to inventory.", Toast.LENGTH_LONG).show();
                            refreshAllData();
                        } else {
                            Toast.makeText(this, "Failed to delete transaction.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton("Delete Record Only", (dialog, which) -> {
                        boolean success = dbHelper.deleteSale(sale.getId(), false);
                        if (success) {
                            Toast.makeText(this, "Transaction " + sale.getReceiptNo() + " deleted from history.", Toast.LENGTH_SHORT).show();
                            refreshAllData();
                        } else {
                            Toast.makeText(this, "Failed to delete transaction.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });
    }

    private void openManageSalesHistoryDialog() {
        String[] options = {
            "🗑️ Delete All Voided Sales (Declutter)",
            "🧹 Clear Sales for Current Period (" + getPeriodLabel() + ")",
            "⚠️ Clear Entire Sales History (Full Reset)"
        };

        new AlertDialog.Builder(this)
            .setTitle("Manage & Clean Sales History")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    new AlertDialog.Builder(this)
                        .setTitle("Delete All Voided Sales")
                        .setMessage("Are you sure you want to delete all cancelled/voided transaction records? This will clean up your sales history.")
                        .setPositiveButton("Confirm Clean", (d, w) -> {
                            int count = dbHelper.deleteAllVoidedSales();
                            Toast.makeText(this, "Deleted " + count + " voided transaction(s).", Toast.LENGTH_SHORT).show();
                            renderSales();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                } else if (which == 1) {
                    String[] dates = getSelectedPeriodDateRange();
                    new AlertDialog.Builder(this)
                        .setTitle("Clear Sales for " + getPeriodLabel())
                        .setMessage("Delete all sales records for the " + getPeriodLabel() + " time period?\n\nDo you want to restock inventory items as well?")
                        .setPositiveButton("Restock & Clear", (d, w) -> {
                            int count = dbHelper.clearSalesHistory(dates[0], dates[1], true);
                            Toast.makeText(this, "Cleared " + count + " sale(s) and restocked inventory.", Toast.LENGTH_LONG).show();
                            refreshAllData();
                        })
                        .setNeutralButton("Clear Records Only", (d, w) -> {
                            int count = dbHelper.clearSalesHistory(dates[0], dates[1], false);
                            Toast.makeText(this, "Cleared " + count + " sale(s) from history.", Toast.LENGTH_LONG).show();
                            refreshAllData();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                } else if (which == 2) {
                    new AlertDialog.Builder(this)
                        .setTitle("⚠️ Reset Entire Sales History")
                        .setMessage("WARNING: This will permanently delete ALL recorded sales transactions across all time.\n\nAre you absolutely sure you want to proceed?")
                        .setPositiveButton("Yes, Reset Everything", (d, w) -> {
                            int count = dbHelper.clearSalesHistory(null, null, false);
                            receiptCounter = 1001;
                            Toast.makeText(this, "Entire sales history cleared (" + count + " records).", Toast.LENGTH_LONG).show();
                            refreshAllData();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void renderSales() {
        if (layoutSalesItemsContainer == null) return;
        layoutSalesItemsContainer.removeAllViews();

        updatePeriodChipsUI();
        updatePaymentFilterUI();

        String[] dates = getSelectedPeriodDateRange();
        String startDate = dates[0];
        String endDate = dates[1];

        // 1. Update Store Merchandise Sales Metrics & Profit
        DatabaseHelper.SalesStats stats = dbHelper.getSalesStats(startDate, endDate);

        if (textSalesTodayDate != null) textSalesTodayDate.setText(getPeriodLabel());
        if (textSalesTodayTotal != null) textSalesTodayTotal.setText(String.format(Locale.US, "₱%,.2f", stats.grossSales));
        if (textSalesNetProfit != null) {
            textSalesNetProfit.setText(String.format(Locale.US, "Profit: +₱%,.2f (%.1f%% margin)", stats.netProfit, stats.profitMarginPercent));
        }
        if (textSalesTodayOrders != null) textSalesTodayOrders.setText(String.valueOf(stats.orderCount));
        if (textSalesTodayCash != null) textSalesTodayCash.setText(String.format(Locale.US, "₱%,.2f", stats.cashTotal));
        if (textSalesTodayGcash != null) textSalesTodayGcash.setText(String.format(Locale.US, "₱%,.2f", stats.gcashTotal));
        if (textSalesUnpaidCredit != null) textSalesUnpaidCredit.setText(String.format(Locale.US, "₱%,.2f", stats.unpaidCreditTotal));

        // 1.5 Update GCash Remittance Services & Drawer Reconciliation
        DatabaseHelper.GcashStats gcashStats = dbHelper.getGcashStats(startDate, endDate);
        if (textSalesGcashFees != null) {
            textSalesGcashFees.setText(String.format(Locale.US, "+₱%,.2f", gcashStats.totalFeesEarned));
        }
        if (textSalesGcashTxCount != null) {
            int totalGcashTx = gcashStats.countCashIn + gcashStats.countCashOut;
            textSalesGcashTxCount.setText(totalGcashTx + (totalGcashTx == 1 ? " txn" : " txns"));
        }
        if (textSalesGcashCashin != null) {
            textSalesGcashCashin.setText(String.format(Locale.US, "₱%,.2f (%dx)", gcashStats.totalCashInAmount, gcashStats.countCashIn));
        }
        if (textSalesGcashCashout != null) {
            textSalesGcashCashout.setText(String.format(Locale.US, "₱%,.2f (%dx)", gcashStats.totalCashOutAmount, gcashStats.countCashOut));
        }

        double cashInHandled = gcashStats.totalCashInAmount + gcashStats.totalFeesEarned;
        double expectedDrawerCash = stats.cashTotal + cashInHandled - gcashStats.totalCashOutAmount;
        if (textSalesExpectedDrawerCash != null) {
            textSalesExpectedDrawerCash.setText(String.format(Locale.US, "₱%,.2f", expectedDrawerCash));
        }
        if (textSalesDrawerCalcBreakdown != null) {
            textSalesDrawerCalcBreakdown.setText(String.format(Locale.US, "Store Cash (₱%,.2f) + Cash-In (₱%,.2f) - Cash-Out (₱%,.2f)",
                    stats.cashTotal, cashInHandled, gcashStats.totalCashOutAmount));
        }

        // 2. Multi-Segment Proportion Bar
        if (layoutSalesShareBar != null) {
            double gross = stats.grossSales;
            if (gross > 0) {
                layoutSalesShareBar.setVisibility(View.VISIBLE);
                float cashW = (float) Math.max(0.0, (stats.cashTotal / gross) * 100.0);
                float gcashW = (float) Math.max(0.0, (stats.gcashTotal / gross) * 100.0);
                float creditW = (float) Math.max(0.0, (stats.unpaidCreditTotal / gross) * 100.0);

                if (barSegmentCash != null) barSegmentCash.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, cashW));
                if (barSegmentGcash != null) barSegmentGcash.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, gcashW));
                if (barSegmentCredit != null) barSegmentCredit.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, creditW));
            } else {
                layoutSalesShareBar.setVisibility(View.GONE);
            }
        }

        // 3. Top 5 Best-Selling Items Card
        if (containerTopSellersItems != null && layoutSalesTopSellersCard != null) {
            containerTopSellersItems.removeAllViews();
            List<DatabaseHelper.TopSellingItem> topItems = dbHelper.getTopSellingItems(startDate, endDate, 5);
            if (topItems == null || topItems.isEmpty()) {
                layoutSalesTopSellersCard.setVisibility(View.GONE);
            } else {
                layoutSalesTopSellersCard.setVisibility(View.VISIBLE);
                if (textTopSellersPeriod != null) textTopSellersPeriod.setText(getPeriodLabel() + " • Top 5");
                for (int i = 0; i < topItems.size(); i++) {
                    DatabaseHelper.TopSellingItem item = topItems.get(i);
                    containerTopSellersItems.addView(createTopSellerRow(i + 1, item.getName(), item.getQuantity(), item.getTotalRevenue()));
                }
            }
        }

        // 4. Customer Utang Ledger Card
        if (layoutDebtorsSummaryContainer != null && containerDebtorsItems != null) {
            containerDebtorsItems.removeAllViews();
            boolean isDebtFilter = "CREDIT_UNPAID".equals(selectedSalesFilterMode);
            List<DatabaseHelper.CustomerDebtor> debtors = dbHelper.getCustomerDebtors();
            if ((isDebtFilter || stats.unpaidCreditTotal > 0) && debtors != null && !debtors.isEmpty()) {
                layoutDebtorsSummaryContainer.setVisibility(View.VISIBLE);
                for (DatabaseHelper.CustomerDebtor debtor : debtors) {
                    containerDebtorsItems.addView(createDebtorRow(debtor));
                }
            } else {
                layoutDebtorsSummaryContainer.setVisibility(View.GONE);
            }
        }

        // 5. Query and Render Individual Transactions
        String searchQuery = editSalesSearch != null ? editSalesSearch.getText().toString().trim() : null;
        List<SaleTransaction> sales = dbHelper.getFilteredSales(startDate, endDate, selectedSalesFilterMode, searchQuery);

        if (sales == null || sales.isEmpty()) {
            if (textSalesEmpty != null) {
                textSalesEmpty.setVisibility(View.VISIBLE);
                if (searchQuery != null && !searchQuery.isEmpty()) {
                    textSalesEmpty.setText("No transactions matching \"" + searchQuery + "\".");
                } else if ("CREDIT_UNPAID".equals(selectedSalesFilterMode)) {
                    textSalesEmpty.setText("No outstanding unpaid utang in this period.");
                } else {
                    textSalesEmpty.setText("No transactions recorded for " + getPeriodLabel().toLowerCase(Locale.US) + ".");
                }
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
            Button btnVoid = itemView.findViewById(R.id.btn_void_sale);
            Button btnReprint = itemView.findViewById(R.id.btn_reprint_receipt);

            textReceiptNo.setText(sale.getReceiptNo());
            textDateTime.setText(sale.getDateTime());

            String mode = sale.getPaymentMode();
            boolean isVoid = "VOID".equalsIgnoreCase(mode);

            if (isVoid) {
                textBadge.setText("VOIDED");
                textBadge.setBackgroundResource(R.drawable.badge_red);
                textBadge.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeRedText));
                textTotal.setText("₱0.00 (VOID)");
                textTotal.setTextColor(ContextCompat.getColor(this, R.color.colorTextLight));
                itemView.setAlpha(0.6f);
                if (btnSettle != null) btnSettle.setVisibility(View.GONE);
                if (btnVoid != null) btnVoid.setVisibility(View.GONE);
            } else {
                itemView.setAlpha(1.0f);
                textTotal.setText(String.format(Locale.US, "₱%.2f", sale.getTotalAmount()));
                textTotal.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));

                if ("CREDIT".equalsIgnoreCase(mode)) {
                    if (sale.isSettled()) {
                        textBadge.setText("CREDIT (PAID)");
                        textBadge.setBackgroundResource(R.drawable.badge_green);
                        textBadge.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeGreenText));
                        if (btnSettle != null) btnSettle.setVisibility(View.GONE);
                    } else {
                        textBadge.setText("UNPAID UTANG");
                        textBadge.setBackgroundResource(R.drawable.badge_red);
                        textBadge.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeRedText));
                        if (btnSettle != null) {
                            btnSettle.setVisibility(View.VISIBLE);
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
                        }
                    }
                } else if ("GCASH".equalsIgnoreCase(mode)) {
                    textBadge.setText("GCASH");
                    textBadge.setBackgroundResource(R.drawable.badge_blue);
                    textBadge.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeBlueText));
                    if (btnSettle != null) btnSettle.setVisibility(View.GONE);
                } else {
                    textBadge.setText("CASH");
                    textBadge.setBackgroundResource(R.drawable.badge_green);
                    textBadge.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeGreenText));
                    if (btnSettle != null) btnSettle.setVisibility(View.GONE);
                }

                if (btnVoid != null) {
                    btnVoid.setVisibility(View.VISIBLE);
                    btnVoid.setOnClickListener(v -> confirmVoidSale(sale));
                }
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

            // Re-print Receipt Click
            if (btnReprint != null) {
                btnReprint.setOnClickListener(v -> onReprintClicked(sale));
            }

            // Delete Transaction Click
            Button btnDelete = itemView.findViewById(R.id.btn_delete_sale);
            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> confirmDeleteSale(sale));
            }

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
            sb.append(leftRightText(receiptDesign.getEffectiveReceiptNoLabel() + " " + sale.getReceiptNo(), "", W)).append("\n");
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
        final int W = receiptDesign.charactersPerLine > 0 ? receiptDesign.charactersPerLine : 32;
        float widthMm = receiptDesign.paperWidthMm == 80 ? 72f : 48f;
        String divider = configDivider(receiptDesign.dividerStyle, W);
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, widthMm, W);

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
            receipt.append("[L]").append(receiptDesign.getEffectiveReceiptNoLabel()).append(" ").append(sale.getReceiptNo()).append("\n");
        }

        receipt.append("[C]").append(divider).append("\n");
        receipt.append("[L]<b>ITEM</b>[R]<b>PRICE</b>\n");

        List<SaleTransactionItem> items = sale.getItems();
        if (items != null) {
            int maxNameLen = W > 32 ? 26 : 18;
            for (SaleTransactionItem item : items) {
                String itemName = item.getItemName();
                if (itemName.length() > maxNameLen) itemName = itemName.substring(0, maxNameLen - 2) + "..";
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

        boolean shouldPrintQr = receiptDesign.enableGcashQrOnReceipt && receiptDesign.hasGcashQrImage(this);
        if (shouldPrintQr && "GCASH_ONLY".equalsIgnoreCase(receiptDesign.gcashQrPrintMode)) {
            shouldPrintQr = "GCASH".equalsIgnoreCase(sale.getPaymentMode());
        }
        if (shouldPrintQr) {
            Bitmap qrBmp = loadGcashQrBitmap();
            if (qrBmp != null) {
                try {
                    String qrHex = com.dantsu.escposprinter.textparser.PrinterTextParserImg.bitmapToHexadecimalString(printer, qrBmp, false);
                    receipt.append("[C]").append(divider).append("\n");
                    receipt.append("[C]<img>").append(qrHex).append("</img>\n");
                    receipt.append("[C]<b>Scan to Pay with GCash</b>\n");
                } catch (Exception e) {
                    Log.e("MorpsPOS", "Error printing GCash QR on past receipt", e);
                }
            }
        }
        receipt.append("\n\n");

        return printer.addTextToPrint(receipt.toString());
    }

    private void onPrintZReportClicked() {
        SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String today = dFormat.format(new Date());

        String[] reportTypes = {
            "📋 Current Shift Summary (Mid-Day / Anytime Snapshot)",
            "📊 End-of-Day Closing Report (Final Daily Totals)"
        };

        new AlertDialog.Builder(this)
            .setTitle("Select Sales Report (" + today + ")")
            .setItems(reportTypes, (dialog, which) -> {
                boolean isClosingReport = (which == 1);
                promptReportAction(today, isClosingReport);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void promptReportAction(String dateOnly, boolean isClosingReport) {
        String reportName = isClosingReport ? "End-of-Day Closing Report" : "Current Shift Summary";
        String[] actions = {
            "👁️ View on Screen Preview",
            "🖨️ Print to Bluetooth (" + receiptDesign.paperWidthMm + "mm)",
            "🔌 Print to USB (" + receiptDesign.paperWidthMm + "mm)"
        };

        new AlertDialog.Builder(this)
            .setTitle(reportName + " (" + dateOnly + ")")
            .setItems(actions, (dialog, which) -> {
                if (which == 0) {
                    showReportPreviewDialog(dateOnly, isClosingReport);
                } else if (which == 1) {
                    printReportBluetooth(dateOnly, isClosingReport);
                } else if (which == 2) {
                    printReportUsb(dateOnly, isClosingReport);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showReportPreviewDialog(String dateOnly, boolean isClosingReport) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_receipt_preview, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView textPreview = dialogView.findViewById(R.id.text_receipt_preview_content);
        textPreview.setText(generateReportText(dateOnly, isClosingReport));

        dialogView.findViewById(R.id.btn_close_preview).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_print_from_preview).setOnClickListener(v -> {
            dialog.dismiss();
            printReportBluetooth(dateOnly, isClosingReport);
        });

        dialog.show();
    }

    private void printReportBluetooth(String dateOnly, boolean isClosingReport) {
        final String reportTitle = isClosingReport ? "End-of-Day Closing Report" : "Current Shift Summary";
        this.checkBluetoothPermissions(() -> {
            new AsyncBluetoothEscPosPrint(
                this,
                new AsyncEscPosPrint.OnPrintFinished() {
                    @Override
                    public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                        Log.e("AsyncPrint", "Print error code: " + codeException);
                        Toast.makeText(MainActivity.this, "Unable to print " + reportTitle + "! Make sure Bluetooth and printer are on.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                        Toast.makeText(MainActivity.this, reportTitle + " printed successfully!", Toast.LENGTH_SHORT).show();
                    }
                }
            ).execute(this.getReportPrinter(selectedBluetoothDevice, dateOnly, isClosingReport));
        });
    }

    private void printReportUsb(String dateOnly, boolean isClosingReport) {
        final String reportTitle = isClosingReport ? "End-of-Day Closing Report" : "Current Shift Summary";
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
                        Toast.makeText(MainActivity.this, reportTitle + " printed via USB!", Toast.LENGTH_SHORT).show();
                    }
                }
            ).execute(getReportPrinter(usbConnection, dateOnly, isClosingReport));
        } catch (Exception e) {
            Toast.makeText(this, "USB Print failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String generateReportText(String dateOnly, boolean isClosingReport) {
        final int W = receiptDesign.charactersPerLine > 0 ? receiptDesign.charactersPerLine : 32;
        String divider = receiptDesign.getDivider(W);
        StringBuilder sb = new StringBuilder();

        sb.append(centerText(receiptDesign.storeName, W)).append("\n");
        sb.append(centerText(isClosingReport ? "END-OF-DAY CLOSING REPORT" : "CURRENT SHIFT SUMMARY", W)).append("\n");
        sb.append(centerText(isClosingReport ? "(FINAL DAILY SUMMARY)" : "(MID-DAY SALES SNAPSHOT)", W)).append("\n");
        sb.append(divider).append("\n");

        SimpleDateFormat fullFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        sb.append(leftRightText("Printed: " + fullFmt.format(new Date()), "", W)).append("\n");
        sb.append(leftRightText("Business Date: " + dateOnly, "", W)).append("\n");
        sb.append(divider).append("\n");

        DatabaseHelper.SalesStats stats = dbHelper.getSalesStats(dateOnly, dateOnly);
        DatabaseHelper.GcashStats gcashStats = dbHelper.getGcashStats(dateOnly, dateOnly);

        sb.append(leftRightText("[1] STORE MERCHANDISE SALES", "", W)).append("\n");
        sb.append(leftRightText("Gross Store Sales:", String.format(Locale.US, "PHP %.2f", stats.grossSales), W)).append("\n");
        sb.append(leftRightText("Cost of Goods:", String.format(Locale.US, "PHP %.2f", stats.totalCost), W)).append("\n");
        sb.append(leftRightText("Store Net Profit:", String.format(Locale.US, "PHP %.2f", stats.netProfit), W)).append("\n");
        sb.append(leftRightText("Profit Margin:", String.format(Locale.US, "%.1f%%", stats.profitMarginPercent), W)).append("\n");
        sb.append(leftRightText("Orders Completed:", String.valueOf(stats.orderCount), W)).append("\n");
        sb.append(leftRightText("- Store Cash:", String.format(Locale.US, "PHP %.2f", stats.cashTotal), W)).append("\n");
        sb.append(leftRightText("- Store GCash:", String.format(Locale.US, "PHP %.2f", stats.gcashTotal), W)).append("\n");
        sb.append(leftRightText("- Unpaid Utang:", String.format(Locale.US, "PHP %.2f", stats.unpaidCreditTotal), W)).append("\n");
        sb.append(divider).append("\n");

        sb.append(leftRightText("[2] GCASH REMITTANCE SERVICES", "", W)).append("\n");
        sb.append(leftRightText("Total Cash-In:", String.format(Locale.US, "PHP %.2f", gcashStats.totalCashInAmount), W)).append("\n");
        sb.append(leftRightText("Total Cash-Out:", String.format(Locale.US, "PHP %.2f", gcashStats.totalCashOutAmount), W)).append("\n");
        sb.append(leftRightText("GCash Fees (Profit):", String.format(Locale.US, "PHP %.2f", gcashStats.totalFeesEarned), W)).append("\n");
        int totalGcashOrders = gcashStats.countCashIn + gcashStats.countCashOut;
        sb.append(leftRightText("Remittance Orders:", String.valueOf(totalGcashOrders), W)).append("\n");
        sb.append(divider).append("\n");

        double totalCashInHandled = gcashStats.totalCashInAmount + gcashStats.totalFeesEarned;
        double expectedDrawerCash = stats.cashTotal + totalCashInHandled - gcashStats.totalCashOutAmount;
        double totalEarnings = stats.netProfit + gcashStats.totalFeesEarned;

        sb.append(leftRightText("[3] CASH DRAWER RECONCILIATION", "", W)).append("\n");
        sb.append(leftRightText("Store Cash In:", String.format(Locale.US, "PHP %.2f", stats.cashTotal), W)).append("\n");
        sb.append(leftRightText("+ Cash-In Cash:", String.format(Locale.US, "PHP %.2f", totalCashInHandled), W)).append("\n");
        sb.append(leftRightText("- Cash-Out Cash:", String.format(Locale.US, "PHP %.2f", gcashStats.totalCashOutAmount), W)).append("\n");
        sb.append(leftRightText("EXPECTED DRAWER CASH:", String.format(Locale.US, "PHP %.2f", expectedDrawerCash), W)).append("\n");
        sb.append(leftRightText("COMBINED NET INCOME:", String.format(Locale.US, "PHP %.2f", totalEarnings), W)).append("\n");
        sb.append(divider).append("\n");

        // Top 3 Best-Sellers of the Day
        List<DatabaseHelper.TopSellingItem> topItems = dbHelper.getTopSellingItems(dateOnly, dateOnly, 3);
        if (topItems != null && !topItems.isEmpty()) {
            sb.append(leftRightText("TOP 3 BEST-SELLERS", "", W)).append("\n");
            int maxNameLen = W > 32 ? 26 : 16;
            for (int i = 0; i < topItems.size(); i++) {
                DatabaseHelper.TopSellingItem item = topItems.get(i);
                String name = item.getName();
                if (name.length() > maxNameLen) name = name.substring(0, maxNameLen - 2) + "..";
                sb.append(leftRightText((i + 1) + ". " + name + " (" + item.getQuantity() + "x)", String.format(Locale.US, "%.2f", item.getTotalRevenue()), W)).append("\n");
            }
            sb.append(divider).append("\n");
        }

        int prodCount = dbHelper.getProductCount();
        int stockUnits = dbHelper.getTotalStockUnits();
        double invValue = dbHelper.getTotalInventoryValue();

        sb.append(leftRightText("INVENTORY STATUS", "", W)).append("\n");
        sb.append(leftRightText("Total Products:", String.valueOf(prodCount), W)).append("\n");
        sb.append(leftRightText("In-Stock Units:", String.valueOf(stockUnits), W)).append("\n");
        sb.append(leftRightText("Current Valuation:", String.format(Locale.US, "PHP %.2f", invValue), W)).append("\n");
        sb.append(divider).append("\n");

        sb.append(centerText(isClosingReport ? "End of Daily Closing Report" : "End of Shift Sales Summary", W)).append("\n");
        sb.append(centerText("Morps POS System", W)).append("\n");

        return sb.toString();
    }

    public AsyncEscPosPrinter getReportPrinter(DeviceConnection printerConnection, String dateOnly, boolean isClosingReport) {
        final int W = receiptDesign.charactersPerLine > 0 ? receiptDesign.charactersPerLine : 32;
        float widthMm = receiptDesign.paperWidthMm == 80 ? 72f : 48f;
        String divider = configDivider(receiptDesign.dividerStyle, W);
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, widthMm, W);

        StringBuilder sb = new StringBuilder();
        sb.append("[C]<b><font size='big'>").append(receiptDesign.storeName).append("</font></b>\n");
        sb.append("[C]<b>").append(isClosingReport ? "END-OF-DAY CLOSING REPORT" : "CURRENT SHIFT SUMMARY").append("</b>\n");
        sb.append("[C]").append(isClosingReport ? "<font size='small'>(FINAL DAILY SUMMARY)</font>" : "<font size='small'>(MID-DAY SALES SNAPSHOT)</font>").append("\n");
        sb.append("[C]").append(divider).append("\n");

        SimpleDateFormat fullFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        sb.append("[L]Printed: ").append(fullFmt.format(new Date())).append("\n");
        sb.append("[L]Business Date: ").append(dateOnly).append("\n");
        sb.append("[C]").append(divider).append("\n");

        DatabaseHelper.SalesStats stats = dbHelper.getSalesStats(dateOnly, dateOnly);
        DatabaseHelper.GcashStats gcashStats = dbHelper.getGcashStats(dateOnly, dateOnly);

        sb.append("[L]<b>[1] STORE MERCHANDISE SALES</b>\n");
        sb.append("[L]Gross Store Sales:[R]<b>PHP ").append(String.format(Locale.US, "%.2f", stats.grossSales)).append("</b>\n");
        sb.append("[L]Cost of Goods:[R]PHP ").append(String.format(Locale.US, "%.2f", stats.totalCost)).append("\n");
        sb.append("[L]Store Net Profit:[R]<b>PHP ").append(String.format(Locale.US, "%.2f", stats.netProfit)).append("</b>\n");
        sb.append("[L]Profit Margin:[R]").append(String.format(Locale.US, "%.1f%%", stats.profitMarginPercent)).append("\n");
        sb.append("[L]Store Orders:[R]").append(stats.orderCount).append("\n");
        sb.append("[L]- Store Cash:[R]PHP ").append(String.format(Locale.US, "%.2f", stats.cashTotal)).append("\n");
        sb.append("[L]- Store GCash:[R]PHP ").append(String.format(Locale.US, "%.2f", stats.gcashTotal)).append("\n");
        sb.append("[L]- Unpaid Utang:[R]PHP ").append(String.format(Locale.US, "%.2f", stats.unpaidCreditTotal)).append("\n");
        sb.append("[C]").append(divider).append("\n");

        sb.append("[L]<b>[2] GCASH REMITTANCE SERVICES</b>\n");
        sb.append("[L]Total Cash-In:[R]PHP ").append(String.format(Locale.US, "%.2f", gcashStats.totalCashInAmount)).append("\n");
        sb.append("[L]Total Cash-Out:[R]PHP ").append(String.format(Locale.US, "%.2f", gcashStats.totalCashOutAmount)).append("\n");
        sb.append("[L]GCash Fees (Profit):[R]<b>PHP ").append(String.format(Locale.US, "%.2f", gcashStats.totalFeesEarned)).append("</b>\n");
        int totalGcashOrders = gcashStats.countCashIn + gcashStats.countCashOut;
        sb.append("[L]Remittance Orders:[R]").append(totalGcashOrders).append("\n");
        sb.append("[C]").append(divider).append("\n");

        double totalCashInHandled = gcashStats.totalCashInAmount + gcashStats.totalFeesEarned;
        double expectedDrawerCash = stats.cashTotal + totalCashInHandled - gcashStats.totalCashOutAmount;
        double totalEarnings = stats.netProfit + gcashStats.totalFeesEarned;

        sb.append("[L]<b>[3] CASH DRAWER RECONCILIATION</b>\n");
        sb.append("[L]Store Cash In:[R]PHP ").append(String.format(Locale.US, "%.2f", stats.cashTotal)).append("\n");
        sb.append("[L]+ Cash-In Cash:[R]PHP ").append(String.format(Locale.US, "%.2f", totalCashInHandled)).append("\n");
        sb.append("[L]- Cash-Out Cash:[R]PHP ").append(String.format(Locale.US, "%.2f", gcashStats.totalCashOutAmount)).append("\n");
        sb.append("[L]<b>EXPECTED DRAWER CASH:</b>[R]<b>PHP ").append(String.format(Locale.US, "%.2f", expectedDrawerCash)).append("</b>\n");
        sb.append("[L]<b>COMBINED NET INCOME:</b>[R]<b>PHP ").append(String.format(Locale.US, "%.2f", totalEarnings)).append("</b>\n");
        sb.append("[C]").append(divider).append("\n");

        // Top 3 Best-Sellers of the Day
        List<DatabaseHelper.TopSellingItem> topItems = dbHelper.getTopSellingItems(dateOnly, dateOnly, 3);
        if (topItems != null && !topItems.isEmpty()) {
            sb.append("[L]<b>TOP 3 BEST-SELLERS</b>\n");
            int maxNameLen = W > 32 ? 26 : 16;
            for (int i = 0; i < topItems.size(); i++) {
                DatabaseHelper.TopSellingItem item = topItems.get(i);
                String name = item.getName();
                if (name.length() > maxNameLen) name = name.substring(0, maxNameLen - 2) + "..";
                sb.append("[L]").append(i + 1).append(". ").append(name).append(" (").append(item.getQuantity()).append("x)")
                  .append("[R]").append(String.format(Locale.US, "%.2f", item.getTotalRevenue())).append("\n");
            }
            sb.append("[C]").append(divider).append("\n");
        }

        int prodCount = dbHelper.getProductCount();
        int stockUnits = dbHelper.getTotalStockUnits();
        double invValue = dbHelper.getTotalInventoryValue();

        sb.append("[L]<b>INVENTORY STATUS</b>\n");
        sb.append("[L]Total Products:[R]").append(prodCount).append("\n");
        sb.append("[L]In-Stock Units:[R]").append(stockUnits).append("\n");
        sb.append("[L]Stock Value:[R]PHP ").append(String.format(Locale.US, "%.2f", invValue)).append("\n");
        sb.append("[C]").append(divider).append("\n");

        sb.append("[C]").append(isClosingReport ? "End of Daily Closing Report" : "End of Shift Sales Summary").append("\n");
        sb.append("[C]Morps POS System\n\n\n");

        return printer.addTextToPrint(sb.toString());
    }

    /*==============================================================================================
    ==================================GCASH CASH-IN & CASH-OUT SECTION=============================
    ==============================================================================================*/

    private void setupGcashEvents() {
        if (btnGcashNewCashIn != null) {
            btnGcashNewCashIn.setOnClickListener(v -> openRecordGcashDialog(GcashTransaction.TYPE_CASH_IN));
        }
        if (btnGcashNewCashOut != null) {
            btnGcashNewCashOut.setOnClickListener(v -> openRecordGcashDialog(GcashTransaction.TYPE_CASH_OUT));
        }
        if (btnGcashAdjustFloat != null) {
            btnGcashAdjustFloat.setOnClickListener(v -> requireOwnerPin("Adjust GCash Float", () -> openAdjustGcashFloatDialog()));
        }
        if (btnGcashExportCsv != null) {
            btnGcashExportCsv.setOnClickListener(v -> exportGcashToCsv());
        }
        if (btnGcashManageHistory != null) {
            btnGcashManageHistory.setOnClickListener(v -> requireOwnerPin("Manage GCash History", () -> openManageGcashHistoryDialog()));
        }

        if (chipGcashToday != null) chipGcashToday.setOnClickListener(v -> selectGcashPeriod(SalesPeriod.TODAY));
        if (chipGcashYesterday != null) chipGcashYesterday.setOnClickListener(v -> selectGcashPeriod(SalesPeriod.YESTERDAY));
        if (chipGcash7days != null) chipGcash7days.setOnClickListener(v -> selectGcashPeriod(SalesPeriod.DAYS_7));
        if (chipGcash30days != null) chipGcash30days.setOnClickListener(v -> selectGcashPeriod(SalesPeriod.THIS_MONTH));
        if (chipGcashAlltime != null) chipGcashAlltime.setOnClickListener(v -> selectGcashPeriod(SalesPeriod.ALL_TIME));

        if (filterGcashAll != null) filterGcashAll.setOnClickListener(v -> selectGcashFilterType("ALL"));
        if (filterGcashIn != null) filterGcashIn.setOnClickListener(v -> selectGcashFilterType("CASH_IN"));
        if (filterGcashOut != null) filterGcashOut.setOnClickListener(v -> selectGcashFilterType("CASH_OUT"));

        if (editGcashSearch != null) {
            editGcashSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    renderGcash();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void selectGcashPeriod(SalesPeriod period) {
        selectedGcashPeriod = period;
        renderGcash();
    }

    private void selectGcashFilterType(String type) {
        selectedGcashFilterType = type;
        renderGcash();
    }

    private String[] getGcashPeriodDateRange() {
        SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        String today = dFormat.format(cal.getTime());

        switch (selectedGcashPeriod) {
            case TODAY:
                return new String[]{today, today};
            case YESTERDAY:
                cal.add(Calendar.DAY_OF_YEAR, -1);
                String yest = dFormat.format(cal.getTime());
                return new String[]{yest, yest};
            case DAYS_7:
                cal.add(Calendar.DAY_OF_YEAR, -6);
                String start7 = dFormat.format(cal.getTime());
                return new String[]{start7, today};
            case THIS_MONTH:
                cal.set(Calendar.DAY_OF_MONTH, 1);
                String startMonth = dFormat.format(cal.getTime());
                return new String[]{startMonth, today};
            case ALL_TIME:
            default:
                return new String[]{null, null};
        }
    }

    private void updateGcashPeriodChipsUI() {
        Button[] chips = {chipGcashToday, chipGcashYesterday, chipGcash7days, chipGcash30days, chipGcashAlltime};
        SalesPeriod[] periods = {SalesPeriod.TODAY, SalesPeriod.YESTERDAY, SalesPeriod.DAYS_7, SalesPeriod.THIS_MONTH, SalesPeriod.ALL_TIME};
        int activeColor = ContextCompat.getColor(this, R.color.colorOnPrimary);
        int inactiveColor = ContextCompat.getColor(this, R.color.colorSlate600);

        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null) continue;
            boolean isActive = (selectedGcashPeriod == periods[i]);
            chips[i].setBackgroundResource(isActive ? R.drawable.tab_pill_active : R.drawable.tab_pill_inactive);
            chips[i].setTextColor(isActive ? activeColor : inactiveColor);
        }
    }

    private void updateGcashTypeFilterUI() {
        Button[] filters = {filterGcashAll, filterGcashIn, filterGcashOut};
        String[] types = {"ALL", "CASH_IN", "CASH_OUT"};
        int activeColor = ContextCompat.getColor(this, R.color.colorOnPrimary);
        int inactiveColor = ContextCompat.getColor(this, R.color.colorSlate600);

        for (int i = 0; i < filters.length; i++) {
            if (filters[i] == null) continue;
            boolean isActive = selectedGcashFilterType.equals(types[i]);
            filters[i].setBackgroundResource(isActive ? R.drawable.tab_pill_active : R.drawable.tab_pill_inactive);
            filters[i].setTextColor(isActive ? activeColor : inactiveColor);
        }
    }

    private double getGcashStartingFloat() {
        SharedPreferences prefs = getSharedPreferences("MorpsPOSPrefs", Context.MODE_PRIVATE);
        return (double) prefs.getFloat("gcashStartingFloat", 10000.0f);
    }

    private void setGcashStartingFloat(double val) {
        getSharedPreferences("MorpsPOSPrefs", Context.MODE_PRIVATE)
                .edit().putFloat("gcashStartingFloat", (float) val).apply();
    }

    private double getGcashDrawerFloat() {
        SharedPreferences prefs = getSharedPreferences("MorpsPOSPrefs", Context.MODE_PRIVATE);
        return (double) prefs.getFloat("gcashDrawerFloat", 5000.0f);
    }

    private void setGcashDrawerFloat(double val) {
        getSharedPreferences("MorpsPOSPrefs", Context.MODE_PRIVATE)
                .edit().putFloat("gcashDrawerFloat", (float) val).apply();
    }

    private void renderGcash() {
        if (layoutGcashItemsContainer == null) return;
        layoutGcashItemsContainer.removeAllViews();

        updateGcashPeriodChipsUI();
        updateGcashTypeFilterUI();

        String[] dates = getGcashPeriodDateRange();
        String startDate = dates[0];
        String endDate = dates[1];

        // Overall stats across all time for accurate digital float computation
        DatabaseHelper.GcashStats allTimeStats = dbHelper.getGcashStats(null, null);
        double startingFloat = getGcashStartingFloat();
        double currentDigitalBalance = startingFloat + allTimeStats.netGcashWalletFlow;

        double startingDrawer = getGcashDrawerFloat();
        double currentDrawerBalance = startingDrawer + allTimeStats.netCashDrawerFlow;

        if (textGcashDigitalBalance != null) {
            textGcashDigitalBalance.setText(String.format(Locale.US, "₱%,.2f", currentDigitalBalance));
        }
        if (textGcashDigitalFlow != null) {
            textGcashDigitalFlow.setText(String.format(Locale.US, "Base: ₱%,.2f • Net Flow: %s₱%,.2f",
                    startingFloat,
                    allTimeStats.netGcashWalletFlow >= 0 ? "+" : "",
                    allTimeStats.netGcashWalletFlow));
        }

        // Selected period stats for performance display
        DatabaseHelper.GcashStats periodStats = dbHelper.getGcashStats(startDate, endDate);

        if (textGcashFeesProfit != null) {
            textGcashFeesProfit.setText(String.format(Locale.US, "+₱%,.2f", periodStats.totalFeesEarned));
        }
        if (textGcashDrawerFlow != null) {
            textGcashDrawerFlow.setText(String.format(Locale.US, "Cash Drawer: ₱%,.2f", currentDrawerBalance));
        }

        if (textGcashStatCashIn != null) {
            textGcashStatCashIn.setText(String.format(Locale.US, "Cash-In: ₱%,.2f (%d)", periodStats.totalCashInAmount, periodStats.countCashIn));
        }
        if (textGcashStatCashOut != null) {
            textGcashStatCashOut.setText(String.format(Locale.US, "Cash-Out: ₱%,.2f (%d)", periodStats.totalCashOutAmount, periodStats.countCashOut));
        }
        if (textGcashStatFees != null) {
            textGcashStatFees.setText(String.format(Locale.US, "Profit: +₱%,.2f", periodStats.totalFeesEarned));
        }

        String query = (editGcashSearch != null) ? editGcashSearch.getText().toString().trim() : "";
        List<GcashTransaction> transactions = dbHelper.getGcashTransactions(startDate, endDate, selectedGcashFilterType, query);

        if (textGcashEmpty != null) {
            textGcashEmpty.setVisibility(transactions.isEmpty() ? View.VISIBLE : View.GONE);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (GcashTransaction tx : transactions) {
            View itemView = inflater.inflate(R.layout.item_gcash_transaction, layoutGcashItemsContainer, false);

            TextView textSlipNo = itemView.findViewById(R.id.text_gcash_item_slip_no);
            TextView textRefNo = itemView.findViewById(R.id.text_gcash_item_ref_no);
            TextView textDateTime = itemView.findViewById(R.id.text_gcash_item_date_time);
            TextView textTypeBadge = itemView.findViewById(R.id.text_gcash_item_type_badge);
            TextView textCustomerName = itemView.findViewById(R.id.text_gcash_item_customer_name);
            TextView textCustomerNumber = itemView.findViewById(R.id.text_gcash_item_customer_number);
            TextView textNote = itemView.findViewById(R.id.text_gcash_item_note);
            TextView textAmount = itemView.findViewById(R.id.text_gcash_item_amount);
            TextView textFee = itemView.findViewById(R.id.text_gcash_item_fee);
            TextView textCashHandled = itemView.findViewById(R.id.text_gcash_item_cash_handled);
            Button btnPrintSlip = itemView.findViewById(R.id.btn_gcash_item_print_slip);
            Button btnVoid = itemView.findViewById(R.id.btn_gcash_item_void);

            textSlipNo.setText(tx.getRefNo() != null ? tx.getRefNo() : ("GC-" + tx.getId()));

            if (tx.getNote() != null && tx.getNote().startsWith("REF:")) {
                textRefNo.setText(tx.getNote());
            } else {
                textRefNo.setText("");
            }

            textDateTime.setText(tx.getDateTime());

            boolean isVoided = tx.isVoided();
            boolean isCashIn = tx.isCashIn();

            if (isVoided) {
                textTypeBadge.setText("VOIDED");
                textTypeBadge.setBackgroundResource(R.drawable.badge_red);
                textTypeBadge.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeRedText));
                textAmount.setTextColor(ContextCompat.getColor(this, R.color.colorTextMuted));
                btnVoid.setText("🗑️ Delete");
                btnVoid.setOnClickListener(v -> confirmDeleteGcashTransaction(tx));
            } else {
                if (isCashIn) {
                    textTypeBadge.setText("CASH-IN");
                    textTypeBadge.setBackgroundResource(R.drawable.badge_green);
                    textTypeBadge.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeGreenText));
                } else {
                    textTypeBadge.setText("CASH-OUT");
                    textTypeBadge.setBackgroundResource(R.drawable.badge_blue);
                    textTypeBadge.setTextColor(ContextCompat.getColor(this, R.color.colorBadgeBlueText));
                }
                btnVoid.setText("🚫 Void Record");
                btnVoid.setOnClickListener(v -> confirmVoidGcashTransaction(tx));
            }

            String name = (tx.getCustomerName() != null && !tx.getCustomerName().trim().isEmpty())
                    ? tx.getCustomerName().trim() : "Walk-in Customer";
            textCustomerName.setText("Customer: " + name);

            String number = (tx.getCustomerNumber() != null && !tx.getCustomerNumber().trim().isEmpty())
                    ? tx.getCustomerNumber().trim() : "No mobile provided";
            textCustomerNumber.setText("Mobile: " + number);

            if (tx.getNote() != null && !tx.getNote().trim().isEmpty()) {
                textNote.setVisibility(View.VISIBLE);
                textNote.setText("Note: " + tx.getNote().trim());
            } else {
                textNote.setVisibility(View.GONE);
            }

            textAmount.setText(String.format(Locale.US, "₱%,.2f", tx.getAmount()));
            textFee.setText(String.format(Locale.US, "Fee: +₱%,.2f", tx.getFee()));
            textCashHandled.setText(String.format(Locale.US, "Cash: ₱%,.2f", tx.getTotalCashHandled()));

            btnPrintSlip.setOnClickListener(v -> printGcashSlip(tx));

            layoutGcashItemsContainer.addView(itemView);
        }
    }

    private void openRecordGcashDialog(String defaultType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_gcash_transaction, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView textTitle = dialogView.findViewById(R.id.text_dialog_gcash_title);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radiogroup_gcash_type);
        RadioButton radioCashIn = dialogView.findViewById(R.id.radio_gcash_cash_in);
        RadioButton radioCashOut = dialogView.findViewById(R.id.radio_gcash_cash_out);
        EditText editNumber = dialogView.findViewById(R.id.edit_dialog_gcash_number);
        TextView textNumberCounter = dialogView.findViewById(R.id.text_dialog_gcash_number_counter);
        TextView textNumberStatus = dialogView.findViewById(R.id.text_dialog_gcash_number_status);
        EditText editName = dialogView.findViewById(R.id.edit_dialog_gcash_name);
        EditText editAmount = dialogView.findViewById(R.id.edit_dialog_gcash_amount);
        TextView textAmountStatus = dialogView.findViewById(R.id.text_dialog_gcash_amount_status);
        EditText editFee = dialogView.findViewById(R.id.edit_dialog_gcash_fee);
        EditText editRefNo = dialogView.findViewById(R.id.edit_dialog_gcash_ref_no);
        EditText editNote = dialogView.findViewById(R.id.edit_dialog_gcash_note);
        CheckBox checkAutoPrint = dialogView.findViewById(R.id.check_dialog_gcash_autoprint);
        TextView textSummaryTitle = dialogView.findViewById(R.id.text_dialog_gcash_summary_title);
        TextView textSummaryAmount = dialogView.findViewById(R.id.text_dialog_gcash_summary_amount);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_gcash_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_dialog_gcash_save);

        if (GcashTransaction.TYPE_CASH_OUT.equalsIgnoreCase(defaultType)) {
            radioCashOut.setChecked(true);
            textTitle.setText("📱 Record GCash Cash-Out");
        } else {
            radioCashIn.setChecked(true);
            textTitle.setText("📱 Record GCash Cash-In");
        }

        editFee.setText("15.00");

        // Attach strict Philippine mobile number validation & auto-formatting
        DataValidator.attachPhilippineMobileWatcher(editNumber, textNumberStatus, textNumberCounter, null);

        Runnable updateSummary = () -> {
            boolean isCashIn = radioCashIn.isChecked();
            double amt = 0.0;
            double fee = 0.0;
            try {
                String amtStr = editAmount.getText().toString().trim();
                if (!amtStr.isEmpty()) amt = Double.parseDouble(amtStr);
            } catch (Exception ignored) {}
            try {
                String feeStr = editFee.getText().toString().trim();
                if (!feeStr.isEmpty()) fee = Double.parseDouble(feeStr);
            } catch (Exception ignored) {}

            if (isCashIn) {
                textTitle.setText("📱 Record GCash Cash-In");
                textSummaryTitle.setText("💵 Customer Pays (Physical Cash):");
                textSummaryAmount.setText(String.format(Locale.US, "₱%,.2f (₱%,.2f + ₱%,.2f Fee)", (amt + fee), amt, fee));
            } else {
                textTitle.setText("📱 Record GCash Cash-Out");
                textSummaryTitle.setText("💵 Hand to Customer (Physical Cash):");
                textSummaryAmount.setText(String.format(Locale.US, "₱%,.2f", amt));
            }

            // Real-time amount status feedback
            if (textAmountStatus != null) {
                String aStr = editAmount.getText().toString().trim();
                if (aStr.isEmpty()) {
                    textAmountStatus.setText("Remittance amount must be greater than ₱0.00");
                    textAmountStatus.setTextColor(DataValidator.COLOR_MUTED);
                } else {
                    try {
                        double v = Double.parseDouble(aStr);
                        if (v <= 0) {
                            textAmountStatus.setText("⚠️ Amount must be greater than ₱0.00");
                            textAmountStatus.setTextColor(DataValidator.COLOR_ERROR);
                        } else if (v > 50000.0) {
                            textAmountStatus.setText("⚠️ Notice: Amount exceeds standard ₱50k GCash limit");
                            textAmountStatus.setTextColor(DataValidator.COLOR_WARNING);
                        } else {
                            textAmountStatus.setText("✓ Valid remittance amount");
                            textAmountStatus.setTextColor(DataValidator.COLOR_SUCCESS);
                        }
                    } catch (Exception e) {
                        textAmountStatus.setText("⚠️ Invalid numeric amount");
                        textAmountStatus.setTextColor(DataValidator.COLOR_ERROR);
                    }
                }
            }
        };

        radioGroup.setOnCheckedChangeListener((group, id) -> updateSummary.run());

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSummary.run();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        editAmount.addTextChangedListener(watcher);
        editFee.addTextChangedListener(watcher);

        // Quick amount helpers
        dialogView.findViewById(R.id.chip_amt_100).setOnClickListener(v -> { editAmount.setText("100.00"); editFee.setText("10.00"); });
        dialogView.findViewById(R.id.chip_amt_200).setOnClickListener(v -> { editAmount.setText("200.00"); editFee.setText("10.00"); });
        dialogView.findViewById(R.id.chip_amt_500).setOnClickListener(v -> { editAmount.setText("500.00"); editFee.setText("10.00"); });
        dialogView.findViewById(R.id.chip_amt_1000).setOnClickListener(v -> { editAmount.setText("1000.00"); editFee.setText("15.00"); });
        dialogView.findViewById(R.id.chip_amt_2000).setOnClickListener(v -> { editAmount.setText("2000.00"); editFee.setText("25.00"); });
        dialogView.findViewById(R.id.chip_amt_5000).setOnClickListener(v -> { editAmount.setText("5000.00"); editFee.setText("50.00"); });

        // Quick fee helpers
        dialogView.findViewById(R.id.chip_fee_free).setOnClickListener(v -> editFee.setText("0.00"));
        dialogView.findViewById(R.id.chip_fee_10).setOnClickListener(v -> editFee.setText("10.00"));
        dialogView.findViewById(R.id.chip_fee_15).setOnClickListener(v -> editFee.setText("15.00"));
        dialogView.findViewById(R.id.chip_fee_20).setOnClickListener(v -> editFee.setText("20.00"));
        dialogView.findViewById(R.id.chip_fee_30).setOnClickListener(v -> editFee.setText("30.00"));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String type = radioCashIn.isChecked() ? GcashTransaction.TYPE_CASH_IN : GcashTransaction.TYPE_CASH_OUT;
            String rawNumber = editNumber.getText().toString().trim();
            String name = editName.getText().toString().trim();
            String amtStr = editAmount.getText().toString().trim();
            String feeStr = editFee.getText().toString().trim();
            String refNo = editRefNo.getText().toString().trim();
            String note = editNote.getText().toString().trim();

            // DATA INTEGRITY CHECK 1: Cellphone Number Validation
            String normalizedNumber = DataValidator.normalizePhilippineMobile(rawNumber);
            if (normalizedNumber.isEmpty()) {
                new AlertDialog.Builder(this)
                    .setTitle("⚠️ Mobile Number Required")
                    .setMessage("Please enter the customer's 11-digit GCash mobile number (e.g. 09171234567).\n\nA valid mobile number is required to avoid sending remittance to the wrong account.")
                    .setPositiveButton("Enter Number", (d, w) -> editNumber.requestFocus())
                    .show();
                return;
            }

            if (!DataValidator.isValidPhilippineMobile(normalizedNumber)) {
                new AlertDialog.Builder(this)
                    .setTitle("⚠️ Invalid Cellphone Number")
                    .setMessage("The number entered (\"" + rawNumber + "\") is not a valid Philippine cellphone number.\n\n" +
                            "• Philippine mobile numbers must be exactly 11 digits.\n" +
                            "• Must start with '09' (e.g. 09171234567).\n\n" +
                            "Please correct the number before proceeding to protect your transactions.")
                    .setPositiveButton("Correct Number", (d, w) -> editNumber.requestFocus())
                    .show();
                return;
            }

            // DATA INTEGRITY CHECK 2: Amount Validation
            if (amtStr.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                editAmount.requestFocus();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amtStr);
                if (!DataValidator.isValidPositiveAmount(amount)) {
                    Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
                    editAmount.requestFocus();
                    return;
                }
            } catch (Exception e) {
                Toast.makeText(this, "Invalid amount entered", Toast.LENGTH_SHORT).show();
                editAmount.requestFocus();
                return;
            }

            // DATA INTEGRITY CHECK 3: Fee Validation
            double fee = 0.0;
            try {
                if (!feeStr.isEmpty()) {
                    fee = Double.parseDouble(feeStr);
                    if (!DataValidator.isValidFee(fee)) {
                        Toast.makeText(this, "Service fee cannot be negative", Toast.LENGTH_SHORT).show();
                        editFee.requestFocus();
                        return;
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "Invalid fee entered", Toast.LENGTH_SHORT).show();
                editFee.requestFocus();
                return;
            }

            double totalCash = (GcashTransaction.TYPE_CASH_IN.equals(type)) ? (amount + fee) : amount;
            String formattedPhone = DataValidator.formatPhilippineMobile(normalizedNumber);

            // DATA INTEGRITY CHECK 4: Verification Confirmation Modal
            // Prevents unintended clicks and confirms customer mobile number matches
            String confirmTitle = (GcashTransaction.TYPE_CASH_IN.equals(type)) ?
                    "📱 Confirm GCash Cash-In" : "📱 Confirm GCash Cash-Out";

            String confirmMessage = "Please double check the transaction details:\n\n" +
                    "• Customer Mobile: " + formattedPhone + "\n" +
                    (name.isEmpty() ? "" : ("• Customer Name: " + name + "\n")) +
                    "• Principal Amount: ₱" + String.format(Locale.US, "%,.2f", amount) + "\n" +
                    "• Service Fee: ₱" + String.format(Locale.US, "%,.2f", fee) + "\n" +
                    (GcashTransaction.TYPE_CASH_IN.equals(type) ?
                            "• Collect from Customer: ₱" + String.format(Locale.US, "%,.2f", totalCash) :
                            "• Hand to Customer: ₱" + String.format(Locale.US, "%,.2f", totalCash)) + "\n\n" +
                    "⚠️ Ensure the cellphone number matches the customer's GCash account.";

            final double finalAmount = amount;
            final double finalFee = fee;
            final double finalTotalCash = totalCash;

            new AlertDialog.Builder(this)
                .setTitle(confirmTitle)
                .setMessage(confirmMessage)
                .setPositiveButton("Confirm & Record", (confirmDialog, which) -> {
                    SimpleDateFormat dtFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                    SimpleDateFormat dFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Date now = new Date();
                    String dateTime = dtFmt.format(now);
                    String dateOnly = dFmt.format(now);

                    String slipNo = dbHelper.generateNextGcashSlipNo();
                    String finalRef = (refNo.isEmpty()) ? slipNo : refNo;

                    GcashTransaction tx = new GcashTransaction(type, finalRef, normalizedNumber, name, finalAmount, finalFee, finalTotalCash, dateTime, dateOnly, note, GcashTransaction.STATUS_COMPLETED);
                    long id = dbHelper.addGcashTransaction(tx);
                    tx.setId(id);

                    Toast.makeText(this, "✅ " + (tx.isCashIn() ? "Cash-In" : "Cash-Out") + " recorded successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                    renderGcash();

                    if (checkAutoPrint != null && checkAutoPrint.isChecked()) {
                        printGcashSlip(tx);
                    }
                })
                .setNegativeButton("Review / Edit", null)
                .show();
        });

        // Wire Smart Auto-Fill Buttons
        Button btnScanScreen = dialogView.findViewById(R.id.btn_gcash_scan_screen);
        Button btnPasteSms = dialogView.findViewById(R.id.btn_gcash_paste_sms);
        Button btnRecentCustomers = dialogView.findViewById(R.id.btn_gcash_recent_customers);
        View layoutPreview = dialogView.findViewById(R.id.layout_gcash_autofill_preview);
        TextView textBadge = dialogView.findViewById(R.id.text_gcash_autofill_badge);
        TextView textSummary = dialogView.findViewById(R.id.text_gcash_autofill_summary);
        TextView btnDismiss = dialogView.findViewById(R.id.btn_gcash_autofill_dismiss);

        if (btnDismiss != null && layoutPreview != null) {
            btnDismiss.setOnClickListener(v -> layoutPreview.setVisibility(View.GONE));
        }

        if (btnScanScreen != null) {
            btnScanScreen.setOnClickListener(v -> promptGcashCameraOrGallery(dialogView, dialog));
        }

        if (btnPasteSms != null) {
            btnPasteSms.setOnClickListener(v -> handleGcashPasteFromClipboard(editNumber, editName, editAmount, editRefNo, layoutPreview, textBadge, textSummary, updateSummary));
        }

        if (btnRecentCustomers != null) {
            btnRecentCustomers.setOnClickListener(v -> openRecentGcashCustomersDialog(editNumber, editName, updateSummary));
        }

        currentGcashDialogView = dialogView;
        currentGcashDialog = dialog;

        dialog.setOnDismissListener(d -> {
            if (currentGcashDialog == dialog) {
                currentGcashDialog = null;
                currentGcashDialogView = null;
            }
        });

        updateSummary.run();
        dialog.show();
    }

    /*==============================================================================================
    ========================GCASH 100% FOOL-PROOF AUTO-FILL (OCR, QR, CLIPBOARD, RECENT)============
    ==============================================================================================*/

    private void promptGcashCameraOrGallery(View dialogView, AlertDialog dialog) {
        currentGcashDialogView = dialogView;
        currentGcashDialog = dialog;

        String[] options = {"📸 Take Photo of Screen, Note, or QR", "🖼️ Choose Screenshot from Gallery", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("📸 AI Screen / Receipt / QR Scanner")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndLaunchGcash();
                    } else if (which == 1) {
                        launchGalleryPickGcash();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndLaunchGcash() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQUEST);
        } else {
            launchCameraCaptureGcash();
        }
    }

    private void launchCameraCaptureGcash() {
        try {
            File cacheDir = new File(getCacheDir(), "camera");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File photoFile = new File(cacheDir, "gcash_scan_" + System.currentTimeMillis() + ".jpg");
            currentGcashPhotoPath = photoFile.getAbsolutePath();
            currentGcashPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentGcashPhotoUri);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_CAMERA_GCASH_SCAN);
            } else {
                Toast.makeText(this, "No camera application found on device.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error setting up camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGalleryPickGcash() {
        try {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickIntent.setType("image/*");
            startActivityForResult(pickIntent, REQUEST_GALLERY_GCASH_SCAN);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processGcashAutoFillImage(Bitmap bitmap) {
        if (bitmap == null) return;

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("🤖 Analyzing screen / receipt with Google ML Kit & QR Scanner...");
        progress.setCancelable(false);
        progress.show();

        try {
            // Check for QR code first via ZXing
            String qrText = GcashAutoFillParser.decodeQrCode(bitmap);

            // Run OCR text recognition via Google ML Kit
            InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
            TextRecognizer textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            textRecognizer.process(inputImage)
                    .addOnSuccessListener(visionText -> {
                        progress.dismiss();
                        String ocrText = (visionText != null) ? visionText.getText() : "";
                        String combined = (qrText != null ? (qrText + "\n" + ocrText) : ocrText);

                        GcashAutoFillParser.ExtractionResult result = GcashAutoFillParser.parse(combined);
                        if (qrText != null && !result.hasMobile()) {
                            String norm = DataValidator.normalizePhilippineMobile(qrText);
                            if (DataValidator.isValidPhilippineMobile(norm)) {
                                result.mobileNumber = norm;
                                result.formattedMobile = DataValidator.formatPhilippineMobile(norm);
                                result.detectedSourceType = "QR Code";
                            }
                        }

                        if (!result.hasAnyData()) {
                            new AlertDialog.Builder(this)
                                    .setTitle("⚠️ No Details Detected")
                                    .setMessage("Could not detect a mobile number, amount, or QR code in the image.\n\nTips for 100% accuracy:\n• Capture screen/paper directly\n• Ensure good lighting without glare\n• You can also use '📋 Paste SMS'")
                                    .setPositiveButton("OK", null)
                                    .show();
                            return;
                        }

                        showGcashAutoFillConfirmationDialog(result);
                    })
                    .addOnFailureListener(e -> {
                        progress.dismiss();
                        if (qrText != null) {
                            GcashAutoFillParser.ExtractionResult res = GcashAutoFillParser.parse(qrText);
                            if (res.hasAnyData()) {
                                showGcashAutoFillConfirmationDialog(res);
                                return;
                            }
                        }
                        Toast.makeText(this, "AI Analysis error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            progress.dismiss();
            e.printStackTrace();
            Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showGcashAutoFillConfirmationDialog(GcashAutoFillParser.ExtractionResult res) {
        if (currentGcashDialogView == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("✨ Auto-Detected Details (" + res.detectedSourceType + ")");
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Verify or adjust details before applying to transaction:");
        subtitle.setTextSize(11);
        subtitle.setTextColor(ContextCompat.getColor(this, R.color.colorTextMuted));
        subtitle.setPadding(0, 2, 0, 10);
        root.addView(subtitle);

        // Mobile Number
        TextView lblMobile = new TextView(this);
        lblMobile.setText("Customer Mobile Number:");
        lblMobile.setTextSize(11);
        lblMobile.setTypeface(null, Typeface.BOLD);
        lblMobile.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
        root.addView(lblMobile);

        EditText editNum = new EditText(this);
        editNum.setInputType(InputType.TYPE_CLASS_PHONE);
        editNum.setText(res.mobileNumber);
        editNum.setHint("09XXXXXXXXX");
        editNum.setBackgroundResource(R.drawable.edittext_border);
        editNum.setPadding(pad, pad / 2, pad, pad / 2);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (44 * getResources().getDisplayMetrics().density));
        lp1.bottomMargin = 8;
        editNum.setLayoutParams(lp1);
        root.addView(editNum);

        // Amount
        TextView lblAmt = new TextView(this);
        lblAmt.setText("Amount (₱):");
        lblAmt.setTextSize(11);
        lblAmt.setTypeface(null, Typeface.BOLD);
        lblAmt.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
        root.addView(lblAmt);

        EditText editAmt = new EditText(this);
        editAmt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (res.amount > 0) editAmt.setText(String.format(Locale.US, "%.2f", res.amount));
        editAmt.setHint("0.00");
        editAmt.setBackgroundResource(R.drawable.edittext_border);
        editAmt.setPadding(pad, pad / 2, pad, pad / 2);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (44 * getResources().getDisplayMetrics().density));
        lp2.bottomMargin = 8;
        editAmt.setLayoutParams(lp2);
        root.addView(editAmt);

        // Name
        TextView lblName = new TextView(this);
        lblName.setText("Customer Name (Optional):");
        lblName.setTextSize(11);
        lblName.setTypeface(null, Typeface.BOLD);
        lblName.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
        root.addView(lblName);

        EditText editName = new EditText(this);
        editName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editName.setText(res.customerName);
        editName.setHint("Customer Name");
        editName.setBackgroundResource(R.drawable.edittext_border);
        editName.setPadding(pad, pad / 2, pad, pad / 2);
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (44 * getResources().getDisplayMetrics().density));
        lp3.bottomMargin = 8;
        editName.setLayoutParams(lp3);
        root.addView(editName);

        // Ref No
        TextView lblRef = new TextView(this);
        lblRef.setText("Reference Number (Optional):");
        lblRef.setTextSize(11);
        lblRef.setTypeface(null, Typeface.BOLD);
        lblRef.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
        root.addView(lblRef);

        EditText editRef = new EditText(this);
        editRef.setText(res.refNo);
        editRef.setHint("e.g. 100293841029");
        editRef.setBackgroundResource(R.drawable.edittext_border);
        editRef.setPadding(pad, pad / 2, pad, pad / 2);
        LinearLayout.LayoutParams lp4 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (44 * getResources().getDisplayMetrics().density));
        editRef.setLayoutParams(lp4);
        root.addView(editRef);

        builder.setView(root);
        builder.setPositiveButton("✅ Apply to Transaction", (d, w) -> {
            if (currentGcashDialogView != null) {
                EditText targetNumber = currentGcashDialogView.findViewById(R.id.edit_dialog_gcash_number);
                EditText targetName = currentGcashDialogView.findViewById(R.id.edit_dialog_gcash_name);
                EditText targetAmount = currentGcashDialogView.findViewById(R.id.edit_dialog_gcash_amount);
                EditText targetRef = currentGcashDialogView.findViewById(R.id.edit_dialog_gcash_ref_no);
                View layoutPreview = currentGcashDialogView.findViewById(R.id.layout_gcash_autofill_preview);
                TextView textBadge = currentGcashDialogView.findViewById(R.id.text_gcash_autofill_badge);
                TextView textSummary = currentGcashDialogView.findViewById(R.id.text_gcash_autofill_summary);

                String chosenNum = editNum.getText().toString().trim();
                String chosenAmt = editAmt.getText().toString().trim();
                String chosenName = editName.getText().toString().trim();
                String chosenRef = editRef.getText().toString().trim();

                String normNum = DataValidator.normalizePhilippineMobile(chosenNum);
                if (!normNum.isEmpty()) targetNumber.setText(normNum);
                if (!chosenAmt.isEmpty()) targetAmount.setText(chosenAmt);
                if (!chosenName.isEmpty()) targetName.setText(chosenName);
                if (!chosenRef.isEmpty()) targetRef.setText(chosenRef);

                if (layoutPreview != null) {
                    layoutPreview.setVisibility(View.VISIBLE);
                    if (textBadge != null) textBadge.setText("✨ Auto-Filled (" + res.detectedSourceType + ")");
                    if (textSummary != null) {
                        textSummary.setText((normNum.isEmpty() ? "" : ("Mobile: " + DataValidator.formatPhilippineMobile(normNum) + " • ")) +
                                (chosenAmt.isEmpty() ? "" : ("Amount: ₱" + chosenAmt + " • ")) +
                                (chosenName.isEmpty() ? "" : ("Name: " + chosenName)));
                    }
                }

                Toast.makeText(this, "✅ Details applied to transaction form!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void handleGcashPasteFromClipboard(EditText editNumber, EditText editName, EditText editAmount, EditText editRefNo, View layoutPreview, TextView textBadge, TextView textSummary, Runnable updateSummary) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        String clipText = "";
        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
            CharSequence itemText = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (itemText != null) {
                clipText = itemText.toString().trim();
            }
        }

        if (!clipText.isEmpty()) {
            GcashAutoFillParser.ExtractionResult res = GcashAutoFillParser.parse(clipText);
            if (res.hasAnyData()) {
                showGcashAutoFillConfirmationDialog(res);
                return;
            }
        }

        promptManualPasteDialog(editNumber, editName, editAmount, editRefNo, layoutPreview, textBadge, textSummary, updateSummary);
    }

    private void promptManualPasteDialog(EditText editNumber, EditText editName, EditText editAmount, EditText editRefNo, View layoutPreview, TextView textBadge, TextView textSummary, Runnable updateSummary) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("📋 Paste GCash SMS or Text");
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Paste any GCash SMS alert, Messenger chat, or phone number below:");
        subtitle.setTextSize(11);
        subtitle.setTextColor(ContextCompat.getColor(this, R.color.colorTextMuted));
        subtitle.setPadding(0, 2, 0, 10);
        root.addView(subtitle);

        EditText editPaste = new EditText(this);
        editPaste.setHint("e.g. You have received PHP 500.00 of GCash from 09171234567 with Ref No...");
        editPaste.setMinLines(3);
        editPaste.setGravity(Gravity.TOP | Gravity.START);
        editPaste.setBackgroundResource(R.drawable.edittext_border);
        editPaste.setPadding(pad, pad, pad, pad);
        root.addView(editPaste);

        builder.setView(root);
        builder.setPositiveButton("⚡ Auto-Detect & Fill", (d, w) -> {
            String text = editPaste.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Please paste or enter text first.", Toast.LENGTH_SHORT).show();
                return;
            }
            GcashAutoFillParser.ExtractionResult res = GcashAutoFillParser.parse(text);
            if (!res.hasAnyData()) {
                Toast.makeText(this, "Could not detect a valid mobile number or amount.", Toast.LENGTH_SHORT).show();
                return;
            }
            showGcashAutoFillConfirmationDialog(res);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openRecentGcashCustomersDialog(EditText editNumber, EditText editName, Runnable updateSummary) {
        List<DatabaseHelper.GcashCustomerRecord> customers = dbHelper.getFrequentGcashCustomers(35);
        if (customers.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("👥 Recent Customers")
                    .setMessage("No past GCash customer transactions recorded yet. As you record transactions, frequent customers will automatically appear here for 1-tap auto-fill.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (14 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("👥 Select Recent Customer");
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Tap a neighbor/customer to auto-fill their mobile number and name:");
        subtitle.setTextSize(11);
        subtitle.setTextColor(ContextCompat.getColor(this, R.color.colorTextMuted));
        subtitle.setPadding(0, 2, 0, 8);
        root.addView(subtitle);

        EditText searchEdit = new EditText(this);
        searchEdit.setHint("🔍 Search name or number...");
        searchEdit.setTextSize(13);
        searchEdit.setBackgroundResource(R.drawable.edittext_border);
        searchEdit.setPadding(pad, pad / 2, pad, pad / 2);
        root.addView(searchEdit);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (320 * getResources().getDisplayMetrics().density)));
        scroll.setPadding(0, 8, 0, 0);

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);
        root.addView(scroll);

        builder.setView(root);
        builder.setNegativeButton("Cancel", null);
        AlertDialog recentDialog = builder.create();

        Runnable renderList = () -> {
            listContainer.removeAllViews();
            String query = searchEdit.getText().toString().trim().toLowerCase(Locale.US);

            for (DatabaseHelper.GcashCustomerRecord c : customers) {
                String name = c.customerName != null ? c.customerName : "";
                String num = c.customerNumber != null ? c.customerNumber : "";
                if (!query.isEmpty() && !name.toLowerCase(Locale.US).contains(query) && !num.contains(query)) {
                    continue;
                }

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(pad, pad, pad, pad);
                row.setBackgroundResource(R.drawable.card_background);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = 6;
                row.setLayoutParams(lp);
                row.setClickable(true);
                row.setFocusable(true);

                LinearLayout textCol = new LinearLayout(this);
                textCol.setOrientation(LinearLayout.VERTICAL);
                textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

                TextView textCustName = new TextView(this);
                textCustName.setText(name.isEmpty() ? ("Customer (" + DataValidator.formatPhilippineMobile(num) + ")") : name);
                textCustName.setTextSize(13);
                textCustName.setTypeface(null, Typeface.BOLD);
                textCustName.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
                textCol.addView(textCustName);

                TextView textCustNum = new TextView(this);
                textCustNum.setText(DataValidator.formatPhilippineMobile(num) + " • " + c.transactionCount + " transaction" + (c.transactionCount > 1 ? "s" : ""));
                textCustNum.setTextSize(11);
                textCustNum.setTextColor(ContextCompat.getColor(this, R.color.colorTextMuted));
                textCol.addView(textCustNum);

                row.addView(textCol);

                TextView arrow = new TextView(this);
                arrow.setText("Select ➔");
                arrow.setTextSize(11);
                arrow.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
                arrow.setTypeface(null, Typeface.BOLD);
                row.addView(arrow);

                row.setOnClickListener(v -> {
                    editNumber.setText(num);
                    if (!name.isEmpty()) editName.setText(name);
                    recentDialog.dismiss();
                    Toast.makeText(this, "✅ Selected: " + DataValidator.formatPhilippineMobile(num), Toast.LENGTH_SHORT).show();
                    if (updateSummary != null) updateSummary.run();
                });

                listContainer.addView(row);
            }
        };

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderList.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        renderList.run();
        recentDialog.show();
    }

    private void confirmVoidGcashTransaction(GcashTransaction tx) {
        requireOwnerPin("Void GCash Transaction", () -> {
            new AlertDialog.Builder(this)
                .setTitle("🚫 Void GCash Transaction: " + tx.getRefNo())
                .setMessage("Are you sure you want to void this " + (tx.isCashIn() ? "Cash-In" : "Cash-Out") +
                        " transaction of ₱" + String.format(Locale.US, "%.2f", tx.getAmount()) + "?\n\nThis will reverse its effect on your GCash float and drawer totals.")
                .setPositiveButton("Void Transaction", (d, w) -> {
                    boolean ok = dbHelper.voidGcashTransaction(tx.getId());
                    if (ok) {
                        Toast.makeText(this, "Transaction voided.", Toast.LENGTH_SHORT).show();
                        renderGcash();
                    } else {
                        Toast.makeText(this, "Failed to void transaction.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void confirmDeleteGcashTransaction(GcashTransaction tx) {
        requireOwnerPin("Delete Voided GCash Record", () -> {
            new AlertDialog.Builder(this)
                .setTitle("🗑️ Delete Voided GCash Record")
                .setMessage("Permanently remove this record (" + tx.getRefNo() + ") from the database?")
                .setPositiveButton("Delete Permanently", (d, w) -> {
                    boolean ok = dbHelper.deleteGcashTransaction(tx.getId());
                    if (ok) {
                        Toast.makeText(this, "Record deleted.", Toast.LENGTH_SHORT).show();
                        renderGcash();
                    } else {
                        Toast.makeText(this, "Failed to delete record.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void openAdjustGcashFloatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        TextView labelFloat = new TextView(this);
        labelFloat.setText("Starting / Base GCash Digital Float (₱):");
        labelFloat.setTextSize(12);
        labelFloat.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
        container.addView(labelFloat);

        final EditText editFloat = new EditText(this);
        editFloat.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editFloat.setText(String.format(Locale.US, "%.2f", getGcashStartingFloat()));
        editFloat.setBackgroundResource(R.drawable.edittext_border);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * getResources().getDisplayMetrics().density));
        lp1.topMargin = 4;
        lp1.bottomMargin = (int) (12 * getResources().getDisplayMetrics().density);
        editFloat.setLayoutParams(lp1);
        container.addView(editFloat);

        TextView labelDrawer = new TextView(this);
        labelDrawer.setText("Starting Physical Cash Drawer Float (₱):");
        labelDrawer.setTextSize(12);
        labelDrawer.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
        container.addView(labelDrawer);

        final EditText editDrawer = new EditText(this);
        editDrawer.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editDrawer.setText(String.format(Locale.US, "%.2f", getGcashDrawerFloat()));
        editDrawer.setBackgroundResource(R.drawable.edittext_border);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * getResources().getDisplayMetrics().density));
        lp2.topMargin = 4;
        editDrawer.setLayoutParams(lp2);
        container.addView(editDrawer);

        builder.setTitle("💰 Set Starting GCash Floats");
        builder.setView(container);
        builder.setPositiveButton("Save", (d, w) -> {
            try {
                double fVal = Double.parseDouble(editFloat.getText().toString().trim());
                double dVal = Double.parseDouble(editDrawer.getText().toString().trim());
                if (fVal < 0 || dVal < 0) {
                    Toast.makeText(this, "Float balances cannot be negative.", Toast.LENGTH_SHORT).show();
                    return;
                }
                setGcashStartingFloat(fVal);
                setGcashDrawerFloat(dVal);
                Toast.makeText(this, "Float balances updated!", Toast.LENGTH_SHORT).show();
                renderGcash();
            } catch (Exception e) {
                Toast.makeText(this, "Please enter valid numeric amounts.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openManageGcashHistoryDialog() {
        String[] options = {
            "🗑️ Delete All Voided GCash Records",
            "🧹 Clear GCash Records for Current Period",
            "⚠️ Reset Entire GCash Ledger"
        };

        new AlertDialog.Builder(this)
            .setTitle("Manage GCash Ledger")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    new AlertDialog.Builder(this)
                        .setTitle("Delete Voided Records")
                        .setMessage("Are you sure you want to delete all voided GCash records?")
                        .setPositiveButton("Confirm", (d, w) -> {
                            int count = dbHelper.deleteAllVoidedGcashTransactions();
                            Toast.makeText(this, "Deleted " + count + " voided record(s).", Toast.LENGTH_SHORT).show();
                            renderGcash();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                } else if (which == 1) {
                    String[] dates = getGcashPeriodDateRange();
                    new AlertDialog.Builder(this)
                        .setTitle("Clear GCash Records")
                        .setMessage("Delete all GCash records for the selected period?")
                        .setPositiveButton("Confirm Clear", (d, w) -> {
                            int count = dbHelper.clearGcashHistory(dates[0], dates[1]);
                            Toast.makeText(this, "Cleared " + count + " record(s).", Toast.LENGTH_SHORT).show();
                            renderGcash();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                } else if (which == 2) {
                    new AlertDialog.Builder(this)
                        .setTitle("⚠️ Reset Entire GCash Ledger")
                        .setMessage("WARNING: This will permanently delete ALL recorded GCash cash-in and cash-out transactions across all time.\n\nAre you sure?")
                        .setPositiveButton("Yes, Reset Everything", (d, w) -> {
                            int count = dbHelper.clearGcashHistory(null, null);
                            Toast.makeText(this, "Entire GCash ledger cleared (" + count + " records).", Toast.LENGTH_LONG).show();
                            renderGcash();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }

    public void printGcashSlip(GcashTransaction tx) {
        this.checkBluetoothPermissions(() -> {
            DeviceConnection connection = selectedBluetoothDevice;
            if (connection == null) {
                connection = BluetoothPrintersConnections.selectFirstPaired();
            }
            if (connection == null) {
                Toast.makeText(this, "No Bluetooth thermal printer connected. Pair printer first.", Toast.LENGTH_SHORT).show();
                return;
            }

            final DeviceConnection targetConn = connection;
            new AsyncBluetoothEscPosPrint(
                this,
                new AsyncEscPosPrint.OnPrintFinished() {
                    @Override
                    public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                        Log.e("GcashPrint", "Print error: " + codeException);
                        Toast.makeText(MainActivity.this, "Unable to print slip. Check printer power & Bluetooth.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                        Toast.makeText(MainActivity.this, "GCash slip printed successfully!", Toast.LENGTH_SHORT).show();
                    }
                }
            ).execute(getGcashSlipPrinter(targetConn, tx));
        });
    }

    public AsyncEscPosPrinter getGcashSlipPrinter(DeviceConnection printerConnection, GcashTransaction tx) {
        final int W = receiptDesign.charactersPerLine > 0 ? receiptDesign.charactersPerLine : 32;
        float widthMm = receiptDesign.paperWidthMm == 80 ? 72f : 48f;
        String divider = configDivider(receiptDesign.dividerStyle, W);

        StringBuilder sb = new StringBuilder();
        if (receiptDesign.isHeaderBig) {
            sb.append("[C]<font size='big'><b>").append(receiptDesign.storeName).append("</b></font>\n");
        } else {
            sb.append("[C]<b>").append(receiptDesign.storeName).append("</b>\n");
        }
        if (!receiptDesign.subheader.isEmpty()) {
            sb.append("[C]").append(receiptDesign.subheader).append("\n");
        }
        sb.append("[C]").append(divider).append("\n");
        String title = tx.isCashIn() ? "GCASH CASH-IN SLIP" : "GCASH CASH-OUT SLIP";
        sb.append("[C]<b>").append(title).append("</b>\n");
        sb.append("[C]").append(divider).append("\n");

        sb.append("[L]Slip #: [R]").append(tx.getRefNo()).append("\n");
        sb.append("[L]Date: [R]").append(tx.getDateTime()).append("\n");
        if (tx.getCustomerName() != null && !tx.getCustomerName().trim().isEmpty()) {
            sb.append("[L]Customer: [R]").append(tx.getCustomerName().trim()).append("\n");
        }
        if (tx.getCustomerNumber() != null && !tx.getCustomerNumber().trim().isEmpty()) {
            sb.append("[L]GCash No: [R]").append(tx.getCustomerNumber().trim()).append("\n");
        }
        sb.append("[C]").append(configDivider("-", W)).append("\n");

        sb.append("[L]").append(tx.isCashIn() ? "Amount to Send:" : "Amount Received:").append("[R]₱")
                .append(String.format(Locale.US, "%,.2f", tx.getAmount())).append("\n");
        sb.append("[L]Service Fee:[R]₱")
                .append(String.format(Locale.US, "%,.2f", tx.getFee())).append("\n");
        sb.append("[C]").append(divider).append("\n");

        if (tx.isCashIn()) {
            sb.append("[L]<b>TOTAL CASH PAID:</b>[R]<b>₱")
                    .append(String.format(Locale.US, "%,.2f", tx.getTotalCashHandled())).append("</b>\n");
        } else {
            sb.append("[L]<b>CASH HANDED OUT:</b>[R]<b>₱")
                    .append(String.format(Locale.US, "%,.2f", tx.getTotalCashHandled())).append("</b>\n");
        }
        sb.append("[C]").append(divider).append("\n");

        if (tx.getNote() != null && !tx.getNote().trim().isEmpty()) {
            sb.append("[L]Note: ").append(tx.getNote().trim()).append("\n");
        }

        sb.append("[C]Thank you for your transaction!\n");
        sb.append("[C]Please keep this slip as proof.\n\n\n");

        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, widthMm, W);
        return printer.addTextToPrint(sb.toString());
    }

    private void exportGcashToCsv() {
        ProgressDialog progressDialog = ProgressDialog.show(this, "Exporting GCash Ledger", "Generating CSV file...", true, false);
        new Thread(() -> {
            try {
                List<GcashTransaction> list = dbHelper.getGcashTransactions(null, null, "ALL", null);
                SimpleDateFormat fileDate = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
                String timestamp = fileDate.format(new Date());

                StringBuilder csv = new StringBuilder();
                csv.append("Slip No,Date Time,Date,Type,Customer Mobile,Customer Name,Amount,Service Fee,Total Cash Handled,Status,Note\n");

                double totalIn = 0.0;
                double totalOut = 0.0;
                double totalFees = 0.0;

                for (GcashTransaction tx : list) {
                    if (!tx.isVoided()) {
                        if (tx.isCashIn()) totalIn += tx.getAmount();
                        else totalOut += tx.getAmount();
                        totalFees += tx.getFee();
                    }

                    csv.append(escapeCsv(tx.getRefNo())).append(",")
                       .append(escapeCsv(tx.getDateTime())).append(",")
                       .append(escapeCsv(tx.getDateOnly())).append(",")
                       .append(escapeCsv(tx.getType())).append(",")
                       .append(escapeCsv(tx.getCustomerNumber())).append(",")
                       .append(escapeCsv(tx.getCustomerName())).append(",")
                       .append(String.format(Locale.US, "%.2f", tx.getAmount())).append(",")
                       .append(String.format(Locale.US, "%.2f", tx.getFee())).append(",")
                       .append(String.format(Locale.US, "%.2f", tx.getTotalCashHandled())).append(",")
                       .append(escapeCsv(tx.getStatus())).append(",")
                       .append(escapeCsv(tx.getNote())).append("\n");
                }

                csv.append("\n");
                csv.append("TOTALS,").append(list.size()).append(" records,,,Cash-In: ")
                   .append(String.format(Locale.US, "%.2f", totalIn)).append(",Cash-Out: ")
                   .append(String.format(Locale.US, "%.2f", totalOut)).append(",Total Fees (Profit): ")
                   .append(String.format(Locale.US, "%.2f", totalFees)).append(",,,\n");

                String filename = "morps_gcash_ledger_" + timestamp + ".csv";
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    shareCsvFile(filename, csv.toString(), "Morps POS - GCash Ledger (" + list.size() + " records)");
                });
            } catch (Exception e) {
                Log.e("MorpsPOS", "Error exporting GCash CSV", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
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
        TextView textContactStatus = dialogView.findViewById(R.id.text_design_contact_status);
        EditText editReceiptTitle = dialogView.findViewById(R.id.edit_design_receipt_title);
        EditText editReceiptNoLabel = dialogView.findViewById(R.id.edit_design_receipt_no_label);
        EditText editReceiptPrefix = dialogView.findViewById(R.id.edit_design_receipt_prefix);
        RadioGroup radioGroupHeaderSize = dialogView.findViewById(R.id.radiogroup_header_size);
        RadioGroup radioGroupDivider = dialogView.findViewById(R.id.radiogroup_divider);
        EditText editFooter1 = dialogView.findViewById(R.id.edit_design_footer_1);
        EditText editFooter2 = dialogView.findViewById(R.id.edit_design_footer_2);
        EditText editPolicy = dialogView.findViewById(R.id.edit_design_policy);
        CheckBox checkShowDate = dialogView.findViewById(R.id.check_design_show_date);
        CheckBox checkShowReceiptNo = dialogView.findViewById(R.id.check_design_show_receipt_no);
        CheckBox checkEnableGcash = dialogView.findViewById(R.id.check_design_enable_gcash);
        CheckBox checkEnableStoreCredit = dialogView.findViewById(R.id.check_design_enable_store_credit);
        CheckBox checkPromptPrint = dialogView.findViewById(R.id.check_design_prompt_print);
        CheckBox checkAutoPrint = dialogView.findViewById(R.id.check_design_autoprint);
        TextView textLivePreview = dialogView.findViewById(R.id.text_design_live_preview);

        // Section 4: Paper Width (58mm vs 80mm)
        RadioGroup radioGroupPaperWidth = dialogView.findViewById(R.id.radiogroup_paper_width);
        RadioButton radioPaper58mm = dialogView.findViewById(R.id.radio_paper_58mm);
        RadioButton radioPaper80mm = dialogView.findViewById(R.id.radio_paper_80mm);
        if (receiptDesign.paperWidthMm == 80) {
            if (radioPaper80mm != null) radioPaper80mm.setChecked(true);
        } else {
            if (radioPaper58mm != null) radioPaper58mm.setChecked(true);
        }

        // Section 5: Custom GCash QR Code on Receipt
        ImageView imageGcashQrPreview = dialogView.findViewById(R.id.image_design_gcash_qr_preview);
        Button btnUploadGcashQr = dialogView.findViewById(R.id.btn_design_upload_gcash_qr);
        Button btnRemoveGcashQr = dialogView.findViewById(R.id.btn_design_remove_gcash_qr);
        CheckBox checkEnableGcashQr = dialogView.findViewById(R.id.check_design_enable_gcash_qr);
        RadioGroup radioGroupGcashQrMode = dialogView.findViewById(R.id.radiogroup_gcash_qr_mode);
        RadioButton radioGcashQrModeAlways = dialogView.findViewById(R.id.radio_gcash_qr_mode_always);
        RadioButton radioGcashQrModeGcashOnly = dialogView.findViewById(R.id.radio_gcash_qr_mode_gcash_only);

        currentSettingsQrPreview = imageGcashQrPreview;
        currentSettingsRemoveQrBtn = btnRemoveGcashQr;
        currentSettingsDialog = dialog;

        if (receiptDesign.hasGcashQrImage(this)) {
            Bitmap qrBmp = loadGcashQrBitmap();
            if (qrBmp != null && imageGcashQrPreview != null) {
                imageGcashQrPreview.setImageBitmap(qrBmp);
            }
            if (btnRemoveGcashQr != null) btnRemoveGcashQr.setVisibility(View.VISIBLE);
        } else {
            if (imageGcashQrPreview != null) imageGcashQrPreview.setImageResource(android.R.drawable.ic_menu_gallery);
            if (btnRemoveGcashQr != null) btnRemoveGcashQr.setVisibility(View.GONE);
        }

        if (checkEnableGcashQr != null) {
            checkEnableGcashQr.setChecked(receiptDesign.enableGcashQrOnReceipt);
        }

        if ("GCASH_ONLY".equalsIgnoreCase(receiptDesign.gcashQrPrintMode)) {
            if (radioGcashQrModeGcashOnly != null) radioGcashQrModeGcashOnly.setChecked(true);
        } else {
            if (radioGcashQrModeAlways != null) radioGcashQrModeAlways.setChecked(true);
        }

        if (btnUploadGcashQr != null) {
            btnUploadGcashQr.setOnClickListener(v -> {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                try {
                    startActivityForResult(pickIntent, REQUEST_PICK_GCASH_QR);
                } catch (Exception e) {
                    Intent getPhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    getPhotoIntent.setType("image/*");
                    startActivityForResult(Intent.createChooser(getPhotoIntent, "Select GCash QR Image"), REQUEST_PICK_GCASH_QR);
                }
            });
        }

        if (btnRemoveGcashQr != null) {
            btnRemoveGcashQr.setOnClickListener(v -> {
                File qrFile = new File(getFilesDir(), ReceiptDesignConfig.GCASH_QR_FILENAME);
                if (qrFile.exists()) {
                    qrFile.delete();
                }
                receiptDesign.enableGcashQrOnReceipt = false;
                receiptDesign.save(this);
                if (checkEnableGcashQr != null) checkEnableGcashQr.setChecked(false);
                if (imageGcashQrPreview != null) imageGcashQrPreview.setImageResource(android.R.drawable.ic_menu_gallery);
                btnRemoveGcashQr.setVisibility(View.GONE);
                Toast.makeText(this, "GCash QR code removed.", Toast.LENGTH_SHORT).show();
            });
        }

        // Section 6: Data Safety & Offline DB Backup / Restore / CSV Export
        Button btnBackupDb = dialogView.findViewById(R.id.btn_settings_backup_db);
        Button btnRestoreDb = dialogView.findViewById(R.id.btn_settings_restore_db);
        Button btnExportSalesCsv = dialogView.findViewById(R.id.btn_settings_export_sales_csv);
        Button btnExportInvCsv = dialogView.findViewById(R.id.btn_settings_export_inv_csv);

        if (btnBackupDb != null) {
            btnBackupDb.setOnClickListener(v -> backupDatabase());
        }
        if (btnRestoreDb != null) {
            btnRestoreDb.setOnClickListener(v -> launchRestoreDatabasePicker());
        }
        if (btnExportSalesCsv != null) {
            btnExportSalesCsv.setOnClickListener(v -> exportSalesToCsv());
        }
        if (btnExportInvCsv != null) {
            btnExportInvCsv.setOnClickListener(v -> exportInventoryToCsv());
        }

        // Section 7: Store Security & Cashier PIN Lock
        CheckBox checkEnablePin = dialogView.findViewById(R.id.check_enable_pin_security);
        Button btnChangePin = dialogView.findViewById(R.id.btn_change_owner_pin);
        if (checkEnablePin != null) {
            checkEnablePin.setChecked(receiptDesign.enablePinSecurity);
        }
        if (btnChangePin != null) {
            btnChangePin.setOnClickListener(v -> openChangePinDialog());
        }

        // Section 8: App Version & Updates
        TextView textAppVersion = dialogView.findViewById(R.id.text_app_version_label);
        if (textAppVersion != null) {
            textAppVersion.setText("Version " + getCurrentAppVersionName() + " (Build " + getCurrentAppVersionCode() + ")");
        }
        Button btnCheckOnlineUpdate = dialogView.findViewById(R.id.btn_check_online_update);
        if (btnCheckOnlineUpdate != null) {
            btnCheckOnlineUpdate.setOnClickListener(v -> checkForOnlineUpdate());
        }
        Button btnInstallOfflineApk = dialogView.findViewById(R.id.btn_install_offline_apk);
        if (btnInstallOfflineApk != null) {
            btnInstallOfflineApk.setOnClickListener(v -> pickLocalApkForInstall());
        }
        Button btnConfigUpdateUrl = dialogView.findViewById(R.id.btn_configure_update_url);
        if (btnConfigUpdateUrl != null) {
            btnConfigUpdateUrl.setOnClickListener(v -> openConfigureUpdateUrlDialog());
        }

        // Pre-fill with current config
        editStoreName.setText(receiptDesign.storeName);
        editSubheader.setText(receiptDesign.subheader);
        editAddress.setText(receiptDesign.address);
        editContact.setText(receiptDesign.contact);
        editReceiptTitle.setText(receiptDesign.receiptTitle);
        if (editReceiptNoLabel != null) editReceiptNoLabel.setText(receiptDesign.receiptNoLabel);
        if (editReceiptPrefix != null) editReceiptPrefix.setText(receiptDesign.receiptPrefix);
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
        if (checkEnableGcash != null) checkEnableGcash.setChecked(receiptDesign.enableGcash);
        if (checkEnableStoreCredit != null) checkEnableStoreCredit.setChecked(receiptDesign.enableStoreCredit);
        if (checkPromptPrint != null) checkPromptPrint.setChecked(receiptDesign.promptToPrint);
        if (checkAutoPrint != null) checkAutoPrint.setChecked(receiptDesign.autoPrintOnCheckout);

        // Quick chip helpers for Receipt # Label
        View chipLabelReceipt = dialogView.findViewById(R.id.chip_label_receipt);
        if (chipLabelReceipt != null && editReceiptNoLabel != null) {
            chipLabelReceipt.setOnClickListener(v -> editReceiptNoLabel.setText("Receipt #:"));
        }
        View chipLabelInvoice = dialogView.findViewById(R.id.chip_label_invoice);
        if (chipLabelInvoice != null && editReceiptNoLabel != null) {
            chipLabelInvoice.setOnClickListener(v -> editReceiptNoLabel.setText("Invoice #:"));
        }
        View chipLabelOr = dialogView.findViewById(R.id.chip_label_or);
        if (chipLabelOr != null && editReceiptNoLabel != null) {
            chipLabelOr.setOnClickListener(v -> editReceiptNoLabel.setText("OR #:"));
        }
        View chipLabelOrder = dialogView.findViewById(R.id.chip_label_order);
        if (chipLabelOrder != null && editReceiptNoLabel != null) {
            chipLabelOrder.setOnClickListener(v -> editReceiptNoLabel.setText("Order #:"));
        }
        View chipLabelSlip = dialogView.findViewById(R.id.chip_label_slip);
        if (chipLabelSlip != null && editReceiptNoLabel != null) {
            chipLabelSlip.setOnClickListener(v -> editReceiptNoLabel.setText("Slip #:"));
        }

        // Quick chip helpers for Receipt Prefix
        View chipPrefixMorps = dialogView.findViewById(R.id.chip_prefix_morps);
        if (chipPrefixMorps != null && editReceiptPrefix != null) {
            chipPrefixMorps.setOnClickListener(v -> editReceiptPrefix.setText("MORPS-"));
        }
        View chipPrefixInv = dialogView.findViewById(R.id.chip_prefix_inv);
        if (chipPrefixInv != null && editReceiptPrefix != null) {
            chipPrefixInv.setOnClickListener(v -> editReceiptPrefix.setText("INV-"));
        }
        View chipPrefixOr = dialogView.findViewById(R.id.chip_prefix_or);
        if (chipPrefixOr != null && editReceiptPrefix != null) {
            chipPrefixOr.setOnClickListener(v -> editReceiptPrefix.setText("OR-"));
        }
        View chipPrefixHash = dialogView.findViewById(R.id.chip_prefix_hash);
        if (chipPrefixHash != null && editReceiptPrefix != null) {
            chipPrefixHash.setOnClickListener(v -> editReceiptPrefix.setText("#"));
        }
        View chipPrefixNone = dialogView.findViewById(R.id.chip_prefix_none);
        if (chipPrefixNone != null && editReceiptPrefix != null) {
            chipPrefixNone.setOnClickListener(v -> editReceiptPrefix.setText(""));
        }

        // Live preview updater callback
        Runnable updateLivePreview = () -> {
            ReceiptDesignConfig temp = new ReceiptDesignConfig();
            temp.storeName = editStoreName.getText().toString().trim();
            temp.subheader = editSubheader.getText().toString().trim();
            temp.address = editAddress.getText().toString().trim();
            temp.contact = editContact.getText().toString().trim();
            temp.receiptTitle = editReceiptTitle.getText().toString().trim();
            if (editReceiptNoLabel != null) temp.receiptNoLabel = editReceiptNoLabel.getText().toString();
            if (editReceiptPrefix != null) temp.receiptPrefix = editReceiptPrefix.getText().toString();
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

            if (radioGroupPaperWidth != null && radioGroupPaperWidth.getCheckedRadioButtonId() == R.id.radio_paper_80mm) {
                temp.paperWidthMm = 80;
                temp.charactersPerLine = 48;
            } else {
                temp.paperWidthMm = 58;
                temp.charactersPerLine = 32;
            }

            if (checkEnableGcashQr != null) {
                temp.enableGcashQrOnReceipt = checkEnableGcashQr.isChecked();
            }

            if (radioGroupGcashQrMode != null && radioGroupGcashQrMode.getCheckedRadioButtonId() == R.id.radio_gcash_qr_mode_gcash_only) {
                temp.gcashQrPrintMode = "GCASH_ONLY";
            } else {
                temp.gcashQrPrintMode = "ALWAYS";
            }

            textLivePreview.setText(generatePreviewText(temp, true));

            if (textContactStatus != null) {
                String cText = editContact.getText().toString().trim();
                if (cText.isEmpty()) {
                    textContactStatus.setText("Printed on receipt header (e.g. 0917-123-4567 or landline)");
                    textContactStatus.setTextColor(DataValidator.COLOR_MUTED);
                } else {
                    String norm = DataValidator.normalizePhilippineMobile(cText);
                    if (norm.startsWith("09")) {
                        DataValidator.ValidationStatus st = DataValidator.evaluateMobile(norm);
                        if (st.isValid) {
                            textContactStatus.setText("✓ Valid Philippine Mobile (" + DataValidator.formatPhilippineMobile(norm) + ")");
                            textContactStatus.setTextColor(DataValidator.COLOR_SUCCESS);
                        } else {
                            textContactStatus.setText(st.message);
                            textContactStatus.setTextColor(st.color);
                        }
                    } else {
                        textContactStatus.setText("Custom store contact info");
                        textContactStatus.setTextColor(DataValidator.COLOR_MUTED);
                    }
                }
            }
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
        if (editReceiptNoLabel != null) editReceiptNoLabel.addTextChangedListener(watcher);
        if (editReceiptPrefix != null) editReceiptPrefix.addTextChangedListener(watcher);
        editFooter1.addTextChangedListener(watcher);
        editFooter2.addTextChangedListener(watcher);
        editPolicy.addTextChangedListener(watcher);

        radioGroupHeaderSize.setOnCheckedChangeListener((group, id) -> updateLivePreview.run());
        radioGroupDivider.setOnCheckedChangeListener((group, id) -> updateLivePreview.run());
        checkShowDate.setOnCheckedChangeListener((buttonView, isChecked) -> updateLivePreview.run());
        checkShowReceiptNo.setOnCheckedChangeListener((buttonView, isChecked) -> updateLivePreview.run());
        if (radioGroupPaperWidth != null) {
            radioGroupPaperWidth.setOnCheckedChangeListener((group, id) -> updateLivePreview.run());
        }
        if (checkEnableGcashQr != null) {
            checkEnableGcashQr.setOnCheckedChangeListener((buttonView, isChecked) -> updateLivePreview.run());
        }
        if (radioGroupGcashQrMode != null) {
            radioGroupGcashQrMode.setOnCheckedChangeListener((group, id) -> updateLivePreview.run());
        }

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

            if (editReceiptNoLabel != null) {
                receiptDesign.receiptNoLabel = editReceiptNoLabel.getText().toString().trim();
                if (receiptDesign.receiptNoLabel.isEmpty()) receiptDesign.receiptNoLabel = "Receipt #:";
            }
            if (editReceiptPrefix != null) {
                receiptDesign.receiptPrefix = editReceiptPrefix.getText().toString().trim();
            }

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
            if (checkEnableGcash != null) receiptDesign.enableGcash = checkEnableGcash.isChecked();
            if (checkEnableStoreCredit != null) receiptDesign.enableStoreCredit = checkEnableStoreCredit.isChecked();
            if (checkPromptPrint != null) receiptDesign.promptToPrint = checkPromptPrint.isChecked();
            if (checkAutoPrint != null) receiptDesign.autoPrintOnCheckout = checkAutoPrint.isChecked();

            if (radioGroupPaperWidth != null && radioGroupPaperWidth.getCheckedRadioButtonId() == R.id.radio_paper_80mm) {
                receiptDesign.paperWidthMm = 80;
                receiptDesign.charactersPerLine = 48;
            } else {
                receiptDesign.paperWidthMm = 58;
                receiptDesign.charactersPerLine = 32;
            }

            if (checkEnableGcashQr != null) {
                receiptDesign.enableGcashQrOnReceipt = checkEnableGcashQr.isChecked();
            }

            if (radioGroupGcashQrMode != null && radioGroupGcashQrMode.getCheckedRadioButtonId() == R.id.radio_gcash_qr_mode_gcash_only) {
                receiptDesign.gcashQrPrintMode = "GCASH_ONLY";
            } else {
                receiptDesign.gcashQrPrintMode = "ALWAYS";
            }

            if (checkEnablePin != null) {
                receiptDesign.enablePinSecurity = checkEnablePin.isChecked();
            }

            receiptDesign.save(this);
            applyStoreDesignToUI();
            Toast.makeText(this, "Settings saved successfully! Main menu updated.", Toast.LENGTH_SHORT).show();
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
            if (editReceiptNoLabel != null) editReceiptNoLabel.setText(receiptDesign.receiptNoLabel);
            if (editReceiptPrefix != null) editReceiptPrefix.setText(receiptDesign.receiptPrefix);
            ((RadioButton) dialogView.findViewById(R.id.radio_header_big)).setChecked(true);
            ((RadioButton) dialogView.findViewById(R.id.radio_divider_double)).setChecked(true);
            editFooter1.setText(receiptDesign.footer1);
            editFooter2.setText(receiptDesign.footer2);
            editPolicy.setText(receiptDesign.policy);
            checkShowDate.setChecked(true);
            checkShowReceiptNo.setChecked(true);
            if (checkEnableGcash != null) checkEnableGcash.setChecked(true);
            if (checkEnableStoreCredit != null) checkEnableStoreCredit.setChecked(true);
            if (checkPromptPrint != null) checkPromptPrint.setChecked(true);
            if (checkAutoPrint != null) checkAutoPrint.setChecked(false);
            if (radioPaper58mm != null) radioPaper58mm.setChecked(true);
            if (checkEnableGcashQr != null) checkEnableGcashQr.setChecked(false);
            if (radioGcashQrModeAlways != null) radioGcashQrModeAlways.setChecked(true);
            if (checkEnablePin != null) checkEnablePin.setChecked(false);
            updateLivePreview.run();
            Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show();
        });

        dialogView.findViewById(R.id.btn_cancel_receipt_design).setOnClickListener(v -> dialog.dismiss());

        dialog.setOnDismissListener(d -> {
            currentSettingsDialog = null;
            currentSettingsQrPreview = null;
            currentSettingsRemoveQrBtn = null;
        });

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
        final int W = config.charactersPerLine > 0 ? config.charactersPerLine : 32;
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
            String sampleNo = config.getEffectiveReceiptPrefix() + receiptCounter;
            sb.append(leftRightText(config.getEffectiveReceiptNoLabel() + " " + sampleNo, "", W)).append("\n");
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
            int maxNameLen = W > 32 ? 26 : 18;
            for (CartItem item : cartItems) {
                String name = item.getName();
                if (name.length() > maxNameLen) name = name.substring(0, maxNameLen - 2) + "..";
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

        boolean showQr = config.enableGcashQrOnReceipt && (isSample || config.hasGcashQrImage(this));
        if (showQr) {
            sb.append(divider).append("\n");
            sb.append(centerText("[ GCASH QR CODE IMAGE ]", W)).append("\n");
            sb.append(centerText("Scan to Pay with GCash", W)).append("\n");
        }

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
        if (requestCode == PERMISSION_CAMERA_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (currentGcashDialogView != null) {
                    launchCameraCaptureGcash();
                } else {
                    launchCameraCapture();
                }
            } else {
                Toast.makeText(this, "Camera permission is required to scan.", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            this.checkBluetoothPermissions(this.onBluetoothPermissionsGranted);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_CAMERA_PRODUCT) {
            if (currentProductPhotoPath != null) {
                Bitmap bitmap = loadScaledBitmap(currentProductPhotoPath, 1024);
                if (bitmap != null) {
                    runOnDeviceProductRecognition(bitmap);
                } else {
                    Toast.makeText(this, "Could not load camera photo.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_GALLERY_PRODUCT) {
            if (data != null && data.getData() != null) {
                Bitmap bitmap = loadScaledBitmapFromUri(data.getData(), 1024);
                if (bitmap != null) {
                    runOnDeviceProductRecognition(bitmap);
                } else {
                    Toast.makeText(this, "Could not load gallery image.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_CAMERA_GCASH_SCAN) {
            if (currentGcashPhotoPath != null) {
                Bitmap bitmap = loadScaledBitmap(currentGcashPhotoPath, 1280);
                if (bitmap != null) {
                    processGcashAutoFillImage(bitmap);
                } else {
                    Toast.makeText(this, "Could not load camera photo.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_GALLERY_GCASH_SCAN) {
            if (data != null && data.getData() != null) {
                Bitmap bitmap = loadScaledBitmapFromUri(data.getData(), 1280);
                if (bitmap != null) {
                    processGcashAutoFillImage(bitmap);
                } else {
                    Toast.makeText(this, "Could not load gallery screenshot.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_PICK_GCASH_QR) {
            if (data != null && data.getData() != null) {
                try {
                    Uri qrUri = data.getData();
                    Bitmap raw = loadScaledBitmapFromUri(qrUri, 300);
                    if (raw != null) {
                        int target = 260;
                        Bitmap scaled = Bitmap.createScaledBitmap(raw, target, target, true);
                        File qrFile = new File(getFilesDir(), ReceiptDesignConfig.GCASH_QR_FILENAME);
                        FileOutputStream fos = new FileOutputStream(qrFile);
                        scaled.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        fos.close();

                        receiptDesign.enableGcashQrOnReceipt = true;
                        receiptDesign.save(this);

                        if (currentSettingsQrPreview != null) {
                            currentSettingsQrPreview.setImageBitmap(scaled);
                        }
                        if (currentSettingsRemoveQrBtn != null) {
                            currentSettingsRemoveQrBtn.setVisibility(View.VISIBLE);
                        }
                        if (currentSettingsDialog != null) {
                            CheckBox cb = currentSettingsDialog.findViewById(R.id.check_design_enable_gcash_qr);
                            if (cb != null) cb.setChecked(true);
                        }

                        Toast.makeText(this, "✅ GCash QR Code uploaded & enabled for receipts!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Could not load selected QR image.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("MorpsPOS", "Error saving GCash QR code", e);
                    Toast.makeText(this, "Failed to upload QR: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        } else if (requestCode == REQUEST_INSTALL_LOCAL_APK) {
            if (data != null && data.getData() != null) {
                installApkFromUri(data.getData());
            }
        } else if (requestCode == REQUEST_RESTORE_DB) {
            if (data != null && data.getData() != null) {
                final Uri backupUri = data.getData();
                new AlertDialog.Builder(this)
                    .setTitle("⚠️ Restore Database?")
                    .setMessage("Restoring a backup will overwrite the current sales, stock inventory, and product records on this device.\n\nAre you sure you want to proceed?")
                    .setPositiveButton("Restore Database", (d, w) -> {
                        requireOwnerPin("Authorize Database Restore", () -> restoreDatabase(backupUri));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        }
    }

    public Bitmap loadGcashQrBitmap() {
        try {
            File qrFile = new File(getFilesDir(), ReceiptDesignConfig.GCASH_QR_FILENAME);
            if (!qrFile.exists() || qrFile.length() == 0) return null;
            return BitmapFactory.decodeFile(qrFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("MorpsPOS", "Failed to load GCash QR bitmap", e);
            return null;
        }
    }

    private void backupDatabase() {
        try {
            if (dbHelper != null) {
                dbHelper.checkpoint();
            }

            File currentDb = getDatabasePath("morps_pos.db");
            if (!currentDb.exists()) {
                Toast.makeText(this, "Database file not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            File backupDir = new File(getCacheDir(), "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String timestamp = sdf.format(new Date());
            File backupFile = new File(backupDir, "morps_pos_backup_" + timestamp + ".db");

            java.io.FileInputStream fis = new java.io.FileInputStream(currentDb);
            FileOutputStream fos = new FileOutputStream(backupFile);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
            fos.close();
            fis.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", backupFile);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("application/octet-stream");
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Morps POS Database Backup (" + timestamp + ")");
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Morps POS database backup created on " + timestamp + ".\nKeep this file safe to restore on any device.");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(sendIntent, "Share or Save Database Backup"));
            Toast.makeText(this, "Backup file generated! Choose where to save.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("MorpsBackup", "Error creating database backup", e);
            Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void launchRestoreDatabasePicker() {
        requireOwnerPin("Restore Database", () -> {
            try {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "Select Morps POS Database Backup (.db)"), REQUEST_RESTORE_DB);
            } catch (Exception e) {
                Toast.makeText(this, "Could not open file picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void restoreDatabase(Uri backupUri) {
        ProgressDialog progressDialog = ProgressDialog.show(this, "Restoring Database", "Please wait while database is restored...", true, false);
        new Thread(() -> {
            boolean success = false;
            String errorMessage = "";
            try {
                InputStream is = getContentResolver().openInputStream(backupUri);
                byte[] header = new byte[16];
                int bytesRead = is.read(header);
                is.close();

                String headerStr = new String(header, StandardCharsets.UTF_8);
                if (bytesRead < 16 || !headerStr.startsWith("SQLite format 3")) {
                    throw new IllegalArgumentException("The selected file is not a valid SQLite database (header mismatch).");
                }

                DatabaseHelper.resetInstance();

                File targetDb = getDatabasePath("morps_pos.db");
                File walFile = new File(targetDb.getPath() + "-wal");
                if (walFile.exists()) walFile.delete();
                File shmFile = new File(targetDb.getPath() + "-shm");
                if (shmFile.exists()) shmFile.delete();

                is = getContentResolver().openInputStream(backupUri);
                FileOutputStream fos = new FileOutputStream(targetDb);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
                fos.close();
                is.close();

                dbHelper = DatabaseHelper.getInstance(MainActivity.this);
                dbHelper.checkpoint();
                success = true;
            } catch (Exception e) {
                Log.e("MorpsRestore", "Restore error", e);
                errorMessage = e.getMessage();
                dbHelper = DatabaseHelper.getInstance(MainActivity.this);
            }

            final boolean finalSuccess = success;
            final String finalMsg = errorMessage;
            runOnUiThread(() -> {
                progressDialog.dismiss();
                if (finalSuccess) {
                    cartItems.clear();
                    parkedCartItems.clear();
                    updateCartDisplay();
                    refreshAllData();

                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("✅ Database Restored")
                        .setMessage("Your Morps POS database has been successfully restored!\n\nAll products, inventory stock, and sales transactions have been updated.")
                        .setPositiveButton("OK", null)
                        .show();
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("❌ Restore Failed")
                        .setMessage("Failed to restore database:\n" + finalMsg)
                        .setPositiveButton("OK", null)
                        .show();
                }
            });
        }).start();
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
                        SharedPreferences.Editor editor = getSharedPreferences("MorpsPOSPrefs", Context.MODE_PRIVATE).edit();
                        if (index == -1) {
                            selectedBluetoothDevice = null;
                            editor.remove("savedPrinterMac").remove("savedPrinterName").apply();
                            textPrinterName.setText("🖨️ Printer: Default Bluetooth");
                        } else {
                            selectedBluetoothDevice = bluetoothDevicesList[index];
                            String mac = selectedBluetoothDevice.getDevice() != null ? selectedBluetoothDevice.getDevice().getAddress() : "";
                            String name = items[i1];
                            editor.putString("savedPrinterMac", mac).putString("savedPrinterName", name).apply();
                            textPrinterName.setText("🖨️ Printer: " + name);
                            Toast.makeText(MainActivity.this, "Saved " + name + " as preferred printer!", Toast.LENGTH_SHORT).show();
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
                        if (!cartItems.isEmpty()) {
                            new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Complete Sale")
                                .setMessage("Receipt printed! Would you like to complete this sale and update inventory now?")
                                .setPositiveButton("Complete Sale", (dialog, which) -> completeSaleAndReset(false))
                                .setNegativeButton("Keep in Cart", null)
                                .show();
                        }
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
                                        if (!cartItems.isEmpty()) {
                                            new AlertDialog.Builder(MainActivity.this)
                                                .setTitle("Complete Sale")
                                                .setMessage("Receipt printed! Would you like to complete this sale and update inventory now?")
                                                .setPositiveButton("Complete Sale", (dialog, which) -> completeSaleAndReset(false))
                                                .setNegativeButton("Keep in Cart", null)
                                                .show();
                                        }
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
        final int W = receiptDesign.charactersPerLine > 0 ? receiptDesign.charactersPerLine : 32;
        float widthMm = receiptDesign.paperWidthMm == 80 ? 72f : 48f;
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
        int maxNameLen = W > 32 ? 26 : 18;
        for (CartItem item : cartItems) {
            String itemName = item.getName();
            if (itemName.length() > maxNameLen) {
                itemName = itemName.substring(0, maxNameLen - 2) + "..";
            }
            itemsBuilder.append("[L]").append(item.getQuantity()).append("x ").append(itemName)
                .append("[R]").append(String.format(Locale.US, "%.2f", item.getTotalPrice()))
                .append("\n");
        }

        // Thermal printer: 203 DPI, 58mm (48mm printable) or 80mm (72mm printable)
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, widthMm, W);

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
            receipt.append("[L]").append(receiptDesign.getEffectiveReceiptNoLabel()).append(" ")
                   .append(receiptDesign.getEffectiveReceiptPrefix()).append(receiptCounter).append("\n");
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

        boolean shouldPrintQr = receiptDesign.enableGcashQrOnReceipt && receiptDesign.hasGcashQrImage(this);
        if (shouldPrintQr && "GCASH_ONLY".equalsIgnoreCase(receiptDesign.gcashQrPrintMode)) {
            shouldPrintQr = "GCASH".equalsIgnoreCase(paymentMode);
        }
        if (shouldPrintQr) {
            Bitmap qrBmp = loadGcashQrBitmap();
            if (qrBmp != null) {
                try {
                    String qrHex = com.dantsu.escposprinter.textparser.PrinterTextParserImg.bitmapToHexadecimalString(printer, qrBmp, false);
                    receipt.append("[C]").append(divider).append("\n");
                    receipt.append("[C]<img>").append(qrHex).append("</img>\n");
                    receipt.append("[C]<b>Scan to Pay with GCash</b>\n");
                } catch (Exception e) {
                    Log.e("MorpsPOS", "Error printing GCash QR on receipt", e);
                }
            }
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

    private void autoReconnectSavedPrinter() {
        SharedPreferences prefs = getSharedPreferences("MorpsPOSPrefs", Context.MODE_PRIVATE);
        String savedMac = prefs.getString("savedPrinterMac", null);
        String savedName = prefs.getString("savedPrinterName", null);

        if (savedMac != null && !savedMac.isEmpty()) {
            if (textPrinterName != null) {
                textPrinterName.setText("🖨️ Printer: " + (savedName != null ? savedName : savedMac));
            }
            this.checkBluetoothPermissions(() -> {
                BluetoothConnection[] bluetoothDevicesList = new BluetoothPrintersConnections().getList();
                if (bluetoothDevicesList != null) {
                    for (BluetoothConnection conn : bluetoothDevicesList) {
                        if (conn.getDevice() != null && savedMac.equalsIgnoreCase(conn.getDevice().getAddress())) {
                            selectedBluetoothDevice = conn;
                            if (textPrinterName != null) {
                                String dName = conn.getDevice().getName();
                                textPrinterName.setText("🖨️ Printer: " + (dName != null ? dName : savedName));
                            }
                            return;
                        }
                    }
                }
            });
        }
    }

    /*==============================================================================================
    ==================================CSV EXPORT & REPORT SHARING===================================
    ==============================================================================================*/

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void exportSalesToCsv() {
        String[] dates = getSelectedPeriodDateRange();
        String searchQuery = editSalesSearch != null ? editSalesSearch.getText().toString().trim() : null;
        List<SaleTransaction> sales = dbHelper.getFilteredSales(dates[0], dates[1], selectedSalesFilterMode, searchQuery);

        if (sales == null || sales.isEmpty()) {
            Toast.makeText(this, "No sales transactions to export for " + getPeriodLabel().toLowerCase(Locale.US) + ".", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String timestamp = sdf.format(new Date());
        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Receipt No,Date & Time,Date,Total Amount,Payment Mode,Amount Paid,Change Amount,Customer Note,Credit Settled,Items Breakdown\n");

        double totalRevenue = 0.0;
        double cashTotal = 0.0;
        double gcashTotal = 0.0;
        double creditTotal = 0.0;

        for (SaleTransaction s : sales) {
            totalRevenue += s.getTotalAmount();
            if ("CASH".equalsIgnoreCase(s.getPaymentMode())) cashTotal += s.getTotalAmount();
            else if ("GCASH".equalsIgnoreCase(s.getPaymentMode())) gcashTotal += s.getTotalAmount();
            else if ("CREDIT".equalsIgnoreCase(s.getPaymentMode()) && !s.isSettled()) creditTotal += s.getTotalAmount();

            // Format items
            StringBuilder itemsStr = new StringBuilder();
            List<SaleTransactionItem> items = s.getItems();
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    if (i > 0) itemsStr.append("; ");
                    SaleTransactionItem it = items.get(i);
                    itemsStr.append(it.getQuantity()).append("x ").append(it.getItemName()).append(" (₱").append(String.format(Locale.US, "%.2f", it.getSubtotal())).append(")");
                }
            }

            csv.append(escapeCsv(s.getReceiptNo())).append(",")
               .append(escapeCsv(s.getDateTime())).append(",")
               .append(escapeCsv(s.getDateOnly())).append(",")
               .append(String.format(Locale.US, "%.2f", s.getTotalAmount())).append(",")
               .append(escapeCsv(s.getPaymentMode())).append(",")
               .append(String.format(Locale.US, "%.2f", s.getAmountPaid())).append(",")
               .append(String.format(Locale.US, "%.2f", s.getChangeAmount())).append(",")
               .append(escapeCsv(s.getCustomerNote())).append(",")
               .append(s.isSettled() ? "YES" : "NO").append(",")
               .append(escapeCsv(itemsStr.toString())).append("\n");
        }

        DatabaseHelper.SalesStats stats = dbHelper.getSalesStats(dates[0], dates[1]);

        // Summary row
        csv.append("\n");
        csv.append("SUMMARY (").append(getPeriodLabel()).append("),").append(sales.size()).append(" transactions,,")
           .append(String.format(Locale.US, "Gross: %.2f", totalRevenue)).append(",")
           .append(String.format(Locale.US, "Profit: %.2f (%.1f%%)", stats.netProfit, stats.profitMarginPercent)).append(",")
           .append("Cash: ").append(String.format(Locale.US, "%.2f", cashTotal)).append(",")
           .append("GCash: ").append(String.format(Locale.US, "%.2f", gcashTotal)).append(",")
           .append("Unpaid Credit: ").append(String.format(Locale.US, "%.2f", creditTotal)).append(",,,\n");

        String filename = "morps_sales_" + getPeriodLabel().toLowerCase(Locale.US).replace(" ", "_") + "_" + timestamp + ".csv";
        shareCsvFile(filename, csv.toString(), "Morps POS - Sales Report (" + getPeriodLabel() + ", " + sales.size() + " records)");
    }

    private void exportInventoryToCsv() {
        List<Product> products = dbHelper.getAllProducts(null, null);

        if (products.isEmpty()) {
            Toast.makeText(this, "No products to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String timestamp = sdf.format(new Date());
        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Product ID,Product Name,Category,Selling Price,Cost Price,Stock on Hand,Total Valuation,Min Alert Threshold,Stock Status\n");

        int totalUnits = 0;
        double totalValuation = 0.0;

        for (Product p : products) {
            double itemVal = p.getStock() * p.getPrice();
            totalUnits += p.getStock();
            totalValuation += itemVal;

            String status = "In Stock";
            if (p.isOutOfStock()) status = "OUT OF STOCK";
            else if (p.isLowStock()) status = "LOW STOCK";

            csv.append(p.getId()).append(",")
               .append(escapeCsv(p.getName())).append(",")
               .append(escapeCsv(p.getCategory())).append(",")
               .append(String.format(Locale.US, "%.2f", p.getPrice())).append(",")
               .append(String.format(Locale.US, "%.2f", p.getCost())).append(",")
               .append(p.getStock()).append(",")
               .append(String.format(Locale.US, "%.2f", itemVal)).append(",")
               .append(p.getMinThreshold()).append(",")
               .append(status).append("\n");
        }

        // Summary row
        csv.append("\n");
        csv.append("TOTALS,").append(products.size()).append(" products,,,,,")
           .append(totalUnits).append(" units,")
           .append(String.format(Locale.US, "₱%.2f", totalValuation)).append(",,\n");

        String filename = "morps_inventory_" + timestamp + ".csv";
        shareCsvFile(filename, csv.toString(), "Morps POS - Inventory Valuation Report (" + products.size() + " products)");
    }

    private void shareCsvFile(String filename, String content, String subject) {
        try {
            File reportsDir = new File(getCacheDir(), "reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }
            File file = new File(reportsDir, filename);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            fos.flush();
            fos.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/csv");
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            sendIntent.putExtra(Intent.EXTRA_TEXT, subject + "\nGenerated from Morps POS.");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(sendIntent, "Export Report via"));
        } catch (Exception e) {
            Log.e("MorpsExport", "Error sharing CSV report", e);
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /*==============================================================================================
    ==================================STORE OWNER PIN SECURITY LOCK=================================
    ==============================================================================================*/

    /**
     * Verifies owner authorization with a 4-digit numeric PIN if PIN security is enabled.
     * If PIN security is disabled, executes onAuthorized immediately.
     */
    private void requireOwnerPin(String actionTitle, Runnable onAuthorized) {
        if (!receiptDesign.enablePinSecurity) {
            if (onAuthorized != null) onAuthorized.run();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        TextView promptText = new TextView(this);
        promptText.setText("Enter 4-digit Owner PIN to authorize:\n" + actionTitle);
        promptText.setTextSize(13);
        promptText.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
        container.addView(promptText);

        final EditText editPin = new EditText(this);
        editPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        editPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
        editPin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        editPin.setGravity(Gravity.CENTER);
        editPin.setTextSize(22);
        editPin.setHint("••••");
        editPin.setLetterSpacing(0.3f);
        editPin.setBackgroundResource(R.drawable.edittext_border);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (int) (48 * getResources().getDisplayMetrics().density)
        );
        lp.topMargin = (int) (14 * getResources().getDisplayMetrics().density);
        editPin.setLayoutParams(lp);
        container.addView(editPin);

        builder.setTitle("🔒 Owner PIN Required");
        builder.setView(container);
        builder.setPositiveButton("Authorize", (dialog, which) -> {
            String entered = editPin.getText().toString().trim();
            String currentPin = (receiptDesign.securityPin == null || receiptDesign.securityPin.trim().isEmpty())
                    ? "1234" : receiptDesign.securityPin.trim();
            if (entered.equals(currentPin)) {
                if (onAuthorized != null) onAuthorized.run();
            } else {
                Toast.makeText(this, "❌ Incorrect PIN. Action denied.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dlg = builder.create();
        dlg.show();
        editPin.requestFocus();
    }

    private void openChangePinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        TextView labelOld = new TextView(this);
        labelOld.setText("Current PIN (Default is 1234):");
        labelOld.setTextSize(12);
        labelOld.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
        container.addView(labelOld);

        final EditText editOldPin = new EditText(this);
        editOldPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        editOldPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
        editOldPin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        editOldPin.setHint("Current PIN");
        editOldPin.setBackgroundResource(R.drawable.edittext_border);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (44 * getResources().getDisplayMetrics().density));
        lp1.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
        lp1.bottomMargin = (int) (10 * getResources().getDisplayMetrics().density);
        editOldPin.setLayoutParams(lp1);
        container.addView(editOldPin);

        TextView labelNew = new TextView(this);
        labelNew.setText("New 4-digit PIN:");
        labelNew.setTextSize(12);
        labelNew.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
        container.addView(labelNew);

        final EditText editNewPin = new EditText(this);
        editNewPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        editNewPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
        editNewPin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        editNewPin.setHint("New PIN");
        editNewPin.setBackgroundResource(R.drawable.edittext_border);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (44 * getResources().getDisplayMetrics().density));
        lp2.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
        lp2.bottomMargin = (int) (10 * getResources().getDisplayMetrics().density);
        editNewPin.setLayoutParams(lp2);
        container.addView(editNewPin);

        TextView labelConfirm = new TextView(this);
        labelConfirm.setText("Confirm New PIN:");
        labelConfirm.setTextSize(12);
        labelConfirm.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
        container.addView(labelConfirm);

        final EditText editConfirmPin = new EditText(this);
        editConfirmPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        editConfirmPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
        editConfirmPin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        editConfirmPin.setHint("Confirm New PIN");
        editConfirmPin.setBackgroundResource(R.drawable.edittext_border);
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (44 * getResources().getDisplayMetrics().density));
        lp3.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
        editConfirmPin.setLayoutParams(lp3);
        container.addView(editConfirmPin);

        builder.setTitle("🔑 Change Store Owner PIN");
        builder.setView(container);
        builder.setPositiveButton("Save PIN", (dialog, which) -> {
            String current = (receiptDesign.securityPin == null || receiptDesign.securityPin.trim().isEmpty())
                    ? "1234" : receiptDesign.securityPin.trim();
            String oldEntered = editOldPin.getText().toString().trim();
            String newEntered = editNewPin.getText().toString().trim();
            String confirmEntered = editConfirmPin.getText().toString().trim();

            if (!oldEntered.equals(current)) {
                Toast.makeText(this, "Current PIN is incorrect.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newEntered.length() < 4) {
                Toast.makeText(this, "PIN must be at least 4 digits.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newEntered.equals(confirmEntered)) {
                Toast.makeText(this, "New PIN and confirmation do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            receiptDesign.securityPin = newEntered;
            receiptDesign.enablePinSecurity = true;
            receiptDesign.save(this);
            Toast.makeText(this, "✅ Owner PIN updated successfully!", Toast.LENGTH_LONG).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /*==============================================================================================
    ==================================IN-APP APP UPDATER (ONLINE & OFFLINE)========================
    ==============================================================================================*/

    private String getCurrentAppVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

    private int getCurrentAppVersionCode() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 1;
        }
    }

    private void checkForOnlineUpdate() {
        ProgressDialog checkDialog = ProgressDialog.show(this, "Checking for Updates", "Connecting to update server...", true, false);
        new Thread(() -> {
            try {
                String targetUrl = receiptDesign.updateSourceUrl;
                if (targetUrl == null || targetUrl.trim().isEmpty()) {
                    targetUrl = "https://api.github.com/repos/cmorpe7-ui/MorpsPOS/releases/latest";
                }

                URL url = new URL(targetUrl.trim());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "MorpsPOS-App");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json, application/json");

                int respCode = conn.getResponseCode();
                if (respCode >= 200 && respCode < 300) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    conn.disconnect();

                    String jsonStr = sb.toString();
                    runOnUiThread(() -> {
                        checkDialog.dismiss();
                        parseAndPromptUpdate(jsonStr);
                    });
                } else {
                    conn.disconnect();
                    runOnUiThread(() -> {
                        checkDialog.dismiss();
                        Toast.makeText(this, "Update server returned HTTP " + respCode + ". Check connection or URL.", Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                Log.e("MorpsPOS", "Update check failed", e);
                runOnUiThread(() -> {
                    checkDialog.dismiss();
                    new AlertDialog.Builder(this)
                        .setTitle("Update Check Failed")
                        .setMessage("Could not connect to update source:\n" + e.getMessage() + "\n\nIf you are offline or using a local APK, you can install updates manually via 'Install Update from Local Storage'.")
                        .setPositiveButton("OK", null)
                        .show();
                });
            }
        }).start();
    }

    private boolean isVersionNewer(String remoteVer, String currentVer) {
        if (remoteVer == null || currentVer == null) return false;
        String cleanRemote = remoteVer.trim().replaceAll("^[vV]", "");
        String cleanCurrent = currentVer.trim().replaceAll("^[vV]", "");
        if (cleanRemote.equalsIgnoreCase(cleanCurrent)) return false;

        String[] rParts = cleanRemote.split("[.-]");
        String[] cParts = cleanCurrent.split("[.-]");
        int len = Math.max(rParts.length, cParts.length);
        for (int i = 0; i < len; i++) {
            int rVal = 0;
            int cVal = 0;
            if (i < rParts.length) {
                try { rVal = Integer.parseInt(rParts[i]); } catch (Exception ignored) {}
            }
            if (i < cParts.length) {
                try { cVal = Integer.parseInt(cParts[i]); } catch (Exception ignored) {}
            }
            if (rVal > cVal) return true;
            if (rVal < cVal) return false;
        }
        return false;
    }

    private void parseAndPromptUpdate(String jsonStr) {
        try {
            JSONObject obj = new JSONObject(jsonStr);
            String remoteVersion = obj.optString("tag_name", obj.optString("version", ""));
            String changelog = obj.optString("body", obj.optString("changelog", "No release notes provided."));
            String downloadUrl = null;
            String apkName = "MorpsPOS-update.apk";

            if (obj.has("assets")) {
                JSONArray assets = obj.getJSONArray("assets");
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    String name = asset.optString("name", "");
                    if (name.toLowerCase(Locale.US).endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", "");
                        apkName = name;
                        break;
                    }
                }
            }
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                downloadUrl = obj.optString("apkUrl", obj.optString("downloadUrl", ""));
            }

            String currentVersion = getCurrentAppVersionName();
            boolean isNewer = isVersionNewer(remoteVersion, currentVersion);

            if (!isNewer) {
                new AlertDialog.Builder(this)
                    .setTitle("✅ Morps POS is Up to Date")
                    .setMessage("You are running the latest version: " + currentVersion + (remoteVersion.isEmpty() ? "" : " (Server: " + remoteVersion + ")"))
                    .setPositiveButton("OK", null)
                    .show();
                return;
            }

            if (downloadUrl == null || downloadUrl.isEmpty()) {
                new AlertDialog.Builder(this)
                    .setTitle("Update Available: " + remoteVersion)
                    .setMessage("A new version (" + remoteVersion + ") is available, but no direct APK download link was found in the release assets.\n\nNotes:\n" + changelog)
                    .setPositiveButton("OK", null)
                    .show();
                return;
            }

            final String finalDownloadUrl = downloadUrl;
            final String finalApkName = apkName;
            new AlertDialog.Builder(this)
                .setTitle("🚀 New Update Available!")
                .setMessage("A new version of Morps POS is ready!\n\n" +
                        "• Current Version: " + currentVersion + "\n" +
                        "• New Version: " + remoteVersion + "\n\n" +
                        "What's New:\n" + changelog + "\n\n" +
                        "How would you like to download the update?")
                .setPositiveButton("🌐 Open in Browser", (dialog, which) -> {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalDownloadUrl));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not open browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Direct Download", (dialog, which) -> downloadAndInstallApk(finalDownloadUrl, finalApkName))
                .setNegativeButton("Later", null)
                .show();

        } catch (Exception e) {
            Log.e("MorpsPOS", "Error parsing update JSON", e);
            Toast.makeText(this, "Could not parse update metadata: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void downloadAndInstallApk(String urlStr, String fileName) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Downloading Update");
        progress.setMessage("Downloading APK...");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setCancelable(false);
        progress.setMax(100);
        progress.setProgress(0);
        progress.show();

        new Thread(() -> {
            File updateDir = new File(getCacheDir(), "updates");
            if (!updateDir.exists()) updateDir.mkdirs();
            File apkFile = new File(updateDir, (fileName != null && !fileName.isEmpty()) ? fileName : "update.apk");

            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "MorpsPOS-App");
                conn.setInstanceFollowRedirects(true);
                conn.connect();

                int status = conn.getResponseCode();
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    String newUrl = conn.getHeaderField("Location");
                    conn.disconnect();
                    url = new URL(newUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "MorpsPOS-App");
                    conn.connect();
                }

                int fileLength = conn.getContentLength();
                InputStream input = conn.getInputStream();
                FileOutputStream output = new FileOutputStream(apkFile);

                byte[] data = new byte[8192];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    if (fileLength > 0) {
                        int percent = (int) (total * 100 / fileLength);
                        runOnUiThread(() -> progress.setProgress(percent));
                    }
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
                conn.disconnect();

                runOnUiThread(() -> {
                    progress.dismiss();
                    installApk(apkFile);
                });
            } catch (Exception e) {
                Log.e("MorpsPOS", "Download APK failed", e);
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Failed to download update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void pickLocalApkForInstall() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.android.package-archive");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Morps POS APK Update File"), REQUEST_INSTALL_LOCAL_APK);
        } catch (Exception e) {
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.setType("*/*");
            fallback.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(fallback, "Select Morps POS APK Update File"), REQUEST_INSTALL_LOCAL_APK);
        }
    }

    private void installApkFromUri(Uri apkUri) {
        ProgressDialog progress = ProgressDialog.show(this, "Preparing Update", "Copying APK file...", true, false);
        new Thread(() -> {
            try {
                File updateDir = new File(getCacheDir(), "updates");
                if (!updateDir.exists()) updateDir.mkdirs();
                File targetApk = new File(updateDir, "offline_update.apk");

                InputStream is = getContentResolver().openInputStream(apkUri);
                FileOutputStream fos = new FileOutputStream(targetApk);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.flush();
                fos.close();
                if (is != null) is.close();

                runOnUiThread(() -> {
                    progress.dismiss();
                    installApk(targetApk);
                });
            } catch (Exception e) {
                Log.e("MorpsPOS", "Failed to copy local APK", e);
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Failed to prepare update APK: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void installApk(File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            Toast.makeText(this, "APK update file not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("MorpsPOS", "Error launching APK install", e);
            Toast.makeText(this, "Failed to launch installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openConfigureUpdateUrlDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        TextView info = new TextView(this);
        info.setText("Enter the GitHub repository releases endpoint or a custom HTTP/HTTPS update server URL.\n\nDefault is GitHub repository cmorpe7-ui/MorpsPOS.");
        info.setTextSize(12);
        info.setTextColor(ContextCompat.getColor(this, R.color.colorTextDark));
        container.addView(info);

        final EditText editUrl = new EditText(this);
        editUrl.setText(receiptDesign.updateSourceUrl);
        editUrl.setHint("https://api.github.com/repos/.../releases/latest");
        editUrl.setBackgroundResource(R.drawable.edittext_border);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (48 * getResources().getDisplayMetrics().density));
        lp.topMargin = (int) (10 * getResources().getDisplayMetrics().density);
        editUrl.setLayoutParams(lp);
        container.addView(editUrl);

        builder.setTitle("⚙️ Configure Update Server URL");
        builder.setView(container);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String url = editUrl.getText().toString().trim();
            if (url.isEmpty()) {
                url = "https://api.github.com/repos/cmorpe7-ui/MorpsPOS/releases/latest";
            }
            receiptDesign.updateSourceUrl = url;
            receiptDesign.save(this);
            Toast.makeText(this, "Update server URL saved.", Toast.LENGTH_SHORT).show();
        });
        builder.setNeutralButton("Reset to Default", (dialog, which) -> {
            receiptDesign.updateSourceUrl = "https://api.github.com/repos/cmorpe7-ui/MorpsPOS/releases/latest";
            receiptDesign.save(this);
            Toast.makeText(this, "Reset to default GitHub URL.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(this.usbReceiver);
        } catch (Exception ignored) {}
    }
}

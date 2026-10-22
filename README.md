# Morps POS 🛒📱

**Morps POS** is an offline-ready, native Android Point of Sale (POS), Inventory, and Financial Management application purpose-built for Philippine Sari-Sari Stores, retail shops, and mini-marts. It features native **58mm & 80mm ESC/POS Bluetooth & USB thermal receipt printing**, a dedicated **GCash Cash-In & Cash-Out float ledger**, barcode scanning, and complete offline SQLite database backup.

---

## 🌟 Key Application Features

### 🛒 1. Point of Sale (POS) Checkout & Smart Cart
* **Live Product Catalog & Category Filters**: Instant filtering by category (*Snacks, Drinks, Canned Goods, Condiments, Toiletries, Cigarettes, Rice & Grains*).
* **Stock-Aware Cart Clamping**: Cart quantities are strictly synchronized with actual inventory stocks. Cashiers cannot ring up more units than what physically exists on shelves.
* **Real-time Stock Badges**: Product buttons visually show current stock levels and change to amber/red warnings when running low.
* **Quick-Add Custom Item**: Ring up uncataloged or seasonal items with one tap by entering a custom name and price.
* **Fast Cash Calculation Tender Chips**: Quick tender buttons (`Exact`, `₱50`, `₱100`, `₱200`, `₱500`, `₱1,000`) with instant *sukli* (change) calculation.
* **Flexible Tender Options**:
  * 💵 **Cash**: Live cash tendered and change calculation.
  * 📱 **GCash**: Track digital payments with optional reference number.
  * 📝 **Store Credit (*Utang / Listahan*)**: Track customer store credit until paid.
* **Cart Park & Resume**: Temporarily park a customer's cart to attend to another waiting customer, then restore it with a single tap.
* **On-Screen Monospace Thermal Preview**: Live 32-column (58mm) or 48-column (80mm) receipt preview showing exactly how the slip will look before printing.

---

### 📱 2. GCash Cash-In & Cash-Out Management
A dedicated tab designed for retail stores offering financial remittance and digital wallet cash services:
* **⚡ Zero-Friction Auto-Fill System (100% Fool-Proof & Zero Manual Typing)**:
  * 📸 **AI Camera Scanner (Customer Screen / SMS / Note / QR)**: Tap `📸 Scan Screen / QR` to point the camera at the customer's confirmation screen, SMS message, paper note, or receipt. The on-device Google ML Kit OCR and ZXing engine automatically extracts:
    * 💵 **Amount sent** (e.g. `₱1,000.00`)
    * 📱 **Customer Mobile Number** (`09XXXXXXXXX`)
    * 🔢 **GCash Reference No.** (e.g. `Ref No. 1002 9384 1029`)
    * 👤 **Sender / Customer Name**
  * 🔲 **Customer GCash QR Code Scanner**: Seamlessly decodes the customer's personal "Receive Money" QR code from the camera or photo gallery, immediately populating their 11-digit number and name.
  * 📋 **1-Tap "Paste from GCash SMS" / Clipboard**: Cashiers tap `📋 Paste SMS` to parse incoming GCash remittance SMS notifications (e.g. *"You have received PHP 500.00 of GCash from JUAN DELA CRUZ 09171234567 with Ref No. 900123456789"*), automatically distributing each value to its exact form field.
  * 👥 **Quick-Select Frequent / Returning Customers**: Tap `👥 Recent` to open a search-enabled directory of neighborhood regulars from past transactions. Selecting any customer instantly pre-fills their 11-digit number and name.
  * 🛡️ **100% Fool-Proof Verification Preview**: Before any auto-detected data touches the transaction form, an editable verification modal displays all extracted fields for cashier confirmation. Strict 11-digit format checking (`09XXXXXXXXX`) and amount limits guarantee zero financial errors.
* **Dual Float Balance Tracking**:
  * **GCash App Balance (Digital Float)**: Tracks digital wallet funds. Automatically decreases on Cash-In and increases on Cash-Out.
  * **Cash Drawer Balance (Physical Float)**: Tracks physical cash in drawer. Automatically increases on Cash-In and decreases on Cash-Out.
* **Adjust Opening Floats**: Set opening drawer and GCash balances anytime.
* **Smart Convenience Fee Calculator**:
  * Quick amount chips (`₱100`, `₱200`, `₱500`, `₱1,000`, `₱2,000`, `₱5,000`).
  * Standard fee presets (`₱0`, `₱10`, `₱15`, `₱20`, `₱30`) with auto-suggested fee brackets.
  * Real-time calculation showing exact physical cash to collect or dispense.
* **Thermal Acknowledgment Slip Printing**: Prints clean thermal slips containing store header, slip number (`GC-YYYYMMDD-XXXX`), customer mobile, amounts, convenience fees, and timestamp.
* **Ledger Filter & Search**: Filter by date (*Today, Yesterday, Last 7 Days, This Month, All Time*) and transaction type (*All, Cash-In, Cash-Out*), or search by customer name, mobile number, or reference number.
* **Summary Analytics**: Real-time totals for **Total In**, **Total Out**, and **Net Service Fees Earned (Gross Remittance Profit)**.
* **CSV Export & Share**: Export the entire GCash ledger to CSV and share via email or messaging apps.
* **Void & Audit Protection**: Void erroneous entries with Owner PIN verification.

---

### 📦 3. SQLite Inventory & Stock Control
* **On-Device SQLite Database (`morps_pos.db`)**: Atomic updates ensure inventory decrements instantly upon checkout.
* **Inventory Metrics Overview**:
  * Total registered products.
  * Total physical stock count.
  * Real-time inventory valuation (total store asset value in PHP).
  * Low-stock warning banner.
* **1-Tap Quick Restock**: Instant `+5` and `+10` buttons directly on inventory item cards.
* **Product Management**: Add new items with selling price, cost price, barcode, and custom low-stock threshold alert.
* **Barcode & Camera Scanning**: Integrated Google ML Kit camera barcode recognition and text label scanner for fast product lookups.
* **Inventory Stock CSV Export**: Export complete stock inventory sheets for offline auditing.

---

### 📊 4. Sales History & End-of-Day Daily Summary
* **Sales Analytics Cards**: Real-time metrics for Gross Revenue, Transaction Count, Cash Collected, GCash Payments, and Outstanding Store Credit.
* **Store Credit (*Utang*) Ledger**: View all unpaid customer balances and mark them as settled in 1 tap.
* **Receipt Reprinting & Voiding**: Reprint thermal receipts or void past sales with Owner PIN protection.
* **End-of-Day Daily Summary (Z-Reading)**:
  * Computes daily gross sales, tender breakdowns, net cash, and inventory movements.
  * Prints a formatted thermal summary ticket for store owners at closing time.
* **Sales CSV Export**: Export sales transactions filtered by date range for external accounting in Excel.

---

### ⚙️ 5. Clean Card-Based Settings & Receipt Customization
* **Store Profile & Branding**: Customize store name, tagline/subtitle, store address, and contact number.
* **Receipt Layout & Typography**:
  * Thermal paper width toggle: **58mm (32 columns)** or **80mm (48 columns)**.
  * Receipt title header (*ORDER RECEIPT, SALES INVOICE, OFFICIAL RECEIPT*).
  * Receipt number prefix (*MORPS-, INV-, OR-, #* or custom).
  * Large or Normal header font size.
  * Divider styles (`===`, `---`, `***`).
  * Customizable footer greetings and return policy notice (*"No Return, No Exchange"*).
  * Toggle date/time and receipt number visibility.
* **Custom GCash QR Code on Receipts**:
  * Upload your personal or merchant GCash QR code image directly from the phone gallery.
  * Advanced on-device bilevel dithering prints crisp QR codes on thermal paper.
  * Configurable printing rule: print on all receipts or only when GCash payment is selected.
* **Checkout Automation**: Option to auto-print receipt immediately upon checkout, or prompt before printing.

---

### 🔒 6. Security & Offline Data Protection
* **Store Owner 4-Digit PIN Security**: Protects sensitive operations (deleting past sales, price modifications, database restore, and ledger resets).
* **Offline Database Backup & Restore**:
  * **Backup**: 1-tap backup of the entire SQLite database to local storage.
  * **Restore**: Restore previous database backups safely with PIN verification.
* **100% Offline-First Architecture**: Operates with full functionality without requiring an active internet connection.

---

### 🚀 7. In-App Updates & Antivirus Hardening
* **In-App Update Checker**: Checks for new version releases via GitHub Releases API or custom update servers.
* **Safe Browser Download**: Opens official releases in the user's browser, allowing native Android system package verification without heuristic false positives.
* **Zero Suspicious Permissions**: Clean manifest without high-risk permissions like `REQUEST_INSTALL_PACKAGES`.
* **Production Signed**: Signed with a dedicated 2048-bit RSA keystore (`morps-release.jks`) using both **v1 (JAR signing)** and **v2 (APK Signature Scheme)** to prevent Google Play Protect and antivirus warnings.
* **Enforced TLS/HTTPS**: Configured with `usesCleartextTraffic="false"` for strict secure communications.

### 🛡️ 8. Strict Data Integrity & Input Validation
Enterprise-grade data guards ensure zero financial or inventory mistakes occur during daily retail operations:
* **Strict Philippine Mobile Number Validation (`09XXXXXXXXX`)**:
  * Enforces exact 11-digit mobile format starting with `09` (Globe, TM, Smart, TNT, DITO, Sun).
  * Real-time digit counter (`X / 11 digits`) and color-coded status badges (green for valid, amber for in-progress, red for invalid prefix).
  * Auto-normalization: automatically cleans pasted numbers from `+639XXXXXXXXX`, `639XXXXXXXXX`, `9XXXXXXXXX`, or dashed formats (`0917-123-4567`) into clean 11-digit numbers.
  * Blocks submission and alerts cashier if the number is incomplete or mistyped, preventing irreversible remittance errors.
  * Verification confirmation dialog displays customer mobile number in bold before committing any Cash-In or Cash-Out transaction.
* **Financial Amount & Tender Integrity**:
  * GCash transaction amounts must be strictly positive (`> ₱0.00`). Warns cashier if amount exceeds standard ₱50,000 limits.
  * Non-negative service fee enforcement (`>= ₱0.00`).
  * Cash Tendered check: Blocks underpayment during CASH checkout if entered cash is less than the total bill.
  * Store Credit (*Utang*) integrity: Customer/debtor name is strictly required when selecting Credit to prevent anonymous untrackable debt.
* **Inventory & Pricing Data Integrity**:
  * Product selling price must be strictly positive (`> ₱0.00`).
  * Supplier cost, stock quantity, and low-stock alert thresholds cannot be negative.
  * Below-Cost Warning: Automatically detects if selling price is set below supplier cost and alerts store owner before saving.
  * Float balance integrity: Starting drawer and GCash digital float balances cannot be negative.

---

## 🖨️ Thermal Printer Hardware Compatibility

| Connection | Supported Hardware | Roll Specifications |
| :--- | :--- | :--- |
| **Bluetooth** | Standard 58mm & 80mm ESC/POS Mobile Printers (e.g. MPT-II, POS-5802, GOOJPRT, MUNBYN, Xprinter, Netum) | 58mm (32 chars/line) or 80mm (48 chars/line), 203 DPI |
| **USB OTG** | Direct USB connection via USB OTG cable adapter | Standard USB ESC/POS thermal printers with automatic USB device permission handling |

---

## 🛠️ Tech Stack & Architecture

* **Platform**: Native Android (Java 8 / Java 17 toolchain)
* **Minimum SDK**: Android 5.0 (API 21 - Lollipop)
* **Target SDK**: Android 13 (API 33)
* **Database**: Embedded SQLite (`DatabaseHelper.java`, schema version 3)
* **Hardware Drivers**: Custom native ESC/POS thermal printer driver library (`escposprinter` module)
* **Machine Learning**: Google ML Kit On-Device Vision (Barcode Scanning, Image Labeling, Text Recognition)
* **UI Design**: Modern Material 3 Emerald Slate styling with custom card containers and responsive layout

---

## 📦 Building the Application

1. Open the project in **Android Studio** (Hedgehog / Iguana / Ladybug or newer).
2. Set Gradle JDK to **Java 17**:
   * `File` ➔ `Settings` ➔ `Build, Execution, Deployment` ➔ `Build Tools` ➔ `Gradle` ➔ `Gradle JDK: Java 17`.
3. To build via command line:
   ```powershell
   # Debug Build:
   .\gradlew.bat assembleDebug

   # Production Release Build (Signed with morps-release.jks):
   .\gradlew.bat assembleRelease
   ```
4. Output APK locations:
   * **Release APK**: `app/build/outputs/apk/release/app-release.apk`
   * **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`

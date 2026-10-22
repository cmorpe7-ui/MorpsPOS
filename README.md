# Morps POS 🛒

**Morps POS** is a modern, native Android Point of Sale (POS) and inventory management application designed for Philippine Sari-Sari Stores and retail shops, featuring native **58mm ESC/POS Bluetooth & USB thermal receipt printing**.

---

## 🌟 Key Features

### 🛒 1. Point of Sale (POS) Checkout
- **Interactive Product Catalog**: Instant item filtering across pre-seeded categories (Snacks, Drinks, Canned Goods, Condiments, Toiletries, Cigarettes, Rice & Grains).
- **Custom Item Quick-Add**: Manually add custom unlisted items to the cart with one tap.
- **Fast Cash Calculation Chips**: Instant tender suggestions (`Exact`, `₱50`, `₱100`, `₱500`, `₱1,000`) with auto-calculated change (*sukli*).
- **Flexible Payment Methods**:
  - 💵 **Cash** (with live change calculation)
  - 📱 **GCash** (with customer reference notes)
  - 📝 **Store Credit / Utang** (tracked in database until settled)
- **Real-Time 32-Column Monospace Receipt Preview**: Visual on-screen paper preview formatted exactly as printed on 58mm thermal rolls.

### 📦 2. Full SQLite Inventory Management
- **SQLite Database (`morps_pos.db`)**:
  - Products table with live stock tracking, selling prices, cost prices, and low-stock threshold alerts.
  - Automatically deducts stock upon every completed sale in a single atomic transaction.
- **Inventory Metrics Dashboard**:
  - Total registered products
  - Total in-stock physical units
  - Real-time inventory valuation (total asset worth in PHP)
  - Low stock warning banner with direct navigation
- **1-Tap Quick Restock**: Instant `+5` and `+10` unit buttons on each item card.
- **Product Management Modal**: Add new products, edit prices/thresholds, or delete items.

### 📊 3. Sales Tracking & Daily Z-Reading Report
- **Sales Analytics Dashboard**:
  - Today's Gross Sales Revenue
  - Transaction Count
  - Cash Received Breakdown
  - GCash Breakdown
  - Outstanding Unpaid Credit (*Utang*)
- **Store Credit Ledger**: Filter unpaid store credit transactions and mark them as paid with 1 tap.
- **Receipt Re-Printing**: Re-print previous receipts to Bluetooth/USB printers or view digital copies.
- **End-of-Day Daily Z-Reading Report**:
  - Computes and prints a dedicated 58mm thermal summary ticket covering daily gross sales, tender breakdowns, and end-of-day stock levels.

### ⚙️ 4. Customizable Receipt Designer
- **Store Branding**: Editable Store Name, Subheader, Address, Contact Number, and Receipt Title.
- **Customizable Layout**:
  - Normal or Large Header Font
  - Divider styles (`=`, `-`, `*`)
  - Footers & Policy notes (*"No Return, No Exchange"*)
  - Date & Receipt Number toggles
- **Persistent Preferences**: Saved locally via `SharedPreferences`.

---

## 🖨️ 58mm Thermal Printer Hardware Support

- **ESC/POS Specification**: Formatted for standard 58mm thermal rolls (203 DPI, 48mm printable width, 32 characters per line).
- **Bluetooth**: Connect to any standard Bluetooth thermal receipt printer (e.g. MPT-II, POS-58, GOOJPRT, MUNBYN, Xprinter).
- **USB OTG**: Connect directly to USB thermal printers via an OTG adapter cable.

---

## 🚀 Getting Started

1. Clone or open this repository in **Android Studio**:
   ```bash
   git clone <repo-url>
   ```
2. In Android Studio, ensure the Gradle JDK is set to **Java 17**:
   - `File` ➔ `Settings` ➔ `Build, Execution, Deployment` ➔ `Build Tools` ➔ `Gradle` ➔ `Gradle JDK: Java 17`.
3. Build and run on an Android device or emulator (`Shift + F10`).

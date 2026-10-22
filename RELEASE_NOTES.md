# 🛒 Morps POS v1.0 — Official Production Release

**Morps POS** is an offline-ready, native Android Point of Sale (POS), Inventory, and Financial Management application purpose-built for Philippine Sari-Sari Stores, retail shops, and mini-marts. It features native **58mm & 80mm ESC/POS Bluetooth & USB thermal receipt printing**, a dedicated **GCash Cash-In & Cash-Out float ledger**, on-device AI camera recognition, and complete offline SQLite database backup.

---

## 🌟 What's New in v1.0

### 1. 📱 Dedicated GCash Cash-In & Cash-Out Float Ledger
* **Dual Float Tracking**: Real-time tracking of both your **GCash Digital App Float** and **Physical Cash Drawer Float** (automatically updated on every transaction).
* **Smart Convenience Fee Calculator**: Quick fee chips (`₱10`, `₱15`, `₱20`, `₱30`) and auto-suggested brackets with live physical cash dispense/collection calculation.
* **Separated Financial Reporting**: Store merchandise sales and GCash remittance service fees are tracked in distinct, dedicated ledgers to keep business profits clean and accurate.
* **Thermal Acknowledgment Slips**: Print formatted 58mm/80mm thermal receipts with transaction ref #, customer mobile, principal amount, fee, and store branding.
* **Void & Audit Security**: Erroneous records can be voided with Owner PIN protection.

### 2. ⚡ 4 Zero-Friction Auto-Fill Methods (100% Fool-Proof)
Say goodbye to manual typing errors during busy store hours:
* 📸 **AI Camera Scanner (Screen, SMS & Receipts)**: Uses Google ML Kit on-device OCR to read customer phone screens, SMS alerts, or paper notes, automatically extracting:
  * 💵 Amount sent (e.g. `₱1,000.00`)
  * 📱 Customer Mobile Number (`09XXXXXXXXX`)
  * 🔢 GCash Reference No. (e.g. `Ref No. 1002 9384 1029`)
  * 👤 Sender Name
* 🔲 **Customer QR Code Scanner**: Offline ZXing engine scans the customer's personal "Receive Money" QR code from the camera or photo gallery, instantly populating their 11-digit mobile number and registered name.
* 📋 **1-Tap "Paste from GCash SMS"**: Cashiers tap 1 button to paste incoming GCash notifications; intelligent regex patterns automatically sort each value into its correct field.
* 👥 **Quick-Select Frequent Customers**: 1-tap customer directory querying past transactions with live search. Tapping a neighborhood regular instantly fills their name and number.
* 🛡️ **Interactive Verification Preview**: Detected details pass through an editable verification sheet before populating the form, with strict 11-digit validation (`09XXXXXXXXX`).

### 3. 📦 Preloaded Filipino Sari-Sari Store Inventory
* Preloaded with **60+ ubiquitous Philippine staples** complete with realistic retail prices, wholesale costs, starting stocks, and reorder thresholds:
  * 🍜 **Noodles**: Lucky Me! Pancit Canton (Kalamansi, Chilimansi, Original, Sweet & Spicy, Hot Chili), Mami, Payless.
  * ☕ **Coffee & Drinks**: Kopiko Blanca/Brown/Black, Nescafe 3-in-1, Nescafe Classic, Milo, Bear Brand, Tang.
  * 🥤 **Beverages**: Coke Mismo, Sprite Mismo, Royal Mismo, Mountain Dew, Coke 1.5L, Nature's Spring, C2, Red Horse Beer, San Miguel Pale Pilsen.
  * 🥫 **Canned Goods**: 555 Sardines, Mega Green Chili, Ligo Red, Argentina Corned Beef, CDO Karne Norte, Century Tuna, CDO Meat Loaf, Purefoods Corned Beef, San Marino Corned Tuna.
  * 🥨 **Snacks**: Piattos, Nova, Chippy BBQ, Oishi Prawn Crackers, Clover Chips, Ding Dong Mix, SkyFlakes, Rebisco, Fudgee Barr, Choc-Nut.
  * 🧂 **Condiments**: Silver Swan Soy Sauce, Datu Puti Vinegar, Mang Tomas, UFC Ketchup, Magic Sarap, Ajinomoto MSG, Knorr Cubes, Mantika, Asin, Sugar.
  * 🍚 **Grains & Poultry**: Sinandomeng Rice 1kg, Dinorado Special Rice 1kg, Fresh Eggs (Large, Medium).
  * 🧼 **Personal Care**: Safeguard White Soap, Palmolive Shampoo, Sunsilk Green, Head & Shoulders, Colgate, Modess.
  * 🧺 **Laundry & Household**: Surf Powder, Ariel Powder, Champion Bar, Downy Sunrise Fresh, Joy Dishwashing, Zonrox Bleach, Posporo, Kandila.
  * 🚬 **Tobacco & Candies**: Marlboro Red/Gold, Winston Red, Fortune Blue, Maxx Menthol, Mentos Mint, Kopiko Coffee Candy.

### 4. 🛒 Point of Sale (POS) Checkout & Smart Cart
* **Live Product Catalog & Category Tabs**: Filter products in 1 tap or use real-time search.
* **Stock Clamping**: Cart quantities are locked to actual shelf stock to prevent overselling.
* **Unlisted / Tingi Item Entry**: Quickly ring up seasonal or tingi items with a custom name and price.
* **Fast Cash Tender Chips**: 1-tap change calculation (`Exact`, `₱50`, `₱100`, `₱200`, `₱500`, `₱1,000`).
* **Multi-Payment Checkout**: Cash, GCash, or Store Credit (*Utang / Listahan*).
* **Cart Park & Resume**: Hold a customer's order to assist another waiting customer, then resume in 1 tap.
* **On-Screen Monospace Thermal Preview**: See exactly how the 58mm or 80mm slip will print before sending to hardware.

### 5. 🖨️ Thermal Printer & Receipt Customization
* **Bluetooth & USB OTG Support**: Compatible with all standard 58mm (32 cols) and 80mm (48 cols) mobile POS thermal printers.
* **Custom Store Branding**: Set Store Name, Tagline, Address, Contact #, and Custom Return Policies.
* **GCash QR Code on Receipts**: Upload your personal or store QR code image to print crisp dithered QR codes directly on customer receipts.

### 6. 📊 End-of-Day Z-Reading & Business Analytics
* **Daily Z-Reading**: Calculates daily gross revenue, tender breakdowns, net profit margins, and drawer totals, with a 1-tap thermal ticket printout.
* **Customer Debt (*Utang*) Ledger**: View outstanding store credit balances and settle accounts in 1 tap.
* **CSV Export**: Export full sales history, inventory sheets, and GCash ledgers to CSV for external accounting in Excel.

### 7. 🔒 Offline Security & Antivirus Hardening
* **Owner 4-Digit PIN Security**: Protects price edits, transaction deletion, database restore, and ledger resets.
* **100% Offline-First Architecture**: Runs fully without internet. All data resides in local SQLite storage.
* **Production Signed**: Signed with a dedicated 2048-bit RSA keystore using **v1 (JAR)** and **v2 (Full APK Signature)** to ensure clean installation without Google Play Protect or antivirus warnings.

---

## 📥 Installation

1. Download [`MorpsPOS v1.0-release.apk`](https://github.com/cmorpe7-ui/MorpsPOS/raw/main/MorpsPOS%20v1.0-release.apk).
2. Open the downloaded APK on your Android phone or tablet (Android 5.0 Lollipop or newer).
3. If prompted, allow **Install Unknown Apps** for your file manager or browser.
4. Tap **Install** and open **Morps POS**!

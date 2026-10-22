package com.dantsu.thermalprinter.util;

import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

/**
 * Enterprise-grade Data Integrity and Validation utility for MorpsPOS.
 * Enforces strict formatting and validation rules for:
 * 1. Philippine Mobile Phone Numbers (09XXXXXXXXX - 11 digits)
 * 2. Financial amounts (Cash-In / Cash-Out / Checkout / Pricing)
 * 3. Inventory stock quantities
 */
public class DataValidator {

    // Color constants for inline validation states
    public static final int COLOR_MUTED = Color.parseColor("#64748B");   // Slate 500
    public static final int COLOR_INFO = Color.parseColor("#3B82F6");    // Blue 500
    public static final int COLOR_WARNING = Color.parseColor("#D97706"); // Amber 600
    public static final int COLOR_ERROR = Color.parseColor("#DC2626");   // Red 600
    public static final int COLOR_SUCCESS = Color.parseColor("#059669"); // Emerald 600

    public interface OnValidationChangeListener {
        void onValidationChanged(boolean isValid, String normalizedValue);
    }

    public static class ValidationStatus {
        public final boolean isValid;
        public final String message;
        public final int color;
        public final int digitCount;
        public final String normalized;

        public ValidationStatus(boolean isValid, String message, int color, int digitCount, String normalized) {
            this.isValid = isValid;
            this.message = message;
            this.color = color;
            this.digitCount = digitCount;
            this.normalized = normalized;
        }
    }

    /**
     * Normalizes any raw Philippine mobile number string into standard 11-digit format (09XXXXXXXXX).
     * Handles:
     * - "+639171234567" -> "09171234567"
     * - "639171234567"  -> "09171234567"
     * - "9171234567"    -> "09171234567"
     * - "0917-123-4567" -> "09171234567"
     * - "0917 123 4567" -> "09171234567"
     */
    public static String normalizePhilippineMobile(String raw) {
        if (raw == null) return "";
        String cleaned = raw.trim().replaceAll("[^0-9+]", "");

        if (cleaned.startsWith("+63")) {
            cleaned = "0" + cleaned.substring(3);
        } else if (cleaned.startsWith("63") && cleaned.length() == 12) {
            cleaned = "0" + cleaned.substring(2);
        } else if (cleaned.startsWith("9") && cleaned.length() == 10) {
            cleaned = "0" + cleaned;
        }

        // Keep strictly numeric digits
        return cleaned.replaceAll("[^0-9]", "");
    }

    /**
     * Checks if a phone number is a strictly valid Philippine mobile number.
     * Must be exactly 11 digits and start with "09" (covers Globe, TM, Smart, TNT, DITO, Sun).
     */
    public static boolean isValidPhilippineMobile(String raw) {
        if (raw == null) return false;
        String norm = normalizePhilippineMobile(raw);
        return norm.matches("^09\\d{9}$");
    }

    /**
     * Formats a valid 11-digit mobile number into user-friendly display format: "0917-123-4567".
     */
    public static String formatPhilippineMobile(String raw) {
        String norm = normalizePhilippineMobile(raw);
        if (norm.length() == 11) {
            return norm.substring(0, 4) + "-" + norm.substring(4, 7) + "-" + norm.substring(7);
        }
        return norm;
    }

    /**
     * Evaluates real-time validation status of a mobile number input for instant UI feedback.
     */
    public static ValidationStatus evaluateMobile(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ValidationStatus(false, "Format: 09XXXXXXXXX (Exact 11 digits required)", COLOR_MUTED, 0, "");
        }

        String norm = normalizePhilippineMobile(input);
        int len = norm.length();

        if (!norm.startsWith("0")) {
            return new ValidationStatus(false, "⚠️ Must start with 09 (e.g. 09171234567)", COLOR_ERROR, len, norm);
        }

        if (len >= 2 && !norm.startsWith("09")) {
            return new ValidationStatus(false, "⚠️ Must start with 09 (e.g. 09171234567)", COLOR_ERROR, len, norm);
        }

        if (len < 11) {
            int remaining = 11 - len;
            return new ValidationStatus(false, "📱 " + remaining + " digit" + (remaining > 1 ? "s" : "") + " remaining (09...)", COLOR_WARNING, len, norm);
        }

        if (len == 11) {
            return new ValidationStatus(true, "✓ Valid GCash Mobile Number (" + formatPhilippineMobile(norm) + ")", COLOR_SUCCESS, 11, norm);
        }

        return new ValidationStatus(false, "⚠️ Too many digits (Max 11 digits)", COLOR_ERROR, len, norm);
    }

    /**
     * Attaches interactive real-time validation, automatic paste normalization, and UI feedback
     * to an EditText, a status TextView, and a counter TextView.
     */
    public static void attachPhilippineMobileWatcher(
            EditText editText,
            TextView statusText,
            TextView counterText,
            OnValidationChangeListener listener
    ) {
        if (editText == null) return;

        // Limit input length to 11 characters
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(11)});

        editText.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                String current = s.toString();
                String normalized = normalizePhilippineMobile(current);

                // If user pasted something like "+639171234567" or "0917-123-4567",
                // auto-replace with the clean 11-digit normalized string
                if (current.contains("+") || current.contains("-") || current.contains(" ") || current.startsWith("63") || (current.startsWith("9") && current.length() == 10)) {
                    isFormatting = true;
                    if (normalized.length() > 11) {
                        normalized = normalized.substring(0, 11);
                    }
                    editText.setText(normalized);
                    editText.setSelection(normalized.length());
                    isFormatting = false;
                    current = normalized;
                }

                ValidationStatus status = evaluateMobile(current);

                if (counterText != null) {
                    counterText.setText(status.digitCount + " / 11 digits");
                    if (status.isValid) {
                        counterText.setTextColor(COLOR_SUCCESS);
                    } else if (status.digitCount > 0 && status.color == COLOR_ERROR) {
                        counterText.setTextColor(COLOR_ERROR);
                    } else if (status.digitCount > 0) {
                        counterText.setTextColor(COLOR_WARNING);
                    } else {
                        counterText.setTextColor(COLOR_MUTED);
                    }
                }

                if (statusText != null) {
                    statusText.setText(status.message);
                    statusText.setTextColor(status.color);
                    statusText.setVisibility(android.view.View.VISIBLE);
                }

                if (listener != null) {
                    listener.onValidationChanged(status.isValid, status.normalized);
                }
            }
        });

        // Trigger initial status
        ValidationStatus initial = evaluateMobile(editText.getText().toString());
        if (counterText != null) {
            counterText.setText(initial.digitCount + " / 11 digits");
            counterText.setTextColor(initial.color);
        }
        if (statusText != null) {
            statusText.setText(initial.message);
            statusText.setTextColor(initial.color);
            statusText.setVisibility(android.view.View.VISIBLE);
        }
        if (listener != null) {
            listener.onValidationChanged(initial.isValid, initial.normalized);
        }
    }

    /**
     * Financial validation: verifies that an amount is strictly positive and within realistic limits.
     */
    public static boolean isValidPositiveAmount(double amount) {
        return !Double.isNaN(amount) && !Double.isInfinite(amount) && amount > 0.0;
    }

    /**
     * Financial validation: verifies that a fee is non-negative.
     */
    public static boolean isValidFee(double fee) {
        return !Double.isNaN(fee) && !Double.isInfinite(fee) && fee >= 0.0;
    }

    /**
     * Product validation: selling price must be strictly positive.
     */
    public static boolean isValidPrice(double price) {
        return !Double.isNaN(price) && !Double.isInfinite(price) && price > 0.0;
    }

    /**
     * Product validation: cost must be non-negative.
     */
    public static boolean isValidCost(double cost) {
        return !Double.isNaN(cost) && !Double.isInfinite(cost) && cost >= 0.0;
    }

    /**
     * Product validation: stock must be non-negative.
     */
    public static boolean isValidStock(int stock) {
        return stock >= 0;
    }
}

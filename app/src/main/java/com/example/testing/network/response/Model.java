package com.example.testing.network.response;

public class Model {
    private String id;
    private String name;
    private String description;
    private int context_length;
    private Pricing pricing;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getContextLength() { return context_length; }
    public Pricing getPricing() { return pricing; }

    // Helper to format price per 1M tokens for readability
    public String getFormattedPricing() {
        if (pricing == null) return "Pricing unavailable";

        try {
            double promptPrice = Double.parseDouble(pricing.getPrompt()) * 1000000;
            double completionPrice = Double.parseDouble(pricing.getCompletion()) * 1000000;

            return String.format("Input: $%.2f/1M | Output: $%.2f/1M", promptPrice, completionPrice);
        } catch (NumberFormatException | NullPointerException e) {
            return "Pricing unavailable";
        }
    }

    @Override
    public String toString() {
        return id; // Important for AutoCompleteTextView
    }
}
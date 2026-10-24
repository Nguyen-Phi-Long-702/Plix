package com.longvuong.plix.presentation.transaction;

public class CategorySuggestionUiModel {
    public final String label;
    public final boolean lowConfidence;
    public final String categoryName;
    public final int confidencePercent;

    public CategorySuggestionUiModel(String label, boolean lowConfidence, String categoryName, int confidencePercent) {
        this.label = label;
        this.lowConfidence = lowConfidence;
        this.categoryName = categoryName;
        this.confidencePercent = confidencePercent;
    }
}
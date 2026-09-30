package com.longvuong.plix.presentation.transaction;

public class CategorySuggestionUiModel {
    public final String label;
    public final boolean lowConfidence;

    public CategorySuggestionUiModel(String label, boolean lowConfidence) {
        this.label = label;
        this.lowConfidence = lowConfidence;
    }
}
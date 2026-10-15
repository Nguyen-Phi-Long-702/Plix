package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ParseNlRequestDto {
    public final String text;

    //true = người dùng đang bật "Cho phép dùng Gemini dự phòng" trong Cài đặt. Backend chỉ được gọi Gemini khi true
    @SerializedName("allow_gemini")
    public final boolean allowGemini;

    public ParseNlRequestDto(String text, boolean allowGemini) {
        this.text = text;
        this.allowGemini = allowGemini;
    }
}
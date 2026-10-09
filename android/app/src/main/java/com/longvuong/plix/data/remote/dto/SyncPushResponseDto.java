package com.longvuong.plix.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SyncPushResponseDto {
    @SerializedName("upserted_ids")
    public List<String> upsertedIds;

    @SerializedName("rejected")
    public List<RejectedRecord> rejected;

    public static class RejectedRecord {
        @SerializedName("id")
        public String id;

        @SerializedName("reason")
        public String reason;
    }
}
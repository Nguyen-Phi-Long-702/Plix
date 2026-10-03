package com.longvuong.plix.data.sync;

import androidx.annotation.Nullable;

//Một bản ghi vừa được kéo về và ghi vào Room: before = bản cũ trên máy (null nếu là bản ghi mới), after = bản vừa ghi
public class EntityChange<E> {
    @Nullable
    public final E before;

    public final E after;

    public EntityChange(@Nullable E before, E after) {
        this.before = before;
        this.after = after;
    }
}
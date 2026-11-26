package com.longvuong.plix.domain.usecase.reminder;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import javax.inject.Inject;

//Số mili giây từ `now` tới lần `target` kế tiếp nghiêm ngặt sau `now` (FIN-10).
//Tính bằng ZonedDateTime nên đúng cả khi chuyển giờ mùa hè
public class ComputeNextTriggerUseCase {
    @Inject
    public ComputeNextTriggerUseCase() {
    }

    public long execute(ZonedDateTime now, LocalTime target) {
        ZonedDateTime next = now.toLocalDate().atTime(target).atZone(now.getZone());
        //target trùng đúng giờ hiện tại hoặc đã qua thì tính sang ngày mai
        if (!next.isAfter(now)) {
            next = now.toLocalDate().plusDays(1).atTime(target).atZone(now.getZone());
        }
        return Duration.between(now, next).toMillis();
    }
}

package com.longvuong.plix.presentation.budget;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BudgetProgressLevelTest {
    @Test
    public void of_belowThreshold_isNormal() {
        assertEquals(BudgetProgressLevel.NORMAL, BudgetProgressLevel.of(0, 80));
        assertEquals(BudgetProgressLevel.NORMAL, BudgetProgressLevel.of(79, 80));
    }

    @Test
    public void of_atOrAboveThresholdBelow100_isWarning() {
        assertEquals(BudgetProgressLevel.WARNING, BudgetProgressLevel.of(80, 80));
        assertEquals(BudgetProgressLevel.WARNING, BudgetProgressLevel.of(99, 80));
    }

    @Test
    public void of_atOrAbove100_isDanger() {
        assertEquals(BudgetProgressLevel.DANGER, BudgetProgressLevel.of(100, 80));
        assertEquals(BudgetProgressLevel.DANGER, BudgetProgressLevel.of(150, 80));
    }

    @Test
    public void of_threshold100_reaching100IsDanger() {
        assertEquals(BudgetProgressLevel.NORMAL, BudgetProgressLevel.of(99, 100));
        assertEquals(BudgetProgressLevel.DANGER, BudgetProgressLevel.of(100, 100));
    }
}
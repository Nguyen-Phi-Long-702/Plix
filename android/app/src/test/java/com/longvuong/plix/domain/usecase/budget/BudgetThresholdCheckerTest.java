package com.longvuong.plix.domain.usecase.budget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BudgetThresholdCheckerTest {
    private final BudgetThresholdChecker checker = new BudgetThresholdChecker();
    private static final long LIMIT = 1_000_000L;
    private static final int THRESHOLD = 80;

    @Test
    public void justCrossedThreshold_beforeZeroAfterZero_returnsFalse() {
        assertFalse(checker.justCrossedThreshold(LIMIT, THRESHOLD, 0, 0));
    }

    @Test
    public void justCrossedThreshold_before79AfterStill79_returnsFalse() {
        assertFalse(checker.justCrossedThreshold(LIMIT, THRESHOLD, 790_000, 790_000));
    }

    @Test
    public void justCrossedThreshold_before79AfterExactly80_returnsTrue() {
        assertTrue(checker.justCrossedThreshold(LIMIT, THRESHOLD, 790_000, 800_000));
    }

    @Test
    public void justCrossedThreshold_alreadyAtThresholdBefore_doesNotNotifyAgain() {
        assertFalse(checker.justCrossedThreshold(LIMIT, THRESHOLD, 800_000, 850_000));
    }

    @Test
    public void justCrossedThreshold_before79AfterExactly100_returnsTrue() {
        assertTrue(checker.justCrossedThreshold(LIMIT, THRESHOLD, 790_000, 1_000_000));
    }

    @Test
    public void justCrossedThreshold_alreadyOver100Before_doesNotNotifyAgain() {
        assertFalse(checker.justCrossedThreshold(LIMIT, THRESHOLD, 1_100_000, 1_300_000));
    }

    @Test
    public void justCrossedThreshold_limitAmountZero_returnsFalseWithoutDivideByZero() {
        assertFalse(checker.justCrossedThreshold(0, THRESHOLD, 0, 500_000));
    }
}
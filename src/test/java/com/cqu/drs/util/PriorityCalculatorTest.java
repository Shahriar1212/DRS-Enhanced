package com.cqu.drs.util;

import com.cqu.drs.model.DisasterType;
import com.cqu.drs.model.SeverityLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriorityCalculatorTest {

    @Test
    void higherSeverityProducesHigherScoreForSameDisasterType() {
        int low = PriorityCalculator.calculate(DisasterType.FIRE, SeverityLevel.LOW);
        int high = PriorityCalculator.calculate(DisasterType.FIRE, SeverityLevel.HIGH);
        assertTrue(high > low);
    }

    @Test
    void moreUrgentDisasterTypeProducesHigherScoreForSameSeverity() {
        int fire = PriorityCalculator.calculate(DisasterType.FIRE, SeverityLevel.MEDIUM);
        int earthquake = PriorityCalculator.calculate(DisasterType.EARTHQUAKE, SeverityLevel.MEDIUM);
        assertTrue(earthquake > fire);
    }

    @Test
    void scoreMatchesFormula() {
        int score = PriorityCalculator.calculate(DisasterType.FIRE, SeverityLevel.HIGH);
        assertEquals(SeverityLevel.HIGH.getWeight() * 10 + PriorityCalculator.typeWeight(DisasterType.FIRE) * 5, score);
    }

    @Test
    void nullSeverityThrows() {
        assertThrows(IllegalArgumentException.class, () -> PriorityCalculator.calculate(DisasterType.FIRE, null));
    }
}

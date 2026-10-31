package com.unciv.testing.pure.domain.mapgeneration

import com.unciv.pure.domain.mapgeneration.LatitudeRange
import org.junit.Assert.*
import org.junit.Test

/** Географический диапазон не зависит от движка, числа клеток и случайности. */
class LatitudeRangeTest {
    /** Верх и низ соответствуют краям изображения, середина — середине диапазона. */
    @Test fun projectsNorthernAndSouthernBands() {
        for (range in listOf(LatitudeRange(60.0, 20.0), LatitudeRange(-20.0, -60.0))) {
            assertEquals(range.top, range.at(10.0, -10.0, 10.0), 0.0)
            assertEquals(range.bottom, range.at(-10.0, -10.0, 10.0), 0.0)
            assertEquals((range.top + range.bottom) / 2, range.at(0.0, -10.0, 10.0), 0.0)
        }
    }

    /** Диапазон может пересекать экватор или задавать постоянную широту всей карты. */
    @Test fun supportsEquatorAndConstantLatitude() {
        assertEquals(0.0, LatitudeRange(20.0, -20.0).at(5.0, 0.0, 10.0), 0.0)
        assertEquals(35.0, LatitudeRange(35.0, 35.0).at(7.0, 0.0, 10.0), 0.0)
    }

    /** Одна строка и погрешность её Float-координат не создают перепад от полюса до полюса. */
    @Test fun singleRowUsesMidpoint() {
        val range = LatitudeRange(60.0, 20.0)
        assertEquals(40.0, range.at(2.0, 2.0, 2.0), 0.0)
        assertEquals(40.0, range.at(0.00001, -0.00001, 0.00001), 0.0)
    }

    /** Неконечные, перевёрнутые и выходящие за пределы широты отклоняются. */
    @Test fun rejectsInvalidRangesAndCoordinates() {
        for ((top, bottom) in listOf(91.0 to 0.0, 0.0 to -91.0, 20.0 to 60.0,
            Double.NaN to 0.0, 0.0 to Double.NEGATIVE_INFINITY)) {
            try { LatitudeRange(top, bottom); fail("Invalid range accepted") }
            catch (_: IllegalArgumentException) { }
        }
        try { LatitudeRange(60.0, 20.0).at(11.0, 0.0, 10.0); fail("Invalid coordinate accepted") }
        catch (_: IllegalArgumentException) { }
    }
}

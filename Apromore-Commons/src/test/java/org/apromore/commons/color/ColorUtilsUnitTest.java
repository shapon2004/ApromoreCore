/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2018 - 2021 Apromore Pty Ltd.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
package org.apromore.commons.color;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;

import org.apromore.commons.color.ColorUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ColorUtilsUnitTest {

    private static Stream<String> VALID_HEXCOLORS() {
        return Stream.of(
                "#000000",
                "#ffffff",
                "#000",
                "#fff",
                "#1a1a1a",
                "#1A1A1A",
                "#CCC"
        );
    }

    private static Stream<String> INVALID_HEXCOLORS() {
        return Stream.of(
                "123456", // prefixed with a #
                "#aabbzz", // invalid hex digit
                "#-?./~!", // invalid hex digit
                "#1234567", // only 3 or 6 digits
                "#12", // only 3 or 6 digits
                " ",  // space
                "" // empty
        );
    }

    @ParameterizedTest(name = "#{index} - Test valid hex color: {0}")
    @MethodSource("VALID_HEXCOLORS")
    void isValidHexColor_ShouldDetectValidHexColors(String color) {
        assertTrue(ColorUtils.isValidHexColor(color));
    }

    @ParameterizedTest(name = "#{index} - Test invalid hex color: {0}")
    @MethodSource("INVALID_HEXCOLORS")
    void isValidHexColor_ShouldDetectInvalidHexColors(String color) {
        assertFalse(ColorUtils.isValidHexColor(color));
    }

}

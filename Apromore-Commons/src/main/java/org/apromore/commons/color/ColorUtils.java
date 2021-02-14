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

import java.util.regex.Pattern;

public final class ColorUtils {

    private static final String HEXCOLOR_REGEXP = "^#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})$";
    private static final Pattern hexColorPattern = Pattern.compile(HEXCOLOR_REGEXP);

    /**
     * Test hex color code #123456 (6-digit) or #123 (3-digit)
     *
     * @param hexColorCode A date string
     * @return true or false
     */
    public static boolean isValidHexColor(final String hexColorCode) {
        return hexColorPattern.matcher(hexColorCode).matches();
    }
}

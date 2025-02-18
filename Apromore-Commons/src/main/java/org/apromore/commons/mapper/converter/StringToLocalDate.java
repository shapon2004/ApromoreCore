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
package org.apromore.commons.mapper.converter;

import java.time.LocalDate;

import com.github.dozermapper.core.DozerConverter;


public class StringToLocalDate extends DozerConverter<String, LocalDate> {
  
    public StringToLocalDate() {
        super(String.class, LocalDate.class);
    }

    @Override
    public LocalDate convertTo(String source, LocalDate destination) {
    LocalDate localDate = LocalDate.parse(source);
    return localDate;
}

@Override
public String convertFrom(LocalDate source, String destination) {

    return source.toString();
}

}

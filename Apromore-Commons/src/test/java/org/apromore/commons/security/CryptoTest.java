/*-
 * #%L
 * This file is part of "Apromore Core".
 * %%
 * Copyright (C) 2018 - 2020 Apromore Pty Ltd.
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
package org.apromore.commons.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class CryptoTest {

  @Test
  public void testCrypto() {
    // Given
    Crypto crypto = new Crypto("aksX9g9oOL0B", true);

    // When
    // password is MAcri
    String password = crypto.getPassword("2kAvhKEZkyCSF1VLCLeMWg==");

    // Then
    assertThat(password).isEqualTo("MAcri");
  }
  
  @Test
  public void testCryptoRandom() {
    // Given
    Crypto crypto = new Crypto("aksX9g9oOL0B", true);

    // When
    // password is MAcri
    String password = crypto.getPassword("5+2WGkfWzljNTIPDldDilJSuN9Agaygv"); 
    // Then
    assertThat(password).isEqualTo("7fHJV41fpJ");
  }

}

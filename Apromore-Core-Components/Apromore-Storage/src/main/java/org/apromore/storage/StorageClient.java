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
package org.apromore.storage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apromore.storage.exception.ObjectCreationException;
import org.apromore.storage.exception.ObjectNotFoundException;

/**
 * @author nolantellis This is the main interface which is used to upload or
 *         download file from a storage management.
 * 
 */

public interface StorageClient {
    int COPY_BUFFER_SIZE = 8192;
    String getStorageType();
    InputStream getInputStream(String prefix, String key) throws ObjectNotFoundException;
    OutputStream getOutputStream(String prefix, String key) throws ObjectCreationException;
    boolean delete(String prefix, String key);
    boolean delete(String prefix) throws IllegalAccessException;
    List<String> listObjects(String prefix);

    default String getValidPrefix(String prefix) {
	prefix = prefix == null ? "" : prefix;
	return prefix;
    }

    default void copyStream(InputStream sourceFile, OutputStream targetFile) throws Exception {
    byte[] buf = new byte[COPY_BUFFER_SIZE];
    int length;
    while ((length = sourceFile.read(buf)) > 0) {
        targetFile.write(buf, 0, length);
    }
    sourceFile.close();
    targetFile.close();
    }
}

/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.spi.types.PathItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * A {@linkplain TranslationMapping} maps canonical information about business objects to SQL, i.e. it contains field
 * names of an object and maps them to their SQL schema column name.
 * <p>
 * Essentially, it contains a map that links field name to column name. In case the field is a complex object, it in
 * turn must be represented by another {@link TranslationMapping} in order to recursively resolve field name.
 */
public abstract class TranslationMapping {
    protected final Map<String, Object> fieldMap = new HashMap<>();

    /**
     * Converts a field/property from the canonical model into its SQL column equivalent. If the canonical property name
     * is nested, e.g. {@code something.other.thing} then the mapper attempts to descend recursively into the tree until
     * the correct mapping is found, or throws an exception if not found.
     *
     * @throws IllegalArgumentException if the canonical property name was not found.
     */
    public String getStatement(String canonicalPropertyName, Class<?> type) {
        if (canonicalPropertyName == null) {
            throw new IllegalArgumentException(format("Translation failed for Model '%s' input path is null", getClass().getName()));
        }

        return getStatement(PathItem.parse(canonicalPropertyName), type);
    }

    public String getStatement(List<PathItem> path, Class<?> type) {
        var key = path.get(0);
        var entry = fieldMap.get(key.toString());
        if (entry == null) {
            return null;
        }

        if (entry instanceof TranslationMapping mappingEntry) {
            var remainingPath = path.stream().skip(1).toList();
            return mappingEntry.getStatement(remainingPath, type);
        }

        return entry.toString();
    }

    protected void add(String fieldId, Object value) {
        fieldMap.put(fieldId, value);
    }
}

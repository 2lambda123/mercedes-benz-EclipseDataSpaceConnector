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

package org.eclipse.edc.util.collection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentLruCacheTest {
    private final ConcurrentLruCache<String, String> cache = new ConcurrentLruCache<>(2);

    @Test
    void verifyEviction() {
        cache.put("foo", "foo");
        cache.put("bar", "bar");
        assertThat(cache)
                .containsKey("foo")
                .containsKey("bar");

        cache.put("baz", "baz");
        assertThat(cache)
                .containsKey("baz")
                .containsKey("bar")
                .doesNotContainKey("foo");
    }
}

/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.transform;

import jakarta.json.Json;
import org.eclipse.edc.protocol.dsp.catalog.transform.from.JsonObjectFromCatalogRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.catalog.transform.to.JsonObjectToCatalogRequestMessageTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

/**
 * Provides the transformers for catalog message types via the {@link TypeTransformerRegistry}.
 */
@Extension(value = DspCatalogTransformExtension.NAME)
public class DspCatalogTransformExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Catalog Transform Extension";

    @Inject
    private TypeTransformerRegistry registry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var jsonFactory = Json.createBuilderFactory(Map.of());

        registry.register(new JsonObjectFromCatalogRequestMessageTransformer(jsonFactory));
        registry.register(new JsonObjectToCatalogRequestMessageTransformer());
    }
}

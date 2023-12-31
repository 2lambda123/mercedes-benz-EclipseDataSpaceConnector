/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.policy.transform;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_POLICY;
import static org.eclipse.edc.connector.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_PRIVATE_PROPERTIES;

public class JsonObjectToPolicyDefinitionTransformer extends AbstractJsonLdTransformer<JsonObject, PolicyDefinition> {

    public JsonObjectToPolicyDefinitionTransformer() {
        super(JsonObject.class, PolicyDefinition.class);
    }

    @Override
    public @Nullable PolicyDefinition transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var builder = PolicyDefinition.Builder.newInstance();
        builder.id(nodeId(input));
        visitProperties(input, (key, value) -> transformProperties(key, value, builder, context));
        return builder.build();
    }

    private void transformProperties(String key, JsonValue jsonValue, PolicyDefinition.Builder builder, TransformerContext context) {
        switch (key) {
            case EDC_POLICY_DEFINITION_POLICY ->
                    transformArrayOrObject(jsonValue, Policy.class, builder::policy, context);
            case EDC_POLICY_DEFINITION_PRIVATE_PROPERTIES -> {
                var props = jsonValue.asJsonArray().getJsonObject(0);
                visitProperties(props, (k, val) -> transformProperties(k, val, builder, context));
            }
            default -> {
                builder.privateProperty(key, transformGenericProperty(jsonValue, context));
            }
        }

    }
}

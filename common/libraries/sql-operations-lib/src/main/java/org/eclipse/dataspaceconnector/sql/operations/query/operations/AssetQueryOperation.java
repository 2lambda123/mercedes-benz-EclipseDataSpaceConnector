/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.sql.operations.query.operations;

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.sql.operations.mapper.IdMapper;
import org.eclipse.dataspaceconnector.sql.operations.mapper.PropertyMapper;
import org.eclipse.dataspaceconnector.sql.operations.serializer.EnvelopePacker;
import org.eclipse.dataspaceconnector.sql.operations.types.Property;
import org.eclipse.dataspaceconnector.sql.operations.util.PreparedStatementResourceReader;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

public class AssetQueryOperation implements QueryOperation<Asset> {

    private final Map<String, Object> filters;

    public AssetQueryOperation(@NotNull Map<String, Object> filters) {
        this.filters = filters;
    }

    @NotNull
    public List<Asset> invoke(Connection connection) throws SQLException {

        String sqlQueryTemplate = PreparedStatementResourceReader.readAssetQuery();
        String sqlPropertiesByKv = PreparedStatementResourceReader.readPropertiesSelectByKv();
        String sqlPropertiesByAssetId = PreparedStatementResourceReader.readPropertiesSelectByAssetId();
        String sqlAssetsAll = PreparedStatementResourceReader.readAssetSelectAll();

        List<String> targetAssetIds;
        if (filters.isEmpty()) {
            targetAssetIds = executeQuery(connection, new IdMapper(), sqlAssetsAll);
        } else {
            StringBuilder sqlQuery = new StringBuilder(sqlQueryTemplate);
            // start with 1, because the query template already contains 1 WHERE clause
            for (int i = 1; i < filters.size(); i++) {
                sqlQuery.append(" AND asset_id IN ( ").append(sqlPropertiesByKv).append(" )");
            }

            List<Object> arguments = new ArrayList<>();
            filters.forEach((key, value) -> {
                arguments.add(key);
                arguments.add(EnvelopePacker.pack(value));
            });

            targetAssetIds = executeQuery(connection, new IdMapper(), sqlQuery.toString(), arguments.toArray());
        }

        List<Asset> assets = new ArrayList<>();

        for (String assetId : targetAssetIds) {
            List<Property> properties = executeQuery(connection, new PropertyMapper(), sqlPropertiesByAssetId, assetId);
            //noinspection unchecked
            assets.add(Asset.Builder.newInstance().properties(asMap(properties)).build());
        }

        return assets;
    }

    private Map<String, Object> asMap(List<Property> properties) {
        Map<String, Object> map = new HashMap<>();
        properties.forEach(p -> map.put(p.getKey(), p.getValue()));
        return map;
    }
}

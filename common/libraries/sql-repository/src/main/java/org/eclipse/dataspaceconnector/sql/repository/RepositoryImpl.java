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

package org.eclipse.dataspaceconnector.sql.repository;

import org.eclipse.dataspaceconnector.sql.repository.operation.CreateOperation;
import org.eclipse.dataspaceconnector.sql.repository.operation.DeleteOperation;
import org.eclipse.dataspaceconnector.sql.repository.operation.QueryOperation;
import org.eclipse.dataspaceconnector.sql.repository.operation.UpdateOperation;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.sql.SqlClient;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class RepositoryImpl implements Repository {

    private final CreateOperation createOperation;
    private final UpdateOperation updateOperation;
    private final DeleteOperation deleteOperation;
    private final QueryOperation queryOperation;

    public RepositoryImpl(@NotNull SqlClient sqlClient) {
        Objects.requireNonNull(sqlClient);
        createOperation = new CreateOperation(sqlClient);
        updateOperation = new UpdateOperation(sqlClient);
        deleteOperation = new DeleteOperation(sqlClient);
        queryOperation = new QueryOperation(sqlClient);
    }

    @NotNull
    @Override
    public List<Asset> query(@NotNull List<Criterion> criteria) throws SQLException {
        return queryOperation.invoke(Objects.requireNonNull(criteria));
    }

    @Override
    public void create(@NotNull Asset asset) throws SQLException {
        createOperation.invoke(Objects.requireNonNull(asset));
    }

    @Override
    public void update(@NotNull Asset asset) throws SQLException {
        updateOperation.invoke(Objects.requireNonNull(asset));
    }

    @Override
    public void delete(@NotNull Asset asset) throws SQLException {
        deleteOperation.invoke(Objects.requireNonNull(asset));
    }
}

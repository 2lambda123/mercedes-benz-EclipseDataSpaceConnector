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

package org.eclipse.dataspaceconnector.sql.assetloader;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.sql.operations.SqlDataSourceExtension;
import org.eclipse.dataspaceconnector.sql.operations.address.SqlAddressQuery;
import org.eclipse.dataspaceconnector.sql.operations.asset.SqlAssetQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

@ExtendWith(SqlDataSourceExtension.class)
public class PostgresqlAssetLoaderTest {

    private AssetLoader assetLoader;
    private DataSource dataSource;

    // mocks

    @BeforeEach
    public void setup(DataSource dataSource) {
        this.dataSource = dataSource;
        assetLoader = new SqlAssetLoader(this.dataSource, new FakeTransactionContext());
    }

    @Test
    public void testAssetLoadSuccess() throws SQLException {
        Asset asset = Asset.Builder.newInstance().build();
        DataAddress dataAddress = DataAddress.Builder.newInstance().type("test").build();

        assetLoader.accept(asset, dataAddress);

        Connection connection = dataSource.getConnection();
        SqlAssetQuery assetQuery = new SqlAssetQuery(connection);
        SqlAddressQuery addressQuery = new SqlAddressQuery(connection);

        List<Asset> assets = assetQuery.execute();
        List<DataAddress> addresses = addressQuery.execute();

        Assertions.assertEquals(1, assets.size());
        Assertions.assertEquals(1, addresses.size());
    }

    private static class FakeTransactionContext implements TransactionContext {

        @Override
        public void execute(TransactionBlock block) {
            block.execute();
        }
    }
}

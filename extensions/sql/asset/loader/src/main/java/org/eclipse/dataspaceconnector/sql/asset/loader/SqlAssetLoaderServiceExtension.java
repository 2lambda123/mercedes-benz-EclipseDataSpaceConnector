/*
 *  Copyright (c) 2021-2022 Daimler TSS GmbH
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

package org.eclipse.dataspaceconnector.sql.asset.loader;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;

import java.util.Objects;
import javax.sql.DataSource;

public class SqlAssetLoaderServiceExtension implements ServiceExtension {

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Override
    public void initialize(ServiceExtensionContext context) {
        String dataSourceName = context.getConfig().getString(ConfigurationKeys.DATASOURCE_NAME);

        DataSource dataSource = new LazyDataSource(() -> Objects.requireNonNull(
                dataSourceRegistry.resolve(dataSourceName),
                String.format("DataSource %s could not be resolved", dataSourceName)));

        SqlAssetLoader sqlAssetLoader = new SqlAssetLoader(dataSource, transactionContext);

        context.registerService(AssetLoader.class, sqlAssetLoader);
    }
}

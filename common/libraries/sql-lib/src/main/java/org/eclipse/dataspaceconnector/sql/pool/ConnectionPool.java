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

package org.eclipse.dataspaceconnector.sql.pool;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The connection pool maintains a cache of reusable database connections.
 */
public interface ConnectionPool extends AutoCloseable {

    /**
     * Retrieves a connection managed by the pool.
     *
     * @return connection to be exclusively used until returned to the pool
     * @throws SQLException if an errors was encountered while retrieving the connection
     */
    Connection getConnection() throws SQLException;

    /**
     * Returns a provided connection back to the pool and thus makes
     * it available to be used by other consumers.
     *
     * @param connection to be returned to the pool
     * @throws SQLException if an errors was encountered while returning the connection
     */
    void returnConnection(Connection connection) throws SQLException;
}
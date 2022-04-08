/*
 *  Copyright (c) 2022 Mercedes Benz Tech Innovation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes Benz Tech Innovation - Add Configuration Keys and default value
 *
 */

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

/**
 * Defines configuration keys used by the SqlContractDefinitionStoreExtension.
 */
public interface ConfigurationKeys {

    /**
     * Name of the datasource to use for accessing contract definitions.
     */
    @EdcSetting
    String DATASOURCE_SETTING_NAME = "edc.contractdefinition.datasource.name";

    String DATASOURCE_SETTING_NAME_DEFAULT = "contractdefinition";
}

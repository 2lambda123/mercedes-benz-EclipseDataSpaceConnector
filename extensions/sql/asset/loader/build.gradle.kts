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
 *       Daimler TSS GmbH - initial API and implementation
 *
 */

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

dependencies {
    api(project(":spi"))
    api(project(":extensions:dataloading"))
    api(project(":extensions:transaction:transaction-spi"))
    api(project(":extensions:transaction:transaction-datasource-spi"))

    implementation(project(":extensions:sql:asset:schema"))
    implementation(project(":extensions:sql:common"))

    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation(project(":core:base"))
    testImplementation(project(":extensions:sql:pool:apache-commons-pool"))
    testImplementation(project(":extensions:transaction:transaction-local"))
    testImplementation("com.h2database:h2:2.1.210")
}

publishing {
    publications {
        create<MavenPublication>("sql-asset-loader") {
            artifactId = "sql-asset-loader"
            from(components["java"])
        }
    }
}

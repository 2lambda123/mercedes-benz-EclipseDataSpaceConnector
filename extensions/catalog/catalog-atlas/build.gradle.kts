/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))
    api(project(":common"))
    api(project(":extensions:schema"))

    testImplementation(project(":distributions:junit"))
}
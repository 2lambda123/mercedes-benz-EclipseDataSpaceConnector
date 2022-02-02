plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val jupiterVersion: String by project
val rsApi: String by project
val okHttpVersion: String by project

dependencies {
    implementation(project(":spi"))
    implementation(project(":extensions:transaction:transaction-datasource-spi"))
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
    api("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    implementation(project(":extensions:dataloading:dataloading-asset"))
    implementation(project(":extensions:dataloading:dataloading-contractdef"))

    implementation("org.postgresql:postgresql:42.3.1")

}

publishing {
    publications {
        create<MavenPublication>("sprint4-fake") {
            artifactId = "sprint4-fake"
            from(components["java"])
        }
    }
}

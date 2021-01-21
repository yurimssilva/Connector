val infoModelVersion: String by project

plugins {
    `java-library`
}


dependencies {
    api(project(":spi"))

    api("de.fraunhofer.iais.eis.ids.infomodel:java:${infoModelVersion}")

}



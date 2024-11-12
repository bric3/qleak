import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.plugin.allopen)
    alias(libs.plugins.shadow.jar)
    alias(libs.plugins.asciidoctor)
    application
}


version = "0.1"
group = "qleak"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        // https://search.maven.org/artifact/io.micronaut/micronaut-bom/1.2.0/pom
        mavenBom("io.micronaut:micronaut-bom:${project.ext["micronautVersion"]}") {
            bomProperties(mapOf(
                    "junit5.version" to project.ext["junitVersion"].toString(),
                    "picocli.version" to project.ext["picocliVersion"].toString(),
                    "jackson.version" to project.ext["jacksonVersion"].toString(),
                    "jackson.modules.version" to project.ext["jacksonVersion"].toString(),
                    "jackson.datatype.version" to project.ext["jacksonVersion"].toString(),
            ))
        }
    }
}

// for dependencies that are needed for development only
val developmentOnly by configurations.registering
val generateConfig by configurations.registering

dependencies {
    generateConfig("info.picocli:picocli-codegen:${project.ext["picocliVersion"]}")

    annotationProcessor("io.micronaut.configuration:micronaut-picocli")
    implementation("io.micronaut:micronaut-runtime")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${project.ext["kotlinVersion"]}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${project.ext["kotlinVersion"]}")
    implementation("info.picocli:picocli")
    implementation("io.micronaut.configuration:micronaut-picocli")
    implementation("org.fusesource.jansi:jansi:${project.ext["jansiVersion"]}")
    implementation("com.jakewharton.picnic:picnic:${project.ext["picnicVersion"]}")

    implementation("com.squareup.leakcanary:shark:${project.ext["sharkVersion"]}")
    implementation("com.squareup.leakcanary:shark-hprof:${project.ext["sharkVersion"]}")
    implementation("com.squareup.leakcanary:shark-graph:${project.ext["sharkVersion"]}")
    implementation("com.squareup.leakcanary:shark-log:${project.ext["sharkVersion"]}")

    kapt("io.micronaut:micronaut-inject-java")
    kapt("io.micronaut:micronaut-validation")
    kaptTest("io.micronaut:micronaut-inject-java")

    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("ch.qos.logback:logback-classic:${project.ext["logbackVersion"]}")

    // micraunaut graalvm-native-image
    compileOnly("com.oracle.substratevm:svm")
    annotationProcessor("io.micronaut:micronaut-graal")

    testAnnotationProcessor("io.micronaut:micronaut-inject-java")
    testImplementation("org.assertj:assertj-core:${project.ext["assertjVersion"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("io.micronaut:micronaut-inject-java")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass = "qleak.QLeakCommand"
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        javaParameters = true
        jvmTarget = JvmTarget.JVM_21
    }
}

tasks {
    test {
        classpath += developmentOnly.get()
        useJUnitPlatform()
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf(
                "--release", "11",
                "-parameters",
                "-Aproject=${project.group}/${project.name}"
        ))
    }

    val generateGraalReflectionConfig by registering(JavaExec::class) {
        dependsOn("classes")
        mainClass = "picocli.codegen.aot.graalvm.ReflectionConfigGenerator"
        classpath = generateConfig.get() + project.sourceSets.main.get().runtimeClasspath
        val outputFile = project.layout.buildDirectory
                .map {
                    it.file("resources/main/META-INF/native-image/${project.group}/${project.name}/reflect-config.json")
                }
        args = outputFile.map { listOf("--output=${it.asFile}", "qleak.QLeakCommand") }.get()
    }

    assemble {
        dependsOn(generateGraalReflectionConfig)
    }

    jar {
        manifest {
            attributes(
                    "Main-Class" to application.mainClass.get(),
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version
            )
        }
    }

    shadowJar {
        mergeServiceFiles()
//    archiveClassifier = 'shadow'
        archiveClassifier = ""
        archiveBaseName = rootProject.name
    }

    named<JavaExec>("run") {
        classpath += developmentOnly.get()
        jvmArgs("-noverify", "-XX:TieredStopAtLevel=1", "-Dcom.sun.management.jmxremote")
    }

    distTar {
        enabled = false
    }

    distZip {
        enabled = false
    }

    shadowDistTar {
        enabled = false
    }

    shadowDistZip {
        enabled = false
    }

    withType<CreateStartScripts>() {
        enabled = false
    }

    asciidoctor {
        sourceDirProperty = file("docs")
//    sources {
//        include 'toplevel.adoc', 'another.adoc', 'third.adoc'
//    }
        outputDirProperty = layout.buildDirectory.file("docs").get().asFile
    }
}

allOpen {
    annotation("io.micronaut.aop.Around")
}

asciidoctorj {
    options(mapOf("doctype" to "article"))
    attributes(mapOf("toc" to "right"))
}

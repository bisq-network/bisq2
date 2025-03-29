import bisq.gradle.tasks.VersionUtil
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("bisq.java-library")
    id("bisq.gradle.webcam_app.WebcamAppPlugin")
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.openjfx)
    alias(libs.plugins.gradle.javacpp.platform)
}

application {
    mainClass.set("bisq.webcam.WebcamAppLauncher")
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.media")
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.file("generated/src/main/resources"))
        }
    }
}

dependencies {
    implementation(libs.zxing)
    implementation(libs.sarxos)
    implementation(libs.javacv) {
        exclude(module = "ibfreenect2-platform")
        exclude(module = "ffmpeg-platform")
        exclude(module = "flycapture-platform")
        exclude(module = "spinnaker-platform")
        exclude(module = "libdc1394-platform")
        exclude(module = "libfreenect-platform")
        exclude(module = "libfreenect2-platform")
        exclude(module = "librealsense-platform")
        exclude(module = "librealsense2-platform")
        exclude(module = "videoinput-platform")
        exclude(module = "artoolkitplus-platform")
        exclude(module = "chilitags-platform")
        exclude(module = "flandmark-platform")
        exclude(module = "arrow-platform")
        exclude(module = "hdf5-platform")
        exclude(module = "hyperscan-platform")
        exclude(module = "lz4-platform")
        exclude(module = "mkl-platform")
        exclude(module = "mkl-dnn-platform")
        exclude(module = "dnnl-platform")
        exclude(module = "arpack-ng-platform")
        exclude(module = "cminpack-platform")
        exclude(module = "fftw-platform")
        exclude(module = "gsl-platform")
        exclude(module = "cpython-platform")
        exclude(module = "numpy-platform")
        exclude(module = "scipy-platform")
        exclude(module = "gym-platform")
        exclude(module = "llvm-platform")
        exclude(module = "libffi-platform")
        exclude(module = "libpostal-platform")
        exclude(module = "libraw-platform")
        exclude(module = "leptonica-platform")
        exclude(module = "tesseract-platform")
        exclude(module = "caffe-platform")
        exclude(module = "openpose-platform")
        exclude(module = "cuda-platform")
        exclude(module = "nvcodec-platform")
        exclude(module = "opencl-platform")
        exclude(module = "mxnet-platform")
        exclude(module = "pytorch-platform")
        exclude(module = "sentencepiece-platform")
        exclude(module = "tensorflow-platform")
        exclude(module = "tensorflow-lite-platform")
        exclude(module = "tensorrt-platform")
        exclude(module = "tritonserver-platform")
        exclude(module = "ale-platform")
        exclude(module = "depthai-platform")
        exclude(module = "onnx-platform")
        exclude(module = "ngraph-platform")
        exclude(module = "onnxruntime-platform")
        exclude(module = "tvm-platform")
        exclude(module = "bullet-platform")
        exclude(module = "liquidfun-platform")
        exclude(module = "qt-platform")
        exclude(module = "skia-platform")
        exclude(module = "cpu_features-platform")
        exclude(module = "modsecurity-platform")
        exclude(module = "systems-platform")
    }
}


tasks {
    named<Jar>("jar") {
        manifest {
            attributes(
                    mapOf(
                            Pair("Implementation-Title", project.name),
                            Pair("Implementation-Version", project.version),
                            Pair("Main-Class", "bisq.webcam.WebcamAppLauncher")
                    )
            )
        }
    }

    named<ShadowJar>("shadowJar") {
        val version = VersionUtil.getVersionFromFile(project)
        archiveClassifier.set("$version-all")
    }

    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }
}

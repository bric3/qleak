package qleak

import io.quarkus.test.junit.NativeImageTest

@NativeImageTest
class NativeQleakIT : QleakCommandTest()
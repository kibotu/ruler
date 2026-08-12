package com.kibotu.ruler.analysis.verification

import org.gradle.api.GradleException

class SizeExceededException(label: String, size: Long, threshold: Long) :
    GradleException("$label size is ${size - threshold} bytes above the threshold of $threshold bytes.")

package com.kibotu.ruler.common.verification

class SizeExceededException(label: String, size: Long, threshold: Long) :
    Exception("$label size exceeds the threshold by ${size - threshold} bytes.")

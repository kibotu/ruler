package com.spotify.ruler.common.veritication

class SizeExceededException(label: String, size: Long, threshold: Long) :
    Exception("$label size exceeds the threshold by ${size - threshold} bytes.")

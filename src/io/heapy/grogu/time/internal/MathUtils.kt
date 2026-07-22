package io.heapy.grogu.time.internal

internal fun addExact(first: Long, second: Long): Long {
    val result = first + second
    if (((first xor result) and (second xor result)) < 0) {
        throw ArithmeticException("long overflow")
    }
    return result
}

internal fun subtractExact(first: Long, second: Long): Long {
    val result = first - second
    if (((first xor second) and (first xor result)) < 0) {
        throw ArithmeticException("long overflow")
    }
    return result
}

internal fun multiplyExact(first: Long, second: Long): Long {
    if (first == 0L || second == 0L) return 0
    if (first == -1L && second == Long.MIN_VALUE) throw ArithmeticException("long overflow")
    if (second == -1L && first == Long.MIN_VALUE) throw ArithmeticException("long overflow")

    val result = first * second
    if (result / second != first) {
        throw ArithmeticException("long overflow")
    }
    return result
}

internal fun floorDiv(dividend: Long, positiveDivisor: Long): Long {
    val quotient = dividend / positiveDivisor
    return if (dividend % positiveDivisor < 0) quotient - 1 else quotient
}

internal fun floorMod(dividend: Long, positiveDivisor: Long): Long {
    val remainder = dividend % positiveDivisor
    return if (remainder < 0) remainder + positiveDivisor else remainder
}

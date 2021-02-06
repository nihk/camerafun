package nick.camerafun

object IdGenerator {
    private var count = 1

    fun next(): Int = count++
}
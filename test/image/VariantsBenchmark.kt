package info.vlassiev.serg.image

import org.imgscalr.Scalr
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.management.MemoryType
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * Old against new, on the same synthetic phone photos, in a JVM limited like the production pod:
 *
 *   docker run --rm --cpus 0.2 -m 384m <build-stage image> \
 *     java -XX:MaxHeapSize=126m -cp build/libs/hiking-api.jar:build/classes/kotlin/test \
 *     info.vlassiev.serg.image.VariantsBenchmarkKt old|new [photos] [width] [height]
 *
 * One JVM per run, so the two cannot disturb each other's heap. Not a unit test: it takes minutes
 * at 0.2 CPU, and its point is the numbers.
 */
fun main(args: Array<String>) {
    val mode = args.getOrElse(0) { "new" }
    val photos = args.getOrElse(1) { "3" }.toInt()
    val width = args.getOrElse(2) { "4080" }.toInt()
    val height = args.getOrElse(3) { "3072" }.toInt()
    val files = (1..photos).map { TestPhotos.jpeg(width, height) }
    println("mode=$mode photos=$photos size=${width}x$height file=${files.first().length() / 1024} KB " +
        "maxHeap=${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB cpus=${Runtime.getRuntime().availableProcessors()}")

    val heapPools = ManagementFactory.getMemoryPoolMXBeans().filter { it.type == MemoryType.HEAP }
    for ((i, file) in files.withIndex()) {
        System.gc()
        heapPools.forEach { it.resetPeakUsage() }
        val started = System.nanoTime()
        val outcome = try {
            if (mode == "old") oldVariants(file) else makeVariants(file) { _, _ -> }
            "ok"
        } catch (e: Throwable) {
            e.javaClass.simpleName
        }
        val seconds = (System.nanoTime() - started) / 1e9
        val peak = heapPools.sumByDouble { it.peakUsage.used.toDouble() } / 1024 / 1024
        println("photo ${i + 1}: %-20s %6.1f s   peak heap %5.0f MB".format(outcome, seconds, peak))
    }
}

/** ImageManager's resize() as it was until 2026-09-18, for comparison only: one full decode per variant. */
private fun oldVariants(original: File) {
    for (size in listOf(80, 2048, 1024, 800)) {
        val out = Files.createTempFile("", ".jpg").toFile()
        try {
            val image = ImageIO.read(original)
            val scaled = Scalr.resize(image, if (size < 100) Scalr.Method.SPEED else Scalr.Method.ULTRA_QUALITY, size)
            ImageIO.write(scaled, "JPEG", out)
        } finally {
            out.delete()
        }
    }
}

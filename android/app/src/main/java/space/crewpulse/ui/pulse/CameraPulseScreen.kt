package space.crewpulse.ui.pulse

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import space.crewpulse.R
import space.crewpulse.domain.PulseFromCamera
import space.crewpulse.domain.SimulatedFingertip
import space.crewpulse.ui.components.CrewCard
import space.crewpulse.ui.components.CrewScaffold
import space.crewpulse.ui.components.LineChart
import space.crewpulse.ui.theme.StatusColors

private enum class Stage { READY, MEASURING, DONE, POOR }

private const val WARM_UP_MS = 3_000L
private const val MEASURE_MS = 30_000L

/**
 * Fingertip over the rear camera with the flash on, 30 seconds; the result is saved as heart rate (source "camera").
 * Demo mode feeds a simulated fingertip signal at [demoBpm] through the same algorithm, for devices without a camera.
 */
@Composable
fun CameraPulseScreen(onSave: (Double) -> Boolean, onBack: () -> Unit, demoBpm: Double = 72.0) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    var stage by remember { mutableStateOf(Stage.READY) }
    var progress by remember { mutableFloatStateOf(0f) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var wave by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var result by remember { mutableStateOf<PulseFromCamera.Estimate?>(null) }
    var saved by remember { mutableStateOf(false) }
    val samples = remember { ArrayList<Pair<Long, Double>>() }
    var startedAt by remember { mutableLongStateOf(0L) }
    var simulated by remember { mutableStateOf(false) }

    // Camera with the torch on while measuring; frames go to the analyzer on a background thread.
    DisposableEffect(stage, granted, simulated) {
        if (stage != Stage.MEASURING || !granted || simulated) return@DisposableEffect onDispose { }
        val executor = Executors.newSingleThreadExecutor()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var camera: Camera? = null
        providerFuture.addListener({
            val provider = providerFuture.get()
            val analysis = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { image ->
                val red = meanRed(image)
                val t = System.currentTimeMillis()
                image.close()
                synchronized(samples) { if (t - startedAt >= WARM_UP_MS) samples += t to red }
            }
            runCatching {
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, analysis)
                camera?.cameraControl?.enableTorch(true)
            }.onFailure { stage = Stage.POOR }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            camera?.cameraControl?.enableTorch(false)
            runCatching { providerFuture.get().unbindAll() }
            executor.shutdown()
        }
    }

    // Demo: simulated frames at about 30 per second instead of the camera.
    LaunchedEffect(stage, simulated) {
        if (stage != Stage.MEASURING || !simulated) return@LaunchedEffect
        val finger = SimulatedFingertip(demoBpm, seed = startedAt.toInt())
        while (stage == Stage.MEASURING) {
            val t = System.currentTimeMillis()
            val value = finger.sample((t - startedAt) / 1000.0)
            synchronized(samples) { if (t - startedAt >= WARM_UP_MS) samples += t to value }
            delay(33)
        }
    }

    // Progress, live waveform and the final estimate.
    LaunchedEffect(stage) {
        while (stage == Stage.MEASURING) {
            val elapsed = System.currentTimeMillis() - startedAt
            elapsedMs = elapsed
            progress = (elapsed.toFloat() / (WARM_UP_MS + MEASURE_MS)).coerceIn(0f, 1f)
            val snapshot = synchronized(samples) { samples.toList() }
            val recent = snapshot.filter { it.first >= System.currentTimeMillis() - 12_000 }
            PulseFromCamera.filtered(recent, minSeconds = 3.0)?.takeLast(150)?.let { w ->
                wave = w.mapIndexed { i, v -> i.toFloat() to v.toFloat() }
            }
            if (elapsed >= WARM_UP_MS + MEASURE_MS) {
                val e = PulseFromCamera.estimate(snapshot)
                result = e
                stage = if (e != null && e.quality >= PulseFromCamera.MIN_QUALITY) Stage.DONE else Stage.POOR
            }
            delay(150)
        }
    }

    fun start(demo: Boolean = simulated) {
        simulated = demo
        synchronized(samples) { samples.clear() }
        wave = emptyList()
        result = null
        saved = false
        startedAt = System.currentTimeMillis()
        elapsedMs = 0L
        stage = Stage.MEASURING
    }

    CrewScaffold(title = stringResource(R.string.cam_title), onBack = onBack) { pad ->
        Column(
            pad.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CrewCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Fingerprint, null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.cam_how), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                }
            }

            if (!granted && stage == Stage.READY) {
                Text(stringResource(R.string.cam_permission), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { askPermission.launch(Manifest.permission.CAMERA) }) { Text(stringResource(R.string.cam_allow)) }
            }
            if (simulated && stage != Stage.READY) {
                Text(stringResource(R.string.cam_demo_label), style = MaterialTheme.typography.bodyMedium, color = StatusColors.yellow)
            }

            when (stage) {
                Stage.READY -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (granted) {
                        Button(onClick = { start(demo = false) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cam_start)) }
                    }
                    OutlinedButton(onClick = { start(demo = true) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cam_demo)) }
                }
                Stage.MEASURING -> CrewCard {
                    Text(
                        stringResource(if (elapsedMs < WARM_UP_MS) R.string.cam_warmup else R.string.cam_measuring),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    if (wave.size > 10) LineChart(points = wave, color = StatusColors.red, height = 90.dp)
                }
                Stage.DONE -> CrewCard {
                    val e = result!!
                    Text(stringResource(R.string.cam_result, e.bpm.roundToInt()), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(R.string.cam_quality, (e.quality * 100).roundToInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                        Button(onClick = { saved = onSave(e.bpm) }, enabled = !saved) {
                            Text(stringResource(if (saved) R.string.cam_saved else R.string.cam_save))
                        }
                        OutlinedButton(onClick = { start() }) { Text(stringResource(R.string.cam_retry)) }
                    }
                }
                Stage.POOR -> CrewCard {
                    Text(stringResource(R.string.cam_poor), style = MaterialTheme.typography.bodyLarge, color = StatusColors.yellow)
                    OutlinedButton(onClick = { start() }, modifier = Modifier.padding(top = 10.dp)) { Text(stringResource(R.string.cam_retry)) }
                }
            }

            Text(
                stringResource(R.string.cam_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

/** Mean red value of the central half of an RGBA frame, sampling every 4th pixel. */
private fun meanRed(image: ImageProxy): Double {
    val plane = image.planes[0]
    val buffer = plane.buffer
    val w = image.width
    val h = image.height
    var sum = 0L
    var count = 0
    var y = h / 4
    while (y < h * 3 / 4) {
        var x = w / 4
        while (x < w * 3 / 4) {
            sum += buffer.get(y * plane.rowStride + x * plane.pixelStride).toInt() and 0xFF
            count++
            x += 4
        }
        y += 4
    }
    return if (count == 0) 0.0 else sum.toDouble() / count
}

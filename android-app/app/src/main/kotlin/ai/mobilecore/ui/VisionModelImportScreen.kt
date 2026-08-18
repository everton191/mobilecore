package ai.mobilecore.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.io.File
import java.util.Locale

enum class VisionModelSlot(
    val defaultTitle: String,
    val taskLabel: String,
    val expectedFormat: String,
    val defaultRuntime: String
) {
    YOLO_DETECT(
        defaultTitle = "YOLO detecção de objetos",
        taskLabel = "Verificar",
        expectedFormat = "ONNX / ORT / TFLite",
        defaultRuntime = "ONNX Runtime Mobile"
    ),
    YOLO_SEGMENT(
        defaultTitle = "YOLO segmentação de instâncias",
        taskLabel = "Segmentação",
        expectedFormat = "ONNX / ORT / TFLite",
        defaultRuntime = "ONNX Runtime Mobile"
    ),
    CLIP_RETRIEVAL(
        defaultTitle = "Recuperação imagem-texto CLIP",
        taskLabel = "Correspondência imagem-texto",
        expectedFormat = "Codificador de imagem + Codificador de texto + Tokenizer / Sidecar de embeddings",
        defaultRuntime = "ONNX Runtime Mobile"
    ),
    SMALL_VLM(
        defaultTitle = "Revisão VLM pequeno",
        taskLabel = "Revisão G2D",
        expectedFormat = "Modelo principal GGUF + mmproj",
        defaultRuntime = "llama.cpp"
    )
}

enum class VisionArtifactRole(val label: String) {
    YOLO_MODEL("Modelo"),
    CLIP_IMAGE_ENCODER("Codificador de imagem"),
    CLIP_TEXT_ENCODER("Codificador de texto"),
    CLIP_TOKENIZER("Tokenizer de texto"),
    CLIP_EMBEDDING_SIDECAR("Sidecar de rótulos fixos"),
    VLM_MAIN_MODEL("Modelo principal GGUF"),
    VLM_MMPROJ("Projeção visual mmproj")
}

data class VisionModelArtifact(
    val fileName: String,
    val role: VisionArtifactRole,
    val sizeBytes: Long = 0L
)

enum class VisionTransferPhase {
    IDLE,
    IMPORTING,
    PAUSED,
    FAILED
}

data class VisionImportTransfer(
    val phase: VisionTransferPhase = VisionTransferPhase.IDLE,
    val bytesCopied: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String = ""
)

data class VisionModelPackageInput(
    val id: String,
    val slot: VisionModelSlot,
    val title: String = slot.defaultTitle,
    val artifacts: List<VisionModelArtifact> = emptyList(),
    val transfer: VisionImportTransfer = VisionImportTransfer()
)

enum class VisionPackageStatus {
    READY,
    LIMITED,
    MISSING_FILES,
    INCOMPATIBLE,
    IMPORTING,
    PAUSED,
    FAILED
}

data class VisionModelActionUiModel(
    val id: String,
    val label: String,
    val destructive: Boolean = false
)

data class VisionModelPackageUiModel(
    val id: String,
    val title: String,
    val taskLabel: String,
    val status: VisionPackageStatus,
    val statusLabel: String,
    val statusDetail: String,
    val formatLabel: String,
    val runtimeLabel: String,
    val accelerationLabel: String,
    val artifactLabels: List<String>,
    val progressPercent: Int?,
    val progressLabel: String?,
    val actions: List<VisionModelActionUiModel>
)

data class VisionModelImportUiModel(
    val readyCount: Int,
    val totalCount: Int,
    val summary: String,
    val packages: List<VisionModelPackageUiModel>
)

/**
 * Converts imported files into honest package readiness. READY means the required files are
 * present; it does not claim that a GPU/NPU execution provider or a model architecture works.
 * Runtime compatibility is intentionally left to the diagnostic action.
 */
object VisionModelImportPresenter {
    fun present(inputs: List<VisionModelPackageInput>): VisionModelImportUiModel {
        val packagesBySlot = inputs.groupBy { it.slot }
        val normalized = VisionModelSlot.entries.flatMap { slot ->
            packagesBySlot[slot].orEmpty().ifEmpty {
                listOf(VisionModelPackageInput(id = slot.name.lowercase(Locale.US), slot = slot))
            }
        }
        val packages = normalized.map(::presentPackage)
        val ready = packages.count { it.status == VisionPackageStatus.READY || it.status == VisionPackageStatus.LIMITED }
        return VisionModelImportUiModel(
            readyCount = ready,
            totalCount = packages.size,
            summary = if (ready == 0) {
                "Nenhum pacote completo de modelo visual, importe todos os arquivos necessários para uma tarefa primeiro."
            } else {
                "$ready / ${packages.size} arquivos do pacote de modelo prontos, diagnóstico item por item ainda necessário antes da execução."
            },
            packages = packages
        )
    }

    fun presentPackage(input: VisionModelPackageInput): VisionModelPackageUiModel {
        val validation = validate(input)
        val status = when (input.transfer.phase) {
            VisionTransferPhase.IMPORTING -> VisionPackageStatus.IMPORTING
            VisionTransferPhase.PAUSED -> VisionPackageStatus.PAUSED
            VisionTransferPhase.FAILED -> VisionPackageStatus.FAILED
            VisionTransferPhase.IDLE -> validation.status
        }
        val progress = input.transfer.takeIf {
            it.phase == VisionTransferPhase.IMPORTING || it.phase == VisionTransferPhase.PAUSED
        }?.let(::progressPercent)
        val runtime = inferRuntime(input)
        return VisionModelPackageUiModel(
            id = input.id,
            title = input.title,
            taskLabel = input.slot.taskLabel,
            status = status,
            statusLabel = statusLabel(status),
            statusDetail = when (status) {
                VisionPackageStatus.IMPORTING -> "Copiando para o diretório de modelos visuais locais, progresso continuará após sair da página."
                VisionPackageStatus.PAUSED -> "Importação pausada, arquivos temporários copiados serão mantidos."
                VisionPackageStatus.FAILED -> input.transfer.errorMessage.ifBlank { "Falha na importação, selecione o arquivo novamente." }
                else -> validation.detail
            },
            formatLabel = actualFormatLabel(input),
            runtimeLabel = runtime,
            accelerationLabel = accelerationLabel(runtime, input.artifacts.isNotEmpty()),
            artifactLabels = input.artifacts.map(::artifactLabel).ifEmpty { listOf("Nenhum arquivo importado") },
            progressPercent = progress,
            progressLabel = progress?.let {
                "${formatBytes(input.transfer.bytesCopied)} / ${formatBytes(input.transfer.totalBytes)} · $it%"
            },
            actions = actions(status, input.artifacts.isNotEmpty())
        )
    }

    private fun validate(input: VisionModelPackageInput): Validation {
        val invalidArtifact = input.artifacts.firstOrNull { artifact ->
            artifact.sizeBytes < 0L || !isRoleAllowed(input.slot, artifact.role) || !isExtensionAllowed(artifact)
        }
        if (invalidArtifact != null) {
            return Validation(
                VisionPackageStatus.INCOMPATIBLE,
                "O papel ou formato de ${invalidArtifact.fileName} não corresponde a esta tarefa, substitua e diagnostique novamente."
            )
        }
        if (input.artifacts.any { it.fileName.extensionLower() == "mnn" }) {
            return Validation(
                VisionPackageStatus.INCOMPATIBLE,
                "Versão atual pode gerenciar arquivos MNN, mas a cadeia de execução MNN para esta tarefa ainda não foi integrada."
            )
        }
        return when (input.slot) {
            VisionModelSlot.YOLO_DETECT,
            VisionModelSlot.YOLO_SEGMENT -> validateYolo(input)

            VisionModelSlot.CLIP_RETRIEVAL -> validateClip(input)
            VisionModelSlot.SMALL_VLM -> validateVlm(input)
        }
    }

    private fun validateYolo(input: VisionModelPackageInput): Validation {
        val model = input.artifacts.firstOrNull { it.role == VisionArtifactRole.YOLO_MODEL }
        return if (model == null) {
            Validation(VisionPackageStatus.MISSING_FILES, "Arquivo do modelo YOLO ausente (ONNX / ORT / TFLite).")
        } else {
            Validation(
                VisionPackageStatus.READY,
                "Arquivo do modelo pronto; tamanho de entrada, tensor de saída e suporte a operadores ainda precisam de diagnóstico em tempo de execução para confirmar."
            )
        }
    }

    private fun validateClip(input: VisionModelPackageInput): Validation {
        val hasImage = input.artifacts.any { it.role == VisionArtifactRole.CLIP_IMAGE_ENCODER }
        val hasText = input.artifacts.any { it.role == VisionArtifactRole.CLIP_TEXT_ENCODER }
        val hasSidecar = input.artifacts.any { it.role == VisionArtifactRole.CLIP_EMBEDDING_SIDECAR }
        val tokenizerNames = input.artifacts
            .filter { it.role == VisionArtifactRole.CLIP_TOKENIZER }
            .mapTo(linkedSetOf()) { it.fileName.lowercase(Locale.US) }
        val missingTokenizer = setOf("vocab.json", "merges.txt", "tokenizer_config.json") - tokenizerNames
        if (!hasImage) {
            return Validation(VisionPackageStatus.MISSING_FILES, "Codificador de imagem CLIP ausente.")
        }
        if (hasText && missingTokenizer.isEmpty()) {
            return Validation(
                VisionPackageStatus.READY,
                "Codificadores de imagem e texto com tokenizer pareados, podem ser usados para busca de texto aberto após os diagnósticos passarem."
            )
        }
        if (hasText) {
            return Validation(
                VisionPackageStatus.MISSING_FILES,
                "文本编码器已导入，但缺少 ${missingTokenizer.sorted().joinToString("、")}。"
            )
        }
        if (hasSidecar) {
            return Validation(
                VisionPackageStatus.LIMITED,
                "Codificador de imagem e sidecar de embeddings pareados, suporta apenas rótulos fixos dentro do sidecar; busca de texto ainda precisa do codificador de texto."
            )
        }
        return Validation(
            VisionPackageStatus.MISSING_FILES,
            "Codificador de texto CLIP ausente; pode importar sidecar de embeddings JSON para verificação de rótulos fixos."
        )
    }

    private fun validateVlm(input: VisionModelPackageInput): Validation {
        val hasMain = input.artifacts.any { it.role == VisionArtifactRole.VLM_MAIN_MODEL }
        val hasMmproj = input.artifacts.any { it.role == VisionArtifactRole.VLM_MMPROJ }
        return when {
            !hasMain && !hasMmproj -> Validation(
                VisionPackageStatus.MISSING_FILES,
                "Deve importar o modelo principal GGUF e o arquivo de projeção visual mmproj correspondente como par."
            )

            !hasMain -> Validation(VisionPackageStatus.MISSING_FILES, "mmproj existe, mas falta o modelo principal GGUF correspondente.")
            !hasMmproj -> Validation(VisionPackageStatus.MISSING_FILES, "Modelo principal GGUF existe, mas falta o arquivo de projeção visual mmproj correspondente.")
            else -> Validation(
                VisionPackageStatus.READY,
                "Modelo principal GGUF e arquivo mmproj pareados; arquitetura e dimensões de projeção ainda precisam de diagnóstico em tempo de execução para confirmar."
            )
        }
    }

    private fun isRoleAllowed(slot: VisionModelSlot, role: VisionArtifactRole): Boolean = when (slot) {
        VisionModelSlot.YOLO_DETECT,
        VisionModelSlot.YOLO_SEGMENT -> role == VisionArtifactRole.YOLO_MODEL

        VisionModelSlot.CLIP_RETRIEVAL -> role in setOf(
            VisionArtifactRole.CLIP_IMAGE_ENCODER,
            VisionArtifactRole.CLIP_TEXT_ENCODER,
            VisionArtifactRole.CLIP_TOKENIZER,
            VisionArtifactRole.CLIP_EMBEDDING_SIDECAR
        )

        VisionModelSlot.SMALL_VLM -> role == VisionArtifactRole.VLM_MAIN_MODEL || role == VisionArtifactRole.VLM_MMPROJ
    }

    private fun isExtensionAllowed(artifact: VisionModelArtifact): Boolean {
        val extension = artifact.fileName.extensionLower()
        return when (artifact.role) {
            VisionArtifactRole.YOLO_MODEL -> extension in setOf("onnx", "ort", "tflite", "mnn")
            VisionArtifactRole.CLIP_IMAGE_ENCODER,
            VisionArtifactRole.CLIP_TEXT_ENCODER -> extension in setOf("onnx", "ort", "tflite", "mnn")

            VisionArtifactRole.CLIP_TOKENIZER -> extension in setOf("json", "txt")
            VisionArtifactRole.CLIP_EMBEDDING_SIDECAR -> extension == "json"
            VisionArtifactRole.VLM_MAIN_MODEL -> extension == "gguf"
            VisionArtifactRole.VLM_MMPROJ -> extension == "mmproj" || extension == "gguf"
        }
    }

    private fun actualFormatLabel(input: VisionModelPackageInput): String {
        if (input.artifacts.isEmpty()) return input.slot.expectedFormat
        val formats = input.artifacts.map { artifact ->
            when (artifact.role) {
                VisionArtifactRole.VLM_MMPROJ -> "mmproj"
                VisionArtifactRole.CLIP_TOKENIZER -> "tokenizer"
                VisionArtifactRole.CLIP_EMBEDDING_SIDECAR -> "JSON sidecar"
                else -> artifact.fileName.extensionLower().uppercase(Locale.US)
            }
        }.distinct()
        return formats.joinToString(" + ")
    }

    private fun inferRuntime(input: VisionModelPackageInput): String {
        val coreExtension = input.artifacts.firstOrNull {
            it.role != VisionArtifactRole.CLIP_EMBEDDING_SIDECAR &&
                it.role != VisionArtifactRole.CLIP_TOKENIZER &&
                it.role != VisionArtifactRole.VLM_MMPROJ
        }?.fileName?.extensionLower()
        return when (coreExtension) {
            "onnx", "ort" -> "ONNX Runtime Mobile"
            "tflite" -> "TensorFlow Lite"
            "mnn" -> "MNN (cadeia de execução pendente)"
            "gguf" -> "llama.cpp"
            else -> input.slot.defaultRuntime
        }
    }

    private fun accelerationLabel(runtime: String, hasFiles: Boolean): String {
        val prefix = if (hasFiles) "Padrão atual" else "Linha de base estimada"
        return when {
            runtime.startsWith("ONNX") -> "$prefix: CPU · executor NNAPI/QNN/GPU não registrado"
            runtime.startsWith("TensorFlow") -> "$prefix: CPU · delegado GPU/NNAPI não adicionado"
            runtime.startsWith("llama.cpp") -> "$prefix: CPU (gpu_layers=0) · GPU/NPU não habilitado"
            else -> "Status de execução aguardando diagnóstico · Aceleração GPU/NPU não verificada"
        }
    }

    private fun actions(status: VisionPackageStatus, hasArtifacts: Boolean): List<VisionModelActionUiModel> = when (status) {
        VisionPackageStatus.IMPORTING -> listOf(VisionModelActionUiModel("pause", "Pausar"))
        VisionPackageStatus.PAUSED -> listOf(
            VisionModelActionUiModel("resume", "Continuar importação"),
            VisionModelActionUiModel("remove", "Remover", destructive = true)
        )

        VisionPackageStatus.FAILED -> listOf(
            VisionModelActionUiModel("retry", "Reimportar"),
            VisionModelActionUiModel("remove", "Limpar arquivos temporários", destructive = true)
        )

        VisionPackageStatus.MISSING_FILES -> listOfNotNull(
            VisionModelActionUiModel("import", if (hasArtifacts) "Completar arquivos" else "Importar pacote de modelo"),
            VisionModelActionUiModel("remove", "Remover", destructive = true).takeIf { hasArtifacts }
        )

        VisionPackageStatus.INCOMPATIBLE -> listOf(
            VisionModelActionUiModel("replace", "Substituir arquivos"),
            VisionModelActionUiModel("diagnose", "Ver diagnóstico")
        )

        VisionPackageStatus.READY,
        VisionPackageStatus.LIMITED -> listOf(
            VisionModelActionUiModel("diagnose", "Executar diagnósticos"),
            VisionModelActionUiModel("remove", "Remover", destructive = true)
        )
    }

    private fun statusLabel(status: VisionPackageStatus): String = when (status) {
        VisionPackageStatus.READY -> "Arquivo pronto"
        VisionPackageStatus.LIMITED -> "Rótulos fixos prontos"
        VisionPackageStatus.MISSING_FILES -> "Arquivo faltando"
        VisionPackageStatus.INCOMPATIBLE -> "Incompatível"
        VisionPackageStatus.IMPORTING -> "Importando"
        VisionPackageStatus.PAUSED -> "Pausado"
        VisionPackageStatus.FAILED -> "Falha na importação"
    }

    private fun progressPercent(transfer: VisionImportTransfer): Int {
        if (transfer.totalBytes <= 0L) return 0
        return (transfer.bytesCopied.toDouble() / transfer.totalBytes.toDouble() * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun artifactLabel(artifact: VisionModelArtifact): String {
        val size = artifact.sizeBytes.takeIf { it > 0L }?.let { " · ${formatBytes(it)}" }.orEmpty()
        return "${artifact.role.label} · ${artifact.fileName}$size"
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "Tamanho desconhecido"
        val mib = bytes / (1024.0 * 1024.0)
        return if (mib >= 1024.0) "%.1f GB".format(Locale.US, mib / 1024.0) else "%.0f MB".format(Locale.US, mib)
    }

    private fun String.extensionLower(): String = substringAfterLast('.', "").lowercase(Locale.US)

    private data class Validation(val status: VisionPackageStatus, val detail: String)
}

/** Maps the files already present in MobileCore's private vision directory into task packages. */
object VisionModelImportCatalog {
    fun fromFiles(files: List<File>): List<VisionModelPackageInput> {
        return files
            .filter(File::isFile)
            .mapNotNull(::classify)
            .groupBy({ it.first }, { it.second })
            .map { (slot, artifacts) ->
                VisionModelPackageInput(
                    id = slot.name.lowercase(Locale.US),
                    slot = slot,
                    artifacts = artifacts.distinctBy { "${it.role}:${it.fileName}" }
                )
            }
    }

    private fun classify(file: File): Pair<VisionModelSlot, VisionModelArtifact>? {
        val lower = file.name.lowercase(Locale.US)
        val extension = file.extension.lowercase(Locale.US)
        val slot: VisionModelSlot
        val role: VisionArtifactRole
        when {
            lower in setOf("vocab.json", "merges.txt", "tokenizer_config.json") -> {
                slot = VisionModelSlot.CLIP_RETRIEVAL
                role = VisionArtifactRole.CLIP_TOKENIZER
            }
            ("yolo" in lower || "detect" in lower) && ("seg" in lower || "mask" in lower) -> {
                slot = VisionModelSlot.YOLO_SEGMENT
                role = VisionArtifactRole.YOLO_MODEL
            }
            "yolo" in lower || "detect" in lower -> {
                slot = VisionModelSlot.YOLO_DETECT
                role = VisionArtifactRole.YOLO_MODEL
            }
            "clip" in lower || "mobileclip" in lower || "vit" in lower -> {
                slot = VisionModelSlot.CLIP_RETRIEVAL
                role = when {
                    extension == "json" -> VisionArtifactRole.CLIP_EMBEDDING_SIDECAR
                    "text" in lower -> VisionArtifactRole.CLIP_TEXT_ENCODER
                    else -> VisionArtifactRole.CLIP_IMAGE_ENCODER
                }
            }
            extension in setOf("gguf", "mmproj") || "mmproj" in lower -> {
                slot = VisionModelSlot.SMALL_VLM
                role = if (extension == "mmproj" || "mmproj" in lower) {
                    VisionArtifactRole.VLM_MMPROJ
                } else {
                    VisionArtifactRole.VLM_MAIN_MODEL
                }
            }
            else -> return null
        }
        return slot to VisionModelArtifact(file.name, role, file.length())
    }
}

data class VisionModelImportCallbacks(
    val onImport: (String) -> Unit = {},
    val onPause: (String) -> Unit = {},
    val onResume: (String) -> Unit = {},
    val onRemove: (String) -> Unit = {},
    val onDiagnose: (String) -> Unit = {},
    val onReplace: (String) -> Unit = {},
    val onRetry: (String) -> Unit = {}
)

/** Standalone native View surface. The host only needs to provide state and action callbacks. */
class VisionModelImportScreen(context: Context) : LinearLayout(context) {
    private val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(4), 0, dp(24))
    }

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Palette.background)
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun bind(model: VisionModelImportUiModel, callbacks: VisionModelImportCallbacks = VisionModelImportCallbacks()) {
        content.removeAllViews()
        content.addView(header())
        content.addView(space(12))
        content.addView(summaryCard(model))
        content.addView(space(16))
        content.addView(text("Pacote de modelo", 18f, Palette.deepInk, Typeface.BOLD))
        content.addView(text("Completude dos arquivos não significa compatibilidade de runtime; cada pacote precisa de diagnósticos locais.", 12f, Palette.muted))
        content.addView(space(10))
        model.packages.forEachIndexed { index, packageModel ->
            content.addView(packageCard(packageModel, callbacks))
            if (index != model.packages.lastIndex) content.addView(space(12))
        }
    }

    private fun header(): View = LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(TuiMaTheme.compactHeaderHeightDp)
        addView(IconBadgeView(context, "image", Palette.sky), LinearLayout.LayoutParams(dp(42), dp(42)).apply {
            marginEnd = dp(12)
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("Modelo visual", 20f, Palette.deepInk, Typeface.BOLD))
            addView(text("Importação, pareamento e diagnóstico de compatibilidade local", 12f, Palette.muted))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun summaryCard(model: VisionModelImportUiModel): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(Palette.mintWash, Palette.mint, 16f)
        addView(text("${model.readyCount} / ${model.totalCount} pacotes de modelo prontos", 18f, Palette.deepInk, Typeface.BOLD))
        addView(space(4))
        addView(text(model.summary, 13f, Palette.ink))
        addView(space(10))
        addView(text("Linha de execução atual é CPU; GPU, NNAPI, QNN ou NPU só serão marcados como habilitados após conexão explícita e diagnóstico.", 12f, Palette.muted))
    }

    private fun packageCard(model: VisionModelPackageUiModel, callbacks: VisionModelImportCallbacks): View {
        val accent = statusColor(model.status)
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(15), dp(15), dp(14))
            background = rounded(Palette.surface, Palette.stroke, 16f)
            elevation = dp(1).toFloat()

            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(IconBadgeView(context, if (model.taskLabel == "Revisão G2D") "chip" else "image", accent), LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                    marginEnd = dp(11)
                })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(text(model.title, 16f, Palette.deepInk, Typeface.BOLD))
                    addView(text(model.taskLabel, 12f, Palette.muted))
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(statusPill(model.statusLabel, accent))
            })
            addView(space(11))
            addView(text(model.statusDetail, 13f, Palette.ink))
            addView(space(10))
            addView(metaRow("Formato", model.formatLabel))
            addView(metaRow("Runtime", model.runtimeLabel))
            addView(metaRow("Aceleração", model.accelerationLabel))
            addView(space(8))
            model.artifactLabels.forEach { label ->
                addView(text("• $label", 12f, Palette.muted).apply { setPadding(0, dp(2), 0, dp(2)) })
            }
            model.progressPercent?.let { percent ->
                addView(space(10))
                addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                    progress = percent
                    progressTintList = ColorStateList.valueOf(Palette.mintDark)
                    progressBackgroundTintList = ColorStateList.valueOf(Palette.stroke)
                    contentDescription = "Progresso de importação ${model.title} $percent%"
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)))
                addView(space(5))
                addView(text(model.progressLabel.orEmpty(), 12f, Palette.muted))
            }
            if (model.actions.isNotEmpty()) {
                addView(space(12))
                addView(actionRow(model, callbacks))
            }
        }
    }

    private fun actionRow(model: VisionModelPackageUiModel, callbacks: VisionModelImportCallbacks): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            model.actions.forEachIndexed { index, action ->
                addView(
                    actionButton(action) { dispatchAction(action.id, model.id, callbacks) },
                    LinearLayout.LayoutParams(0, dp(TuiMaTheme.minimumTouchTargetDp), 1f).apply {
                        if (index > 0) marginStart = dp(8)
                    }
                )
            }
        }

    private fun dispatchAction(action: String, packageId: String, callbacks: VisionModelImportCallbacks) {
        when (action) {
            "import" -> callbacks.onImport(packageId)
            "pause" -> callbacks.onPause(packageId)
            "resume" -> callbacks.onResume(packageId)
            "remove" -> callbacks.onRemove(packageId)
            "diagnose" -> callbacks.onDiagnose(packageId)
            "replace" -> callbacks.onReplace(packageId)
            "retry" -> callbacks.onRetry(packageId)
        }
    }

    private fun metaRow(label: String, value: String): View = LinearLayout(context).apply {
        gravity = Gravity.TOP
        setPadding(0, dp(2), 0, dp(2))
        addView(text(label, 12f, Palette.muted, Typeface.BOLD), LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(text(value, 12f, Palette.ink), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun statusPill(label: String, accent: Int): TextView = text(label, 11f, accent, Typeface.BOLD).apply {
        gravity = Gravity.CENTER
        setPadding(dp(9), dp(5), dp(9), dp(5))
        background = rounded(tint(accent, 0.12f), tint(accent, 0.40f), 99f)
    }

    private fun actionButton(action: VisionModelActionUiModel, onClick: () -> Unit): Button = Button(context).apply {
        text = action.label
        textSize = 13f
        isAllCaps = false
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (action.destructive) 0xFFE15E64.toInt() else Palette.deepInk)
        background = rounded(
            if (action.destructive) tint(0xFFE15E64.toInt(), 0.08f) else Palette.mintPale,
            if (action.destructive) tint(0xFFE15E64.toInt(), 0.34f) else Palette.mint,
            14f
        )
        contentDescription = action.label
        setOnClickListener { onClick() }
    }

    private fun statusColor(status: VisionPackageStatus): Int = when (status) {
        VisionPackageStatus.READY -> Palette.mintDark
        VisionPackageStatus.LIMITED -> Palette.sky
        VisionPackageStatus.IMPORTING -> Palette.blue
        VisionPackageStatus.PAUSED -> Palette.lavender
        VisionPackageStatus.MISSING_FILES -> Palette.muted
        VisionPackageStatus.INCOMPATIBLE,
        VisionPackageStatus.FAILED -> 0xFFE15E64.toInt()
    }

    private fun text(value: String, sizeSp: Float, color: Int, style: Int = Typeface.NORMAL): TextView =
        TextView(context).apply {
            text = value
            textSize = sizeSp
            setTextColor(color)
            typeface = Typeface.create(Typeface.DEFAULT, style)
            setLineSpacing(0f, 1.08f)
        }

    private fun rounded(fill: Int, stroke: Int, radiusDp: Float): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        setStroke(dp(1), stroke)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun tint(color: Int, alpha: Float): Int {
        val clamped = alpha.coerceIn(0f, 1f)
        return (color and 0x00FFFFFF) or ((clamped * 255f).toInt() shl 24)
    }

    private fun space(heightDp: Int): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
}

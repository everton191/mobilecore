package ai.mobilecore.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONObject
import java.util.Locale

enum class OmniLifecycleStage {
    SERVICE_OFFLINE,
    READY,
    BLOCKED,
    INSTALLING,
    VERIFYING,
    INSTALLED,
    LOADED,
    FAILED,
    CANCELLED,
}

enum class OmniLifecycleAction {
    START_SERVICE,
    REFRESH,
    INSTALL,
    CANCEL,
    VERIFY,
    LOAD,
    UNINSTALL,
    OPEN_SOURCE,
}

data class OmniLifecycleSnapshot(
    val serviceReachable: Boolean = false,
    val phase: String = "idle",
    val pairVerified: Boolean = false,
    val loaded: Boolean = false,
    val wifiConnected: Boolean = false,
    val requiredMemoryBytes: Long = 0L,
    val availableMemoryBytes: Long = 0L,
    val requiredStorageBytes: Long = 0L,
    val availableStorageBytes: Long = 0L,
    val resourcesSufficient: Boolean = false,
    val mainInstalled: Boolean = false,
    val mainVerified: Boolean = false,
    val mmprojInstalled: Boolean = false,
    val mmprojVerified: Boolean = false,
    val licenseId: String = "qwen-research",
    val licenseReviewStatus: String = "source_declared_not_legal_reviewed",
    val revision: String = "",
    val failureCode: String? = null,
    val failureMessage: String? = null,
)

data class OmniLifecycleActionUiModel(
    val action: OmniLifecycleAction,
    val label: String,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
)

data class OmniLifecycleUiModel(
    val stage: OmniLifecycleStage,
    val statusLabel: String,
    val statusDetail: String,
    val isBusy: Boolean,
    val memoryLabel: String,
    val memoryReady: Boolean,
    val storageLabel: String,
    val storageReady: Boolean,
    val wifiLabel: String,
    val mainArtifactLabel: String,
    val mmprojArtifactLabel: String,
    val licenseLabel: String,
    val revisionLabel: String,
    val actions: List<OmniLifecycleActionUiModel>,
)

/** Pure projection of the loopback lifecycle contract into an honest user-facing state. */
object OmniLifecyclePresenter {
    fun parseStatus(body: String): OmniLifecycleSnapshot {
        val root = JSONObject(body)
        val status = root.optJSONObject("status") ?: root
        val preflight = status.optJSONObject("preflight") ?: JSONObject()
        val artifacts = status.optJSONObject("artifacts") ?: JSONObject()
        val main = artifacts.optJSONObject("main") ?: JSONObject()
        val mmproj = artifacts.optJSONObject("mmproj") ?: JSONObject()
        val license = status.optJSONObject("license") ?: JSONObject()
        val failure = status.optJSONObject("failure")
        val requiredMemory = preflight.optLong("required_memory_bytes", 0L)
        val availableMemory = preflight.optLong("available_memory_bytes", 0L)
        val requiredStorage = preflight.optLong("required_storage_bytes", 0L)
        val availableStorage = preflight.optLong("available_storage_bytes", 0L)
        return OmniLifecycleSnapshot(
            serviceReachable = true,
            phase = status.optString("phase", "idle"),
            pairVerified = status.optBoolean("pair_verified", false),
            loaded = status.optBoolean("loaded", false),
            wifiConnected = status.optBoolean("wifi_connected", false),
            requiredMemoryBytes = requiredMemory,
            availableMemoryBytes = availableMemory,
            requiredStorageBytes = requiredStorage,
            availableStorageBytes = availableStorage,
            resourcesSufficient = preflight.optBoolean(
                "resources_sufficient",
                requiredMemory > 0L && requiredStorage > 0L &&
                    availableMemory >= requiredMemory && availableStorage >= requiredStorage,
            ),
            mainInstalled = main.optBoolean("installed", false),
            mainVerified = main.optBoolean("verified", false),
            mmprojInstalled = mmproj.optBoolean("installed", false),
            mmprojVerified = mmproj.optBoolean("verified", false),
            licenseId = license.optString("id", "qwen-research"),
            licenseReviewStatus = license.optString(
                "review_status",
                "source_declared_not_legal_reviewed",
            ),
            revision = status.optString("revision", ""),
            failureCode = failure?.optString("code")?.takeIf(String::isNotBlank),
            failureMessage = failure?.optString("message")?.takeIf(String::isNotBlank),
        )
    }

    fun withApiFailure(current: OmniLifecycleSnapshot, body: String): OmniLifecycleSnapshot {
        val error = runCatching { JSONObject(body).optJSONObject("error") }.getOrNull()
        return current.copy(
            serviceReachable = true,
            failureCode = error?.optString("code")?.takeIf(String::isNotBlank) ?: "request_failed",
            failureMessage = error?.optString("message")?.takeIf(String::isNotBlank),
        )
    }

    fun present(snapshot: OmniLifecycleSnapshot): OmniLifecycleUiModel {
        val phase = snapshot.phase.lowercase(Locale.US)
        val busy = phase in setOf("preflight", "downloading", "verifying")
        val resourceFailure = snapshot.failureCode in setOf("insufficient_memory", "insufficient_storage", "wifi_required")
        val stage = when {
            !snapshot.serviceReachable -> OmniLifecycleStage.SERVICE_OFFLINE
            snapshot.loaded && snapshot.pairVerified -> OmniLifecycleStage.LOADED
            snapshot.pairVerified -> OmniLifecycleStage.INSTALLED
            phase == "verifying" -> OmniLifecycleStage.VERIFYING
            phase in setOf("preflight", "downloading") -> OmniLifecycleStage.INSTALLING
            phase == "cancelled" -> OmniLifecycleStage.CANCELLED
            (phase == "failed" || snapshot.failureCode != null) && !resourceFailure ->
                OmniLifecycleStage.FAILED
            resourceFailure || !snapshot.resourcesSufficient || !snapshot.wifiConnected -> OmniLifecycleStage.BLOCKED
            else -> OmniLifecycleStage.READY
        }
        val (statusLabel, detail) = statusCopy(stage, snapshot)
        return OmniLifecycleUiModel(
            stage = stage,
            statusLabel = statusLabel,
            statusDetail = detail,
            isBusy = busy,
            memoryLabel = resourceLabel(snapshot.availableMemoryBytes, snapshot.requiredMemoryBytes),
            memoryReady = snapshot.requiredMemoryBytes > 0L &&
                snapshot.availableMemoryBytes >= snapshot.requiredMemoryBytes,
            storageLabel = resourceLabel(snapshot.availableStorageBytes, snapshot.requiredStorageBytes),
            storageReady = snapshot.requiredStorageBytes > 0L &&
                snapshot.availableStorageBytes >= snapshot.requiredStorageBytes,
            wifiLabel = if (snapshot.wifiConnected) "Conectado, pode baixar sob política de apenas Wi-Fi" else "Wi-Fi não detectado",
            mainArtifactLabel = artifactLabel(snapshot.mainInstalled, snapshot.mainVerified),
            mmprojArtifactLabel = artifactLabel(snapshot.mmprojInstalled, snapshot.mmprojVerified),
            licenseLabel = "${snapshot.licenseId} · ${licenseReviewLabel(snapshot.licenseReviewStatus)}",
            revisionLabel = snapshot.revision.take(12).ifBlank { "Aguardando resposta do serviço" },
            actions = actions(stage, snapshot),
        )
    }

    private fun statusCopy(
        stage: OmniLifecycleStage,
        snapshot: OmniLifecycleSnapshot,
    ): Pair<String, String> = when (stage) {
        OmniLifecycleStage.SERVICE_OFFLINE -> "Serviço não conectado" to "Inicie o serviço local para ler recursos e status do modelo em tempo real."
        OmniLifecycleStage.READY -> "Pode instalar" to "Condições do dispositivo atendidas; ainda precisa ler fonte e licença e concordar explicitamente cada vez."
        OmniLifecycleStage.BLOCKED -> "Condições atuais do dispositivo insuficientes" to blockedDetail(snapshot)
        OmniLifecycleStage.INSTALLING -> "Baixando" to "Dois artefatos de versão fixa sendo escritos no diretório privado do app, pode cancelar a qualquer momento."
        OmniLifecycleStage.VERIFYING -> "Verificando" to "Verificando bytes fixos e SHA-256, não carregará até a conclusão da verificação."
        OmniLifecycleStage.INSTALLED -> "Verificado, ainda não carregado" to "Modelo principal e mmproj verificados; capacidades de imagem e áudio serão anunciadas após o carregamento."
        OmniLifecycleStage.LOADED -> "Multimodal local carregado" to "O runtime confirmou este conjunto de artefatos fixos; capacidades reais ainda sujeitas a /health."
        OmniLifecycleStage.CANCELLED -> "Instalação cancelada" to "Arquivos de download temporários limpos; reinstalar ainda requer acordo explícito."
        OmniLifecycleStage.FAILED -> "Operação falhou" to failureLabel(snapshot.failureCode, snapshot.failureMessage)
    }

    private fun actions(
        stage: OmniLifecycleStage,
        snapshot: OmniLifecycleSnapshot,
    ): List<OmniLifecycleActionUiModel> = when (stage) {
        OmniLifecycleStage.SERVICE_OFFLINE -> listOf(
            OmniLifecycleActionUiModel(OmniLifecycleAction.START_SERVICE, "Iniciar serviço e verificar"),
        )
        OmniLifecycleStage.READY -> listOf(
            OmniLifecycleActionUiModel(
                OmniLifecycleAction.INSTALL,
                "Leia as instruções e concorde com o download",
                enabled = snapshot.resourcesSufficient && snapshot.wifiConnected,
            ),
            OmniLifecycleActionUiModel(OmniLifecycleAction.REFRESH, "Reverificar"),
        )
        OmniLifecycleStage.BLOCKED -> listOf(
            OmniLifecycleActionUiModel(OmniLifecycleAction.REFRESH, "Reverificar condições do dispositivo"),
        )
        OmniLifecycleStage.INSTALLING,
        OmniLifecycleStage.VERIFYING -> listOf(
            OmniLifecycleActionUiModel(OmniLifecycleAction.CANCEL, "Cancelar instalação", destructive = true),
        )
        OmniLifecycleStage.INSTALLED -> listOf(
            OmniLifecycleActionUiModel(OmniLifecycleAction.LOAD, "Carregar no runtime local"),
            OmniLifecycleActionUiModel(OmniLifecycleAction.VERIFY, "Reverificar"),
            OmniLifecycleActionUiModel(OmniLifecycleAction.UNINSTALL, "Desinstalar", destructive = true),
        )
        OmniLifecycleStage.LOADED -> listOf(
            OmniLifecycleActionUiModel(OmniLifecycleAction.VERIFY, "Reverificar"),
            OmniLifecycleActionUiModel(OmniLifecycleAction.UNINSTALL, "Desinstalar e liberar", destructive = true),
        )
        OmniLifecycleStage.CANCELLED,
        OmniLifecycleStage.FAILED -> buildList {
            if (snapshot.resourcesSufficient && snapshot.wifiConnected) {
                add(OmniLifecycleActionUiModel(OmniLifecycleAction.INSTALL, "Reler e instalar"))
            }
            add(OmniLifecycleActionUiModel(OmniLifecycleAction.REFRESH, "Atualizar status"))
            if (snapshot.mainInstalled || snapshot.mmprojInstalled) {
                add(OmniLifecycleActionUiModel(OmniLifecycleAction.UNINSTALL, "Limpar arquivos locais", destructive = true))
            }
        }
    }

    private fun blockedDetail(snapshot: OmniLifecycleSnapshot): String = when {
        snapshot.requiredStorageBytes > 0L && snapshot.availableStorageBytes < snapshot.requiredStorageBytes ->
            "Armazenamento privado do app insuficiente, instalação não será iniciada."
        snapshot.requiredMemoryBytes > 0L && snapshot.availableMemoryBytes < snapshot.requiredMemoryBytes ->
            "Memória disponível abaixo do limite conservador de carregamento, instalação não será iniciada."
        !snapshot.wifiConnected -> "Política de apenas Wi-Fi habilitada, verifique após conectar ao Wi-Fi."
        else -> failureLabel(snapshot.failureCode, snapshot.failureMessage)
    }

    private fun failureLabel(code: String?, message: String?): String = when (code) {
        "checksum_mismatch" -> "Resumo do arquivo incompatível, não é possível carregar; limpe e reinstale."
        "artifact_missing" -> "Par de artefatos fixos incompleto, não é possível carregar."
        "download_failed" -> "Download incompleto, verifique a rede e tente novamente."
        "projector_incompatible" -> "mmproj e modelo principal incompatíveis."
        "projector_load_failed" -> "O runtime rejeitou o mmproj verificado."
        "model_load_failed" -> "O runtime falhou ao carregar o modelo verificado."
        "wifi_required" -> "Download por Wi-Fi requer conexão Wi-Fi válida."
        "insufficient_memory" -> "Memória disponível abaixo do limite conservador."
        "insufficient_storage" -> "Armazenamento privado do app insuficiente."
        "request_failed" -> "Serviço local não completou a requisição, atualize e tente novamente."
        else -> message?.takeIf(String::isNotBlank) ?: "Operação de ciclo de vida local não concluída."
    }

    private fun artifactLabel(installed: Boolean, verified: Boolean): String = when {
        verified -> "Instalado · SHA-256 verificado"
        installed -> "Instalado · Ainda não verificado"
        else -> "Não instalado"
    }

    private fun licenseReviewLabel(value: String): String = when (value) {
        "source_declared_not_legal_reviewed" -> "Declaração de fonte, sem revisão legal conduzida"
        else -> value.ifBlank { "Status da revisão desconhecido" }
    }

    private fun resourceLabel(available: Long, required: Long): String {
        if (required <= 0L) return "Aguardando resposta do serviço local"
        return "${formatBytes(available)} disponível / ${formatBytes(required)} necessário"
    }

    private fun formatBytes(bytes: Long): String {
        val gib = bytes.coerceAtLeast(0L) / (1024.0 * 1024.0 * 1024.0)
        return "%.2f GiB".format(Locale.US, gib)
    }
}

data class OmniLifecycleCallbacks(
    val onAction: (OmniLifecycleAction) -> Unit = {},
)

/** MobileCore-owned lifecycle surface; it exposes no device-control or Phone Use action. */
class OmniLifecycleScreen(context: Context) : LinearLayout(context) {
    private val content = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(0, dp(4), 0, dp(24))
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(Palette.background)
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun bind(model: OmniLifecycleUiModel, callbacks: OmniLifecycleCallbacks) {
        content.removeAllViews()
        content.addView(header())
        content.addView(space(12))
        content.addView(statusCard(model, callbacks))
        content.addView(space(12))
        content.addView(preflightCard(model))
        content.addView(space(12))
        content.addView(artifactCard(model))
        content.addView(space(12))
        content.addView(licenseCard(model, callbacks))
        content.addView(space(12))
        content.addView(boundaryCard())
    }

    private fun header(): View = LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(TuiMaTheme.compactHeaderHeightDp)
        addView(IconBadgeView(context, "chip", Palette.lavender), LayoutParams(dp(42), dp(42)).apply {
            marginEnd = dp(12)
        })
        addView(LinearLayout(context).apply {
            orientation = VERTICAL
            addView(text("Multimodal local", 20f, Palette.deepInk, Typeface.BOLD))
            addView(text("Ciclo de vida de dois artefatos fixos Qwen2.5-Omni-3B", 12f, Palette.muted))
        }, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(pill("Experimento", Palette.lavender))
    }

    private fun statusCard(model: OmniLifecycleUiModel, callbacks: OmniLifecycleCallbacks): View =
        card(stageColor(model.stage)) {
            addView(text(model.statusLabel, 18f, Palette.deepInk, Typeface.BOLD))
            addView(space(5))
            addView(text(model.statusDetail, 13f, Palette.ink))
            if (model.isBusy) {
                addView(space(12))
                addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                    isIndeterminate = true
                    indeterminateTintList = ColorStateList.valueOf(Palette.blue)
                    progressBackgroundTintList = ColorStateList.valueOf(Palette.stroke)
                    contentDescription = model.statusLabel
                }, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)))
            }
            if (model.actions.isNotEmpty()) {
                addView(space(13))
                model.actions.forEachIndexed { index, action ->
                    if (index > 0) addView(space(8))
                    addView(actionButton(action) { callbacks.onAction(action.action) }, LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(TuiMaTheme.minimumTouchTargetDp),
                    ))
                }
            }
        }

    private fun preflightCard(model: OmniLifecycleUiModel): View = card(Palette.sky) {
        addView(sectionTitle("Verificação pré-instalação", "Lê valores em tempo real a cada vez, não armazena em cache a autorização"))
        addView(space(9))
        addView(metaRow("Memória", model.memoryLabel, model.memoryReady))
        addView(metaRow("Armazenamento", model.storageLabel, model.storageReady))
        addView(metaRow("Rede", model.wifiLabel, model.wifiLabel.startsWith("Conectado")))
    }

    private fun artifactCard(model: OmniLifecycleUiModel): View = card(Palette.mint) {
        addView(sectionTitle("Par de artefatos fixos", "Qualquer arquivo não verificado não anunciará capacidade multimodal"))
        addView(space(9))
        addView(metaRow("Modelo principal", model.mainArtifactLabel, model.mainArtifactLabel.contains("Verificado")))
        addView(metaRow("mmproj", model.mmprojArtifactLabel, model.mmprojArtifactLabel.contains("Verificado")))
        addView(metaRow("revision", model.revisionLabel, model.revisionLabel != "Aguardando resposta do serviço"))
    }

    private fun licenseCard(model: OmniLifecycleUiModel, callbacks: OmniLifecycleCallbacks): View = card(Palette.amber) {
        addView(sectionTitle("Fonte e licença", "Deve confirmar separadamente antes de baixar"))
        addView(space(8))
        addView(text("Publicador: ggml-org (não é GGUF oficial do Qwen)", 13f, Palette.ink))
        addView(text("Licença: ${model.licenseLabel}", 13f, Palette.ink).apply { setPadding(0, dp(4), 0, 0) })
        addView(text("Download completo de cerca de 3,39 GiB; escrito apenas no diretório privado do app MobileCore.", 12f, Palette.muted).apply {
            setPadding(0, dp(7), 0, 0)
        })
        addView(space(11))
        addView(actionButton(OmniLifecycleActionUiModel(OmniLifecycleAction.OPEN_SOURCE, "Ver fonte da versão fixa")) {
            callbacks.onAction(OmniLifecycleAction.OPEN_SOURCE)
        }, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(TuiMaTheme.minimumTouchTargetDp)))
    }

    private fun boundaryCard(): View = card(Palette.lavender) {
        addView(sectionTitle("Limite de capacidade", "MobileCore apenas lida com inferência local"))
        addView(space(7))
        addView(text("Esta página não pode clicar em outros apps, fazer login em contas ou fazer pedidos. Phone Use, aprovação de transações e cadeia de evidências ainda são controlados pelo MobileCode.", 12.5f, Palette.ink))
        addView(text("A rota GGUF atual suporta apenas entrada de texto/imagem/áudio para saída de texto; não suporta entrada de vídeo e saída de voz.", 12f, Palette.muted).apply {
            setPadding(0, dp(7), 0, 0)
        })
    }

    private fun card(accent: Int, block: LinearLayout.() -> Unit): View = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(Palette.surface, tint(accent, 0.42f), 16f)
        elevation = dp(1).toFloat()
        block()
    }

    private fun sectionTitle(title: String, subtitle: String): View = LinearLayout(context).apply {
        orientation = VERTICAL
        addView(text(title, 16f, Palette.deepInk, Typeface.BOLD))
        addView(text(subtitle, 12f, Palette.muted).apply { setPadding(0, dp(3), 0, 0) })
    }

    private fun metaRow(label: String, value: String, ready: Boolean): View = LinearLayout(context).apply {
        gravity = Gravity.TOP
        minimumHeight = dp(42)
        setPadding(0, dp(6), 0, dp(6))
        addView(View(context).apply {
            background = rounded(if (ready) Palette.mintDark else Palette.amber, Color.TRANSPARENT, 4f)
        }, LayoutParams(dp(8), dp(8)).apply { topMargin = dp(5); marginEnd = dp(9) })
        addView(text(label, 12.5f, Palette.ink, Typeface.BOLD), LayoutParams(dp(68), ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(text(value, 12f, Palette.muted).apply { gravity = Gravity.END }, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        contentDescription = "$label，$value"
    }

    private fun actionButton(action: OmniLifecycleActionUiModel, onClick: () -> Unit): Button = Button(context).apply {
        text = action.label
        textSize = 13f
        isAllCaps = false
        typeface = Typeface.DEFAULT_BOLD
        isEnabled = action.enabled
        alpha = if (action.enabled) 1f else 0.46f
        setTextColor(if (action.destructive) Palette.danger else Palette.deepInk)
        background = rounded(
            if (action.destructive) Palette.dangerWash else Palette.mintPale,
            if (action.destructive) tint(Palette.danger, 0.45f) else Palette.mint,
            14f,
        )
        contentDescription = action.label
        setOnClickListener { if (action.enabled) onClick() }
    }

    private fun stageColor(stage: OmniLifecycleStage): Int = when (stage) {
        OmniLifecycleStage.READY,
        OmniLifecycleStage.INSTALLED,
        OmniLifecycleStage.LOADED -> Palette.mint
        OmniLifecycleStage.INSTALLING,
        OmniLifecycleStage.VERIFYING -> Palette.blue
        OmniLifecycleStage.BLOCKED,
        OmniLifecycleStage.CANCELLED -> Palette.amber
        OmniLifecycleStage.FAILED -> Palette.danger
        OmniLifecycleStage.SERVICE_OFFLINE -> Palette.muted
    }

    private fun pill(value: String, accent: Int): TextView = text(value, 11f, accent, Typeface.BOLD).apply {
        gravity = Gravity.CENTER
        setPadding(dp(9), dp(5), dp(9), dp(5))
        background = rounded(tint(accent, 0.12f), tint(accent, 0.38f), 99f)
    }

    private fun text(value: String, sizeSp: Float, color: Int, style: Int = Typeface.NORMAL): TextView =
        TextView(context).apply {
            text = value
            textSize = sizeSp
            setTextColor(color)
            setTypeface(typeface, style)
            setLineSpacing(0f, 1.14f)
        }

    private fun rounded(color: Int, stroke: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    private fun tint(color: Int, alpha: Float): Int = Color.argb(
        (255 * alpha).toInt().coerceIn(0, 255),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )

    private fun space(heightDp: Int): View = View(context).apply {
        layoutParams = LayoutParams(1, dp(heightDp))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun dp(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}

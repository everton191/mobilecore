package ai.mobilecore.ui

/**
 * User-facing lifecycle for a local model. Downloading and loading are deliberately separate:
 * a file on disk is not advertised as an active runtime model until the runtime confirms it.
 */
enum class ModelLifecyclePhase {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    DOWNLOADED,
    LOADING,
    LOADED,
    DOWNLOAD_FAILED,
    LOAD_FAILED,
}

enum class ModelLifecycleTone {
    NEUTRAL,
    PROGRESS,
    READY,
    ACTIVE,
    WARNING,
    ERROR,
}

data class ModelLifecycleUiModel(
    val phase: ModelLifecyclePhase,
    val statusLabel: String,
    val supportingText: String,
    val actionLabel: String,
    val actionEnabled: Boolean,
    val tone: ModelLifecycleTone,
)

object ModelLifecyclePresenter {
    fun present(
        downloaded: Boolean,
        active: Boolean,
        loading: Boolean,
        downloadStatus: String? = null,
        loadFailed: Boolean = false,
        progressPercent: Int = 0,
    ): ModelLifecycleUiModel {
        val normalizedDownloadStatus = downloadStatus?.lowercase()
        val phase = when {
            active && downloaded -> ModelLifecyclePhase.LOADED
            loading && downloaded -> ModelLifecyclePhase.LOADING
            loadFailed && downloaded -> ModelLifecyclePhase.LOAD_FAILED
            normalizedDownloadStatus == "downloading" -> ModelLifecyclePhase.DOWNLOADING
            normalizedDownloadStatus == "paused" -> ModelLifecyclePhase.PAUSED
            normalizedDownloadStatus in setOf("failed", "cancelled") -> ModelLifecyclePhase.DOWNLOAD_FAILED
            downloaded -> ModelLifecyclePhase.DOWNLOADED
            else -> ModelLifecyclePhase.NOT_DOWNLOADED
        }
        return when (phase) {
            ModelLifecyclePhase.NOT_DOWNLOADED -> ModelLifecycleUiModel(
                phase, "Não baixado", "Arquivo do modelo ainda não salvo no dispositivo", "Baixar", true, ModelLifecycleTone.NEUTRAL,
            )
            ModelLifecyclePhase.DOWNLOADING -> ModelLifecycleUiModel(
                phase,
                "Baixando ${progressPercent.coerceIn(0, 100)}%",
                "Escrevendo na biblioteca de modelos do app",
                "Pausar",
                true,
                ModelLifecycleTone.PROGRESS,
            )
            ModelLifecyclePhase.PAUSED -> ModelLifecycleUiModel(
                phase, "Pausado", "Progresso do download preservado", "Continuar", true, ModelLifecycleTone.WARNING,
            )
            ModelLifecyclePhase.DOWNLOADED -> ModelLifecycleUiModel(
                phase, "Baixado", "Arquivo no dispositivo, ainda não carregado na memória", "Carregar", true, ModelLifecycleTone.READY,
            )
            ModelLifecyclePhase.LOADING -> ModelLifecycleUiModel(
                phase, "Carregando", "O runtime está verificando e mapeando o modelo", "Carregando", false, ModelLifecycleTone.PROGRESS,
            )
            ModelLifecyclePhase.LOADED -> ModelLifecycleUiModel(
                phase, "Carregado", "Modelo sendo usado pelo runtime local", "Em uso", false, ModelLifecycleTone.ACTIVE,
            )
            ModelLifecyclePhase.DOWNLOAD_FAILED -> ModelLifecycleUiModel(
                phase, "Falha no download", "Arquivo completo do modelo não obtido", "Baixar novamente", true, ModelLifecycleTone.ERROR,
            )
            ModelLifecyclePhase.LOAD_FAILED -> ModelLifecycleUiModel(
                phase, "Falha ao carregar", "Arquivo baixado, mas o runtime falhou ao carregar", "Tentar carregar novamente", true, ModelLifecycleTone.ERROR,
            )
        }
    }
}

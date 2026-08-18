package ai.mobilecore.ui

/**
 * Host boundary for access to local photos. The gallery-search module deliberately does not
 * declare permissions or enumerate MediaStore itself. A host may implement this with Photo
 * Picker grants or a runtime media permission, then feed progress back through [GallerySearchEvent].
 */
interface GalleryMediaAccessContract {
    fun requestGalleryAccess()
    fun scanGrantedMedia()
    fun retryGalleryIndex()
    fun cancelGalleryIndex()
    fun clearGalleryIndex()
}

/** Host boundary for model preparation and retrieval. */
interface GallerySearchRuntimeContract {
    fun prepareSearchModels()
    fun releaseSearchModels()
    fun searchLocalGallery(query: String, topK: Int)
}

sealed interface GalleryIndexState {
    data object PermissionRequired : GalleryIndexState

    data class AccessGranted(
        val persistedIndexDetected: Boolean = false,
    ) : GalleryIndexState

    data class Scanning(val discoveredCount: Int = 0) : GalleryIndexState

    data class Indexing(
        val processedCount: Int,
        val totalCount: Int,
        val skippedCount: Int = 0,
    ) : GalleryIndexState

    data class Ready(
        val indexedCount: Int,
        val lastIndexedAtMs: Long? = null,
        val skippedCount: Int = 0,
    ) : GalleryIndexState

    data class Failed(
        val message: String,
        val retryable: Boolean = true,
    ) : GalleryIndexState
}

sealed interface GalleryModelState {
    data class Missing(
        val clipModelName: String = "MobileCLIP",
        val verifierModelName: String = "Modelo pequeno 0.8B (opcional)",
    ) : GalleryModelState

    data class Preparing(
        val componentName: String,
        val progressPercent: Int,
    ) : GalleryModelState

    data class Ready(
        val clipImageEncoder: String,
        val clipTextEncoder: String,
        val verifierModel: String? = null,
        val modelId: String = "user-imported/clip-compatible",
        val identityVerified: Boolean = false,
    ) : GalleryModelState

    data class Released(
        val reason: String,
    ) : GalleryModelState

    data class Failed(
        val message: String,
        val retryable: Boolean = true,
    ) : GalleryModelState
}

enum class GalleryResultSource {
    CLIP_DIRECT,
    G2D_VERIFIED,
}

/**
 * A search hit contains only host-owned identifiers and display metadata. [contentUri] may be a
 * persisted Photo Picker URI or a MediaStore URI. The screen never opens it directly; hosts bind
 * thumbnails through [GalleryThumbnailBinder].
 */
data class GallerySearchResult(
    val mediaId: String,
    val contentUri: String,
    val title: String,
    val subtitle: String,
    val similarity: Double,
    val source: GalleryResultSource,
)

sealed interface GalleryRetrievalState {
    data object Idle : GalleryRetrievalState

    data class Searching(val query: String) : GalleryRetrievalState

    data class Results(
        val query: String,
        val items: List<GallerySearchResult>,
    ) : GalleryRetrievalState

    data class NoMatch(val query: String) : GalleryRetrievalState

    data class Failed(
        val query: String,
        val message: String,
    ) : GalleryRetrievalState
}

data class GallerySearchState(
    val index: GalleryIndexState = GalleryIndexState.PermissionRequired,
    val models: GalleryModelState = GalleryModelState.Missing(),
    val query: String = "",
    val retrieval: GalleryRetrievalState = GalleryRetrievalState.Idle,
    val limitedPhotoAccess: Boolean = false,
) {
    val isIndexReady: Boolean
        get() = (index as? GalleryIndexState.Ready)?.indexedCount?.let { it > 0 } == true

    val areModelsReady: Boolean
        get() = models is GalleryModelState.Ready

    val canSearch: Boolean
        get() = isIndexReady && areModelsReady
}

sealed interface GallerySearchEvent {
    data object AccessGranted : GallerySearchEvent
    data class AccessAvailable(val persistedIndexDetected: Boolean) : GallerySearchEvent
    data object AccessRevoked : GallerySearchEvent
    data class PhotoAccessScopeChanged(val limited: Boolean) : GallerySearchEvent
    data class ScanProgress(val discoveredCount: Int) : GallerySearchEvent
    data class ScanCompleted(val totalCount: Int, val completedAtMs: Long? = null) : GallerySearchEvent
    data class IndexProgress(
        val processedCount: Int,
        val totalCount: Int,
        val skippedCount: Int = 0,
    ) : GallerySearchEvent
    data class IndexCompleted(
        val indexedCount: Int,
        val completedAtMs: Long,
        val skippedCount: Int = 0,
    ) : GallerySearchEvent
    data class IndexRestored(val indexedCount: Int, val completedAtMs: Long) : GallerySearchEvent
    data object IndexCleared : GallerySearchEvent
    data class IndexFailed(val message: String, val retryable: Boolean = true) : GallerySearchEvent
    data object RetryIndex : GallerySearchEvent

    data class ModelPreparationStarted(val componentName: String) : GallerySearchEvent
    data class ModelPreparationProgress(
        val componentName: String,
        val progressPercent: Int,
    ) : GallerySearchEvent
    data class ModelsReady(
        val clipImageEncoder: String,
        val clipTextEncoder: String,
        val verifierModel: String? = null,
        val modelId: String = "user-imported/clip-compatible",
        val identityVerified: Boolean = false,
    ) : GallerySearchEvent
    data class ModelsReleased(val reason: String) : GallerySearchEvent
    data class ModelPreparationFailed(
        val message: String,
        val retryable: Boolean = true,
    ) : GallerySearchEvent

    data class QueryChanged(val value: String) : GallerySearchEvent
    data object SearchStarted : GallerySearchEvent
    data class SearchCompleted(
        val query: String,
        val items: List<GallerySearchResult>,
    ) : GallerySearchEvent
    data class SearchFailed(val query: String, val message: String) : GallerySearchEvent
    data object ClearSearch : GallerySearchEvent
}

/** Pure reducer used by the Android screen host and unit tests. */
object GallerySearchStateMachine {
    fun reduce(state: GallerySearchState, event: GallerySearchEvent): GallerySearchState = when (event) {
        GallerySearchEvent.AccessGranted -> state.copy(
            index = GalleryIndexState.Scanning(),
            retrieval = GalleryRetrievalState.Idle,
        )
        is GallerySearchEvent.AccessAvailable -> state.copy(
            index = GalleryIndexState.AccessGranted(event.persistedIndexDetected),
            retrieval = GalleryRetrievalState.Idle,
        )
        GallerySearchEvent.AccessRevoked -> state.copy(
            index = GalleryIndexState.PermissionRequired,
            retrieval = GalleryRetrievalState.Idle,
            limitedPhotoAccess = false,
        )
        is GallerySearchEvent.PhotoAccessScopeChanged -> state.copy(
            limitedPhotoAccess = event.limited,
        )
        is GallerySearchEvent.ScanProgress -> if (state.index is GalleryIndexState.Scanning) {
            state.copy(index = GalleryIndexState.Scanning(event.discoveredCount.coerceAtLeast(0)))
        } else {
            state
        }
        is GallerySearchEvent.ScanCompleted -> if (state.index is GalleryIndexState.Scanning) {
            if (event.totalCount <= 0) {
                state.copy(index = GalleryIndexState.Ready(0, event.completedAtMs))
            } else {
                state.copy(index = GalleryIndexState.Indexing(0, event.totalCount))
            }
        } else {
            state
        }
        is GallerySearchEvent.IndexProgress -> if (state.index is GalleryIndexState.Indexing) {
            val total = event.totalCount.coerceAtLeast(0)
            state.copy(
                index = GalleryIndexState.Indexing(
                    processedCount = event.processedCount.coerceIn(0, total),
                    totalCount = total,
                    skippedCount = event.skippedCount.coerceIn(0, event.processedCount.coerceAtLeast(0)),
                ),
            )
        } else {
            state
        }
        is GallerySearchEvent.IndexCompleted -> if (state.index is GalleryIndexState.Indexing) {
            state.copy(
                index = GalleryIndexState.Ready(
                    event.indexedCount.coerceAtLeast(0),
                    event.completedAtMs,
                    event.skippedCount.coerceAtLeast(0),
                ),
                retrieval = GalleryRetrievalState.Idle,
            )
        } else {
            state
        }
        is GallerySearchEvent.IndexRestored -> state.copy(
            index = GalleryIndexState.Ready(
                indexedCount = event.indexedCount.coerceAtLeast(0),
                lastIndexedAtMs = event.completedAtMs.coerceAtLeast(0L),
            ),
            retrieval = GalleryRetrievalState.Idle,
        )
        GallerySearchEvent.IndexCleared -> state.copy(
            index = GalleryIndexState.AccessGranted(persistedIndexDetected = false),
            retrieval = GalleryRetrievalState.Idle,
        )
        is GallerySearchEvent.IndexFailed -> state.copy(
            index = GalleryIndexState.Failed(event.message, event.retryable),
            retrieval = GalleryRetrievalState.Idle,
        )
        GallerySearchEvent.RetryIndex -> if (
            (state.index is GalleryIndexState.Failed && state.index.retryable) ||
            state.index is GalleryIndexState.Ready ||
            state.index is GalleryIndexState.AccessGranted
        ) {
            state.copy(index = GalleryIndexState.Scanning(), retrieval = GalleryRetrievalState.Idle)
        } else {
            state
        }
        is GallerySearchEvent.ModelPreparationStarted -> state.copy(
            models = GalleryModelState.Preparing(event.componentName, 0),
            retrieval = GalleryRetrievalState.Idle,
        )
        is GallerySearchEvent.ModelPreparationProgress -> if (state.models is GalleryModelState.Preparing) {
            state.copy(
                models = GalleryModelState.Preparing(
                    componentName = event.componentName,
                    progressPercent = event.progressPercent.coerceIn(0, 100),
                ),
            )
        } else {
            state
        }
        is GallerySearchEvent.ModelsReady -> state.copy(
            models = GalleryModelState.Ready(
                clipImageEncoder = event.clipImageEncoder,
                clipTextEncoder = event.clipTextEncoder,
                verifierModel = event.verifierModel,
                modelId = event.modelId,
                identityVerified = event.identityVerified,
            ),
        )
        is GallerySearchEvent.ModelsReleased -> state.copy(
            models = GalleryModelState.Released(event.reason),
            retrieval = GalleryRetrievalState.Idle,
        )
        is GallerySearchEvent.ModelPreparationFailed -> state.copy(
            models = GalleryModelState.Failed(event.message, event.retryable),
            retrieval = GalleryRetrievalState.Idle,
        )
        is GallerySearchEvent.QueryChanged -> state.copy(
            query = event.value,
            retrieval = GalleryRetrievalState.Idle,
        )
        GallerySearchEvent.SearchStarted -> {
            val normalized = state.query.trim()
            if (state.canSearch && normalized.isNotEmpty()) {
                state.copy(query = normalized, retrieval = GalleryRetrievalState.Searching(normalized))
            } else {
                state
            }
        }
        is GallerySearchEvent.SearchCompleted -> if (state.retrieval.matches(event.query)) {
            val unique = event.items
                .sortedByDescending { it.similarity }
                .distinctBy { it.mediaId }
            state.copy(
                retrieval = if (unique.isEmpty()) {
                    GalleryRetrievalState.NoMatch(event.query)
                } else {
                    GalleryRetrievalState.Results(event.query, unique)
                },
            )
        } else {
            state
        }
        is GallerySearchEvent.SearchFailed -> if (state.retrieval.matches(event.query)) {
            state.copy(retrieval = GalleryRetrievalState.Failed(event.query, event.message))
        } else {
            state
        }
        GallerySearchEvent.ClearSearch -> state.copy(query = "", retrieval = GalleryRetrievalState.Idle)
    }

    private fun GalleryRetrievalState.matches(query: String): Boolean =
        this is GalleryRetrievalState.Searching && this.query == query.trim()
}

enum class GalleryStatusAction {
    REQUEST_ACCESS,
    RETRY_INDEX,
    CANCEL_INDEX,
    CLEAR_INDEX,
    PREPARE_MODELS,
    RELEASE_MODELS,
    SELECT_MORE_PHOTOS,
}

data class GalleryStatusUiModel(
    val eyebrow: String,
    val title: String,
    val detail: String,
    val progressPercent: Int? = null,
    val actionLabel: String? = null,
    val action: GalleryStatusAction? = null,
    val actionEnabled: Boolean = true,
    val secondaryActionLabel: String? = null,
    val secondaryAction: GalleryStatusAction? = null,
    val secondaryActionEnabled: Boolean = true,
    val isSuccess: Boolean = false,
    val isBusy: Boolean = false,
)

data class GalleryResultUiModel(
    val rank: Int,
    val mediaId: String,
    val contentUri: String,
    val title: String,
    val subtitle: String,
    val scoreLabel: String,
    val sourceLabel: String,
    val sourceDetail: String,
    val source: GalleryResultSource,
)

data class GallerySearchUiModel(
    val indexStatus: GalleryStatusUiModel,
    val modelStatus: GalleryStatusUiModel,
    val query: String,
    val queryEnabled: Boolean,
    val searchEnabled: Boolean,
    val searchActionLabel: String,
    val searchHint: String,
    val resultTitle: String,
    val resultMessage: String,
    val results: List<GalleryResultUiModel>,
    val isSearching: Boolean,
    val showNoMatch: Boolean,
    val privacyLabel: String,
    val topK: Int,
)

object GallerySearchPresenter {
    const val DEFAULT_TOP_K = 20

    fun present(state: GallerySearchState, topK: Int = DEFAULT_TOP_K): GallerySearchUiModel {
        val safeTopK = topK.coerceAtLeast(1)
        val ranked = (state.retrieval as? GalleryRetrievalState.Results)
            ?.items
            .orEmpty()
            .sortedByDescending { it.similarity }
            .take(safeTopK)
            .mapIndexed { index, result -> result.toUi(index + 1) }
        val isSearching = state.retrieval is GalleryRetrievalState.Searching
        val noMatch = state.retrieval is GalleryRetrievalState.NoMatch
        val resultQuery = when (val retrieval = state.retrieval) {
            is GalleryRetrievalState.Results -> retrieval.query
            is GalleryRetrievalState.NoMatch -> retrieval.query
            is GalleryRetrievalState.Failed -> retrieval.query
            else -> state.query.trim()
        }
        val resultMessage = when (val retrieval = state.retrieval) {
            GalleryRetrievalState.Idle -> when {
                state.index is GalleryIndexState.Ready && state.index.indexedCount == 0 ->
                    "Nenhuma foto encontrada para indexar. Autorize mais fotos e tente novamente."
                !state.canSearch -> "Após o índice e o modelo CLIP estarem prontos, pesquise fotos com linguagem natural."
                else -> "Tente 'pessoa de vermelho na praia' ou 'foto de reunião com gráfico de barras'."
            }
            is GalleryRetrievalState.Searching -> "Calculando similaridade cosseno CLIP a partir do índice de vetores local."
            is GalleryRetrievalState.Results -> "\"${retrieval.query}\" retornou ${ranked.size} candidatos com maior similaridade cosseno"
            is GalleryRetrievalState.NoMatch -> "Índice local não retornou fotos candidatas ordenáveis."
            is GalleryRetrievalState.Failed -> retrieval.message
        }
        return GallerySearchUiModel(
            indexStatus = indexStatus(state.index, state.limitedPhotoAccess),
            modelStatus = modelStatus(state.models),
            query = state.query,
            queryEnabled = state.canSearch && !isSearching,
            searchEnabled = state.canSearch && state.query.isNotBlank() && !isSearching,
            searchActionLabel = if (isSearching) "Pesquisando" else "Pesquisar fotos locais",
            searchHint = if (state.canSearch) "Descreva pessoas, objetos, animais, cores ou cenários" else "Conclua primeiro o índice da galeria e preparação do modelo",
            resultTitle = if (ranked.isNotEmpty()) "Top-${ranked.size} Resultados" else if (resultQuery.isNotEmpty()) "Resultados da busca" else "Iniciar busca",
            resultMessage = resultMessage,
            results = ranked,
            isSearching = isSearching,
            showNoMatch = noMatch,
            privacyLabel = "MobileCore não envia fotos, consultas ou índices",
            topK = safeTopK,
        )
    }

    private fun indexStatus(
        state: GalleryIndexState,
        limitedPhotoAccess: Boolean,
    ): GalleryStatusUiModel = when (state) {
        GalleryIndexState.PermissionRequired -> GalleryStatusUiModel(
            eyebrow = "Índice de álbum · Não autorizado",
            title = "Crie o índice local após permitir o acesso",
            detail = "Lê apenas suas fotos autorizadas; imagens originais, vetores e consultas ficam no dispositivo.",
            actionLabel = "Autorizar e criar índice",
            action = GalleryStatusAction.REQUEST_ACCESS,
        )
        is GalleryIndexState.AccessGranted -> GalleryStatusUiModel(
            eyebrow = "Índice de álbum · Autorizado",
            title = if (state.persistedIndexDetected) "Índice local pendente encontrado" else "Nenhum índice de fotos criado",
            detail = if (state.persistedIndexDetected) {
                "Após preparar o CLIP, o índice existente será verificado e restaurado; deve ser reconstruído quando o modelo mudar."
            } else {
                "Varredura de fotos autorizadas e geração de vetores locais, pode ser cancelada e retomada a qualquer momento."
            },
            actionLabel = if (limitedPhotoAccess) "Selecionar mais e criar índice" else "Criar índice de fotos",
            action = if (limitedPhotoAccess) {
                GalleryStatusAction.SELECT_MORE_PHOTOS
            } else {
                GalleryStatusAction.RETRY_INDEX
            },
            secondaryActionLabel = if (state.persistedIndexDetected) "Limpar índice existente" else null,
            secondaryAction = if (state.persistedIndexDetected) GalleryStatusAction.CLEAR_INDEX else null,
        )
        is GalleryIndexState.Scanning -> GalleryStatusUiModel(
            eyebrow = "Índice de álbum · Varrendo",
            title = "Descobrindo fotos pesquisáveis",
            detail = "${state.discoveredCount} fotos encontradas, vetores serão gerados automaticamente após a conclusão da verificação.",
            progressPercent = null,
            isBusy = true,
        )
        is GalleryIndexState.Indexing -> {
            val progress = if (state.totalCount > 0) {
                (state.processedCount * 100 / state.totalCount).coerceIn(0, 100)
            } else {
                0
            }
            GalleryStatusUiModel(
                eyebrow = "Índice de álbum · Indexando",
                title = "Gerando vetores de imagem",
                detail = buildString {
                    append("${state.processedCount} / ${state.totalCount} imagens · $progress%")
                    if (state.skippedCount > 0) append(" · ${state.skippedCount} imagens puladas")
                },
                progressPercent = progress,
                actionLabel = "Cancelar e salvar progresso",
                action = GalleryStatusAction.CANCEL_INDEX,
                isBusy = true,
            )
        }
        is GalleryIndexState.Ready -> GalleryStatusUiModel(
            eyebrow = "Índice de álbum · Pronto",
            title = if (state.indexedCount > 0) "${state.indexedCount} fotos pesquisáveis" else "Nenhuma foto pesquisável temporariamente",
            detail = if (state.indexedCount > 0) {
                buildString {
                    append("Pode atualizar incrementalmente; fotos e vetores não serão enviados para a nuvem.")
                    if (state.skippedCount > 0) append(" ${state.skippedCount} fotos ilegíveis puladas desta vez.")
                }
            } else {
                "Autorize mais fotos e recrie o índice."
            },
            actionLabel = when {
                limitedPhotoAccess -> "Selecionar mais e atualizar índice"
                state.indexedCount == 0 -> "Verificar novamente"
                else -> "Atualizar índice"
            },
            action = if (limitedPhotoAccess) {
                GalleryStatusAction.SELECT_MORE_PHOTOS
            } else {
                GalleryStatusAction.RETRY_INDEX
            },
            secondaryActionLabel = "Limpar índice",
            secondaryAction = GalleryStatusAction.CLEAR_INDEX,
            isSuccess = state.indexedCount > 0,
        )
        is GalleryIndexState.Failed -> GalleryStatusUiModel(
            eyebrow = "Índice de álbum · Falhou",
            title = "Índice local não concluído",
            detail = state.message,
            actionLabel = if (state.retryable) "Recriar índice" else null,
            action = if (state.retryable) GalleryStatusAction.RETRY_INDEX else null,
            actionEnabled = state.retryable,
        )
    }

    private fun modelStatus(state: GalleryModelState): GalleryStatusUiModel = when (state) {
        is GalleryModelState.Missing -> GalleryStatusUiModel(
            eyebrow = "Modelo de busca · Pendente",
            title = "Preparar codificador CLIP de imagem e texto",
            detail = "${state.clipModelName} para recall; ${state.verifierModelName} pode fazer revisão G2D em candidatos difusos.",
            actionLabel = "Preparar modelo de busca",
            action = GalleryStatusAction.PREPARE_MODELS,
        )
        is GalleryModelState.Preparing -> GalleryStatusUiModel(
            eyebrow = "Modelo de busca · Preparando",
            title = "Preparando ${state.componentName}",
            detail = "Progresso ${state.progressPercent.coerceIn(0, 100)}% · Após conclusão, busca offline disponível",
            progressPercent = state.progressPercent.coerceIn(0, 100),
            isBusy = true,
        )
        is GalleryModelState.Ready -> GalleryStatusUiModel(
            eyebrow = if (state.identityVerified) {
                "Modelo de busca · Pronto · Identidade verificada"
            } else {
                "Modelo de busca · Pronto · Identidade não verificada"
            },
            title = if (state.verifierModel == null) "Recuperação CLIP pronta" else "CLIP + G2D pronto",
            detail = buildString {
                append("Imagem: ${state.clipImageEncoder} · Texto: ${state.clipTextEncoder}")
                if (state.identityVerified) {
                    append(" · Identidade verificada: ${state.modelId}")
                } else {
                    append(" · Identidade não verificada: Modelo CLIP compatível importado pelo usuário")
                }
                if (state.verifierModel == null) {
                    append(" · Revisão G2D não habilitada")
                } else {
                    append(" · Revisão: ${state.verifierModel}")
                }
            },
            actionLabel = "Liberar memória do modelo",
            action = GalleryStatusAction.RELEASE_MODELS,
            isSuccess = true,
        )
        is GalleryModelState.Released -> GalleryStatusUiModel(
            eyebrow = "Modelo de busca · Liberado",
            title = "Arquivos CLIP ainda no dispositivo",
            detail = state.reason,
            actionLabel = "Recarregar modelo de busca",
            action = GalleryStatusAction.PREPARE_MODELS,
        )
        is GalleryModelState.Failed -> GalleryStatusUiModel(
            eyebrow = "Modelo de busca · Falhou",
            title = "Preparação do modelo não concluída",
            detail = state.message,
            actionLabel = if (state.retryable) "Preparar novamente" else null,
            action = if (state.retryable) GalleryStatusAction.PREPARE_MODELS else null,
            actionEnabled = state.retryable,
        )
    }

    private fun GallerySearchResult.toUi(rank: Int): GalleryResultUiModel = GalleryResultUiModel(
        rank = rank,
        mediaId = mediaId,
        contentUri = contentUri,
        title = title,
        subtitle = subtitle,
        scoreLabel = "余弦 ${String.format(java.util.Locale.US, "%.3f", similarity)}",
        sourceLabel = when (source) {
            GalleryResultSource.CLIP_DIRECT -> "Saída direta CLIP"
            GalleryResultSource.G2D_VERIFIED -> "Revisão G2D"
        },
        sourceDetail = when (source) {
            GalleryResultSource.CLIP_DIRECT -> "Ordenado por similaridade cosseno do CLIP, sem aplicação de limiar de calibração"
            GalleryResultSource.G2D_VERIFIED -> "Modelo pequeno já revisado no conjunto de candidatos"
        },
        source = source,
    )
}

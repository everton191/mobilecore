package ai.mobilecore.ui

import ai.mobilecore.playground.PlaygroundArtifactOrigin
import ai.mobilecore.playground.PlaygroundCatalogEntry
import ai.mobilecore.playground.PlaygroundInstallFailureCode
import ai.mobilecore.playground.PlaygroundInstallPhase
import ai.mobilecore.playground.PlaygroundInstallSnapshot
import java.io.File

enum class PlaygroundLocalPhase {
    NOT_DOWNLOADED,
    PREFLIGHT,
    DOWNLOADING,
    VERIFYING,
    INSTALLED,
    LOADING,
    LOADED,
    VERIFICATION_FAILED,
    SOURCE_MISMATCH,
    DOWNLOAD_FAILED,
    INSUFFICIENT_STORAGE,
    ARTIFACT_MISSING,
    ATOMIC_INSTALL_FAILED,
    UNINSTALL_FAILED,
    VERIFICATION_IO_FAILED,
    LOAD_FAILED,
    CANCELLED,
    PARTIAL,
    LOCAL_UNVERIFIED,
    ACTIVE_UNVERIFIED,
}

data class PlaygroundEntryUiModel(
    val id: String,
    val title: String,
    val metadata: String,
    val originLabel: String,
    val attributionLabel: String,
    val validationLabel: String,
    val validationPassed: Boolean,
    val localStatusLabel: String,
    val localStatusDetail: String,
    val localPhase: PlaygroundLocalPhase,
    val distributionLabel: String,
    val recommended: Boolean,
    val progressPercent: Int,
    val primaryActionLabel: String,
    val primaryActionEnabled: Boolean,
    val canUninstall: Boolean,
)

object PlaygroundPresenter {
    private val positiveValidationStates = setOf(
        "EMULATOR_CONTRACT_CHECKED",
        "DEVICE_VALIDATED",
        "QUALITY_VALIDATED",
        "PERFORMANCE_VALIDATED",
    )

    fun present(
        entry: PlaygroundCatalogEntry,
        localFileNames: Set<String>,
        activeModelPath: String?,
        managedPrimaryModelPath: String? = null,
        installSnapshot: PlaygroundInstallSnapshot? = null,
    ): PlaygroundEntryUiModel {
        val required = entry.requiredArtifactNames
        val present = required.count { candidate -> localFileNames.any { it.equals(candidate, ignoreCase = true) } }
        val primary = entry.artifacts.firstOrNull { it.role == "mobile_runtime" }?.name
        val activeModelFileName = activeModelPath?.let { File(it).name }
        val activeTrustedModel = installSnapshot?.verified == true &&
            sameCanonicalPath(activeModelPath, managedPrimaryModelPath)
        val discoveredPhase = when {
            primary != null &&
                primary.equals(activeModelFileName, ignoreCase = true) &&
                present == required.size -> PlaygroundLocalPhase.ACTIVE_UNVERIFIED
            present == 0 -> PlaygroundLocalPhase.NOT_DOWNLOADED
            present == required.size -> PlaygroundLocalPhase.LOCAL_UNVERIFIED
            else -> PlaygroundLocalPhase.PARTIAL
        }
        val phase = installSnapshot?.let { snapshot ->
            when (snapshot.phase) {
                PlaygroundInstallPhase.NOT_DOWNLOADED,
                PlaygroundInstallPhase.UNINSTALLED,
                -> PlaygroundLocalPhase.NOT_DOWNLOADED
                PlaygroundInstallPhase.PREFLIGHT -> PlaygroundLocalPhase.PREFLIGHT
                PlaygroundInstallPhase.DOWNLOADING -> PlaygroundLocalPhase.DOWNLOADING
                PlaygroundInstallPhase.VERIFYING -> PlaygroundLocalPhase.VERIFYING
                PlaygroundInstallPhase.INSTALLED -> when {
                    !snapshot.verified -> discoveredPhase
                    activeTrustedModel -> PlaygroundLocalPhase.LOADED
                    else -> PlaygroundLocalPhase.INSTALLED
                }
                PlaygroundInstallPhase.LOADING -> if (snapshot.verified) {
                    PlaygroundLocalPhase.LOADING
                } else {
                    discoveredPhase
                }
                PlaygroundInstallPhase.LOADED -> when {
                    !snapshot.verified -> discoveredPhase
                    activeTrustedModel -> PlaygroundLocalPhase.LOADED
                    else -> PlaygroundLocalPhase.INSTALLED
                }
                PlaygroundInstallPhase.VERIFICATION_FAILED ->
                    if (snapshot.failure?.code == PlaygroundInstallFailureCode.VERIFICATION_IO_FAILED) {
                        PlaygroundLocalPhase.VERIFICATION_IO_FAILED
                    } else {
                        PlaygroundLocalPhase.VERIFICATION_FAILED
                    }
                PlaygroundInstallPhase.SOURCE_MISMATCH -> PlaygroundLocalPhase.SOURCE_MISMATCH
                PlaygroundInstallPhase.CANCELLED -> PlaygroundLocalPhase.CANCELLED
                PlaygroundInstallPhase.FAILED -> when (snapshot.failure?.code) {
                    PlaygroundInstallFailureCode.MODEL_LOAD_FAILED -> PlaygroundLocalPhase.LOAD_FAILED
                    PlaygroundInstallFailureCode.SOURCE_MISMATCH -> PlaygroundLocalPhase.SOURCE_MISMATCH
                    PlaygroundInstallFailureCode.CHECKSUM_MISMATCH,
                    PlaygroundInstallFailureCode.SIZE_MISMATCH,
                    -> PlaygroundLocalPhase.VERIFICATION_FAILED
                    PlaygroundInstallFailureCode.VERIFICATION_IO_FAILED -> PlaygroundLocalPhase.VERIFICATION_IO_FAILED
                    PlaygroundInstallFailureCode.INSUFFICIENT_STORAGE -> PlaygroundLocalPhase.INSUFFICIENT_STORAGE
                    PlaygroundInstallFailureCode.ARTIFACT_MISSING -> PlaygroundLocalPhase.ARTIFACT_MISSING
                    PlaygroundInstallFailureCode.ATOMIC_INSTALL_FAILED -> PlaygroundLocalPhase.ATOMIC_INSTALL_FAILED
                    PlaygroundInstallFailureCode.UNINSTALL_FAILED -> PlaygroundLocalPhase.UNINSTALL_FAILED
                    else -> PlaygroundLocalPhase.DOWNLOAD_FAILED
                }
            }
        } ?: discoveredPhase
        val status = when (phase) {
            PlaygroundLocalPhase.NOT_DOWNLOADED -> "Não baixado" to "Nenhum arquivo do manifesto encontrado no dispositivo"
            PlaygroundLocalPhase.PREFLIGHT -> "Verificar espaço de instalação" to "Confirmando que o diretório privado pode acomodar o modelo e margem de segurança"
            PlaygroundLocalPhase.DOWNLOADING -> "Baixando · ${installSnapshot?.progressPercent ?: 0}%" to
                "Escrevendo arquivo temporário retomável; ainda não instalado como modelo"
            PlaygroundLocalPhase.VERIFYING -> "Verificando" to "Verificando bytes exatos e SHA-256, não carregará até a conclusão"
            PlaygroundLocalPhase.INSTALLED -> "Instalado · Verificado" to "Fonte fixa, bytes e SHA-256 correspondem, seguro para carregar"
            PlaygroundLocalPhase.LOADING -> "Carregando" to "Modelo verificado sendo entregue ao runtime local llama.cpp"
            PlaygroundLocalPhase.LOADED -> "Carregado" to "O runtime atual está usando este modelo verificado"
            PlaygroundLocalPhase.VERIFICATION_FAILED -> "Verificação falhou" to
                "Arquivo baixado incompatível com bytes ou SHA-256 do manifesto, arquivos temporários limpos"
            PlaygroundLocalPhase.SOURCE_MISMATCH -> "Fonte incompatível" to
                "Encontrado arquivo com mesmo nome mas não verificado por este manifesto; não sobrescreverá ou carregará"
            PlaygroundLocalPhase.DOWNLOAD_FAILED -> "Falha no download · Pode retomar" to
                "Arquivos .part restritos são mantidos quando a rede é interrompida; downloads retomam do ponto de interrupção"
            PlaygroundLocalPhase.INSUFFICIENT_STORAGE -> "Espaço de armazenamento insuficiente" to
                "Espaço disponível abaixo dos bytes do modelo e margem de segurança; transferência de rede não iniciada"
            PlaygroundLocalPhase.ARTIFACT_MISSING -> "Arquivo de instalação ausente" to
                "Arquivo gerenciado exigido pelo manifesto não existe mais; arquivos residuais não serão tratados como instalação completa"
            PlaygroundLocalPhase.ATOMIC_INSTALL_FAILED -> "Instalação atômica falhou" to
                "Arquivos temporários verificados falharam na troca atômica para modelo oficial; arquivos semi-instalados não serão carregados"
            PlaygroundLocalPhase.UNINSTALL_FAILED -> "Desinstalação não concluída" to
                "Pelo menos um arquivo gerenciado não pôde ser deletado; status atual não mostrará como desinstalado"
            PlaygroundLocalPhase.VERIFICATION_IO_FAILED -> "Leitura de verificação falhou" to
                "Verificação SHA-256 completa ou envio de registro de verificação não concluído; modelo permanece não confiável"
            PlaygroundLocalPhase.LOAD_FAILED -> "Falha ao carregar" to "Modelo verificado e instalado, mas o runtime local falhou ao carregar"
            PlaygroundLocalPhase.CANCELLED -> "Cancelado" to
                "Download ou verificação interrompidos; downloads temporários limpos, arquivos de instalação oficiais não deletados"
            PlaygroundLocalPhase.PARTIAL -> "Arquivo incompleto" to "Modelo multi-arquivo faltando ${required.size - present} componentes"
            PlaygroundLocalPhase.LOCAL_UNVERIFIED -> "Arquivo local · Aguardando verificação" to "Arquivo encontrado, ainda não verificado pelo SHA-256 do Playground"
            PlaygroundLocalPhase.ACTIVE_UNVERIFIED -> if (required.size > 1) {
                "Modelo principal em execução · Fonte aguardando verificação" to
                    "GGUF principal em execução; componente de projeção apenas confirmado existente, ainda não confirmado carregado ou correspondente ao SHA-256 do manifesto"
            } else {
                "Executando · Fonte aguardando verificação" to "O runtime atual usa arquivo com mesmo nome, ainda não corresponde ao SHA-256 do manifesto"
            }
        }
        val action = when (phase) {
            PlaygroundLocalPhase.NOT_DOWNLOADED,
            PlaygroundLocalPhase.DOWNLOAD_FAILED,
            -> "Baixar e verificar" to true
            PlaygroundLocalPhase.CANCELLED -> if (installSnapshot?.installedArtifactNames?.isNotEmpty() == true) {
                "Reverificar" to true
            } else {
                "Baixar e verificar" to true
            }
            PlaygroundLocalPhase.VERIFICATION_FAILED -> if (installSnapshot?.installedArtifactNames?.isNotEmpty() == true) {
                "Remover arquivos com erro" to true
            } else {
                "Baixar novamente e verificar" to true
            }
            PlaygroundLocalPhase.PREFLIGHT -> "Verificando espaço" to false
            PlaygroundLocalPhase.DOWNLOADING -> "Cancelar download" to true
            PlaygroundLocalPhase.VERIFYING -> "Cancelar verificação" to true
            PlaygroundLocalPhase.INSTALLED,
            PlaygroundLocalPhase.LOAD_FAILED,
            -> "Carregar modelo" to true
            PlaygroundLocalPhase.LOADING -> "Carregando" to false
            PlaygroundLocalPhase.LOADED -> "Modelo em execução" to false
            PlaygroundLocalPhase.SOURCE_MISMATCH -> "Remover arquivos com erro" to true
            PlaygroundLocalPhase.INSUFFICIENT_STORAGE -> "Reverificar espaço" to true
            PlaygroundLocalPhase.ARTIFACT_MISSING -> if (installSnapshot?.installedArtifactNames?.isNotEmpty() == true) {
                "Remover arquivos residuais" to true
            } else {
                "Baixar novamente e verificar" to true
            }
            PlaygroundLocalPhase.ATOMIC_INSTALL_FAILED -> "Tentar instalação atômica novamente" to true
            PlaygroundLocalPhase.UNINSTALL_FAILED -> "Tentar desinstalar novamente" to true
            PlaygroundLocalPhase.VERIFICATION_IO_FAILED -> "Reverificar" to true
            PlaygroundLocalPhase.PARTIAL,
            PlaygroundLocalPhase.LOCAL_UNVERIFIED,
            PlaygroundLocalPhase.ACTIVE_UNVERIFIED,
            -> "Ver arquivos locais" to false
        }
        val metadata = listOf(entry.parameterLabel, entry.quantizationLabel)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        val validationPassed = entry.state in positiveValidationStates &&
            entry.verifiedCapabilities.status == "pass"
        val recommended = entry.featured &&
            entry.source.licenseReview == "cleared" &&
            validationPassed
        val attribution = when (entry.origin) {
            PlaygroundArtifactOrigin.UPSTREAM -> "Upstream ${entry.source.upstreamPublisher} · GGUF Oficial"
            PlaygroundArtifactOrigin.HARZVA,
            PlaygroundArtifactOrigin.THIRD_PARTY,
            PlaygroundArtifactOrigin.RECIPE ->
                "Upstream ${entry.source.upstreamPublisher} · Conversor ${entry.source.conversionPublisher}"
        }
        val distributionLabel = when {
            entry.distribution.mode == "huggingface_model_repo" && entry.distribution.downloadable ->
                "Hugging Face verificado · Fornece link direto fixo"
            entry.distribution.downloadable -> "Publicado · Pode baixar diretamente"
            entry.distribution.mode == "gitcode_model_repo" &&
                entry.distribution.publicationState == "POST_PUBLISH_VERIFIED" &&
                entry.distribution.installTransport == "git_lfs_batch" ->
                "GitCode verificado · Instalação direta pendente LFS"
            entry.distribution.published -> "Repositório fonte publicado · Instalação direta ainda não disponível"
            entry.distribution.publishable -> "Passou pelo gate de publicação · Ainda não publicado"
            else -> "Incluído · Ainda não pode publicar"
        }
        return PlaygroundEntryUiModel(
            id = entry.id,
            title = entry.displayName,
            metadata = metadata,
            originLabel = entry.origin.displayLabel,
            attributionLabel = attribution,
            validationLabel = entry.validationLabel,
            validationPassed = validationPassed,
            localStatusLabel = status.first,
            localStatusDetail = status.second,
            localPhase = phase,
            distributionLabel = distributionLabel,
            recommended = recommended,
            progressPercent = installSnapshot?.progressPercent ?: 0,
            primaryActionLabel = action.first,
            primaryActionEnabled = action.second,
            canUninstall = installSnapshot != null && phase in setOf(
                PlaygroundLocalPhase.INSTALLED,
                PlaygroundLocalPhase.LOAD_FAILED,
                PlaygroundLocalPhase.LOADED,
                PlaygroundLocalPhase.UNINSTALL_FAILED,
            ),
        )
    }

    fun originAccessibilityLabel(origin: PlaygroundArtifactOrigin): String = when (origin) {
        PlaygroundArtifactOrigin.HARZVA -> "Convertido por Harzva"
        PlaygroundArtifactOrigin.THIRD_PARTY -> "Convertido por terceiros, preservando atribuição do conversor original"
        PlaygroundArtifactOrigin.UPSTREAM -> "Publicado pelo upstream oficial"
        PlaygroundArtifactOrigin.RECIPE -> "Apenas fornece receita de conversão"
    }

    private fun sameCanonicalPath(first: String?, second: String?): Boolean {
        if (first.isNullOrBlank() || second.isNullOrBlank()) return false
        val canonicalFirst = runCatching { File(first).canonicalPath }.getOrNull() ?: return false
        val canonicalSecond = runCatching { File(second).canonicalPath }.getOrNull() ?: return false
        return canonicalFirst == canonicalSecond
    }
}

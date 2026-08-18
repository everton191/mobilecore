package ai.mobilecore

import ai.mobilecore.benchmark.AndroidBenchmarkTelemetry
import ai.mobilecore.benchmark.BenchmarkAggregator
import ai.mobilecore.benchmark.BenchmarkDeviceIdentity
import ai.mobilecore.benchmark.BenchmarkDigestVerifier
import ai.mobilecore.benchmark.BenchmarkFailureKind
import ai.mobilecore.benchmark.BenchmarkManifestRepository
import ai.mobilecore.benchmark.BenchmarkPreflight
import ai.mobilecore.benchmark.BenchmarkPreflightReason
import ai.mobilecore.benchmark.BenchmarkPreflightResult
import ai.mobilecore.benchmark.BenchmarkPreflightSnapshot
import ai.mobilecore.benchmark.BenchmarkProfile
import ai.mobilecore.benchmark.BenchmarkReport
import ai.mobilecore.benchmark.BenchmarkReportStore
import ai.mobilecore.benchmark.BenchmarkRunSample
import ai.mobilecore.benchmark.BenchmarkScoreEngine
import ai.mobilecore.benchmark.BenchmarkSpecV2
import ai.mobilecore.benchmark.ThermalStatus
import ai.mobilecore.benchmark.BenchmarkUiEvent
import ai.mobilecore.benchmark.BenchmarkUiState
import ai.mobilecore.benchmark.BenchmarkUiStateMachine
import ai.mobilecore.runtime.BenchmarkResult
import ai.mobilecore.runtime.BenchmarkScorer
import ai.mobilecore.runtime.BenchmarkSpec
import ai.mobilecore.runtime.GgufMetadataReader
import ai.mobilecore.runtime.ModelLoadStatusContract
import ai.mobilecore.runtime.RuntimeBridge
import ai.mobilecore.g2d.G2dBranchTool
import ai.mobilecore.g2d.OxfordPetsG2dRunner
import ai.mobilecore.g2d.OxfordPetsRunScale
import ai.mobilecore.gallery.search.AndroidGallerySearchHost
import ai.mobilecore.gallery.search.AndroidGallerySearchHostResult
import ai.mobilecore.gallery.search.GalleryCancellationToken
import ai.mobilecore.gallery.search.GalleryClipArtifactSet
import ai.mobilecore.gallery.search.GalleryIndexOutcome
import ai.mobilecore.gallery.search.GalleryPersistedIndexStatus
import ai.mobilecore.gallery.search.GalleryPhotoSelection
import ai.mobilecore.gallery.search.GalleryQueryOutcome
import ai.mobilecore.gallery.search.GallerySearchFailure
import ai.mobilecore.gallery.search.GallerySearchFailureCode
import ai.mobilecore.gallery.search.ClipImageSampling
import ai.mobilecore.gallery.search.GalleryRuntimeReleaseBarrier
import ai.mobilecore.playground.PlaygroundArtifactOrigin
import ai.mobilecore.playground.PlaygroundArtifactInstaller
import ai.mobilecore.playground.PlaygroundCatalogEntry
import ai.mobilecore.playground.PlaygroundCatalogRepository
import ai.mobilecore.playground.PlaygroundInstallPhase
import ai.mobilecore.playground.PlaygroundInstallerRegistry
import ai.mobilecore.playground.PlaygroundRuntimeCandidate
import ai.mobilecore.playground.PlaygroundRuntimeTruthResolver
import ai.mobilecore.playground.PlaygroundManagedArtifactPolicy
import ai.mobilecore.playground.AtomicGgufImport
import ai.mobilecore.service.MobileCoreService
import ai.mobilecore.ui.BenchmarkLiveSnapshot
import ai.mobilecore.ui.BenchmarkShareCardRenderer
import ai.mobilecore.ui.BenchmarkScreenPresenter
import ai.mobilecore.ui.GallerySearchActions
import ai.mobilecore.ui.GallerySearchEvent
import ai.mobilecore.ui.GallerySearchPresenter
import ai.mobilecore.ui.GallerySearchScreen
import ai.mobilecore.ui.GallerySearchState
import ai.mobilecore.ui.GallerySearchStateMachine
import ai.mobilecore.ui.GallerySearchResult
import ai.mobilecore.ui.GalleryThumbnailBinder
import ai.mobilecore.ui.GalleryResultSource
import ai.mobilecore.ui.G2dValidationCallbacks
import ai.mobilecore.ui.G2dValidationExperiment
import ai.mobilecore.ui.G2dValidationInput
import ai.mobilecore.ui.G2dValidationMeasurement
import ai.mobilecore.ui.G2dValidationPresenter
import ai.mobilecore.ui.G2dValidationRouteCounts
import ai.mobilecore.ui.G2dValidationRunState
import ai.mobilecore.ui.G2dValidationScreen
import ai.mobilecore.ui.HomeScreenPresenter
import ai.mobilecore.ui.IconBadgeView
import ai.mobilecore.ui.ModelLifecyclePhase
import ai.mobilecore.ui.ModelLifecyclePresenter
import ai.mobilecore.ui.ModelLifecycleTone
import ai.mobilecore.ui.ModelLifecycleUiModel
import ai.mobilecore.ui.OmniLifecycleAction
import ai.mobilecore.ui.OmniLifecycleCallbacks
import ai.mobilecore.ui.OmniLifecyclePresenter
import ai.mobilecore.ui.OmniLifecycleScreen
import ai.mobilecore.ui.OmniLifecycleSnapshot
import ai.mobilecore.ui.Palette
import ai.mobilecore.ui.PlaygroundLocalPhase
import ai.mobilecore.ui.PlaygroundPresenter
import ai.mobilecore.ui.ResultsScreenPresenter
import ai.mobilecore.ui.StandardModelDownloadPhase
import ai.mobilecore.ui.TuiMaCircularProgressView
import ai.mobilecore.ui.TuiMaTheme
import ai.mobilecore.ui.TuiMaThemeMode
import ai.mobilecore.ui.VisionModelImportCallbacks
import ai.mobilecore.ui.VisionModelImportCatalog
import ai.mobilecore.ui.VisionModelImportPresenter
import ai.mobilecore.ui.VisionModelImportScreen
import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Typeface
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.util.Size
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.TextViewCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

private const val PREF_RECOMMENDATION_MODE = "recommendation_preference"
private const val PREF_UI_THEME_MODE = "ui_theme_mode"
private const val STATE_CURRENT_TAB = "current_tab"
private const val BYTES_PER_MB = 1024L * 1024L

private class BenchmarkRunException(
    val kind: BenchmarkFailureKind,
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var runtimeChipText: TextView
    private lateinit var preferenceLabelText: TextView
    private lateinit var recommendationContainer: LinearLayout
    private lateinit var rootScrollView: ScrollView
    private lateinit var contentRoot: LinearLayout
    private lateinit var bottomNavHost: FrameLayout
    private var currentTab = AppTab.HOME
    private var routeStatusText: TextView? = null
    private var visionImageText: TextView? = null
    private var visionResultText: TextView? = null
    private var visionModelSummaryText: TextView? = null
    private var selectedVisionImageUri: Uri? = null
    private var selectedVisionImageName: String? = null
    private var selectedVisionImagePath: String? = null
    private var requiredModelDownloadContainer: LinearLayout? = null
    private var isTestRunning = false
    private val benchmarkUiStateMachine = BenchmarkUiStateMachine()
    private var selectedBenchmarkProfile = BenchmarkProfile.STANDARD
    private var selectedResultRunId: String? = null
    private var comparisonBaselineRunId: String? = null
    private var selectingComparisonBaseline = false
    private var selectedThemeMode = TuiMaThemeMode.SYSTEM
    private var gallerySearchState = GallerySearchState()
    private val galleryWorker: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mobilecore-gallery-search").apply { isDaemon = true }
    }
    private val galleryThumbnailWorker: ExecutorService = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "mobilecore-gallery-thumbnail").apply { isDaemon = true }
    }
    private val galleryHostLock = Any()
    @Volatile private var gallerySearchHost: AndroidGallerySearchHost? = null
    @Volatile private var galleryHostOpenInFlight = false
    @Volatile private var galleryIndexRequestedAfterHostOpen = false
    @Volatile private var galleryRuntimeGeneration = 0L
    @Volatile private var galleryIndexOperationGeneration = 0L
    @Volatile private var galleryIndexCancellation: GalleryCancellationToken? = null
    @Volatile private var galleryLastIndexFailureCode: GallerySearchFailureCode? = null
    @Volatile private var runtimeReportsLoadedModel = false
    @Volatile private var runtimeModelStateRefreshInFlight = false
    private val runtimeModelStateCompletionCallbacks = mutableListOf<() -> Unit>()
    private val runtimeModelStateResultCallbacks = mutableListOf<(Boolean) -> Unit>()
    private var g2dValidationInput = G2dValidationInput(
        datasetName = "Oxford-Pets (test.txt oficial)",
        targetSampleCount = 3_669,
        preparationMessage = "Divisão de teste oficial bloqueada; somente começa após importação do pacote de imagens Oxford-Pets, CLIP e VLM.",
    )
    private var activeG2dRunner: OxfordPetsG2dRunner? = null
    private var benchmarkStartedAtMs = 0L
    private var benchmarkLiveSnapshot = BenchmarkLiveSnapshot()
    @Volatile private var benchmarkCancellationRequested = false
    private var recommendationPreference = RecommendationPreference.STABILITY
    private val serviceHost = "127.0.0.1"
    private val servicePort = 8080
    private val notificationPermissionRequestCode = 1001
    private val importModelRequestCode = 1002
    private val pickVisionImageRequestCode = 1003
    private val importVisionModelRequestCode = 1004
    private val galleryPermissionRequestCode = 1005
    private var pendingAfterNotificationPermission: (() -> Unit)? = null
    private val providerStateByProvider = mutableMapOf<String, ModelDownloadState>()
    private val providerTitleByProvider = mutableMapOf<String, TextView>()
    private val providerStatusByProvider = mutableMapOf<String, TextView>()
    private val providerMessageByProvider = mutableMapOf<String, TextView>()
    private val providerProgressByProvider = mutableMapOf<String, TextView>()
    private val providerCancelByProvider = mutableMapOf<String, TextView>()
    private val providerTileByProvider = mutableMapOf<String, View>()
    private var modelScopeSearchQuery = ""
    private var modelScopeLoading = false
    private var modelScopeLoaded = false
    private var modelScopeError: String? = null
    private var modelScopeLoadedQuery = ""
    private var modelScopeRemoteTotal: Int? = null
    private val modelScopeCatalog = mutableListOf<ModelScopeCatalogEntry>()
    private var modelScopeStatusText: TextView? = null
    private var modelScopeResultsContainer: LinearLayout? = null
    private var activeModelPath: String? = null
    private var pendingModelPath: String? = null
    private var modelLoadFailurePath: String? = null
    private var modelLoadFailureMessage: String? = null
    private var modelLoadReceiverRegistered = false
    private var omniLifecycleSnapshot = OmniLifecycleSnapshot()
    private var omniStatusRefreshInFlight = false
    private val modelLoadStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ModelLoadStatusContract.ACTION) return
            val modelPath = intent.getStringExtra(ModelLoadStatusContract.EXTRA_MODEL_PATH) ?: return
            when (intent.getStringExtra(ModelLoadStatusContract.EXTRA_STATE)) {
                ModelLoadStatusContract.STATE_LOADING -> {
                    val requestedPath = canonicalModelPath(modelPath)
                    if (activeModelPath != requestedPath) {
                        activeModelPath = null
                        runtimeReportsLoadedModel = false
                        reconcilePlaygroundRuntimeTruth(null)
                    }
                    pendingModelPath = requestedPath
                    modelLoadFailurePath = null
                    modelLoadFailureMessage = null
                    playgroundInstallerForModelPath(modelPath)?.markLoading()
                    updateStatus("Carregando")
                }
                ModelLoadStatusContract.STATE_LOADED -> {
                    runtimeReportsLoadedModel = true
                    activeModelPath = canonicalModelPath(modelPath)
                    pendingModelPath = null
                    modelLoadFailurePath = null
                    modelLoadFailureMessage = null
                    reconcilePlaygroundRuntimeTruth(activeModelPath)
                    updateStatus("Carregado")
                }
                ModelLoadStatusContract.STATE_FAILED -> {
                    pendingModelPath = null
                    modelLoadFailurePath = modelPath
                    modelLoadFailureMessage = intent.getStringExtra(ModelLoadStatusContract.EXTRA_MESSAGE)
                    playgroundInstallerForModelPath(modelPath)?.markLoadFailed()
                    // Loading B may have released A before B failed. Restore an active artifact only
                    // after the runtime health contract confirms its exact identity.
                    activeModelPath = null
                    runtimeReportsLoadedModel = false
                    reconcilePlaygroundRuntimeTruth(null)
                    refreshRuntimeModelState()
                    updateStatus("Falhou")
                }
            }
            if (currentTab in setOf(AppTab.HOME, AppTab.MODELS, AppTab.PLAYGROUND, AppTab.TEST)) {
                renderCurrentTab()
            }
        }
    }
    private val activeDownloadThreads = ConcurrentHashMap<String, Thread>()
    private val progressHandler = Handler(Looper.getMainLooper())
    private val omniStatusPollRunnable = Runnable { refreshOmniLifecycleStatus() }
    private val modelScopeSearchRunnable = Runnable {
        refreshModelScopeCatalog(force = true)
    }
    private val progressPollRunnable = object : Runnable {
        override fun run() {
            if (hasActiveDownload()) {
                progressHandler.postDelayed(this, 900L)
            }
        }
    }
    private val modelHubItems = listOf(
        ModelHubItem(
            provider = "HuggingFace",
            shortName = "Qwen2.5 0.5B Q4",
            fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            url = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf?download=true"
        ),
        ModelHubItem(
            provider = "ModelScope",
            shortName = "Gemma3 270M Q4",
            fileName = "gemma-3-270m-it-Q4_K_M.gguf",
            url = "https://modelscope.cn/models/unsloth/gemma-3-270m-it-GGUF/resolve/master/gemma-3-270m-it-Q4_K_M.gguf"
        )
    )
    private val playgroundCatalog by lazy(LazyThreadSafetyMode.NONE) {
        runCatching { PlaygroundCatalogRepository(applicationContext).load() }.getOrNull()
    }
    private val modelScopeSeeds = listOf(
        ModelScopeRepoSeed("unsloth", "gemma-3-270m-it-GGUF", "Gemma3 270M"),
        ModelScopeRepoSeed("unsloth", "gemma-3-1b-it-GGUF", "Gemma3 1B"),
        ModelScopeRepoSeed("Qwen", "Qwen2.5-0.5B-Instruct-GGUF", "Qwen2.5 0.5B"),
        ModelScopeRepoSeed("unsloth", "Qwen3-0.6B-GGUF", "Qwen3 0.6B"),
        ModelScopeRepoSeed("Qwen", "Qwen2.5-1.5B-Instruct-GGUF", "Qwen2.5 1.5B")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedThemeMode = readThemeMode()
        TuiMaTheme.configure(selectedThemeMode, isSystemDarkTheme())
        currentTab = savedInstanceState?.getString(STATE_CURRENT_TAB)?.let { saved ->
            AppTab.entries.firstOrNull { it.name == saved }
        } ?: currentTab
        recommendationPreference = readRecommendationPreference()
        if (providerStateByProvider.isEmpty()) {
            modelHubItems.forEach { providerStateByProvider[downloadTaskKey(it)] = ModelDownloadState(item = it) }
        }
        actionBar?.hide()
        window.statusBarColor = Palette.background
        window.navigationBarColor = Palette.background
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !TuiMaTheme.isDark
            isAppearanceLightNavigationBars = !TuiMaTheme.isDark
        }

        val pageRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ambientPageBackground()
        }
        rootScrollView = ScrollView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isFillViewport = true
            isVerticalScrollBarEnabled = false
        }
        contentRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(pageGutterDp()), dp(14), dp(pageGutterDp()), dp(10))
        }
        bottomNavHost = FrameLayout(this).apply {
            setBackgroundColor(Palette.background)
            setPadding(0, dp(2), 0, dp(8))
        }

        rootScrollView.addView(
            contentRoot,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        pageRoot.addView(rootScrollView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        pageRoot.addView(bottomNavHost, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        ViewCompat.setOnApplyWindowInsetsListener(pageRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            rootScrollView.setPadding(0, systemBars.top, 0, 0)
            rootScrollView.clipToPadding = true
            contentRoot.setPadding(dp(pageGutterDp()), dp(12), dp(pageGutterDp()), dp(12))
            bottomNavHost.setPadding(0, dp(2), 0, max(dp(8), systemBars.bottom + dp(4)))
            insets
        }
        setContentView(pageRoot)
        syncBenchmarkReadiness(render = false)
        renderCurrentTab()
        refreshRecommendationSnapshot()
    }

    override fun onDestroy() {
        progressHandler.removeCallbacks(progressPollRunnable)
        progressHandler.removeCallbacks(modelScopeSearchRunnable)
        progressHandler.removeCallbacks(omniStatusPollRunnable)
        providerStateByProvider.values.forEach { it.cancelRequested = true }
        activeDownloadThreads.values.forEach { it.interrupt() }
        if (isFinishing) PlaygroundInstallerRegistry.values().forEach(PlaygroundArtifactInstaller::cancel)
        galleryIndexCancellation?.cancel()
        galleryIndexCancellation = null
        releaseGallerySearchRuntime(
            "Página fechada, liberando sessão CLIP; arquivos do modelo e índice local ainda preservados.",
        )
        galleryWorker.shutdown()
        galleryThumbnailWorker.shutdownNow()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        if (!modelLoadReceiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                modelLoadStatusReceiver,
                IntentFilter(ModelLoadStatusContract.ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            modelLoadReceiverRegistered = true
        }
    }

    override fun onStop() {
        // Indexing can hold decoded bitmaps and ONNX buffers for minutes. Stop at the Activity
        // boundary; the coordinator checkpoints completed vectors before returning CANCELLED.
        galleryIndexCancellation?.cancel()
        releaseGallerySearchRuntime(
            "App entrou em segundo plano, sessão CLIP liberada; será retomada automaticamente ao voltar para a busca na galeria.",
        )
        if (modelLoadReceiverRegistered) {
            unregisterReceiver(modelLoadStatusReceiver)
            modelLoadReceiverRegistered = false
        }
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_CURRENT_TAB, currentTab.name)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        syncBenchmarkReadiness(render = false)
        refreshRuntimeModelState {
            if (currentTab == AppTab.GALLERY) reconcileGalleryLifecycle()
        }
        refreshRecommendationSnapshot()
        if (currentTab == AppTab.OMNI) refreshOmniLifecycleStatus()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        val shouldReleaseClip = level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        if (shouldReleaseClip && (gallerySearchHost != null || galleryHostOpenInFlight)) {
            releaseGallerySearchRuntime(
                "Sistema solicitou recuperação de memória, sessão CLIP liberada; arquivos do modelo e índice local ainda preservados.",
            )
        }
    }

    private fun renderCurrentTab(resetScroll: Boolean = false) {
        if (!::contentRoot.isInitialized) return
        val targetScrollY = if (resetScroll) 0 else rootScrollView.scrollY
        requiredModelDownloadContainer = null
        contentRoot.removeAllViews()
        when (currentTab) {
            AppTab.HOME -> renderHomeTab(contentRoot)
            AppTab.MODELS -> renderModelsTab(contentRoot)
            AppTab.PLAYGROUND -> renderPlaygroundTab(contentRoot)
            AppTab.GALLERY -> renderGalleryTab(contentRoot)
            AppTab.VISION_MODELS -> renderVisionModelsTab(contentRoot)
            AppTab.G2D_LAB -> renderG2dLabTab(contentRoot)
            AppTab.VISION -> renderVisionTab(contentRoot)
            AppTab.OMNI -> renderOmniTab(contentRoot)
            AppTab.TEST -> renderTestTab(contentRoot)
            AppTab.RESULTS -> renderResultsTab(contentRoot)
            AppTab.API -> renderApiTab(contentRoot)
            AppTab.SETTINGS -> renderSettingsTab(contentRoot)
        }
        if (::bottomNavHost.isInitialized) {
            bottomNavHost.removeAllViews()
            bottomNavHost.addView(buildBottomNavigation())
        }
        rootScrollView.post {
            rootScrollView.scrollTo(0, targetScrollY)
            if (resetScroll) {
                contentRoot.animate().cancel()
                contentRoot.alpha = 0f
                contentRoot.translationY = dp(8).toFloat()
                contentRoot.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180L)
                    .start()
            }
        }
    }

    private fun setTab(tab: AppTab) {
        if (currentTab == tab) return
        currentTab = tab
        // Reset synchronously so an immediate async refresh cannot capture the
        // previous tab's scroll offset and restore it over the new screen.
        rootScrollView.scrollTo(0, 0)
        renderCurrentTab(resetScroll = true)
        if (tab == AppTab.HOME || tab == AppTab.MODELS) {
            refreshRecommendationSnapshot()
        }
        if (tab == AppTab.GALLERY) {
            refreshRuntimeModelState {
                if (currentTab == AppTab.GALLERY) reconcileGalleryLifecycle()
            }
        }
        if (tab == AppTab.OMNI) refreshOmniLifecycleStatus()
    }

    private fun renderHomeTab(content: LinearLayout) {
        content.addView(buildHeader())
        content.addView(space(12))
        content.addView(buildHomeDeviceOverview())
        content.addView(space(12))
        content.addView(buildHomePrimaryCard())
        content.addView(space(12))
        content.addView(buildLocalPrivacyRow())
    }

    private fun renderModelsTab(content: LinearLayout) {
        content.addView(buildCompactHeader("Modelo", "Biblioteca de modelos local e status do runtime no dispositivo", "cube"))
        content.addView(space(12))
        content.addView(buildStorageCard())
        content.addView(space(18))
        content.addView(sectionTitle("Loja de Modelos Confiável", "Fonte, licença, hash e evidências no dispositivo rastreáveis"))
        content.addView(space(10))
        content.addView(buildPlaygroundBrandCard())
        content.addView(space(18))
        content.addView(sectionTitle("Candidatos da Comunidade", "Ordenados por estimativa do dispositivo, não equivalente ao Playground verificado"))
        content.addView(space(10))
        content.addView(buildFeaturedModelScopeCard())
        content.addView(space(18))
        content.addView(sectionTitle("Busca da Comunidade · Não Verificado", "Encontre mais GGUF no ModelScope, verifique a fonte antes de baixar"))
        content.addView(space(10))
        content.addView(buildModelScopeCatalogCard())
        content.addView(space(18))
        content.addView(sectionTitle("Sugestões de Runtime", "Ordenados por capacidade do dispositivo e velocidade histórica"))
        content.addView(space(10))
        content.addView(buildRecommendationCard())
    }

    private fun renderPlaygroundTab(content: LinearLayout) {
        content.addView(buildCompactHeader("Loja de Modelos", "Mobile Model Playground", "cube"))
        content.addView(space(12))
        val catalog = playgroundCatalog
        if (catalog == null) {
            content.addView(
                surfaceCard(Palette.lavender) {
                    addView(cardHeader("Catálogo Indisponível", "Falha ao analisar catálogo interno, nenhuma fonte não verificada exibida.", "alert", Palette.lavender, "FAIL CLOSED"))
                }
            )
            return
        }
        content.addView(
            softInfoBlock(
                "Playground cuida da fonte do modelo, conversor, licença, SHA-256 e evidências; MobileCore cuida da descoberta, carregamento e benchmark local.",
                Palette.lavender,
                maxLines = 4,
            )
        )
        content.addView(space(10))
        content.addView(
            chipButton("Ver projeto Playground", false) {
                openPlaygroundUrl(catalog.sourceRepository, "Não foi possível abrir o projeto Playground")
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)),
        )
        content.addView(space(16))
        catalog.entries.forEachIndexed { index, entry ->
            content.addView(buildPlaygroundEntryCard(entry))
            if (index != catalog.entries.lastIndex) content.addView(space(10))
        }
    }

    private fun renderTestTab(content: LinearLayout) {
        content.addView(buildCompactHeader("Benchmark", "Teste de desempenho de IA no dispositivo", "play"))
        content.addView(space(14))
        content.addView(buildTestChatCard())
        content.addView(space(12))
        content.addView(buildBenchmarkRequirementsCard())
    }

    private fun renderResultsTab(content: LinearLayout) {
        content.addView(buildCompactHeader("Resultados", "Pontuações em duas camadas e explicação de desempenho", "gauge"))
        content.addView(space(18))
        content.addView(buildLatestBenchmarkResultCard())
        content.addView(space(18))
        content.addView(sectionTitle("Histórico", "Últimos 10 testes v2"))
        content.addView(buildBenchmarkHistoryCard())
    }

    private fun renderVisionTab(content: LinearLayout) {
        content.addView(buildCompactHeader("Laboratório Visual", "OCR e sondas visuais leves", "image"))
        content.addView(space(12))
        content.addView(sectionTitle("OCR Visual", "Reconhecimento de texto em imagens"))
        content.addView(buildVisionHeroCard())
        content.addView(space(14))
        content.addView(sectionTitle("Status do Modelo", "ONNX / TFLite / MNN"))
        content.addView(buildVisionModelStatusCard())
        content.addView(space(14))
        content.addView(sectionTitle("Modelo OCR", "Backend visual independente"))
        content.addView(buildOcrModelCard())
        content.addView(space(14))
        content.addView(sectionTitle("CLIP / Classificação", "CIFAR10 / MNIST"))
        content.addView(buildVisionClassificationCard())
        content.addView(space(14))
        content.addView(sectionTitle("Resultado do Reconhecimento", "Status do processamento local"))
        content.addView(buildOcrResultCard())
    }

    private fun renderOmniTab(content: LinearLayout) {
        val screen = OmniLifecycleScreen(this)
        screen.bind(
            OmniLifecyclePresenter.present(omniLifecycleSnapshot),
            OmniLifecycleCallbacks(onAction = ::handleOmniLifecycleAction),
        )
        content.addView(screen)
    }

    private fun renderGalleryTab(content: LinearLayout) {
        val screen = GallerySearchScreen(this)
        screen.bind(
            GallerySearchPresenter.present(gallerySearchState),
            gallerySearchActions(),
            galleryThumbnailBinder(),
        )
        content.addView(screen)
    }

    private fun renderVisionModelsTab(content: LinearLayout) {
        val files = (scanVisionModelFiles() + scanVisionSidecarFiles()).distinctBy(File::getAbsolutePath)
        val screen = VisionModelImportScreen(this)
        screen.bind(
            VisionModelImportPresenter.present(VisionModelImportCatalog.fromFiles(files)),
            VisionModelImportCallbacks(
                onImport = { openVisionModelPicker() },
                onReplace = { openVisionModelPicker() },
                onRetry = { openVisionModelPicker() },
                onDiagnose = {
                    setTab(AppTab.VISION)
                    contentRoot.post { runVisionModelsProbe() }
                },
                onRemove = ::showVisionPackageRemoveDialog
            )
        )
        content.addView(screen)
    }

    private fun renderG2dLabTab(content: LinearLayout) {
        val screen = G2dValidationScreen(this)
        screen.bind(
            G2dValidationPresenter.present(g2dValidationInput),
            G2dValidationCallbacks(
                onStart = ::startNextOxfordPetsStage,
                onCancel = { activeG2dRunner?.cancel() },
                onExport = ::shareLatestOxfordPetsReport,
            )
        )
        content.addView(screen)
    }

    private fun startNextOxfordPetsStage() {
        val runner = OxfordPetsG2dRunner(this)
        val readiness = runner.readiness()
        if (!readiness.optBoolean("ready")) {
            Toast.makeText(this, "Coloque os recursos Oxford-Pets, CLIP e Qwen VLM no diretório G2D do app", Toast.LENGTH_LONG).show()
            return
        }
        val reports = File(requireNotNull(getExternalFilesDir("g2d")), "reports")
        val scale = when {
            !File(reports, "oxford-pets-smoke.json").isFile -> OxfordPetsRunScale.SMOKE
            !File(reports, "oxford-pets-pilot.json").isFile -> OxfordPetsRunScale.PILOT
            else -> OxfordPetsRunScale.FULL
        }
        activeG2dRunner = runner
        g2dValidationInput = G2dValidationInput(
            state = G2dValidationRunState.RUNNING,
            datasetName = "Oxford-Pets（${scale.displayName}）",
            targetSampleCount = scale.expectedSamples,
            totalWorkItems = scale.expectedSamples,
            preparationMessage = "Inferência real no dispositivo em execução; progresso atualizado por estágios CLIP e VLM.",
        )
        renderCurrentTab()
        Thread({
            runCatching {
                runner.run(scale) { progress ->
                    runOnUiThread {
                        g2dValidationInput = g2dValidationInput.copy(
                            completedWorkItems = progress.completed.coerceAtMost(progress.total),
                            totalWorkItems = progress.total,
                            preparationMessage = "${progress.stage}: ${progress.sampleId.orEmpty()}",
                        )
                        if (currentTab == AppTab.G2D_LAB) renderCurrentTab()
                    }
                }
            }.onSuccess { result ->
                runOnUiThread {
                    activeG2dRunner = null
                    g2dValidationInput = g2dValidationInput.copy(
                        state = G2dValidationRunState.COMPLETED,
                        completedWorkItems = scale.expectedSamples,
                        totalWorkItems = scale.expectedSamples,
                        measurements = g2dMeasurements(result.report),
                        preparationMessage = "Relatório real no dispositivo ${scale.displayName} salvo.",
                    )
                    renderCurrentTab()
                }
            }.onFailure { error ->
                runOnUiThread {
                    activeG2dRunner = null
                    g2dValidationInput = g2dValidationInput.copy(
                        state = G2dValidationRunState.FAILED,
                        failureMessage = error.message ?: error.javaClass.simpleName,
                    )
                    renderCurrentTab()
                }
            }
        }, "oxford-pets-g2d-${scale.name.lowercase()}").start()
    }

    private fun g2dMeasurements(report: JSONObject): List<G2dValidationMeasurement> {
        val experiments = mapOf(
            "clip" to G2dValidationExperiment.CLIP_ONLY,
            "vlm" to G2dValidationExperiment.VLM_ONLY,
            "g2d_one" to G2dValidationExperiment.G2D_ONE_THETA,
            "g2d_two" to G2dValidationExperiment.G2D_TWO_THETA,
            "agentic" to G2dValidationExperiment.AGENTIC_G2D,
        )
        val methods = report.getJSONArray("methods")
        return (0 until methods.length()).mapNotNull { index ->
            val row = methods.getJSONObject(index)
            val experiment = experiments[row.getString("key")] ?: return@mapNotNull null
            val routes = row.getJSONObject("routes")
            val toolCounts = row.optJSONObject("tools")?.let { tools ->
                tools.keys().asSequence().mapNotNull { name ->
                    G2dBranchTool.fromWireName(name)?.let { it to tools.getInt(name) }
                }.toMap()
            }
            G2dValidationMeasurement(
                experiment = experiment,
                evaluatedSamples = row.getInt("samples"),
                correctSamples = row.getInt("correct"),
                routeCounts = if (experiment == G2dValidationExperiment.CLIP_ONLY ||
                    experiment == G2dValidationExperiment.VLM_ONLY) null else G2dValidationRouteCounts(
                    routeA = routes.optInt("A"),
                    routeB = routes.optInt("B"),
                    routeC = routes.optInt("C"),
                ),
                p50LatencyMs = row.getDouble("p50_latency_ms"),
                p95LatencyMs = row.getDouble("p95_latency_ms"),
                backend = "Android ONNX Runtime + llama.cpp/libmtmd",
                quantization = if (experiment == G2dValidationExperiment.CLIP_ONLY) "FP32" else "Q4_K_M + BF16 mmproj",
                agentToolCounts = toolCounts,
                agentFallbackCount = row.optInt("fallbacks").takeIf {
                    experiment == G2dValidationExperiment.AGENTIC_G2D
                },
                routerP50LatencyMs = row.optDouble("router_p50_ms").takeIf {
                    experiment == G2dValidationExperiment.AGENTIC_G2D
                },
            )
        }
    }

    private fun shareLatestOxfordPetsReport() {
        val reports = File(requireNotNull(getExternalFilesDir("g2d")), "reports")
        val file = listOf("full", "pilot", "smoke")
            .map { File(reports, "oxford-pets-$it.json") }
            .firstOrNull(File::isFile)
        if (file == null) {
            Toast.makeText(this, "Nenhum relatório de medição real disponível para exportação", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Compartilhar Relatório G2D Oxford-Pets"))
    }

    private fun gallerySearchActions() = object : GallerySearchActions {
        override fun requestGalleryAccess() {
            if (hasFullGalleryImageAccess() ||
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE && hasGalleryImageAccess())
            ) {
                startGalleryIndex()
            } else {
                // Android 14's selected-photos grant is deliberately re-requested here so the
                // system can present its "select more photos" surface.
                requestPermissions(requiredGalleryPermissions(), galleryPermissionRequestCode)
            }
        }

        override fun scanGrantedMedia() = startGalleryIndex()

        override fun retryGalleryIndex() = startGalleryIndex()

        override fun cancelGalleryIndex() {
            galleryIndexCancellation?.cancel()
            Toast.makeText(
                this@MainActivity,
                "Cancelando; vetores concluídos serão salvos para a próxima retomada",
                Toast.LENGTH_SHORT,
            ).show()
        }

        override fun clearGalleryIndex() = clearPersistedGalleryIndex()

        override fun prepareSearchModels() = prepareGallerySearchHost(indexWhenReady = false)

        override fun releaseSearchModels() {
            releaseGallerySearchRuntime(
                "Sessão CLIP liberada manualmente; arquivos do modelo e índice de fotos ainda preservados.",
            )
        }

        override fun searchLocalGallery(query: String, topK: Int) {
            if (!hasGalleryImageAccess()) {
                handleGalleryAccessRevoked(showToast = true)
                return
            }
            runGallerySearch(query, topK)
        }

        override fun updateGalleryQuery(query: String) {
            gallerySearchState = GallerySearchStateMachine.reduce(gallerySearchState, GallerySearchEvent.QueryChanged(query))
        }

        override fun clearGallerySearch() {
            gallerySearchState = GallerySearchStateMachine.reduce(gallerySearchState, GallerySearchEvent.ClearSearch)
            renderCurrentTab()
        }

        override fun openGalleryResult(mediaId: String, contentUri: String) {
            if (!hasGalleryImageAccess()) {
                handleGalleryAccessRevoked(showToast = true)
                return
            }
            val uri = runCatching { Uri.parse(contentUri) }.getOrNull()
            if (uri?.scheme != "content" || uri.authority.isNullOrBlank()) {
                Toast.makeText(this@MainActivity, "Fonte da foto não mais válida", Toast.LENGTH_SHORT).show()
                return
            }
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            }.onFailure {
                Toast.makeText(this@MainActivity, "Não foi possível abrir esta foto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requiredGalleryPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun hasGalleryImageAccess(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ==
                PackageManager.PERMISSION_GRANTED
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED
        else -> ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasFullGalleryImageAccess(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED
        else -> ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasLimitedGalleryImageAccess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !hasFullGalleryImageAccess() &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ==
            PackageManager.PERMISSION_GRANTED

    private fun galleryIndexDirectory(): File = File(noBackupFilesDir, "gallery-search")

    private fun hasCompleteGalleryClipArtifacts(): Boolean {
        val directory = internalVisionModelDir()
        return listOf(
            GalleryClipArtifactSet.IMAGE_ENCODER_FILE,
            GalleryClipArtifactSet.TEXT_ENCODER_FILE,
            "vocab.json",
            "merges.txt",
            "tokenizer_config.json",
        ).all { File(directory, it).isFile }
    }

    private fun reconcileGalleryLifecycle() {
        if (!hasGalleryImageAccess()) {
            handleGalleryAccessRevoked(showToast = false)
            return
        }
        dispatchGalleryEvents(
            GallerySearchEvent.PhotoAccessScopeChanged(hasLimitedGalleryImageAccess()),
        )
        if (gallerySearchState.index is ai.mobilecore.ui.GalleryIndexState.Scanning ||
            gallerySearchState.index is ai.mobilecore.ui.GalleryIndexState.Indexing
        ) {
            return
        }
        val host = gallerySearchHost
        if (host != null) {
            dispatchGalleryEvents(
                GallerySearchEvent.ModelsReady(
                    clipImageEncoder = host.descriptor.imageEncoderName,
                    clipTextEncoder = host.descriptor.textEncoderName,
                    modelId = host.descriptor.modelId,
                    identityVerified = host.descriptor.identityVerified,
                ),
            )
            hydratePersistedGalleryIndex(host)
            return
        }
        dispatchGalleryEvents(
            GallerySearchEvent.AccessAvailable(
                AndroidGallerySearchHost.hasPersistedIndex(galleryIndexDirectory()),
            ),
        )
        if (hasCompleteGalleryClipArtifacts()) {
            prepareGallerySearchHost(indexWhenReady = false)
        }
    }

    private fun handleGalleryAccessRevoked(showToast: Boolean) {
        galleryIndexOperationGeneration += 1L
        galleryIndexCancellation?.cancel()
        galleryIndexCancellation = null
        dispatchGalleryEvents(GallerySearchEvent.AccessRevoked)
        if (showToast) {
            Toast.makeText(this, "Acesso à galeria revogado, por favor reautorize para construir o índice", Toast.LENGTH_SHORT).show()
        }
    }

    private fun galleryRuntimePreflightFailure(): String? {
        if (runtimeModelStateRefreshInFlight) {
            return "Confirmando status do modelo de linguagem local, por favor tente novamente mais tarde."
        }
        if (runtimeReportsLoadedModel || activeModelPath != null || pendingModelPath != null) {
            return "Para evitar que CLIP e GGUF ocupem memória simultaneamente, por favor descarregue ou aguarde o modelo de linguagem local."
        }
        val artifacts = listOf(
            File(internalVisionModelDir(), GalleryClipArtifactSet.IMAGE_ENCODER_FILE),
            File(internalVisionModelDir(), GalleryClipArtifactSet.TEXT_ENCODER_FILE),
        ).filter(File::isFile)
        if (artifacts.size < 2) return null
        val info = ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(info)
        val modelBytes = artifacts.sumOf(File::length)
        val requiredBytes = modelBytes + 384L * BYTES_PER_MB
        if (info.lowMemory || info.availMem < requiredBytes) {
            val needMb = (requiredBytes + BYTES_PER_MB - 1L) / BYTES_PER_MB
            val availableMb = info.availMem / BYTES_PER_MB
            return "Memória disponível insuficiente: CLIP precisa de cerca de ${needMb} MB, atualmente há cerca de ${availableMb} MB."
        }
        return null
    }

    private fun prepareGallerySearchHost(indexWhenReady: Boolean) {
        gallerySearchHost?.let { host ->
            dispatchGalleryEvents(
                GallerySearchEvent.ModelsReady(
                    clipImageEncoder = host.descriptor.imageEncoderName,
                    clipTextEncoder = host.descriptor.textEncoderName,
                    modelId = host.descriptor.modelId,
                    identityVerified = host.descriptor.identityVerified,
                ),
            )
            if (indexWhenReady) startGalleryIndexWithHost(host) else hydratePersistedGalleryIndex(host)
            return
        }
        if (waitForPreviousGalleryRuntimeRelease(indexWhenReady)) return
        galleryRuntimePreflightFailure()?.let { message ->
            dispatchGalleryEvents(GallerySearchEvent.ModelPreparationFailed(message, retryable = true))
            return
        }
        synchronized(galleryHostLock) {
            galleryIndexRequestedAfterHostOpen = galleryIndexRequestedAfterHostOpen || indexWhenReady
            if (galleryHostOpenInFlight) return
            galleryHostOpenInFlight = true
        }
        dispatchGalleryEvents(GallerySearchEvent.ModelPreparationStarted("Codificador duplo CLIP"))
        val openingGeneration = galleryRuntimeGeneration
        galleryWorker.execute {
            val opened = AndroidGallerySearchHost.open(
                contentResolver = contentResolver,
                modelsDirectory = internalVisionModelDir(),
                tokenizerDirectory = internalVisionModelDir(),
                indexDirectory = galleryIndexDirectory(),
            )
            val shouldIndex = synchronized(galleryHostLock) {
                galleryHostOpenInFlight = false
                val requested = galleryIndexRequestedAfterHostOpen
                galleryIndexRequestedAfterHostOpen = false
                requested
            }
            when (opened) {
                is AndroidGallerySearchHostResult.Ready -> {
                    if (isDestroyed || openingGeneration != galleryRuntimeGeneration) {
                        opened.host.close()
                        return@execute
                    }
                    gallerySearchHost = opened.host
                    postGalleryEvents(
                        GallerySearchEvent.ModelsReady(
                            clipImageEncoder = opened.host.descriptor.imageEncoderName,
                            clipTextEncoder = opened.host.descriptor.textEncoderName,
                            modelId = opened.host.descriptor.modelId,
                            identityVerified = opened.host.descriptor.identityVerified,
                        ),
                    )
                    if (shouldIndex) {
                        startGalleryIndexWithHost(opened.host, alreadyOnWorker = true)
                    } else {
                        hydratePersistedGalleryIndex(opened.host, alreadyOnWorker = true)
                    }
                }
                is AndroidGallerySearchHostResult.Blocked -> {
                    val message = galleryFailureMessage(opened.failure)
                    postGalleryEvents(
                        GallerySearchEvent.ModelPreparationFailed(message, opened.failure.retryable),
                        *if (shouldIndex) {
                            arrayOf(GallerySearchEvent.IndexFailed(message, opened.failure.retryable))
                        } else {
                            emptyArray()
                        },
                    )
                }
            }
        }
    }

    private fun startGalleryIndex() {
        if (!hasGalleryImageAccess()) {
            handleGalleryAccessRevoked(showToast = true)
            return
        }
        dispatchGalleryEvents(
            GallerySearchEvent.PhotoAccessScopeChanged(hasLimitedGalleryImageAccess()),
        )
        dispatchGalleryEvents(GallerySearchEvent.AccessGranted)
        val host = gallerySearchHost
        if (host == null) {
            prepareGallerySearchHost(indexWhenReady = true)
        } else {
            startGalleryIndexWithHost(host)
        }
    }

    private fun startGalleryIndexWithHost(
        host: AndroidGallerySearchHost,
        alreadyOnWorker: Boolean = false,
    ) {
        galleryIndexOperationGeneration += 1L
        val token = GalleryCancellationToken()
        galleryIndexCancellation?.cancel()
        galleryIndexCancellation = token
        val clearCorruptIndex = galleryLastIndexFailureCode == GallerySearchFailureCode.INDEX_CORRUPT
        galleryLastIndexFailureCode = null
        val task = {
            if (clearCorruptIndex) host.clearIndex()
            runGalleryIndexOnWorker(host, token)
        }
        if (alreadyOnWorker) task() else galleryWorker.execute { task() }
    }

    private fun hydratePersistedGalleryIndex(
        host: AndroidGallerySearchHost,
        alreadyOnWorker: Boolean = false,
    ) {
        val runtimeGeneration = galleryRuntimeGeneration
        val operationGeneration = galleryIndexOperationGeneration
        val task = {
            val status = host.persistedIndexStatus()
            runOnUiThread {
                if (isDestroyed ||
                    host !== gallerySearchHost ||
                    runtimeGeneration != galleryRuntimeGeneration ||
                    operationGeneration != galleryIndexOperationGeneration ||
                    !hasGalleryImageAccess()
                ) {
                    return@runOnUiThread
                }
                when (status) {
                    is GalleryPersistedIndexStatus.Ready -> dispatchGalleryEvents(
                        GallerySearchEvent.IndexRestored(status.indexedCount, status.updatedAtMs),
                    )
                    GalleryPersistedIndexStatus.Missing -> dispatchGalleryEvents(
                        GallerySearchEvent.AccessAvailable(persistedIndexDetected = false),
                    )
                    GalleryPersistedIndexStatus.Invalidated -> {
                        galleryLastIndexFailureCode = GallerySearchFailureCode.MODEL_DIGEST_MISMATCH
                        dispatchGalleryEvents(
                            GallerySearchEvent.IndexFailed(
                                "Modelo CLIP alterado, recrie o índice de fotos local.",
                                retryable = true,
                            ),
                        )
                    }
                    GalleryPersistedIndexStatus.Corrupt -> {
                        galleryLastIndexFailureCode = GallerySearchFailureCode.INDEX_CORRUPT
                        dispatchGalleryEvents(
                            GallerySearchEvent.IndexFailed(
                                "Índice de fotos local corrompido, limpe e recrie.",
                                retryable = true,
                            ),
                        )
                    }
                }
            }
        }
        if (alreadyOnWorker) task() else galleryWorker.execute { task() }
    }

    private fun clearPersistedGalleryIndex() {
        galleryIndexOperationGeneration += 1L
        val operationGeneration = galleryIndexOperationGeneration
        galleryIndexCancellation?.cancel()
        galleryIndexCancellation = null
        galleryWorker.execute {
            val cleared = runCatching {
                gallerySearchHost?.clearIndex()
                    ?: AndroidGallerySearchHost.clearPersistedIndex(galleryIndexDirectory())
            }.isSuccess
            runOnUiThread {
                if (isDestroyed || operationGeneration != galleryIndexOperationGeneration) return@runOnUiThread
                if (cleared) {
                    galleryLastIndexFailureCode = null
                    dispatchGalleryEvents(GallerySearchEvent.IndexCleared)
                    Toast.makeText(this, "Índice de fotos local limpo", Toast.LENGTH_SHORT).show()
                } else {
                    dispatchGalleryEvents(
                        GallerySearchEvent.IndexFailed("Índice da Galeria · Falhou", retryable = true),
                    )
                }
            }
        }
    }

    private fun runGalleryIndexOnWorker(
        host: AndroidGallerySearchHost,
        token: GalleryCancellationToken,
    ) {
        var scanAnnounced = false
        var lastProgressAtMs = 0L
        val outcome = host.buildOrUpdateIndex(
            selection = GalleryPhotoSelection.MediaStoreImages(),
            cancellation = token,
        ) { progress ->
            if (galleryIndexCancellation !== token) return@buildOrUpdateIndex
            val now = SystemClock.elapsedRealtime()
            val shouldRender = !scanAnnounced ||
                progress.processedCount == progress.totalCount ||
                now - lastProgressAtMs >= 250L
            if (!shouldRender) return@buildOrUpdateIndex
            lastProgressAtMs = now
            val events = buildList<GallerySearchEvent> {
                if (!scanAnnounced) {
                    scanAnnounced = true
                    add(GallerySearchEvent.ScanProgress(progress.totalCount))
                    add(GallerySearchEvent.ScanCompleted(progress.totalCount))
                }
                add(
                    GallerySearchEvent.IndexProgress(
                        progress.processedCount,
                        progress.totalCount,
                        progress.skippedCount,
                    ),
                )
            }
            postGalleryEvents(*events.toTypedArray())
        }
        if (galleryIndexCancellation !== token) return
        galleryIndexCancellation = null
        when (outcome) {
            is GalleryIndexOutcome.Completed -> {
                val prefix = if (scanAnnounced) emptyArray() else arrayOf<GallerySearchEvent>(
                    GallerySearchEvent.ScanProgress(outcome.stats.discoveredCount),
                    GallerySearchEvent.ScanCompleted(outcome.stats.discoveredCount),
                )
                postGalleryEvents(
                    *prefix,
                    GallerySearchEvent.IndexCompleted(
                        indexedCount = outcome.snapshot.entries.size,
                        completedAtMs = outcome.snapshot.updatedAtMs,
                        skippedCount = outcome.stats.skippedCount,
                    ),
                )
            }
            is GalleryIndexOutcome.Cancelled -> {
                galleryLastIndexFailureCode = outcome.failure.code
                postGalleryEvents(
                    GallerySearchEvent.IndexFailed(
                        galleryFailureMessage(outcome.failure),
                        outcome.failure.retryable,
                    ),
                )
            }
            is GalleryIndexOutcome.Failed -> {
                galleryLastIndexFailureCode = outcome.failure.code
                postGalleryEvents(
                    GallerySearchEvent.IndexFailed(
                        galleryFailureMessage(outcome.failure),
                        outcome.failure.retryable,
                    ),
                )
            }
        }
    }

    private fun runGallerySearch(query: String, topK: Int) {
        if (!hasGalleryImageAccess()) {
            handleGalleryAccessRevoked(showToast = true)
            return
        }
        val normalized = query.trim()
        gallerySearchState = GallerySearchStateMachine.reduce(
            gallerySearchState,
            GallerySearchEvent.QueryChanged(normalized),
        )
        gallerySearchState = GallerySearchStateMachine.reduce(
            gallerySearchState,
            GallerySearchEvent.SearchStarted,
        )
        if (currentTab == AppTab.GALLERY) renderCurrentTab()
        val host = gallerySearchHost
        if (host == null) {
            dispatchGalleryEvents(
                GallerySearchEvent.SearchFailed(normalized, "Modelo de busca CLIP ainda não está pronto."),
            )
            return
        }
        galleryWorker.execute {
            if (!hasGalleryImageAccess()) {
                postGalleryEvents(GallerySearchEvent.AccessRevoked)
                return@execute
            }
            when (val outcome = host.search(normalized, topK)) {
                is GalleryQueryOutcome.Results -> {
                    val results = outcome.hits.mapIndexed { index, hit ->
                        val displayName = resolveGalleryDisplayName(hit.photo.contentUri)
                        val format = hit.photo.mimeType.substringAfter('/', "image").uppercase(Locale.US)
                        GallerySearchResult(
                            mediaId = hit.photo.mediaId,
                            contentUri = hit.photo.contentUri,
                            title = displayName ?: "Foto local ${index + 1}",
                            subtitle = "Índice local · $format",
                            similarity = hit.similarity,
                            source = GalleryResultSource.CLIP_DIRECT,
                        )
                    }
                    postGalleryEvents(GallerySearchEvent.SearchCompleted(normalized, results))
                }
                is GalleryQueryOutcome.Blocked -> postGalleryEvents(
                    GallerySearchEvent.SearchFailed(normalized, galleryFailureMessage(outcome.failure)),
                )
            }
        }
    }

    private fun galleryFailureMessage(failure: GallerySearchFailure): String = when (failure.code) {
        GallerySearchFailureCode.ACCESS_DENIED -> "Acesso à galeria revogado, por favor reautorize para construir o índice"
        GallerySearchFailureCode.IMAGE_ENCODER_UNAVAILABLE ->
            "Arquivos do modelo CLIP não encontrados"
        GallerySearchFailureCode.TEXT_ENCODER_UNAVAILABLE ->
            "Arquivos do modelo CLIP não encontrados"
        GallerySearchFailureCode.TEXT_TOKENIZER_UNAVAILABLE ->
            "Arquivos do modelo CLIP não encontrados"
        GallerySearchFailureCode.MODEL_ABI_MISMATCH -> "Falha ao carregar o modelo CLIP"
        GallerySearchFailureCode.MODEL_LOAD_FAILED -> "Memória insuficiente para executar o modelo CLIP"
        GallerySearchFailureCode.MODEL_DIGEST_MISMATCH -> "Dados do índice corrompidos"
        GallerySearchFailureCode.INDEX_CORRUPT -> "Dados do índice corrompidos"
        GallerySearchFailureCode.INDEX_MISSING -> "Falha ao construir o índice de fotos"
        GallerySearchFailureCode.MEDIA_NOT_FOUND -> "Nenhuma foto disponível para indexação"
        GallerySearchFailureCode.IMAGE_DECODE_FAILED -> "Dados do índice corrompidos"
        GallerySearchFailureCode.EMBEDDING_DIMENSION_MISMATCH,
        GallerySearchFailureCode.INVALID_EMBEDDING,
        -> "Falha ao carregar o modelo CLIP"
        GallerySearchFailureCode.CANCELLED -> "Índice cancelado; vetores locais concluídos serão reutilizados na próxima tentativa."
        GallerySearchFailureCode.UNSUPPORTED_URI -> "Nenhuma foto disponível para indexação"
        GallerySearchFailureCode.IO_FAILED -> "Armazenamento cheio, não é possível construir o índice"
    }

    private fun dispatchGalleryEvents(vararg events: GallerySearchEvent) {
        if (isDestroyed) return
        events.forEach { event ->
            gallerySearchState = GallerySearchStateMachine.reduce(gallerySearchState, event)
        }
        if (currentTab == AppTab.GALLERY) renderCurrentTab()
    }

    private fun postGalleryEvents(vararg events: GallerySearchEvent) {
        runOnUiThread { dispatchGalleryEvents(*events) }
    }

    private fun resolveGalleryDisplayName(contentUri: String): String? {
        val uri = runCatching { Uri.parse(contentUri) }.getOrNull() ?: return null
        if (uri.scheme != "content" || uri.authority.isNullOrBlank()) return null
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) null else {
                        val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (column < 0) null else cursor.getString(column)?.takeIf(String::isNotBlank)
                    }
                }
        }.getOrNull()
    }

    private fun galleryThumbnailBinder() = GalleryThumbnailBinder { target, result ->
        val expectedUri = result.contentUri
        target.tag = expectedUri
        runCatching {
            galleryThumbnailWorker.execute {
                val bitmap = loadGalleryThumbnail(expectedUri)
                target.post {
                    if (target.tag == expectedUri && target.isAttachedToWindow) {
                        if (bitmap != null) target.setImageBitmap(bitmap)
                    } else {
                        bitmap?.recycle()
                    }
                }
            }
        }
    }

    private fun loadGalleryThumbnail(contentUri: String): Bitmap? {
        val uri = runCatching { Uri.parse(contentUri) }.getOrNull() ?: return null
        if (uri.scheme != "content" || uri.authority.isNullOrBlank()) return null
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(uri, Size(512, 512), null)
            } else {
                contentResolver.openInputStream(uri)?.use { input ->
                    val decoder = BitmapRegionDecoder.newInstance(input, false)
                        ?: return@use null
                    try {
                        val plan = ClipImageSampling.plan(decoder.width, decoder.height, 512)
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = plan.inSampleSize
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val bitmap = decoder.decodeRegion(
                            Rect(plan.left, plan.top, plan.right, plan.bottom),
                            options,
                        ) ?: return@use null
                        val pixels = bitmap.width.toLong() * bitmap.height.toLong()
                        if (!ClipImageSampling.withinDecodeBudget(
                                pixels,
                                bitmap.allocationByteCount.toLong(),
                            )
                        ) {
                            bitmap.recycle()
                            null
                        } else {
                            bitmap
                        }
                    } finally {
                        decoder.recycle()
                    }
                }
            }
        }.getOrNull()
    }

    private fun waitForPreviousGalleryRuntimeRelease(indexWhenReady: Boolean): Boolean {
        val barriers = GalleryRuntimeReleaseBarrier.pending()
        if (barriers.isEmpty()) return false
        synchronized(galleryHostLock) {
            galleryIndexRequestedAfterHostOpen = galleryIndexRequestedAfterHostOpen || indexWhenReady
            if (galleryHostOpenInFlight) return true
            galleryHostOpenInFlight = true
        }
        val generation = galleryRuntimeGeneration
        dispatchGalleryEvents(GallerySearchEvent.ModelPreparationStarted("Sessão CLIP anterior liberada"))
        galleryWorker.execute {
            val released = barriers.all { barrier ->
                runCatching { barrier.get(30L, TimeUnit.SECONDS) }.isSuccess.also {
                    GalleryRuntimeReleaseBarrier.clear(barrier)
                }
            }
            val shouldIndex = synchronized(galleryHostLock) {
                galleryHostOpenInFlight = false
                val requested = galleryIndexRequestedAfterHostOpen
                galleryIndexRequestedAfterHostOpen = false
                requested
            }
            runOnUiThread {
                if (isDestroyed || generation != galleryRuntimeGeneration) return@runOnUiThread
                if (released) {
                    prepareGallerySearchHost(shouldIndex)
                } else {
                    dispatchGalleryEvents(
                        GallerySearchEvent.ModelPreparationFailed(
                            "Sessão CLIP anterior não foi liberada com segurança; para evitar duas sessões, reinicie completamente o app e tente novamente.",
                            retryable = false,
                        ),
                    )
                }
            }
        }
        return true
    }

    private fun releaseGallerySearchRuntime(reason: String): Future<*>? {
        val hadRuntime = gallerySearchHost != null || galleryHostOpenInFlight
        val wasIndexing = gallerySearchState.index is ai.mobilecore.ui.GalleryIndexState.Scanning ||
            gallerySearchState.index is ai.mobilecore.ui.GalleryIndexState.Indexing
        val hadModelState = gallerySearchState.models is ai.mobilecore.ui.GalleryModelState.Ready ||
            gallerySearchState.models is ai.mobilecore.ui.GalleryModelState.Preparing
        if (!hadRuntime && !wasIndexing && !hadModelState) return null

        galleryRuntimeGeneration += 1L
        galleryIndexOperationGeneration += 1L
        galleryIndexCancellation?.cancel()
        galleryIndexCancellation = null
        synchronized(galleryHostLock) {
            galleryIndexRequestedAfterHostOpen = false
        }
        val previous = gallerySearchHost
        gallerySearchHost = null
        val events = buildList<GallerySearchEvent> {
            add(GallerySearchEvent.ModelsReleased(reason))
            if (wasIndexing) {
                add(
                    GallerySearchEvent.IndexFailed(
                        "Índice pausado; vetores concluídos foram salvos, pode recarregar o modelo para continuar.",
                        retryable = true,
                    ),
                )
            }
        }
        dispatchGalleryEvents(*events.toTypedArray())
        val release = runCatching {
            galleryWorker.submit { previous?.close() }
        }.getOrNull()
        release?.let(GalleryRuntimeReleaseBarrier::register)
        return release
    }

    private fun invalidateGallerySearchRuntime(reason: String) {
        val hadRuntime = gallerySearchHost != null || galleryHostOpenInFlight
        galleryRuntimeGeneration += 1L
        galleryIndexOperationGeneration += 1L
        galleryIndexCancellation?.cancel()
        galleryIndexCancellation = null
        synchronized(galleryHostLock) {
            galleryIndexRequestedAfterHostOpen = false
        }
        val previous = gallerySearchHost
        gallerySearchHost = null
        if (hadRuntime) {
            runCatching { galleryWorker.submit { previous?.close() } }
                .getOrNull()
                ?.let(GalleryRuntimeReleaseBarrier::register)
        }
        dispatchGalleryEvents(
            GallerySearchEvent.ModelPreparationFailed(reason, retryable = true),
            GallerySearchEvent.IndexFailed("Modelo alterado, recrie o índice de fotos local.", retryable = true),
        )
    }

    private fun showVisionPackageRemoveDialog(packageId: String) {
        val allFiles = (scanVisionModelFiles() + scanVisionSidecarFiles()).distinctBy(File::getAbsolutePath)
        val artifactNames = VisionModelImportCatalog.fromFiles(allFiles)
            .firstOrNull { it.id == packageId }?.artifacts?.map { it.fileName }.orEmpty().toSet()
        if (artifactNames.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("Remover pacote do modelo?")
            .setMessage("Serão deletados ${artifactNames.size} arquivos do diretório privado do aplicativo. Esta operação não afeta o álbum do sistema.")
            .setNegativeButton("Manter", null)
            .setPositiveButton("Remover") { _, _ ->
                allFiles.filter { it.name in artifactNames }.forEach(File::delete)
                if (packageId == "clip_retrieval") {
                    invalidateGallerySearchRuntime("Pacote do modelo visual removido, prepare novamente o modelo de busca CLIP.")
                }
                renderCurrentTab()
            }
            .show()
    }

    private fun renderApiTab(content: LinearLayout) {
        content.addView(buildCompactHeader("Interface de desenvolvedor", "API local e diagnósticos", "cloud"))
        content.addView(space(12))
        content.addView(buildApiEndpointCard())
        content.addView(space(14))
        content.addView(buildApiActionStrip())
        content.addView(space(14))
        content.addView(buildApiRoutesCard())
        content.addView(space(14))
        content.addView(buildStatusCard())
    }

    private fun renderSettingsTab(content: LinearLayout) {
        content.addView(buildCompactHeader("Meu Perfil", "Privacidade, dados locais e laboratório", "person"))
        content.addView(space(12))
        content.addView(sectionTitle("Meu Perfil", "Privacidade e Dados Locais"))
        content.addView(buildSettingsCard())
        content.addView(space(18))
        content.addView(sectionTitle("Laboratório", "Recursos Avançados"))
        content.addView(buildLabAccessCard())
    }

    private fun buildHeader(): View {
        val lifecycle = requiredBenchmarkModelLifecycle()
        val statusAccent = modelLifecycleAccent(lifecycle.tone)
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            minimumHeight = dp(52)
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(label("TuiMa", 22f, Palette.mint, Typeface.BOLD).apply { letterSpacing = -0.02f })
                    addView(space(2))
                    addView(label("IA local em um vislumbre", 11.5f, Palette.muted, Typeface.BOLD))
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            runtimeChipText = label(lifecycle.statusLabel, 11f, statusAccent, Typeface.BOLD)
            addView(
                chip(runtimeChipText, tint(statusAccent, 0.12f), statusAccent),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)).apply { marginStart = dp(10) }
            )
            contentDescription = "Console IA local TuiMa, modelo padrão ${lifecycle.statusLabel}"
        }
    }

    private fun buildHomeDeviceOverview(): View {
        val profile = probeDeviceProfile()
        val telemetry = runCatching { AndroidBenchmarkTelemetry(applicationContext).sample() }.getOrNull()
        val lifecycle = requiredBenchmarkModelLifecycle()
        val lifecycleAccent = modelLifecycleAccent(lifecycle.tone)
        val availableRam = if (profile.availableRamMb >= 1024L) {
            "${"%.1f".format(Locale.US, profile.availableRamMb / 1024.0)} GB"
        } else {
            "${profile.availableRamMb} MB"
        }
        val temperature = telemetry?.batteryTemperatureCelsius?.let {
            "${"%.1f".format(Locale.US, it)}°C"
        } ?: "Detectando"

        return surfaceCard(Palette.mint, gradient = true) {
            addView(
                LinearLayout(context).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        IconBadgeView(context, "chip", Palette.mint),
                        LinearLayout.LayoutParams(dp(42), dp(42)).apply { marginEnd = dp(11) }
                    )
                    addView(
                        LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            addView(label(profile.model, 15.5f, Palette.deepInk, Typeface.BOLD).apply { maxLines = 2 })
                            addView(space(3))
                            addView(label("${profile.abi} · ${profile.backend} · CPU", 11.5f, Palette.muted, Typeface.NORMAL).apply { maxLines = 2 })
                        },
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    )
                    addView(
                        chip(
                            label(lifecycle.statusLabel, 10.8f, lifecycleAccent, Typeface.BOLD),
                            tint(lifecycleAccent, 0.10f),
                            lifecycleAccent,
                        ),
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)).apply { marginStart = dp(8) }
                    )
                }
            )
            addView(space(14))
            addView(thinDivider())
            addView(space(12))
            addView(
                LinearLayout(context).apply {
                    addView(instrumentMetric("Memória Disponível", availableRam, Palette.blue), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(instrumentMetric("Núcleos CPU", "${profile.coreCount} núcleos", Palette.sky), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(instrumentMetric("Bateria Atual", telemetry?.let { "${it.batteryPercent}%" } ?: "Detectando", Palette.mint), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(instrumentMetric("Temperatura do Dispositivo", temperature, if ((telemetry?.batteryTemperatureCelsius ?: 0.0) >= 42.0) Palette.amber else Palette.mint), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                }
            )
            addView(space(12))
            addView(label("Modelo padrão · ${lifecycle.supportingText}", 11.5f, Palette.muted, Typeface.NORMAL).apply { maxLines = 2 })
        }
    }

    private fun instrumentMetric(title: String, value: String, accent: Int): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), 0, dp(4), 0)
            addView(autoSizeSingleLineLabel(value, 13f, 9f, accent, Typeface.BOLD).apply { gravity = Gravity.CENTER })
            addView(space(3))
            addView(label(title, 9.7f, Palette.muted, Typeface.NORMAL).apply { gravity = Gravity.CENTER; maxLines = 1 })
            contentDescription = "$title，$value"
        }
    }

    private fun buildHomeIntro(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label("TODAY ON DEVICE", 10.5f, Palette.mintDark, Typeface.BOLD).apply { letterSpacing = 0.12f })
            addView(space(7))
            addView(autoSizeSingleLineLabel("IA local em um vislumbre", 27f, 21f, Palette.deepInk, Typeface.BOLD))
            addView(space(7))
            addView(label("A disponibilidade do modelo, adequação do dispositivo e resultados do benchmark são baseados no status real local.", 13f, Palette.muted, Typeface.NORMAL).apply {
                setLineSpacing(dp(2).toFloat(), 1f)
            })
            contentDescription = "IA local em um vislumbre. Modelo, dispositivo e benchmark baseados no status real local"
        }
    }

    private fun buildCompactHeader(title: String, subtitle: String, icon: String): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(TuiMaTheme.compactHeaderHeightDp)
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(label(title, 23f, Palette.deepInk, Typeface.BOLD))
                    addView(space(4))
                    addView(label(subtitle, 12f, Palette.muted, Typeface.NORMAL).apply { maxLines = 2 })
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            tag = icon
            contentDescription = "$title，$subtitle"
        }
    }

    private fun buildHomePrimaryCard(): View {
        val state = benchmarkUiStateMachine.state
        val latest = latestScoredBenchmarkReport()
        val latestScore = latest?.optJSONObject("score")
        val modelFile = requiredBenchmarkModel()
        val lifecycle = requiredBenchmarkModelLifecycle()
        val accent = modelLifecycleAccent(lifecycle.tone)
        val headline = latestScore?.optInt("headline")
        val canonical = latestScore?.optInt("canonical")

        return surfaceCard(accent, gradient = true) {
            addView(
                LinearLayout(context).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            addView(label("Modelo padrão de benchmark", 11f, Palette.muted, Typeface.BOLD))
                            addView(space(4))
                            addView(label("Qwen2.5 0.5B · Q4_K_M", 16.5f, Palette.deepInk, Typeface.BOLD).apply { maxLines = 2 })
                        },
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    )
                    addView(
                        chip(label(lifecycle.statusLabel, 10.8f, accent, Typeface.BOLD), tint(accent, 0.11f), accent),
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32)).apply { marginStart = dp(10) }
                    )
                }
            )
            addView(space(10))
            addView(label(lifecycle.supportingText, 12.5f, Palette.muted, Typeface.NORMAL).apply { maxLines = 3 })

            if (headline != null && canonical != null) {
                addView(space(16))
                addView(thinDivider())
                addView(space(14))
                addView(label("Último resultado", 10.5f, Palette.muted, Typeface.BOLD))
                addView(space(5))
                addView(
                    autoSizeSingleLineLabel(formatHeadlineScore(headline), 34f, 24f, Palette.blue, Typeface.BOLD),
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                )
                addView(space(5))
                addView(label("TuiMa · Pontuação padrão $canonical / 1000", 12f, Palette.mintDark, Typeface.BOLD).apply { maxLines = 2 })
            }

            addView(space(16))
            val actionText = when {
                state.isRunning -> "Ver progresso do benchmark"
                lifecycle.phase == ModelLifecyclePhase.LOADED -> if (latestScore != null) "Refazer benchmark" else "Iniciar benchmark padrão"
                lifecycle.phase == ModelLifecyclePhase.DOWNLOADED || lifecycle.phase == ModelLifecyclePhase.LOAD_FAILED -> lifecycle.actionLabel
                lifecycle.phase == ModelLifecyclePhase.LOADING -> "Carregando modelo"
                lifecycle.phase == ModelLifecyclePhase.DOWNLOADING -> "Pausar download"
                lifecycle.phase == ModelLifecyclePhase.PAUSED -> "Continuar download"
                lifecycle.phase == ModelLifecyclePhase.DOWNLOAD_FAILED -> "Baixar novamente"
                else -> "Baixar modelo padrão · 469 MB"
            }
            addView(
                pillButton(actionText, Palette.mintDark, Palette.mint) {
                    when {
                        state.isRunning -> setTab(AppTab.TEST)
                        lifecycle.phase == ModelLifecyclePhase.LOADED -> {
                            selectedBenchmarkProfile = BenchmarkProfile.STANDARD
                            setTab(AppTab.TEST)
                        }
                        lifecycle.phase == ModelLifecyclePhase.DOWNLOADED || lifecycle.phase == ModelLifecyclePhase.LOAD_FAILED -> {
                            modelFile?.let(::ensureNotificationPermissionAndLoadModel)
                        }
                        lifecycle.phase == ModelLifecyclePhase.DOWNLOADING -> {
                            val item = modelHubItems.first { it.fileName == requiredBenchmarkModelName() }
                            pauseModelDownload(downloadTaskKey(item))
                        }
                        else -> downloadRequiredBenchmarkModel()
                    }
                }.apply {
                    contentDescription = actionText
                    isEnabled = lifecycle.actionEnabled || state.isRunning || lifecycle.phase == ModelLifecyclePhase.LOADED
                    alpha = if (isEnabled) 1f else 0.55f
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52))
            )
        }
    }

    private fun buildHomeReadinessCard(): View {
        val model = requiredBenchmarkModel()
        val lifecycle = requiredBenchmarkModelLifecycle()
        val telemetry = runCatching { AndroidBenchmarkTelemetry(applicationContext).sample() }.getOrNull()
        val thermalReady = telemetry?.thermalStatus?.ordinal?.let { it <= ThermalStatus.LIGHT.ordinal } ?: true
        return surfaceCard(Palette.sky) {
            addView(label("Preparação local", 14f, Palette.deepInk, Typeface.BOLD))
            addView(space(10))
            addView(readinessRow("Modelo padrão", lifecycle.statusLabel, model != null, modelLifecycleAccent(lifecycle.tone)))
            addView(thinDivider())
            addView(readinessRow("Bateria Atual", telemetry?.let { "${it.batteryPercent}%" } ?: "Detectando", (telemetry?.batteryPercent ?: 30) >= 30))
            addView(thinDivider())
            addView(readinessRow("Controle térmico do dispositivo", if (thermalReady) "Adequado para benchmark" else "Recomenda resfriamento", thermalReady))
            addView(space(8))
            addView(label("Antes de iniciar o benchmark, verificará novamente integridade do modelo, armazenamento e runtime.", 11.8f, Palette.muted, Typeface.NORMAL).apply { maxLines = 3 })
        }
    }

    private fun readinessMetric(title: String, value: String, ready: Boolean): View {
        val accent = if (ready) Palette.mintDark else Palette.amber
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(tint(accent, 0.08f), tint(accent, 0.22f), 7f)
            addView(label(if (ready) "✓" else "!", 16f, accent, Typeface.BOLD))
            addView(space(2))
            addView(label(value, 12.5f, accent, Typeface.BOLD).apply { maxLines = 1 })
            addView(space(2))
            addView(label(title, 10.2f, Palette.muted, Typeface.NORMAL))
            contentDescription = "$title，$value"
        }
    }

    private fun buildRequiredModelDownloadCard(): View {
        requiredModelDownloadContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        renderRequiredModelDownloadStatus()
        return surfaceCard(Palette.mint, gradient = true) {
            addView(requiredModelDownloadContainer)
        }
    }

    private fun renderRequiredModelDownloadStatus() {
        val container = requiredModelDownloadContainer ?: return
        val item = modelHubItems.firstOrNull { it.fileName == requiredBenchmarkModelName() } ?: return
        val taskKey = downloadTaskKey(item)
        val state = providerStateByProvider.getOrPut(taskKey) { ModelDownloadState(item) }
        val phase = when (state.status) {
            DownloadState.DOWNLOADING -> StandardModelDownloadPhase.DOWNLOADING
            DownloadState.PAUSED -> StandardModelDownloadPhase.PAUSED
            DownloadState.FAILED, DownloadState.CANCELLED -> StandardModelDownloadPhase.FAILED
            DownloadState.SUCCESS -> StandardModelDownloadPhase.COMPLETE
            DownloadState.IDLE -> StandardModelDownloadPhase.IDLE
        }
        val model = HomeScreenPresenter.standardModelDownload(
            phase = phase,
            bytesDownloaded = state.bytesDownloaded,
            totalBytes = state.totalBytes,
            startedAtMs = state.transferStartedAtMs,
            startedBytes = state.transferStartedBytes,
            nowMs = System.currentTimeMillis()
        )

        container.removeAllViews()
        container.addView(cardHeader(model.title, "Qwen2.5 0.5B · Modelo padrão TuiMa", "download", Palette.mint, "Local"))
        container.addView(space(12))
        container.addView(
            ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = model.progressPercent
                progressTintList = ColorStateList.valueOf(Palette.mintDark)
                progressBackgroundTintList = ColorStateList.valueOf(tint(Palette.muted, 0.16f))
                contentDescription = "Progresso do download do modelo padrão ${model.progressPercent}%"
                visibility = if (phase == StandardModelDownloadPhase.IDLE) View.GONE else View.VISIBLE
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8))
        )
        if (phase != StandardModelDownloadPhase.IDLE) container.addView(space(10))
        container.addView(
            LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(label(model.progressLabel, 13f, Palette.ink, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(label(model.remainingLabel, 12f, Palette.muted, Typeface.NORMAL))
            }
        )
        if (!state.failureMessage.isNullOrBlank() && phase == StandardModelDownloadPhase.FAILED) {
            container.addView(space(6))
            container.addView(label(state.failureMessage.orEmpty(), 12f, Palette.blue, Typeface.NORMAL).apply { maxLines = 2 })
        }
        container.addView(space(12))
        container.addView(
            pillButton(model.actionLabel, Palette.mintDark, Palette.mint) {
                when (phase) {
                    StandardModelDownloadPhase.DOWNLOADING -> pauseModelDownload(taskKey)
                    StandardModelDownloadPhase.COMPLETE -> {
                        requiredBenchmarkModel()?.let(::ensureNotificationPermissionAndLoadModel)
                    }
                    else -> enqueueModelDownload(item)
                }
            }.apply {
                isEnabled = model.actionEnabled
                contentDescription = model.actionLabel
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(TuiMaTheme.minimumTouchTargetDp))
        )
    }

    private fun readinessRow(title: String, value: String, ready: Boolean, accentOverride: Int? = null): View {
        val accent = accentOverride ?: if (ready) Palette.mintDark else Palette.blue
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(48)
            setPadding(0, dp(7), 0, dp(7))
            addView(
                View(context).apply { background = rounded(accent, Color.TRANSPARENT, 4f) },
                LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(10) }
            )
            addView(label(title, 13.5f, Palette.ink, Typeface.BOLD).apply { maxLines = 2 }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(label(value, 12f, accent, Typeface.BOLD).apply { maxLines = 2; gravity = Gravity.END }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(12) })
            contentDescription = "$title，$value"
        }
    }

    private fun buildModelLifecycleBanner(lifecycle: ModelLifecycleUiModel, modelName: String): View {
        val accent = modelLifecycleAccent(lifecycle.tone)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(tint(accent, 0.075f), Color.TRANSPARENT, TuiMaTheme.cardRadiusDp)
            setPadding(dp(12), dp(11), dp(12), dp(11))
            addView(
                LinearLayout(context).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        View(context).apply { background = rounded(accent, Color.TRANSPARENT, 4f) },
                        LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(9) }
                    )
                    addView(label(modelName, 12.8f, Palette.ink, Typeface.BOLD).apply { maxLines = 2 }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(label(lifecycle.statusLabel, 11.5f, accent, Typeface.BOLD).apply { maxLines = 2; gravity = Gravity.END }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(10) })
                }
            )
            addView(space(6))
            addView(label(lifecycle.supportingText, 11.5f, Palette.muted, Typeface.NORMAL).apply { maxLines = 3 })
            contentDescription = "$modelName，${lifecycle.statusLabel}。${lifecycle.supportingText}"
        }
    }

    private fun buildHomeLatestResultCard(): View {
        val latest = latestScoredBenchmarkReport()
        val snapshot = latest?.let(ResultsScreenPresenter::parse)
        return surfaceCard(Palette.lavender) {
            if (latest == null || snapshot == null) {
                addView(cardHeader("Nenhum resultado ainda", "Após um teste, veja o desempenho 5 dimensões aqui", "gauge", Palette.lavender))
                addView(space(12))
                addView(chipButton("Ir para benchmark", false) { setTab(AppTab.TEST) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
            } else {
                val insight = ResultsScreenPresenter.insight(snapshot)
                addView(cardHeader("Análise de capacidade · ${insight.rating}", insight.summary, "gauge", Palette.lavender, "${snapshot.canonicalScore}/1000", Palette.mintDark))
                addView(space(12))
                addView(softInfoBlock(insight.recommendation, Palette.lavender, maxLines = 3))
                addView(space(12))
                addView(chipButton("Ver resultados 5 dimensões  →", false) { setTab(AppTab.RESULTS) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
            }
        }
    }

    private fun buildLocalPrivacyRow(): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(48)
            setPadding(dp(4), 0, dp(4), 0)
            addView(IconBadgeView(context, "chip", Palette.mintDark), LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginEnd = dp(10) })
            addView(label("Executa offline · Dados ficam no dispositivo", 12.5f, Palette.muted, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(label("›", 22f, Palette.muted, Typeface.NORMAL))
            isClickable = true
            isFocusable = true
            background = ripple(rounded(Color.TRANSPARENT, Color.TRANSPARENT, 7f), Palette.mint)
            contentDescription = "Executa offline, dados ficam no dispositivo. Ver privacidade e dados locais"
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                setTab(AppTab.SETTINGS)
            }
        }
    }

    private fun tuimaWordmark(compact: Boolean): View {
        val latinSize = if (compact) 24f else 42f
        val chineseSize = if (compact) 15f else 23f
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(label("Tui", latinSize, Palette.deepInk, Typeface.BOLD))
            addView(label("Ma", latinSize, Palette.blue, Typeface.BOLD))
            addView(space(if (compact) 6 else 10))
            addView(label("TuiMa", chineseSize, Palette.mintDark, Typeface.BOLD))
        }
    }

    private fun notificationBubble(): View {
        return FrameLayout(this).apply {
            background = ripple(rounded(Palette.surface, Palette.stroke, 22f), Palette.blue)
            elevation = dp(2).toFloat()
            isClickable = true
            isFocusable = true
            contentDescription = "Ver resultados do benchmark"
            setOnClickListener { setTab(AppTab.RESULTS) }
            addView(
                IconBadgeView(context, "gauge", Palette.deepInk),
                FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER)
            )
        }
    }

    private fun surfaceCard(accent: Int, gradient: Boolean = false, block: LinearLayout.() -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = if (gradient) {
                roundedGradient(
                    intArrayOf(
                        Palette.surface,
                        mixColor(Palette.surface, accent, if (TuiMaTheme.isDark) 0.075f else 0.045f),
                    ),
                    TuiMaTheme.cardRadiusDp,
                )
            } else {
                rounded(Palette.surface, tint(Palette.stroke, 0.48f), TuiMaTheme.cardRadiusDp)
            }
            elevation = dp(1).toFloat()
            setPadding(dp(15), dp(16), dp(15), dp(15))
            block()
        }
    }

    private fun cardHeader(
        title: String,
        caption: String,
        icon: String,
        accent: Int,
        badge: String? = null,
        badgeAccent: Int = accent
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                LinearLayout(context).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(IconBadgeView(context, icon, accent), LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(10) })
                    addView(
                        label(title, 15.2f, tint(Palette.ink, 0.88f), Typeface.BOLD).apply { maxLines = 2 },
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    )
                    if (badge != null) {
                        addView(
                            chip(label(badge, 10.8f, badgeAccent, Typeface.BOLD), tint(badgeAccent, 0.12f), badgeAccent),
                            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30)).apply { marginStart = dp(8) }
                        )
                    }
                }
            )
            if (caption.isNotBlank()) {
                addView(space(7))
                addView(label(caption, 12f, Palette.muted, Typeface.NORMAL).apply {
                    maxLines = 3
                    setLineSpacing(dp(2).toFloat(), 1f)
                })
            }
        }
    }

    private fun softInfoBlock(text: String, accent: Int, maxLines: Int = 2): TextView {
        return label(text, 12.4f, tint(Palette.ink, 0.68f), Typeface.NORMAL).apply {
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(tint(accent, 0.075f), Color.TRANSPARENT, 7f)
            this.maxLines = maxLines
        }
    }

    private fun buildStorageCard(): View {
        val freeMb = runCatching { externalModelDir().freeSpace / (1024 * 1024) }.getOrDefault(0L)
        val totalMb = runCatching { externalModelDir().totalSpace / (1024 * 1024) }.getOrDefault(0L)
        val usedMb = (totalMb - freeMb).coerceAtLeast(0L)
        val storagePercent = if (totalMb > 0L) {
            ((usedMb.toDouble() / totalMb.toDouble()) * 100).roundToInt().coerceIn(0, 100)
        } else {
            0
        }
        val modelBytes = modelDirs()
            .flatMap { it.listFiles()?.toList() ?: emptyList() }
            .filter { it.isFile && it.extension.equals("gguf", ignoreCase = true) }
            .sumOf { it.length() }
        val storageLine = if (totalMb > 0) {
            "Modelo ${formatBytes(modelBytes)} · Disponível ${freeMb / 1024} / ${totalMb / 1024} GB"
        } else {
            "Modelo ${formatBytes(modelBytes)}"
        }
        val localModels = availableGgufModels()
        val activeName = localModels.firstOrNull { it.absolutePath == activeModelPath }?.nameWithoutExtension
        return surfaceCard(Palette.mint) {
            addView(cardHeader("Biblioteca de modelos local", storageLine, "chip", Palette.mint, "${localModels.size} itens"))
            addView(space(12))
            addView(
                ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 100
                    progress = storagePercent
                    progressTintList = ColorStateList.valueOf(Palette.blue)
                    progressBackgroundTintList = ColorStateList.valueOf(tint(Palette.muted, 0.14f))
                    contentDescription = "Armazenamento do dispositivo usado $storagePercent%"
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6))
            )
            addView(space(10))
            addView(readinessRow("Runtime", activeName?.let { "Carregado ${displayModelName(it)}" } ?: "Nenhum modelo carregado", activeName != null))
            addView(thinDivider())
            addView(readinessRow("Arquivos locais", if (localModels.isEmpty()) "Não baixado" else "${localModels.size} itens baixados", localModels.isNotEmpty()))
            addView(space(10))
            addView(
                chipButton("Copiar diretório de modelos", false) {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("TuiMa model directory", externalModelDir().absolutePath))
                    Toast.makeText(this@MainActivity, "Diretório de modelos copiado", Toast.LENGTH_SHORT).show()
                    updateStatus("Diretório de modelos copiado")
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        }
    }

    private fun buildPlaygroundBrandCard(): View {
        val catalog = playgroundCatalog
        val entries = catalog?.entries.orEmpty()
        val localNames = availableGgufModels().mapTo(linkedSetOf()) { it.name }
        val activePath = activeModelPath
        val installedCount = entries.count { entry ->
            PlaygroundPresenter.present(entry, localNames, activePath).localPhase != PlaygroundLocalPhase.NOT_DOWNLOADED
        }
        val recommendedCount = entries.count { entry ->
            PlaygroundPresenter.present(entry, localNames, activePath).recommended
        }
        return surfaceCard(Palette.lavender, gradient = true) {
            addView(
                cardHeader(
                    "Mobile Model Playground",
                    "Adaptação de modelos móveis, fonte e evidências no dispositivo",
                    "cube",
                    Palette.lavender,
                    if (catalog == null) "Catálogo anômalo" else "${entries.size} itens",
                    if (catalog == null) Palette.danger else Palette.lavender,
                )
            )
            addView(space(12))
            if (catalog == null) {
                addView(softInfoBlock("Falha ao analisar catálogo interno; para evitar confusão, modelos da comunidade não serão marcados como verificados.", Palette.danger, 3))
            } else {
                addView(readinessRow("Fonte confiável", "Revisão fixa e SHA-256", true, Palette.lavender))
                addView(thinDivider())
                addView(readinessRow("Descoberta local", "$installedCount / ${entries.size} itens", installedCount > 0, Palette.blue))
                addView(thinDivider())
                addView(readinessRow("Recomendação atual", "$recommendedCount aprovados no gate de exibição", recommendedCount > 0, Palette.mintDark))
                addView(space(11))
                addView(
                    chipButton("Abrir loja de modelos", false) { setTab(AppTab.PLAYGROUND) },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)),
                )
            }
        }
    }

    private fun buildPlaygroundEntryCard(entry: PlaygroundCatalogEntry): View {
        val localNames = availableGgufModels().mapTo(linkedSetOf()) { it.name }
        val activePath = activeModelPath
        val installer = playgroundInstaller(entry)
        val model = PlaygroundPresenter.present(
            entry = entry,
            localFileNames = localNames,
            activeModelPath = activePath,
            managedPrimaryModelPath = installer?.managedPrimaryModelFile()?.absolutePath,
            installSnapshot = installer?.snapshot(),
        )
        val accent = playgroundOriginAccent(entry.origin)
        val artifactBytes = entry.artifacts.sumOf { it.sizeBytes }
        val declaredInputs = entry.declaredCapabilities.inputs.joinToString(" / ").ifBlank { "Não declarado" }
        val declaredOutputs = entry.declaredCapabilities.outputs.joinToString(" / ").ifBlank { "Não declarado" }
        val verifiedInputs = entry.verifiedCapabilities.inputs.joinToString(" / ")
        val verifiedOutputs = entry.verifiedCapabilities.outputs.joinToString(" / ")
        val verifiedCapabilityLabel = when (entry.verifiedCapabilities.status) {
            "pass" -> "$verifiedInputs → $verifiedOutputs"
            "quality_failed" -> "$verifiedInputs → $verifiedOutputs · Qualidade não aprovada"
            else -> "Ainda não testado"
        }
        return surfaceCard(accent) {
            addView(
                LinearLayout(context).apply {
                    gravity = Gravity.TOP
                    addView(
                        LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            addView(label(model.title, 14.2f, Palette.ink, Typeface.BOLD).apply { maxLines = 3 })
                            if (model.metadata.isNotBlank()) {
                                addView(space(4))
                                addView(label("${model.metadata} · ${formatBytes(artifactBytes)}", 11.2f, Palette.muted, Typeface.BOLD).apply { maxLines = 2 })
                            }
                        },
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) },
                    )
                    addView(
                        chip(label(model.originLabel, 10.2f, accent, Typeface.BOLD), tint(accent, 0.11f), accent),
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30)),
                    )
                }
            )
            addView(space(8))
            addView(label("${model.attributionLabel} · ${entry.source.license} · ${entry.source.revision.take(8)}", 11.2f, Palette.muted, Typeface.NORMAL).apply { maxLines = 3 })
            addView(space(7))
            addView(label(entry.summary, 11.8f, tint(Palette.ink, 0.72f), Typeface.NORMAL).apply {
                maxLines = 4
                setLineSpacing(dp(2).toFloat(), 1f)
            })
            addView(space(10))
            addView(readinessRow("Capacidade declarada", "$declaredInputs → $declaredOutputs", false, Palette.muted))
            addView(thinDivider())
            addView(
                readinessRow(
                    "Capacidade testada",
                    verifiedCapabilityLabel,
                    model.validationPassed,
                    if (model.validationPassed) Palette.mintDark else Palette.amber,
                )
            )
            addView(thinDivider())
            addView(readinessRow("Controle de evidências", model.validationLabel, model.validationPassed, if (model.validationPassed) Palette.mintDark else Palette.amber))
            addView(thinDivider())
            addView(readinessRow("Status de distribuição", model.distributionLabel, entry.distribution.published, Palette.blue))
            addView(thinDivider())
            addView(readinessRow("Status local", model.localStatusLabel, false, Palette.blue))
            addView(label(model.localStatusDetail, 10.8f, Palette.muted, Typeface.NORMAL).apply {
                setPadding(dp(2), dp(5), dp(2), 0)
                maxLines = 2
            })
            if (model.localPhase == PlaygroundLocalPhase.DOWNLOADING ||
                model.localPhase == PlaygroundLocalPhase.VERIFYING
            ) {
                addView(space(8))
                addView(
                    ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                        max = 100
                        progress = model.progressPercent
                        progressTintList = ColorStateList.valueOf(Palette.mintDark)
                        progressBackgroundTintList = ColorStateList.valueOf(tint(Palette.muted, 0.16f))
                        contentDescription = "Progresso de instalação do modelo confiável ${model.progressPercent}%"
                    },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7)),
                )
            }
            addView(space(10))
            if (installer != null) {
                addView(
                    pillButton(model.primaryActionLabel, Palette.mintDark, Palette.mint) {
                        handlePlaygroundPrimaryAction(entry, installer, model.localPhase)
                    }.apply {
                        isEnabled = model.primaryActionEnabled
                        alpha = if (isEnabled) 1f else 0.55f
                        contentDescription = "${model.title}，${model.primaryActionLabel}"
                    },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)),
                )
                if (model.canUninstall) {
                    addView(space(7))
                    addView(
                        chipButton("Desinstalar modelo", false) {
                            confirmPlaygroundUninstall(entry, installer)
                        },
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)),
                    )
                }
                addView(space(7))
            }
            addView(
                chipButton(playgroundSourceActionLabel(entry), false) { openPlaygroundSource(entry) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)),
            )
            contentDescription = listOf(
                model.title,
                PlaygroundPresenter.originAccessibilityLabel(entry.origin),
                model.validationLabel,
                model.distributionLabel,
                model.localStatusLabel,
            ).joinToString("，")
        }
    }

    private fun playgroundOriginAccent(origin: PlaygroundArtifactOrigin): Int = when (origin) {
        PlaygroundArtifactOrigin.HARZVA -> Palette.mintDark
        PlaygroundArtifactOrigin.UPSTREAM -> Palette.blue
        PlaygroundArtifactOrigin.THIRD_PARTY -> Palette.lavender
        PlaygroundArtifactOrigin.RECIPE -> Palette.muted
    }

    private fun playgroundInstaller(entry: PlaygroundCatalogEntry): PlaygroundArtifactInstaller? {
        val installer = PlaygroundInstallerRegistry.getOrCreate(applicationContext, entry) ?: return null
        if (installer.beginStartupVerification()) verifyPlaygroundInstallOnStartup(installer)
        return installer
    }

    private fun verifyPlaygroundInstallOnStartup(installer: PlaygroundArtifactInstaller) {
        val handle = installer.verifyInstalled { snapshot ->
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    if (snapshot.phase == PlaygroundInstallPhase.INSTALLED) {
                        updateStatus("Revisão SHA-256 do modelo local aprovada")
                    } else if (snapshot.phase in setOf(
                            PlaygroundInstallPhase.VERIFICATION_FAILED,
                            PlaygroundInstallPhase.SOURCE_MISMATCH,
                            PlaygroundInstallPhase.FAILED,
                        )
                    ) {
                        updateStatus("Falha na revisão de inicialização do modelo local")
                    }
                    if (currentTab == AppTab.PLAYGROUND) renderCurrentTab()
                }
            }
        }
        if (!handle.started) installer.releaseStartupVerificationClaim()
    }

    private fun playgroundInstallerForModelPath(modelPath: String): PlaygroundArtifactInstaller? {
        return PlaygroundInstallerRegistry.forManagedModelPath(modelPath)
    }

    private fun handlePlaygroundPrimaryAction(
        entry: PlaygroundCatalogEntry,
        installer: PlaygroundArtifactInstaller,
        phase: PlaygroundLocalPhase,
    ) {
        when (phase) {
            PlaygroundLocalPhase.NOT_DOWNLOADED,
            PlaygroundLocalPhase.DOWNLOAD_FAILED,
            PlaygroundLocalPhase.INSUFFICIENT_STORAGE,
            -> confirmPlaygroundInstall(entry, installer)
            PlaygroundLocalPhase.CANCELLED -> {
                if (installer.snapshot().installedArtifactNames.isNotEmpty()) {
                    startPlaygroundVerification(installer)
                } else {
                    confirmPlaygroundInstall(entry, installer)
                }
            }
            PlaygroundLocalPhase.VERIFICATION_FAILED -> {
                if (installer.snapshot().installedArtifactNames.isNotEmpty()) {
                    confirmPlaygroundUninstall(entry, installer)
                } else {
                    confirmPlaygroundInstall(entry, installer)
                }
            }
            PlaygroundLocalPhase.DOWNLOADING,
            PlaygroundLocalPhase.VERIFYING,
            -> {
                installer.cancel()
                updateStatus("Cancelando operação do modelo confiável")
            }
            PlaygroundLocalPhase.INSTALLED,
            PlaygroundLocalPhase.LOAD_FAILED,
            -> loadVerifiedPlaygroundModel(installer)
            PlaygroundLocalPhase.SOURCE_MISMATCH -> confirmPlaygroundUninstall(entry, installer)
            PlaygroundLocalPhase.ARTIFACT_MISSING -> {
                if (installer.snapshot().installedArtifactNames.isNotEmpty()) {
                    confirmPlaygroundUninstall(entry, installer)
                } else {
                    confirmPlaygroundInstall(entry, installer)
                }
            }
            PlaygroundLocalPhase.ATOMIC_INSTALL_FAILED -> startPlaygroundInstall(installer)
            PlaygroundLocalPhase.UNINSTALL_FAILED -> uninstallPlaygroundModel(installer)
            PlaygroundLocalPhase.VERIFICATION_IO_FAILED -> startPlaygroundVerification(installer)
            else -> Unit
        }
    }

    private fun confirmPlaygroundInstall(
        entry: PlaygroundCatalogEntry,
        installer: PlaygroundArtifactInstaller,
    ) {
        val size = formatBytes(installer.spec.totalBytes)
        AlertDialog.Builder(this)
            .setTitle("Baixar e verificar ${entry.displayName}")
            .setMessage(
                "Baixará $size da revisão fixa do Hugging Face para o diretório privado do MobileCore." +
                    "O espaço será verificado antes da instalação, e bytes e SHA-256 serão rigorosamente verificados; fonte: ${entry.source.conversionPublisher}," +
                    "Licença: ${entry.source.license}.",
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Baixar e verificar") { _, _ -> startPlaygroundInstall(installer) }
            .show()
    }

    private fun startPlaygroundInstall(installer: PlaygroundArtifactInstaller) {
        val handle = installer.install { snapshot ->
            runOnUiThread {
                when (snapshot.phase) {
                    PlaygroundInstallPhase.INSTALLED -> {
                        updateStatus("Modelo aprovado na verificação SHA-256")
                        Toast.makeText(this, "Modelo instalado e verificado, opção de carregar disponível", Toast.LENGTH_SHORT).show()
                    }
                    PlaygroundInstallPhase.VERIFICATION_FAILED -> updateStatus("Falha na verificação do modelo")
                    PlaygroundInstallPhase.SOURCE_MISMATCH -> updateStatus("Fonte do modelo incompatível")
                    PlaygroundInstallPhase.CANCELLED -> updateStatus("Download do modelo cancelado")
                    PlaygroundInstallPhase.FAILED -> updateStatus("Download do modelo incompleto")
                    else -> Unit
                }
                if (currentTab == AppTab.PLAYGROUND) renderCurrentTab()
            }
        }
        if (!handle.started) {
            Toast.makeText(this, "Já existe uma tarefa de instalação de modelo confiável em execução", Toast.LENGTH_SHORT).show()
        } else {
            updateStatus("Verificando espaço de instalação do modelo")
            if (currentTab == AppTab.PLAYGROUND) renderCurrentTab()
        }
    }

    private fun loadVerifiedPlaygroundModel(installer: PlaygroundArtifactInstaller) {
        val modelFile = installer.primaryModelFile()
        if (modelFile == null) {
            startPlaygroundVerification(installer)
            updateStatus("Status de confiança do modelo expirado, reverificando em segundo plano")
            return
        }
        withNotificationPermission {
            installer.markLoading()
            startServiceWithModel(modelFile)
        }
    }

    private fun startPlaygroundVerification(installer: PlaygroundArtifactInstaller) {
        val handle = installer.verifyInstalled { snapshot ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                when (snapshot.phase) {
                    PlaygroundInstallPhase.INSTALLED -> updateStatus("Verificação SHA-256 do modelo aprovada, clique novamente para carregar")
                    PlaygroundInstallPhase.CANCELLED -> updateStatus("Verificação do modelo cancelada")
                    PlaygroundInstallPhase.VERIFICATION_FAILED -> updateStatus("Verificação do modelo não aprovada")
                    PlaygroundInstallPhase.FAILED -> updateStatus("Verificação do modelo não concluída")
                    else -> Unit
                }
                if (currentTab == AppTab.PLAYGROUND) renderCurrentTab()
            }
        }
        if (!handle.started) {
            Toast.makeText(this, "Este modelo já possui tarefa de instalação ou verificação", Toast.LENGTH_SHORT).show()
        } else {
            updateStatus("Verificando SHA-256 do modelo em segundo plano")
            if (currentTab == AppTab.PLAYGROUND) renderCurrentTab()
        }
    }

    private fun confirmPlaygroundUninstall(
        entry: PlaygroundCatalogEntry,
        installer: PlaygroundArtifactInstaller,
    ) {
        if (pendingModelPath?.let(installer::managesModelPath) == true) {
            Toast.makeText(this, "Modelo ainda carregando, aguarde a conclusão para desinstalar", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(if (installer.snapshot().phase == PlaygroundInstallPhase.SOURCE_MISMATCH) "Remover arquivos incompatíveis" else "Desinstalar modelo")
            .setMessage("Serão removidos apenas ${entry.displayName}, arquivos temporários e registros de verificação do diretório privado de modelos do MobileCore.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar remoção") { _, _ -> uninstallPlaygroundModel(installer) }
            .show()
    }

    private fun uninstallPlaygroundModel(installer: PlaygroundArtifactInstaller) {
        updateStatus("Confirmando modelo em execução")
        refreshRuntimeModelStateForDestructiveAction { refreshed ->
            if (!refreshed) {
                updateStatus("Não foi possível confirmar o status do runtime, arquivos do modelo mantidos")
                Toast.makeText(this, "Status do serviço local não confirmado, nenhum arquivo removido", Toast.LENGTH_LONG).show()
                return@refreshRuntimeModelStateForDestructiveAction
            }
            uninstallPlaygroundModelAfterRefresh(installer)
        }
    }

    private fun uninstallPlaygroundModelAfterRefresh(installer: PlaygroundArtifactInstaller) {
        if (pendingModelPath?.let(installer::managesModelPath) == true) {
            Toast.makeText(this, "Modelo ainda carregando, arquivos não serão removidos", Toast.LENGTH_LONG).show()
            return
        }
        if (runtimeReportsLoadedModel && activeModelPath == null) {
            Toast.makeText(this, "Não foi possível confirmar exclusivamente o modelo em execução, arquivos mantidos", Toast.LENGTH_LONG).show()
            refreshRuntimeModelState()
            return
        }
        callLocalApi(
            path = "/mobilecore/playground/uninstall",
            method = "POST",
            body = JSONObject().put("model_id", installer.spec.id).toString(),
            retryCount = 1,
            readTimeoutMs = 45_000,
            onResult = { status, body, _ ->
                val result = runCatching { JSONObject(body) }.getOrNull()
                val removed = status in 200..299 && result?.optBoolean("ok", false) == true
                if (removed) {
                    if (result?.optBoolean("model_loaded", false) != true) {
                        runtimeReportsLoadedModel = false
                        activeModelPath = null
                        pendingModelPath = null
                        reconcilePlaygroundRuntimeTruth(null)
                    }
                    updateStatus("Modelo desinstalado")
                    Toast.makeText(this, "Modelo e registros de verificação removidos", Toast.LENGTH_SHORT).show()
                    refreshRecommendationSnapshot()
                    syncBenchmarkReadiness()
                    refreshRuntimeModelState()
                    if (currentTab in setOf(AppTab.MODELS, AppTab.PLAYGROUND)) renderCurrentTab()
                } else {
                    updateStatus("Falha ao desinstalar modelo, arquivos mantidos")
                    Toast.makeText(this, "Status do runtime ou arquivos não confirmado com segurança, desinstalação não concluída", Toast.LENGTH_LONG).show()
                    refreshRuntimeModelState()
                }
            },
            onError = {
                updateStatus("Serviço local inacessível, arquivos do modelo mantidos")
                Toast.makeText(this, "Não foi possível entrar na exclusão mútua do runtime, nenhum arquivo removido", Toast.LENGTH_LONG).show()
            },
        )
    }

    private fun playgroundSourceActionLabel(entry: PlaygroundCatalogEntry): String = when {
        entry.distribution.mode == "huggingface_model_repo" -> "Ver fonte no Hugging Face"
        entry.distribution.repositoryUrl == null -> "Ver fonte fixa"
        else -> "Ver repositório de publicação"
    }

    private fun openPlaygroundSource(entry: PlaygroundCatalogEntry) {
        openPlaygroundUrl(entry.distribution.repositoryUrl ?: entry.source.repository, "Não foi possível abrir a fonte do modelo")
    }

    private fun openPlaygroundUrl(url: String, failureMessage: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(this, failureMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildFeaturedModelScopeCard(): View {
        val profile = probeDeviceProfile()
        val featuredItems = featuredModelScopeCatalog()
            .sortedWith(
                compareByDescending<ModelScopeCatalogEntry> { mobileFitScore(it, profile) }
                    .thenBy { modelParameterValue(it.parameterLabel) }
                    .thenBy { estimateMobileMemoryMb(it) }
            )
            .take(6)
        val best = featuredItems.firstOrNull()
        val headerText = best?.let {
            "${fitLabel(it, profile)} · Recomendado ${it.parameterLabel} ${it.quantization}"
        } ?: "Preparando recomendação"

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                label(
                    "${profile.coreCount} núcleos · Memória disponível ${profile.availableRamMb}MB · $headerText",
                    11.8f,
                    Palette.mintDark,
                    Typeface.BOLD,
                ).apply {
                    setPadding(dp(2), 0, dp(2), 0)
                    maxLines = 2
                }
            )
            addView(space(10))
            featuredItems.forEachIndexed { index, entry ->
                addView(buildModelScopeResultRow(entry, compact = index >= 2))
                if (index != featuredItems.lastIndex) {
                    addView(space(8))
                }
            }
        }
    }

    private fun buildModelScopeCatalogCard(): View {
        return surfaceCard(Palette.blue) {
            addView(
                LinearLayout(context).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(cardHeader("ModelScope GGUF", "Pesquisar, filtrar e baixar modelos executáveis", "cloud", Palette.blue), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(
                        chipButton(if (modelScopeLoading) "Carregando" else "Pesquisar", false) {
                            refreshModelScopeCatalog(force = true)
                        },
                        LinearLayout.LayoutParams(dp(88), dp(40))
                    )
                }
            )
            addView(space(10))
            addView(
                EditText(context).apply {
                    setText(modelScopeSearchQuery)
                    hint = "Pesquisar Qwen, Q4_K_M, 0.5B..."
                    textSize = 14f
                    setSingleLine(true)
                    setTextColor(Palette.ink)
                    setHintTextColor(Palette.muted)
                    background = rounded(tint(Palette.blueWash, 0.70f), Palette.stroke, 16f)
                    setPadding(dp(14), 0, dp(14), 0)
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            val nextQuery = s?.toString().orEmpty()
                            if (nextQuery == modelScopeSearchQuery) return
                            modelScopeSearchQuery = nextQuery
                            renderModelScopeResults()
                            scheduleModelScopeSearch()
                        }

                        override fun afterTextChanged(s: Editable?) = Unit
                    })
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52))
            )
            addView(space(8))
            modelScopeStatusText = label("", 12f, Palette.muted, Typeface.NORMAL)
            addView(modelScopeStatusText)
            addView(space(8))
            modelScopeResultsContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            addView(modelScopeResultsContainer)
            renderModelScopeResults()
            if (!modelScopeLoaded && !modelScopeLoading) {
                refreshModelScopeCatalog(force = false)
            }
        }
    }

    private fun scheduleModelScopeSearch() {
        progressHandler.removeCallbacks(modelScopeSearchRunnable)
        progressHandler.postDelayed(modelScopeSearchRunnable, 650L)
    }

    private fun renderModelScopeResults() {
        val container = modelScopeResultsContainer ?: return
        container.removeAllViews()
        val query = modelScopeSearchQuery.trim().lowercase(Locale.US)
        val visibleItems = modelScopeCatalog
            .filter { entry -> query.isBlank() || entry.searchText.contains(query) }
            .sortedWith(compareBy<ModelScopeCatalogEntry> { it.sizeBytes }.thenBy { it.fileName })
            .take(8)

        modelScopeStatusText?.text = when {
            modelScopeLoading -> "Buscando detalhes do repositório e lista de arquivos GGUF no ModelScope..."
            visibleItems.isNotEmpty() -> {
                val repoText = modelScopeRemoteTotal?.let { " · $it repositórios encontrados" } ?: ""
                "${modelScopeCatalog.size} arquivos GGUF expandidos · Exibindo ${visibleItems.size} itens$repoText"
            }
            modelScopeError != null -> "ModelScope temporariamente indisponível: $modelScopeError"
            modelScopeLoaded -> "Nenhum GGUF correspondente, tente qwen / q4 / 0.5b"
            else -> "Preparando lista de modelos do ModelScope"
        }

        if (visibleItems.isEmpty()) {
            container.addView(
                label(
                    if (modelScopeLoading) "Aguarde, conectando ao ModelScope." else "Nenhum resultado; clique em pesquisar para tentar novamente.",
                    13f,
                    Palette.muted,
                    Typeface.NORMAL
                ).apply {
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    background = rounded(tint(Palette.mint, 0.08f), Palette.stroke, 14f)
                },
            )
            return
        }

        visibleItems.forEachIndexed { index, entry ->
            container.addView(buildModelScopeResultRow(entry))
            if (index != visibleItems.lastIndex) {
                container.addView(space(8))
            }
        }
    }

    private fun buildModelScopeResultRow(entry: ModelScopeCatalogEntry, compact: Boolean = false): View {
        val localFile = File(externalModelDir(), entry.fileName)
        val downloaded = localFile.exists() && localFile.length() > 1024 * 1024
        val modelHubItem = modelScopeItem(entry)
        val taskKey = downloadTaskKey(modelHubItem)
        val downloadState = providerStateByProvider[taskKey]
        val lifecycle = modelLifecycle(
            file = localFile.takeIf { downloaded },
            expectedFileName = entry.fileName,
            downloadState = downloadState,
        )
        val loadedInRuntime = lifecycle.phase == ModelLifecyclePhase.LOADED
        val profile = probeDeviceProfile()
        val estimatedMemoryMb = estimateMobileMemoryMb(entry)
        val fit = fitLabel(entry, profile)
        val reason = entry.recommendationReason.ifBlank {
            "$fit · Memória estimada ${estimatedMemoryMb}MB · ${recommendationReasonFor(entry, profile)}"
        }
        val accent = modelLifecycleAccent(lifecycle.tone)
        val actionAccent = when (lifecycle.phase) {
            ModelLifecyclePhase.NOT_DOWNLOADED,
            ModelLifecyclePhase.DOWNLOADING,
            ModelLifecyclePhase.PAUSED -> Palette.mintDark
            ModelLifecyclePhase.DOWNLOADED,
            ModelLifecyclePhase.LOADING -> Palette.blue
            ModelLifecyclePhase.LOADED -> Palette.mint
            ModelLifecyclePhase.DOWNLOAD_FAILED,
            ModelLifecyclePhase.LOAD_FAILED -> Palette.danger
        }
        val badge = lifecycle.statusLabel
        val primaryAction = lifecycle.actionLabel
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ripple(
                rounded(
                    mixColor(Palette.surface, accent, if (loadedInRuntime) 0.11f else 0.035f),
                    if (loadedInRuntime) tint(accent, 0.42f) else tint(Palette.stroke, 0.50f),
                    TuiMaTheme.cardRadiusDp,
                ),
                accent,
            )
            setPadding(dp(12), dp(11), dp(12), dp(11))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                Toast.makeText(this@MainActivity, "${entry.repoId}\n${entry.fileName}\n$reason", Toast.LENGTH_LONG).show()
            }
            addView(
                LinearLayout(context).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        IconBadgeView(context, "cube", accent),
                        LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(10) },
                    )
                    addView(
                        LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            addView(label(entry.displayTitle, 14f, Palette.ink, Typeface.BOLD).apply { maxLines = 2 })
                            addView(space(3))
                            addView(
                                label(
                                    "${entry.parameterLabel} · ${entry.quantization} · ${formatBytes(entry.sizeBytes)} · $fit",
                                    11.2f,
                                    Palette.muted,
                                    Typeface.NORMAL,
                                ).apply { maxLines = 2 }
                            )
                        },
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                    )
                    addView(
                        chip(label(badge, 10.5f, accent, Typeface.BOLD), tint(accent, 0.10f), accent),
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30)).apply { marginStart = dp(8) },
                    )
                }
            )
            if (!compact) {
                addView(space(8))
                addView(
                    label(
                        reason,
                        11.5f,
                        Palette.muted,
                        Typeface.NORMAL,
                    ).apply { maxLines = 2 }
                )
            }
            addView(space(9))
            addView(
                LinearLayout(context).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        label(lifecycle.supportingText, 11.4f, accent, Typeface.BOLD).apply { maxLines = 2 },
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(10) },
                    )
                    addView(
                        compactActionButton(primaryAction, actionAccent, lifecycle.actionEnabled) {
                            val currentFile = File(externalModelDir(), entry.fileName)
                            val currentState = providerStateByProvider[taskKey]
                            val currentDownloaded = currentFile.exists() && currentFile.length() > 1024 * 1024
                            val currentLifecycle = modelLifecycle(
                                file = currentFile.takeIf { currentDownloaded },
                                expectedFileName = entry.fileName,
                                downloadState = currentState,
                            )
                            when (currentLifecycle.phase) {
                                ModelLifecyclePhase.LOADED, ModelLifecyclePhase.LOADING -> Unit
                                ModelLifecyclePhase.DOWNLOADED, ModelLifecyclePhase.LOAD_FAILED -> ensureNotificationPermissionAndLoadModel(currentFile)
                                ModelLifecyclePhase.DOWNLOADING -> pauseModelDownload(taskKey)
                                else -> enqueueModelDownload(modelHubItem)
                            }
                        },
                        LinearLayout.LayoutParams(dp(92), dp(44)),
                    )
                }
            )
            contentDescription = "${entry.displayTitle}, ${lifecycle.statusLabel}, $fit. Clique para ver detalhes"
        }
    }

    private fun featuredModelScopeCatalog(): List<ModelScopeCatalogEntry> {
        fun mb(value: Long) = value * 1024L * 1024L
        return listOf(
            ModelScopeCatalogEntry(
                repoId = "unsloth/gemma-3-270m-it-GGUF",
                displayTitle = "Gemma3 270M Instruct",
                fileName = "gemma-3-270m-it-Q4_K_M.gguf",
                filePath = "gemma-3-270m-it-Q4_K_M.gguf",
                sizeBytes = 253115424L,
                quantization = "Q4_K_M",
                parameterLabel = "270M",
                architecture = "gemma3",
                downloads = 0L,
                recommendationReason = "Entrada de texto ultraleve Gemma3, ideal para verificar primeiro carregamento e cadeia de diálogo.",
                tier = "tiny"
            ),
            ModelScopeCatalogEntry(
                repoId = "unsloth/gemma-3-1b-it-GGUF",
                displayTitle = "Gemma3 1B Instruct",
                fileName = "gemma-3-1b-it-Q4_K_M.gguf",
                filePath = "gemma-3-1b-it-Q4_K_M.gguf",
                sizeBytes = 806058272L,
                quantization = "Q4_K_M",
                parameterLabel = "1B",
                architecture = "gemma3",
                downloads = 0L,
                recommendationReason = "Linha de base de qualidade para celular Gemma3, Q4 mais estável.",
                tier = "phone"
            ),
            ModelScopeCatalogEntry(
                repoId = "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
                displayTitle = "Qwen2.5 0.5B Instruct",
                fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
                filePath = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
                sizeBytes = mb(469),
                quantization = "Q4_K_M",
                parameterLabel = "0.5B",
                architecture = "qwen2",
                downloads = 0L,
                recommendationReason = "Opção de entrada, tamanho pequeno, ideal para primeira verificação.",
                tier = "tiny"
            ),
            ModelScopeCatalogEntry(
                repoId = "unsloth/Qwen3-0.6B-GGUF",
                displayTitle = "Qwen3 0.6B Ultra Small",
                fileName = "Qwen3-0.6B-UD-IQ1_S.gguf",
                filePath = "Qwen3-0.6B-UD-IQ1_S.gguf",
                sizeBytes = mb(205),
                quantization = "UD-IQ1_S",
                parameterLabel = "0.6B",
                architecture = "qwen3",
                downloads = 0L,
                recommendationReason = "Pacote de download mínimo, teste primeiro em celulares com pouca memória.",
                tier = "tiny"
            ),
            ModelScopeCatalogEntry(
                repoId = "unsloth/Qwen3-0.6B-GGUF",
                displayTitle = "Qwen3 0.6B Balanced",
                fileName = "Qwen3-0.6B-Q4_K_M.gguf",
                filePath = "Qwen3-0.6B-Q4_K_M.gguf",
                sizeBytes = mb(468),
                quantization = "Q4_K_M",
                parameterLabel = "0.6B",
                architecture = "qwen3",
                downloads = 0L,
                recommendationReason = "Modelo pequeno mas quantização mais estável, ideal para testes diários.",
                tier = "tiny"
            ),
            ModelScopeCatalogEntry(
                repoId = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
                displayTitle = "Qwen2.5 1.5B Instruct",
                fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
                filePath = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
                sizeBytes = mb(1066),
                quantization = "Q4_K_M",
                parameterLabel = "1.5B",
                architecture = "qwen2",
                downloads = 0L,
                recommendationReason = "Dispositivos modestos podem usar, qualidade de resposta significativamente melhor que 0.5B.",
                tier = "phone"
            ),
            ModelScopeCatalogEntry(
                repoId = "unsloth/Qwen3-1.7B-GGUF",
                displayTitle = "Qwen3 1.7B Compact",
                fileName = "Qwen3-1.7B-Q2_K.gguf",
                filePath = "Qwen3-1.7B-Q2_K.gguf",
                sizeBytes = mb(742),
                quantization = "Q2_K",
                parameterLabel = "1.7B",
                architecture = "qwen3",
                downloads = 0L,
                recommendationReason = "Parâmetros maiores mas arquivo ainda pequeno, ideal para priorizar velocidade.",
                tier = "phone"
            ),
            ModelScopeCatalogEntry(
                repoId = "unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF",
                displayTitle = "DeepSeek R1 Qwen 1.5B",
                fileName = "DeepSeek-R1-Distill-Qwen-1.5B-Q2_K.gguf",
                filePath = "DeepSeek-R1-Distill-Qwen-1.5B-Q2_K.gguf",
                sizeBytes = mb(718),
                quantization = "Q2_K",
                parameterLabel = "1.5B",
                architecture = "qwen2",
                downloads = 0L,
                recommendationReason = "Experiência de reasoning leve, ideal para demonstrar cadeia de raciocínio.",
                tier = "phone"
            ),
            ModelScopeCatalogEntry(
                repoId = "AI-ModelScope/Phi-3.1-mini-4k-instruct-GGUF",
                displayTitle = "Phi-3.1 Mini 4K Instruct",
                fileName = "Phi-3.1-mini-4k-instruct-IQ2_M.gguf",
                filePath = "Phi-3.1-mini-4k-instruct-IQ2_M.gguf",
                sizeBytes = mb(1255),
                quantization = "IQ2_M",
                parameterLabel = "3.8B",
                architecture = "phi3",
                downloads = 0L,
                recommendationReason = "Dispositivos intermediários podem tentar, equilíbrio entre qualidade e tamanho.",
                tier = "phone"
            ),
            ModelScopeCatalogEntry(
                repoId = "unsloth/Qwen3-4B-GGUF",
                displayTitle = "Qwen3 4B Compact",
                fileName = "Qwen3-4B-Q2_K.gguf",
                filePath = "Qwen3-4B-Q2_K.gguf",
                sizeBytes = mb(1592),
                quantization = "Q2_K",
                parameterLabel = "4B",
                architecture = "qwen3",
                downloads = 0L,
                recommendationReason = "Ponto ideal para dispositivos com muita memória, qualidade mais próxima de um assistente útil.",
                tier = "tablet"
            ),
            ModelScopeCatalogEntry(
                repoId = "unsloth/DeepSeek-R1-Distill-Qwen-7B-GGUF",
                displayTitle = "DeepSeek R1 Qwen 7B",
                fileName = "DeepSeek-R1-Distill-Qwen-7B-Q2_K.gguf",
                filePath = "DeepSeek-R1-Distill-Qwen-7B-Q2_K.gguf",
                sizeBytes = mb(2876),
                quantization = "Q2_K",
                parameterLabel = "7B",
                architecture = "qwen2",
                downloads = 0L,
                recommendationReason = "Modelo grande para dispositivos flagship, ideal para demonstrar reasoning.",
                tier = "heavy"
            ),
            ModelScopeCatalogEntry(
                repoId = "unsloth/Qwen3-8B-GGUF",
                displayTitle = "Qwen3 8B Ultra Compact",
                fileName = "Qwen3-8B-UD-IQ1_S.gguf",
                filePath = "Qwen3-8B-UD-IQ1_S.gguf",
                sizeBytes = mb(2170),
                quantization = "UD-IQ1_S",
                parameterLabel = "8B",
                architecture = "qwen3",
                downloads = 0L,
                recommendationReason = "Experimente grande quantidade de parâmetros em flagship, priorize a memória disponível.",
                tier = "heavy"
            )
        )
    }

    private fun refreshModelScopeCatalog(force: Boolean) {
        if (modelScopeLoading) return
        val requestedQuery = modelScopeSearchQuery.trim()
        if (modelScopeLoaded && !force && modelScopeLoadedQuery == requestedQuery) {
            renderModelScopeResults()
            return
        }
        modelScopeLoading = true
        modelScopeError = null
        renderModelScopeResults()
        Thread {
            try {
                val remoteTotal: Int?
                val repos = if (requestedQuery.isBlank()) {
                    remoteTotal = null
                    modelScopeSeeds
                } else {
                    val searchResult = searchModelScopeRepos(requestedQuery)
                    remoteTotal = searchResult.totalCount
                    searchResult.repos
                }
                val loaded = repos
                    .distinctBy { it.repoId.lowercase(Locale.US) }
                    .take(8)
                    .flatMap { seed ->
                        runCatching { fetchModelScopeRepo(seed) }.getOrDefault(emptyList())
                    }
                runOnUiThread {
                    if (modelScopeSearchQuery.trim() != requestedQuery) {
                        modelScopeLoading = false
                        scheduleModelScopeSearch()
                        return@runOnUiThread
                    }
                    modelScopeCatalog.clear()
                    modelScopeCatalog.addAll(
                        if (loaded.isEmpty() && requestedQuery.isBlank()) fallbackModelScopeCatalog() else loaded
                    )
                    modelScopeLoaded = true
                    modelScopeLoadedQuery = requestedQuery
                    modelScopeRemoteTotal = remoteTotal
                    modelScopeLoading = false
                    modelScopeError = null
                    renderModelScopeResults()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    modelScopeCatalog.clear()
                    modelScopeCatalog.addAll(fallbackModelScopeCatalog())
                    modelScopeLoaded = true
                    modelScopeLoadedQuery = requestedQuery
                    modelScopeRemoteTotal = null
                    modelScopeLoading = false
                    modelScopeError = readableDownloadError(e)
                    renderModelScopeResults()
                }
            }
        }.start()
    }

    private fun searchModelScopeRepos(query: String): ModelScopeSearchResult {
        val searchQuery = if (query.contains("gguf", ignoreCase = true)) query else "$query gguf"
        val requestBody = JSONObject().apply {
            put("PageSize", 12)
            put("PageNumber", 1)
            put("SortBy", "Default")
            put("Target", "")
            put(
                "Criterion",
                JSONArray().apply {
                    put(JSONObject().apply {
                        put("category", "libraries")
                        put("predicate", "contains")
                        put("values", JSONArray().apply { put("gguf") })
                    })
                }
            )
            put("SingleCriterion", JSONArray())
            put("Name", searchQuery)
        }
        val json = requestModelScopeJson(
            url = "https://modelscope.cn/api/v1/dolphin/model/suggestv2",
            method = "POST",
            body = requestBody
        )
        val modelData = json.optJSONObject("Data")?.optJSONObject("Model") ?: JSONObject()
        val suggestions = modelData.optJSONArray("Suggests") ?: JSONArray()
        val repos = mutableListOf<ModelScopeRepoSeed>()
        for (index in 0 until suggestions.length()) {
            val item = suggestions.optJSONObject(index) ?: continue
            val owner = item.optString("Path")
            val name = item.optString("Name")
            if (owner.isBlank() || name.isBlank()) continue
            repos.add(
                ModelScopeRepoSeed(
                    owner = owner,
                    name = name,
                    label = item.optString("ChineseName").ifBlank { name }
                )
            )
        }
        return ModelScopeSearchResult(
            repos = repos,
            totalCount = modelData.optInt("TotalCount", repos.size)
        )
    }

    private fun fetchModelScopeRepo(seed: ModelScopeRepoSeed): List<ModelScopeCatalogEntry> {
        val repoId = seed.repoId
        val detailData = requestModelScopeJson("https://modelscope.cn/api/v1/models/$repoId")
            .optJSONObject("Data")
            ?: JSONObject()
        val fileData = requestModelScopeJson("https://modelscope.cn/api/v1/models/$repoId/repo/files?Revision=master&Recursive=true")
            .optJSONObject("Data")
            ?: JSONObject()
        val files = fileData.optJSONArray("Files") ?: JSONArray()
        val displayName = detailData.optString("ChineseName").ifBlank {
            detailData.optString("Name").ifBlank { seed.label }
        }
        val downloads = detailData.optLong("Downloads", 0L)
        val modelInfo = detailData.optJSONObject("ModelInfos")?.optJSONObject("gguf")
        val architecture = modelInfo?.optString("architecture")?.ifBlank { null } ?: inferArchitecture(repoId)
        val entries = mutableListOf<ModelScopeCatalogEntry>()
        for (index in 0 until files.length()) {
            val file = files.optJSONObject(index) ?: continue
            val name = file.optString("Name")
            if (!name.endsWith(".gguf", ignoreCase = true)) continue
            val size = file.optLong("Size", 0L)
            if (size <= 1024 * 1024) continue
            entries.add(
                ModelScopeCatalogEntry(
                    repoId = repoId,
                    displayTitle = displayName,
                    fileName = name,
                    filePath = file.optString("Path").ifBlank { name },
                    sizeBytes = size,
                    quantization = extractQuantization(name),
                    parameterLabel = inferParameterLabel("$repoId $name"),
                    architecture = architecture,
                    downloads = downloads
                )
            )
        }
        return entries
    }

    private fun requestModelScopeJson(url: String, method: String = "GET", body: JSONObject? = null): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8000
            readTimeout = 12000
            setRequestProperty("User-Agent", "TuiMa-MobileCore/0.1.1 Android")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            if (body != null) {
                connection.outputStream.use { output ->
                    output.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }
            val status = connection.responseCode
            val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.readText()
                ?: "{}"
            if (status !in 200..299) throw IOException("ModelScope HTTP $status")
            JSONObject(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun fallbackModelScopeCatalog(): List<ModelScopeCatalogEntry> {
        return listOf(
            ModelScopeCatalogEntry(
                repoId = "unsloth/gemma-3-270m-it-GGUF",
                displayTitle = "Gemma3-270M-Instruct-GGUF",
                fileName = "gemma-3-270m-it-Q4_K_M.gguf",
                filePath = "gemma-3-270m-it-Q4_K_M.gguf",
                sizeBytes = 253115424L,
                quantization = "Q4_K_M",
                parameterLabel = "270M",
                architecture = "gemma3",
                downloads = 0L,
                recommendationReason = "Entrada de texto ultraleve Gemma3, ideal para verificar primeiro carregamento e cadeia de diálogo.",
                tier = "tiny"
            ),
            ModelScopeCatalogEntry(
                repoId = "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
                displayTitle = "Qwen2.5-0.5B-Instruct-GGUF",
                fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
                filePath = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
                sizeBytes = 491400032L,
                quantization = "Q4_K_M",
                parameterLabel = "0.5B",
                architecture = "qwen2",
                downloads = 0L
            ),
            ModelScopeCatalogEntry(
                repoId = "unsloth/Qwen3-0.6B-GGUF",
                displayTitle = "Qwen3-0.6B-GGUF",
                fileName = "Qwen3-0.6B-Q4_K_M.gguf",
                filePath = "Qwen3-0.6B-Q4_K_M.gguf",
                sizeBytes = 396705472L,
                quantization = "Q4_K_M",
                parameterLabel = "0.6B",
                architecture = "qwen3",
                downloads = 0L
            )
        )
    }

    private fun modelScopeDownloadUrl(entry: ModelScopeCatalogEntry): String {
        return "https://modelscope.cn/models/${entry.repoId}/resolve/master/${entry.filePath.replace(" ", "%20")}"
    }

    private fun modelScopeItem(entry: ModelScopeCatalogEntry): ModelHubItem {
        return ModelHubItem(
            provider = "ModelScope",
            shortName = "${entry.parameterLabel} ${entry.quantization}",
            fileName = entry.fileName,
            url = modelScopeDownloadUrl(entry)
        )
    }

    private fun downloadTaskKey(item: ModelHubItem): String {
        return "${item.provider}:${item.fileName}".lowercase(Locale.US)
    }

    private fun extractQuantization(fileName: String): String {
        val patterns = listOf(
            "UD-IQ\\d(?:_[A-Z]+)?",
            "IQ\\d(?:_[A-Z]+)?",
            "Q\\d(?:_[A-Z0-9]+)?",
            "BF16",
            "FP16",
            "F16"
        )
        val upper = fileName.uppercase(Locale.US)
        return patterns.firstNotNullOfOrNull { pattern ->
            Regex(pattern).find(upper)?.value
        } ?: "GGUF"
    }

    private fun inferParameterLabel(text: String): String {
        val normalized = text.replace("-", " ")
        Regex("(\\d+(?:\\.\\d+)?)\\s*[Bb]").find(normalized)?.let {
            return "${it.groupValues[1]}B"
        }
        Regex("(\\d+)\\s*[Mm]").find(normalized)?.let {
            return "${it.groupValues[1]}M"
        }
        return "LLM"
    }

    private fun inferArchitecture(repoId: String): String {
        return when {
            repoId.contains("gemma", ignoreCase = true) -> "gemma3"
            repoId.contains("qwen3", ignoreCase = true) -> "qwen3"
            repoId.contains("qwen", ignoreCase = true) -> "qwen2"
            repoId.contains("llama", ignoreCase = true) -> "llama"
            else -> "gguf"
        }
    }

    private fun estimateMobileMemoryMb(entry: ModelScopeCatalogEntry): Long {
        val sizeMb = (entry.sizeBytes / (1024 * 1024)).coerceAtLeast(1L)
        val quant = entry.quantization.lowercase(Locale.US)
        val multiplier = when {
            quant.contains("iq1") || quant.contains("ud-iq1") -> 0.70
            quant.contains("iq2") || quant.contains("q2") -> 0.78
            quant.contains("q3") -> 0.90
            quant.contains("q4") -> 1.05
            quant.contains("q5") -> 1.18
            quant.contains("q6") -> 1.30
            quant.contains("q8") || quant.contains("f16") || quant.contains("bf16") -> 1.55
            else -> 1.10
        }
        val params = modelParameterValue(entry.parameterLabel)
        val cacheOverhead = when {
            params >= 7.0 -> 640L
            params >= 4.0 -> 512L
            params >= 1.5 -> 384L
            else -> 256L
        }
        return (sizeMb * multiplier).toLong() + cacheOverhead
    }

    private fun mobileFitScore(entry: ModelScopeCatalogEntry, profile: DeviceProbeSnapshot): Int {
        val estimated = estimateMobileMemoryMb(entry)
        val available = profile.availableRamMb.coerceAtLeast(512L)
        val memoryScore = when {
            estimated <= available * 0.55 -> 100
            estimated <= available * 0.70 -> 90
            estimated <= available * 0.88 -> 72
            estimated <= available -> 55
            else -> 25
        }
        val params = modelParameterValue(entry.parameterLabel)
        val cpuBonus = when {
            profile.coreCount >= 8 && params >= 4.0 -> 6
            profile.coreCount >= 6 -> 3
            params <= 1.7 -> 4
            else -> 0
        }
        val preferenceBonus = when (recommendationPreference) {
            RecommendationPreference.SPEED -> if (params <= 1.7 || entry.quantization.contains("IQ1", ignoreCase = true)) 8 else 0
            RecommendationPreference.STABILITY -> if (entry.quantization.contains("Q4", ignoreCase = true) || params in 1.0..4.0) 8 else 0
            RecommendationPreference.SMALL_MODEL -> if (estimated <= 1100L) 10 else 0
        }
        return (memoryScore + cpuBonus + preferenceBonus).coerceIn(0, 100)
    }

    private fun fitLabel(entry: ModelScopeCatalogEntry, profile: DeviceProbeSnapshot): String {
        val estimated = estimateMobileMemoryMb(entry)
        val available = profile.availableRamMb.coerceAtLeast(512L)
        return when {
            estimated <= available * 0.70 -> "Recomendado"
            estimated <= available * 0.90 -> "Pode tentar"
            estimated <= available -> "Apertado"
            else -> "Não recomendado"
        }
    }

    private fun recommendationReasonFor(entry: ModelScopeCatalogEntry, profile: DeviceProbeSnapshot): String {
        val params = modelParameterValue(entry.parameterLabel)
        return when {
            fitLabel(entry, profile) == "Não recomendado" -> "RAM disponível atualmente baixa, recomenda-se 0.6B/1.5B primeiro."
            params >= 7.0 -> "Adequado para flagship com muita memória, verifique armazenamento e resfriamento antes de baixar."
            params >= 4.0 -> "Adequado para dispositivos intermediários a altos, escolha quando a qualidade for prioridade."
            params >= 1.5 -> "Equilíbrio entre qualidade e velocidade em dispositivos móveis."
            else -> "Ideal para verificar rapidamente API, download e cadeia de carregamento."
        }
    }

    private fun modelParameterValue(label: String): Double {
        val normalized = label.trim().uppercase(Locale.US)
        return when {
            normalized.endsWith("B") -> normalized.removeSuffix("B").toDoubleOrNull() ?: 0.0
            normalized.endsWith("M") -> (normalized.removeSuffix("M").toDoubleOrNull() ?: 0.0) / 1000.0
            else -> Regex("(\\d+(?:\\.\\d+)?)").find(normalized)?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        }
    }

    private fun buildTestChatCard(): View {
        val state = benchmarkUiStateMachine.state
        val activeProfile = state.profile ?: selectedBenchmarkProfile
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                surfaceCard(Palette.mint) {
                    addView(label("Modo de teste", 13f, Palette.muted, Typeface.BOLD))
                    addView(space(10))
                    addView(
                        LinearLayout(context).apply {
                            orientation = LinearLayout.HORIZONTAL
                            BenchmarkProfile.entries.forEachIndexed { index, profile ->
                                addView(
                                    benchmarkProfileOption(profile, activeProfile == profile, state.isRunning),
                                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                                        if (index > 0) marginStart = dp(4)
                                        if (index < BenchmarkProfile.entries.lastIndex) marginEnd = dp(4)
                                    }
                                )
                            }
                        }
                    )
                    addView(space(10))
                    val modeDetail = when (activeProfile) {
                        BenchmarkProfile.QUICK -> "Rápido: ver desempenho geral, não participa do ranking."
                        BenchmarkProfile.STANDARD -> "Padrão: especificação unificada repetida 3 vezes, gera pontuação para ranking."
                        BenchmarkProfile.STRESS -> "Estresse: execução contínua, observa temperatura e degradação de desempenho."
                    }
                    addView(label(modeDetail, 11.8f, Palette.muted, Typeface.NORMAL).apply { maxLines = 3 })
                }
            )
            addView(space(12))
            addView(
                surfaceCard(Palette.lavender, gradient = true) {
                    addView(cardHeader("Teste de desempenho de IA no dispositivo", "Modelo unificado, prompt unificado, algoritmo de pontuação fixo", "play", Palette.lavender, "v2"))
                    addView(space(12))
                    addView(buildModelLifecycleBanner(requiredBenchmarkModelLifecycle(), "Modelo padrão Qwen2.5 0.5B"))
                    addView(space(14))
                    addView(buildBenchmarkStatePanel(state))
                    addView(space(14))
                    when {
                        state is BenchmarkUiState.NeedsModel -> addView(
                            pillButton("Baixar modelo padrão · 469 MB", Palette.mintDark, Palette.mint) { downloadRequiredBenchmarkModel() },
                            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54))
                        )
                        state.isRunning -> {
                            addView(
                                softInfoBlock("Teste em execução, mantenha o app em primeiro plano.", Palette.sky, maxLines = 2).apply {
                                    gravity = Gravity.CENTER
                                    contentDescription = "Benchmark em andamento, mantenha o app em primeiro plano"
                                },
                                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            )
                            if (state !is BenchmarkUiState.Cancelling) {
                                addView(space(8))
                                addView(chipButton("Cancelar este benchmark", false) { confirmCancelBenchmark() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
                            }
                        }
                        state is BenchmarkUiState.Completed -> addView(
                            pillButton("Ver resultado desta vez", Palette.mintDark, Palette.mint) { setTab(AppTab.RESULTS) },
                            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54))
                        )
                        else -> {
                            val actionText = if (state is BenchmarkUiState.Blocked || state is BenchmarkUiState.Failed) "Verificar novamente" else "Iniciar ${benchmarkProfileName(selectedBenchmarkProfile)}"
                            addView(
                                pillButton(actionText, Palette.mintDark, Palette.mint) { runBenchmark(selectedBenchmarkProfile) },
                                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54))
                            )
                        }
                    }
                }
            )
        }
    }

    private fun benchmarkProfileOption(profile: BenchmarkProfile, selected: Boolean, disabled: Boolean): View {
        val accent = if (selected) Palette.mintDark else Palette.muted
        val caption = when (profile) {
            BenchmarkProfile.QUICK -> "Pré-visualização"
            BenchmarkProfile.STANDARD -> "3 vezes · Elegível para ranking"
            BenchmarkProfile.STRESS -> "Teste contínuo"
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            minimumHeight = dp(64)
            setPadding(dp(5), dp(10), dp(5), dp(10))
            background = ripple(
                rounded(
                    if (selected) Palette.mintPale else mixColor(Palette.surface, Palette.muted, 0.025f),
                    if (selected) tint(accent, 0.44f) else Color.TRANSPARENT,
                    7f,
                ),
                accent
            )
            isClickable = !disabled
            isFocusable = true
            isEnabled = !disabled
            alpha = if (disabled && !selected) 0.52f else 1f
            contentDescription = "${benchmarkProfileName(profile)}，$caption${if (selected) ", selecionado" else ""}"
            setOnClickListener {
                selectedBenchmarkProfile = profile
                renderCurrentTab()
            }
            addView(autoSizeSingleLineLabel(benchmarkProfileName(profile).removeSuffix("Modo"), 13.5f, 11f, accent, Typeface.BOLD))
            addView(space(4))
            addView(label(caption, 10.5f, Palette.muted, Typeface.NORMAL).apply { maxLines = 2; gravity = Gravity.CENTER })
        }
    }

    private fun buildBenchmarkStatePanel(state: BenchmarkUiState): View {
        val screen = benchmarkScreenUi(state)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            addView(label(screen.title, 16f, Palette.ink, Typeface.BOLD))
            addView(space(5))
            addView(label(screen.message, 13f, Palette.muted, Typeface.NORMAL).apply {
                maxLines = 4
                setLineSpacing(dp(2).toFloat(), 1f)
            })
            if (state !is BenchmarkUiState.NeedsModel && state !is BenchmarkUiState.Blocked && state !is BenchmarkUiState.Failed && state != BenchmarkUiState.Cancelled) {
                addView(space(14))
                addView(buildBenchmarkStepIndicator(state))
            }
            if (state.isRunning || state is BenchmarkUiState.Completed) {
                addView(space(12))
                addView(
                    LinearLayout(context).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        addView(
                            FrameLayout(context).apply {
                                addView(TuiMaCircularProgressView(context).apply { progress = screen.progressPercent }, FrameLayout.LayoutParams(dp(96), dp(96)))
                                addView(label("${screen.progressPercent}%", 18f, Palette.deepInk, Typeface.BOLD).apply { gravity = Gravity.CENTER }, FrameLayout.LayoutParams(dp(96), dp(96)))
                            },
                            LinearLayout.LayoutParams(dp(96), dp(96)).apply { marginEnd = dp(14) }
                        )
                        addView(
                            LinearLayout(context).apply {
                                orientation = LinearLayout.VERTICAL
                                addView(label(screen.phaseLabel, 13.5f, Palette.ink, Typeface.BOLD))
                                addView(space(7))
                                addView(label("Tempo restante estimado ${screen.remainingLabel}", 12f, Palette.muted, Typeface.NORMAL))
                                addView(space(5))
                                addView(label("Mantenha o app em primeiro plano", 11.5f, Palette.mintDark, Typeface.BOLD))
                            },
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        )
                    }
                )
                addView(space(12))
                addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        addView(liveMetricTile("Bateria", benchmarkLiveSnapshot.batteryPercent?.let { "$it%" } ?: "--"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) })
                        addView(liveMetricTile("Temperatura", benchmarkLiveSnapshot.temperatureCelsius?.let { "${"%.1f".format(Locale.US, it)}°C" } ?: "--"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
                        addView(liveMetricTile("Velocidade em tempo real", benchmarkLiveSnapshot.decodeTokensPerSecond?.let { "${"%.1f".format(Locale.US, it)} tok/s" } ?: "--"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) })
                    }
                )
            }
            if (state is BenchmarkUiState.Blocked) {
                addView(space(10))
                state.reasons.forEachIndexed { index, reason ->
                    addView(label("• ${preflightRecoveryLabel(reason)}", 12.5f, Palette.ink, Typeface.NORMAL))
                    if (index != state.reasons.lastIndex) addView(space(5))
                }
            }
            contentDescription = "${screen.title}。${screen.message}"
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
    }

    private fun buildBenchmarkStepIndicator(state: BenchmarkUiState): View {
        val activeStep = when (state) {
            is BenchmarkUiState.Checking -> 0
            is BenchmarkUiState.LoadingModel -> 1
            is BenchmarkUiState.WarmingUp -> 2
            is BenchmarkUiState.Measuring, is BenchmarkUiState.Cooling, is BenchmarkUiState.Cancelling -> 3
            is BenchmarkUiState.Completed -> 4
            else -> -1
        }
        val labels = listOf("Verificar", "Modelo", "Aquecimento", "Pontuação", "Resultados")
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            labels.forEachIndexed { index, title ->
                val reached = activeStep >= index
                val current = activeStep == index
                val accent = if (reached || current) Palette.mintDark else Palette.muted
                addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        addView(
                            label(if (activeStep > index) "✓" else "${index + 1}", 11f, if (reached) Palette.background else Palette.muted, Typeface.BOLD).apply {
                                gravity = Gravity.CENTER
                                background = rounded(if (reached) Palette.mint else tint(Palette.muted, 0.10f), tint(accent, 0.28f), 14f)
                            },
                            LinearLayout.LayoutParams(dp(28), dp(28))
                        )
                        addView(space(5))
                        addView(label(title, 10f, accent, if (current) Typeface.BOLD else Typeface.NORMAL))
                    },
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                )
            }
            contentDescription = if (activeStep >= 0) "Estágio atual ${activeStep + 1}, ${labels[activeStep]}" else "Aguardando início, cinco estágios no total"
        }
    }

    private fun liveMetricTile(title: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            minimumHeight = dp(64)
            setPadding(dp(4), dp(8), dp(4), dp(8))
            background = rounded(tint(Palette.surface, 0.76f), tint(Palette.sky, 0.18f), 11f)
            addView(label(value, 11.5f, Palette.ink, Typeface.BOLD).apply { maxLines = 1 })
            addView(space(3))
            addView(label(title, 10f, Palette.muted, Typeface.NORMAL))
            contentDescription = "$title，$value"
        }
    }

    private fun benchmarkScreenUi(state: BenchmarkUiState) = BenchmarkScreenPresenter.present(
        state = state,
        live = benchmarkLiveSnapshot.copy(
            elapsedMs = if (benchmarkStartedAtMs > 0L) (System.currentTimeMillis() - benchmarkStartedAtMs).coerceAtLeast(0L) else 0L
        ),
        modelDisplayName = ::displayModelName
    )

    private fun benchmarkStateTitle(state: BenchmarkUiState): String = benchmarkScreenUi(state).title

    private fun benchmarkStateMessage(state: BenchmarkUiState): String = benchmarkScreenUi(state).message

    private fun buildBenchmarkRequirementsCard(): View {
        return surfaceCard(Palette.sky) {
            addView(label("Verificação prévia", 14f, Palette.deepInk, Typeface.BOLD))
            addView(space(10))
            addView(readinessRow("Bateria", "Pelo menos 30%", true))
            addView(thinDivider())
            addView(readinessRow("Controle térmico", "Manter fresco", true))
            addView(thinDivider())
            addView(readinessRow("Executar", "App em primeiro plano", true))
            addView(space(8))
            addView(label("Modo padrão elegível para ranking; modo rápido para pré-visualização; modo estresse observa desempenho sustentado.", 12f, Palette.muted, Typeface.NORMAL).apply { maxLines = 2 })
        }
    }

    private fun requirementMetric(title: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(tint(Palette.sky, 0.06f), tint(Palette.sky, 0.18f), 7f)
            addView(label(value, 12f, Palette.deepInk, Typeface.BOLD).apply { maxLines = 1 })
            addView(space(3))
            addView(label(title, 10f, Palette.muted, Typeface.NORMAL))
            contentDescription = "$title，$value"
        }
    }

    private fun buildApiEndpointCard(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Palette.surface, Palette.stroke, 18f)
            elevation = dp(2).toFloat()
            setPadding(dp(16), dp(16), dp(16), dp(14))
            addView(
                LinearLayout(context).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(IconBadgeView(context, "cloud", Palette.blue), LinearLayout.LayoutParams(dp(42), dp(42)).apply { marginEnd = dp(12) })
                    addView(label("Interface local", 14f, tint(Palette.ink, 0.86f), Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(chip(label("Token local", 12f, Palette.mintDark, Typeface.BOLD), Palette.mintPale, Palette.mint))
                }
            )
            addView(space(12))
            addView(
                roundedTextBlock("http://127.0.0.1:8080"),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52))
            )
            addView(space(8))
            routeStatusText = label("Solicitações processadas apenas no serviço local.", 12f, Palette.muted, Typeface.NORMAL)
            addView(routeStatusText)
        }
    }

    private fun buildVisionHeroCard(): View {
        val imageName = selectedVisionImageName ?: "Nenhuma imagem selecionada"
        return surfaceCard(Palette.sky, gradient = true) {
            addView(cardHeader("Selecionar imagem para OCR", "Backend visual independente, não ocupa a biblioteca de modelos GGUF", "image", Palette.sky, "Vision"))
            addView(space(12))
            visionImageText = label(imageName, 13f, Palette.ink, Typeface.BOLD).apply {
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = rounded(tint(Palette.sky, 0.08f), Palette.stroke, 14f)
                maxLines = 2
            }
            addView(visionImageText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(space(12))
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(
                        pillButton("Selecionar imagem", Palette.sky, Palette.blue) { openVisionImagePicker() },
                        LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) }
                    )
                    addView(
                        pillButton("Iniciar OCR", Palette.mintDark, Palette.mint) { runOcrProbe() },
                        LinearLayout.LayoutParams(0, dp(48), 1f)
                    )
                }
            )
        }
    }

    private fun buildVisionModelStatusCard(): View {
        val models = scanVisionModelFiles()
        val sidecars = scanVisionSidecarFiles()
        return surfaceCard(Palette.mint) {
            addView(cardHeader("Modelos Visuais", "ONNX / TFLite / MNN / sidecar", "chip", Palette.mint, "${models.size + sidecars.size} itens", Palette.mintDark))
            addView(space(8))
            visionModelSummaryText = label(visionModelSummary(models, sidecars), 12f, Palette.muted, Typeface.NORMAL).apply {
                maxLines = 3
            }
            addView(visionModelSummaryText)
            addView(space(10))
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(
                        pillButton("Importar modelo", Palette.mintDark, Palette.mint) { openVisionModelPicker() },
                        LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(8) }
                    )
                    addView(
                        pillButton("Verificar modelo", Palette.sky, Palette.blue) { runVisionModelsProbe() },
                        LinearLayout.LayoutParams(0, dp(46), 1f)
                    )
                }
            )
            addView(space(8))
            addView(
                chipButton("Copiar diretório de modelos visuais", false) { copyVisionModelDir() },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42))
            )
            addView(space(8))
            addView(visionTaskRow("OCR", "rapid / ppocr / paddle / trocr", "ocr", Palette.mint))
            addView(space(8))
            addView(visionTaskRow("CLIP", "clip / vit ONNX encoder", "clip", Palette.sky))
            addView(space(8))
            addView(visionTaskRow("CIFAR10", "CNN pequena TFLite cifar10", "cifar10", Palette.blue))
            addView(space(8))
            addView(visionTaskRow("MNIST", "mnist TFLite pequena CNN", "mnist", Palette.lavender))
            addView(space(8))
            addView(visionTaskRow("Difusão", "Pacote de recursos MNN-Diffusion / SD1.5", "diffusion", Palette.sky))
            addView(space(10))
            addView(
                softInfoBlock("Pode importar .onnx / .ort / .tflite / .mnn, também pode importar cifar10-text-embeddings.json do CLIP.", Palette.sky, maxLines = 3)
            )
        }
    }

    private fun visionTaskRow(title: String, hint: String, task: String, accent: Int): View {
        val installed = hasVisionModelTask(task)
        return modelRow(
            title,
            hint,
            if (installed) "Importado" else "Ausente",
            if (installed) accent else Palette.muted
        )
    }

    private fun buildOcrModelCard(): View {
        return surfaceCard(Palette.mint) {
            addView(cardHeader("Soluções disponíveis", "Modelo OCR independente, não misturado no diretório LLM", "image", Palette.mint, "Visual", Palette.mintDark))
            addView(space(8))
            addView(modelRow("RapidOCR / PP-OCR", "ONNX Runtime Mobile, ideal para primeiro demo OCR Android", "Prioridade", Palette.mint))
            addView(space(8))
            addView(modelRow("Modelo pequeno PaddleOCR", "Dois estágios detecção + reconhecimento, mais estável para cenários em chinês", "Candidato", Palette.sky))
            addView(space(8))
            addView(modelRow("TrOCR tiny", "OCR Transformer, futura avaliação de imagens de documento", "Pesquisa", Palette.lavender))
        }
    }

    private fun buildVisionClassificationCard(): View {
        return surfaceCard(Palette.blue) {
            addView(cardHeader("CLIP / Classificação", "CLIP, CIFAR10, MNIST verificados separadamente", "gauge", Palette.blue, "Experimento"))
            addView(space(8))
            addView(modelRow("CLIP zero-shot", "Codificador imagem/texto ONNX, ideal para demonstração CIFAR10", "CIFAR10", Palette.sky))
            addView(space(8))
            addView(modelRow("CNN pequena CIFAR10", "Classificação direta TFLite, ideal para teste rápido de imagens locais", "CIFAR10", Palette.blue))
            addView(space(8))
            addView(modelRow("CNN pequena MNIST", "TFLite mais adequado para dígitos manuscritos, não forçar CLIP", "MNIST", Palette.lavender))
            addView(space(10))
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(
                        pillButton("Testar CIFAR10", Palette.sky, Palette.blue) { runVisionClassify("cifar10") },
                        LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(8) }
                    )
                    addView(
                        pillButton("Testar MNIST", Palette.mintDark, Palette.mint) { runVisionClassify("mnist") },
                        LinearLayout.LayoutParams(0, dp(46), 1f)
                    )
                }
            )
        }
    }

    private fun buildOcrResultCard(): View {
        return surfaceCard(Palette.lavender) {
            addView(cardHeader("Resultados", "Texto reconhecido, tempo e status do backend", "play", Palette.lavender, "Local"))
            addView(space(10))
            visionResultText = softInfoBlock("Selecione uma imagem. Após a integração do motor OCR, o texto reconhecido e o tempo serão exibidos aqui.", Palette.mint, maxLines = 5)
            addView(visionResultText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun buildApiActionStrip(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(
                actionTile("Copiar comando", "Copiar exemplo", "chip", Palette.mint) { copyCurlExample() },
                LinearLayout.LayoutParams(0, dp(126), 1f).apply { marginEnd = dp(6) }
            )
            addView(
                actionTile("Lista de modelos", "Modelo local", "cube", Palette.sky) { runModelsProbe() },
                LinearLayout.LayoutParams(0, dp(126), 1f).apply { marginStart = dp(6); marginEnd = dp(6) }
            )
            addView(
                actionTile("Testar conversa", "Resposta local", "play", Palette.lavender) { runTestChat() },
                LinearLayout.LayoutParams(0, dp(126), 1f).apply { marginStart = dp(6) }
            )
        }
    }

    private fun buildApiRoutesCard(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Palette.surface, Palette.stroke, 18f)
            elevation = dp(2).toFloat()
            setPadding(dp(16), dp(16), dp(16), dp(14))
            addView(label("Capacidade da interface", 14f, tint(Palette.ink, 0.86f), Typeface.BOLD))
            addView(space(10))
            addView(routeRow("GET", "/v1/models", "Ver modelos GGUF locais") { runModelsProbe() })
            addView(routeRow("POST", "/v1/chat/completions", "Enviar uma resposta local") { runTestChat() })
            addView(routeRow("GET", "/metrics", "Velocidade, primeiro token, memória") { runMetricsProbe() })
            addView(routeRow("GET", "/v1/benchmark/latest", "Último relatório TuiMa v2") {
                callLocalApi("/v1/benchmark/latest", "GET", null, onResult = { status, body, _ ->
                    routeStatusText?.text = if (status in 200..299) body.take(500) else "Nenhum relatório v2 disponível"
                })
            })
            addView(routeRow("GET", "/v1/recommendations", "Recomendado por capacidade do dispositivo") {
                setTab(AppTab.HOME)
            })
            addView(routeRow("GET", "/leaderboard/local", "Ler ranking local") { runLocalLeaderboardProbe() })
            addView(routeRow("GET", "/leaderboard/shared", "Status de configuração do ranking compartilhado") { runSharedLeaderboardProbe() })
            addView(routeRow("POST", "/leaderboard/shared", "Enviar registro do ranking local") { runSharedLeaderboardSync() })
            addView(routeRow("GET", "/vision/status", "Status da capacidade visual") { runVisionStatusProbe() })
            addView(routeRow("GET", "/vision/models", "Modelos visuais importados") { runVisionModelsProbe() })
            addView(routeRow("POST", "/vision/diffusion", "Prontidão de geração por difusão") { runVisionDiffusionProbe() })
        }
    }

    private fun buildSettingsCard(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Palette.surface, Palette.stroke, 18f)
            elevation = dp(2).toFloat()
            setPadding(dp(16), dp(16), dp(16), dp(14))
            addView(modelRow("Processamento local", "Benchmark e inferência de modelo concluídos no celular por padrão", "Privado", Palette.mint))
            addView(thinDivider())
            addView(modelRow("Diretório de modelos", "Biblioteca de modelos privada do app, suporta importação e download de GGUF", "Arquivo", Palette.sky))
            addView(thinDivider())
            addView(modelRow("Registro de benchmark", "Máximo de 50 relatórios v2 salvos", "Local", Palette.lavender))
            addView(thinDivider())
            addView(
                miniListCard(
                    title = "Aparência",
                    subtitle = "Seguir sistema, tema claro ou escuro",
                    badge = selectedThemeMode.displayName,
                    icon = "image",
                    accent = Palette.blue,
                    onClick = ::cycleThemeMode
                )
            )
        }
    }

    private fun buildLabAccessCard(): View {
        return surfaceCard(Palette.sky) {
            val links = listOf(
                LabLink("Multimodal local", "Autorização Omni, pré-verificação e gerenciamento de dois artefatos", "Experimento", "chip", Palette.lavender, AppTab.OMNI),
                LabLink("Busca na galeria local", "Recall local CLIP · Revisão G2D pendente", "Produto", "image", Palette.mint, AppTab.GALLERY),
                LabLink("Validação G2D no dispositivo", "Teste real de cinco estratégias Oxford-Pets", "Artigo", "chip", Palette.lavender, AppTab.G2D_LAB),
                LabLink("Gerenciamento de modelos visuais", "YOLO, CLIP e VLM pequeno", "Modelo", "cube", Palette.sky, AppTab.VISION_MODELS),
                LabLink("Reconhecimento visual", "OCR e sondas visuais leves", "Experimento", "image", Palette.lavender, AppTab.VISION),
                LabLink("Interface de desenvolvedor", "API local, serviços e rotas de diagnóstico", "Avançado", "cloud", Palette.sky, AppTab.API)
            )
            links.forEachIndexed { index, link ->
                addView(miniListCard(link.title, link.subtitle, link.badge, link.icon, link.accent) { setTab(link.tab) })
                if (index != links.lastIndex) addView(space(10))
            }
        }
    }

    private data class LabLink(
        val title: String,
        val subtitle: String,
        val badge: String,
        val icon: String,
        val accent: Int,
        val tab: AppTab
    )

    private fun buildLatestBenchmarkResultCard(): View {
        val reports = BenchmarkReportStore(applicationContext).toJson(limit = 50).optJSONArray("data") ?: JSONArray()
        val report = selectedScoredBenchmarkReport(reports)
        val snapshot = report?.let(ResultsScreenPresenter::parse)
        if (report == null || snapshot == null) {
            return surfaceCard(Palette.lavender, gradient = true) {
                addView(cardHeader("Nenhum resultado válido ainda", "Após um benchmark, mostrará pontuações duplas e detalhes 5 dimensões", "gauge", Palette.lavender))
                addView(space(16))
                addView(pillButton("Iniciar teste padrão", Palette.mintDark, Palette.mint) {
                    selectedBenchmarkProfile = BenchmarkProfile.STANDARD
                    setTab(AppTab.TEST)
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)))
            }
        }
        val insight = ResultsScreenPresenter.insight(snapshot)
        val selectedBaseline = ResultsScreenPresenter.comparableByRunId(snapshot, reports, comparisonBaselineRunId)
        val previous = selectedBaseline ?: ResultsScreenPresenter.previousComparable(snapshot, reports)
        val comparison = ResultsScreenPresenter.compare(snapshot, previous)

        return surfaceCard(Palette.mint, gradient = true) {
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    addView(label("Resultado deste teste", 11f, Palette.muted, Typeface.BOLD).apply { letterSpacing = 0.04f })
                    addView(space(9))
                    addView(autoSizeSingleLineLabel(formatHeadlineScore(snapshot.headlineScore), 48f, 28f, Palette.blue, Typeface.BOLD).apply {
                        gravity = Gravity.CENTER
                    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    addView(label("TuiMa", 16f, Palette.deepInk, Typeface.BOLD).apply { gravity = Gravity.CENTER })
                    addView(space(7))
                    addView(label("Pontuação padrão ${snapshot.canonicalScore} / 1000", 14f, Palette.mintDark, Typeface.BOLD).apply { gravity = Gravity.CENTER })
                    addView(space(10))
                    addView(
                        chip(
                            label(insight.rating, 15f, Palette.mintDark, Typeface.BOLD),
                            Palette.mintPale,
                            Palette.mint
                        ),
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38))
                    )
                    addView(space(8))
                    addView(label(insight.modeHint, 10.8f, Palette.muted, Typeface.BOLD).apply {
                        gravity = Gravity.CENTER
                        maxLines = 2
                    })
                    addView(space(8))
                    addView(
                        chip(label(snapshot.executionLabel, 10.5f, Palette.blue, Typeface.BOLD).apply { maxLines = 2 }, Palette.blueWash, Palette.sky),
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    )
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
            addView(space(18))
            addView(buildResultInsightCard(insight))
            addView(space(18))
            addView(label("Desempenho 5 dimensões", 14f, Palette.ink, Typeface.BOLD))
            addView(space(12))
            snapshot.dimensions.forEachIndexed { index, dimension ->
                val accent = listOf(Palette.mint, Palette.sky, Palette.lavender, Palette.blue, Palette.mintDark)[index]
                addView(scoreDimensionRow(dimension.label, dimension.value, dimension.maximum, accent))
                if (index != snapshot.dimensions.lastIndex) addView(space(10))
            }
            addView(space(18))
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(resultMetricTile("Velocidade de geração", "${"%.2f".format(Locale.US, snapshot.decodeTokensPerSecond)} tok/s"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) })
                    addView(resultMetricTile("Resposta do primeiro token", "${snapshot.firstTokenMs} ms"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
                    addView(resultMetricTile("Memória pico", "${snapshot.memoryPeakMb} MB"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) })
                }
            )
            addView(space(10))
            val temperatureText = snapshot.temperaturePeakCelsius?.let { "${"%.1f".format(Locale.US, it)}°C" } ?: "--"
            addView(label("Variação da bateria ${snapshot.batteryDeltaPercent}% · Temperatura máxima $temperatureText · ${formatReportDate(snapshot.createdAtMs)}", 12f, Palette.muted, Typeface.NORMAL).apply { maxLines = 2 })
            addView(space(16))
            addView(buildResultComparisonCard(comparison, customBaseline = selectedBaseline != null))
            addView(space(16))
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(chipButton("Compartilhar resultado", false) { shareBenchmarkResult(report) }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(5) })
                    addView(pillButton("Testar novamente", Palette.mintDark, Palette.mint) { setTab(AppTab.TEST) }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(5) })
                }
            )
        }
    }

    private fun buildResultInsightCard(insight: ai.mobilecore.ui.ResultInsight): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            addView(label("Interpretação de capacidade", 14f, Palette.deepInk, Typeface.BOLD))
            addView(space(7))
            addView(label(insight.summary, 13.5f, Palette.ink, Typeface.NORMAL).apply { maxLines = 2 })
            addView(space(10))
            addView(readinessRow("Ponto forte", insight.strongest.joinToString("、"), true))
            addView(thinDivider())
            addView(readinessRow("Gargalo principal", insight.bottleneck, false))
            addView(space(10))
            addView(label(insight.recommendation, 12.5f, Palette.muted, Typeface.NORMAL).apply {
                maxLines = 3
                setLineSpacing(dp(2).toFloat(), 1f)
            })
        }
    }

    private fun buildResultComparisonCard(comparison: ai.mobilecore.ui.ResultComparison?, customBaseline: Boolean): View {
        if (comparison == null) {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(tint(Palette.lavender, 0.06f), tint(Palette.lavender, 0.18f), 13f)
                setPadding(dp(13), dp(12), dp(13), dp(12))
                addView(label("Em comparação com anterior", 13.5f, Palette.ink, Typeface.BOLD))
                addView(space(4))
                addView(label("Nenhum resultado anterior com mesmo dispositivo, modelo, especificação e modo.", 12f, Palette.muted, Typeface.NORMAL).apply { maxLines = 2 })
            }
        }
        val comparisonTitle = if (customBaseline) "Em comparação com o resultado selecionado" else "Em comparação com anterior"
        val headline = comparison.canonicalPercentDelta?.let { "$comparisonTitle ${formatSignedPercent(it)}" }
            ?: "$comparisonTitle ${formatSignedInt(comparison.canonicalDelta)} pontos"
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(tint(Palette.lavender, 0.07f), tint(Palette.lavender, 0.20f), 13f)
            setPadding(dp(13), dp(12), dp(13), dp(12))
            addView(label(headline, 14f, Palette.deepInk, Typeface.BOLD))
            addView(space(9))
            addView(comparisonRow(
                "Pontuação padrão",
                comparison.current.canonicalScore.toString(),
                comparison.previous.canonicalScore.toString(),
                formatSignedInt(comparison.canonicalDelta),
                comparisonDeltaColor(comparison.canonicalDelta.toDouble(), lowerIsBetter = false)
            ))
            addView(space(6))
            addView(comparisonRow(
                "Velocidade de geração",
                "${"%.2f".format(Locale.US, comparison.current.decodeTokensPerSecond)}",
                "${"%.2f".format(Locale.US, comparison.previous.decodeTokensPerSecond)}",
                comparison.speedPercentDelta?.let(::formatSignedPercent) ?: "--",
                comparison.speedPercentDelta?.let { comparisonDeltaColor(it, lowerIsBetter = false) } ?: Palette.muted
            ))
            addView(space(6))
            addView(comparisonRow(
                "Resposta do primeiro token",
                "${comparison.current.firstTokenMs} ms",
                "${comparison.previous.firstTokenMs} ms",
                comparison.firstTokenPercentDelta?.let(::formatSignedPercent) ?: "--",
                comparison.firstTokenPercentDelta?.let { comparisonDeltaColor(it, lowerIsBetter = true) } ?: Palette.muted
            ))
            addView(space(6))
            addView(comparisonRow(
                "Memória pico",
                "${comparison.current.memoryPeakMb} MB",
                "${comparison.previous.memoryPeakMb} MB",
                "${if (comparison.memoryDeltaMb >= 0) "+" else ""}${comparison.memoryDeltaMb} MB",
                comparisonDeltaColor(comparison.memoryDeltaMb.toDouble(), lowerIsBetter = true)
            ))
            addView(space(6))
            addView(comparisonRow(
                "Temperatura máxima",
                comparison.current.temperaturePeakCelsius?.let { "${"%.1f".format(Locale.US, it)}°C" } ?: "--",
                comparison.previous.temperaturePeakCelsius?.let { "${"%.1f".format(Locale.US, it)}°C" } ?: "--",
                comparison.temperatureDeltaCelsius?.let { "${if (it >= 0) "+" else ""}${"%.1f".format(Locale.US, it)}°C" } ?: "--",
                comparison.temperatureDeltaCelsius?.let { comparisonDeltaColor(it, lowerIsBetter = true) } ?: Palette.muted
            ))
            addView(space(7))
            addView(label("Unidade de velocidade tok/s; compara apenas mesmo dispositivo, modelo, backend, especificação e modo de teste.", 10.8f, Palette.muted, Typeface.NORMAL).apply { maxLines = 2 })
        }
    }

    private fun comparisonHeaderRow(): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(label("Métrica", 10.5f, Palette.muted, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.15f))
            addView(label("Atual", 10.5f, Palette.muted, Typeface.BOLD).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(label("Anterior", 10.5f, Palette.muted, Typeface.BOLD).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(label("Variação", 10.5f, Palette.muted, Typeface.BOLD).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun comparisonRow(labelText: String, current: String, previous: String, delta: String, deltaColor: Int): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(tint(Palette.surface, 0.32f), Color.TRANSPARENT, TuiMaTheme.cardRadiusDp)
            setPadding(dp(10), dp(9), dp(10), dp(9))
            addView(label(labelText, 11.5f, Palette.ink, Typeface.BOLD))
            addView(space(7))
            addView(
                LinearLayout(context).apply {
                    addView(comparisonValueCell("Atual", current, Palette.ink), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) })
                    addView(comparisonValueCell("Anterior", previous, Palette.muted), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
                    addView(comparisonValueCell("Variação", delta, deltaColor), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) })
                }
            )
        }
    }

    private fun comparisonValueCell(caption: String, value: String, color: Int): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(autoSizeSingleLineLabel(value, 11f, 8f, color, Typeface.BOLD).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(space(3))
            addView(label(caption, 9.5f, Palette.muted, Typeface.NORMAL).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun comparisonDeltaColor(value: Double, lowerIsBetter: Boolean): Int {
        if (value == 0.0) return Palette.muted
        val improved = if (lowerIsBetter) value < 0.0 else value > 0.0
        return if (improved) Palette.mintDark else Palette.blue
    }

    private fun formatSignedInt(value: Int): String = "${if (value >= 0) "+" else ""}$value"

    private fun formatSignedPercent(value: Double): String = "${if (value >= 0) "+" else ""}${"%.1f".format(Locale.US, value)}%"

    private fun scoreDimensionRow(title: String, value: Int, maximum: Int, accent: Int): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                LinearLayout(context).apply {
                    addView(label(title, 13.5f, Palette.ink, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(label("$value / $maximum", 13f, accent, Typeface.BOLD))
                }
            )
            addView(space(6))
            addView(
                ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = maximum
                    progress = value.coerceIn(0, maximum)
                    progressTintList = ColorStateList.valueOf(accent)
                    progressBackgroundTintList = ColorStateList.valueOf(tint(Palette.muted, 0.16f))
                    contentDescription = "$title $value pontos, máximo $maximum pontos"
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8))
            )
        }
    }

    private fun resultMetricTile(title: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            minimumHeight = dp(72)
            setPadding(dp(5), dp(9), dp(5), dp(9))
            background = rounded(tint(Palette.sky, 0.08f), tint(Palette.sky, 0.20f), 7f)
            addView(autoSizeSingleLineLabel(value, 12.5f, 9f, Palette.ink, Typeface.BOLD).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(space(3))
            addView(label(title, 10.5f, Palette.muted, Typeface.NORMAL))
            contentDescription = "$title，$value"
        }
    }

    private fun buildBenchmarkHistoryCard(): View {
        val data = BenchmarkReportStore(applicationContext).toJson(limit = 10).optJSONArray("data") ?: JSONArray()
        return surfaceCard(Palette.sky) {
            if (data.length() == 0) {
                addView(label("Nenhum histórico. Após o benchmark, os 10 últimos resultados serão salvos no dispositivo.", 13f, Palette.muted, Typeface.NORMAL).apply {
                    setPadding(dp(8), dp(10), dp(8), dp(10))
                    maxLines = 3
                })
            } else {
                addView(buildHistoryComparisonSelector(data))
                addView(space(12))
                for (index in 0 until data.length()) {
                    val report = data.optJSONObject(index) ?: continue
                    addView(buildBenchmarkHistoryRow(report))
                    if (index < data.length() - 1) addView(space(8))
                }
            }
        }
    }

    private fun buildHistoryComparisonSelector(data: JSONArray): View {
        val allReports = BenchmarkReportStore(applicationContext).toJson(limit = 50).optJSONArray("data") ?: data
        val current = selectedScoredBenchmarkReport(allReports)?.let(ResultsScreenPresenter::parse)
        val baseline = current?.let { ResultsScreenPresenter.comparableByRunId(it, allReports, comparisonBaselineRunId) }
        val status = when {
            current == null -> "Complete um benchmark válido primeiro"
            selectingComparisonBaseline -> "Selecione outro resultado abaixo"
            baseline != null -> "Selecionado ${formatReportDate(baseline.createdAtMs)} · Pontuação padrão ${baseline.canonicalScore}"
            else -> "Comparar automaticamente com o resultado anterior da mesma especificação"
        }
        val action = when {
            selectingComparisonBaseline -> "Cancelar seleção"
            baseline != null -> "Trocar referência"
            else -> "Selecionar referência de comparação"
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(tint(Palette.blue, 0.07f), tint(Palette.blue, 0.20f), 13f)
            setPadding(dp(13), dp(12), dp(13), dp(12))
            addView(label("Comparação de dois resultados", 13.5f, Palette.ink, Typeface.BOLD))
            addView(space(4))
            addView(label(status, 11.8f, if (selectingComparisonBaseline) Palette.mintDark else Palette.muted, Typeface.NORMAL).apply { maxLines = 2 })
            addView(space(9))
            addView(
                chipButton(action, selectingComparisonBaseline) {
                    selectingComparisonBaseline = !selectingComparisonBaseline
                    renderCurrentTab()
                }.apply { isEnabled = current != null },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44))
            )
        }
    }

    private fun latestScoredBenchmarkReport(): JSONObject? {
        val data = BenchmarkReportStore(applicationContext).toJson(limit = 50).optJSONArray("data") ?: return null
        return selectedScoredBenchmarkReport(data)
    }

    private fun selectedScoredBenchmarkReport(data: JSONArray): JSONObject? {
        if (!selectedResultRunId.isNullOrBlank()) {
            for (index in 0 until data.length()) {
                val report = data.optJSONObject(index) ?: continue
                if (report.optString("run_id") == selectedResultRunId && report.optBoolean("valid", false) && report.optJSONObject("score") != null) {
                    return report
                }
            }
        }
        for (index in 0 until data.length()) {
            val report = data.optJSONObject(index) ?: continue
            if (report.optBoolean("valid", false) && report.optJSONObject("score") != null) return report
        }
        return null
    }

    private fun buildBenchmarkHistoryRow(report: JSONObject): View {
        val score = report.optJSONObject("score")
        val spec = report.optJSONObject("spec") ?: JSONObject()
        val summary = report.optJSONObject("summary") ?: JSONObject()
        val valid = report.optBoolean("valid", false) && score != null
        val title = if (valid) "${formatHeadlineScore(score?.optInt("headline") ?: 0)} TuiMa" else "Nenhuma pontuação gerada"
        val detail = if (valid) {
            val backend = ResultsScreenPresenter.parse(report)?.backendLabel ?: "CPU"
            "标准分 ${score?.optInt("canonical")} / 1000 · ${profileDisplayName(spec.optString("profile"))} · $backend"
        } else {
            "${report.optString("failure_kind", "Teste não concluído")} · 已完成 ${summary.optInt("completed_runs")}/${summary.optInt("measured_runs")}"
        }
        val accent = if (valid) Palette.mint else Palette.lavender
        val selected = report.optString("run_id") == selectedResultRunId
        val comparisonBaseline = report.optString("run_id") == comparisonBaselineRunId
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(68)
            background = if (valid) {
                val active = selected || comparisonBaseline
                ripple(rounded(tint(accent, if (active) 0.16f else 0.07f), tint(accent, if (active) 0.42f else 0.18f), 13f), accent)
            } else {
                rounded(tint(accent, 0.07f), tint(accent, 0.18f), 13f)
            }
            setPadding(dp(12), dp(10), dp(12), dp(10))
            isClickable = valid
            isFocusable = valid
            if (valid) {
                setOnClickListener {
                    if (selectingComparisonBaseline) {
                        selectComparisonBaseline(report)
                    } else {
                        selectedResultRunId = report.optString("run_id")
                        comparisonBaselineRunId = null
                        selectingComparisonBaseline = false
                        renderCurrentTab()
                    }
                }
            }
            addView(IconBadgeView(context, if (valid) "gauge" else "stop", accent).apply { contentDescription = null }, LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(10) })
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(label(title, 14f, Palette.ink, Typeface.BOLD))
                    addView(space(3))
                    addView(label(detail, 11.5f, Palette.muted, Typeface.NORMAL).apply { maxLines = 2 })
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.END
                    addView(label(formatReportDate(report.optLong("created_at_ms")), 10.5f, Palette.muted, Typeface.NORMAL))
                    if (comparisonBaseline) {
                        addView(space(4))
                        addView(label("Referência de comparação", 10f, Palette.mintDark, Typeface.BOLD))
                    }
                }
            )
            contentDescription = "$title，$detail，${formatReportDate(report.optLong("created_at_ms"))}${when {
                comparisonBaseline -> ", referência de comparação atual"
                selected -> ", visualizando"
                selectingComparisonBaseline && valid -> ", clique para definir como referência"
                valid -> ", clique para ver detalhes"
                else -> ""
            }}"
        }
    }

    private fun selectComparisonBaseline(report: JSONObject) {
        val allReports = BenchmarkReportStore(applicationContext).toJson(limit = 50).optJSONArray("data") ?: JSONArray()
        val current = selectedScoredBenchmarkReport(allReports)?.let(ResultsScreenPresenter::parse)
        val candidate = ResultsScreenPresenter.parse(report)
        when {
            current == null || candidate == null -> Toast.makeText(this, "Nenhum resultado válido comparável", Toast.LENGTH_SHORT).show()
            candidate.runId == current.runId -> Toast.makeText(this, "Selecione outro resultado", Toast.LENGTH_SHORT).show()
            candidate.comparisonKey != current.comparisonKey -> Toast.makeText(this, "Só é possível comparar mesmo dispositivo, modelo, backend, especificação e modo", Toast.LENGTH_LONG).show()
            else -> {
                comparisonBaselineRunId = candidate.runId
                selectingComparisonBaseline = false
                renderCurrentTab()
            }
        }
    }

    private fun formatReportDate(createdAtMs: Long): String {
        if (createdAtMs <= 0L) return "Tempo desconhecido"
        return SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(createdAtMs))
    }

    private fun formatHeadlineScore(value: Int): String = NumberFormat.getIntegerInstance(Locale.US).format(value)

    private fun profileDisplayName(apiName: String): String = when (apiName) {
        BenchmarkProfile.STANDARD.apiName -> "Padrão"
        BenchmarkProfile.STRESS.apiName -> "Estresse"
        else -> "Rápido"
    }

    private fun shareBenchmarkResult(report: JSONObject) {
        val snapshot = ResultsScreenPresenter.parse(report) ?: return
        val insight = ResultsScreenPresenter.insight(snapshot)
        runCatching {
            val file = BenchmarkShareCardRenderer.render(applicationContext, snapshot, insight)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareText = "Meu celular alcançou ${formatHeadlineScore(snapshot.headlineScore)} TuiMa, pontuação padrão ${snapshot.canonicalScore} / 1000."
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        clipData = ClipData.newUri(contentResolver, "Cartão de resultado TuiMa", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Compartilhar cartão de resultado TuiMa"
                )
            )
        }.onFailure {
            Toast.makeText(this, "Falha ao gerar cartão de resultado, tente novamente mais tarde", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildModelHubCard(): View {
        providerStateByProvider.putIfAbsent(downloadTaskKey(modelHubItems[0]), ModelDownloadState(modelHubItems[0]))
        providerStateByProvider.putIfAbsent(downloadTaskKey(modelHubItems[1]), ModelDownloadState(modelHubItems[1]))
        return surfaceCard(Palette.mint) {
            addView(cardHeader("Estação de modelos", "Fila de download ModelScope / HuggingFace", "download", Palette.mint, "GGUF"))
            addView(space(10))
            val modelScopeTile = actionTile("ModelScope", "Espelho nacional (recomendado)", "download", Palette.mintDark) {
                enqueueModelDownload(modelHubItems.first { it.provider == "ModelScope" })
            }
            val huggingFaceTile = actionTile("HuggingFace", "Qwen 0.5B", "download", Palette.blue) {
                enqueueModelDownload(modelHubItems.first { it.provider == "HuggingFace" })
            }
            providerTileByProvider[downloadTaskKey(modelHubItems.first { it.provider == "HuggingFace" })] = huggingFaceTile
            providerTileByProvider[downloadTaskKey(modelHubItems.first { it.provider == "ModelScope" })] = modelScopeTile
            addView(
                actionRow(modelScopeTile, huggingFaceTile)
            )
            addView(space(10))
            addView(buildModelHubStatusRows())
            addView(space(10))
            addView(label("Baixar para biblioteca de modelos do app, disponível para carregar após conclusão.", 12f, Palette.muted, Typeface.NORMAL).apply { maxLines = 2 })
        }
    }

    private fun buildModelHubStatusRows(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label("Status do download", 13f, Palette.ink, Typeface.BOLD).apply { maxLines = 1 })
            addView(space(6))
            addView(buildModelHubStatusRow(modelHubItems.first { it.provider == "ModelScope" }))
            addView(space(6))
            addView(buildModelHubStatusRow(modelHubItems.first { it.provider == "HuggingFace" }))
        }
    }

    private fun buildModelHubStatusRow(item: ModelHubItem): View {
        val taskKey = downloadTaskKey(item)
        val titleText = label("${item.provider} · ${item.shortName}", 13f, Palette.ink, Typeface.BOLD)
        val statusText = label("Não baixado", 12f, Palette.muted, Typeface.BOLD)
        val messageText = label("Arquivo do modelo ainda não salvo no dispositivo", 12f, Palette.muted, Typeface.NORMAL)
        val progressText = label("Progresso: 0B / Desconhecido (0%)", 11f, Palette.muted, Typeface.NORMAL)
        val cancelButton = (pillButton("Pausar", Palette.sky, Palette.blue) {
            handleDownloadControl(taskKey)
        } as TextView).apply {
            visibility = View.GONE
            setPadding(0, dp(4), 0, dp(4))
        }

        providerTitleByProvider[taskKey] = titleText
        providerStatusByProvider[taskKey] = statusText
        providerMessageByProvider[taskKey] = messageText
        providerProgressByProvider[taskKey] = progressText
        providerCancelByProvider[taskKey] = cancelButton
        refreshModelDownloadStatus(taskKey)

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(tint(Palette.mint, 0.10f), Palette.stroke, 14f)
            addView(
                LinearLayout(context).apply {
                    addView(titleText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(cancelButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }
            )
            addView(space(4))
            addView(statusText)
            addView(space(2))
            addView(messageText)
            addView(progressText)
        }
    }

    private fun buildRecommendationCard(): View {
        recommendationContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        renderRecommendationPlaceholder("Inicie a API para carregar recomendações; ou baixe GGUF da estação de modelos primeiro.")

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Palette.surface, Palette.stroke, 18f)
            elevation = dp(2).toFloat()
            setPadding(dp(16), dp(16), dp(16), dp(14))
            addView(
                LinearLayout(context).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(label("Modelo recomendado", 14f, tint(Palette.ink, 0.86f), Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(label("MobileCore", 13f, Palette.muted, Typeface.BOLD))
                }
            )
            addView(space(10))
            addView(buildPreferenceControl())
            addView(space(10))
            addView(recommendationContainer)
        }
    }

    private fun buildPreferenceControl(): View {
        val captions = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(label("Prioridade de velocidade", 11f, Palette.muted, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(label("Prioridade de estabilidade", 11f, Palette.muted, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(label("Prioridade de modelo pequeno", 11f, Palette.muted, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Palette.mintPale, tint(Palette.mint, 0.20f), 16f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            addView(
                LinearLayout(context).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(label("Preferência", 12f, Palette.muted, Typeface.BOLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    preferenceLabelText = label(recommendationPreference.label, 13f, Palette.mintDark, Typeface.BOLD)
                    addView(preferenceLabelText)
                }
            )
            addView(space(8))
            addView(
                SeekBar(context).apply {
                    max = 2
                    progress = recommendationPreference.progress
                    progressTintList = ColorStateList.valueOf(Palette.mintDark)
                    thumbTintList = ColorStateList.valueOf(Palette.blue)
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            recommendationPreference = RecommendationPreference.fromProgress(progress)
                            updatePreferenceLabel()
                            if (fromUser) {
                                saveRecommendationPreference(recommendationPreference)
                                renderRecommendationPlaceholder("Atualizando recomendações por ${recommendationPreference.label}...")
                                refreshRecommendationSnapshot()
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                    })
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44))
            )
            addView(captions)
        }
    }

    private fun updatePreferenceLabel() {
        if (::preferenceLabelText.isInitialized) {
            preferenceLabelText.text = recommendationPreference.label
        }
    }

    private fun readRecommendationPreference(): RecommendationPreference {
        val stored = getPreferences(MODE_PRIVATE).getString(PREF_RECOMMENDATION_MODE, null)
        return RecommendationPreference.fromQueryValue(stored)
    }

    private fun saveRecommendationPreference(preference: RecommendationPreference) {
        getPreferences(MODE_PRIVATE)
            .edit()
            .putString(PREF_RECOMMENDATION_MODE, preference.queryValue)
            .apply()
    }

    private fun readThemeMode(): TuiMaThemeMode {
        return TuiMaThemeMode.fromPreference(getPreferences(MODE_PRIVATE).getString(PREF_UI_THEME_MODE, null))
    }

    private fun cycleThemeMode() {
        selectedThemeMode = selectedThemeMode.next()
        getPreferences(MODE_PRIVATE)
            .edit()
            .putString(PREF_UI_THEME_MODE, selectedThemeMode.preferenceValue)
            .apply()
        TuiMaTheme.configure(selectedThemeMode, isSystemDarkTheme())
        recreate()
    }

    private fun isSystemDarkTheme(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun miniListCard(
        title: String,
        subtitle: String,
        badge: String?,
        icon: String,
        accent: Int,
        selected: Boolean = false,
        onClick: (() -> Unit)? = null
    ): View {
        val backgroundColor = tint(accent, if (selected) 0.14f else 0.075f)
        val borderColor = tint(accent, if (selected) 0.34f else 0.18f)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(58)
            background = if (onClick == null) rounded(backgroundColor, borderColor, 14f) else ripple(rounded(backgroundColor, borderColor, 14f), accent)
            setPadding(dp(11), dp(9), dp(10), dp(9))
            if (onClick != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
            addView(IconBadgeView(context, icon, accent), LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginEnd = dp(10) })
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(label(title, 12.2f, Palette.ink, Typeface.BOLD).apply { maxLines = 2 })
                    addView(space(4))
                    addView(label(subtitle, 10.5f, Palette.muted, Typeface.NORMAL).apply { maxLines = 2 })
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            if (!badge.isNullOrBlank()) {
                addView(
                    chip(label(badge, 10.2f, tint(accent, 0.78f), Typeface.BOLD), tint(accent, 0.12f), accent),
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30)).apply { marginStart = dp(8) }
                )
            }
        }
    }

    private fun actionRow(left: View, right: View): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(5) })
            addView(right, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(5) })
        }
    }

    private fun actionTile(
        title: String,
        caption: String,
        icon: String,
        accent: Int,
        onClick: () -> Unit
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(92)
            background = ripple(rounded(tint(accent, 0.075f), tint(accent, 0.18f), 14f), accent)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            addView(IconBadgeView(context, icon, accent), LinearLayout.LayoutParams(dp(30), dp(30)))
            addView(space(7))
            addView(label(title, 11.5f, tint(Palette.ink, 0.62f), Typeface.BOLD).apply { maxLines = 1 })
            addView(space(3))
            addView(label(caption, 10.3f, Palette.muted, Typeface.NORMAL).apply { maxLines = 1 })
        }
    }

    private fun buildRecentModelsCard(): View {
        val model = findPreferredGguf()
        return surfaceCard(Palette.lavender) {
            addView(cardHeader("Modelos recentes", "Importar para carregar e testar diretamente", "cube", Palette.lavender, "GGUF"))
            addView(space(12))
            if (model != null) {
                val lifecycle = modelLifecycle(model, model.name, null)
                addView(modelRow(model.nameWithoutExtension, "${formatBytes(model.length())} · ${lifecycle.supportingText}", lifecycle.statusLabel, modelLifecycleAccent(lifecycle.tone)))
            } else {
                addView(modelRow("Nenhum modelo local disponível", "Baixe da página de modelos ou importe GGUF de arquivo", "Não baixado", Palette.muted))
            }
            addView(space(8))
            if (model?.name != requiredBenchmarkModelName()) {
                val standardLifecycle = requiredBenchmarkModelLifecycle()
                addView(modelRow("Qwen2.5 0.5B", standardLifecycle.supportingText, standardLifecycle.statusLabel, modelLifecycleAccent(standardLifecycle.tone)))
            }
        }
    }

    private fun buildStatusCard(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Palette.surface, Palette.stroke, 18f)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(label("Endereço local", 12f, Palette.muted, Typeface.BOLD))
            addView(space(6))
            statusText = label("http://$serviceHost:$servicePort/v1", 14f, Palette.ink, Typeface.BOLD)
            addView(statusText)
            addView(space(4))
            addView(label("Formato compatível com OpenAI, solicitações mantidas localmente.", 12f, Palette.muted, Typeface.NORMAL))
        }
    }

    private fun refreshRecommendationSnapshot() {
        Thread {
            repeat(3) {
                try {
                    val url = "http://$serviceHost:$servicePort/v1/recommendations?preference=${recommendationPreference.queryValue}"
                    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Authorization", "Bearer local")
                        connectTimeout = 1200
                        readTimeout = 1200
                    }
                    val status = connection.responseCode
                    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                    val body = stream?.bufferedReader()?.readText() ?: "{}"
                    if (status in 200..299) {
                        applyRecommendationPayload(JSONObject(body))
                        return@Thread
                    }
                } catch (_: Exception) {
                    // Keep raw networking details out of the visible UI.
                }
                Thread.sleep(300)
            }
            runOnUiThread {
                renderRecommendationPlaceholder("Recomendação temporariamente indisponível, inicie o serviço local primeiro.")
            }
        }.start()
    }

    private fun applyRecommendationPayload(json: JSONObject) {
        val recommendations = json.optJSONArray("recommendations") ?: JSONArray()

        runOnUiThread {
            if (!::recommendationContainer.isInitialized) return@runOnUiThread

            recommendationContainer.removeAllViews()
            if (recommendations.length() == 0) {
                renderRecommendationPlaceholder("Serviço conectado, mas nenhum GGUF detectado. Importe um modelo primeiro.")
                return@runOnUiThread
            }

            for (i in 0 until recommendations.length()) {
                val recommendation = recommendations.optJSONObject(i) ?: continue
                val modelId = recommendation.optString("model_id", "unknown")
                val fit = recommendation.optString("fit", "marginal")
                val score = recommendation.optDouble("score", 0.0)
                val expected = recommendation.optDouble("expected_tokens_per_second", 0.0)
                val loaded = recommendation.optBoolean("loaded", false)
                val reasonArray = recommendation.optJSONArray("reasons")
                val reason = if (reasonArray == null || reasonArray.length() == 0) {
                    "Adequado para a configuração atual do dispositivo."
                } else {
                    (0 until reasonArray.length()).joinToString(" · ") { idx ->
                        reasonArray.optString(idx)
                    }
                }
                recommendationContainer.addView(
                    buildRecommendationRow(
                        modelId = modelId,
                        score = score,
                        fit = fit,
                        estimatedMemoryMb = recommendation.optLong("estimated_memory_mb", 0L),
                        expectedTokensPerSecond = expected,
                        loaded = loaded,
                        reason = reason
                    )
                )
            }
        }
    }

    private fun renderRecommendationPlaceholder(message: String) {
        if (!::recommendationContainer.isInitialized) return
        recommendationContainer.removeAllViews()
        recommendationContainer.addView(
            label(message, 13f, Palette.muted, Typeface.NORMAL).apply {
                setPadding(0, dp(2), 0, dp(2))
            }
        )
    }

    private fun enqueueModelDownload(item: ModelHubItem) {
        val taskKey = downloadTaskKey(item)
        val state = providerStateByProvider[taskKey] ?: ModelDownloadState(item)
        val destination = File(externalModelDir(), item.fileName)
        providerStateByProvider[taskKey] = state

        if (state.isActive) {
            Toast.makeText(this, "Download de ${item.shortName} em andamento", Toast.LENGTH_SHORT).show()
            return
        }

        state.item = item
        if (destination.exists() && destination.length() > 1024 * 1024) {
            state.status = DownloadState.SUCCESS
            state.bytesDownloaded = destination.length()
            state.totalBytes = destination.length()
            state.percent = 100
            state.failureMessage = null
            refreshModelDownloadStatus(taskKey)
            syncBenchmarkReadiness()
            Toast.makeText(this, "${item.shortName} já está no dispositivo", Toast.LENGTH_SHORT).show()
            ensureNotificationPermissionAndLoadModel(destination)
            return
        }

        destination.parentFile?.mkdirs()
        state.status = DownloadState.DOWNLOADING
        state.destination = destination
        val partFile = File(destination.parentFile, "${destination.name}.part")
        val resumeBytes = partFile.takeIf { it.exists() }?.length()?.coerceAtLeast(0L) ?: 0L
        state.bytesDownloaded = resumeBytes
        state.transferStartedAtMs = System.currentTimeMillis()
        state.transferStartedBytes = resumeBytes
        state.percent = if (state.totalBytes > 0L && resumeBytes > 0L) {
            ((resumeBytes.toDouble() / state.totalBytes.toDouble()) * 100).toInt().coerceIn(0, 99)
        } else {
            0
        }
        state.failureMessage = null
        state.cancelRequested = false
        refreshModelDownloadStatus(taskKey)
        updateStatus(
            if (resumeBytes > 0L) "Continuar download de ${item.shortName}"
            else "Baixando ${item.shortName}"
        )
        progressHandler.removeCallbacks(progressPollRunnable)
        progressHandler.post(progressPollRunnable)
        Toast.makeText(this, "Download de ${item.shortName} iniciado", Toast.LENGTH_SHORT).show()
        val thread = Thread {
            downloadModelInApp(taskKey, item, destination, state)
        }
        activeDownloadThreads[taskKey] = thread
        thread.start()
    }

    private fun downloadModelInApp(taskKey: String, item: ModelHubItem, destination: File, state: ModelDownloadState) {
        val partFile = File(destination.parentFile, "${destination.name}.part")
        var connection: HttpURLConnection? = null
        try {
            if (destination.exists()) destination.delete()
            var resumeBytes = partFile.takeIf { it.exists() }?.length()?.coerceAtLeast(0L) ?: 0L
            connection = openDownloadConnection(item.url, startByte = resumeBytes)
            if (resumeBytes > 0L && connection.responseCode != HttpURLConnection.HTTP_PARTIAL) {
                partFile.delete()
                resumeBytes = 0L
            }
            val totalBytes = (connection.contentLengthLong.coerceAtLeast(0L) + resumeBytes).coerceAtLeast(0L)
            state.totalBytes = totalBytes
            state.bytesDownloaded = resumeBytes
            FileOutputStream(partFile, resumeBytes > 0L).use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = resumeBytes
                    var lastUiUpdate = 0L
                    while (true) {
                        if (state.cancelRequested || Thread.currentThread().isInterrupted) {
                            throw DownloadPausedException()
                        }
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read.toLong()
                        val now = System.currentTimeMillis()
                        if (now - lastUiUpdate > 250L) {
                            lastUiUpdate = now
                            state.bytesDownloaded = downloaded
                            state.percent = if (totalBytes > 0L) {
                                ((downloaded.toDouble() / totalBytes.toDouble()) * 100).toInt().coerceIn(0, 99)
                            } else {
                                0
                            }
                            runOnUiThread { refreshModelDownloadStatus(taskKey) }
                        }
                    }
                }
            }

            if (partFile.length() <= 1024 * 1024) {
                throw IOException("Arquivo de download muito pequeno, pode não ser modelo GGUF")
            }
            if (!partFile.renameTo(destination)) {
                partFile.copyTo(destination, overwrite = true)
                partFile.delete()
            }
            state.status = DownloadState.SUCCESS
            state.bytesDownloaded = destination.length()
            state.totalBytes = destination.length()
            state.percent = 100
            state.failureMessage = null
            state.cancelRequested = false
            activeDownloadThreads.remove(taskKey)
            runOnUiThread {
                refreshModelDownloadStatus(taskKey)
                updateStatus("Modelo baixado: ${destination.name}")
                Toast.makeText(this, "Modelo baixado", Toast.LENGTH_SHORT).show()
                ensureNotificationPermissionAndLoadModel(destination)
                refreshRecommendationSnapshot()
                syncBenchmarkReadiness()
                progressEndIfNeeded()
                if (currentTab in setOf(AppTab.MODELS, AppTab.PLAYGROUND)) renderCurrentTab()
            }
        } catch (_: DownloadPausedException) {
            state.status = DownloadState.PAUSED
            state.bytesDownloaded = partFile.takeIf { it.exists() }?.length()?.coerceAtLeast(0L) ?: state.bytesDownloaded
            state.percent = if (state.totalBytes > 0L) {
                ((state.bytesDownloaded.toDouble() / state.totalBytes.toDouble()) * 100).toInt().coerceIn(0, 99)
            } else {
                0
            }
            state.failureMessage = "Pausado, pode continuar"
            state.cancelRequested = false
            activeDownloadThreads.remove(taskKey)
            runOnUiThread {
                refreshModelDownloadStatus(taskKey)
                updateStatus("Download de ${item.shortName} pausado")
                Toast.makeText(this, "${item.shortName} pausado", Toast.LENGTH_SHORT).show()
                progressEndIfNeeded()
                if (currentTab in setOf(AppTab.MODELS, AppTab.PLAYGROUND)) renderCurrentTab()
            }
        } catch (e: Exception) {
            state.status = DownloadState.FAILED
            state.failureMessage = "Motivo da falha: ${readableDownloadError(e)}"
            state.bytesDownloaded = partFile.takeIf { it.exists() }?.length()?.coerceAtLeast(0L) ?: state.bytesDownloaded
            state.cancelRequested = false
            activeDownloadThreads.remove(taskKey)
            runOnUiThread {
                refreshModelDownloadStatus(taskKey)
                updateStatus("Falha no download do modelo: ${readableDownloadError(e)}")
                Toast.makeText(this, "Falha no download do modelo", Toast.LENGTH_LONG).show()
                progressEndIfNeeded()
                if (currentTab in setOf(AppTab.MODELS, AppTab.PLAYGROUND)) renderCurrentTab()
            }
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildRecommendationRow(
        modelId: String,
        score: Double,
        fit: String,
        estimatedMemoryMb: Long,
        expectedTokensPerSecond: Double,
        loaded: Boolean,
        reason: String
    ): View {
        val scoreText = String.format(Locale.US, "%.1f", score)
        val speedText = String.format(Locale.US, "%.2f", expectedTokensPerSecond)
        val accent = when (fit.lowercase()) {
            "perfect" -> Palette.mint
            "good" -> Palette.sky
            "marginal" -> Palette.lavender
            else -> Palette.blue
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Palette.surface, Palette.stroke, 16f)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        modelRow(
                            modelId,
                            "Compatível ${fitLabelForUi(fit)} · Pontuação $scoreText",
                            fitLabelForUi(fit),
                            accent
                        ),
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    )
                    if (!loaded) {
                        addView(
                            pillButton("Carregar", Palette.sky, Palette.blue) {
                                if (modelId.isNotBlank() && modelId != "unknown") {
                                    loadRecommendedModel(modelId)
                                } else {
                                    Toast.makeText(this@MainActivity, "Identificador do modelo inválido, não é possível carregar", Toast.LENGTH_SHORT).show()
                                }
                            }.apply {
                                gravity = Gravity.CENTER
                                setPadding(0, dp(4), 0, dp(4))
                            },
                            LinearLayout.LayoutParams(dp(72), dp(42))
                        )
                    }
                }
            )
            addView(space(5))
            addView(label("Memória estimada ${estimatedMemoryMb}MB · Cerca de $speedText tok/s", 12f, Palette.muted, Typeface.NORMAL))
            addView(space(2))
            addView(label("Motivo da recomendação: $reason", 12f, Palette.muted, Typeface.NORMAL).apply { maxLines = 2 })
        }
    }

    private fun fitLabelForUi(fit: String): String {
        return when (fit.lowercase(Locale.US)) {
            "perfect" -> "Excelente"
            "good" -> "Bom"
            "marginal" -> "Pode tentar"
            else -> "Observar"
        }
    }

    private fun refreshModelDownloadStatus(taskKey: String) {
        val state = providerStateByProvider[taskKey] ?: return
        if (state.item.fileName == requiredBenchmarkModelName()) {
            renderRequiredModelDownloadStatus()
        }
        val titleView = providerTitleByProvider[taskKey]
        val statusView = providerStatusByProvider[taskKey] ?: return
        val messageView = providerMessageByProvider[taskKey] ?: return
        val progressView = providerProgressByProvider[taskKey] ?: return
        val cancelView = providerCancelByProvider[taskKey]
        val tile = providerTileByProvider[taskKey]
        val bytesTotalText = if (state.totalBytes > 0) formatBytes(state.totalBytes) else "Desconhecido"
        val percentText = if (state.totalBytes > 0L) "${state.percent}%" else "Desconhecido"
        titleView?.text = "${state.item.provider} · ${state.item.shortName}"
        val localFile = availableGgufModels().firstOrNull { it.name.equals(state.item.fileName, ignoreCase = true) }
        val lifecycle = modelLifecycle(localFile, state.item.fileName, state)
        val accent = modelLifecycleAccent(lifecycle.tone)
        statusView.text = lifecycle.statusLabel
        statusView.setTextColor(accent)
        messageView.text = when {
            lifecycle.phase == ModelLifecyclePhase.LOAD_FAILED && !modelLoadFailureMessage.isNullOrBlank() -> modelLoadFailureMessage
            !state.failureMessage.isNullOrBlank() && lifecycle.phase in setOf(ModelLifecyclePhase.DOWNLOAD_FAILED, ModelLifecyclePhase.PAUSED) -> state.failureMessage
            else -> lifecycle.supportingText
        }
        cancelView?.text = lifecycle.actionLabel
        cancelView?.visibility = if (lifecycle.actionEnabled) View.VISIBLE else View.GONE
        tile?.alpha = if (lifecycle.phase == ModelLifecyclePhase.DOWNLOADING) 0.6f else 1f
        progressView.text = "Progresso ${formatBytes(state.bytesDownloaded)} / $bytesTotalText · $percentText"
        progressView.visibility = if (lifecycle.phase in setOf(
                ModelLifecyclePhase.DOWNLOADING,
                ModelLifecyclePhase.PAUSED,
                ModelLifecyclePhase.DOWNLOAD_FAILED,
            )) View.VISIBLE else View.GONE
    }

    private fun handleDownloadControl(taskKey: String) {
        val state = providerStateByProvider[taskKey] ?: return
        val localFile = availableGgufModels().firstOrNull { it.name.equals(state.item.fileName, ignoreCase = true) }
        when (modelLifecycle(localFile, state.item.fileName, state).phase) {
            ModelLifecyclePhase.LOADED, ModelLifecyclePhase.LOADING -> Unit
            ModelLifecyclePhase.DOWNLOADED, ModelLifecyclePhase.LOAD_FAILED -> localFile?.let(::ensureNotificationPermissionAndLoadModel)
            ModelLifecyclePhase.DOWNLOADING -> pauseModelDownload(taskKey)
            else -> enqueueModelDownload(state.item)
        }
    }

    private fun pauseModelDownload(taskKey: String) {
        val state = providerStateByProvider[taskKey] ?: return
        if (state.status != DownloadState.DOWNLOADING) {
            Toast.makeText(this, "Nenhuma tarefa em download", Toast.LENGTH_SHORT).show()
            return
        }
        state.cancelRequested = true
        state.status = DownloadState.PAUSED
        state.failureMessage = "Pausando..."
        refreshModelDownloadStatus(taskKey)
        activeDownloadThreads[taskKey]?.interrupt()
    }

    private fun openDownloadConnection(rawUrl: String, redirectLimit: Int = 5, startByte: Long = 0L): HttpURLConnection {
        var nextUrl = rawUrl
        repeat(redirectLimit) {
            val connection = (URL(nextUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "TuiMa-MobileCore/0.1.1 Android")
                setRequestProperty("Accept", "application/octet-stream,*/*")
                if (startByte > 0L) {
                    setRequestProperty("Range", "bytes=$startByte-")
                }
            }
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrBlank()) {
                    throw IOException("Redirecionamento sem Location")
                }
                nextUrl = URL(URL(nextUrl), location).toString()
            } else if (code in 200..299) {
                return connection
            } else {
                val error = connection.errorStream?.bufferedReader()?.readText()?.take(160)
                connection.disconnect()
                throw IOException("HTTP $code ${error ?: ""}".trim())
            }
        }
        throw IOException("Muitos redirecionamentos")
    }

    private fun readableDownloadError(error: Exception): String {
        return when (error) {
            is java.net.SocketTimeoutException -> "Tempo limite da rede esgotado"
            is java.net.UnknownHostException -> "Não foi possível resolver o host"
            is java.net.ConnectException -> "Falha na conexão"
            is IOException -> {
                val message = error.message.orEmpty()
                when {
                    message.contains("Arquivo de download muito pequeno") -> "Falha na verificação do arquivo"
                    message.contains("Redirecionamento") || message.contains("Location") -> "Link de download temporariamente indisponível"
                    message.startsWith("HTTP") -> "Serviço de download temporariamente indisponível"
                    else -> "Erro de arquivo ou rede"
                }
            }
            else -> "Operação falhou, tente novamente mais tarde"
        }
    }

    private class DownloadPausedException : IOException("download paused")

    private fun hasActiveDownload(): Boolean {
        return providerStateByProvider.values.any { it.isActive }
    }

    private fun progressEndIfNeeded() {
        if (!hasActiveDownload()) {
            progressHandler.removeCallbacks(progressPollRunnable)
        }
    }

    private fun loadRecommendedModel(modelId: String) {
        val requestedPath = availableGgufModels()
            .filter { it.nameWithoutExtension.equals(modelId, ignoreCase = true) }
            .singleOrNull()
            ?.let { canonicalModelPath(it.absolutePath) }
        if (requestedPath != null) {
            if (activeModelPath != requestedPath) {
                activeModelPath = null
                runtimeReportsLoadedModel = false
                reconcilePlaygroundRuntimeTruth(null)
            }
            pendingModelPath = requestedPath
        }
        Thread {
            try {
                val requestBody = JSONObject().apply {
                    put("model_id", modelId)
                }.toString()
                val connection = (URL("http://$serviceHost:$servicePort/mobilecore/model/load").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer local")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 1200
                    readTimeout = 120_000
                }
                connection.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.readText() ?: "{}"
                val response = JSONObject(body)
                runOnUiThread {
                    if (status in 200..299 && response.optBoolean("ok", false)) {
                        val modelName = response.optString("model", modelId)
                        Toast.makeText(this@MainActivity, "Carregado $modelName", Toast.LENGTH_SHORT).show()
                        updateStatus("Modelo carregado: $modelName")
                    } else {
                        Toast.makeText(this@MainActivity, response.optString("error", "Falhou"), Toast.LENGTH_LONG).show()
                    }
                    refreshRuntimeModelState()
                    refreshRecommendationSnapshot()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    pendingModelPath = null
                    refreshRuntimeModelState()
                    Toast.makeText(this@MainActivity, "Falha ao carregar modelo, verifique se o serviço foi iniciado", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun probeDeviceProfile(): DeviceProbeSnapshot {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val freeRam = memoryInfo.availMem / (1024 * 1024)
        val totalRam = memoryInfo.totalMem / (1024 * 1024)
        return DeviceProbeSnapshot(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            availableRamMb = freeRam,
            totalRamMb = totalRam,
            coreCount = Runtime.getRuntime().availableProcessors(),
            backend = "llama.cpp",
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        )
    }

    private data class DeviceProbeSnapshot(
        val manufacturer: String,
        val model: String,
        val availableRamMb: Long,
        val totalRamMb: Long,
        val coreCount: Int,
        val backend: String,
        val abi: String
    )

    private data class ModelHubItem(
        val provider: String,
        val shortName: String,
        val fileName: String,
        val url: String
    )

    private data class LocalApiResult(
        val status: Int,
        val body: String,
        val elapsedMs: Long
    )

    private data class ModelScopeRepoSeed(
        val owner: String,
        val name: String,
        val label: String
    ) {
        val repoId: String
            get() = "$owner/$name"
    }

    private data class ModelScopeSearchResult(
        val repos: List<ModelScopeRepoSeed>,
        val totalCount: Int
    )

    private data class ModelScopeCatalogEntry(
        val repoId: String,
        val displayTitle: String,
        val fileName: String,
        val filePath: String,
        val sizeBytes: Long,
        val quantization: String,
        val parameterLabel: String,
        val architecture: String,
        val downloads: Long,
        val recommendationReason: String = "",
        val tier: String = ""
    ) {
        val searchText: String
            get() = listOf(repoId, displayTitle, fileName, quantization, parameterLabel, architecture, recommendationReason, tier)
                .joinToString(" ")
                .lowercase(Locale.US)
    }

    private enum class DownloadState {
        IDLE,
        DOWNLOADING,
        PAUSED,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    private data class ModelDownloadState(
        var item: ModelHubItem,
        var status: DownloadState = DownloadState.IDLE,
        var destination: File? = null,
        var bytesDownloaded: Long = 0L,
        var totalBytes: Long = 0L,
        var percent: Int = 0,
        var failureMessage: String? = null,
        var transferStartedAtMs: Long = 0L,
        var transferStartedBytes: Long = 0L,
        @Volatile var cancelRequested: Boolean = false
    ) {
        val isActive: Boolean
            get() = status == DownloadState.DOWNLOADING
    }

    private enum class RecommendationPreference(
        val progress: Int,
        val queryValue: String,
        val label: String
    ) {
        SPEED(0, "speed", "Prioridade de velocidade"),
        STABILITY(1, "stability", "Prioridade de estabilidade"),
        SMALL_MODEL(2, "small", "Prioridade de modelo pequeno");

        companion object {
            fun fromProgress(progress: Int): RecommendationPreference {
                return values().firstOrNull { it.progress == progress } ?: STABILITY
            }

            fun fromQueryValue(value: String?): RecommendationPreference {
                return values().firstOrNull { it.queryValue == value } ?: STABILITY
            }
        }
    }

    private enum class AppTab {
        HOME,
        MODELS,
        PLAYGROUND,
        GALLERY,
        VISION_MODELS,
        G2D_LAB,
        VISION,
        OMNI,
        TEST,
        RESULTS,
        API,
        SETTINGS
    }

    private fun modelRow(name: String, subtitle: String, badge: String, accent: Int): View {
        return miniListCard(name, subtitle, badge, "cube", accent)
    }

    private fun buildBottomNavigation(): View {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = rounded(Palette.surface, Color.TRANSPARENT, 0f)
            elevation = dp(8).toFloat()
            setPadding(dp(7), dp(3), dp(7), dp(3))
            addView(navItem("Início", "home", AppTab.HOME), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(navItem("Benchmark", "play", AppTab.TEST), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(navItem("Resultados", "gauge", AppTab.RESULTS), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(navItem("Modelo", "cube", AppTab.MODELS), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(navItem("Meu Perfil", "person", AppTab.SETTINGS), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun navItem(title: String, icon: String, tab: AppTab): View {
        val selected = currentTab == tab ||
            (tab == AppTab.MODELS && currentTab == AppTab.PLAYGROUND) ||
            (tab == AppTab.SETTINGS && currentTab in setOf(
                AppTab.GALLERY, AppTab.VISION_MODELS, AppTab.G2D_LAB, AppTab.VISION, AppTab.OMNI, AppTab.API
            ))
        val accent = if (selected) Palette.mint else Palette.muted
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            minimumHeight = dp(62)
            setPadding(dp(2), dp(4), dp(2), dp(4))
            background = ripple(
                rounded(Color.TRANSPARENT, Color.TRANSPARENT, 7f),
                accent
            )
            isClickable = true
            isFocusable = true
            contentDescription = "$title${if (selected) ", selecionado" else ""}"
            isSelected = selected
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                setTab(tab)
            }
            addView(
                View(context).apply {
                    background = rounded(if (selected) Palette.mintDark else Color.TRANSPARENT, Color.TRANSPARENT, 2f)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                LinearLayout.LayoutParams(dp(22), dp(3)).apply { bottomMargin = dp(4) }
            )
            addView(
                FrameLayout(context).apply {
                    addView(IconBadgeView(context, icon, accent), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
                    if (tab == AppTab.TEST && benchmarkUiStateMachine.state.isRunning) {
                        addView(
                            View(context).apply {
                                background = rounded(Palette.mintDark, Color.WHITE, 5f)
                                contentDescription = "Benchmark em andamento"
                            },
                            FrameLayout.LayoutParams(dp(9), dp(9), Gravity.END or Gravity.TOP)
                        )
                    }
                },
                LinearLayout.LayoutParams(dp(24), dp(20))
            )
            addView(space(2))
            addView(label(title, 11f, accent, if (selected) Typeface.BOLD else Typeface.NORMAL).apply { maxLines = 1 })
        }
    }

    private fun sectionTitle(title: String, subtitle: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            minimumHeight = dp(34)
            addView(label(title, 15.5f, Palette.deepInk, Typeface.BOLD))
            if (subtitle.isNotBlank()) {
                addView(space(3))
                addView(label(subtitle, 11.2f, Palette.muted, Typeface.NORMAL).apply { maxLines = 2 })
            }
            contentDescription = "$title，$subtitle"
        }
    }

    private fun chipButton(text: String, selected: Boolean, onClick: () -> Unit): View {
        val accent = if (selected) Palette.mintDark else Palette.blue
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            textSize = 14f
            minimumHeight = dp(TuiMaTheme.minimumTouchTargetDp)
            maxLines = 2
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setTextColor(if (selected) Palette.mintDark else Palette.muted)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            background = ripple(
                rounded(if (selected) Palette.mintPale else Palette.surface, tint(accent, 0.30f), 7f),
                accent
            )
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this,
                11,
                14,
                1,
                TypedValue.COMPLEX_UNIT_SP,
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            }
        }
    }

    private fun compactActionButton(text: String, accent: Int, enabled: Boolean, onClick: () -> Unit): View {
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            textSize = 12f
            maxLines = 1
            minimumHeight = dp(44)
            setPadding(dp(10), 0, dp(10), 0)
            setTextColor(accent)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            background = ripple(rounded(tint(accent, 0.10f), tint(accent, 0.30f), 7f), accent)
            isClickable = enabled
            isFocusable = enabled
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.52f
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this,
                9,
                12,
                1,
                TypedValue.COMPLEX_UNIT_SP,
            )
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            }
        }
    }

    private fun roundedTextBlock(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER_VERTICAL
            textSize = 12.8f
            setTextColor(tint(Palette.ink, 0.72f))
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            setPadding(dp(14), 0, dp(14), 0)
            background = rounded(tint(Palette.blueWash, 0.65f), Palette.stroke, 14f)
            maxLines = 2
        }
    }

    private fun routeRow(method: String, path: String, caption: String, onClick: () -> Unit): View {
        val accent = if (method == "GET") Palette.mintDark else Palette.blue
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = ripple(rounded(Color.WHITE, Palette.stroke, 12f), accent)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            addView(
                chip(label(method, 12f, accent, Typeface.BOLD), tint(accent, 0.10f), accent),
                LinearLayout.LayoutParams(dp(76), dp(34)).apply { marginEnd = dp(10) }
            )
            addView(label(path, 13f, tint(Palette.ink, 0.76f), Typeface.BOLD).apply { maxLines = 1 }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(label(caption, 11.5f, Palette.muted, Typeface.NORMAL).apply { maxLines = 1 }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)).apply {
                bottomMargin = dp(8)
            }
        }
    }

    private fun copyCurlExample() {
        val command = """
            curl -s http://127.0.0.1:8080/v1/chat/completions \
              -H 'Authorization: Bearer local' \
              -H 'Content-Type: application/json' \
              -d '{"model":"local","messages":[{"role":"user","content":"Say hi from MobileCore"}],"max_tokens":48}'
        """.trimIndent()
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("MobileCore cURL", command))
        routeStatusText?.text = "Exemplo cURL copiado"
        Toast.makeText(this, "cURL copiado", Toast.LENGTH_SHORT).show()
    }

    private fun runModelsProbe() {
        ensureNotificationPermissionAndStartService()
        callLocalApi(
            path = "/v1/models",
            method = "GET",
            body = null,
            onResult = { status, body, elapsed ->
                val count = runCatching { JSONObject(body).optJSONArray("data")?.length() ?: 0 }.getOrDefault(0)
                val message = if (status in 200..299) {
                    "Lista de modelos atualizada · $count itens · ${elapsed}ms"
                } else {
                    "Lista de modelos temporariamente indisponível"
                }
                routeStatusText?.text = message
                updateStatus(message)
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun runMetricsProbe() {
        ensureNotificationPermissionAndStartService()
        callLocalApi(
            path = "/metrics",
            method = "GET",
            body = null,
            onResult = { status, body, _ ->
                val json = runCatching { JSONObject(body) }.getOrNull()
                val tps = json?.optDouble("last_decode_tokens_per_second", 0.0) ?: 0.0
                val firstToken = json?.optLong("last_first_token_ms", 0L) ?: 0L
                val message = if (status in 200..299) {
                    "推理指标已刷新 · ${"%.2f".format(Locale.US, tps)} tok/s · 首字 ${firstToken}ms"
                } else {
                    "Métricas de inferência temporariamente indisponíveis"
                }
                routeStatusText?.text = message
                updateStatus(message)
                Toast.makeText(this, "Métricas atualizadas", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun runLocalLeaderboardProbe() {
        ensureNotificationPermissionAndStartService()
        callLocalApi(
            path = "/leaderboard/local?limit=10",
            method = "GET",
            body = null,
            onResult = { status, body, _ ->
                val count = runCatching { JSONObject(body).optInt("count", 0) }.getOrDefault(0)
                val message = if (status in 200..299) "Ranking local atualizado · $count registros" else "Ranking local temporariamente indisponível"
                routeStatusText?.text = message
                updateStatus(if (status in 200..299) "Ranking local atualizado" else "Exceção na solicitação do ranking local")
                Toast.makeText(this, "Ranking local atualizado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun runSharedLeaderboardProbe() {
        ensureNotificationPermissionAndStartService()
        callLocalApi(
            path = "/leaderboard/shared",
            method = "GET",
            body = null,
            onResult = { status, body, _ ->
                val sharedStatus = runCatching { JSONObject(body).optString("status", "local_only") }.getOrDefault("local_only")
                val displayStatus = if (sharedStatus == "not_configured") "Não configurado" else sharedStatus
                val message = if (status in 200..299) "Ranking compartilhado verificado · $displayStatus" else "Ranking compartilhado temporariamente indisponível"
                routeStatusText?.text = message
                updateStatus("Status do ranking compartilhado verificado")
                Toast.makeText(this, "Status do ranking compartilhado verificado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun runSharedLeaderboardSync() {
        ensureNotificationPermissionAndStartService()
        callLocalApi(
            path = "/leaderboard/shared?limit=10",
            method = "POST",
            body = "{}",
            onResult = { status, body, _ ->
                val json = runCatching { JSONObject(body) }.getOrNull()
                val sharedStatus = json?.optString("status", "unknown") ?: "unknown"
                val uploaded = json?.optInt("uploaded", 0) ?: 0
                val displayStatus = when (sharedStatus) {
                    "ok" -> "$uploaded registros enviados"
                    "not_configured" -> "Não configurado"
                    "empty" -> "Nenhum registro local"
                    else -> sharedStatus
                }
                val message = if (status in 200..299) "Ranking compartilhado: $displayStatus" else "Falha na sincronização do ranking compartilhado"
                routeStatusText?.text = message
                updateStatus(if (sharedStatus == "ok") "Ranking compartilhado sincronizado" else "Ranking compartilhado não sincronizado")
                Toast.makeText(this, displayStatus, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun runVisionStatusProbe() {
        ensureNotificationPermissionAndStartService()
        callLocalApi(
            path = "/vision/status",
            method = "GET",
            body = null,
            onResult = { status, body, _ ->
                val json = runCatching { JSONObject(body) }.getOrNull()
                val visionStatus = json?.optString("status", "unknown") ?: "unknown"
                val message = if (status in 200..299) "Backend visual verificado · $visionStatus" else "Backend visual temporariamente indisponível"
                routeStatusText?.text = message
                updateStatus(if (visionStatus == "backend_not_installed") "Backend visual não instalado" else "Backend visual verificado")
                Toast.makeText(this, "Backend visual verificado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun runVisionModelsProbe() {
        ensureNotificationPermissionAndStartService()
        callLocalApi(
            path = "/vision/models",
            method = "GET",
            body = null,
            onResult = { status, body, _ ->
                val json = runCatching { JSONObject(body) }.getOrNull()
                val count = json?.optInt("count", 0) ?: 0
                val message = if (status in 200..299) "Modelos visuais verificados · $count itens" else "Modelos visuais temporariamente indisponíveis"
                routeStatusText?.text = message
                val models = scanVisionModelFiles()
                visionModelSummaryText?.text = visionModelSummary(models)
                visionResultText?.text = if (count > 0) {
                    "$count modelos visuais detectados.\\n${visionModelSummary(models)}"
                } else {
                    "Nenhum modelo visual importado.\\nColoque .onnx / .ort / .tflite / .mnn no diretório de modelos visuais."
                }
                updateStatus(if (count > 0) "Modelos visuais detectados" else "Nenhum modelo visual importado")
                Toast.makeText(this, if (count > 0) "$count modelos visuais detectados" else "Nenhum modelo visual importado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun runVisionDiffusionProbe() {
        ensureNotificationPermissionAndStartService()
        callLocalApi(
            path = "/vision/diffusion",
            method = "POST",
            body = JSONObject().apply {
                put("prompt", "a small mobilecore smoke image")
                put("width", 512)
                put("height", 512)
                put("steps", 4)
                put("seed", 42)
            }.toString(),
            onResult = { status, body, _ ->
                val json = runCatching { JSONObject(body) }.getOrNull()
                val diffusionStatus = json?.optString("status", "unknown") ?: "unknown"
                val message = when (diffusionStatus) {
                    "model_missing" -> "Modelo de difusão ausente"
                    "runtime_not_installed" -> "Runtime de difusão não conectado"
                    "pipeline_not_implemented" -> "Pipeline de difusão não implementado"
                    "model_load_error" -> "Falha ao carregar modelo de difusão"
                    else -> "Status de difusão: $diffusionStatus"
                }
                routeStatusText?.text = if (status in 200..299) message else "Falha na solicitação de prontidão de difusão"
                visionResultText?.text = json?.optString("message").orEmpty().ifBlank { message }
                updateStatus(message)
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun runTestChat() {
        if (isTestRunning) {
            Toast.makeText(this, "Teste em execução", Toast.LENGTH_SHORT).show()
            return
        }
        isTestRunning = true
        routeStatusText?.text = "Iniciando API local e enviando solicitação de teste..."
        ensureNotificationPermissionAndStartService()

        val requestBody = JSONObject().apply {
            put("model", findPreferredGguf()?.nameWithoutExtension ?: "local")
            put("max_tokens", 48)
            put("temperature", 0.2)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Only output this exact sentence: MobileCore runs GGUF language models locally on your phone.")
                    })
                }
            )
        }.toString()

        callLocalApi(
            path = "/v1/chat/completions",
            method = "POST",
            body = requestBody,
            retryCount = 4,
            onResult = { status, body, elapsed ->
                isTestRunning = false
                val json = runCatching { JSONObject(body) }.getOrNull()
                val answer = json
                    ?.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.takeIf { it.isNotBlank() }
                    ?: body.take(220)
                val mobilecore = json?.optJSONObject("mobilecore")
                val tps = mobilecore?.optDouble("decode_tokens_per_second", 0.0) ?: 0.0
                val firstToken = mobilecore?.optLong("first_token_ms", 0L) ?: 0L
                val total = mobilecore?.optLong("total_ms", elapsed) ?: elapsed
                routeStatusText?.text = if (status in 200..299) {
                    "试聊完成 · ${elapsed}ms\n${answer.take(160)}\n速度 ${"%.2f".format(Locale.US, tps)} tok/s · 首字 ${firstToken}ms · 总耗时 ${total}ms"
                } else {
                    "Falha na solicitação, verifique se o modelo foi carregado e tente novamente."
                }
                updateStatus(if (status in 200..299) "Teste concluído · ${elapsed}ms" else "Teste falhou")
            },
            onError = {
                isTestRunning = false
                routeStatusText?.text = "Teste falhou, verifique se o serviço local foi iniciado e o modelo pode ser carregado."
                updateStatus("Teste falhou")
            }
        )
    }

    private fun requiredBenchmarkModelName(): String {
        return runCatching { BenchmarkManifestRepository(applicationContext).load().model.fileName }
            .getOrDefault("qwen2.5-0.5b-instruct-q4_k_m.gguf")
    }

    private fun requiredBenchmarkModel(): File? {
        val requiredName = requiredBenchmarkModelName()
        return availableGgufModels().firstOrNull { it.name == requiredName }
    }

    private fun requiredBenchmarkModelLifecycle(): ModelLifecycleUiModel {
        val file = requiredBenchmarkModel()
        val item = modelHubItems.firstOrNull { it.fileName == requiredBenchmarkModelName() }
        return modelLifecycle(
            file = file,
            expectedFileName = requiredBenchmarkModelName(),
            downloadState = item?.let { providerStateByProvider[downloadTaskKey(it)] },
        )
    }

    private fun modelLifecycle(
        file: File?,
        expectedFileName: String,
        downloadState: ModelDownloadState?,
    ): ModelLifecycleUiModel {
        val downloadedFile = file?.takeIf { it.isFile && it.length() > 1024 * 1024 }
            ?: availableGgufModels().firstOrNull { it.name.equals(expectedFileName, ignoreCase = true) }
        val path = downloadedFile?.absolutePath
        return ModelLifecyclePresenter.present(
            downloaded = downloadedFile != null,
            active = path != null && activeModelPath == path,
            loading = path != null && pendingModelPath == path,
            downloadStatus = downloadState?.status?.name,
            loadFailed = path != null && modelLoadFailurePath == path,
            progressPercent = downloadState?.percent ?: 0,
        )
    }

    private fun modelLifecycleAccent(tone: ModelLifecycleTone): Int = when (tone) {
        ModelLifecycleTone.ACTIVE -> Palette.mint
        ModelLifecycleTone.READY -> Palette.sky
        ModelLifecycleTone.PROGRESS -> Palette.blue
        ModelLifecycleTone.WARNING -> Palette.amber
        ModelLifecycleTone.ERROR -> Palette.danger
        ModelLifecycleTone.NEUTRAL -> Palette.muted
    }

    private fun refreshRuntimeModelState(onComplete: (() -> Unit)? = null) {
        onComplete?.let(runtimeModelStateCompletionCallbacks::add)
        beginRuntimeModelStateRefresh()
    }

    private fun refreshRuntimeModelStateForDestructiveAction(onComplete: (Boolean) -> Unit) {
        runtimeModelStateResultCallbacks.add(onComplete)
        beginRuntimeModelStateRefresh()
    }

    private fun beginRuntimeModelStateRefresh() {
        if (runtimeModelStateRefreshInFlight) return
        runtimeModelStateRefreshInFlight = true
        callLocalApi(
            path = "/health",
            method = "GET",
            body = null,
            retryCount = 1,
            onResult = { status, body, _ ->
                if (status !in 200..299) {
                    completeRuntimeModelStateRefresh(success = false)
                    return@callLocalApi
                }
                val health = runCatching { JSONObject(body) }.getOrNull()
                if (health == null) {
                    completeRuntimeModelStateRefresh(success = false)
                    return@callLocalApi
                }
                val loaded = health.optBoolean("model_loaded", false)
                runtimeReportsLoadedModel = loaded
                val activeId = health.optString("active_model").takeIf { it.isNotBlank() && it != "null" }
                val mainArtifact = health.optJSONObject("artifacts")?.optJSONObject("main")
                val verifiedDigest = mainArtifact
                    ?.takeIf { it.optBoolean("verified", false) }
                    ?.optString("digest")
                    ?.takeIf { it.matches(Regex("^[0-9a-fA-F]{64}$")) }
                val resolvedPath = PlaygroundRuntimeTruthResolver.resolve(
                    modelLoaded = loaded,
                    activeModelId = activeId,
                    verifiedMainDigest = verifiedDigest,
                    candidates = runtimeModelCandidates(),
                )
                activeModelPath = resolvedPath
                pendingModelPath = null
                if (resolvedPath != null) {
                    modelLoadFailurePath = null
                    modelLoadFailureMessage = null
                }
                reconcilePlaygroundRuntimeTruth(resolvedPath)
                if (currentTab in setOf(AppTab.HOME, AppTab.MODELS, AppTab.PLAYGROUND, AppTab.TEST)) renderCurrentTab()
                completeRuntimeModelStateRefresh(success = true)
            },
            onError = {
                // A path-free failed health request cannot establish exact runtime identity. Keep
                // the last broadcast truth, but never promote a Playground entry from a filename.
                runtimeReportsLoadedModel = activeModelPath != null
                reconcilePlaygroundRuntimeTruth(activeModelPath)
                completeRuntimeModelStateRefresh(success = false)
            },
        )
    }

    private fun completeRuntimeModelStateRefresh(success: Boolean) {
        runtimeModelStateRefreshInFlight = false
        val callbacks = runtimeModelStateCompletionCallbacks.toList()
        runtimeModelStateCompletionCallbacks.clear()
        callbacks.forEach { it() }
        val resultCallbacks = runtimeModelStateResultCallbacks.toList()
        runtimeModelStateResultCallbacks.clear()
        resultCallbacks.forEach { it(success) }
    }

    private fun runtimeModelCandidates(): List<PlaygroundRuntimeCandidate> {
        val trustedDigestByPath = PlaygroundInstallerRegistry.values().mapNotNull { installer ->
            val file = installer.primaryModelFile() ?: return@mapNotNull null
            canonicalModelPath(file.absolutePath)?.let { it to installer.spec.primaryArtifact.sha256 }
        }.toMap()
        return availableGgufModels().mapNotNull { file ->
            val path = canonicalModelPath(file.absolutePath) ?: return@mapNotNull null
            PlaygroundRuntimeCandidate(
                publicModelId = file.nameWithoutExtension,
                path = path,
                trustedSha256 = trustedDigestByPath[path],
            )
        }
    }

    private fun canonicalModelPath(path: String): String? =
        runCatching { File(path).canonicalFile.absolutePath }.getOrNull()

    private fun reconcilePlaygroundRuntimeTruth(exactActivePath: String?) {
        PlaygroundInstallerRegistry.values().forEach { installer ->
            val exactMatch = exactActivePath?.let(installer::managesModelPath) == true
            when {
                exactMatch && installer.snapshot().verified -> installer.markLoaded()
                !exactMatch && installer.snapshot().phase in setOf(
                    PlaygroundInstallPhase.LOADING,
                    PlaygroundInstallPhase.LOADED,
                ) -> installer.markUnloaded()
            }
        }
    }

    private fun syncBenchmarkReadiness(render: Boolean = true) {
        val missingModel = requiredBenchmarkModel().let { if (it == null) requiredBenchmarkModelName() else null }
        dispatchBenchmarkUi(BenchmarkUiEvent.ReadinessChanged(missingModel), render)
    }

    private fun downloadRequiredBenchmarkModel() {
        val item = modelHubItems.firstOrNull { it.fileName == requiredBenchmarkModelName() }
        if (item == null) {
            Toast.makeText(this, "Item de download do modelo padrão temporariamente indisponível", Toast.LENGTH_SHORT).show()
            return
        }
        enqueueModelDownload(item)
    }

    private fun dispatchBenchmarkUi(event: BenchmarkUiEvent, render: Boolean = true) {
        val update = {
            val previous = benchmarkUiStateMachine.state
            val next = benchmarkUiStateMachine.dispatch(event)
            isTestRunning = next.isRunning
            if (render && previous != next && currentTab in setOf(AppTab.HOME, AppTab.TEST, AppTab.RESULTS)) {
                renderCurrentTab()
                contentRoot.announceForAccessibility("${benchmarkStateTitle(next)}。${benchmarkStateMessage(next)}")
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) update() else runOnUiThread(update)
    }

    private fun updateBenchmarkLive(
        batteryPercent: Int?,
        temperatureCelsius: Double?,
        decodeTokensPerSecond: Double?
    ) {
        val update = {
            benchmarkLiveSnapshot = BenchmarkLiveSnapshot(
                batteryPercent = batteryPercent,
                temperatureCelsius = temperatureCelsius,
                decodeTokensPerSecond = decodeTokensPerSecond ?: benchmarkLiveSnapshot.decodeTokensPerSecond,
                elapsedMs = if (benchmarkStartedAtMs > 0L) System.currentTimeMillis() - benchmarkStartedAtMs else 0L
            )
            if (currentTab == AppTab.TEST && benchmarkUiStateMachine.state.isRunning) renderCurrentTab()
        }
        if (Looper.myLooper() == Looper.getMainLooper()) update() else runOnUiThread(update)
    }

    private fun runBenchmark(profile: BenchmarkProfile) {
        if (benchmarkUiStateMachine.state.isRunning) {
            Toast.makeText(this, "Benchmark TuiMa em execução", Toast.LENGTH_SHORT).show()
            return
        }

        selectedBenchmarkProfile = profile
        benchmarkStartedAtMs = System.currentTimeMillis()
        benchmarkLiveSnapshot = BenchmarkLiveSnapshot()
        dispatchBenchmarkUi(BenchmarkUiEvent.Started(profile))

        val manifestRepository = BenchmarkManifestRepository(applicationContext)
        val manifest = runCatching { manifestRepository.load() }.getOrElse {
            dispatchBenchmarkUi(
                BenchmarkUiEvent.Failed(
                    profile,
                    BenchmarkFailureKind.MODEL_INVALID,
                    "Falha na verificação da lista de benchmark, compilação atual não pontuável."
                )
            )
            return
        }
        val model = availableGgufModels().firstOrNull { it.name == manifest.model.fileName }
        if (model == null) {
            dispatchBenchmarkUi(BenchmarkUiEvent.Failed(profile, BenchmarkFailureKind.MODEL_INVALID, "Modelo padrão ${manifest.model.fileName} ausente."))
            dispatchBenchmarkUi(BenchmarkUiEvent.ReadinessChanged(manifest.model.fileName))
            updateStatus("Modelo padrão TuiMa ausente")
            return
        }

        withNotificationPermission {
            isTestRunning = true
            benchmarkCancellationRequested = false
            val deviceProfile = probeDeviceProfile()
            val spec = BenchmarkSpecV2.forProfile(profile, threads = deviceProfile.coreCount.coerceAtMost(6))
            val galleryRelease = releaseGallerySearchRuntime(
                "Benchmark precisa carregar GGUF, sessão CLIP liberada; índice de fotos preservado.",
            )
            startServiceInForeground()

            Thread {
                try {
                    if (galleryRelease != null &&
                        runCatching { galleryRelease.get(30L, TimeUnit.SECONDS) }.isFailure
                    ) {
                        throw BenchmarkRunException(
                            BenchmarkFailureKind.RUNTIME_UNAVAILABLE,
                            "Não foi possível liberar memória CLIP com segurança, benchmark cancelado.",
                        )
                    }
                    val health = localApiRequestBlocking(
                        path = "/health",
                        method = "GET",
                        body = null,
                        retryCount = 8,
                        readTimeoutMs = 2500
                    )

                    val prompt = manifestRepository.loadPrompt(manifest).trimEnd()
                    val telemetry = AndroidBenchmarkTelemetry(applicationContext)
                    val initialTelemetry = telemetry.sample()
                    updateBenchmarkLive(
                        batteryPercent = initialTelemetry.batteryPercent,
                        temperatureCelsius = initialTelemetry.batteryTemperatureCelsius,
                        decodeTokensPerSecond = null
                    )
                    val modelHashMatches = BenchmarkDigestVerifier.matches(model, manifest.model.sha256)
                    val preflight = BenchmarkPreflight.evaluate(
                        BenchmarkPreflightSnapshot(
                            batteryPercent = initialTelemetry.batteryPercent,
                            charging = initialTelemetry.charging,
                            thermalStatus = initialTelemetry.thermalStatus,
                            freeStorageMb = initialTelemetry.freeStorageMb,
                            modelSizeMb = (model.length() + BYTES_PER_MB - 1L) / BYTES_PER_MB,
                            modelHashMatches = modelHashMatches,
                            promptHashMatches = true,
                            apiHealthy = health.status in 200..299 &&
                                runCatching { JSONObject(health.body).optString("status") == "ok" }.getOrDefault(false),
                            benchmarkRunning = false
                        )
                    )
                    if (preflight is BenchmarkPreflightResult.Blocked) {
                        dispatchBenchmarkUi(BenchmarkUiEvent.PreflightBlocked(profile, preflight.reasons))
                        throw BenchmarkRunException(
                            BenchmarkFailureKind.PREFLIGHT_BLOCKED,
                            "跑分门禁：${preflight.reasons.joinToString("、", transform = ::preflightReasonLabel)}"
                        )
                    }

                    dispatchBenchmarkUi(BenchmarkUiEvent.ModelLoading(profile, model.name))
                    val loadBody = JSONObject().apply {
                        put("path", model.absolutePath)
                        put("context_length", spec.contextLength)
                        put("threads", spec.threads)
                        put("gpu_layers", 0)
                    }.toString()
                    val loadResult = localApiRequestBlocking(
                        path = "/mobilecore/model/load",
                        method = "POST",
                        body = loadBody,
                        retryCount = 2,
                        readTimeoutMs = 120000
                    )
                    if (loadResult.status !in 200..299) {
                        throw BenchmarkRunException(
                            BenchmarkFailureKind.MODEL_INVALID,
                            "Falha ao carregar modelo ${loadResult.status}: ${loadResult.body.take(180)}"
                        )
                    }
                    val loadJson = JSONObject(loadResult.body)
                    if (!loadJson.optBoolean("ok", false)) {
                        throw BenchmarkRunException(BenchmarkFailureKind.MODEL_INVALID, "Falha ao carregar modelo padrão")
                    }
                    activeModelPath = model.absolutePath
                    runtimeReportsLoadedModel = true
                    pendingModelPath = null
                    modelLoadFailurePath = null
                    modelLoadFailureMessage = null
                    val loadMs = loadJson.optLong("load_time_ms", loadResult.elapsedMs)

                    val chatBody = JSONObject().apply {
                        put("model", model.nameWithoutExtension)
                        put("max_tokens", spec.profile.outputTokens)
                        put("temperature", spec.temperature.toDouble())
                        put(
                            "messages",
                            JSONArray().apply {
                                put(JSONObject().apply {
                                    put("role", "user")
                                    put("content", prompt)
                                })
                            }
                        )
                    }.toString()

                    repeat(spec.profile.warmupRuns) { index ->
                        throwIfBenchmarkCancelled()
                        dispatchBenchmarkUi(BenchmarkUiEvent.WarmupProgress(profile, index + 1, spec.profile.warmupRuns))
                        executeBenchmarkChat(chatBody, spec.timeoutMs)
                        throwIfBenchmarkCancelled()
                    }

                    val samples = ArrayList<BenchmarkRunSample>(spec.profile.measuredRuns)
                    repeat(spec.profile.measuredRuns) { index ->
                        throwIfBenchmarkCancelled()
                        dispatchBenchmarkUi(BenchmarkUiEvent.MeasurementProgress(profile, index + 1, spec.profile.measuredRuns))
                        val before = telemetry.sample()
                        val chat = executeBenchmarkChat(chatBody, spec.timeoutMs)
                        throwIfBenchmarkCancelled()
                        val after = telemetry.sample()
                        val usage = chat.optJSONObject("usage") ?: JSONObject()
                        val metrics = chat.optJSONObject("mobilecore") ?: JSONObject()
                        val promptTokens = usage.optInt("prompt_tokens", 0)
                        val generatedTokens = usage.optInt("completion_tokens", 0)
                        val promptEvalMs = metrics.optLong("prompt_eval_ms", 0L)
                        val decodeTps = metrics.optDouble("decode_tokens_per_second", 0.0)
                        updateBenchmarkLive(
                            batteryPercent = after.batteryPercent,
                            temperatureCelsius = after.batteryTemperatureCelsius,
                            decodeTokensPerSecond = decodeTps.takeIf { it > 0.0 }
                        )
                        val thermalPeak = listOf(before.thermalStatus, after.thermalStatus).maxBy { it.ordinal }
                        val temperaturePeak = listOfNotNull(
                            before.batteryTemperatureCelsius,
                            after.batteryTemperatureCelsius
                        ).maxOrNull()
                        samples += BenchmarkRunSample(
                            runIndex = index,
                            promptTokens = promptTokens,
                            generatedTokens = generatedTokens,
                            loadTimeMs = loadMs,
                            promptEvalMs = promptEvalMs,
                            firstTokenMs = metrics.optLong("first_token_ms", 0L),
                            decodeMs = metrics.optLong("decode_ms", 0L),
                            totalMs = metrics.optLong("total_ms", 0L),
                            prefillTokensPerSecond = if (promptEvalMs > 0L) {
                                promptTokens * 1000.0 / promptEvalMs
                            } else {
                                0.0
                            },
                            decodeTokensPerSecond = decodeTps,
                            memoryPeakMb = metrics.optLong("memory_peak_mb", 0L),
                            availableMemoryBeforeMb = before.availableMemoryMb,
                            batteryPercentStart = before.batteryPercent,
                            batteryPercentEnd = after.batteryPercent,
                            batteryTemperatureStartCelsius = before.batteryTemperatureCelsius,
                            batteryTemperaturePeakCelsius = temperaturePeak,
                            batteryTemperatureEndCelsius = after.batteryTemperatureCelsius,
                            thermalStart = before.thermalStatus,
                            thermalPeak = thermalPeak,
                            thermalEnd = after.thermalStatus,
                            chargingStart = before.charging,
                            chargingEnd = after.charging,
                            completed = generatedTokens > 0 && decodeTps > 0.0,
                            failureKind = if (generatedTokens > 0 && decodeTps > 0.0) null else BenchmarkFailureKind.METRICS_INCOMPLETE
                        )
                        if (index < spec.profile.measuredRuns - 1 && spec.profile.cooldownMs > 0L) {
                            var remainingMs = spec.profile.cooldownMs
                            while (remainingMs > 0L) {
                                dispatchBenchmarkUi(BenchmarkUiEvent.Cooldown(profile, (remainingMs + 999L) / 1000L))
                                val waitMs = remainingMs.coerceAtMost(1_000L)
                                Thread.sleep(waitMs)
                                remainingMs -= waitMs
                                throwIfBenchmarkCancelled()
                            }
                        }
                    }

                    val summary = BenchmarkAggregator.aggregate(spec, samples)
                    val score = BenchmarkScoreEngine.score(summary)
                    val report = BenchmarkReport(
                        runId = "run-${UUID.randomUUID()}",
                        createdAtMs = System.currentTimeMillis(),
                        manifestSha256 = BenchmarkManifestRepository.EXPECTED_MANIFEST_SHA256,
                        device = BenchmarkDeviceIdentity(
                            manufacturer = deviceProfile.manufacturer,
                            model = deviceProfile.model,
                            device = Build.DEVICE,
                            androidRelease = Build.VERSION.RELEASE,
                            apiLevel = Build.VERSION.SDK_INT,
                            abi = deviceProfile.abi,
                            totalMemoryMb = deviceProfile.totalRamMb,
                            coreCount = deviceProfile.coreCount
                        ),
                        summary = summary,
                        score = score
                    )
                    BenchmarkReportStore(applicationContext).record(report)

                    runOnUiThread {
                        if (score == null) {
                            dispatchBenchmarkUi(
                                BenchmarkUiEvent.Failed(
                                    profile,
                                    summary.failureKind ?: BenchmarkFailureKind.METRICS_INCOMPLETE,
                                    "Benchmark inválido, concluído ${summary.completedRuns}/${summary.measuredRuns} medições."
                                )
                            )
                        } else {
                            dispatchBenchmarkUi(BenchmarkUiEvent.Finished(profile, score.headlineScore, score.canonicalScore))
                            transitionToResultsAfterBenchmark()
                        }
                        routeStatusText?.text = if (score != null) "Benchmark concluído · ${score.headlineScore} TuiMa" else "Benchmark inválido"
                        updateStatus(if (score != null) "TuiMa ${score.headlineScore}" else "Benchmark inválido")
                        refreshRecommendationSnapshot()
                    }
                } catch (e: Throwable) {
                    val failureKind = when (e) {
                        is BenchmarkRunException -> e.kind
                        is SocketTimeoutException -> BenchmarkFailureKind.TIMEOUT
                        is OutOfMemoryError -> BenchmarkFailureKind.OOM
                        else -> BenchmarkFailureKind.RUNTIME_UNAVAILABLE
                    }
                    val failedSummary = BenchmarkAggregator.invalid(spec, failureKind)
                    runCatching {
                        BenchmarkReportStore(applicationContext).record(
                            BenchmarkReport(
                                runId = "run-${UUID.randomUUID()}",
                                createdAtMs = System.currentTimeMillis(),
                                manifestSha256 = BenchmarkManifestRepository.EXPECTED_MANIFEST_SHA256,
                                device = BenchmarkDeviceIdentity(
                                    manufacturer = deviceProfile.manufacturer,
                                    model = deviceProfile.model,
                                    device = Build.DEVICE,
                                    androidRelease = Build.VERSION.RELEASE,
                                    apiLevel = Build.VERSION.SDK_INT,
                                    abi = deviceProfile.abi,
                                    totalMemoryMb = deviceProfile.totalRamMb,
                                    coreCount = deviceProfile.coreCount
                                ),
                                summary = failedSummary,
                                score = null
                            )
                        )
                    }
                    runOnUiThread {
                        when {
                            failureKind == BenchmarkFailureKind.CANCELLED -> dispatchBenchmarkUi(BenchmarkUiEvent.Cancelled)
                            failureKind == BenchmarkFailureKind.PREFLIGHT_BLOCKED && benchmarkUiStateMachine.state is BenchmarkUiState.Blocked -> Unit
                            else -> dispatchBenchmarkUi(
                                BenchmarkUiEvent.Failed(
                                    profile,
                                    failureKind,
                                    e.message?.takeIf { it.isNotBlank() }
                                        ?: "Benchmark TuiMa falhou, tente novamente mais tarde."
                                )
                            )
                        }
                        routeStatusText?.text = "Benchmark TuiMa falhou"
                        updateStatus("Benchmark TuiMa falhou")
                    }
                }
            }.start()
        }
    }

    private fun executeBenchmarkChat(body: String, timeoutMs: Long): JSONObject {
        val result = try {
            localApiRequestBlocking(
                path = "/v1/chat/completions",
                method = "POST",
                body = body,
                retryCount = 1,
                readTimeoutMs = timeoutMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            )
        } catch (e: SocketTimeoutException) {
            RuntimeBridge.cancel()
            throw BenchmarkRunException(BenchmarkFailureKind.TIMEOUT, "Tempo limite de inferência", e)
        }
        if (result.status !in 200..299) {
            throw BenchmarkRunException(
                BenchmarkFailureKind.RUNTIME_UNAVAILABLE,
                "Falha na solicitação de inferência ${result.status}: ${result.body.take(160)}"
            )
        }
        return JSONObject(result.body)
    }

    private fun transitionToResultsAfterBenchmark() {
        contentRoot.postDelayed({
            if (currentTab != AppTab.TEST || benchmarkUiStateMachine.state !is BenchmarkUiState.Completed) return@postDelayed
            contentRoot.animate()
                .alpha(0f)
                .setDuration(180L)
                .withEndAction {
                    selectedResultRunId = null
                    comparisonBaselineRunId = null
                    selectingComparisonBaseline = false
                    setTab(AppTab.RESULTS)
                    contentRoot.alpha = 0f
                    contentRoot.animate().alpha(1f).setDuration(240L).start()
                }
                .start()
        }, 700L)
    }

    private fun cancelBenchmark() {
        if (!benchmarkUiStateMachine.state.isRunning) {
            Toast.makeText(this, "Nenhuma tarefa de benchmark em andamento", Toast.LENGTH_SHORT).show()
            return
        }
        benchmarkCancellationRequested = true
        RuntimeBridge.cancel()
        dispatchBenchmarkUi(BenchmarkUiEvent.CancelRequested)
    }

    private fun confirmCancelBenchmark() {
        if (!benchmarkUiStateMachine.state.isRunning) return
        AlertDialog.Builder(this)
            .setTitle("Cancelar este benchmark?")
            .setMessage("Este teste não gerará pontuação.")
            .setNegativeButton("Continuar Benchmark", null)
            .setPositiveButton("Confirmar Cancelamento") { _, _ -> cancelBenchmark() }
            .show()
    }

    private fun throwIfBenchmarkCancelled() {
        if (benchmarkCancellationRequested) {
            throw BenchmarkRunException(BenchmarkFailureKind.CANCELLED, "Benchmark cancelado")
        }
    }

    private fun benchmarkProfileName(profile: BenchmarkProfile): String = when (profile) {
        BenchmarkProfile.QUICK -> "Modo rápido"
        BenchmarkProfile.STANDARD -> "Modo padrão"
        BenchmarkProfile.STRESS -> "Modo estresse"
    }

    private fun preflightReasonLabel(reason: BenchmarkPreflightReason): String = when (reason) {
        BenchmarkPreflightReason.BATTERY_TOO_LOW -> "Bateria abaixo de 30% ou ilegível"
        BenchmarkPreflightReason.DEVICE_CHARGING -> "Desconecte o carregador"
        BenchmarkPreflightReason.THERMAL_TOO_HIGH -> "Temperatura do dispositivo muito alta"
        BenchmarkPreflightReason.STORAGE_TOO_LOW -> "Espaço de armazenamento insuficiente"
        BenchmarkPreflightReason.MODEL_INVALID -> "Verificação do modelo padrão falhou"
        BenchmarkPreflightReason.PROMPT_INVALID -> "Verificação do prompt falhou"
        BenchmarkPreflightReason.RUNTIME_UNAVAILABLE -> "Serviço de inferência local indisponível"
        BenchmarkPreflightReason.BENCHMARK_ALREADY_RUNNING -> "Já existe tarefa de benchmark"
    }

    private fun preflightRecoveryLabel(reason: BenchmarkPreflightReason): String = when (reason) {
        BenchmarkPreflightReason.BATTERY_TOO_LOW -> "Carregue a bateria acima de 30%, depois desconecte o carregador"
        BenchmarkPreflightReason.DEVICE_CHARGING -> "Desconecte o carregador, aguarde o estado da bateria estabilizar"
        BenchmarkPreflightReason.THERMAL_TOO_HIGH -> "Trave a tela e aguarde alguns minutos para o dispositivo resfriar"
        BenchmarkPreflightReason.STORAGE_TOO_LOW -> "Libere pelo menos 512 MB mais o tamanho do modelo padrão"
        BenchmarkPreflightReason.MODEL_INVALID -> "Baixe novamente o modelo padrão, garantindo que o arquivo esteja completo"
        BenchmarkPreflightReason.PROMPT_INVALID -> "Recursos de teste da compilação atual anômalos, reinstale"
        BenchmarkPreflightReason.RUNTIME_UNAVAILABLE -> "Feche apps que estejam usando recursos e verifique novamente"
        BenchmarkPreflightReason.BENCHMARK_ALREADY_RUNNING -> "Aguarde o benchmark atual terminar ou cancele primeiro"
    }

    private fun localApiRequestBlocking(
        path: String,
        method: String,
        body: String?,
        retryCount: Int,
        readTimeoutMs: Int
    ): LocalApiResult {
        var lastError: Exception? = null
        repeat(retryCount.coerceAtLeast(1)) { attempt ->
            try {
                if (attempt > 0) Thread.sleep(450L)
                val started = System.currentTimeMillis()
                val connection = (URL("http://$serviceHost:$servicePort$path").openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    setRequestProperty("Authorization", "Bearer local")
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 1800
                    readTimeout = readTimeoutMs
                    if (body != null) doOutput = true
                }
                try {
                    if (body != null) {
                        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    }
                    val status = connection.responseCode
                    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                    val responseBody = stream?.bufferedReader()?.readText() ?: ""
                    return LocalApiResult(status, responseBody, System.currentTimeMillis() - started)
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Falha na solicitação da interface local")
    }

    private fun callLocalApi(
        path: String,
        method: String,
        body: String?,
        retryCount: Int = 2,
        readTimeoutMs: Int = 8000,
        onResult: (Int, String, Long) -> Unit,
        onError: (Exception) -> Unit = {
            runOnUiThread {
                routeStatusText?.text = "Falha na solicitação da interface local"
                Toast.makeText(this, "Falha na solicitação API", Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Thread {
            var lastError: Exception? = null
            repeat(retryCount) { attempt ->
                try {
                    if (attempt > 0) Thread.sleep(450L)
                    val started = System.currentTimeMillis()
                    val connection = (URL("http://$serviceHost:$servicePort$path").openConnection() as HttpURLConnection).apply {
                        requestMethod = method
                        setRequestProperty("Authorization", "Bearer local")
                        setRequestProperty("Content-Type", "application/json")
                        connectTimeout = 1600
                        readTimeout = readTimeoutMs
                        if (body != null) {
                            doOutput = true
                        }
                    }
                    if (body != null) {
                        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    }
                    val status = connection.responseCode
                    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                    val responseBody = stream?.bufferedReader()?.readText() ?: ""
                    val elapsed = System.currentTimeMillis() - started
                    runOnUiThread { onResult(status, responseBody, elapsed) }
                    return@Thread
                } catch (e: Exception) {
                    lastError = e
                }
            }
            runOnUiThread { onError(lastError ?: IOException("Falha na solicitação da interface local")) }
        }.start()
    }

    private fun handleOmniLifecycleAction(action: OmniLifecycleAction) {
        when (action) {
            OmniLifecycleAction.START_SERVICE -> withNotificationPermission {
                startServiceInForeground()
                progressHandler.postDelayed(omniStatusPollRunnable, 650L)
            }
            OmniLifecycleAction.REFRESH -> refreshOmniLifecycleStatus()
            OmniLifecycleAction.INSTALL -> showOmniInstallConsentDialog()
            OmniLifecycleAction.CANCEL -> performOmniLifecycleRequest(
                actionLabel = "Cancelar instalação",
                path = "/mobilecore/omni/cancel",
                body = "{}",
                readTimeoutMs = 45_000,
            )
            OmniLifecycleAction.VERIFY -> performOmniLifecycleRequest(
                actionLabel = "Verificar artifact",
                path = "/mobilecore/omni/verify",
                body = "{}",
                readTimeoutMs = 300_000,
            )
            OmniLifecycleAction.LOAD -> {
                val loadOmni = {
                    performOmniLifecycleRequest(
                        actionLabel = "Carregar modelo multimodal",
                        path = "/mobilecore/omni/load",
                        body = JSONObject()
                            .put("context_length", 4096)
                            .put("threads", 4)
                            .toString(),
                        readTimeoutMs = 180_000,
                    )
                }
                val release = releaseGallerySearchRuntime(
                    "Carregando Omni GGUF, sessão CLIP liberada para evitar sobreposição de memória.",
                )
                if (release == null) {
                    loadOmni()
                } else {
                    Thread {
                        val released = runCatching { release.get(30L, TimeUnit.SECONDS) }.isSuccess
                        runOnUiThread {
                            if (released) loadOmni() else Toast.makeText(
                                this,
                                "Não foi possível liberar CLIP com segurança, carregamento Omni cancelado",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }.start()
                }
            }
            OmniLifecycleAction.UNINSTALL -> showOmniUninstallDialog()
            OmniLifecycleAction.OPEN_SOURCE -> openOmniPinnedSource()
        }
    }

    private fun refreshOmniLifecycleStatus() {
        if (omniStatusRefreshInFlight) return
        omniStatusRefreshInFlight = true
        progressHandler.removeCallbacks(omniStatusPollRunnable)
        callLocalApi(
            path = "/mobilecore/omni/status",
            method = "GET",
            body = null,
            retryCount = 1,
            onResult = { status, body, _ ->
                omniStatusRefreshInFlight = false
                omniLifecycleSnapshot = if (status in 200..299) {
                    runCatching { OmniLifecyclePresenter.parseStatus(body) }.getOrElse {
                        omniLifecycleSnapshot.copy(
                            serviceReachable = true,
                            failureCode = "request_failed",
                            failureMessage = "Resposta de status não pode ser analisada",
                        )
                    }
                } else {
                    OmniLifecyclePresenter.withApiFailure(omniLifecycleSnapshot, body)
                }
                renderOmniLifecycleIfVisible()
                scheduleOmniPollIfBusy()
            },
            onError = {
                omniStatusRefreshInFlight = false
                omniLifecycleSnapshot = OmniLifecycleSnapshot()
                renderOmniLifecycleIfVisible()
            },
        )
    }

    private fun performOmniLifecycleRequest(
        actionLabel: String,
        path: String,
        body: String,
        readTimeoutMs: Int = 15_000,
    ) {
        updateStatus("Executando $actionLabel")
        callLocalApi(
            path = path,
            method = "POST",
            body = body,
            retryCount = 1,
            readTimeoutMs = readTimeoutMs,
            onResult = { status, responseBody, _ ->
                omniLifecycleSnapshot = if (status in 200..299) {
                    runCatching { OmniLifecyclePresenter.parseStatus(responseBody) }.getOrElse {
                        omniLifecycleSnapshot.copy(
                            serviceReachable = true,
                            failureCode = "request_failed",
                            failureMessage = "Operação concluída, mas resposta de status não pode ser analisada",
                        )
                    }
                } else {
                    OmniLifecyclePresenter.withApiFailure(omniLifecycleSnapshot, responseBody)
                }
                updateStatus(if (status in 200..299) "$actionLabel enviado" else "$actionLabel não concluído")
                Toast.makeText(
                    this,
                    if (status in 200..299) "$actionLabel enviado" else OmniLifecyclePresenter.present(omniLifecycleSnapshot).statusDetail,
                    Toast.LENGTH_LONG,
                ).show()
                renderOmniLifecycleIfVisible()
                scheduleOmniPollIfBusy()
            },
            onError = {
                omniLifecycleSnapshot = omniLifecycleSnapshot.copy(
                    serviceReachable = false,
                    failureCode = null,
                    failureMessage = null,
                )
                updateStatus("$actionLabel falhou: serviço local inacessível")
                Toast.makeText(this, "$actionLabel falhou, serviço local inacessível", Toast.LENGTH_LONG).show()
                renderOmniLifecycleIfVisible()
            },
        )
    }

    private fun scheduleOmniPollIfBusy() {
        progressHandler.removeCallbacks(omniStatusPollRunnable)
        if (OmniLifecyclePresenter.present(omniLifecycleSnapshot).isBusy) {
            progressHandler.postDelayed(omniStatusPollRunnable, 1_000L)
        }
    }

    private fun renderOmniLifecycleIfVisible() {
        if (currentTab == AppTab.OMNI) renderCurrentTab()
    }

    private fun showOmniInstallConsentDialog() {
        val model = OmniLifecyclePresenter.present(omniLifecycleSnapshot)
        val installAllowed = omniLifecycleSnapshot.resourcesSufficient && omniLifecycleSnapshot.wifiConnected
        if (!installAllowed) {
            Toast.makeText(this, "Condições do dispositivo não aprovadas, verifique novamente", Toast.LENGTH_LONG).show()
            refreshOmniLifecycleStatus()
            return
        }
        val consent = CheckBox(this).apply {
            text = "Li as instruções de fonte e licença, e concordo em baixar cerca de 3,39 GiB apenas via Wi-Fi para o diretório privado do MobileCore."
            setTextColor(Palette.ink)
            textSize = 13f
            setPadding(dp(4), dp(6), dp(4), dp(6))
            contentDescription = "Concordar explicitamente com o download do modelo Omni"
        }
        val message = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
            addView(label(
                "O publicador é ggml-org, não o GGUF oficial do Qwen. A licença é ${omniLifecycleSnapshot.licenseId}, status 'declaração de fonte, sem revisão legal'. O download inclui o modelo principal Q4_K_M e Q8_0 mmproj; cada arquivo deve passar na verificação de bytes fixos e SHA-256.",
                13f,
                Palette.ink,
                Typeface.NORMAL,
            ).apply { setLineSpacing(0f, 1.16f) })
            addView(space(10))
            addView(label(
                "Memória: ${model.memoryLabel}\\nArmazenamento: ${model.storageLabel}\\nRede: ${model.wifiLabel}",
                12f,
                Palette.muted,
                Typeface.NORMAL,
            ))
            addView(space(10))
            addView(consent)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Instalar modelo multimodal local?")
            .setView(message)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Ver fonte", null)
            .setPositiveButton("Concordar e iniciar", null)
            .create()
        dialog.setOnShowListener {
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.isEnabled = false
            consent.setOnCheckedChangeListener { _, checked -> positive.isEnabled = checked }
            positive.setOnClickListener {
                if (!consent.isChecked) return@setOnClickListener
                dialog.dismiss()
                performOmniLifecycleRequest(
                    actionLabel = "Instalação Omni",
                    path = "/mobilecore/omni/install",
                    body = JSONObject()
                        .put("explicit_consent", true)
                        .put("accepted_license_id", omniLifecycleSnapshot.licenseId)
                        .put("wifi_only", true)
                        .toString(),
                    readTimeoutMs = 20_000,
                )
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { openOmniPinnedSource() }
        }
        dialog.show()
    }

    private fun showOmniUninstallDialog() {
        AlertDialog.Builder(this)
            .setTitle("Desinstalar modelo multimodal local?")
            .setMessage("Primeiro desinstalará o runtime, depois excluirá este conjunto de modelo principal fixo, mmproj, arquivos temporários e registros de verificação. Conversas e evidências do MobileCode não serão modificadas.")
            .setNegativeButton("Manter", null)
            .setPositiveButton("Desinstalar") { _, _ ->
                performOmniLifecycleRequest(
                    actionLabel = "Desinstalar modelo multimodal",
                    path = "/mobilecore/omni/uninstall",
                    body = "{}",
                    readTimeoutMs = 60_000,
                )
            }
            .show()
    }

    private fun openOmniPinnedSource() {
        val revision = omniLifecycleSnapshot.revision
            .takeIf { it.matches(Regex("[0-9a-f]{40}")) }
            ?: "75f1b73b657a50f5092502799457ccb4a4a1f9df"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
            "https://huggingface.co/ggml-org/Qwen2.5-Omni-3B-GGUF/tree/$revision",
        )))
    }

    private fun ensureNotificationPermissionAndStartService() {
        withNotificationPermission {
            startServiceInForeground()
        }
    }

    private fun ensureNotificationPermissionAndLoadFirstModel() {
        withNotificationPermission {
            loadFirstModel()
        }
    }

    private fun withNotificationPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pendingAfterNotificationPermission = action
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationPermissionRequestCode)
            return
        }
        action()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == galleryPermissionRequestCode) {
            if (hasGalleryImageAccess()) {
                startGalleryIndex()
            } else {
                handleGalleryAccessRevoked(showToast = false)
                Toast.makeText(this, "Permissão de acesso a fotos não concedida, nenhum índice criado", Toast.LENGTH_LONG).show()
            }
            return
        }
        if (requestCode != notificationPermissionRequestCode) return
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val action = pendingAfterNotificationPermission
            pendingAfterNotificationPermission = null
            action?.invoke() ?: startServiceInForeground()
        } else {
            val wasWaitingForBenchmark = benchmarkUiStateMachine.state is BenchmarkUiState.Checking
            val waitingProfile = benchmarkUiStateMachine.state.profile
            pendingAfterNotificationPermission = null
            updateStatus("Permissão de notificação não concedida, não foi possível iniciar serviço em primeiro plano")
            if (wasWaitingForBenchmark) {
                dispatchBenchmarkUi(
                    BenchmarkUiEvent.Failed(
                        waitingProfile,
                        BenchmarkFailureKind.RUNTIME_UNAVAILABLE,
                        "Permissão de notificação necessária para manter o serviço local em execução durante o benchmark."
                    )
                )
            }
            Toast.makeText(this, "Permita a permissão de notificação antes de iniciar o serviço", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("startActivityForResult keeps this skeleton dependency-light.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            importModelRequestCode -> importGguf(uri)
            pickVisionImageRequestCode -> handleVisionImage(uri)
            importVisionModelRequestCode -> handleVisionModelFile(uri)
        }
    }

    private fun startServiceInForeground() {
        val intent = Intent(this, MobileCoreService::class.java)
        // Every entry point here is user-initiated from the visible activity.
        // startService avoids creating another foreground-start timeout when
        // the already-promoted local API service receives a refresh request.
        // MobileCoreService promotes itself in onCreate/onStartCommand.
        startService(intent)
        updateStatus("Serviço local iniciado")
        refreshRecommendationSnapshot()
    }

    private fun stopMobileCoreService() {
        val intent = Intent(this, MobileCoreService::class.java)
        stopService(intent)
        runtimeReportsLoadedModel = false
        activeModelPath = null
        pendingModelPath = null
        reconcilePlaygroundRuntimeTruth(null)
        updateStatus("Modelo descarregado")
        renderRecommendationPlaceholder("Serviço parado, reinicie a API e atualize as recomendações.")
    }

    private fun openGgufPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf("application/octet-stream", "application/x-gguf", "application/gguf")
            )
        }
        startActivityForResult(intent, importModelRequestCode)
    }

    private fun openVisionImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, pickVisionImageRequestCode)
    }

    private fun openVisionModelPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "application/octet-stream",
                    "application/json",
                    "text/json",
                    "text/plain",
                    "application/x-tflite",
                    "application/x-onnx"
                )
            )
        }
        startActivityForResult(intent, importVisionModelRequestCode)
    }

    private fun handleVisionImage(uri: Uri) {
        selectedVisionImageUri = uri
        val displayName = resolveDisplayName(uri) ?: "selected-image-${System.currentTimeMillis()}"
        val safeName = sanitizeVisionImageFileName(displayName)
        val destination = File(internalVisionImageDir(), safeName)
        selectedVisionImageName = safeName
        selectedVisionImagePath = null
        visionImageText?.text = "Importando $safeName..."
        visionResultText?.text = "Copiando imagem para o workspace visual local..."
        updateStatus("Importando imagem")

        Thread {
            try {
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Não foi possível abrir a imagem selecionada" }
                    FileOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                }
                runOnUiThread {
                    selectedVisionImagePath = destination.absolutePath
                    visionImageText?.text = "${destination.name} · ${formatBytes(destination.length())}"
                    visionResultText?.text = "Imagem importada. Clique em iniciar OCR ou classificação para verificação local."
                    updateStatus("Imagem importada")
                }
            } catch (e: Exception) {
                if (destination.exists()) destination.delete()
                runOnUiThread {
                    selectedVisionImageName = null
                    selectedVisionImagePath = null
                    visionImageText?.text = "Falha na importação da imagem"
                    visionResultText?.text = "Falha na importação da imagem. Tente outra imagem local."
                    updateStatus("Falha na importação da imagem")
                    Toast.makeText(this, "Falha na importação da imagem", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun handleVisionModelFile(uri: Uri) {
        val displayName = resolveDisplayName(uri) ?: "vision-model-${System.currentTimeMillis()}"
        val safeName = sanitizeVisionModelFileName(displayName)
        val destination = File(internalVisionModelDir(), safeName)
        val temporary = File(internalVisionModelDir(), ".$safeName.${UUID.randomUUID()}.part")
        visionResultText?.text = "Importando modelo visual: $safeName"
        updateStatus("Importando modelo visual")

        Thread {
            try {
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Não foi possível abrir o modelo visual selecionado" }
                    FileOutputStream(temporary).use { output ->
                        input.copyTo(output)
                        output.fd.sync()
                    }
                }
                require(temporary.length() > 0L) { "Arquivo do modelo visual vazio" }
                Files.move(
                    temporary.toPath(),
                    destination.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
                runOnUiThread {
                    if (isGalleryClipArtifactName(destination.name)) {
                        invalidateGallerySearchRuntime("Arquivo do modelo CLIP atualizado, prepare novamente e crie o índice de fotos.")
                    }
                    visionModelSummaryText?.text = visionModelSummary()
                    visionResultText?.text = "${destination.name} importado · ${formatBytes(destination.length())}\\nClique para verificar o modelo e atualizar o status do backend."
                    Toast.makeText(this, "Modelo visual importado", Toast.LENGTH_SHORT).show()
                    updateStatus("Modelo visual importado")
                    if (currentTab == AppTab.VISION || currentTab == AppTab.VISION_MODELS) renderCurrentTab()
                }
            } catch (e: Exception) {
                temporary.delete()
                runOnUiThread {
                    visionResultText?.text = "Falha na importação do modelo visual. Suporta ONNX / ORT / TFLite / MNN / JSON / TXT / GGUF / mmproj."
                    Toast.makeText(this, "Falha na importação do modelo visual", Toast.LENGTH_SHORT).show()
                    updateStatus("Falha na importação do modelo visual")
                }
            }
        }.start()
    }

    private fun runOcrProbe() {
        val imageName = selectedVisionImageName
        val imagePath = selectedVisionImagePath
        if (selectedVisionImageUri == null || imageName.isNullOrBlank() || imagePath.isNullOrBlank()) {
            visionResultText?.text = "Selecione uma imagem primeiro."
            Toast.makeText(this, "Selecione uma imagem primeiro", Toast.LENGTH_SHORT).show()
            return
        }
        visionResultText?.text = "Verificando motor OCR..."
        ensureNotificationPermissionAndStartService()
        callLocalApi(
            path = "/vision/ocr",
            method = "POST",
            body = JSONObject().apply {
                put("image_name", imageName)
                put("image_path", imagePath)
            }.toString(),
            retryCount = 4,
            onResult = { _, body, elapsed ->
                val json = runCatching { JSONObject(body) }.getOrNull()
                val backendStatus = json?.optString("status", "unknown") ?: "unknown"
                val image = json?.optJSONObject("image")
                val imageLine = image?.let {
                    val width = it.optInt("width", 0)
                    val height = it.optInt("height", 0)
                    val bytes = it.optLong("size_bytes", 0L)
                    if (width > 0 && height > 0) "Imagem ${width}x${height} · ${formatBytes(bytes)}" else "Imagem ${formatBytes(bytes)}"
                } ?: "Imagem lida"
                val message = when (backendStatus) {
                    "ok" -> json?.optString("text").orEmpty().ifBlank { "Nenhum texto reconhecido" }
                    "invalid_image" -> json?.optString("message", "Não foi possível ler a imagem.") ?: "Não foi possível ler a imagem."
                    "backend_not_installed" -> "Motor OCR não instalado. Recomenda-se conectar RapidOCR / PP-OCR (ONNX Runtime Mobile)."
                    else -> json?.optString("message", "OCR temporariamente indisponível, tente novamente mais tarde.") ?: "OCR temporariamente indisponível, tente novamente mais tarde."
                }
                visionResultText?.text = "$imageLine\\n$message\\n\\nTempo ${elapsed}ms"
                updateStatus(if (backendStatus == "ok") "OCR concluído" else "Motor OCR não instalado")
            }
        )
    }

    private fun runVisionClassify(dataset: String) {
        val imageName = selectedVisionImageName
        val imagePath = selectedVisionImagePath
        if (selectedVisionImageUri == null || imageName.isNullOrBlank() || imagePath.isNullOrBlank()) {
            visionResultText?.text = "Selecione uma imagem primeiro."
            Toast.makeText(this, "Selecione uma imagem primeiro", Toast.LENGTH_SHORT).show()
            return
        }
        val displayDataset = if (dataset == "mnist") "MNIST" else "CIFAR10"
        visionResultText?.text = "Verificando motor de classificação $displayDataset..."
        ensureNotificationPermissionAndStartService()
        callLocalApi(
            path = "/vision/classify",
            method = "POST",
            body = JSONObject().apply {
                put("image_name", imageName)
                put("image_path", imagePath)
                put("dataset", dataset)
            }.toString(),
            retryCount = 4,
            onResult = { _, body, elapsed ->
                val json = runCatching { JSONObject(body) }.getOrNull()
                val backendStatus = json?.optString("status", "unknown") ?: "unknown"
                val image = json?.optJSONObject("image")
                val imageLine = image?.let {
                    val width = it.optInt("width", 0)
                    val height = it.optInt("height", 0)
                    val bytes = it.optLong("size_bytes", 0L)
                    if (width > 0 && height > 0) "Imagem ${width}x${height} · ${formatBytes(bytes)}" else "Imagem ${formatBytes(bytes)}"
                } ?: "Imagem lida"
                val message = when (backendStatus) {
                    "ok" -> {
                        val label = json?.optString("label").orEmpty().ifBlank { "Categoria desconhecida" }
                        val confidence = json?.optDouble("confidence", 0.0) ?: 0.0
                        "$displayDataset：$label · 置信度 ${"%.2f".format(Locale.US, confidence)}"
                    }
                    "invalid_image" -> json?.optString("message", "Não foi possível ler a imagem.") ?: "Não foi possível ler a imagem."
                    "model_missing" -> json?.optString("message").orEmpty()
                        .ifBlank { "Importe primeiro o modelo TFLite/ONNX correspondente a $displayDataset." }
                    "text_embeddings_missing" -> json?.optString("message").orEmpty()
                        .ifBlank { "CLIP pronto, mas sidecar de embedding de texto CIFAR10 ausente." }
                    "unsupported_model_shape", "model_load_error", "inference_error" -> json?.optString("message").orEmpty()
                        .ifBlank { "Modelo de classificação $displayDataset temporariamente indisponível." }
                    else -> json?.optString("message", "Classificação temporariamente indisponível, tente novamente mais tarde.") ?: "Classificação temporariamente indisponível, tente novamente mais tarde."
                }
                visionResultText?.text = "$imageLine\\n$message\\n\\nTempo ${elapsed}ms"
                updateStatus(if (backendStatus == "ok") "Classificação concluída" else "Classificação requer modelo")
            }
        )
    }

    private fun importGguf(uri: Uri) {
        val displayName = resolveDisplayName(uri) ?: "imported-${System.currentTimeMillis()}.gguf"
        val safeName = sanitizeModelFileName(displayName)
        val destination = File(internalModelDir(), safeName)
        val catalog = playgroundCatalog
        if (catalog == null || PlaygroundManagedArtifactPolicy.isManagedFileName(catalog, safeName)) {
            updateStatus("Importação rejeitada: nome do arquivo pertence ao caminho gerenciado do Playground")
            Toast.makeText(this, "Este nome de arquivo é gerenciado pelo instalador de modelos confiáveis, não pode ser sobrescrito por importação comum", Toast.LENGTH_LONG).show()
            return
        }
        if (destination.exists()) {
            updateStatus("Importação rejeitada: modelo com mesmo nome já existe")
            Toast.makeText(this, "Modelo com mesmo nome já existe; importação comum não sobrescreverá arquivos locais", Toast.LENGTH_LONG).show()
            return
        }
        val temporary = File(internalModelDir(), ".$safeName.${UUID.randomUUID()}.import")

        updateStatus("Importando modelo: $safeName")
        Thread {
            try {
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Não foi possível abrir o arquivo selecionado" }
                    AtomicGgufImport.copy(input, temporary, destination)
                }
                runOnUiThread {
                    Toast.makeText(this, "Modelo importado", Toast.LENGTH_SHORT).show()
                    ensureNotificationPermissionAndLoadModel(destination)
                }
            } catch (e: Exception) {
                temporary.delete()
                runOnUiThread {
                    updateStatus("Falha na importação do modelo")
                    Toast.makeText(this, "Falha na importação do GGUF; arquivo original não foi sobrescrito", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun ensureNotificationPermissionAndLoadModel(model: File) {
        withNotificationPermission {
            startServiceWithModel(model)
        }
    }

    private fun loadFirstModel() {
        val model = findPreferredGguf()
        if (model == null) {
            updateStatus("Modelo GGUF não encontrado")
            Toast.makeText(this, "Importe primeiro um modelo GGUF", Toast.LENGTH_SHORT).show()
            return
        }

        startServiceWithModel(model)
    }

    private fun startServiceWithModel(model: File) {
        val release = releaseGallerySearchRuntime(
            "Carregando GGUF, sessão CLIP liberada para evitar que dois modelos ocupem memória simultaneamente.",
        )
        if (release != null) {
            updateStatus("Liberando memória do modelo visual")
            Thread {
                val released = runCatching { release.get(30L, TimeUnit.SECONDS) }.isSuccess
                runOnUiThread {
                    if (released) {
                        startServiceWithModelAfterGalleryRelease(model)
                    } else {
                        updateStatus("Falha ao liberar memória do modelo visual, GGUF não carregado")
                        Toast.makeText(this, "Não foi possível liberar CLIP com segurança, carregamento do modelo cancelado", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
            return
        }
        startServiceWithModelAfterGalleryRelease(model)
    }

    private fun startServiceWithModelAfterGalleryRelease(model: File) {
        val requestedPath = canonicalModelPath(model.absolutePath) ?: model.absolutePath
        if (activeModelPath != requestedPath) {
            activeModelPath = null
            runtimeReportsLoadedModel = false
            reconcilePlaygroundRuntimeTruth(null)
        }
        pendingModelPath = requestedPath
        modelLoadFailurePath = null
        modelLoadFailureMessage = null
        val intent = Intent(this, MobileCoreService::class.java).apply {
            putExtra("modelPath", model.absolutePath)
        }
        startService(intent)
        updateStatus("Carregando modelo: ${model.name}")
        if (currentTab in setOf(AppTab.HOME, AppTab.MODELS, AppTab.PLAYGROUND, AppTab.TEST)) renderCurrentTab()
    }

    private fun findPreferredGguf(): File? {
        return availableGgufModels()
            .minWithOrNull(
                compareBy<File> { preferredGgufScore(it) }
                    .thenBy { it.name.lowercase(Locale.US) }
            )
    }

    private fun availableGgufModels(): List<File> {
        return modelDirs()
            .flatMap { dir ->
                dir.listFiles { file ->
                    file.isFile && file.extension.lowercase(Locale.US) == "gguf"
                }?.toList() ?: emptyList()
            }
            .distinctBy { it.absolutePath }
    }

    private fun preferredGgufScore(file: File): Int {
        val metadata = GgufMetadataReader.read(file)
        val quantization = metadata.quantization.uppercase(Locale.US)
        val quantizationPriority = when {
            quantization.startsWith("Q4") -> 0
            quantization.startsWith("Q5") -> 1
            quantization == "F16" || quantization == "BF16" || quantization.startsWith("Q6") -> 2
            quantization.startsWith("Q3") -> 3
            quantization.startsWith("Q2") || quantization.startsWith("IQ2") -> 4
            quantization.startsWith("Q8") || quantization.startsWith("Q7") -> 5
            quantization.startsWith("Q1") || quantization.startsWith("IQ1") || quantization.contains("IQ1") -> 8
            else -> 6
        }
        val parameterPenalty = ((metadata.parameterCountB ?: 99.0) * 100).roundToInt().coerceAtMost(9900)
        val sizePenalty = (file.length() / (128L * 1024L * 1024L)).toInt().coerceAtMost(99)
        return quantizationPriority * 100_000 + parameterPenalty * 100 + sizePenalty
    }

    private fun modelDirs(): List<File> {
        return listOf(internalModelDir(), externalModelDir()).onEach { it.mkdirs() }
    }

    private fun internalModelDir(): File {
        return File(filesDir, "models")
    }

    private fun externalModelDir(): File {
        return getExternalFilesDir("models") ?: File(filesDir, "models")
    }

    private fun internalVisionImageDir(): File {
        return File(filesDir, "vision/images").apply { mkdirs() }
    }

    private fun visionModelDirs(): List<File> {
        // One canonical app-private directory is shared by import UI, catalog and runtime.
        // Never assemble a runnable CLIP package from files spread across storage roots.
        return listOf(internalVisionModelDir()).onEach { it.mkdirs() }
    }

    private fun internalVisionModelDir(): File {
        return File(filesDir, "vision/models")
    }

    private fun scanVisionModelFiles(): List<File> {
        val supportedExtensions = setOf("onnx", "ort", "tflite", "mnn", "gguf", "mmproj")
        return visionModelDirs()
            .flatMap { dir ->
                dir.listFiles { file ->
                    file.isFile && file.extension.lowercase(Locale.US) in supportedExtensions
                }?.toList() ?: emptyList()
            }
            .distinctBy { it.absolutePath }
            .sortedBy { it.name.lowercase(Locale.US) }
    }

    private fun scanVisionSidecarFiles(): List<File> {
        return visionModelDirs()
            .flatMap { dir ->
                dir.listFiles { file ->
                    file.isFile && file.extension.lowercase(Locale.US) in setOf("json", "txt")
                }?.toList() ?: emptyList()
            }
            .distinctBy { it.absolutePath }
            .sortedBy { it.name.lowercase(Locale.US) }
    }

    private fun hasVisionModelTask(task: String): Boolean {
        return scanVisionModelFiles().any { inferVisionTask(it.name) == task }
    }

    private fun inferVisionTask(fileName: String): String {
        val lower = fileName.lowercase(Locale.US)
        return when {
            "mnist" in lower -> "mnist"
            "clip" in lower || "vit" in lower -> "clip"
            "cifar" in lower -> "cifar10"
            "ocr" in lower || "ppocr" in lower || "paddle" in lower || "rapid" in lower || "trocr" in lower -> "ocr"
            "sd" in lower || "diffusion" in lower || "lcm" in lower -> "diffusion"
            else -> "vision"
        }
    }

    private fun visionModelSummary(
        models: List<File> = scanVisionModelFiles(),
        sidecars: List<File> = scanVisionSidecarFiles()
    ): String {
        if (models.isEmpty() && sidecars.isEmpty()) {
            return "Nenhum modelo visual importado. Você pode importar ONNX / ORT / TFLite / MNN, CLIP pode configurar JSON, VLM precisa de GGUF + mmproj."
        }
        val groups = models.groupingBy { inferVisionTask(it.name) }.eachCount()
        val modelSummary = groups.entries
            .sortedBy { it.key }
            .joinToString(" · ") { "${it.key.uppercase(Locale.US)} ${it.value}" }
        val sidecarSummary = if (sidecars.isNotEmpty()) "SIDECAR ${sidecars.size}" else ""
        return listOf(modelSummary, sidecarSummary)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
    }

    private fun copyVisionModelDir() {
        val directory = internalVisionModelDir().absolutePath
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("MobileCore vision models", directory))
        visionResultText?.text = "Diretório de modelos visuais copiado."
        Toast.makeText(this, "Diretório de modelos visuais copiado", Toast.LENGTH_SHORT).show()
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                } else {
                    null
                }
            }
            ?: uri.lastPathSegment
    }

    private fun sanitizeModelFileName(name: String): String {
        val cleaned = name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
        val nonBlank = cleaned.ifBlank { "imported-${System.currentTimeMillis()}.gguf" }
        return if (nonBlank.endsWith(".gguf", ignoreCase = true)) nonBlank else "$nonBlank.gguf"
    }

    private fun sanitizeVisionImageFileName(name: String): String {
        val cleaned = name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
        val nonBlank = cleaned.ifBlank { "vision-${System.currentTimeMillis()}.png" }
        val allowed = setOf("jpg", "jpeg", "png", "webp", "bmp")
        return if (nonBlank.substringAfterLast('.', "").lowercase(Locale.US) in allowed) {
            nonBlank
        } else {
            "$nonBlank.png"
        }
    }

    private fun sanitizeVisionModelFileName(name: String): String {
        val cleaned = name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
        val nonBlank = cleaned.ifBlank { "vision-model-${System.currentTimeMillis()}.onnx" }
        val allowed = setOf("onnx", "ort", "tflite", "mnn", "json", "txt", "gguf", "mmproj")
        val extension = nonBlank.substringAfterLast('.', "").lowercase(Locale.US)
        return if (extension in allowed) nonBlank else "$nonBlank.onnx"
    }

    private fun isGalleryClipArtifactName(fileName: String): Boolean {
        val normalized = fileName.lowercase(Locale.US)
        return normalized in setOf("vocab.json", "merges.txt", "tokenizer_config.json") ||
            normalized == "openai-clip-vit-b16-image.onnx" ||
            normalized == "openai-clip-vit-b16-text.onnx"
    }

    private fun updateStatus(message: String) {
        if (::statusText.isInitialized) statusText.text = message
        if (::runtimeChipText.isInitialized) {
            runtimeChipText.text = when {
                message.contains("Serviço iniciado") -> "Serviço local iniciado"
                message.contains("Carregando") -> "Carregando"
                message.contains("Baixando") || message.startsWith("Downloading") -> "Baixando modelo"
                message.contains("Baixado") || message.startsWith("Downloaded") -> "Modelo baixado"
                message.contains("Carregado") -> "Carregado"
                message.contains("Falha ao carregar") -> "Falha ao carregar"
                message.contains("Serviço parado") -> "Serviço parado"
                message.contains("GGUF não encontrado") -> "Precisa de modelo"
                message.contains("Falhou") -> "Precisa de processamento"
                else -> "Serviço local"
            }
        }
    }

    private fun label(text: String, sizeSp: Float, color: Int, style: Int): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(color)
            typeface = Typeface.create(Typeface.DEFAULT, style)
            includeFontPadding = false
            setLineSpacing(dp(1).toFloat(), 1f)
        }
    }

    private fun autoSizeSingleLineLabel(
        text: String,
        maximumSp: Float,
        minimumSp: Float,
        color: Int,
        style: Int,
    ): TextView {
        return label(text, maximumSp, color, style).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this,
                minimumSp.toInt(),
                maximumSp.toInt(),
                1,
                TypedValue.COMPLEX_UNIT_SP,
            )
        }
    }

    private fun chip(textView: TextView, background: Int, border: Int): View {
        return FrameLayout(this).apply {
            this.background = rounded(background, tint(border, 0.24f), 7f)
            setPadding(dp(12), 0, dp(12), 0)
            addView(
                textView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
    }

    private fun pillButton(text: String, startColor: Int, endColor: Int, onClick: () -> Unit): View {
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            textSize = 14f
            minimumHeight = dp(TuiMaTheme.minimumTouchTargetDp)
            maxLines = 2
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            background = ripple(roundedGradient(intArrayOf(startColor, endColor), 8f), endColor)
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this,
                12,
                14,
                1,
                TypedValue.COMPLEX_UNIT_SP,
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onClick()
            }
        }
    }

    private fun ambientPageBackground(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                mixColor(Palette.background, Palette.blueWash, if (TuiMaTheme.isDark) 0.44f else 0.56f),
                Palette.background,
                Palette.background
            )
        )
    }

    private fun pageGutterDp(): Int = if (resources.configuration.screenWidthDp < 380) 14 else 18

    private fun mixColor(base: Int, overlay: Int, amount: Float): Int {
        val ratio = amount.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(base) + (Color.red(overlay) - Color.red(base)) * ratio).roundToInt(),
            (Color.green(base) + (Color.green(overlay) - Color.green(base)) * ratio).roundToInt(),
            (Color.blue(base) + (Color.blue(overlay) - Color.blue(base)) * ratio).roundToInt()
        )
    }

    private fun thinDivider(): View {
        return View(this).apply {
            setBackgroundColor(Palette.stroke)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        }
    }

    private fun space(heightDp: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
        }
    }

    private fun rounded(color: Int, stroke: Int, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }
    }

    private fun roundedGradient(colors: IntArray, radiusDp: Float): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun ripple(content: GradientDrawable, accent: Int): RippleDrawable {
        return RippleDrawable(ColorStateList.valueOf(tint(accent, 0.18f)), content, null)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun dp(value: Float): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun tint(color: Int, alpha: Float): Int {
        return Color.argb((255 * alpha).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) "%.1f GB".format(mb / 1024.0) else "%.0f MB".format(mb)
    }

    private fun displayModelName(name: String): String {
        return when {
            name.contains("qwen2.5", ignoreCase = true) -> "Qwen2.5 0.5B"
            name.contains("smollm", ignoreCase = true) -> "SmolLM2 135M"
            name.length <= 16 -> name
            else -> name.take(13).trim('-', '_') + "..."
        }
    }

    private fun displayBackendName(name: String): String {
        return when {
            name.contains("llama", ignoreCase = true) -> "llama.cpp"
            name.length <= 14 -> name
            else -> name.take(12).trim('-', '_') + "..."
        }
    }

    private fun displayDeviceName(manufacturer: String, model: String): String {
        val cleanManufacturer = manufacturer.ifBlank { "Android" }
        val cleanModel = model
            .replace("sdk_gphone64_", "", ignoreCase = true)
            .replace("sdk_gphone_", "", ignoreCase = true)
            .replace("arm64", "arm64", ignoreCase = true)
            .trim('_', '-', ' ')
            .ifBlank { model }
        val combined = if (cleanModel.contains(cleanManufacturer, ignoreCase = true)) {
            cleanModel
        } else {
            "$cleanManufacturer $cleanModel"
        }
        return if (combined.length <= 18) combined else combined.take(15).trim('_', '-', ' ') + "..."
    }

}

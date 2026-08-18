package ai.mobilecore.ui

import ai.mobilecore.benchmark.BenchmarkProfile
import ai.mobilecore.benchmark.BenchmarkUiState

data class BenchmarkLiveSnapshot(
    val batteryPercent: Int? = null,
    val temperatureCelsius: Double? = null,
    val decodeTokensPerSecond: Double? = null,
    val elapsedMs: Long = 0L
)

data class BenchmarkScreenUiModel(
    val title: String,
    val message: String,
    val progressPercent: Int,
    val phaseLabel: String,
    val remainingLabel: String,
    val isRunning: Boolean
)

object BenchmarkScreenPresenter {
    fun present(
        state: BenchmarkUiState,
        live: BenchmarkLiveSnapshot,
        modelDisplayName: (String) -> String = { it }
    ): BenchmarkScreenUiModel {
        val progress = progress(state)
        return BenchmarkScreenUiModel(
            title = title(state),
            message = message(state, modelDisplayName),
            progressPercent = progress,
            phaseLabel = phase(state),
            remainingLabel = estimateRemaining(state, progress, live.elapsedMs),
            isRunning = state.isRunning
        )
    }

    fun progress(state: BenchmarkUiState): Int = when (state) {
        is BenchmarkUiState.Checking -> 8
        is BenchmarkUiState.LoadingModel -> 18
        is BenchmarkUiState.WarmingUp -> 20 + (state.current * 10 / state.total.coerceAtLeast(1))
        is BenchmarkUiState.Measuring -> 30 + (state.current * 58 / state.total.coerceAtLeast(1))
        is BenchmarkUiState.Cooling -> 90
        is BenchmarkUiState.Cancelling -> 92
        is BenchmarkUiState.Completed -> 100
        else -> 0
    }

    private fun title(state: BenchmarkUiState): String = when (state) {
        BenchmarkUiState.Ready -> "Preparado"
        is BenchmarkUiState.NeedsModel -> "Falta um modelo padrão"
        is BenchmarkUiState.Checking -> "Verificando dispositivo"
        is BenchmarkUiState.LoadingModel -> "Preparando modelo"
        is BenchmarkUiState.WarmingUp -> "Aquecendo"
        is BenchmarkUiState.Measuring -> "Pontuando"
        is BenchmarkUiState.Cooling -> "Aguardando resfriamento do dispositivo"
        is BenchmarkUiState.Cancelling -> "Cancelando com segurança"
        is BenchmarkUiState.Blocked -> "Não pode iniciar temporariamente"
        is BenchmarkUiState.Completed -> "Benchmark concluído"
        is BenchmarkUiState.Failed -> "Benchmark desta vez não concluído"
        BenchmarkUiState.Cancelled -> "Benchmark cancelado"
    }

    private fun message(state: BenchmarkUiState, modelDisplayName: (String) -> String): String = when (state) {
        BenchmarkUiState.Ready -> "Modelo padrão pronto. O serviço local será iniciado automaticamente durante o teste."
        is BenchmarkUiState.NeedsModel -> "Necessário o modelo padrão ${modelDisplayName(state.fileName.substringBeforeLast('.'))}, o download será iniciado assim que concluído."
        is BenchmarkUiState.Checking -> "Verificando bateria, temperatura, armazenamento e integridade do modelo."
        is BenchmarkUiState.LoadingModel -> "Carregando ${modelDisplayName(state.modelName.substringBeforeLast('.'))}."
        is BenchmarkUiState.WarmingUp -> "Aquecimento ${state.current} / ${state.total}, esta parte não pontua."
        is BenchmarkUiState.Measuring -> "Pontuação ${state.current} / ${state.total}, mantenha o app em primeiro plano."
        is BenchmarkUiState.Cooling -> "Cerca de ${state.secondsRemaining} segundos restantes, evite que a temperatura afete a próxima rodada."
        is BenchmarkUiState.Cancelling -> "Parando inferência atual e salvando informações de diagnóstico."
        is BenchmarkUiState.Blocked -> "Trate os itens abaixo e clique em re-verificar."
        is BenchmarkUiState.Completed -> "${formatScore(state.headlineScore)} TuiMa · Pontuação padrão ${state.canonicalScore} / 1000"
        is BenchmarkUiState.Failed -> state.message
        BenchmarkUiState.Cancelled -> "Nenhuma pontuação gerada, você pode recomeçar a qualquer momento."
    }

    private fun phase(state: BenchmarkUiState): String = when (state) {
        is BenchmarkUiState.Checking -> "1/5 Verificação do dispositivo"
        is BenchmarkUiState.LoadingModel -> "2/5 Carregando modelo"
        is BenchmarkUiState.WarmingUp -> "3/5 Aquecimento do modelo"
        is BenchmarkUiState.Measuring -> "4/5 Pontuação oficial"
        is BenchmarkUiState.Cooling -> "4/5 Aguardando resfriamento"
        is BenchmarkUiState.Completed -> "5/5 Gerando resultado"
        else -> "Aguardando início"
    }

    private fun estimateRemaining(state: BenchmarkUiState, progress: Int, elapsedMs: Long): String {
        if (!state.isRunning) return if (state is BenchmarkUiState.Completed) "Concluído" else "Não iniciado"
        if (state is BenchmarkUiState.Cooling) return "Cerca de ${state.secondsRemaining} segundos"
        if (elapsedMs <= 0L || progress <= 0) return "Estimando"
        val remaining = (elapsedMs.toDouble() * (100 - progress).toDouble() / progress.toDouble()).toLong()
        return formatRemainingDuration(remaining.coerceAtLeast(1_000L))
    }

    private fun formatScore(value: Int): String = "%,d".format(value)
}

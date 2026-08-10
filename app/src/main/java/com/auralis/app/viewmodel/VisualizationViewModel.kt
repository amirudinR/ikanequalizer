package com.auralis.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.visualization.SpectrumAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VisualizationUiState(
    val spectrum: FloatArray = FloatArray(SpectrumAnalyzer.BAND_COUNT),
    val waveform: FloatArray = FloatArray(SpectrumAnalyzer.WAVEFORM_SAMPLES),
    val rms: Float = 0f,
    val bass: Float = 0f,
    val available: Boolean = false,
    val playing: Boolean = false,
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/** Bridges the SpectrumAnalyzer flows into a single UI state for the 3D/spectrum views. */
class VisualizationViewModel(
    private val analyzer: SpectrumAnalyzer,
    private val isMusicActiveFlow: StateFlow<Boolean>,
) : ViewModel() {

    private val _ui = MutableStateFlow(VisualizationUiState())
    val ui: StateFlow<VisualizationUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { analyzer.spectrum.collect { s -> _ui.value = _ui.value.copy(spectrum = s) } }
        viewModelScope.launch { analyzer.waveform.collect { w -> _ui.value = _ui.value.copy(waveform = w) } }
        viewModelScope.launch { analyzer.rms.collect { r -> _ui.value = _ui.value.copy(rms = r) } }
        viewModelScope.launch { analyzer.bass.collect { b -> _ui.value = _ui.value.copy(bass = b) } }
        viewModelScope.launch { analyzer.available.collect { a -> _ui.value = _ui.value.copy(available = a) } }
        viewModelScope.launch { isMusicActiveFlow.collect { p -> _ui.value = _ui.value.copy(playing = p) } }
    }
}

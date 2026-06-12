package com.my.forintern.HomeScreen

import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.my.forintern.Graph
import com.my.forintern.Message.ChatMessage
import com.my.forintern.OnBoarding.UserProfile
import com.my.forintern.OnBoarding.UserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt


enum class AuraState {
    IDLE, LISTENING
}
//pipeline for the messages
sealed class MessagePipeline {
    object Idle : MessagePipeline()
    object Typing : MessagePipeline()
    object Validating : MessagePipeline()
    object Processing : MessagePipeline()
    object Responding : MessagePipeline()
    data class Error(val reason: String, val lastMessage: String) : MessagePipeline()
}

data class HomeUIState(
    val auraState: AuraState = AuraState.IDLE,
    val amplitude: Float = 0f,
    val hasMicPermission: Boolean = false,
    val isKeyboardVisible: Boolean = false,
    val inputText: String = "",
    val allChatMessages: List<ChatMessage> = emptyList(),
    val visibleMessages: List<ChatMessage> = emptyList(),
    val visibleMessagesCount: Int = 20,
    val isChatHistoryVisible: Boolean = false,
    val editingMessage: ChatMessage? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserProfileRepository(application)
    private val userRepo = Graph.userrepo

    val userProfile: StateFlow<UserProfile> = repository.userProfileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserProfile()
    )

    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState = _uiState.asStateFlow()
    //pipeline for the messages
    private val _messagePipeline = MutableStateFlow<MessagePipeline>(MessagePipeline.Idle)
    val messagePipeline = _messagePipeline.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var messageJob: Job? = null      //pipeline

    init {
        viewModelScope.launch {
            userProfile.collectLatest { profile ->
                val phoneId = profile.phone.toLongOrNull()
                if (phoneId != null) {
                    userRepo.getUserFlowById(phoneId).collectLatest { dataset ->
                        val messages = dataset?.message ?: emptyList()
                        _uiState.update { state ->
                            val newCount = state.visibleMessagesCount
                            state.copy(
                                allChatMessages = messages,
                                visibleMessages = messages.takeLast(newCount)
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMoreMessages() {
        _uiState.update { state ->
            val newCount = state.visibleMessagesCount + 20
            state.copy(
                visibleMessagesCount = newCount,
                visibleMessages = state.allChatMessages.takeLast(newCount)
            )
        }
    }

    fun toggleKeyboard() {
        _uiState.update { it.copy(isKeyboardVisible = !it.isKeyboardVisible) }
    }

    fun setEditingMessage(message: ChatMessage?) {
        _uiState.update {
            it.copy(
                editingMessage = message,
                inputText = message?.text ?: it.inputText,
                isKeyboardVisible = message != null || it.isKeyboardVisible
            )
        }
    }

    fun deleteMessage(message: ChatMessage) {
        val phoneId = userProfile.value.phone.toLongOrNull() ?: return
        viewModelScope.launch {
            userRepo.deleteMessage(phoneId, message)
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
        //pipeline for the messages
        val currentState = _messagePipeline.value
        if (text.isNotBlank() && currentState is MessagePipeline.Idle) {
            _messagePipeline.value = MessagePipeline.Typing
        } else if (text.isBlank() && currentState is MessagePipeline.Typing) {
            _messagePipeline.value = MessagePipeline.Idle
        }
    }

    fun retryMessage(text: String) {
        _uiState.update { it.copy(inputText = text) }
        sendMessage()
    }

    // Visible for testing
    var processingDelayMs: Long = 2000L

    fun sendMessage() {
        val text = _uiState.value.inputText
        if (text.isBlank()) return

        val phoneId = userProfile.value.phone.toLongOrNull() ?: return

        //pipeline for the messages

        messageJob?.cancel() // Cancel mid-flow if sending another message
        messageJob = viewModelScope.launch {
            try {
                _messagePipeline.value = MessagePipeline.Validating
                delay(500) // Simulating validation

                _messagePipeline.value = MessagePipeline.Processing
                withTimeout(8000) {
                    delay(processingDelayMs) // Simulating network/processing
                }

                _messagePipeline.value = MessagePipeline.Responding
                delay(500) // Simulating receiving response

                val editingMsg = _uiState.value.editingMessage
                if (editingMsg != null) {
                    val newTime = if (editingMsg.time.endsWith(" (edited)")) editingMsg.time else "${editingMsg.time} (edited)"
                    val updatedMessage = editingMsg.copy(text = text, time = newTime)
                    userRepo.editMessage(phoneId, editingMsg, updatedMessage)
                } else {
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val time = sdf.format(Date())
                    val newMessage = ChatMessage(text = text, time = time, issentByme = true)

                    // Add message to Room
                    val dataset = Graph.userdatabase.userDao().getUserByIdSync(phoneId)
                    if (dataset != null) {
                        Graph.userdatabase.userDao().addMessageToUser(phoneId, newMessage)
                    }
                }

                //pipeline for the messages
                _uiState.update { it.copy(inputText = "", editingMessage = null) }
                _messagePipeline.value = MessagePipeline.Idle
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _messagePipeline.value = MessagePipeline.Error("Processing timeout exceeded", text)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            }
        }
    }

    fun setChatHistoryVisibility(isVisible: Boolean) {
        _uiState.update { it.copy(isChatHistoryVisible = isVisible) }
    }

    fun setHasMicPermission(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(hasMicPermission = hasPermission)
    }

    fun toggleListening() {
        if (_uiState.value.auraState == AuraState.IDLE) {
            startListening()
        } else {
            stopListening()
        }
    }

    private fun startListening() {
        if (!_uiState.value.hasMicPermission) return

        _uiState.value = _uiState.value.copy(auraState = AuraState.LISTENING)
        startAudioRecording()
    }

    private fun stopListening() {
        _uiState.value = _uiState.value.copy(auraState = AuraState.IDLE, amplitude = 0f)
        stopAudioRecording()
    }

    private fun startAudioRecording() {
        recordingJob?.cancel()
        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 44100
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufSize * 2
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("HomeViewModel", "AudioRecord initialization failed")
                    stopListening()
                    return@launch
                }

                audioRecord?.startRecording()
                val buffer = ShortArray(minBufSize)

                var smoothedAmplitude = 0f

                while (isActive && _uiState.value.auraState == AuraState.LISTENING) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            val v = buffer[i].toDouble()
                            sum += v * v
                        }
                        val rms = sqrt(sum / readSize).toFloat()

                        // Normalize RMS to a 0f..1f range (assuming max typical speech RMS is ~2500)
                        val maxExpectedRms = 2500f
                        val normalized = (rms / maxExpectedRms).coerceIn(0f, 1f)

                        // Apply simple low-pass filter for smooth transitions
                        smoothedAmplitude = smoothedAmplitude + 0.3f * (normalized - smoothedAmplitude)

                        _uiState.value = _uiState.value.copy(amplitude = smoothedAmplitude)
                    }
                    delay(16) // roughly 60fps update rate
                }
            } catch (e: SecurityException) {
                Log.e("HomeViewModel", "SecurityException: ${e.message}")
                stopListening()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Audio exception: ${e.message}")
                stopListening()
            } finally {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
            }
        }
    }

    private fun stopAudioRecording() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            // ignore
        }
        audioRecord?.release()
        audioRecord = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioRecording()
    }
}
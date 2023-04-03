package io.github.guowenlong.chatgptforandroid

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.guowenlong.chatgpt.StreamListener
import io.github.guowenlong.chatgpt.model.request.CompletionRequest
import io.github.guowenlong.chatgpt.model.response.CompletionStream
import io.github.guowenlong.chatgptforandroid.common.base.BaseViewModel
import io.github.guowenlong.chatgptforandroid.common.ext.logE
import io.github.guowenlong.chatgptforandroid.repository.OpenAIRepository
import kotlinx.coroutines.launch

/**
 * Description: [MainActivity]的ViewModel
 * Author:      郭文龙
 * Date:        2023/3/31 2:38
 * Email:       guowenlong20000@sina.com
 */
class MainViewModel(private val repository: OpenAIRepository) : BaseViewModel() {

    val data = mutableListOf<Any>()

    private val _updateLiveData = MutableLiveData<Int>()
    val updateLiveData: LiveData<Int> = _updateLiveData

    private val _insertUserChatLiveData = MutableLiveData<String>()
    val insertUserChatLiveData: LiveData<String> = _insertUserChatLiveData

    private val _insertGPTChatLiveData = MutableLiveData<CompletionStream>()
    val insertGPTChatLiveData: LiveData<CompletionStream> = _insertGPTChatLiveData

    fun getModels() = launch {
        val models = repository.getModels()
        logE("getModels: $models")
    }

    fun completions() = launch {
        val completions = repository.getCompletions(
            CompletionRequest(
                messages = listOf(
                    CompletionRequest.Message(
                        content = "埃隆马斯克的生平简介发给我"
                    )
                )
            )
        )
        logE("completions: $completions")
    }

    fun completionStream(completionRequest: CompletionRequest) = launch {
        repository.getCompletionsByString(
            completionRequest, object : StreamListener {
                override fun onStart() {
                    logE("onStart")
                    data.add(completionRequest.messages[0].content)
                    _insertUserChatLiveData.postValue(completionRequest.messages[0].content)
                }

                override fun onStreamPre(completionStream: CompletionStream) {
                    logE("onStreamPre $completionStream")
                    data.add(completionStream)
                    _insertGPTChatLiveData.postValue(completionStream)
                }

                override fun onStream(completionStream: CompletionStream) {
                    viewModelScope.launch {
                        data.mapIndexed { index, any ->
                            if (any is CompletionStream) {
                                if (any.id == completionStream.id) {
                                    any.choices[0].delta.content += completionStream.choices[0].delta.content
                                    _updateLiveData.postValue(index)
                                }
                            }
                        }
                    }
                    logE("onStream $completionStream")
                }

                override fun onCompleted() {
                    logE("onCompleted")
                }

                override fun onError(exception: Exception) {
                    logE("onError:",exception)
                }
            }
        )
    }

    fun testCompletion() = launch {
        var content = ""
        repository.getCompletionsByString(
            CompletionRequest(
                messages = listOf(
                    CompletionRequest.Message(
                        content = "埃隆马斯克的生平简介发给我"
                    )
                )
            ), object : StreamListener {
                override fun onStart() {
                    logE("onStart")
                }

                override fun onStreamPre(completionStream: CompletionStream) {
                    content += completionStream.choices[0].delta.content
                    logE("onStreamPre:$content")
                }

                override fun onStream(completionStream: CompletionStream) {
                    content += completionStream.choices[0].delta.content
                    logE("onStream:$content")
                }

                override fun onCompleted() {
                    logE("onCompleted")
                }

                override fun onError(exception: Exception) {
                    logE("onError:",exception)
                }
            }
        )
    }
}
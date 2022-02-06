package com.arvindmehra.notes.feature_note.presentation.add_edit_notes

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arvindmehra.notes.feature_note.domain.model.InvalidNoteException
import com.arvindmehra.notes.feature_note.domain.model.Note
import com.arvindmehra.notes.feature_note.domain.use_case.NoteUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AddEditNoteViewModel @Inject constructor(
    private val noteUseCases: NoteUseCases,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _noteTitleState = mutableStateOf(
        NoteTitleFieldState(
            hint = "Enter title ..."
        )
    )
    val noteTitleState: State<NoteTitleFieldState> = _noteTitleState

    private val _noteContentState = mutableStateOf(
        NoteTitleFieldState(
            hint = "Enter some content"
        )
    )
    val noteContentState: State<NoteTitleFieldState> = _noteContentState

    private val _noteColorState = mutableStateOf(Note.noteColors.random().toArgb())
    val noteColorState: State<Int> = _noteColorState

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var currentNoteId: Int? = null

    init {
        savedStateHandle.get<Int>("noteId")?.let { noteId->
            if(noteId != -1) {
                viewModelScope.launch {
                    noteUseCases.getNoteById(noteId)?.also { note ->
                        currentNoteId = note.id
                        _noteTitleState.value = noteTitleState.value.copy(
                            text = note.title,
                            isHintVisible = false
                        )
                        _noteContentState.value = noteContentState.value.copy(
                            text = note.title,
                            isHintVisible = false
                        )
                        _noteColorState.value = note.color
                    }
                }
            }
        }
    }


    fun onEvent(event: AddEditNoteEvent) {
        when (event) {
            is AddEditNoteEvent.EnteredTitle -> {
                _noteTitleState.value = noteTitleState.value.copy(
                    text = event.value
                )
            }
            is AddEditNoteEvent.ChangedTitleFocus -> {
                _noteTitleState.value = noteTitleState.value.copy(
                    isHintVisible = !event.focusState.isFocused and noteTitleState.value.text.isBlank()
                )
            }
            is AddEditNoteEvent.EnteredContent -> {
                _noteContentState.value = noteContentState.value.copy(
                    text = event.value
                )
            }
            is AddEditNoteEvent.ChangedContentFocus -> {
                _noteContentState.value = noteContentState.value.copy(
                    isHintVisible = !event.focusState.isFocused and noteContentState.value.text.isBlank()
                )
            }
            is AddEditNoteEvent.ChangeColor -> {
                _noteColorState.value = event.color
            }
            is AddEditNoteEvent.SaveNote -> {
                viewModelScope.launch {
                    try {
                        noteUseCases.addNote(
                            note = Note(
                                title = noteTitleState.value.text,
                                content = noteContentState.value.text,
                                color = noteColorState.value,
                                timestamp = System.currentTimeMillis(),
                                id = currentNoteId
                            )
                        )
                        _eventFlow.emit(UiEvent.SaveNote)
                    } catch (e: InvalidNoteException) {
                        _eventFlow.emit(
                            UiEvent.ShowSnackbar(
                                message = "Couldn't save note."
                            )
                        )

                    }
                }
            }

        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object SaveNote : UiEvent()
    }


}
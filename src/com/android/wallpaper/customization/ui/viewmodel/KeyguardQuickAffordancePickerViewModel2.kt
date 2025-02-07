/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wallpaper.customization.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.android.customization.module.logging.ThemesUserEventLogger
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordanceSlotViewModel
import com.android.customization.picker.quickaffordance.ui.viewmodel.KeyguardQuickAffordanceSummaryViewModel
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEYGUARD_QUICK_AFFORDANCE_ID_NONE
import com.android.themepicker.R
import com.android.wallpaper.picker.common.button.ui.viewmodel.ButtonStyle
import com.android.wallpaper.picker.common.button.ui.viewmodel.ButtonViewModel
import com.android.wallpaper.picker.common.dialog.ui.viewmodel.DialogViewModel
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

class KeyguardQuickAffordancePickerViewModel2
@AssistedInject
constructor(
    @ApplicationContext private val applicationContext: Context,
    private val quickAffordanceInteractor: KeyguardQuickAffordancePickerInteractor,
    private val logger: ThemesUserEventLogger,
    @Assisted private val viewModelScope: CoroutineScope,
) {
    /** A locally-selected slot, if the user ever switched from the original one. */
    private val _selectedSlotId = MutableStateFlow<String?>(null)
    /** The ID of the selected slot. */
    val selectedSlotId: StateFlow<String> =
        combine(quickAffordanceInteractor.slots, _selectedSlotId) { slots, selectedSlotIdOrNull ->
                if (selectedSlotIdOrNull != null) {
                    slots.first { slot -> slot.id == selectedSlotIdOrNull }
                } else {
                    // If we haven't yet selected a new slot locally, default to the first slot.
                    slots[0]
                }
            }
            .map { selectedSlot -> selectedSlot.id }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = "",
            )
    private val overridingQuickAffordances = MutableStateFlow<Map<String, String>>(emptyMap())
    private val selectedQuickAffordancesGroupBySlotId =
        quickAffordanceInteractor.selections
            .map { it.groupBy { selectionModel -> selectionModel.slotId } }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
    // optimisticUpdateQuickAffordances updates right after applying button is clicked, while the
    // actual update of selectedQuickAffordancesGroupBySlotId later updates until the system
    // completes the update task. This can make sure the apply button state updates before we return
    // to the previous screen.
    private val optimisticUpdateQuickAffordances: MutableStateFlow<Map<String, String>?> =
        MutableStateFlow(null)

    val previewingQuickAffordances =
        combine(
            quickAffordanceInteractor.slots,
            overridingQuickAffordances,
            selectedQuickAffordancesGroupBySlotId,
        ) { slots, overridingQuickAffordances, selectedQuickAffordancesGroupBySlotId ->
            slots.associate { slot ->
                val selectedAffordanceId =
                    overridingQuickAffordances[slot.id]
                        ?: selectedQuickAffordancesGroupBySlotId[slot.id]
                            ?.firstOrNull()
                            ?.affordanceId
                        ?: KEYGUARD_QUICK_AFFORDANCE_ID_NONE
                slot.id to selectedAffordanceId
            }
        }

    fun resetPreview() {
        overridingQuickAffordances.tryEmit(emptyMap())
        _selectedSlotId.tryEmit(SLOT_ID_BOTTOM_START)
    }

    /** View-models for each slot, keyed by slot ID. */
    private val slots: StateFlow<Map<String, KeyguardQuickAffordanceSlotViewModel>> =
        combine(
                quickAffordanceInteractor.slots,
                quickAffordanceInteractor.affordances,
                previewingQuickAffordances,
                selectedSlotId,
            ) { slots, affordances, previewingQuickAffordances, selectedSlotId ->
                slots.associate { slot ->
                    val selectedAffordanceId = previewingQuickAffordances[slot.id]
                    val selectedAffordances =
                        affordances.filter { affordance -> selectedAffordanceId == affordance.id }

                    val isSelected = selectedSlotId == slot.id
                    slot.id to
                        KeyguardQuickAffordanceSlotViewModel(
                            name = getSlotName(slot.id),
                            isSelected = isSelected,
                            selectedQuickAffordances =
                                selectedAffordances.map { affordanceModel ->
                                    OptionItemViewModel<Icon>(
                                        key =
                                            MutableStateFlow("${slot.id}::${affordanceModel.id}")
                                                as StateFlow<String>,
                                        payload =
                                            Icon.Loaded(
                                                drawable =
                                                    getAffordanceIcon(
                                                        affordanceModel.iconResourceId
                                                    ),
                                                contentDescription =
                                                    Text.Loaded(getSlotContentDescription(slot.id)),
                                            ),
                                        text = Text.Loaded(affordanceModel.name),
                                        isSelected = MutableStateFlow(true) as StateFlow<Boolean>,
                                        onClicked = flowOf(null),
                                        onLongClicked = null,
                                        isEnabled = true,
                                    )
                                },
                            maxSelectedQuickAffordances = slot.maxSelectedQuickAffordances,
                            onClicked =
                                if (isSelected) {
                                    null
                                } else {
                                    { _selectedSlotId.tryEmit(slot.id) }
                                },
                        )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = emptyMap(),
            )

    val tabs: Flow<List<FloatingToolbarTabViewModel>> =
        slots.map { slotById ->
            slotById.values.map {
                FloatingToolbarTabViewModel(it.getIcon(), it.name, it.isSelected, it.onClicked)
            }
        }

    /** The list of all available quick affordances for the selected slot. */
    val quickAffordances: Flow<List<OptionItemViewModel2<Icon>>> =
        quickAffordanceInteractor.affordances.map { affordances ->
            val isNoneSelected =
                combine(selectedSlotId, previewingQuickAffordances) {
                        selectedSlotId,
                        previewingQuickAffordances ->
                        previewingQuickAffordances[selectedSlotId] ==
                            KEYGUARD_QUICK_AFFORDANCE_ID_NONE
                    }
                    .stateIn(viewModelScope)
            listOf(
                none(
                    slotId = selectedSlotId,
                    isSelected = isNoneSelected,
                    onSelected =
                        combine(isNoneSelected, selectedSlotId) { isSelected, selectedSlotId ->
                            if (!isSelected) {
                                {
                                    val newMap =
                                        overridingQuickAffordances.value.toMutableMap().apply {
                                            put(selectedSlotId, KEYGUARD_QUICK_AFFORDANCE_ID_NONE)
                                        }
                                    overridingQuickAffordances.tryEmit(newMap)
                                }
                            } else {
                                null
                            }
                        },
                )
            ) +
                affordances.map { affordance ->
                    val affordanceIcon = getAffordanceIcon(affordance.iconResourceId)
                    val isSelectedFlow: StateFlow<Boolean> =
                        combine(selectedSlotId, previewingQuickAffordances) {
                                selectedSlotId,
                                previewingQuickAffordances ->
                                previewingQuickAffordances[selectedSlotId] == affordance.id
                            }
                            .stateIn(viewModelScope)
                    OptionItemViewModel2<Icon>(
                        key =
                            selectedSlotId
                                .map { slotId -> "$slotId::${affordance.id}" }
                                .stateIn(viewModelScope),
                        payload = Icon.Loaded(drawable = affordanceIcon, contentDescription = null),
                        text = Text.Loaded(affordance.name),
                        isSelected = isSelectedFlow,
                        onClicked =
                            if (affordance.isEnabled) {
                                combine(isSelectedFlow, selectedSlotId) { isSelected, selectedSlotId
                                    ->
                                    if (!isSelected) {
                                        {
                                            val newMap =
                                                overridingQuickAffordances.value
                                                    .toMutableMap()
                                                    .apply { put(selectedSlotId, affordance.id) }
                                            overridingQuickAffordances.tryEmit(newMap)
                                        }
                                    } else {
                                        null
                                    }
                                }
                            } else {
                                flowOf {
                                    showEnablementDialog(
                                        icon = affordanceIcon,
                                        name = affordance.name,
                                        explanation = affordance.enablementExplanation,
                                        actionText = affordance.enablementActionText,
                                        actionIntent = affordance.enablementActionIntent,
                                    )
                                }
                            },
                        onLongClicked =
                            if (affordance.configureIntent != null) {
                                { requestActivityStart(affordance.configureIntent) }
                            } else {
                                null
                            },
                        isEnabled = affordance.isEnabled,
                    )
                }
        }

    val onApply: Flow<(suspend () -> Unit)?> =
        combine(overridingQuickAffordances, optimisticUpdateQuickAffordances) {
            overridingQuickAffordances,
            optimisticUpdateQuickAffordances ->
            // If all overridingQuickAffordances is empty or are same as the
            // optimisticUpdateQuickAffordances, it is not yet edited
            val isQuickAffordancesEdited =
                (!overridingQuickAffordances.all { (slotId, overridingQuickAffordanceId) ->
                    optimisticUpdateQuickAffordances?.get(slotId) == overridingQuickAffordanceId
                })
            if (isQuickAffordancesEdited) {
                {
                    this.optimisticUpdateQuickAffordances.value = overridingQuickAffordances
                    overridingQuickAffordances.forEach { entry ->
                        val slotId = entry.key
                        val affordanceId = entry.value
                        if (slotId == KEYGUARD_QUICK_AFFORDANCE_ID_NONE) {
                            quickAffordanceInteractor.unselectAllFromSlot(slotId)
                        } else {
                            quickAffordanceInteractor.select(
                                slotId = slotId,
                                affordanceId = affordanceId,
                            )
                        }
                        logger.logShortcutApplied(shortcut = affordanceId, shortcutSlotId = slotId)
                    }
                }
            } else {
                null
            }
        }

    private val _dialog = MutableStateFlow<DialogViewModel?>(null)
    /**
     * The current dialog to show. If `null`, no dialog should be shown.
     *
     * When the dialog is dismissed, [onDialogDismissed] must be called.
     */
    val dialog: Flow<DialogViewModel?> = _dialog.asStateFlow()

    private val _activityStartRequests = MutableStateFlow<Intent?>(null)
    /**
     * Requests to start an activity with the given [Intent].
     *
     * Important: once the activity is started, the [Intent] should be consumed by calling
     * [onActivityStarted].
     */
    val activityStartRequests: StateFlow<Intent?> = _activityStartRequests.asStateFlow()

    /** Notifies that the dialog has been dismissed in the UI. */
    fun onDialogDismissed() {
        _dialog.value = null
    }

    /**
     * Notifies that an activity request from [activityStartRequests] has been fulfilled (e.g. the
     * activity was started and the view-model can forget needing to start this activity).
     */
    fun onActivityStarted() {
        _activityStartRequests.value = null
    }

    private fun requestActivityStart(intent: Intent) {
        _activityStartRequests.value = intent
    }

    private fun showEnablementDialog(
        icon: Drawable,
        name: String,
        explanation: String,
        actionText: String?,
        actionIntent: Intent?,
    ) {
        _dialog.value =
            DialogViewModel(
                icon = Icon.Loaded(drawable = icon, contentDescription = null),
                headline = Text.Resource(R.string.keyguard_affordance_enablement_dialog_headline),
                message = Text.Loaded(explanation),
                buttons =
                    buildList {
                        add(
                            ButtonViewModel(
                                text =
                                    Text.Resource(
                                        if (actionText != null) {
                                            // This is not the only button on the dialog.
                                            R.string.cancel
                                        } else {
                                            // This is the only button on the dialog.
                                            R.string
                                                .keyguard_affordance_enablement_dialog_dismiss_button
                                        }
                                    ),
                                style = ButtonStyle.Secondary,
                            )
                        )

                        if (actionText != null) {
                            add(
                                ButtonViewModel(
                                    text = Text.Loaded(actionText),
                                    style = ButtonStyle.Primary,
                                    onClicked = {
                                        actionIntent?.let { intent -> requestActivityStart(intent) }
                                    },
                                )
                            )
                        }
                    },
            )
    }

    /** Returns a view-model for the special "None" option. */
    @SuppressLint("UseCompatLoadingForDrawables")
    private suspend fun none(
        slotId: StateFlow<String>,
        isSelected: StateFlow<Boolean>,
        onSelected: Flow<(() -> Unit)?>,
    ): OptionItemViewModel2<Icon> {
        return OptionItemViewModel2<Icon>(
            key = slotId.map { "$it::none" }.stateIn(viewModelScope),
            payload = Icon.Resource(res = R.drawable.link_off, contentDescription = null),
            text = Text.Resource(res = R.string.keyguard_affordance_none),
            isSelected = isSelected,
            onClicked = onSelected,
            onLongClicked = null,
            isEnabled = true,
        )
    }

    private fun getSlotName(slotId: String): String {
        return applicationContext.getString(
            when (slotId) {
                SLOT_ID_BOTTOM_START -> R.string.keyguard_slot_name_bottom_start
                SLOT_ID_BOTTOM_END -> R.string.keyguard_slot_name_bottom_end
                else -> error("No name for slot with ID of \"$slotId\"!")
            }
        )
    }

    private fun getSlotContentDescription(slotId: String): String {
        return applicationContext.getString(
            when (slotId) {
                SLOT_ID_BOTTOM_START -> R.string.keyguard_slot_name_bottom_start
                SLOT_ID_BOTTOM_END -> R.string.keyguard_slot_name_bottom_end
                else -> error("No accessibility label for slot with ID \"$slotId\"!")
            }
        )
    }

    private suspend fun getAffordanceIcon(@DrawableRes iconResourceId: Int): Drawable {
        return quickAffordanceInteractor.getAffordanceIcon(iconResourceId)
    }

    val summary: Flow<KeyguardQuickAffordanceSummaryViewModel> =
        slots.map { slots ->
            val icon2 =
                (slots[SLOT_ID_BOTTOM_END]?.selectedQuickAffordances?.firstOrNull())?.payload
            val icon1 =
                (slots[SLOT_ID_BOTTOM_START]?.selectedQuickAffordances?.firstOrNull())?.payload

            KeyguardQuickAffordanceSummaryViewModel(
                description = toDescriptionText(applicationContext, slots),
                icon1 =
                    icon1
                        ?: if (icon2 == null) {
                            Icon.Resource(res = R.drawable.link_off, contentDescription = null)
                        } else {
                            null
                        },
                icon2 = icon2,
            )
        }

    private fun toDescriptionText(
        context: Context,
        slots: Map<String, KeyguardQuickAffordanceSlotViewModel>,
    ): Text {
        val bottomStartAffordanceName =
            slots[SLOT_ID_BOTTOM_START]?.selectedQuickAffordances?.firstOrNull()?.text
        val bottomEndAffordanceName =
            slots[SLOT_ID_BOTTOM_END]?.selectedQuickAffordances?.firstOrNull()?.text

        return when {
            bottomStartAffordanceName != null && bottomEndAffordanceName != null -> {
                Text.Loaded(
                    context.getString(
                        R.string.keyguard_quick_affordance_two_selected_template,
                        bottomStartAffordanceName.asString(context),
                        bottomEndAffordanceName.asString(context),
                    )
                )
            }
            bottomStartAffordanceName != null -> bottomStartAffordanceName
            bottomEndAffordanceName != null -> bottomEndAffordanceName
            else -> Text.Resource(R.string.keyguard_quick_affordance_none_selected)
        }
    }

    companion object {
        private fun KeyguardQuickAffordanceSlotViewModel.getIcon(): Icon =
            selectedQuickAffordances.firstOrNull()?.payload
                ?: Icon.Resource(res = R.drawable.link_off, contentDescription = null)
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): KeyguardQuickAffordancePickerViewModel2
    }
}

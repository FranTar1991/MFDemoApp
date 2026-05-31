package com.franktardencilla.mfdemoapp

import com.franktardencilla.mfdemoapp.domain.model.SaleState
import com.franktardencilla.mfdemoapp.domain.model.SaleStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SaleStateMachineTest {
    @Test
    fun transitionTo_allowsApprovedEmvOnlineFlowInOrder() {
        val stateMachine = SaleStateMachine()

        stateMachine.transitionTo(SaleState.CHECKING_READINESS)
        stateMachine.transitionTo(SaleState.WAITING_FOR_CARD)
        stateMachine.transitionTo(SaleState.CARD_DETECTED)
        stateMachine.transitionTo(SaleState.READING_EMV)
        stateMachine.transitionTo(SaleState.EMV_DATA_READY)
        stateMachine.transitionTo(SaleState.WAITING_FOR_HOST)
        val finalTransition = stateMachine.transitionTo(SaleState.APPROVED)

        assertEquals(SaleState.APPROVED, finalTransition.state)
        assertEquals(SaleState.APPROVED, stateMachine.currentState)
    }

    @Test
    fun transitionTo_rejectsSkippingRequiredCardAndEmvStates() {
        val stateMachine = SaleStateMachine()

        val error = assertThrows(IllegalArgumentException::class.java) {
            stateMachine.transitionTo(SaleState.WAITING_FOR_HOST)
        }

        assertEquals(
            "Invalid sale transition from IDLE to WAITING_FOR_HOST.",
            error.message
        )
        assertEquals(SaleState.IDLE, stateMachine.currentState)
    }

    @Test
    fun transitionTo_allowsCancelFromAnyActiveStateAndResetAfterTerminalState() {
        val stateMachine = SaleStateMachine()

        stateMachine.transitionTo(SaleState.CHECKING_READINESS)
        stateMachine.transitionTo(SaleState.WAITING_FOR_CARD)
        stateMachine.transitionTo(SaleState.CANCELED)
        assertEquals(SaleState.CANCELED, stateMachine.currentState)

        stateMachine.transitionTo(SaleState.IDLE)
        assertEquals(SaleState.IDLE, stateMachine.currentState)
    }
}

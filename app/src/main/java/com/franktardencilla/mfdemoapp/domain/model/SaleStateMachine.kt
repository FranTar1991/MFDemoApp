package com.franktardencilla.mfdemoapp.domain.model

class SaleStateMachine {
    var currentState: SaleState = SaleState.IDLE
        private set

    fun transitionTo(
        nextState: SaleState,
        message: String = nextState.displayName
    ): SaleStateTransition {
        require(isValidTransition(currentState, nextState)) {
            "Invalid sale transition from $currentState to $nextState."
        }
        currentState = nextState
        return SaleStateTransition(
            state = currentState,
            message = message
        )
    }

    fun reset(): SaleStateTransition {
        currentState = SaleState.IDLE
        return SaleStateTransition(
            state = currentState,
            message = currentState.displayName
        )
    }

    private fun isValidTransition(
        from: SaleState,
        to: SaleState
    ): Boolean {
        if (to == SaleState.ERROR || to == SaleState.CANCELED) {
            return true
        }

        return when (from) {
            SaleState.IDLE -> to == SaleState.CHECKING_READINESS
            SaleState.CHECKING_READINESS -> to == SaleState.WAITING_FOR_CARD
            SaleState.WAITING_FOR_CARD -> to == SaleState.CARD_DETECTED
            SaleState.CARD_DETECTED -> to == SaleState.READING_EMV
            SaleState.READING_EMV -> to == SaleState.EMV_DATA_READY
            SaleState.EMV_DATA_READY -> to == SaleState.WAITING_FOR_HOST
            SaleState.WAITING_FOR_HOST -> to == SaleState.APPROVED || to == SaleState.DECLINED
            SaleState.APPROVED,
            SaleState.DECLINED,
            SaleState.CANCELED,
            SaleState.ERROR -> to == SaleState.IDLE
        }
    }
}

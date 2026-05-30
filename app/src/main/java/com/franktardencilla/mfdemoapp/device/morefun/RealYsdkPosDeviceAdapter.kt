package com.franktardencilla.mfdemoapp.device.morefun

import android.os.Bundle
import android.os.RemoteException
import android.graphics.Bitmap
import com.franktardencilla.mfdemoapp.device.HostClient
import com.franktardencilla.mfdemoapp.device.PedMacResult
import com.franktardencilla.mfdemoapp.device.PosDeviceAdapter
import com.franktardencilla.mfdemoapp.device.PrintResult
import com.franktardencilla.mfdemoapp.device.SaleDeviceResult
import com.franktardencilla.mfdemoapp.device.SaleEvent
import com.franktardencilla.mfdemoapp.device.SaleEventSink
import com.franktardencilla.mfdemoapp.device.TrackAKeyInjectionEvent
import com.franktardencilla.mfdemoapp.device.TrackAKeyInjectionEventSink
import com.franktardencilla.mfdemoapp.domain.model.CardEntryMode
import com.franktardencilla.mfdemoapp.domain.model.DeviceConnectionStatus
import com.franktardencilla.mfdemoapp.domain.model.EmvTag
import com.franktardencilla.mfdemoapp.domain.model.EmvTagSummary
import com.franktardencilla.mfdemoapp.domain.model.Field55Data
import com.franktardencilla.mfdemoapp.domain.model.HostSaleResponse
import com.franktardencilla.mfdemoapp.domain.model.Iso8583Message
import com.franktardencilla.mfdemoapp.domain.model.Iso8583Packager
import com.franktardencilla.mfdemoapp.domain.model.KeyReadinessStatus
import com.franktardencilla.mfdemoapp.domain.model.KeySlotMetadata
import com.franktardencilla.mfdemoapp.domain.model.KeyStatus
import com.franktardencilla.mfdemoapp.domain.model.KeyType
import com.franktardencilla.mfdemoapp.domain.model.MaskedPan
import com.franktardencilla.mfdemoapp.domain.model.SaleIsoRequestBuilder
import com.franktardencilla.mfdemoapp.domain.model.SaleRequest
import com.franktardencilla.mfdemoapp.domain.model.SaleResult
import com.franktardencilla.mfdemoapp.domain.model.SaleState
import com.franktardencilla.mfdemoapp.domain.model.TrackAKeyInjectionRequest
import com.morefun.yapi.device.ped.IPed
import com.morefun.yapi.device.ped.PedCipher
import com.morefun.yapi.device.beeper.BeepModeConstrants
import com.franktardencilla.mfdemoapp.domain.model.TransactionStatus
import com.morefun.yapi.ServiceResult
import com.morefun.yapi.device.pinpad.OnPinPadInputListener
import com.morefun.yapi.device.pinpad.DesAlgorithmType
import com.morefun.yapi.device.pinpad.CheckKeyEnum
import com.morefun.yapi.device.pinpad.MacAlgorithmType
import com.morefun.yapi.device.pinpad.PinAlgorithmMode
import com.morefun.yapi.device.pinpad.PinPadConstrants
import com.morefun.yapi.device.pinpad.PinPadType
import com.morefun.yapi.device.pinpad.TDesKeyObj
import com.morefun.yapi.device.pinpad.WorkKeyType
import com.morefun.yapi.device.printer.OnPrintListener
import com.morefun.yapi.device.printer.PrinterConfig
import com.morefun.yapi.device.reader.mag.MagCardInfoEntity
import com.morefun.yapi.device.reader.mag.OnSearchMagCardListener
import com.morefun.yapi.emv.EmvHandler
import com.morefun.yapi.emv.EmvErrorConstrants
import com.morefun.yapi.emv.EmvOnlineResult
import com.morefun.yapi.emv.EmvTermCfgConstrants
import com.morefun.yapi.emv.EmvTransDataConstrants
import com.morefun.yapi.emv.OnEmvProcessListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import com.morefun.yapi.device.ped.KeyType as MorefunKeyType

class RealYsdkPosDeviceAdapter(
    private val serviceManager: MorefunDeviceServiceManager,
    private val hostClient: HostClient,
    private val saleIsoRequestBuilder: SaleIsoRequestBuilder
) : PosDeviceAdapter {
    private val callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var saleCanceled = false
    @Volatile
    private var keyBackend: KeyBackend? = null

    override suspend fun connect(): DeviceConnectionStatus {
        return serviceManager.login()
    }

    override suspend fun disconnect() {
        serviceManager.logout()
    }

    override suspend fun getConnectionStatus(): DeviceConnectionStatus {
        return serviceManager.getStatus()
    }

    override suspend fun getKeyStatus(): KeyStatus {
        return runCatching {
            readPinPadKeyStatusOrNull(
                detail = "Real PinPad KEK/MK/SK keys: checked through the MoreFun demo flow."
            )?.let { status ->
                if (status.isReady) {
                    keyBackend = KeyBackend.PINPAD
                }
                return@runCatching status
            }

            val ped = serviceManager.requireEngine().getPed()
                ?: throw RemoteException("PED service unavailable.")
            val slotChecks = TRACK_A_SLOTS.map { trackedSlot ->
                ped.describeTrackedSlot(trackedSlot)
            }
            val slots = TRACK_A_SLOTS.mapNotNull { trackedSlot ->
                ped.slotMetadataOrNull(trackedSlot.keyType, trackedSlot.slot)
            }
            val hasMaster = slots.any { it.keyType == KeyType.MASTER }
            val hasMac = slots.any { it.keyType == KeyType.MAC }
            val hasPin = slots.any { it.keyType == KeyType.PIN }
            val pedStatus = KeyStatus(
                readiness = if (hasMaster && hasMac) {
                    KeyReadinessStatus.READY
                } else {
                    KeyReadinessStatus.NOT_READY
                },
                slots = slots,
                message = buildString {
                    append("Real PED keys: ")
                    append(if (hasMaster) "master loaded" else "master missing")
                    append(", ")
                    append(if (hasMac) "MAC loaded" else "MAC missing")
                    append(", ")
                    append(if (hasPin) "PIN loaded" else "PIN optional missing")
                    append("\n")
                    append(slotChecks.joinToString("\n"))
                }
            )
            if (!pedStatus.isReady) {
                readPinPadKeyStatusOrNull(
                    detail = "Real PinPad KEK/MK/SK keys: checked through the MoreFun demo flow."
                )?.let { pinPadStatus ->
                    if (pinPadStatus.isReady) {
                        keyBackend = KeyBackend.PINPAD
                        return@runCatching pinPadStatus
                    }
                }
            }
            pedStatus
        }.getOrElse { error ->
            readPinPadKeyStatusOrNull(
                detail = "Real PinPad KEK/MK/SK keys: checked through the MoreFun demo flow."
            )?.let { pinPadStatus ->
                if (pinPadStatus.isReady) {
                    keyBackend = KeyBackend.PINPAD
                    return@getOrElse pinPadStatus
                }
            }
            KeyStatus(
                readiness = KeyReadinessStatus.UNKNOWN,
                message = "Could not read real PED key status: ${error.message ?: "unknown error"}"
            )
        }
    }

    override suspend fun injectTrackAKeys(
        request: TrackAKeyInjectionRequest,
        events: TrackAKeyInjectionEventSink
    ): KeyStatus {
        events.onEvent(
            TrackAKeyInjectionEvent.Progress(
                "Using PinPad KEK/MK/SK flow from the MoreFun demo"
            )
        )
        val pinPadLogin = serviceManager.loginWithBusinessId(PINPAD_BUSINESS_ID)
        events.onEvent(TrackAKeyInjectionEvent.Progress(pinPadLogin.message))
        if (!pinPadLogin.isConnected) {
            return KeyStatus(
                readiness = KeyReadinessStatus.NOT_READY,
                message = pinPadLogin.message
            )
        }
        return loadPinPadDemoKeys(events)
    }

    override suspend fun clearKeys(): KeyStatus {
        return runCatching {
            val engine = serviceManager.requireEngine()
            val pinPadResults = engine.getPinPad()
                ?.clearDemoPinPadKeys()
                ?: listOf("PinPad service unavailable; no PinPad keys deleted.")
            val pedResults = engine.getPed()
                ?.clearLegacyPedKeys()
                ?: listOf("PED service unavailable; no legacy PED keys deleted.")
            keyBackend = null
            KeyStatus(
                readiness = KeyReadinessStatus.CLEARED,
                message = buildString {
                    append("Real Track A key slots cleared.")
                    append("\n")
                    append(pinPadResults.joinToString("\n"))
                    append("\n")
                    append(pedResults.joinToString("\n"))
                }
            )
        }.getOrElse { error ->
            KeyStatus(
                readiness = KeyReadinessStatus.UNKNOWN,
                message = "Could not clear real Track A keys: ${error.message ?: "unknown error"}"
            )
        }
    }

    override suspend fun startSale(
        request: SaleRequest,
        events: SaleEventSink
    ): SaleDeviceResult {
        saleCanceled = false
        var entryMode = CardEntryMode.CONTACT
        var emvTagSummary = EmvTagSummary()
        return runCatching {
            val engine = serviceManager.requireEngine()
            val emvHandler = engine.getEmvHandler()
                ?: throw RemoteException("EMV service unavailable.")

            events.onEvent(SaleEvent.StateChanged(SaleState.WAITING_FOR_CARD, "EmvHandler.emvTrans waiting for card"))
            val finishResult = CompletableDeferred<RealEmvFinish>()
            val magStripeResult = CompletableDeferred<SaleDeviceResult>()
            val hostAuthorizationResult = CompletableDeferred<Result<HostSaleResponse>>()
            val onlineProcessingStarted = AtomicBoolean(false)
            val cardReadBeeped = AtomicBoolean(false)
            beep(BeepModeConstrants.NORMAL)
            startMagStripeSearch(request, events, magStripeResult)
            emvHandler.initTermConfig(buildTermConfig())
            val startResult = emvHandler.emvTrans(
                buildTransBundle(request),
                buildEmvListener(
                    emvHandler = emvHandler,
                    request = request,
                    events = events,
                    onEntryMode = { detectedEntryMode ->
                        entryMode = detectedEntryMode
                    },
                    onCardRead = {
                        if (cardReadBeeped.compareAndSet(false, true)) {
                            beep(BeepModeConstrants.SUCCESS)
                        }
                    },
                    onEmvTags = { summary ->
                        emvTagSummary = summary
                    },
                    onHostResult = { result ->
                        if (!hostAuthorizationResult.isCompleted) {
                            hostAuthorizationResult.complete(result)
                        }
                    },
                    onOnlineProcessingStarted = {
                        onlineProcessingStarted.set(true)
                    },
                    onFinish = { result ->
                        finishResult.complete(result)
                    }
                )
            )
            if (startResult != ServiceResult.Success) {
                return SaleDeviceResult.Failed("EmvHandler.emvTrans failed to start. Result code: $startResult")
            }

            val selectedResult = withTimeoutOrNull(REAL_EMV_TIMEOUT_MILLIS) {
                select<RealSaleKernelResult> {
                    finishResult.onAwait { RealSaleKernelResult.EmvFinished(it) }
                    magStripeResult.onAwait { RealSaleKernelResult.MagStripeFinished(it) }
                }
            } ?: return SaleDeviceResult.Failed("Card reading timed out.")

            val finish = when (selectedResult) {
                is RealSaleKernelResult.MagStripeFinished -> {
                    runCatching { emvHandler.endPBOC() }
                    return selectedResult.result
                }
                is RealSaleKernelResult.EmvFinished -> selectedResult.finish
            }

            if (saleCanceled || finish.retCode == ServiceResult.Emv_Cancel) {
                return SaleDeviceResult.Canceled
            }
            if (!onlineProcessingStarted.get() && finish.retCode != ServiceResult.Success) {
                return SaleDeviceResult.Failed(
                    "EMV transaction terminated before host authorization. " +
                        finish.describe()
                )
            }

            val completedHostResponse = withTimeoutOrNull(HOST_AUTHORIZATION_TIMEOUT_MILLIS) {
                hostAuthorizationResult.await()
            }?.getOrElse { error ->
                return SaleDeviceResult.Failed(error.message ?: "Host authorization failed.")
            } ?: return SaleDeviceResult.Failed(
                "EMV online processing did not complete host authorization. " +
                    "The EMV kernel finished before a host response was received."
            )
            events.onEvent(SaleEvent.EmvDataReady(emvTagSummary))
            events.onEvent(SaleEvent.StateChanged(SaleState.WAITING_FOR_HOST, "Host simulator responded during onOnlineProc"))

            SaleDeviceResult.Completed(
                SaleResult(
                    status = if (completedHostResponse.isApproved && finish.retCode == ServiceResult.Success) {
                        TransactionStatus.APPROVED
                    } else {
                        TransactionStatus.DECLINED
                    },
                    amount = request.amount,
                    amountBreakdown = request.amountBreakdown,
                    stan = completedHostResponse.responseSummary.stan,
                    entryMode = entryMode,
                    maskedPan = emvTagSummary.maskedPan,
                    responseCode = completedHostResponse.responseSummary.responseCode,
                    authCode = completedHostResponse.responseSummary.authCode,
                    emvTagSummary = emvTagSummary,
                    isoRequest = completedHostResponse.requestSummary,
                    isoResponse = completedHostResponse.responseSummary,
                    message = if (completedHostResponse.isApproved) {
                        "Host approved sale. Auth: ${completedHostResponse.responseSummary.authCode ?: "none"}"
                    } else {
                        completedHostResponse.toDeclineMessage()
                    }
                )
            )
        }.getOrElse { error ->
            SaleDeviceResult.Failed("Real YSDK sale failed: ${error.message ?: "unknown error"}")
        }.also {
            runCatching {
                serviceManager.requireEngine().getMagCardReader()?.stopSearch()
                serviceManager.requireEngine().getEmvHandler()?.endPBOC()
            }
        }
    }

    override suspend fun cancelCurrentOperation() {
        saleCanceled = true
        runCatching {
            serviceManager.requireEngine().getMagCardReader()?.stopSearch()
            serviceManager.requireEngine().getEmvHandler()?.endPBOC()
        }
    }

    override suspend fun printVoucher(voucherBitmap: Bitmap): PrintResult {
        return runCatching {
            val printer = serviceManager.requireEngine().getMultipleAppPrinter()
                ?: return PrintResult(
                    isSuccess = false,
                    message = "Printer service unavailable."
                )
            val printResult = CompletableDeferred<Int>()
            val startResult = printer.printImage(
                voucherBitmap,
                object : OnPrintListener.Stub() {
                    override fun onPrintResult(retCode: Int) {
                        if (!printResult.isCompleted) {
                            printResult.complete(retCode)
                        }
                    }
                },
                Bundle().apply {
                    putInt(PrinterConfig.COMMON_GRAYLEVEL, PrinterConfig.PRINT_DENSITY_NORMAL)
                }
            )
            if (startResult != ServiceResult.Success) {
                return PrintResult(
                    isSuccess = false,
                    message = "Printer did not start. Result code: $startResult (${startResult.describeServiceResult()})"
                )
            }

            val result = withTimeoutOrNull(PRINT_TIMEOUT_MILLIS) {
                printResult.await()
            } ?: return PrintResult(
                isSuccess = false,
                message = "Printer timed out before returning a result."
            )

            if (result == ServiceResult.Success) {
                PrintResult(
                    isSuccess = true,
                    message = "Voucher printed successfully."
                )
            } else {
                PrintResult(
                    isSuccess = false,
                    message = "Printer failed. Result code: $result (${result.describeServiceResult()})"
                )
            }
        }.getOrElse { error ->
            PrintResult(
                isSuccess = false,
                message = "Voucher print failed: ${error.message ?: "unknown error"}"
            )
        }
    }

    private fun buildEmvListener(
        emvHandler: EmvHandler,
        request: SaleRequest,
        events: SaleEventSink,
        onEntryMode: (CardEntryMode) -> Unit,
        onCardRead: () -> Unit,
        onEmvTags: (EmvTagSummary) -> Unit,
        onHostResult: (Result<HostSaleResponse>) -> Unit,
        onOnlineProcessingStarted: () -> Unit,
        onFinish: (RealEmvFinish) -> Unit
    ): OnEmvProcessListener {
        var currentEntryMode = CardEntryMode.CONTACT
        return object : OnEmvProcessListener.Stub() {
            override fun onSelApp(appNameList: MutableList<String>, isFirstSelect: Boolean) {
                events.onEvent(
                    SaleEvent.Progress("onSelApp apps=${appNameList.joinToString()} first=$isFirstSelect")
                )
                runCatching {
                    emvHandler.onSetSelAppResponse(FIRST_APP_INDEX)
                }
            }

            override fun onConfirmCardNo(cardNo: String) {
                events.onEvent(SaleEvent.Progress("onConfirmCardNo received masked PAN"))
                runCatching {
                    emvHandler.onSetConfirmCardNoResponse(true)
                }
            }

            override fun onCardHolderInputPin(isOnlinePin: Boolean, leftTimes: Int) {
                events.onEvent(
                    SaleEvent.Progress("onCardHolderInputPin online=$isOnlinePin leftTimes=$leftTimes")
                )
                callbackScope.launch {
                    inputOnlinePinIfNeeded(
                        emvHandler = emvHandler,
                        pan = readPan(emvHandler),
                        amountText = request.amount.toDecimalText(),
                        isOnlinePin = isOnlinePin
                    )
                }
            }

            override fun onPinPress(keyCode: Byte) = Unit

            override fun onCertVerify(certName: String, certInfo: String) {
                runCatching {
                    emvHandler.onSetCertVerifyResponse(true)
                }
            }

            override fun onOnlineProc(data: Bundle) {
                onOnlineProcessingStarted()
                events.onEvent(SaleEvent.Progress("onOnlineProc received EMV online request"))
                val onlineResult = runBlocking {
                    runCatching {
                            val summary = readEmvSummary(emvHandler)
                            onEmvTags(summary)
                            val field55Data = Field55Data(
                                tlvHex = readField55(emvHandler),
                                includedTags = FIELD_55_TAGS.toList()
                            )
                            events.onEvent(
                                SaleEvent.StateChanged(
                                    SaleState.EMV_DATA_READY,
                                    "EMV data ready for online authorization"
                                )
                            )
                            events.onEvent(
                                SaleEvent.Progress(
                                    "EMV online data ready DE55 length=${field55Data.tlvHex.length} amount=${request.amount.isoAmount12()}"
                                )
                            )
                            val isoRequest = saleIsoRequestBuilder.build(
                                request = request,
                                entryMode = currentEntryMode,
                                field55Data = field55Data
                            )
                            val macKeySlot = requireLoadedMacKeySlot()
                            events.onEvent(SaleEvent.Progress("calcMac(MAC, slot=$macKeySlot)"))
                            val macResult = calcMac(
                                macKeySlot = macKeySlot,
                                dataHex = Iso8583Packager.pack(isoRequest).toHex()
                            )
                            val macHex = when (macResult) {
                                is PedMacResult.Calculated -> macResult.macHex
                                is PedMacResult.Failed -> error(macResult.message)
                            }
                            val securedIsoRequest = isoRequest.withField(ISO_FIELD_MAC, macHex)
                            events.onEvent(SaleEvent.Progress("ISO8583 field 64 MAC attached"))
                            events.onEvent(SaleEvent.Progress("Sending ISO8583 authorization to host simulator"))
                            val response = hostClient.authorizeSale(securedIsoRequest)
                            events.onEvent(SaleEvent.IsoRequestReady(response.requestSummary))
                            events.onEvent(SaleEvent.IsoResponseReady(response.responseSummary))
                            response
                        }
                }

                val response = onlineResult.getOrNull()
                val onlineBundle = Bundle().apply {
                    val responseCode = response?.responseSummary?.responseCode ?: RESPONSE_CODE_ERROR
                    putString(EmvOnlineResult.REJCODE, responseCode)
                    response?.responseSummary?.authCode?.let { authCode ->
                        putString(EmvOnlineResult.AUTHCODE, authCode)
                    }
                    putByteArray(EmvOnlineResult.RECVARPC_DATA, buildIssuerAuthData(responseCode))
                }
                runCatching {
                    emvHandler.onSetOnlineProcResponse(ServiceResult.Success, onlineBundle)
                }
                onlineResult
                    .onSuccess { response ->
                        onHostResult(Result.success(response))
                    }
                    .onFailure { error ->
                        val message = "Host authorization failed: ${error.message ?: "unknown error"}"
                        onHostResult(Result.failure(IllegalStateException(message, error)))
                        events.onEvent(SaleEvent.Error(message))
                    }
            }

            override fun onContactlessOnlinePlaceCardMode(mode: Int) {
                runCatching {
                    emvHandler.onSetContactlessOnlinePlaceCardModeResponse(true)
                }
            }

            override fun onFinish(retCode: Int, data: Bundle) {
                events.onEvent(SaleEvent.Progress("onFinish retCode=$retCode"))
                onFinish(RealEmvFinish(retCode = retCode, data = data))
            }

            override fun onSetAIDParameter(aid: String) = Unit

            override fun onSetCAPubkey(rid: String, index: Int, algMode: Int) = Unit

            override fun onTRiskManage(pan: String, panSn: String) = Unit

            override fun onSelectLanguage(language: String) = Unit

            override fun onSelectAccountType(accountTypes: MutableList<String>) = Unit

            override fun onIssuerVoiceReference(pan: String) = Unit

            override fun onDisplayOfflinePin(retCode: Int) = Unit

            override fun inputAmount(type: Int) {
                runCatching {
                    emvHandler.onSetInputAmountResponse(request.amount.toDecimalText())
                }
            }

            override fun onGetCardResult(retCode: Int, bundle: Bundle) {
                val entryMode = when (bundle.getInt(ICC_CARD_OTHER_KEY, ICC_CONTACT_CARD)) {
                    ICC_CONTACTLESS_CARD -> CardEntryMode.CONTACTLESS
                    else -> CardEntryMode.CONTACT
                }
                currentEntryMode = entryMode
                onEntryMode(entryMode)
                onCardRead()
                events.onEvent(
                    SaleEvent.StateChanged(SaleState.CARD_DETECTED, "Card detected: ${entryMode.displayName}")
                )
                events.onEvent(SaleEvent.StateChanged(SaleState.READING_EMV, "EmvHandler.emvTrans processing card"))
            }

            override fun onDisplayMessage() {
                runCatching {
                    emvHandler.onSetConfirmDisplayMessage(0)
                }
            }

            override fun onUpdateServiceAmount(serviceRelatedData: String) = Unit

            override fun onCheckServiceBlackList(pan: String, amount: String) = Unit

            override fun onGetServiceDirectory(directory: ByteArray) {
                runCatching {
                    emvHandler.onGetServiceDirectory(ServiceResult.Success)
                }
            }

            override fun onRupayCallback(type: Int, bundle: Bundle) {
                runCatching {
                    emvHandler.onSetRupayCallback(type, Bundle().apply { putInt("retCode", ServiceResult.Success) })
                }
            }

            override fun onEmvKernelCallback(type: Int, index: Int, bundle: Bundle) = Unit
        }
    }

    private suspend fun startMagStripeSearch(
        request: SaleRequest,
        events: SaleEventSink,
        result: CompletableDeferred<SaleDeviceResult>
    ) {
        val magCardReader = serviceManager.requireEngine().getMagCardReader()
        if (magCardReader == null) {
            events.onEvent(SaleEvent.Progress("Magstripe reader unavailable; chip/contactless still enabled"))
            return
        }

        magCardReader.setIsCheckLrc(false)
        val startResult = magCardReader.searchCard(
            object : OnSearchMagCardListener.Stub() {
                override fun onSearchResult(
                    retCode: Int,
                    magCardInfo: MagCardInfoEntity?
                ) {
                    if (retCode != ServiceResult.Success || magCardInfo == null) {
                        events.onEvent(
                            SaleEvent.Progress(
                                "Magstripe search result=$retCode (${retCode.describeServiceResult()})"
                            )
                        )
                        return
                    }
                    if (result.isCompleted) {
                        return
                    }
                    beep(BeepModeConstrants.SUCCESS)
                    callbackScope.launch {
                        val saleResult = runCatching {
                            authorizeMagStripeSale(request, magCardInfo, events)
                        }.getOrElse { error ->
                            SaleDeviceResult.Failed(
                                "Magstripe sale failed: ${error.message ?: "unknown error"}"
                            )
                        }
                        result.complete(saleResult)
                    }
                }
            },
            CARD_SEARCH_TIMEOUT_SECONDS,
            Bundle()
        )
        if (startResult == ServiceResult.Success) {
            events.onEvent(SaleEvent.Progress("MagCardReader.searchCard waiting for swipe"))
        } else {
            events.onEvent(
                SaleEvent.Progress(
                    "MagCardReader.searchCard could not start. Result code: $startResult " +
                        "(${startResult.describeServiceResult()})"
                )
            )
        }
    }

    private suspend fun authorizeMagStripeSale(
        request: SaleRequest,
        magCardInfo: MagCardInfoEntity,
        events: SaleEventSink
    ): SaleDeviceResult {
        events.onEvent(SaleEvent.StateChanged(SaleState.CARD_DETECTED, "Card detected: Magstripe"))
        events.onEvent(SaleEvent.StateChanged(SaleState.READING_EMV, "Reading magstripe track data"))

        val maskedPan = magCardInfo.getCardNo()
            ?.takeIf { it.length >= MIN_PAN_LENGTH }
            ?.let { plainPan -> runCatching { MaskedPan.fromPlainPan(plainPan) }.getOrNull() }
        val summary = EmvTagSummary(maskedPan = maskedPan)
        events.onEvent(SaleEvent.Progress("Magstripe track data ready; EMV Field 55 is not used for swipe"))
        events.onEvent(SaleEvent.StateChanged(SaleState.EMV_DATA_READY, "Magstripe data ready for online authorization"))

        val isoRequest = saleIsoRequestBuilder.build(
            request = request,
            entryMode = CardEntryMode.MAGSTRIPE
        ).withOptionalTrack2(magCardInfo.getTk2())

        val macKeySlot = requireLoadedMacKeySlot()
        events.onEvent(SaleEvent.Progress("calcMac(MAC, slot=$macKeySlot)"))
        val macResult = calcMac(
            macKeySlot = macKeySlot,
            dataHex = Iso8583Packager.pack(isoRequest).toHex()
        )
        val macHex = when (macResult) {
            is PedMacResult.Calculated -> macResult.macHex
            is PedMacResult.Failed -> return SaleDeviceResult.Failed(macResult.message)
        }
        val securedIsoRequest = isoRequest.withField(ISO_FIELD_MAC, macHex)
        events.onEvent(SaleEvent.Progress("ISO8583 field 64 MAC attached"))
        events.onEvent(SaleEvent.StateChanged(SaleState.WAITING_FOR_HOST, "Sending magstripe authorization to host simulator"))

        val response = hostClient.authorizeSale(securedIsoRequest)
        events.onEvent(SaleEvent.IsoRequestReady(response.requestSummary))
        events.onEvent(SaleEvent.IsoResponseReady(response.responseSummary))

        return SaleDeviceResult.Completed(
            SaleResult(
                status = if (response.isApproved) TransactionStatus.APPROVED else TransactionStatus.DECLINED,
                amount = request.amount,
                amountBreakdown = request.amountBreakdown,
                stan = response.responseSummary.stan,
                entryMode = CardEntryMode.MAGSTRIPE,
                maskedPan = maskedPan,
                responseCode = response.responseSummary.responseCode,
                authCode = response.responseSummary.authCode,
                emvTagSummary = summary,
                isoRequest = response.requestSummary,
                isoResponse = response.responseSummary,
                message = if (response.isApproved) {
                    "Host approved sale. Auth: ${response.responseSummary.authCode ?: "none"}"
                } else {
                    response.toDeclineMessage()
                }
            )
        )
    }

    private fun Iso8583Message.withOptionalTrack2(track2: String?): Iso8583Message {
        val normalizedTrack2 = track2
            ?.substringBefore("?")
            ?.replace("=", "D")
            ?.filter { character -> character.isDigit() || character == 'D' }
            ?.takeIf { it.isNotBlank() }
        return if (normalizedTrack2 == null) {
            this
        } else {
            withField(ISO_FIELD_TRACK_2, normalizedTrack2)
        }
    }

    suspend fun calcMac(
        macKeySlot: Int,
        dataHex: String
    ): PedMacResult {
        return runCatching {
            if (keyBackend == KeyBackend.PINPAD) {
                val pinPad = serviceManager.requireEngine().getPinPad()
                    ?: throw RemoteException("PinPad service unavailable.")
                val mac = pinPad.getMac(
                    macKeySlot,
                    MacAlgorithmType.ISO9797_ALG3,
                    DesAlgorithmType.TDES,
                    dataHex.hexToBytes(),
                    Bundle()
                )
                return@runCatching if (mac == null || mac.isEmpty()) {
                    PedMacResult.Failed("PinPad.getMac returned an empty MAC.")
                } else {
                    PedMacResult.Calculated(mac.toHex().take(MAC_OUTPUT_LENGTH * 2))
                }
            }
            val ped = serviceManager.requireEngine().getPed()
                ?: throw RemoteException("PED service unavailable.")
            val data = dataHex.hexToBytes()
            val mac = ByteArray(MAC_OUTPUT_LENGTH)
            val result = ped.calcMAC(
                macKeySlot,
                PedCipher.MacFormat.SEC_MAC_X919_FORMAT,
                PedCipher.DesMode.ECB,
                data,
                ByteArray(MAC_OUTPUT_LENGTH),
                mac
            )
            if (result == SERVICE_SUCCESS) {
                PedMacResult.Calculated(mac.toHex())
            } else {
                PedMacResult.Failed("IPed.calcMAC failed. Result code: $result")
            }
        }.getOrElse { error ->
            PedMacResult.Failed("IPed.calcMAC error: ${error.message ?: "unknown error"}")
        }
    }

    private suspend fun inputOnlinePinIfNeeded(
        emvHandler: EmvHandler,
        pan: String,
        amountText: String,
        isOnlinePin: Boolean
    ) {
        if (!isOnlinePin) {
            emvHandler.onSetCardHolderInputPin(null)
            return
        }

        val pinResult = CompletableDeferred<ByteArray?>()
        val pinPad = serviceManager.requireEngine().getPinPad()
            ?: throw RemoteException("PinPad service unavailable.")
        pinPad.setTimeOut(PIN_TIMEOUT_SECONDS)
        pinPad.setSupportPinLen(intArrayOf(PIN_MIN_LENGTH, PIN_MAX_LENGTH))
        val startResult = pinPad.inputOnlinePin(
            Bundle().apply {
                putBoolean(PinPadConstrants.COMMON_NEW_LAYOUT, false)
                putBoolean(PinPadConstrants.COMMON_SUPPORT_KEYVOICE, true)
                putBoolean(PinPadConstrants.COMMON_SUPPORT_BYPASS, false)
                putBoolean(PinPadConstrants.COMMON_IS_RANDOM, true)
                putString(
                    PinPadConstrants.TITLE_HEAD_CONTENT,
                    "Enter online PIN\nAmount: $amountText"
                )
            },
            pan.toByteArray(),
            if (keyBackend == KeyBackend.PINPAD) PINPAD_WORK_KEY_INDEX else PIN_WORKING_KEY_SLOT,
            PinAlgorithmMode.ISO9564FMT1,
            object : OnPinPadInputListener.Stub() {
                override fun onInputResult(
                    retCode: Int,
                    pin: ByteArray?,
                    ksn: String?
                ) {
                    pinResult.complete(
                        if (retCode == ServiceResult.Success) {
                            pin
                        } else {
                            null
                        }
                    )
                }

                override fun onSendKey(keyCode: Byte) {
                    if (keyCode == ServiceResult.PinPad_Input_Cancel.toByte()) {
                        pinResult.complete(null)
                    }
                }
            }
        )
        if (startResult != ServiceResult.Success) {
            emvHandler.onSetCardHolderInputPin(null)
            return
        }

        emvHandler.onSetCardHolderInputPin(
            withTimeoutOrNull(PIN_TIMEOUT_SECONDS * MILLIS_PER_SECOND) {
                pinResult.await()
            }
        )
    }

    private fun buildTermConfig(): Bundle {
        return Bundle().apply {
            putByteArray(EmvTermCfgConstrants.TERMCAP, byteArrayOf(0xE0.toByte(), 0xE0.toByte(), 0xC8.toByte()))
            putByteArray(EmvTermCfgConstrants.ADDTERMCAP, byteArrayOf(0xF2.toByte(), 0x00.toByte(), 0xF0.toByte(), 0xA0.toByte(), 0x01.toByte()))
            putByteArray(EmvTermCfgConstrants.ADD_TERMCAP_EX, byteArrayOf(0xF2.toByte(), 0x00.toByte(), 0xF0.toByte(), 0xA0.toByte(), 0x01.toByte()))
            putByte(EmvTermCfgConstrants.TERMTYPE, 0x22.toByte())
            putByteArray(EmvTermCfgConstrants.COUNTRYCODE, byteArrayOf(0x08.toByte(), 0x40.toByte()))
            putByteArray(EmvTermCfgConstrants.CURRENCYCODE, byteArrayOf(0x08.toByte(), 0x40.toByte()))
            putByteArray(EmvTermCfgConstrants.TRANS_PROP_9F66, byteArrayOf(0x36.toByte(), 0x00.toByte(), 0xC0.toByte(), 0x00.toByte()))
        }
    }

    private fun buildTransBundle(request: SaleRequest): Bundle {
        val timestamp = emvDateTimeFormatter.format(Date())
        return Bundle().apply {
            putBoolean(EmvTransDataConstrants.FORCE_ONLINE_CALL_PIN, true)
            putBoolean(EmvTransDataConstrants.EMV_TRANS_ENABLE_CONTACTLESS, true)
            putBoolean(EmvTransDataConstrants.EMV_TRANS_ENABLE_CONTACT, true)
            putBoolean(EmvTransDataConstrants.CONTACT_SERVICE_SWITCH, false)
            putBoolean(EmvTransDataConstrants.SELECT_APP_RETURN_AID, false)
            putBoolean(EmvTransDataConstrants.SELECT_APP_RETURN_PRIORITY, true)
            putBoolean(EmvTransDataConstrants.SUPPORT_MAG_CARD, true)
            putBoolean(EmvTransDataConstrants.SUPPORT_IC_CARD, true)
            putBoolean(EmvTransDataConstrants.SUPPORT_RF_CARD, true)
            putInt(EmvTransDataConstrants.CHECK_CARD_TIME_OUT, CARD_SEARCH_TIMEOUT_SECONDS)
            putInt(EmvTransDataConstrants.ISQPBOCFORCEONLINE, 1)
            putByte(EmvTransDataConstrants.B9C, SALE_EMV_TRANSACTION_TYPE)
            putString(EmvTransDataConstrants.TRANSDATE, timestamp.substring(0, 6))
            putString(EmvTransDataConstrants.TRANSTIME, timestamp.substring(6, 12))
            putString(EmvTransDataConstrants.SEQNO, "000001")
            putString(EmvTransDataConstrants.TRANSAMT, request.amount.toDecimalText())
            putString(EmvTransDataConstrants.MERNAME, MERCHANT_NAME)
            putString(EmvTransDataConstrants.MERID, MERCHANT_ID)
            putString(EmvTransDataConstrants.TERMID, TERMINAL_ID)
            putStringArrayList(
                EmvTransDataConstrants.TERMINAL_TLVS,
                arrayListOf("DF840B06000000000001", "DF81190118")
            )
        }
    }

    private fun beep(mode: Int) {
        callbackScope.launch {
            runCatching {
                serviceManager.requireEngine().getBeeper()?.beep(mode)
            }
        }
    }

    private suspend fun readEmvSummary(emvHandler: EmvHandler): EmvTagSummary {
        val tags = EMV_SUMMARY_TAGS.mapNotNull { tagName ->
            readSingleEmvTag(emvHandler, tagName)?.let { value ->
                EmvTag(tag = tagName, value = value, label = tagName.toEmvLabel())
            }
        }
        val pan = readPan(emvHandler).takeIf { it.length >= MIN_PAN_LENGTH }
        return EmvTagSummary(
            aid = tags.firstOrNull { it.tag == TAG_AID }?.value
                ?: tags.firstOrNull { it.tag == TAG_DF_NAME }?.value,
            maskedPan = pan?.let { plainPan ->
                runCatching { MaskedPan.fromPlainPan(plainPan) }.getOrNull()
            },
            tags = tags
        )
    }

    private suspend fun readField55(emvHandler: EmvHandler): String {
        val buffer = ByteArray(FIELD_55_BUFFER_LENGTH)
        val length = emvHandler.readEmvData(FIELD_55_TAGS, buffer, Bundle())
        require(length > 0) {
            "EMV Field 55 data is empty."
        }
        return buffer.copyOf(length).toHex()
    }

    private suspend fun readSingleEmvTag(
        emvHandler: EmvHandler,
        tagName: String
    ): String? {
        val buffer = ByteArray(SINGLE_TAG_BUFFER_LENGTH)
        val length = emvHandler.readEmvData(arrayOf(tagName), buffer, Bundle())
        if (length <= 0) {
            return null
        }
        return parseTlv(buffer.copyOf(length))[tagName.uppercase()]
    }

    private suspend fun readPan(emvHandler: EmvHandler): String {
        val pan = readSingleEmvTag(emvHandler, TAG_PAN)?.trimEnd('F').orEmpty()
        if (pan.isNotBlank()) {
            return pan
        }
        val track2 = readSingleEmvTag(emvHandler, TAG_TRACK_2)?.trimEnd('F').orEmpty()
        return track2.substringBefore('D').substringBefore('=')
    }

    private fun parseTlv(data: ByteArray): Map<String, String> {
        val parsedTags = mutableMapOf<String, String>()
        var offset = 0
        while (offset < data.size) {
            val tagStart = offset
            offset += 1
            if ((data[tagStart].toInt() and TLV_LOW_TAG_MASK) == TLV_LOW_TAG_MASK) {
                while (offset < data.size && (data[offset].toInt() and TLV_CONTINUATION_MASK) == TLV_CONTINUATION_MASK) {
                    offset += 1
                }
                offset += 1
            }
            if (offset >= data.size) break
            val lengthByte = data[offset++].toInt() and 0xFF
            val length = if ((lengthByte and TLV_LONG_FORM_MASK) == TLV_LONG_FORM_MASK) {
                val lengthBytes = lengthByte and TLV_LENGTH_COUNT_MASK
                var valueLength = 0
                repeat(lengthBytes) {
                    if (offset >= data.size) return parsedTags
                    valueLength = (valueLength shl 8) or (data[offset++].toInt() and 0xFF)
                }
                valueLength
            } else {
                lengthByte
            }
            if (offset + length > data.size) break
            val tagEnd = if ((lengthByte and TLV_LONG_FORM_MASK) == TLV_LONG_FORM_MASK) {
                offset - (lengthByte and TLV_LENGTH_COUNT_MASK) - 1
            } else {
                offset - 1
            }
            val tag = data.copyOfRange(tagStart, tagEnd).toHex()
            val value = data.copyOfRange(offset, offset + length).toHex()
            parsedTags[tag] = value
            offset += length
        }
        return parsedTags
    }

    private suspend fun requireLoadedMacKeySlot(): Int {
        if (keyBackend == KeyBackend.PINPAD) {
            return PINPAD_WORK_KEY_INDEX
        }
        return getKeyStatus().slots
            .firstOrNull { slot -> slot.keyType == KeyType.MAC }
            ?.slot
            ?: error("MAC key is not loaded.")
    }

    private suspend fun loadPinPadDemoKeys(
        events: TrackAKeyInjectionEventSink
    ): KeyStatus {
        return runCatching {
            val pinPad = serviceManager.requireEngine().getPinPad()
                ?: throw RemoteException("PinPad service unavailable.")

            val initResult = pinPad.initPinPad(PinPadType.INTERNAL)
            events.onEvent(
                TrackAKeyInjectionEvent.Progress(
                    "PinPad.initPinPad(INTERNAL) result=$initResult (${initResult.describeServiceResult()})"
                )
            )
            if (initResult != ServiceResult.Success) {
                return KeyStatus(
                    readiness = KeyReadinessStatus.NOT_READY,
                    message = "PinPad.initPinPad(INTERNAL) failed. Result code: " +
                        "$initResult (${initResult.describeServiceResult()})"
                )
            }

            events.onEvent(
                TrackAKeyInjectionEvent.Progress(
                    "Calling PinPad.loadKEK(index=$PINPAD_KEK_KEY_INDEX)"
                )
            )
            val kekResult = pinPad.loadKEK(
                PINPAD_KEK_KEY_INDEX,
                PINPAD_KEK_KEY_HEX.hexToBytes(),
                PINPAD_KEK_KCV_HEX.hexToBytes()
            )
            events.onEvent(TrackAKeyInjectionEvent.Progress("PinPad.loadKEK result=$kekResult"))
            if (!kekResult) {
                return KeyStatus(
                    readiness = KeyReadinessStatus.NOT_READY,
                    message = "PinPad.loadKEK failed for index $PINPAD_KEK_KEY_INDEX."
                )
            }

            events.onEvent(
                TrackAKeyInjectionEvent.Progress(
                    "Calling PinPad.loadEncryptMKey(index=$PINPAD_MASTER_KEY_INDEX, kek=$PINPAD_KEK_KEY_INDEX)"
                )
            )
            val encryptedMasterKey = PINPAD_CIPHER_MASTER_KEY_HEX.hexToBytes()
            val masterResult = pinPad.loadEncryptMKey(
                PINPAD_MASTER_KEY_INDEX,
                encryptedMasterKey,
                encryptedMasterKey.size,
                PINPAD_KEK_KEY_INDEX,
                true
            )
            events.onEvent(
                TrackAKeyInjectionEvent.Progress(
                    "PinPad.loadEncryptMKey result=$masterResult (${masterResult.describeServiceResult()})"
                )
            )
            if (masterResult != ServiceResult.Success) {
                return KeyStatus(
                    readiness = KeyReadinessStatus.NOT_READY,
                    message = "PinPad.loadEncryptMKey failed for index " +
                        "$PINPAD_MASTER_KEY_INDEX using KEK index $PINPAD_KEK_KEY_INDEX. " +
                        "Result code: $masterResult (${masterResult.describeServiceResult()})"
                )
            }

            if (!hasPinPadMasterKey()) {
                return KeyStatus(
                    readiness = KeyReadinessStatus.NOT_READY,
                    message = "PinPad master key is not accessible after loadEncryptMKey."
                )
            }

            events.onEvent(
                TrackAKeyInjectionEvent.Progress(
                    "PinPad master key exists before working keys: ${hasPinPadMasterKey()}"
                )
            )
            val encryptedPinKey = PINPAD_PIN_WORK_KEY_WITH_KCV_HEX.hexToBytes()
            val pinResult = pinPad.loadDemoWorkKey(
                keyType = WorkKeyType.PINKEY,
                label = "PIN",
                keyBytes = encryptedPinKey,
                events = events
            )
            if (pinResult != ServiceResult.Success) {
                return KeyStatus(
                    readiness = KeyReadinessStatus.NOT_READY,
                    message = "PinPad.loadWKey PIN failed for slot " +
                        "$PINPAD_WORK_KEY_INDEX. Result code: $pinResult (${pinResult.describeServiceResult()})"
                )
            }

            val encryptedMacKey = PINPAD_MAC_WORK_KEY_WITH_KCV_HEX.hexToBytes()
            val macResult = pinPad.loadDemoWorkKey(
                keyType = WorkKeyType.MACKEY,
                label = "MAC",
                keyBytes = encryptedMacKey,
                events = events
            )
            if (macResult != ServiceResult.Success) {
                return KeyStatus(
                    readiness = KeyReadinessStatus.NOT_READY,
                    message = "PinPad.loadWKey MAC failed for slot " +
                        "$PINPAD_WORK_KEY_INDEX. Result code: $macResult (${macResult.describeServiceResult()})"
                )
            }

            keyBackend = KeyBackend.PINPAD
            events.onEvent(TrackAKeyInjectionEvent.Progress("PinPad KEK/MK/SK keys loaded"))
            loadDemoEmvAids(events)
            pinPadKeyStatus(
                detail = "Real PinPad KEK/MK/SK keys: loaded through the MoreFun demo flow."
            )
        }.getOrElse { error ->
            KeyStatus(
                readiness = KeyReadinessStatus.UNKNOWN,
                message = "PinPad key loading error: ${error.message ?: "unknown error"}"
            )
        }
    }

    private fun com.morefun.yapi.device.pinpad.PinPad.loadDemoWorkKey(
        keyType: Int,
        label: String,
        keyBytes: ByteArray,
        events: TrackAKeyInjectionEventSink
    ): Int {
        events.onEvent(
            TrackAKeyInjectionEvent.Progress(
                "Calling PinPad.loadWKey($label, slot=$PINPAD_WORK_KEY_INDEX)"
            )
        )
        val result = loadWKey(
            PINPAD_WORK_KEY_INDEX,
            keyType,
            keyBytes,
            keyBytes.size
        )
        events.onEvent(
            TrackAKeyInjectionEvent.Progress(
                "PinPad.loadWKey $label result=$result (${result.describeServiceResult()})"
            )
        )
        return result
    }

    private fun pinPadKeyStatus(detail: String): KeyStatus {
        val now = System.currentTimeMillis()
        return KeyStatus(
            readiness = KeyReadinessStatus.READY,
            slots = listOf(
                KeySlotMetadata(
                    keyType = KeyType.MASTER,
                    slot = PINPAD_MASTER_KEY_INDEX,
                    kcv = PINPAD_MASTER_KCV,
                    updatedAtMillis = now,
                    storageLabel = PINPAD_KEY_INDEX_LABEL
                ),
                KeySlotMetadata(
                    keyType = KeyType.MAC,
                    slot = PINPAD_WORK_KEY_INDEX,
                    kcv = PINPAD_MAC_WORK_KCV,
                    updatedAtMillis = now,
                    storageLabel = PINPAD_KEY_INDEX_LABEL
                ),
                KeySlotMetadata(
                    keyType = KeyType.PIN,
                    slot = PINPAD_WORK_KEY_INDEX,
                    kcv = PINPAD_PIN_WORK_KCV,
                    updatedAtMillis = now,
                    storageLabel = PINPAD_KEY_INDEX_LABEL
                )
            ),
            message = detail
        )
    }

    private suspend fun readPinPadKeyStatusOrNull(detail: String): KeyStatus? {
        return runCatching {
            val pinPad = serviceManager.requireEngine().getPinPad()
                ?: return@runCatching null
            val now = System.currentTimeMillis()
            val masterKey = pinPad.keyMetadataOrNull(
                sdkType = CheckKeyEnum.DES_MASTER_KEY,
                appType = KeyType.MASTER,
                index = PINPAD_MASTER_KEY_INDEX,
                updatedAtMillis = now
            )
            val macKey = pinPad.keyMetadataOrNull(
                sdkType = CheckKeyEnum.DES_MAC_KEY,
                appType = KeyType.MAC,
                index = PINPAD_WORK_KEY_INDEX,
                updatedAtMillis = now
            )
            val pinKey = pinPad.keyMetadataOrNull(
                sdkType = CheckKeyEnum.DES_PIN_KEY,
                appType = KeyType.PIN,
                index = PINPAD_WORK_KEY_INDEX,
                updatedAtMillis = now
            )
            val slots = listOfNotNull(masterKey, macKey, pinKey)
            if (slots.isEmpty()) {
                return@runCatching null
            }
            val hasMaster = masterKey != null
            val hasMac = macKey != null
            KeyStatus(
                readiness = if (hasMaster && hasMac) {
                    KeyReadinessStatus.READY
                } else {
                    KeyReadinessStatus.NOT_READY
                },
                slots = slots,
                message = buildString {
                    append(detail)
                    append("\n")
                    append(if (hasMaster) "PinPad master loaded" else "PinPad master missing")
                    append(", ")
                    append(if (hasMac) "MAC loaded" else "MAC missing")
                    append(", ")
                    append(if (pinKey != null) "PIN loaded" else "PIN optional missing")
                }
            )
        }.getOrNull()
    }

    private suspend fun hasPinPadMasterKey(): Boolean {
        return runCatching {
            serviceManager.requireEngine().getPinPad()
                ?.hasKey(TDesKeyObj.KeyTypeEnum.DES_MAIN_KEY, PINPAD_MASTER_KEY_INDEX) == true
        }.getOrDefault(false)
    }

    private fun com.morefun.yapi.device.pinpad.PinPad.hasKey(
        keyType: TDesKeyObj.KeyTypeEnum,
        index: Int
    ): Boolean {
        return checkKey(
            TDesKeyObj(
                keyType,
                TDesKeyObj.OperEnum.EXITS_KEY,
                index
            )
        )
    }

    private fun com.morefun.yapi.device.pinpad.PinPad.deleteKey(
        keyType: TDesKeyObj.KeyTypeEnum,
        index: Int
    ): Boolean {
        return checkKey(
            TDesKeyObj(
                keyType,
                TDesKeyObj.OperEnum.DELETE_KEY,
                index
            )
        )
    }

    private fun com.morefun.yapi.device.pinpad.PinPad.keyMetadataOrNull(
        sdkType: CheckKeyEnum,
        appType: KeyType,
        index: Int,
        updatedAtMillis: Long
    ): KeySlotMetadata? {
        val exists = when (appType) {
            KeyType.MASTER -> hasKey(TDesKeyObj.KeyTypeEnum.DES_MAIN_KEY, index)
            KeyType.MAC -> hasKey(TDesKeyObj.KeyTypeEnum.DES_WK_MAC, index)
            KeyType.PIN -> hasKey(TDesKeyObj.KeyTypeEnum.DES_WK_PIN, index)
            KeyType.DATA,
            KeyType.DUKPT -> false
        }
        if (!exists) {
            return null
        }
        val kcv = getKeyKcv(sdkType, index)
            ?.getKCV()
            ?.takeIf { value -> value.isNotBlank() }
            ?.take(KCV_DISPLAY_LENGTH)
        return KeySlotMetadata(
            keyType = appType,
            slot = index,
            kcv = kcv,
            updatedAtMillis = updatedAtMillis,
            storageLabel = PINPAD_KEY_INDEX_LABEL
        )
    }

    private suspend fun loadDemoEmvAids(events: TrackAKeyInjectionEventSink) {
        runCatching {
            val emvHandler = serviceManager.requireEngine().getEmvHandler()
                ?: throw RemoteException("EMV service unavailable.")
            events.onEvent(TrackAKeyInjectionEvent.Progress("Loading demo EMV AID parameters"))
            emvHandler.clearAIDParam()
            var loaded = 0
            DEMO_AID_PARAMS.forEachIndexed { index, aid ->
                val result = emvHandler.addAidParam(aid.hexToBytes())
                if (result != ServiceResult.Success) {
                    events.onEvent(
                        TrackAKeyInjectionEvent.Progress(
                            "EMV AID load failed index=$index result=$result (${result.describeServiceResult()})"
                        )
                    )
                    return@forEachIndexed
                }
                loaded += 1
            }
            events.onEvent(
                TrackAKeyInjectionEvent.Progress("Loaded $loaded demo EMV AID parameter(s)")
            )
        }.getOrElse { error ->
            events.onEvent(
                TrackAKeyInjectionEvent.Progress(
                    "Could not load demo EMV AID parameters: ${error.message ?: "unknown error"}"
                )
            )
        }
    }

    private fun com.franktardencilla.mfdemoapp.domain.model.MoneyAmount.toDecimalText(): String {
        val major = minorUnits / MINOR_UNITS_PER_MAJOR
        val minor = minorUnits % MINOR_UNITS_PER_MAJOR
        return "$major.${minor.toString().padStart(2, '0')}"
    }

    private fun buildIssuerAuthData(responseCode: String): ByteArray {
        return "8A02${responseCode.toByteArray(Charsets.US_ASCII).toHex()}".hexToBytes()
    }

    private fun String.toEmvLabel(): String {
        return when (uppercase()) {
            TAG_PAN -> "Application PAN"
            TAG_TRACK_2 -> "Track 2 equivalent data"
            TAG_AID -> "Application identifier"
            TAG_DF_NAME -> "Dedicated file name"
            "9F26" -> "Application cryptogram"
            "9F27" -> "Cryptogram information data"
            "9F10" -> "Issuer application data"
            "9F37" -> "Unpredictable number"
            "9F36" -> "Application transaction counter"
            "95" -> "Terminal verification results"
            "9A" -> "Transaction date"
            "9C" -> "Transaction type"
            "9F02" -> "Authorized amount"
            "5F2A" -> "Transaction currency code"
            "82" -> "Application interchange profile"
            "9F33" -> "Terminal capabilities"
            "9F34" -> "CVM results"
            "9F35" -> "Terminal type"
            "9F1E" -> "Interface device serial number"
            "9F09" -> "Application version number"
            "5F24" -> "Application expiration date"
            else -> "EMV tag"
        }
    }

    private fun Int.describeServiceResult(): String {
        return when (this) {
            ServiceResult.Success -> "success"
            ServiceResult.PinPad_No_Key_Error -> "no key"
            ServiceResult.PinPad_KeyIdx_Error -> "key index error"
            ServiceResult.PinPad_Key_NOT_EXITS -> "key does not exist"
            ServiceResult.PinPad_Check_Key_Fail -> "KCV check failed"
            ServiceResult.PinPad_LOAD_KEY_FAIL -> "load key failed"
            ServiceResult.PinPad_Dstkey_Idx_Error -> "destination key index error"
            ServiceResult.PinPad_SrcKey_Idx_Error -> "source key index error"
            ServiceResult.PinPad_Key_Len_Error -> "key length error"
            ServiceResult.PinPad_Key_Type_Error -> "key type error"
            ServiceResult.PinPad_Kcv_Check_Fail -> "KCV check failed"
            ServiceResult.PinPad_Kcv_Odd_Check_Fail -> "odd KCV check failed"
            ServiceResult.PinPad_Mac_Error -> "MAC operation error"
            ServiceResult.PinPad_Ped_Data_Rw_Fail -> "secure key storage read/write failed"
            ServiceResult.PinPad_Other_Error -> "PinPad other error"
            ServiceResult.Device_Not_Ready -> "device not ready"
            ServiceResult.Param_In_Invalid -> "invalid parameter"
            ServiceResult.Emv_Terminate -> "EMV terminate"
            ServiceResult.Emv_Declined -> "EMV declined"
            ServiceResult.Emv_Online -> "EMV online"
            ServiceResult.Emv_Cancel -> "EMV canceled"
            ServiceResult.Emv_PARA_ERR -> "EMV parameter error"
            ServiceResult.Emv_FallBack -> "EMV fallback"
            else -> "unmapped SDK result"
        }
    }

    private fun IPed.slotMetadataOrNull(
        keyType: KeyType,
        slot: Int
    ): KeySlotMetadata? {
        val morefunKeyType = keyType.toMorefunPedKeyType()
        if (!isKeyExist(morefunKeyType, slot)) {
            return null
        }
        val kcv = runCatching {
            calcKCV(morefunKeyType, slot)?.toHex()?.take(KCV_DISPLAY_LENGTH)
        }.getOrNull()
        return KeySlotMetadata(
            keyType = keyType,
            slot = slot,
            kcv = kcv,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    private fun IPed.describeTrackedSlot(trackedSlot: TrackedSlot): String {
        return runCatching {
            val sdkKeyType = trackedSlot.keyType.toMorefunPedKeyType()
            val exists = isKeyExist(sdkKeyType, trackedSlot.slot)
            val kcv = if (exists) {
                runCatching {
                    calcKCV(sdkKeyType, trackedSlot.slot)?.toHex()?.take(KCV_DISPLAY_LENGTH)
                }.getOrNull()
            } else {
                null
            }
            "${trackedSlot.keyType.displayName} slot ${trackedSlot.slot}: exists=$exists kcv=${kcv ?: "none"}"
        }.getOrElse { error ->
            "${trackedSlot.keyType.displayName} slot ${trackedSlot.slot}: check failed ${error.message ?: "unknown error"}"
        }
    }

    private fun KeyType.toMorefunPedKeyType(): Int {
        return when (this) {
            KeyType.MASTER -> MorefunKeyType.MAIN_KEY
            KeyType.MAC -> MorefunKeyType.MAC_KEY
            KeyType.PIN -> MorefunKeyType.PIN_KEY
            KeyType.DATA -> MorefunKeyType.TDK_KEY
            KeyType.DUKPT -> MorefunKeyType.TDK_KEY
        }
    }

    private fun com.morefun.yapi.device.pinpad.PinPad.clearDemoPinPadKeys(): List<String> {
        val pinPadSlots = listOf(
            PinPadTrackedSlot("PinPad master", TDesKeyObj.KeyTypeEnum.DES_MAIN_KEY, PINPAD_MASTER_KEY_INDEX),
            PinPadTrackedSlot("PinPad MAC", TDesKeyObj.KeyTypeEnum.DES_WK_MAC, PINPAD_WORK_KEY_INDEX),
            PinPadTrackedSlot("PinPad PIN", TDesKeyObj.KeyTypeEnum.DES_WK_PIN, PINPAD_WORK_KEY_INDEX)
        )
        return pinPadSlots.map { slot ->
            runCatching {
                val existed = hasKey(slot.sdkKeyType, slot.index)
                val deleted = if (existed) deleteKey(slot.sdkKeyType, slot.index) else true
                "${slot.label} slot ${slot.index}: ${if (deleted) "cleared" else "delete failed"}"
            }.getOrElse { error ->
                "${slot.label} slot ${slot.index}: delete failed ${error.message ?: "unknown error"}"
            }
        }
    }

    private fun IPed.clearLegacyPedKeys(): List<String> {
        return TRACK_A_SLOTS.map { trackedSlot ->
            runCatching {
                val result = deleteKey(trackedSlot.slot, trackedSlot.keyType.toMorefunPedKeyType())
                "${trackedSlot.keyType.displayName} PED slot ${trackedSlot.slot}: delete result=$result"
            }.getOrElse { error ->
                "${trackedSlot.keyType.displayName} PED slot ${trackedSlot.slot}: delete failed ${error.message ?: "unknown error"}"
            }
        }
    }

    private fun RealEmvFinish.describe(): String {
        val errorCode = data.getByteArray(EmvErrorConstrants.EMV_ERROR_CODE)
            ?.toString(Charsets.US_ASCII)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        return buildString {
            append("RetCode=$retCode (${retCode.describeServiceResult()})")
            errorCode?.let { append(", EMV error code=$it") }
            append(".")
        }
    }

    private fun HostSaleResponse.toDeclineMessage(): String {
        val responseCode = responseSummary.responseCode ?: "unknown"
        val reason = when (responseCode) {
            RESPONSE_CODE_ERROR -> "security or MAC validation failed"
            "05" -> "do not honor"
            else -> "host declined"
        }
        return "Host rejected sale. Response code: $responseCode ($reason)."
    }

    private data class TrackedSlot(
        val keyType: KeyType,
        val slot: Int
    )

    private data class PinPadTrackedSlot(
        val label: String,
        val sdkKeyType: TDesKeyObj.KeyTypeEnum,
        val index: Int
    )

    private data class RealEmvFinish(
        val retCode: Int,
        val data: Bundle
    )

    private sealed interface RealSaleKernelResult {
        data class EmvFinished(val finish: RealEmvFinish) : RealSaleKernelResult
        data class MagStripeFinished(val result: SaleDeviceResult) : RealSaleKernelResult
    }

    private enum class KeyBackend {
        PED,
        PINPAD
    }

    private companion object {
        const val SERVICE_SUCCESS = 0
        const val MAC_OUTPUT_LENGTH = 8
        const val KCV_DISPLAY_LENGTH = 6
        const val ISO_FIELD_TRACK_2 = 35
        const val ISO_FIELD_MAC = 64
        const val PIN_WORKING_KEY_SLOT = 1
        const val PINPAD_KEK_KEY_INDEX = 0
        const val PINPAD_MASTER_KEY_INDEX = 0
        const val PINPAD_WORK_KEY_INDEX = 0
        const val PINPAD_KEY_INDEX_LABEL = "PinPad key index"
        const val PINPAD_BUSINESS_ID = "00000000"
        const val PINPAD_KEK_KEY_HEX = "11111111111111111111111111111111"
        const val PINPAD_KEK_KCV_HEX = "82E13665"
        const val PINPAD_CIPHER_MASTER_KEY_HEX = "4B24C397E2D59A29A176FC37909A54E6"
        const val PINPAD_CIPHER_MASTER_KEY_KCV_HEX = "64C4E1C6"
        const val PINPAD_MASTER_KCV = "64C4E1"
        const val PINPAD_PIN_WORK_KEY_WITH_KCV_HEX = "DF952C488031F1ECDF952C488031F1ECADC67D84"
        const val PINPAD_MAC_WORK_KEY_WITH_KCV_HEX = "2459FE25EB0A2A442459FE25EB0A2A4444BA838C"
        const val PINPAD_PIN_WORK_KCV = "ADC67D"
        const val PINPAD_MAC_WORK_KCV = "44BA83"
        const val PIN_TIMEOUT_SECONDS = 60
        const val PIN_MIN_LENGTH = 4
        const val PIN_MAX_LENGTH = 6
        const val MILLIS_PER_SECOND = 1_000L
        const val CARD_SEARCH_TIMEOUT_SECONDS = 20
        const val REAL_EMV_TIMEOUT_MILLIS = 120_000L
        const val HOST_AUTHORIZATION_TIMEOUT_MILLIS = 10_000L
        const val PRINT_TIMEOUT_MILLIS = 30_000L
        const val FIRST_APP_INDEX = 0
        const val SALE_EMV_TRANSACTION_TYPE: Byte = 0x00
        const val TERMINAL_ID = "DEMO920"
        const val MERCHANT_ID = "MFDemoMerchant"
        const val MERCHANT_NAME = "MFDemo Merchant"
        const val RESPONSE_CODE_ERROR = "96"
        const val FIELD_55_BUFFER_LENGTH = 4096
        const val SINGLE_TAG_BUFFER_LENGTH = 512
        const val MIN_PAN_LENGTH = 10
        const val TAG_PAN = "5A"
        const val TAG_TRACK_2 = "57"
        const val TAG_AID = "4F"
        const val TAG_DF_NAME = "84"
        const val ICC_CARD_OTHER_KEY = "CardOther"
        const val ICC_CONTACT_CARD = 1
        const val ICC_CONTACTLESS_CARD = 7
        const val MINOR_UNITS_PER_MAJOR = 100L
        const val TLV_LOW_TAG_MASK = 0x1F
        const val TLV_CONTINUATION_MASK = 0x80
        const val TLV_LONG_FORM_MASK = 0x80
        const val TLV_LENGTH_COUNT_MASK = 0x7F
        val emvDateTimeFormatter = SimpleDateFormat("yyMMddHHmmss", Locale.US)
        val FIELD_55_TAGS = arrayOf(
            "9F26",
            "9F27",
            "9F10",
            "9F37",
            "9F36",
            "95",
            "9A",
            "9C",
            "9F02",
            "5F2A",
            "82",
            "9F1A",
            "9F33",
            "9F34",
            "9F35",
            "9F1E",
            "84",
            "9F09",
            "9F63",
            "50",
            "9F12",
            "4F",
            "5F24"
        )
        val EMV_SUMMARY_TAGS = arrayOf(
            TAG_PAN,
            TAG_TRACK_2,
            TAG_AID,
            TAG_DF_NAME,
            "9F26",
            "9F27",
            "9F10",
            "9F37",
            "9F36",
            "95",
            "9A",
            "9C",
            "9F02",
            "5F2A",
            "82",
            "9F33",
            "9F34",
            "9F35",
            "9F1E",
            "9F09",
            "5F24"
        )
        val DEMO_AID_PARAMS = listOf(
            "9F0607A0000000031010DF0101009F09020096DF11050000000000DF12050000000000DF130500000000009F1B0400003A98DF1504000000009F1D080000000000000000DF160100DF170100DF14039F3704DF1801319F7B06000000002000DF1906000000000000DF2006000010000000DF2106000000100000",
            "9F0608A000000003101003DF0101009F09020096DF11050000000000DF12050000000000DF130500000000009F1B0400003A98DF1504000000009F1D080000000000000000DF160150DF170110DF14039F3704DF1801319F7B06000000200000DF1906000000000000DF2006000002000000DF2106000000100000",
            "9F0608A000000003101004DF0101009F09020096DF11050000000000DF12050000000000DF130500000000009F1B0400003A98DF1504000000009F1D080000000000000000DF160150DF170120DF14039F3704DF1801319F7B06000000000020DF1906000000000000DF2006000010000000DF2106000000100000",
            "9F0608A000000003101005DF0101009F09020096DF11050000000000DF12050000000000DF130500000000009F1B0400003A98DF1504000000009F1D080000000000000000DF160150DF170120DF14039F3704DF1801319F7B06000000000020DF1906000000000000DF2006000000200000DF2106000000100000",
            "9F0608A000000003101006DF0101009F09020096DF11050000000000DF12050000000000DF130500000000009F1B0400003A98DF1504000000009F1D080000000000000000DF160150DF170120DF14039F3704DF1801319F7B06000000200000DF1906000000000000DF2006000002000000DF2106000000100000",
            "9F0608A000000003101007DF0101019F09020096DF11050000000000DF12050000000000DF130500000000009F1B0400003A98DF1504000000009F1D080000000000000000DF160150DF170120DF14039F3704DF1801319F7B06000000200000DF1906000000000000DF2006000002000000DF2106000000100000",
            "9F0607A0000000041010DF0101009F09020002DF11050000000000DF12050000000000DF130540000000009F1B0400004E20DF1504000000009F1D080000000000000000DF160150DF170120DF14039F3704DF1801319F7B06000000200000DF1906000000000000DF2006000002000000DF2106000000100000",
            "9F0607A0000000043060DF0101009F09020002DF11050000000000DF12050000000000DF130540000000009F1B0400004E20DF150400000000DF160150DF1701209F1D084C00000000000000DF2106000000200000DF2006000001000000DF1906000000000000",
            "9F0607A0000000651010DF0101009F09020200DF11050000000000DF12050000000000DF130500000080009F1B04000061A8DF1504000000009F1D080000000000000000DF160150DF170120DF14039F3704DF1801319F7B06000000200000DF1906000000000000DF2006000002000000DF2106000000100000",
            "9F0607A0000000250101DF0101009F09020001DF11050000000000DF12050000000000DF130500200000009F1B0400007530DF1504000000009F1D080000000000000000DF160150DF170120DF14039F3704DF1801319F7B06000000200000DF1906000000000000DF2006000002000000DF2106000000100000",
            "9F0608A000000025010501DF0101009F09020001DF11050000000000DF12050000000000DF130500200000009F1B0400007530DF1504000000009F1D080000000000000000DF160150DF170120DF14039F3704DF1801319F7B06000000200000DF1906000000000000DF2006000002000000DF2106000000100000",
            "9F0607A0000003330101DF0101009F09020030DF11050000000000DF12050000000000DF130500000000009F1B0400002710DF1504000000009F1D080000000000000000DF160100DF170100DF14039F3704DF1801319F7B06000000200000DF1906000000000000DF2006000010000000DF2106000000100000",
            "9F0608A000000333010101DF0101009F09020030DF11050000000000DF12050000000000DF130500000000009F1B04000186A0DF1504000000009F1D080000000000000000DF160100DF170100DF14039F3704DF1801319F7B06000000010000DF1906000000000000DF2006000010000000DF2106000000100000",
            "9F0608A000000333010102DF0101009F09020030DF11050000000000DF12050000000000DF130500000000809F1B0400009C40DF1504000000009F1D080000000000000000DF160100DF170100DF14039F3704DF1801319F7B06000000200000DF1906000000000000DF2006000010000000DF2106000000100000",
            "9F0607A0000001523010DF0101009F09020001DF11050000000000DF12050000000000DF130500000000009F1B0400001388DF1504000000009F1D080000000000000000DF160150DF170120DF14039F3704DF1801319F7B06000000200000DF1906000000000000DF2006000002000000DF2106000000100000",
            "9F0607A0000003241010DF0101009F09020001DF11050000000000DF12050000000000DF130500000000009F1B0400001388DF1504000000009F1D080000000000000000DF160150DF170120DF14039F3704DF1801319F7B06000000200000DF1906000000000000DF2006000002000000DF2106000000100000",
            "9F0607A0000005241010DF0101009F09020002DF11050000000000DF12050000000000DF130500000000009F1B04000186A0DF1504000000009F1D080000000000000000DF160105DF170100DF1801319F7B06000000010000DF1906000000000500DF2006000010000000DF2106000000100000"
        )
        val TRACK_A_SLOTS = listOf(
            TrackedSlot(KeyType.MASTER, 1),
            TrackedSlot(KeyType.MAC, 9),
            TrackedSlot(KeyType.PIN, 1)
        )
    }
}

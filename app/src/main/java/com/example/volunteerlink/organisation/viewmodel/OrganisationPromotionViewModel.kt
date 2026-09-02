package com.example.volunteerlink.organisation.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.post.PostTimingState
import com.example.volunteerlink.data.supabase
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.organisation.manage.model.ManagePostItem
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Promotion flow state.
 *
 * Practical 7 style is kept here: one MutableStateFlow in the ViewModel and the
 * screen collects the exposed read-only StateFlow. Promotion/payment persistence
 * goes to Supabase. SharedPreferences only remembers the payment preference,
 * cardholder name and last four digits; full card details are never stored.
 */
class OrganisationPromotionViewModel(
    application: Application
) : AndroidViewModel(application) {

    val paymentPreferenceOwner = supabase.auth.currentUserOrNull()?.id ?: "signed_out"
    val paymentPreferences = application.getSharedPreferences(
        "promotion_payment_preferences_$paymentPreferenceOwner",
        Context.MODE_PRIVATE
    )

    // Practical 7 pattern: mutable StateFlow stays inside the ViewModel,
    // while the screen only receives the read-only StateFlow.
    private val _uiState = MutableStateFlow(
        OrganisationPromotionUiState(
            preferredPaymentMethod = paymentPreferences
                .getString("preferred_payment_method", null)
                ?.let { saved ->
                    runCatching { PromotionPaymentMethod.valueOf(saved) }.getOrNull()
                },
            savedCardholderName = paymentPreferences
                .getString("saved_cardholder_name", null)
                ?.takeIf(String::isNotBlank),
            savedCardLastFour = paymentPreferences
                .getString("saved_card_last_four", null)
                ?.takeIf { it.length == 4 && it.all(Char::isDigit) }
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        refreshPromotions()
    }

    fun refreshPromotions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingPromotions = true,
                promotionLoadError = null
            )

            try {
                val rows = supabase
                    .from("post_promotions")
                    .select(
                        columns = Columns.raw(
                            "promotion_id,post_id,start_at,end_at,created_at"
                        )
                    )
                    .decodeList<JsonObject>()

                val latestByPost = rows
                    .mapNotNull { row ->
                        val promotionId = row.promotionText("promotion_id") ?: return@mapNotNull null
                        val postId = row.promotionText("post_id") ?: return@mapNotNull null
                        val startAt = row.promotionText("start_at")
                            ?.let(::parsePromotionTimestamp)
                            ?: return@mapNotNull null
                        val endAt = row.promotionText("end_at")
                            ?.let(::parsePromotionTimestamp)
                            ?: return@mapNotNull null
                        val createdAt = row.promotionText("created_at")
                            ?.let(::parsePromotionTimestamp)
                            ?: startAt

                        PromotionRecord(
                            promotionId = promotionId,
                            postId = postId,
                            startAtMillis = startAt,
                            endAtMillis = endAt,
                            createdAtMillis = createdAt
                        )
                    }
                    .groupBy { it.postId }
                    .mapValues { (_, promotions) ->
                        promotions.maxBy { it.startAtMillis }
                    }

                _uiState.value = _uiState.value.copy(
                    isLoadingPromotions = false,
                    promotionsByPostId = latestByPost,
                    promotionLoadError = null
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoadingPromotions = false,
                    promotionLoadError = exception.message
                        ?: "Unable to load promotion information."
                )
            }
        }
    }

    fun selectPost(post: ManagePostItem) {
        if (!isPostEligible(post)) {
            _uiState.value = _uiState.value.copy(
                message = "This post is no longer available for promotion."
            )
            return
        }

        if (!canPurchaseAnyPackage(post)) {
            _uiState.value = _uiState.value.copy(
                message = "There is not enough time for a full promotion package before this opportunity begins."
            )
            return
        }

        val defaultPackage = when {
            isPackageAvailable(post, PromotionPackage.THREE_DAYS) ->
                PromotionPackage.THREE_DAYS
            isPackageAvailable(post, PromotionPackage.ONE_DAY) ->
                PromotionPackage.ONE_DAY
            else -> null
        }

        _uiState.value = _uiState.value.copy(
            step = PromotionStep.PACKAGE,
            selectedPost = post,
            selectedPackage = defaultPackage,
            selectedPaymentMethod = null,
            message = null,
            isProcessing = false
        )
    }

    fun selectPackage(promotionPackage: PromotionPackage) {
        val post = _uiState.value.selectedPost ?: return
        if (!isPackageAvailable(post, promotionPackage)) return

        _uiState.value = _uiState.value.copy(
            selectedPackage = promotionPackage,
            message = null
        )
    }

    fun continueFromPackage() {
        val state = _uiState.value
        val post = state.selectedPost ?: return
        val promotionPackage = state.selectedPackage

        if (promotionPackage == null) {
            _uiState.value = state.copy(message = "Choose a promotion duration first.")
            return
        }

        if (!isPackageAvailable(post, promotionPackage)) {
            _uiState.value = state.copy(
                selectedPackage = null,
                message = "That package no longer fits before the opportunity starts."
            )
            return
        }

        _uiState.value = state.copy(
            step = PromotionStep.REVIEW,
            message = null
        )
    }

    fun continueToPayment() {
        val state = _uiState.value
        val post = state.selectedPost ?: return
        val promotionPackage = state.selectedPackage ?: return

        if (!isPackageAvailable(post, promotionPackage)) {
            _uiState.value = state.copy(
                step = PromotionStep.PACKAGE,
                selectedPackage = null,
                message = "The promotion duration needs to be chosen again because the opportunity starts too soon."
            )
            return
        }

        _uiState.value = state.copy(
            step = PromotionStep.PAYMENT_METHOD,
            message = null
        )
    }

    fun choosePaymentMethod(method: PromotionPaymentMethod) {
        val state = _uiState.value
        val hasSavedCard = !state.savedCardholderName.isNullOrBlank() &&
            !state.savedCardLastFour.isNullOrBlank()

        _uiState.value = state.copy(
            selectedPaymentMethod = method,
            step = if (method == PromotionPaymentMethod.TOUCH_N_GO) {
                PromotionStep.TOUCH_N_GO
            } else {
                PromotionStep.CARD
            },
            useSavedCard = method == PromotionPaymentMethod.CARD && hasSavedCard,
            cardholderName = if (
                method == PromotionPaymentMethod.CARD && !hasSavedCard
            ) {
                state.savedCardholderName.orEmpty()
            } else {
                state.cardholderName
            },
            message = null
        )
    }

    fun useAnotherCard() {
        val state = _uiState.value
        _uiState.value = state.copy(
            useSavedCard = false,
            cardholderName = state.savedCardholderName.orEmpty(),
            cardNumber = "",
            cardExpiry = "",
            cardCvv = "",
            message = null
        )
    }

    fun updateCardholder(value: String) {
        val clean = value.filter { character ->
            character.isLetter() || character.isWhitespace() || character == '-' || character == '\''
        }
        _uiState.value = _uiState.value.copy(cardholderName = clean.take(60))
    }

    fun updateCardNumber(value: String) {
        _uiState.value = _uiState.value.copy(
            cardNumber = value.filter(Char::isDigit).take(16)
        )
    }

    fun updateExpiry(value: String) {
        _uiState.value = _uiState.value.copy(
            cardExpiry = value.filter(Char::isDigit).take(4)
        )
    }

    fun updateCvv(value: String) {
        _uiState.value = _uiState.value.copy(
            cardCvv = value.filter(Char::isDigit).take(3)
        )
    }

    fun payWithTouchNGo() {
        if (_uiState.value.isProcessing) return
        beginPayment(PromotionPaymentMethod.TOUCH_N_GO)
    }

    fun payWithCard() {
        val state = _uiState.value
        if (state.isProcessing) return

        val savedCardReady = state.useSavedCard &&
            !state.savedCardholderName.isNullOrBlank() &&
            !state.savedCardLastFour.isNullOrBlank()

        if (!savedCardReady && !isCardValid(state)) return
        beginPayment(PromotionPaymentMethod.CARD)
    }

    fun beginPayment(method: PromotionPaymentMethod) {
        val state = _uiState.value
        val post = state.selectedPost ?: return
        val promotionPackage = state.selectedPackage ?: return

        if (!isPackageAvailable(post, promotionPackage)) {
            _uiState.value = state.copy(
                step = PromotionStep.PACKAGE,
                selectedPackage = null,
                message = "There is no longer enough time for that promotion package."
            )
            return
        }

        _uiState.value = state.copy(
            selectedPaymentMethod = method,
            isProcessing = true,
            message = null
        )

        viewModelScope.launch {
            try {
                // Keep the short processing state for the university payment flow,
                // then persist the successful simulated payment atomically in Supabase.
                delay(700)

                val response = supabase.postgrest.rpc(
                    function = "organisation_purchase_post_promotion",
                    parameters = buildJsonObject {
                        put("p_post_id", post.postId)
                        put("p_package_days", promotionPackage.days)
                        put(
                            "p_payment_method",
                            if (method == PromotionPaymentMethod.TOUCH_N_GO) "TNG" else "CARD"
                        )
                    }
                )

                val result = Json.parseToJsonElement(response.data).jsonObject
                val promotionId = result.promotionText("promotion_id")
                    ?: error("Promotion ID was not returned.")
                val paymentId = result.promotionText("payment_id")
                    ?: error("Payment ID was not returned.")
                val startAt = result.promotionText("start_at")
                    ?.let(::parsePromotionTimestamp)
                    ?: error("Promotion start time was not returned.")
                val endAt = result.promotionText("end_at")
                    ?.let(::parsePromotionTimestamp)
                    ?: error("Promotion end time was not returned.")
                val paidAt = result.promotionText("paid_at")
                    ?.let(::parsePromotionTimestamp)
                    ?: AppClock.nowMillis()
                val cutoffAt = result.promotionText("cutoff_at")
                    ?.let(::parsePromotionTimestamp)
                    ?: promotionCutoffMillis(post)
                    ?: endAt
                val amount = result["amount"]?.jsonPrimitive?.doubleOrNull
                    ?: promotionPackage.price
                val isExtension = result["is_extension"]
                    ?.jsonPrimitive
                    ?.booleanOrNull
                    ?: false

                val promotionRecord = PromotionRecord(
                    promotionId = promotionId,
                    postId = post.postId,
                    startAtMillis = startAt,
                    endAtMillis = endAt,
                    createdAtMillis = _uiState.value.promotionsByPostId[post.postId]
                        ?.createdAtMillis
                        ?: paidAt
                )

                val completed = PromotionPurchase(
                    promotionId = promotionId,
                    paymentId = paymentId,
                    postId = post.postId,
                    title = post.title,
                    promotionPackage = promotionPackage,
                    amount = amount,
                    paymentMethod = method,
                    startAtMillis = startAt,
                    endAtMillis = endAt,
                    paidAtMillis = paidAt,
                    cutoffAtMillis = cutoffAt,
                    isExtension = isExtension
                )

                var savedCardholderName = state.savedCardholderName
                var savedCardLastFour = state.savedCardLastFour

                // Remember only safe display information after a successful payment.
                // Full card number, expiry and CVV are never written to preferences.
                if (method == PromotionPaymentMethod.CARD && !state.useSavedCard) {
                    savedCardholderName = state.cardholderName.trim()
                    savedCardLastFour = state.cardNumber.takeLast(4)

                    paymentPreferences.edit {
                        putString("saved_cardholder_name", savedCardholderName)
                        putString("saved_card_last_four", savedCardLastFour)
                    }
                }

                paymentPreferences.edit {
                    putString("preferred_payment_method", method.name)
                }

                _uiState.value = _uiState.value.copy(
                    step = PromotionStep.SUCCESS,
                    isProcessing = false,
                    completedPromotion = completed,
                    promotionsByPostId = _uiState.value.promotionsByPostId +
                        (post.postId to promotionRecord),
                    preferredPaymentMethod = method,
                    savedCardholderName = savedCardholderName,
                    savedCardLastFour = savedCardLastFour,
                    useSavedCard = method == PromotionPaymentMethod.CARD &&
                        !savedCardLastFour.isNullOrBlank(),
                    promotionLoadError = null,
                    message = null
                )
            } catch (exception: Exception) {
                exception.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    message = exception.message
                        ?: "Unable to save the promotion. Please try again."
                )
            }
        }
    }

    /**
     * Returns true when an internal Promote page handled Back. Returning false
     * means the caller should leave the Promotions module itself.
     */
    fun goBack(): Boolean {
        val state = _uiState.value
        if (state.isProcessing) return true

        val previous = when (state.step) {
            PromotionStep.POST_SELECTION -> return false
            PromotionStep.PACKAGE -> PromotionStep.POST_SELECTION
            PromotionStep.REVIEW -> PromotionStep.PACKAGE
            PromotionStep.PAYMENT_METHOD -> PromotionStep.REVIEW
            PromotionStep.TOUCH_N_GO,
            PromotionStep.CARD -> PromotionStep.PAYMENT_METHOD
            PromotionStep.SUCCESS -> PromotionStep.POST_SELECTION
        }

        _uiState.value = state.copy(
            step = previous,
            message = null
        )
        return true
    }

    fun finishSuccess() {
        _uiState.value = _uiState.value.copy(
            step = PromotionStep.POST_SELECTION,
            selectedPost = null,
            selectedPackage = null,
            selectedPaymentMethod = null,
            completedPromotion = null,
            cardholderName = "",
            cardNumber = "",
            cardExpiry = "",
            cardCvv = "",
            useSavedCard = false,
            message = null,
            isProcessing = false
        )
    }

    fun isPostEligible(post: ManagePostItem): Boolean {
        return post.databaseStatus.equals("PUBLISHED", ignoreCase = true) &&
            post.timingState == PostTimingState.UPCOMING
    }

    fun canPurchaseAnyPackage(post: ManagePostItem): Boolean {
        return PromotionPackage.entries.any { option ->
            isPackageAvailable(post, option)
        }
    }

    fun currentPromotion(post: ManagePostItem): PromotionRecord? {
        return _uiState.value.promotionsByPostId[post.postId]
    }

    fun isPromotionActive(post: ManagePostItem): Boolean {
        val promotion = currentPromotion(post) ?: return false
        val now = AppClock.nowMillis()
        return promotion.startAtMillis <= now && now < promotion.endAtMillis
    }

    fun isExtension(post: ManagePostItem): Boolean = isPromotionActive(post)

    fun promotionBaseMillis(post: ManagePostItem): Long {
        val promotion = currentPromotion(post)
        return if (promotion != null && isPromotionActive(post)) {
            promotion.endAtMillis
        } else {
            AppClock.nowMillis()
        }
    }

    fun promotionCutoffMillis(post: ManagePostItem): Long? {
        val startDate = post.startDate ?: return null
        return parseDateAtStartOfDay(startDate)
    }

    fun remainingPromotionMillis(post: ManagePostItem): Long {
        val cutoff = promotionCutoffMillis(post) ?: return 0L
        return (cutoff - promotionBaseMillis(post)).coerceAtLeast(0L)
    }

    fun availablePromotionTimeLabel(post: ManagePostItem): String {
        val totalMinutes = remainingPromotionMillis(post) / 60_000L
        val days = totalMinutes / (24L * 60L)
        val hours = (totalMinutes % (24L * 60L)) / 60L
        val minutes = totalMinutes % 60L

        return when {
            totalMinutes <= 0L -> "No promotion time available"
            days > 0L && hours > 0L -> "$days d $hours h available"
            days > 0L && minutes > 0L -> "$days d $minutes min available"
            days > 0L -> "$days ${if (days == 1L) "day" else "days"} available"
            hours > 0L && minutes > 0L -> "$hours h $minutes min available"
            hours > 0L -> "$hours ${if (hours == 1L) "hour" else "hours"} available"
            else -> "$minutes min available"
        }
    }

    fun isPackageAvailable(
        post: ManagePostItem,
        promotionPackage: PromotionPackage
    ): Boolean {
        if (!isPostEligible(post)) return false
        val cutoff = promotionCutoffMillis(post) ?: return false
        val proposedEnd = promotionBaseMillis(post) + promotionPackage.durationMillis
        return proposedEnd <= cutoff
    }

    fun promotionStartMillis(post: ManagePostItem): Long = promotionBaseMillis(post)

    fun promotionEndMillis(
        post: ManagePostItem,
        promotionPackage: PromotionPackage
    ): Long? {
        if (!isPackageAvailable(post, promotionPackage)) return null
        return promotionBaseMillis(post) + promotionPackage.durationMillis
    }

    fun isCardValid(state: OrganisationPromotionUiState = _uiState.value): Boolean {
        return cardholderError(state.cardholderName) == null &&
            cardNumberError(state.cardNumber) == null &&
            expiryError(state.cardExpiry) == null &&
            cvvError(state.cardCvv) == null
    }

    fun cardholderError(value: String): String? {
        val clean = value.trim()
        val letterCount = clean.count(Char::isLetter)
        return when {
            clean.isEmpty() -> "Name on card is required."
            letterCount < 2 -> "Enter the name shown on the card."
            clean.any(Char::isDigit) -> "Name on card cannot contain numbers."
            else -> null
        }
    }

    fun cardNumberError(value: String): String? {
        val digits = value.filter(Char::isDigit)
        return when {
            digits.isEmpty() -> "Card number is required."
            digits.length != 16 -> "Enter a 16-digit card number."
            else -> null
        }
    }

    fun expiryError(value: String): String? {
        val digits = value.filter(Char::isDigit)
        if (digits.length != 4) return "Use MM/YY."

        val month = digits.take(2).toIntOrNull() ?: return "Use MM/YY."
        val year = digits.drop(2).toIntOrNull() ?: return "Use MM/YY."
        if (month !in 1..12) return "Enter a valid month."

        val now = Calendar.getInstance().apply { timeInMillis = AppClock.nowMillis() }
        val currentYear = now.get(Calendar.YEAR) % 100
        val currentMonth = now.get(Calendar.MONTH) + 1

        return if (year < currentYear || (year == currentYear && month < currentMonth)) {
            "This card has expired."
        } else {
            null
        }
    }

    fun cvvError(value: String): String? {
        return when {
            value.isEmpty() -> "CVV is required."
            value.length != 3 || !value.all(Char::isDigit) -> "Enter the 3-digit CVV."
            else -> null
        }
    }

    fun parseDateAtStartOfDay(value: String): Long? {
        val parsed = runCatching { promotionDateFormat().parse(value.trim()) }.getOrNull()
            ?: return null
        return startOfDay(parsed.time)
    }

    fun startOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun promotionDateFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
    }

    fun parsePromotionTimestamp(value: String): Long? {
        val normalized = value.trim().replace(
            Regex("""(\.\d{3})\d+(?=Z|[+-]\d{2}:?\d{2}$)"""),
            "$1"
        )

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd HH:mm:ss.SSSXXX",
            "yyyy-MM-dd HH:mm:ssXXX"
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }.parse(normalized)?.time
            }.getOrNull()
        }
    }
}

enum class PromotionStep {
    POST_SELECTION,
    PACKAGE,
    REVIEW,
    PAYMENT_METHOD,
    TOUCH_N_GO,
    CARD,
    SUCCESS
}

enum class PromotionPaymentMethod {
    TOUCH_N_GO,
    CARD
}

enum class PromotionPackage(
    val days: Int,
    val price: Double,
    val label: String,
    val supportingText: String
) {
    ONE_DAY(
        days = 1,
        price = 5.90,
        label = "1 Day",
        supportingText = "Quick visibility boost"
    ),
    THREE_DAYS(
        days = 3,
        price = 14.90,
        label = "3 Days",
        supportingText = "Recommended"
    ),
    SEVEN_DAYS(
        days = 7,
        price = 29.90,
        label = "7 Days",
        supportingText = "Best value"
    );

    val durationMillis: Long
        get() = days * 24L * 60L * 60L * 1000L
}

data class PromotionRecord(
    val promotionId: String,
    val postId: String,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val createdAtMillis: Long
)

data class PromotionPurchase(
    val promotionId: String,
    val paymentId: String,
    val postId: String,
    val title: String,
    val promotionPackage: PromotionPackage,
    val amount: Double,
    val paymentMethod: PromotionPaymentMethod,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val paidAtMillis: Long,
    val cutoffAtMillis: Long,
    val isExtension: Boolean
)

data class OrganisationPromotionUiState(
    val step: PromotionStep = PromotionStep.POST_SELECTION,
    val selectedPost: ManagePostItem? = null,
    val selectedPackage: PromotionPackage? = null,
    val selectedPaymentMethod: PromotionPaymentMethod? = null,
    val isProcessing: Boolean = false,
    val isLoadingPromotions: Boolean = true,
    val completedPromotion: PromotionPurchase? = null,
    val promotionsByPostId: Map<String, PromotionRecord> = emptyMap(),
    val cardholderName: String = "",
    val cardNumber: String = "",
    val cardExpiry: String = "",
    val cardCvv: String = "",
    val preferredPaymentMethod: PromotionPaymentMethod? = null,
    val savedCardholderName: String? = null,
    val savedCardLastFour: String? = null,
    val useSavedCard: Boolean = false,
    val message: String? = null,
    val promotionLoadError: String? = null
)

fun JsonObject.promotionText(key: String): String? {
    return this[key]?.jsonPrimitive?.contentOrNull
}

package com.example.volunteerlink.organisation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volunteerlink.R
import com.example.volunteerlink.organisation.components.OrganisationOfflineStatusCard
import com.example.volunteerlink.organisation.components.OrganisationStatusPill
import com.example.volunteerlink.organisation.manage.model.ManagePostItem
import com.example.volunteerlink.organisation.viewmodel.PromotionPackage
import com.example.volunteerlink.organisation.viewmodel.PromotionPaymentMethod
import com.example.volunteerlink.organisation.viewmodel.PromotionPurchase
import com.example.volunteerlink.organisation.viewmodel.PromotionRecord
import com.example.volunteerlink.ui.theme.VolunteerLinkBackground
import com.example.volunteerlink.ui.theme.VolunteerLinkBorderColour
import com.example.volunteerlink.ui.theme.VolunteerLinkInformation
import com.example.volunteerlink.ui.theme.VolunteerLinkPrimaryGreen
import com.example.volunteerlink.ui.theme.VolunteerLinkSoftGreenSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkSuccess
import com.example.volunteerlink.ui.theme.VolunteerLinkSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextPrimary
import com.example.volunteerlink.ui.theme.OrgTouchNGoBlue
import com.example.volunteerlink.ui.theme.OrgDisabledSurface
import com.example.volunteerlink.ui.theme.VolunteerLinkTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun PromotionPostSelectionScreen(
    isLoading: Boolean,
    errorMessage: String?,
    upcomingPosts: List<ManagePostItem>,
    promotionsByPostId: Map<String, PromotionRecord>,
    isShowingCachedData: Boolean,
    lastSyncedAtEpochMillis: Long?,
    isSyncing: Boolean,
    canPurchase: (ManagePostItem) -> Boolean,
    isPromotionActive: (ManagePostItem) -> Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPostClick: (ManagePostItem) -> Unit
) {
    PromotionPage(
        title = "Promote",
        subtitle = "Choose a post",
        onBack = onBack
    ) {
        when {
            isLoading -> {
                PromotionCenteredState {
                    CircularProgressIndicator(color = VolunteerLinkPrimaryGreen)
                    Text(
                        text = "Loading upcoming posts...",
                        modifier = Modifier.padding(top = 12.dp),
                        fontSize = 13.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }

            errorMessage != null -> {
                PromotionCenteredState {
                    Text(
                        text = "Unable to load posts",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(top = 7.dp),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                        color = VolunteerLinkTextSecondary
                    )
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VolunteerLinkPrimaryGreen
                        )
                    ) {
                        Text("Try Again", fontWeight = FontWeight.Bold)
                    }
                }
            }

            upcomingPosts.isEmpty() -> {
                PromotionCenteredState {
                    Text(
                        text = "No posts available to promote",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerLinkTextPrimary
                    )
                    Text(
                        text = "Published upcoming opportunities will appear here until the day they become active.",
                        modifier = Modifier.padding(top = 7.dp),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 18.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isShowingCachedData) {
                        item(key = "promotion_offline_status") {
                            OrganisationOfflineStatusCard(
                                lastSyncedAtEpochMillis = lastSyncedAtEpochMillis,
                                isSyncing = isSyncing,
                                onSyncSelected = onRetry
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Choose an upcoming opportunity",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary
                        )
                        Text(
                            text = "Boost visibility before volunteering begins.",
                            modifier = Modifier.padding(top = 5.dp, bottom = 4.dp),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = VolunteerLinkTextSecondary
                        )
                    }

                    items(upcomingPosts, key = { it.postId }) { post ->
                        PromotionPostCard(
                            post = post,
                            promotion = promotionsByPostId[post.postId],
                            canPurchase = canPurchase(post),
                            promotionActive = isPromotionActive(post),
                            onClick = { onPostClick(post) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionPackageScreen(
    post: ManagePostItem,
    selectedPackage: PromotionPackage?,
    message: String?,
    availableTime: String,
    isExtension: Boolean,
    currentPromotion: PromotionRecord?,
    cutoffMillis: Long?,
    isPackageAvailable: (PromotionPackage) -> Boolean,
    endMillisFor: (PromotionPackage) -> Long?,
    onBack: () -> Unit,
    onPackageClick: (PromotionPackage) -> Unit,
    onContinue: () -> Unit
) {
    PromotionPage(
        title = "Promote",
        subtitle = if (isExtension) "Extend promotion" else "Promotion duration",
        onBack = onBack,
        bottomContent = {
            PromotionPrimaryButton(
                text = selectedPackage?.let {
                    "Continue · ${formatPrice(it.price)}"
                } ?: "Continue",
                enabled = selectedPackage != null,
                onClick = onContinue
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PromotionSelectedPostSummary(post)
            }

            item {
                Text(
                    text = if (isExtension) "Choose extension duration" else "Choose promotion duration",
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = if (isExtension) {
                        "Extra time starts after the current promotion ends."
                    } else {
                        "Longer packages have a lower daily cost."
                    },
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 13.sp,
                    color = VolunteerLinkTextSecondary
                )
            }

            items(PromotionPackage.entries) { option ->
                val available = isPackageAvailable(option)
                PromotionPackageCard(
                    option = option,
                    selected = selectedPackage == option,
                    enabled = available,
                    unavailableReason = if (available) {
                        null
                    } else {
                        "Opportunity starts too soon for ${option.days} days"
                    },
                    endMillis = endMillisFor(option),
                    onClick = { onPackageClick(option) }
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = VolunteerLinkSoftGreenSurface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = availableTime,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkPrimaryGreen
                        )
                        Text(
                            text = buildString {
                                if (isExtension && currentPromotion != null) {
                                    append("Current promotion ends ")
                                    append(formatDisplayDateTime(currentPromotion.endAtMillis))
                                    append(". ")
                                }
                                append("Promotion must finish by ")
                                append(cutoffMillis?.let(::formatDisplayDateTime) ?: "the opportunity start")
                                append(".")
                            },
                            modifier = Modifier.padding(top = 3.dp),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = VolunteerLinkTextSecondary
                        )
                    }
                }
            }

            if (!message.isNullOrBlank()) {
                item {
                    Text(
                        text = message,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
fun PromotionReviewScreen(
    post: ManagePostItem,
    promotionPackage: PromotionPackage,
    isExtension: Boolean,
    startMillis: Long,
    endMillis: Long,
    cutoffMillis: Long?,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    PromotionPage(
        title = "Review promotion",
        subtitle = "Check before payment",
        onBack = onBack,
        bottomContent = {
            PromotionPrimaryButton(
                text = "Continue to Payment",
                enabled = true,
                onClick = onContinue
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { PromotionSelectedPostSummary(post) }

            item {
                PromotionSectionCard {
                    PromotionDetailRow(
                        "Promotion",
                        if (isExtension) "${promotionPackage.days} Day Extension" else "${promotionPackage.days} Day Boost"
                    )
                    PromotionDivider()
                    PromotionDetailRow(
                        if (isExtension) "Extension starts" else "Starts",
                        formatDisplayDateTime(startMillis)
                    )
                    PromotionDivider()
                    PromotionDetailRow("Ends", formatDisplayDateTime(endMillis))
                    PromotionDivider()
                    PromotionDetailRow(
                        "Opportunity begins",
                        cutoffMillis?.let(::formatDisplayDateTime)
                            ?: formatDisplayDate(post.startDate.orEmpty())
                    )
                }
            }

            item {
                PromotionSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${promotionPackage.days} Day Promotion",
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            color = VolunteerLinkTextSecondary
                        )
                        Text(
                            text = formatPrice(promotionPackage.price),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VolunteerLinkTextPrimary
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = VolunteerLinkBorderColour
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total",
                            modifier = Modifier.weight(1f),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary
                        )
                        Text(
                            text = formatPrice(promotionPackage.price),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkPrimaryGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionPaymentMethodScreen(
    postTitle: String,
    promotionPackage: PromotionPackage,
    preferredPaymentMethod: PromotionPaymentMethod?,
    savedCardLastFour: String?,
    onBack: () -> Unit,
    onTouchNGo: () -> Unit,
    onCard: () -> Unit
) {
    PromotionPage(
        title = "Payment",
        subtitle = "Choose payment method",
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Total",
                    fontSize = 13.sp,
                    color = VolunteerLinkTextSecondary
                )
                Text(
                    text = formatPrice(promotionPackage.price),
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = "$postTitle · ${promotionPackage.label}",
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = VolunteerLinkTextSecondary
                )
            }

            item {
                PromotionPaymentChoiceCard(
                    title = "Touch 'n Go eWallet",
                    subtitle = if (preferredPaymentMethod == PromotionPaymentMethod.TOUCH_N_GO) {
                        "Preferred · Pay using QR code"
                    } else {
                        "Pay using QR code"
                    },
                    badge = "TNG",
                    badgeColor = OrgTouchNGoBlue,
                    onClick = onTouchNGo
                )
            }

            item {
                PromotionPaymentChoiceCard(
                    title = "Credit / Debit Card",
                    subtitle = when {
                        preferredPaymentMethod == PromotionPaymentMethod.CARD &&
                            !savedCardLastFour.isNullOrBlank() ->
                            "Preferred · Saved card •••• $savedCardLastFour"
                        !savedCardLastFour.isNullOrBlank() ->
                            "Saved card · •••• $savedCardLastFour"
                        preferredPaymentMethod == PromotionPaymentMethod.CARD ->
                            "Preferred · Visa · Mastercard"
                        else -> "Visa · Mastercard"
                    },
                    badge = "CARD",
                    badgeColor = VolunteerLinkPrimaryGreen,
                    onClick = onCard
                )
            }
        }
    }
}

@Composable
fun PromotionTouchNGoScreen(
    post: ManagePostItem,
    promotionPackage: PromotionPackage,
    isProcessing: Boolean,
    message: String?,
    onBack: () -> Unit,
    onPay: () -> Unit
) {
    PromotionPage(
        title = "Touch 'n Go eWallet",
        subtitle = "QR payment",
        onBack = onBack,
        backEnabled = !isProcessing,
        bottomContent = {
            PromotionPrimaryButton(
                text = if (isProcessing) "Processing payment..." else "Pay ${formatPrice(promotionPackage.price)}",
                enabled = !isProcessing,
                loading = isProcessing,
                onClick = onPay
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = VolunteerLinkSurface,
                    border = BorderStroke(1.dp, VolunteerLinkBorderColour)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = OrgTouchNGoBlue
                        ) {
                            Text(
                                text = "Touch 'n Go eWallet",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = formatPrice(promotionPackage.price),
                            modifier = Modifier.padding(top = 18.dp),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerLinkTextPrimary
                        )

                        FakePaymentQr(
                            payload = "${post.postId}|${promotionPackage.days}|${promotionPackage.price}",
                            modifier = Modifier
                                .padding(top = 18.dp)
                                .size(218.dp)
                        )

                        Text(
                            text = "Scan the QR code using Touch 'n Go eWallet",
                            modifier = Modifier.padding(top = 16.dp),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = VolunteerLinkTextPrimary
                        )
                        Text(
                            text = "${promotionPackage.label} Promotion · ${post.title}",
                            modifier = Modifier.padding(top = 5.dp),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            color = VolunteerLinkTextSecondary
                        )
                    }
                }
            }

            if (!message.isNullOrBlank()) {
                item {
                    Text(
                        text = message,
                        modifier = Modifier.padding(top = 12.dp),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
fun PromotionCardPaymentScreen(
    postTitle: String,
    promotionPackage: PromotionPackage,
    savedCardholderName: String?,
    savedCardLastFour: String?,
    useSavedCard: Boolean,
    cardholderName: String,
    cardNumber: String,
    cardExpiry: String,
    cardCvv: String,
    cardholderError: String?,
    cardNumberError: String?,
    expiryError: String?,
    cvvError: String?,
    paymentMessage: String?,
    canPay: Boolean,
    isProcessing: Boolean,
    onCardholderChange: (String) -> Unit,
    onCardNumberChange: (String) -> Unit,
    onExpiryChange: (String) -> Unit,
    onCvvChange: (String) -> Unit,
    onUseAnotherCard: () -> Unit,
    onBack: () -> Unit,
    onPay: () -> Unit
) {
    PromotionPage(
        title = "Card payment",
        subtitle = "Credit / debit card",
        onBack = onBack,
        backEnabled = !isProcessing,
        bottomContent = {
            PromotionPrimaryButton(
                text = if (isProcessing) "Processing payment..." else "Pay ${formatPrice(promotionPackage.price)}",
                enabled = canPay && !isProcessing,
                loading = isProcessing,
                onClick = onPay
            )
        },
        bottomModifier = Modifier.imePadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = formatPrice(promotionPackage.price),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = "$postTitle · ${promotionPackage.label}",
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    fontSize = 13.sp,
                    color = VolunteerLinkTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            item {
                Text(
                    text = if (useSavedCard) "Saved card" else "Card information",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
            }

            if (useSavedCard &&
                !savedCardholderName.isNullOrBlank() &&
                !savedCardLastFour.isNullOrBlank()
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = VolunteerLinkSurface,
                        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = savedCardholderName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VolunteerLinkTextPrimary
                            )
                            Text(
                                text = "•••• •••• •••• $savedCardLastFour",
                                modifier = Modifier.padding(top = 6.dp),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = VolunteerLinkTextPrimary
                            )
                            Text(
                                text = "Saved on this device",
                                modifier = Modifier.padding(top = 5.dp),
                                fontSize = 12.sp,
                                color = VolunteerLinkTextSecondary
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Use another card",
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isProcessing, onClick = onUseAnotherCard)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VolunteerLinkPrimaryGreen
                    )
                }

                item {
                    Text(
                        text = "Only the cardholder name and last four digits are remembered. Full card number, expiry and CVV are not saved.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
            } else {
                item {
                    PromotionPaymentField(
                        value = cardNumber,
                        onValueChange = onCardNumberChange,
                        label = "Card number",
                        placeholder = "1234 5678 9012 3456",
                        error = cardNumberError,
                        keyboardType = KeyboardType.Number,
                        enabled = !isProcessing,
                        visualTransformation = CardNumberVisualTransformation
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PromotionPaymentField(
                            value = cardExpiry,
                            onValueChange = onExpiryChange,
                            label = "Expiry",
                            placeholder = "MM/YY",
                            error = expiryError,
                            keyboardType = KeyboardType.Number,
                            enabled = !isProcessing,
                            modifier = Modifier.weight(1f),
                            visualTransformation = CardExpiryVisualTransformation
                        )
                        PromotionPaymentField(
                            value = cardCvv,
                            onValueChange = onCvvChange,
                            label = "CVV",
                            placeholder = "123",
                            error = cvvError,
                            keyboardType = KeyboardType.NumberPassword,
                            enabled = !isProcessing,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    PromotionPaymentField(
                        value = cardholderName,
                        onValueChange = onCardholderChange,
                        label = "Name on card",
                        placeholder = "Name shown on card",
                        error = cardholderError,
                        keyboardType = KeyboardType.Text,
                        enabled = !isProcessing
                    )
                }

                item {
                    Text(
                        text = "After a successful payment, only the cardholder name and last four digits are remembered on this device.",
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }

            if (!paymentMessage.isNullOrBlank()) {
                item {
                    Text(
                        text = paymentMessage,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
fun PromotionPaymentSuccessScreen(
    promotion: PromotionPurchase,
    onDone: () -> Unit
) {
    PromotionPage(
        title = "Payment",
        subtitle = "Completed",
        onBack = onDone,
        bottomContent = {
            PromotionPrimaryButton(
                text = "Done",
                enabled = true,
                onClick = onDone
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(78.dp),
                shape = CircleShape,
                color = VolunteerLinkSuccess.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.tick),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = VolunteerLinkSuccess
                    )
                }
            }

            Text(
                text = "Payment Successful",
                modifier = Modifier.padding(top = 20.dp),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary
            )
            Text(
                text = formatPrice(promotion.amount),
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkPrimaryGreen
            )
            Text(
                text = promotion.title,
                modifier = Modifier.padding(top = 10.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = VolunteerLinkTextSecondary
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
                shape = RoundedCornerShape(16.dp),
                color = VolunteerLinkSoftGreenSurface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PromotionDetailRow(
                        "Promotion",
                        if (promotion.isExtension) {
                            "${promotion.promotionPackage.days} Day Extension"
                        } else {
                            "${promotion.promotionPackage.days} Day Boost"
                        }
                    )
                    PromotionDivider()
                    PromotionDetailRow(
                        "Active",
                        "${formatDisplayDateTime(promotion.startAtMillis)} – ${formatDisplayDateTime(promotion.endAtMillis)}"
                    )
                    PromotionDivider()
                    PromotionDetailRow(
                        "Payment",
                        when (promotion.paymentMethod) {
                            com.example.volunteerlink.organisation.viewmodel.PromotionPaymentMethod.TOUCH_N_GO ->
                                "Touch 'n Go eWallet"
                            com.example.volunteerlink.organisation.viewmodel.PromotionPaymentMethod.CARD ->
                                "Credit / Debit Card"
                        }
                    )
                    PromotionDivider()
                    PromotionDetailRow(
                        "Paid",
                        formatDisplayDateTime(promotion.paidAtMillis)
                    )
                }
            }
        }
    }
}

@Composable
fun PromotionPage(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    backEnabled: Boolean = true,
    bottomContent: (@Composable () -> Unit)? = null,
    bottomModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerLinkBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                enabled = backEnabled
            ) {
                Icon(
                    painter = painterResource(R.drawable.back),
                    contentDescription = "Back",
                    modifier = Modifier.size(21.dp),
                    tint = if (backEnabled) {
                        VolunteerLinkTextPrimary
                    } else {
                        VolunteerLinkTextSecondary.copy(alpha = 0.45f)
                    }
                )
            }

            Column(modifier = Modifier.padding(start = 3.dp)) {
                Text(
                    text = title,
                    fontSize = 19.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 1.dp),
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary
                )
            }
        }

        HorizontalDivider(color = VolunteerLinkBorderColour.copy(alpha = 0.85f))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            content = content
        )

        if (bottomContent != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(bottomModifier),
                color = VolunteerLinkSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = VolunteerLinkBorderColour.copy(alpha = 0.7f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    bottomContent()
                }
            }
        }
    }
}

@Composable
fun PromotionCenteredState(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun PromotionPostCard(
    post: ManagePostItem,
    promotion: PromotionRecord?,
    canPurchase: Boolean,
    promotionActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = canPurchase, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OrganisationStatusPill(
                    text = post.mode.uppercase(Locale.US),
                    color = VolunteerLinkPrimaryGreen
                )
                if (promotionActive) {
                    OrganisationStatusPill(
                        text = "PROMOTED",
                        color = VolunteerLinkInformation,
                        modifier = Modifier.padding(start = 7.dp)
                    )
                }
            }

            Text(
                text = post.title,
                modifier = Modifier.padding(top = 11.dp),
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = VolunteerLinkTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Starts ${formatDisplayDate(post.startDate.orEmpty())}",
                modifier = Modifier.padding(top = 6.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = VolunteerLinkTextSecondary
            )

            when {
                post.mode.equals("HYBRID", true) -> {
                    Text(
                        text = buildString {
                            post.remoteStartDate?.let {
                                append("Remote ${formatDisplayDate(it)}")
                            }
                            if (!post.remoteStartDate.isNullOrBlank() && !post.physicalStartDate.isNullOrBlank()) {
                                append(" · ")
                            }
                            post.physicalStartDate?.let {
                                append("Physical ${formatDisplayDate(it)}")
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 12.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }

                !post.locationName.isNullOrBlank() -> {
                    Text(
                        text = post.locationName,
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 12.sp,
                        color = VolunteerLinkTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (promotion != null) {
                Text(
                    text = if (promotionActive) {
                        "Promoted until ${formatDisplayDateTime(promotion.endAtMillis)}"
                    } else {
                        "Previous promotion ended ${formatDisplayDateTime(promotion.endAtMillis)}"
                    },
                    modifier = Modifier.padding(top = 9.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerLinkInformation
                )
            }

            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        !canPurchase -> "Starts too soon to promote"
                        promotionActive -> "Extend promotion"
                        promotion != null -> "Promote again"
                        else -> "Promote post"
                    },
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkPrimaryGreen
                )
                Icon(
                    painter = painterResource(R.drawable.back),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(180f),
                    tint = VolunteerLinkTextSecondary
                )
            }
        }
    }
}

@Composable
fun PromotionSelectedPostSummary(post: ManagePostItem) {
    PromotionSectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OrganisationStatusPill(
                text = post.mode.uppercase(Locale.US),
                color = VolunteerLinkPrimaryGreen
            )
            Text(
                text = "Starts ${formatDisplayDate(post.startDate.orEmpty())}",
                modifier = Modifier.padding(start = 8.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerLinkTextSecondary
            )
        }
        Text(
            text = post.title,
            modifier = Modifier.padding(top = 9.dp),
            fontSize = 16.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Bold,
            color = VolunteerLinkTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PromotionPackageCard(
    option: PromotionPackage,
    selected: Boolean,
    enabled: Boolean,
    unavailableReason: String?,
    endMillis: Long?,
    onClick: () -> Unit
) {
    val borderColor = when {
        selected && enabled -> VolunteerLinkPrimaryGreen
        else -> VolunteerLinkBorderColour
    }
    val containerColor = when {
        !enabled -> OrgDisabledSurface
        selected -> VolunteerLinkSoftGreenSurface
        else -> VolunteerLinkSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(if (selected && enabled) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected && enabled) VolunteerLinkPrimaryGreen
                        else Color.Transparent
                    )
                    .then(
                        Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(
                        1.5.dp,
                        if (selected && enabled) VolunteerLinkPrimaryGreen
                        else VolunteerLinkTextSecondary.copy(alpha = 0.45f)
                    )
                ) {}
                if (selected && enabled) {
                    Icon(
                        painter = painterResource(R.drawable.tick),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = option.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) VolunteerLinkTextPrimary else VolunteerLinkTextSecondary
                )
                Text(
                    text = when {
                        !enabled -> unavailableReason.orEmpty()
                        endMillis != null -> "${option.supportingText} · Ends ${formatDisplayDateTime(endMillis)}"
                        else -> option.supportingText
                    },
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = VolunteerLinkTextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrice(option.price),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) VolunteerLinkPrimaryGreen else VolunteerLinkTextSecondary
                )
                if (enabled && option.days > 1) {
                    Text(
                        text = "${formatPrice(option.price / option.days)}/day",
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 11.sp,
                        color = VolunteerLinkTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun PromotionPaymentChoiceCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(13.dp),
                color = badgeColor.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = badge,
                        fontSize = if (badge.length <= 3) 12.sp else 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeColor
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VolunteerLinkTextPrimary
                )
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 12.sp,
                    color = VolunteerLinkTextSecondary
                )
            }

            Text(
                text = "›",
                fontSize = 28.sp,
                color = VolunteerLinkTextSecondary
            )
        }
    }
}

object CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.take(16)
        val formatted = buildString {
            raw.forEachIndexed { index, char ->
                if (index > 0 && index % 4 == 0) append(' ')
                append(char)
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, raw.length)
                return safeOffset + when {
                    safeOffset <= 4 -> 0
                    safeOffset <= 8 -> 1
                    safeOffset <= 12 -> 2
                    else -> 3
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, formatted.length)
                return (safeOffset - when {
                    safeOffset <= 4 -> 0
                    safeOffset <= 9 -> 1
                    safeOffset <= 14 -> 2
                    else -> 3
                }).coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

object CardExpiryVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.take(4)
        val formatted = if (raw.length <= 2) {
            raw
        } else {
            "${raw.take(2)}/${raw.drop(2)}"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, raw.length)
                return if (safeOffset <= 2) safeOffset else safeOffset + 1
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, formatted.length)
                return (if (safeOffset <= 2) safeOffset else safeOffset - 1)
                    .coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
fun PromotionPaymentField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String?,
    keyboardType: KeyboardType,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let {
            { Text(it, fontSize = 11.sp) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VolunteerLinkPrimaryGreen,
            focusedLabelColor = VolunteerLinkPrimaryGreen,
            cursorColor = VolunteerLinkPrimaryGreen
        )
    )
}

@Composable
fun PromotionPrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VolunteerLinkPrimaryGreen,
            disabledContainerColor = VolunteerLinkPrimaryGreen.copy(alpha = 0.35f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(19.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(9.dp))
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PromotionSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = VolunteerLinkSurface,
        border = BorderStroke(1.dp, VolunteerLinkBorderColour)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun PromotionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = VolunteerLinkTextSecondary
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.25f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.End,
            fontWeight = FontWeight.SemiBold,
            color = VolunteerLinkTextPrimary
        )
    }
}

@Composable
fun PromotionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = VolunteerLinkBorderColour
    )
}

@Composable
fun FakePaymentQr(
    payload: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE3E3E3))
    ) {
        Canvas(modifier = Modifier.padding(12.dp)) {
            val modules = 29
            val cell = size.minDimension / modules
            val seed = payload.hashCode()

            for (row in 0 until modules) {
                for (column in 0 until modules) {
                    if (isInFinder(column, row, modules)) continue
                    val value = (
                        seed +
                            column * 97 +
                            row * 53 +
                            column * row * 11
                        ) xor (seed ushr ((column + row) % 16))
                    if ((value and 1) == 0) {
                        drawRect(
                            color = Color.Black,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                x = column * cell,
                                y = row * cell
                            ),
                            size = androidx.compose.ui.geometry.Size(cell, cell)
                        )
                    }
                }
            }

            drawFinder(0, 0, cell)
            drawFinder(modules - 7, 0, cell)
            drawFinder(0, modules - 7, cell)
        }
    }
}

fun DrawScope.drawFinder(column: Int, row: Int, cell: Float) {
    drawRect(
        color = Color.Black,
        topLeft = androidx.compose.ui.geometry.Offset(column * cell, row * cell),
        size = androidx.compose.ui.geometry.Size(7 * cell, 7 * cell)
    )
    drawRect(
        color = Color.White,
        topLeft = androidx.compose.ui.geometry.Offset((column + 1) * cell, (row + 1) * cell),
        size = androidx.compose.ui.geometry.Size(5 * cell, 5 * cell)
    )
    drawRect(
        color = Color.Black,
        topLeft = androidx.compose.ui.geometry.Offset((column + 2) * cell, (row + 2) * cell),
        size = androidx.compose.ui.geometry.Size(3 * cell, 3 * cell)
    )
}

fun isInFinder(column: Int, row: Int, modules: Int): Boolean {
    val topLeft = column in 0..6 && row in 0..6
    val topRight = column in (modules - 7)..(modules - 1) && row in 0..6
    val bottomLeft = column in 0..6 && row in (modules - 7)..(modules - 1)
    return topLeft || topRight || bottomLeft
}

fun formatPrice(value: Double): String =
    String.format(Locale.US, "RM%.2f", value)

fun formatDisplayDate(value: String): String {
    if (value.isBlank()) return "—"
    val input = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val output = SimpleDateFormat("d MMM yyyy", Locale.US)
    return runCatching {
        output.format(input.parse(value) ?: return value)
    }.getOrDefault(value)
}

fun formatDisplayDateTime(value: Long): String {
    return SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US).format(Date(value))
}
package com.example.volunteerlink.organisation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.volunteerlink.organisation.viewmodel.OrganisationManageViewModel
import com.example.volunteerlink.organisation.viewmodel.OrganisationPromotionViewModel
import com.example.volunteerlink.organisation.viewmodel.PromotionPaymentMethod
import com.example.volunteerlink.organisation.viewmodel.PromotionStep

@Composable
fun OrganisationPromotionScreen(
    onBack: () -> Unit,
    manageViewModel: OrganisationManageViewModel = viewModel(),
    promotionViewModel: OrganisationPromotionViewModel = viewModel()
) {
    val manageState by manageViewModel.uiState.collectAsStateWithLifecycle()
    val promotionState by promotionViewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                manageViewModel.refresh()
                promotionViewModel.refreshPromotions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        if (!promotionViewModel.goBack()) onBack()
    }

    when (promotionState.step) {
        PromotionStep.POST_SELECTION -> {
            PromotionPostSelectionScreen(
                isLoading = manageState.isLoading || promotionState.isLoadingPromotions,
                errorMessage = manageState.errorMessage ?: promotionState.promotionLoadError,
                upcomingPosts = manageState.upcomingPosts.filter(
                    promotionViewModel::isPostEligible
                ),
                promotionsByPostId = promotionState.promotionsByPostId,
                canPurchase = promotionViewModel::canPurchaseAnyPackage,
                isPromotionActive = promotionViewModel::isPromotionActive,
                onBack = onBack,
                onRetry = {
                    manageViewModel.refresh()
                    promotionViewModel.refreshPromotions()
                },
                onPostClick = promotionViewModel::selectPost
            )
        }

        PromotionStep.PACKAGE -> {
            val post = promotionState.selectedPost
            if (post == null) {
                promotionViewModel.finishSuccess()
                return
            }

            PromotionPackageScreen(
                post = post,
                selectedPackage = promotionState.selectedPackage,
                message = promotionState.message,
                availableTime = promotionViewModel.availablePromotionTimeLabel(post),
                isExtension = promotionViewModel.isExtension(post),
                currentPromotion = promotionViewModel.currentPromotion(post),
                cutoffMillis = promotionViewModel.promotionCutoffMillis(post),
                isPackageAvailable = { option ->
                    promotionViewModel.isPackageAvailable(post, option)
                },
                endMillisFor = { option ->
                    promotionViewModel.promotionEndMillis(post, option)
                },
                onBack = { promotionViewModel.goBack() },
                onPackageClick = promotionViewModel::selectPackage,
                onContinue = promotionViewModel::continueFromPackage
            )
        }

        PromotionStep.REVIEW -> {
            val post = promotionState.selectedPost
            val selectedPackage = promotionState.selectedPackage
            if (post == null || selectedPackage == null) {
                promotionViewModel.finishSuccess()
                return
            }

            PromotionReviewScreen(
                post = post,
                promotionPackage = selectedPackage,
                isExtension = promotionViewModel.isExtension(post),
                startMillis = promotionViewModel.promotionStartMillis(post),
                endMillis = promotionViewModel.promotionEndMillis(post, selectedPackage)
                    ?: promotionViewModel.promotionStartMillis(post),
                cutoffMillis = promotionViewModel.promotionCutoffMillis(post),
                onBack = { promotionViewModel.goBack() },
                onContinue = promotionViewModel::continueToPayment
            )
        }

        PromotionStep.PAYMENT_METHOD -> {
            val post = promotionState.selectedPost
            val selectedPackage = promotionState.selectedPackage
            if (post == null || selectedPackage == null) {
                promotionViewModel.finishSuccess()
                return
            }

            PromotionPaymentMethodScreen(
                postTitle = post.title,
                promotionPackage = selectedPackage,
                preferredPaymentMethod = promotionState.preferredPaymentMethod,
                savedCardLastFour = promotionState.savedCardLastFour,
                onBack = { promotionViewModel.goBack() },
                onTouchNGo = {
                    promotionViewModel.choosePaymentMethod(
                        PromotionPaymentMethod.TOUCH_N_GO
                    )
                },
                onCard = {
                    promotionViewModel.choosePaymentMethod(
                        PromotionPaymentMethod.CARD
                    )
                }
            )
        }

        PromotionStep.TOUCH_N_GO -> {
            val post = promotionState.selectedPost
            val selectedPackage = promotionState.selectedPackage
            if (post == null || selectedPackage == null) {
                promotionViewModel.finishSuccess()
                return
            }

            PromotionTouchNGoScreen(
                post = post,
                promotionPackage = selectedPackage,
                isProcessing = promotionState.isProcessing,
                message = promotionState.message,
                onBack = { promotionViewModel.goBack() },
                onPay = promotionViewModel::payWithTouchNGo
            )
        }

        PromotionStep.CARD -> {
            val post = promotionState.selectedPost
            val selectedPackage = promotionState.selectedPackage
            if (post == null || selectedPackage == null) {
                promotionViewModel.finishSuccess()
                return
            }

            PromotionCardPaymentScreen(
                postTitle = post.title,
                promotionPackage = selectedPackage,
                savedCardholderName = promotionState.savedCardholderName,
                savedCardLastFour = promotionState.savedCardLastFour,
                useSavedCard = promotionState.useSavedCard,
                cardholderName = promotionState.cardholderName,
                cardNumber = promotionState.cardNumber,
                cardExpiry = promotionState.cardExpiry,
                cardCvv = promotionState.cardCvv,
                cardholderError = if (promotionState.cardholderName.isBlank()) {
                    null
                } else {
                    promotionViewModel.cardholderError(promotionState.cardholderName)
                },
                cardNumberError = if (promotionState.cardNumber.isBlank()) {
                    null
                } else {
                    promotionViewModel.cardNumberError(promotionState.cardNumber)
                },
                expiryError = if (promotionState.cardExpiry.isBlank()) {
                    null
                } else {
                    promotionViewModel.expiryError(promotionState.cardExpiry)
                },
                cvvError = if (promotionState.cardCvv.isBlank()) {
                    null
                } else {
                    promotionViewModel.cvvError(promotionState.cardCvv)
                },
                paymentMessage = promotionState.message,
                canPay = if (promotionState.useSavedCard) {
                    !promotionState.savedCardholderName.isNullOrBlank() &&
                        !promotionState.savedCardLastFour.isNullOrBlank()
                } else {
                    promotionViewModel.isCardValid(promotionState)
                },
                isProcessing = promotionState.isProcessing,
                onCardholderChange = promotionViewModel::updateCardholder,
                onCardNumberChange = promotionViewModel::updateCardNumber,
                onExpiryChange = promotionViewModel::updateExpiry,
                onCvvChange = promotionViewModel::updateCvv,
                onUseAnotherCard = promotionViewModel::useAnotherCard,
                onBack = { promotionViewModel.goBack() },
                onPay = promotionViewModel::payWithCard
            )
        }

        PromotionStep.SUCCESS -> {
            val promotion = promotionState.completedPromotion
            if (promotion == null) {
                promotionViewModel.finishSuccess()
                return
            }

            PromotionPaymentSuccessScreen(
                promotion = promotion,
                onDone = promotionViewModel::finishSuccess
            )
        }
    }
}

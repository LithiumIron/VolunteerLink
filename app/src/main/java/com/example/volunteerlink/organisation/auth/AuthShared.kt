package com.example.volunteerlink.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun authFieldColours() =
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        cursorColor = MaterialTheme.colorScheme.primary
    )



fun isValidAuthPhoneNumber(phone: String): Boolean {
    return phone.matches(Regex("^\\+\\d{1,3} \\d{1,2}-\\d{7,9}$"))
}

@Composable
fun SharedOtpVerificationSection(
    email: String,
    otpCode: String,
    onOtpCodeChange: (String) -> Unit,
    isBusy: Boolean,
    errorMessage: String?,
    onVerify: () -> Unit,
    onUseDifferentEmail: () -> Unit,
    isResending: Boolean = false,
    onResend: (() -> Unit)? = null,
    codeLength: Int = 8
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Mail,
            contentDescription = null,
            modifier = Modifier.height(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Check your email",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(7.dp))

        Text(
            text = if (email.isNotBlank())
                "Enter the $codeLength-digit code we sent to $email."
            else
                "Enter the $codeLength-digit code we sent to your email.",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(26.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = { newValue ->
                // OTP is numeric-only; cap defensively at codeLength digits.
                onOtpCodeChange(newValue.filter { it.isDigit() }.take(codeLength))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Verification code") },
            placeholder = { Text("1".repeat(codeLength)) },
            singleLine = true,
            enabled = !isBusy,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(12.dp),
            colors = authFieldColours()
        )

        errorMessage?.let { message ->
            Spacer(Modifier.height(11.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = onVerify,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isBusy && otpCode.length == codeLength,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.height(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = "Verify email", fontWeight = FontWeight.Bold)
            }
        }

        // Only rendered when a caller actually wires up a resend flow —
        // e.g. VolunteerSignUpScreen can omit onResend until its
        // ViewModel has a matching resendVerificationEmail() function.
        if (onResend != null) {
            Spacer(Modifier.height(13.dp))
            TextButton(onClick = onResend, enabled = !isBusy && !isResending) {
                Text(if (isResending) "Resending..." else "Resend code")
            }
        }

        TextButton(onClick = onUseDifferentEmail, enabled = !isBusy) {
            Text("Use a different email")
        }

        Spacer(Modifier.height(24.dp))
    }
}

val countryCallingCodes = mapOf(
    "Malaysia" to "+60",
    "Singapore" to "+65",
    "Indonesia" to "+62",
    "Thailand" to "+66",
    "Philippines" to "+63",
    "Vietnam" to "+84",
    "United States" to "+1",
    "United Kingdom" to "+44",
    "Australia" to "+61",
    "India" to "+91",
    "Japan" to "+81",
    "South Korea" to "+82",
    "China" to "+86",
    "Netherlands" to "+31",
    "Germany" to "+49"
)


val OrganisationTypeOptions = listOf(
    "Registered NGO",
    "Community Organisation",
    "Foundation"
)

val countryStates = mapOf(

    "Malaysia" to mapOf(
        "Johor" to listOf("Johor Bahru", "Batu Pahat", "Muar", "Kluang", "Segamat", "Kota Tinggi"),
        "Kedah" to listOf("Alor Setar", "Sungai Petani", "Kulim", "Langkawi"),
        "Kelantan" to listOf("Kota Bharu", "Pasir Mas", "Tanah Merah"),
        "Melaka" to listOf("Melaka City", "Alor Gajah", "Jasin"),
        "Negeri Sembilan" to listOf("Seremban", "Port Dickson", "Nilai"),
        "Pahang" to listOf("Kuantan", "Temerloh", "Bentong", "Cameron Highlands"),
        "Penang" to listOf("George Town", "Bayan Lepas", "Butterworth", "Bukit Mertajam", "Ayer Itam"),
        "Perak" to listOf("Ipoh", "Taiping", "Teluk Intan", "Sitiawan", "Kampar"),
        "Perlis" to listOf("Kangar", "Arau"),
        "Sabah" to listOf("Kota Kinabalu", "Sandakan", "Tawau", "Lahad Datu"),
        "Sarawak" to listOf("Kuching", "Miri", "Sibu", "Bintulu"),
        "Selangor" to listOf("Shah Alam", "Petaling Jaya", "Klang", "Subang Jaya", "Puchong", "Kajang", "Ampang", "Putra Heights"),
        "Terengganu" to listOf("Kuala Terengganu", "Kemaman", "Dungun"),
        "Kuala Lumpur" to listOf("Kuala Lumpur"),
        "Putrajaya" to listOf("Putrajaya"),
        "Labuan" to listOf("Labuan")
    ),

    "Singapore" to mapOf(
        "Central Region" to listOf("Singapore", "Novena", "Toa Payoh"),
        "East Region" to listOf("Bedok", "Tampines", "Pasir Ris"),
        "West Region" to listOf("Jurong East", "Clementi", "Bukit Batok"),
        "North Region" to listOf("Woodlands", "Yishun", "Sembawang"),
        "North-East Region" to listOf("Sengkang", "Punggol", "Hougang")
    ),

    "Indonesia" to mapOf(
        "Jakarta" to listOf("Central Jakarta", "South Jakarta", "North Jakarta"),
        "West Java" to listOf("Bandung", "Bekasi", "Bogor"),
        "East Java" to listOf("Surabaya", "Malang"),
        "Bali" to listOf("Denpasar", "Ubud", "Kuta")
    ),

    "Thailand" to mapOf(
        "Bangkok" to listOf("Bangkok"),
        "Chiang Mai" to listOf("Chiang Mai City", "Mae Rim"),
        "Phuket" to listOf("Phuket City", "Patong")
    ),

    "Philippines" to mapOf(
        "Metro Manila" to listOf("Manila", "Quezon City", "Makati", "Taguig"),
        "Cebu" to listOf("Cebu City", "Mandaue"),
        "Davao" to listOf("Davao City")
    ),

    "Vietnam" to mapOf(
        "Hanoi" to listOf("Hanoi"),
        "Ho Chi Minh City" to listOf("District 1", "District 3", "Thu Duc"),
        "Da Nang" to listOf("Da Nang City")
    ),

    "United States" to mapOf(
        "California" to listOf("Los Angeles", "San Francisco", "San Diego"),
        "New York" to listOf("New York City", "Buffalo", "Albany"),
        "Texas" to listOf("Houston", "Austin", "Dallas")
    ),

    "United Kingdom" to mapOf(
        "England" to listOf("London", "Manchester", "Birmingham"),
        "Scotland" to listOf("Edinburgh", "Glasgow"),
        "Wales" to listOf("Cardiff", "Swansea")
    ),

    "Australia" to mapOf(
        "New South Wales" to listOf("Sydney", "Newcastle"),
        "Victoria" to listOf("Melbourne", "Geelong"),
        "Queensland" to listOf("Brisbane", "Gold Coast")
    ),

    "India" to mapOf(
        "Maharashtra" to listOf("Mumbai", "Pune"),
        "Delhi" to listOf("New Delhi"),
        "Karnataka" to listOf("Bengaluru", "Mysuru")
    ),

    "Japan" to mapOf(
        "Tokyo" to listOf("Shinjuku", "Shibuya", "Chiyoda"),
        "Osaka" to listOf("Osaka City"),
        "Kyoto" to listOf("Kyoto City")
    ),

    "South Korea" to mapOf(
        "Seoul" to listOf("Gangnam", "Jongno", "Mapo"),
        "Busan" to listOf("Busan City")
    ),

    "China" to mapOf(
        "Beijing" to listOf("Chaoyang", "Haidian"),
        "Shanghai" to listOf("Pudong", "Huangpu"),
        "Guangdong" to listOf("Guangzhou", "Shenzhen")
    ),

    "Netherlands" to mapOf(
        "North Holland" to listOf("Amsterdam", "Haarlem"),
        "South Holland" to listOf("Rotterdam", "The Hague")
    ),

    "Germany" to mapOf(
        "Bavaria" to listOf("Munich", "Nuremberg"),
        "Berlin" to listOf("Berlin"),
        "Hesse" to listOf("Frankfurt")
    )
)
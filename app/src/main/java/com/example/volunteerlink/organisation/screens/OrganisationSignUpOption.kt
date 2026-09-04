package com.example.volunteerlink.organisation

val OrganisationTypeOptions = listOf(
    "Registered NGO",
    "Community Organisation",
    "Foundation"
)

val countryStates = mapOf(

    "Malaysia" to mapOf(

        "Johor" to listOf(
            "Johor Bahru", "Batu Pahat", "Muar", "Kluang", "Segamat", "Kota Tinggi"
        ),

        "Kedah" to listOf(
            "Alor Setar", "Sungai Petani", "Kulim", "Langkawi"
        ),

        "Kelantan" to listOf(
            "Kota Bharu", "Pasir Mas", "Tanah Merah"
        ),

        "Melaka" to listOf(
            "Melaka City", "Alor Gajah", "Jasin"
        ),

        "Negeri Sembilan" to listOf(
            "Seremban", "Port Dickson", "Nilai"
        ),

        "Pahang" to listOf(
            "Kuantan", "Temerloh", "Bentong", "Cameron Highlands"
        ),

        "Penang" to listOf(
            "George Town", "Bayan Lepas", "Butterworth", "Bukit Mertajam", "Ayer Itam"
        ),

        "Perak" to listOf(
            "Ipoh", "Taiping", "Teluk Intan", "Sitiawan", "Kampar"
        ),

        "Perlis" to listOf(
            "Kangar", "Arau"
        ),

        "Sabah" to listOf(
            "Kota Kinabalu", "Sandakan", "Tawau", "Lahad Datu"
        ),

        "Sarawak" to listOf(
            "Kuching", "Miri", "Sibu", "Bintulu"
        ),

        "Selangor" to listOf(
            "Shah Alam", "Petaling Jaya", "Klang", "Subang Jaya",
            "Puchong", "Kajang", "Ampang", "Putra Heights"
        ),

        "Terengganu" to listOf(
            "Kuala Terengganu", "Kemaman", "Dungun"
        ),

        "Kuala Lumpur" to listOf(
            "Kuala Lumpur"
        ),

        "Putrajaya" to listOf(
            "Putrajaya"
        ),

        "Labuan" to listOf(
            "Labuan"
        )
    ),

    "Singapore" to mapOf(

        "Central Region" to listOf(
            "Singapore", "Novena", "Toa Payoh"
        ),

        "East Region" to listOf(
            "Bedok", "Tampines", "Pasir Ris"
        ),

        "West Region" to listOf(
            "Jurong East", "Clementi", "Bukit Batok"
        ),

        "North Region" to listOf(
            "Woodlands", "Yishun", "Sembawang"
        ),

        "North-East Region" to listOf(
            "Sengkang", "Punggol", "Hougang"
        )
    )
)
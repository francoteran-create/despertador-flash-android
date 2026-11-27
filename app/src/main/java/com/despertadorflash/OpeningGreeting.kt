package com.despertadorflash

internal fun openingSalutation(gender: NeonGender?): String = when (gender) {
    NeonGender.MAN -> "BIENVENIDO"
    NeonGender.WOMAN -> "BIENVENIDA"
    else -> "TE DAMOS LA BIENVENIDA"
}

internal fun birthdayToday(savedDate: String, day: Int, month: Int): Boolean {
    val parts = savedDate.split('/')
    return parts.size == 3 && parts.all { it.toIntOrNull() != null } &&
        parts[0].toInt() == day && parts[1].toInt() == month &&
        parts[2].toInt() in 1..9999
}

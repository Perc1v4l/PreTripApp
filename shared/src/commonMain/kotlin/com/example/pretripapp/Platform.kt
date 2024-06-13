package com.example.pretripapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
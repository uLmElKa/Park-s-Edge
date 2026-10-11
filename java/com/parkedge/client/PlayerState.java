package com.parkedge.client;

public enum PlayerState {
    IDLE,
    RUNNING,
    AIRBORNE,
    SIDESTEP,
    SHIFTING,
    SLIDING,
    COILING,
    WALLRUN,
    CLIMBING,      // Обычный залез
    WALLCLIMB,
    VAULTING,
    ROLLING,
    HARD_LANDING,
    HIGH_CLIMB     // <--- Новое: медленное перелазывание через объект 2-2.5 блока
}
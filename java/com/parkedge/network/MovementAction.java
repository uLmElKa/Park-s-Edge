package com.parkedge.network;

public enum MovementAction {
    SLIDE,
    ROLL,
    RESET_FALL   // сервер обнуляет дистанцию падения (хват за стену, зависание)
}
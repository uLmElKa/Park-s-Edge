package com.parkedge.client;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;

/** Настройки мода. Меняются в игре: Mod Menu -> ParkEdge. Файл: config/parkedge.json */
@Config(name = "parkedge")
public class ParkSEdgeConfig implements ConfigData {

    // --- Главные настройки ---
    @ConfigEntry.Category("general")
    public boolean modEnabled = true;                  // Главный переключатель мода

    // --- Скорость ---
    @ConfigEntry.Category("speed")
    public double maxSpeedKmh = 26.0;
    @ConfigEntry.Category("speed")
    public double hopCapKmh = 22.0;
    @ConfigEntry.Category("speed")
    public double hopDecaySeconds = 4.0;
    @ConfigEntry.Category("speed")
    public double safeTurnDegrees = 10.0;

    // --- Инерция ---
    @ConfigEntry.Category("inertia")
    public double startSeconds = 0.4;
    @ConfigEntry.Category("inertia")
    public double startInertia = 0.20;
    @ConfigEntry.Category("inertia")
    @ConfigEntry.Gui.Tooltip
    public double startCurve = 2.0;
    @ConfigEntry.Category("inertia")
    public double accelSeconds = 7.0;
    @ConfigEntry.Category("inertia")
    @ConfigEntry.Gui.Tooltip
    public double accelCurve = 1.0;
    @ConfigEntry.Category("inertia")
    public double stopSeconds = 0.35;
    @ConfigEntry.Category("inertia")
    @ConfigEntry.Gui.Tooltip
    public double stopCurve = 2.0;
    @ConfigEntry.Category("inertia")
    @ConfigEntry.Gui.Tooltip
    public double airDecayPerSecond = 0.10;
    @ConfigEntry.Category("inertia")
    @ConfigEntry.Gui.Tooltip
    public double hopMinMoveKmh = 8.0;

    // --- Боковой рывок (Sidestep) ---
    @ConfigEntry.Category("sidestep")
    public boolean sidestepEnabled = true;             // Включить боковой рывок
    @ConfigEntry.Category("sidestep")
    public double sidestepDistance = 1.3;
    @ConfigEntry.Category("sidestep")
    public int sidestepTicks = 4;
    @ConfigEntry.Category("sidestep")
    public double sidestepCooldownSeconds = 0.0;
    @ConfigEntry.Category("sidestep")
    public double sidestepMaxStartKmh = 3.0;
    @ConfigEntry.Category("sidestep")
    public double sidestepBoostSeconds = 1.0;
    @ConfigEntry.Category("sidestep")
    public double sidestepBoostAngle = 25.0;

    // --- Шифт, подкат и мягкая посадка ---
    @ConfigEntry.Category("slide")
    public boolean slideEnabled = true;                // Включить подкаты
    @ConfigEntry.Category("slide")
    public boolean rollEnabled = true;                 // Включить кувырок при падении
    @ConfigEntry.Category("slide")
    public boolean disableVanillaSneak = true;
    @ConfigEntry.Category("slide")
    public double shiftSpeedKmh = 11.5;
    @ConfigEntry.Category("slide")
    public double slideMinStartKmh = 15.0;
    @ConfigEntry.Category("slide")
    public double slideDecaySeconds = 3.0;
    @ConfigEntry.Category("slide")
    public double slideEndKmh = 14.0;

    // --- Залезание и висение на уступе ---
    @ConfigEntry.Category("climb")
    public boolean climbEnabled = true;                // Залезание на блоки
    @ConfigEntry.Category("climb")
    public double climbMinHeight = 2.0;
    @ConfigEntry.Category("climb")
    public double climbMaxHeight = 2.1;
    @ConfigEntry.Category("climb")
    public double climbReach = 0.7;
    @ConfigEntry.Category("climb")
    public double climbSpeed = 0.35;
    @ConfigEntry.Category("climb")
    public double highClimbSpeed = 0.18;
    @ConfigEntry.Category("climb")
    public double climbForward = 0.22;

    @ConfigEntry.Category("climb")
    public boolean grabEnabled = true;                 // Зацеп в воздухе
    @ConfigEntry.Category("climb")
    public double grabMinHeight = 0.8;
    @ConfigEntry.Category("climb")
    public double grabMaxHeight = 2.4;

    // --- Бег по стене (WallRun), цепочки и Tic-Tac ---
    @ConfigEntry.Category("wallrun")
    public boolean wallRunEnabled = true;              // Включить бег по стенам
    @ConfigEntry.Category("wallrun")
    public boolean wallRunChaining = true;             // Цепочка бега по разным стенам подряд
    @ConfigEntry.Category("wallrun")
    public boolean ticTacEnabled = true;               // Отскок Tic-Tac от стены
    @ConfigEntry.Category("wallrun")
    public double wallRunMinKmh = 12.0;
    @ConfigEntry.Category("wallrun")
    public double wallRunSeconds = 1.5;
    @ConfigEntry.Category("wallrun")
    public double wallRunReach = 0.25;
    @ConfigEntry.Category("wallrun")
    public double wallRunOppositeGap = 1.0;   // если напротив стены ближе этого расстояния есть другая стена, wallrun не работает (0 = выкл)
    @ConfigEntry.Category("wallrun")
    public double wallRunUpVy = 0.10;
    @ConfigEntry.Category("wallrun")
    public double wallRunPeak = 0.40;
    @ConfigEntry.Category("wallrun")
    public double wallRunEndVy = -0.08;
    @ConfigEntry.Category("wallrun")
    public double wallRunStick = 0.03;
    @ConfigEntry.Category("wallrun")
    public double wallJumpPush = 0.35;
    @ConfigEntry.Category("wallrun")
    public double wallJumpUp = 0.38;
    @ConfigEntry.Category("wallrun")
    public double wallJumpKeep = 1.0;

    @ConfigEntry.Category("wallrun")
    public boolean wallBoostEnabled = true;
    @ConfigEntry.Category("wallrun")
    @ConfigEntry.Gui.Tooltip
    public double wallRunInitialBoostKmh = 6.0;
    @ConfigEntry.Category("wallrun")
    @ConfigEntry.Gui.Tooltip
    public double wallBoostWindowSeconds = 0.45;
    @ConfigEntry.Category("wallrun")
    @ConfigEntry.Gui.Tooltip
    public double wallBoostDecaySeconds = 0.6;

    // --- Скалолазание по стене (WallClimb) ---
    @ConfigEntry.Category("wallclimb")
    public boolean wallClimbEnabled = true;            // Включить карабканье по стене
    @ConfigEntry.Category("wallclimb")
    public boolean wallClimbQuickTurnEnabled = true;   // Разворот на 180° и прыжок на другую стену
    @ConfigEntry.Category("wallclimb")
    public double wallClimbMaxHeight = 4.5;
    @ConfigEntry.Category("wallclimb")
    public double wallClimbMinKmh = 8.0;
    @ConfigEntry.Category("wallclimb")
    public double wallClimbSpeed = 0.28;
    @ConfigEntry.Category("wallclimb")
    public double wallClimbKickPush = 0.30;
    @ConfigEntry.Category("wallclimb")
    public double wallClimbKickUp = 0.40;
    @ConfigEntry.Category("wallclimb")
    public int wallClimbMaxChain = 2;   // сколько раз подряд можно залезть по стене, не касаясь земли
    @ConfigEntry.Category("wallclimb")
    public double wallClimbTurnHangSeconds = 1.7;

    // --- Vault (Перепрыгивание препятствий) ---
    @ConfigEntry.Category("vault")
    public boolean vaultEnabled = true;                // Включить Vault
    @ConfigEntry.Category("vault")
    public double vaultMinKmh = 8.0;
    @ConfigEntry.Category("vault")
    public double vaultReach = 1.3;
    @ConfigEntry.Category("vault")
    public double vaultMinHeight = 0.9;
    @ConfigEntry.Category("vault")
    public double vaultMaxHeight = 1.6;
    @ConfigEntry.Category("vault")
    public double vaultMaxDepth = 1.7;
    @ConfigEntry.Category("vault")
    public double vaultClearance = 0.10;

    // --- Coil (Группировка в воздухе) ---
    @ConfigEntry.Category("coil")
    public boolean coilEnabled = true;                 // Включить поджатие ног (Coil)
    @ConfigEntry.Category("coil")
    public double coilAirLiftPower = 0.2;
    @ConfigEntry.Category("coil")
    public double coilMaxHeight = 1.0;
    @ConfigEntry.Category("coil")
    public double coilReach = 0.3;
    @ConfigEntry.Category("coil")
    public double coilMaxAirTimeSeconds = 0.7;

    // --- Физика ---
    @ConfigEntry.Category("physics")
    @ConfigEntry.Gui.Tooltip
    public double gravity = 24.0;
    @ConfigEntry.Category("physics")
    @ConfigEntry.Gui.Tooltip
    public double jumpMultiplier = 1.0;
    @ConfigEntry.Category("physics")
    public boolean waterSprintEnabled = true;   // Ctrl в воде даёт ванильный бег на несколько тиков
    @ConfigEntry.Category("physics")
    public int waterSprintTicks = 3;

    // --- Эффект полос скорости ---
    @ConfigEntry.Category("display")
    public boolean streakEnabled = true;
    @ConfigEntry.Category("display")
    public int streakCount = 300;
    @ConfigEntry.Category("display")
    public double streakStartKmh = 18.0;
    @ConfigEntry.Category("display")
    public double streakMaxAlpha = 0.3;
    @ConfigEntry.Category("display")
    public int streakWidth = 10;
    @ConfigEntry.Category("display")
    public double streakLengthMult = 1.8;

    // --- FOV ---
    @ConfigEntry.Category("display")
    public double fovStrength = 0.35;
    @ConfigEntry.Category("display")
    public double fovReturnSeconds = 0.4;
    @ConfigEntry.Category("display")
    public double fovAttackSpeed = 0.18;
    @ConfigEntry.Category("display")
    public double wallRunFovKick = 0.15;
    @ConfigEntry.Category("display")
    public double slideFovKick = 0.20;
    @ConfigEntry.Category("display")
    public double sidestepFovKick = 0.25;
    @ConfigEntry.Category("display")
    public double coilFovKick = 0.10;
    @ConfigEntry.Category("display")
    public double landingFovCompress = 0.12;

    // --- Camera ---
    @ConfigEntry.Category("camera")
    public boolean cameraEnabled = true;
    @ConfigEntry.Category("camera")
    public double cameraRollStrength = 0.15;
    @ConfigEntry.Category("camera")
    public double cameraPitchStrength = 0.05;
    @ConfigEntry.Category("camera")
    public double cameraMaxAngle = 4.0;
    @ConfigEntry.Category("camera")
    public double cameraSmoothness = 0.25;
    @ConfigEntry.Category("camera")
    public double cameraClimbLift = 0.20;
    @ConfigEntry.Category("camera")
    public double cameraClimbPitch = 2.0;

    @ConfigEntry.Category("camera")
    public double runBobbingStrength = 1.0;
    @ConfigEntry.Category("camera")
    public double slideCameraDrop = 0.35;
    @ConfigEntry.Category("camera")
    public double landingImpactStrength = 1.0;
    @ConfigEntry.Category("camera")
    public double jumpPitchKick = 3.5;
    @ConfigEntry.Category("camera")
    public double vaultPitchKick = 4.5;

    @ConfigEntry.Category("camera")
    public double rollPitchAngle = 12.0;
    @ConfigEntry.Category("camera")
    public double rollDurationSeconds = 0.40;

    @ConfigEntry.Category("camera")
    public double wallRunRollAngle = 7.0;
    @ConfigEntry.Category("camera")
    public double wallRunRollDuration = 0.3;
    @ConfigEntry.Category("camera")
    public double wallJumpRollDuration = 0.3;

    // --- HUD ---
    @ConfigEntry.Category("hud")
    public boolean hudEnabled = true;
    @ConfigEntry.Category("hud")
    public int hudX = 6;
    @ConfigEntry.Category("hud")
    public int hudY = 6;
    @ConfigEntry.Category("hud")
    public double hudScale = 1.0;
    @ConfigEntry.Category("hud")
    @ConfigEntry.ColorPicker
    public int hudColor = 0xFFFFFF;
    @ConfigEntry.Category("hud")
    public boolean hudShowSpeed = true;
    @ConfigEntry.Category("hud")
    public boolean hudShowMomentum = false;
    @ConfigEntry.Category("hud")
    public boolean hudShowState = false;

    // --- Прицел ---
    @ConfigEntry.Category("crosshair")
    public boolean customCrosshair = true;
    @ConfigEntry.Category("crosshair")
    public int crosshairSize = 1;
    @ConfigEntry.Category("crosshair")
    public int crosshairOutline = 1;
    @ConfigEntry.Category("crosshair")
    @ConfigEntry.ColorPicker
    public int crosshairColor = 0xFFFFFF;
    @ConfigEntry.Category("crosshair")
    @ConfigEntry.ColorPicker
    public int crosshairOutlineColor = 0x000000;

    public static Screen createConfigScreen(Screen parent) {
        return AutoConfig.getConfigScreen(ParkSEdgeConfig.class, parent).get();
}

    @Override
    public void validatePostLoad() {
        maxSpeedKmh = MathHelper.clamp(maxSpeedKmh, 17.0, 60.0);
        hopCapKmh = MathHelper.clamp(hopCapKmh, 10.0, maxSpeedKmh);
        hopDecaySeconds = MathHelper.clamp(hopDecaySeconds, 0.5, 30.0);
        safeTurnDegrees = MathHelper.clamp(safeTurnDegrees, 1.0, 90.0);

        startSeconds = MathHelper.clamp(startSeconds, 0.05, 3.0);
        startInertia = MathHelper.clamp(startInertia, 0.05, 0.6);
        startCurve = MathHelper.clamp(startCurve, 1.0, 5.0);
        accelSeconds = MathHelper.clamp(accelSeconds, 0.5, 30.0);
        accelCurve = MathHelper.clamp(accelCurve, 1.0, 5.0);
        stopSeconds = MathHelper.clamp(stopSeconds, 0.05, 3.0);
        stopCurve = MathHelper.clamp(stopCurve, 1.0, 5.0);
        airDecayPerSecond = MathHelper.clamp(airDecayPerSecond, 0.0, 0.9);
        hopMinMoveKmh = MathHelper.clamp(hopMinMoveKmh, 0.0, 20.0);

        sidestepDistance = MathHelper.clamp(sidestepDistance, 0.5, 4.0);
        sidestepTicks = MathHelper.clamp(sidestepTicks, 1, 20);
        sidestepCooldownSeconds = MathHelper.clamp(sidestepCooldownSeconds, 0.0, 10.0);
        sidestepMaxStartKmh = MathHelper.clamp(sidestepMaxStartKmh, 0.0, 20.0);
        sidestepBoostSeconds = MathHelper.clamp(sidestepBoostSeconds, 0.0, 5.0);
        sidestepBoostAngle = MathHelper.clamp(sidestepBoostAngle, 5.0, 90.0);

        shiftSpeedKmh = MathHelper.clamp(shiftSpeedKmh, 3.0, 30.0);
        slideMinStartKmh = MathHelper.clamp(slideMinStartKmh, 5.0, 60.0);
        slideDecaySeconds = MathHelper.clamp(slideDecaySeconds, 0.3, 20.0);
        slideEndKmh = MathHelper.clamp(slideEndKmh, 0.0, 30.0);

        climbMinHeight = MathHelper.clamp(climbMinHeight, 2.0, 2.5);
        climbMaxHeight = MathHelper.clamp(climbMaxHeight, climbMinHeight, 3.0);
        highClimbSpeed = MathHelper.clamp(highClimbSpeed, 0.05, 0.3);
        climbReach = MathHelper.clamp(climbReach, 0.4, 1.2);
        climbSpeed = MathHelper.clamp(climbSpeed, 0.1, 0.8);
        climbForward = MathHelper.clamp(climbForward, 0.05, 0.6);

        grabMinHeight = MathHelper.clamp(grabMinHeight, 0.6, 2.0);
        grabMaxHeight = MathHelper.clamp(grabMaxHeight, grabMinHeight, 3.0);

        wallClimbMaxHeight = MathHelper.clamp(wallClimbMaxHeight, 2.5, 8.0);
        wallClimbMinKmh = MathHelper.clamp(wallClimbMinKmh, 0.0, 40.0);
        wallClimbSpeed = MathHelper.clamp(wallClimbSpeed, 0.1, 0.7);
        wallClimbKickPush = MathHelper.clamp(wallClimbKickPush, 0.0, 0.8);
        wallClimbKickUp = MathHelper.clamp(wallClimbKickUp, 0.1, 0.8);
        wallClimbMaxChain = MathHelper.clamp(wallClimbMaxChain, 1, 10);
        wallClimbTurnHangSeconds = MathHelper.clamp(wallClimbTurnHangSeconds, 0.1, 5.0);

        wallRunRollAngle = MathHelper.clamp(wallRunRollAngle, 0.0, 30.0);
        wallRunRollDuration = MathHelper.clamp(wallRunRollDuration, 0.05, 1.5);
        wallJumpRollDuration = MathHelper.clamp(wallJumpRollDuration, 0.05, 1.5);

        gravity = MathHelper.clamp(gravity, 4.0, 64.0);
        jumpMultiplier = MathHelper.clamp(jumpMultiplier, 0.3, 2.0);

        wallRunMinKmh = MathHelper.clamp(wallRunMinKmh, 5.0, 40.0);
        wallRunSeconds = MathHelper.clamp(wallRunSeconds, 0.2, 10.0);
        wallRunReach = MathHelper.clamp(wallRunReach, 0.1, 0.8);
        wallRunOppositeGap = MathHelper.clamp(wallRunOppositeGap, 0.0, 4.0);
        waterSprintTicks = MathHelper.clamp(waterSprintTicks, 1, 40);
        wallRunUpVy = MathHelper.clamp(wallRunUpVy, 0.0, 0.5);
        wallRunPeak = MathHelper.clamp(wallRunPeak, 0.1, 0.9);
        wallRunEndVy = MathHelper.clamp(wallRunEndVy, -0.4, 0.2);
        wallRunStick = MathHelper.clamp(wallRunStick, 0.0, 0.1);
        wallJumpPush = MathHelper.clamp(wallJumpPush, 0.0, 0.8);
        wallJumpUp = MathHelper.clamp(wallJumpUp, 0.1, 0.8);
        wallJumpKeep = MathHelper.clamp(wallJumpKeep, 0.3, 1.5);

        wallRunInitialBoostKmh = MathHelper.clamp(wallRunInitialBoostKmh, 0.0, 20.0);
        wallBoostWindowSeconds = MathHelper.clamp(wallBoostWindowSeconds, 0.05, 10.0);
        wallBoostDecaySeconds = MathHelper.clamp(wallBoostDecaySeconds, 0.1, 2.0);

        streakCount = MathHelper.clamp(streakCount, 10, 300);
        streakStartKmh = MathHelper.clamp(streakStartKmh, 5.0, 50.0);
        streakMaxAlpha = MathHelper.clamp(streakMaxAlpha, 0.1, 1.0);
        streakWidth = MathHelper.clamp(streakWidth, 1, 10);
        streakLengthMult = MathHelper.clamp(streakLengthMult, 0.2, 5.0);

        coilAirLiftPower = MathHelper.clamp(coilAirLiftPower, 0.1, 0.7);
        coilMaxHeight = MathHelper.clamp(coilMaxHeight, 0.3, 1.5);
        coilReach = MathHelper.clamp(coilReach, 0.3, 2.0);
        coilMaxAirTimeSeconds = MathHelper.clamp(coilMaxAirTimeSeconds, 0.1, 5.0);

        fovStrength = MathHelper.clamp(fovStrength, 0.0, 1.0);
        fovReturnSeconds = MathHelper.clamp(fovReturnSeconds, 0.05, 3.0);
        fovAttackSpeed = MathHelper.clamp(fovAttackSpeed, 0.05, 1.0);
        wallRunFovKick = MathHelper.clamp(wallRunFovKick, 0.0, 0.6);
        slideFovKick = MathHelper.clamp(slideFovKick, 0.0, 0.6);
        sidestepFovKick = MathHelper.clamp(sidestepFovKick, 0.0, 0.6);
        coilFovKick = MathHelper.clamp(coilFovKick, 0.0, 0.6);
        landingFovCompress = MathHelper.clamp(landingFovCompress, 0.0, 0.5);

        cameraRollStrength = MathHelper.clamp(cameraRollStrength, -2.0, 2.0);
        cameraPitchStrength = MathHelper.clamp(cameraPitchStrength, -2.0, 2.0);
        runBobbingStrength = MathHelper.clamp(runBobbingStrength, 0.0, 3.0);
        slideCameraDrop = MathHelper.clamp(slideCameraDrop, 0.0, 0.8);
        landingImpactStrength = MathHelper.clamp(landingImpactStrength, 0.0, 3.0);
        jumpPitchKick = MathHelper.clamp(jumpPitchKick, 0.0, 15.0);
        vaultPitchKick = MathHelper.clamp(vaultPitchKick, 0.0, 15.0);

        rollPitchAngle = MathHelper.clamp(rollPitchAngle, 0.0, 45.0);
        rollDurationSeconds = MathHelper.clamp(rollDurationSeconds, 0.1, 1.5);
        cameraMaxAngle = MathHelper.clamp(cameraMaxAngle, 0.0, 15.0);
        cameraSmoothness = MathHelper.clamp(cameraSmoothness, 0.05, 1.0);

        hudX = MathHelper.clamp(hudX, 0, 4000);
        hudY = MathHelper.clamp(hudY, 0, 4000);
        hudScale = MathHelper.clamp(hudScale, 0.5, 4.0);
        hudColor &= 0xFFFFFF;

        crosshairSize = MathHelper.clamp(crosshairSize, 1, 12);
        crosshairOutline = MathHelper.clamp(crosshairOutline, 0, 4);
        crosshairColor &= 0xFFFFFF;
        crosshairOutlineColor &= 0xFFFFFF;
    }

    public static ParkSEdgeConfig get() {
        return AutoConfig.getConfigHolder(ParkSEdgeConfig.class).getConfig();
    }
}
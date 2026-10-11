package com.parkedge.client;

import com.parkedge.network.MovementAction;
import com.parkedge.network.MovementActionPayload;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class MovementController {
    private MovementController() {}

    private static final double BASE_SPEED = 0.2158;
    private static final double KMH = 72.0;
    private static final double VANILLA_GRAVITY = 0.08;
    private static final double GROUND_FRICTION = 0.546;

    private static final float DECEL = 0.05f;
    private static final float WALL_LOSS = 0.3f;
    private static final float TURN_LOSS = 0.012f;
    private static final int CHAIN_TICKS = 6;
    private static final float HOP_GAIN = 0.10f;
    private static final double SPEED_MARGIN_KMH = 0.5;

    private static final Identifier MOMENTUM_ID = Identifier.of("parkedge", "momentum");
    private static final Identifier GRAVITY_ID = Identifier.of("parkedge", "gravity");
    private static final Identifier JUMP_ID = Identifier.of("parkedge", "jump");

    // Камера
    private static float prevRoll = 0f, roll = 0f;
    private static float prevPitchOff = 0f, pitchOff = 0f;
    private static float prevOffsetY = 0f, offsetY = 0f;

    // S-образная анимация WallRun
    private static float wallRollStart = 0f;
    private static float wallRollTarget = 0f;
    private static int wallRollTicks = 0;
    private static int wallRollMaxTicks = 0;
    private static boolean isWallRollAnimating = false;

    // Состояния анимаций
    private static int jumpCamTicks = 0;
    private static boolean isJumpCamAnimating = false;
    private static int coilCamTicks = 0;
    private static boolean isCoilCamAnimating = false;
    private static int landingTicks = 0;
    private static float landingImpactForce = 0f;
    private static float stepTimer = 0f;

    // Сохранение и удержание импульса WallBoost в воздухе
    private static int airBoostTicks = 0;
    private static double airBoostStartSpeed = 0.0;

    private static PlayerState state = PlayerState.IDLE;
    private static float momentum = 0f;
    private static int accelTicks = 0;
    private static float fovMomentum = 0f;
    private static double appliedBonus = 0.0;
    private static float lastYaw = 0f;
    private static double lastX = 0.0;
    private static double lastZ = 0.0;
    private static double currentKmh = 0.0;
    private static int runTicks = 0;
    private static int groundTicks = 0;
    private static double airSpeed = 0.0;
    private static boolean wasShift = false;
    private static boolean wasSprintKey = false;
    private static boolean waterSprinting = false;   // Плывём ли быстро в воде до остановки

    /** Быстрый плавательный бег в воде активен. */
    public static boolean isWaterSprint() {
        return waterSprinting;
    }
    private static int lastShiftAirTick = -1000;   // последний тик с нажатым shift в воздухе
    private static int lastRollSendTick = -1000;   // когда последний раз отправили ROLL на сервер
    // точная дистанция падения из ванильного handleFallDamage (та же, по которой сервер считает урон)
    private static double landingFall = 0.0;

    public static void onFallLanding(float d) {
        landingFall = d;
    }

    private static boolean wasAirborne = false;
    private static double fallStartY = 0.0;
    private static int rollTicks = 0;
    private static int hardLandingTicks = 0;
    private static float rollProgress = 0f;

    public static float rollProgress() {
        return rollProgress;
    }

    /** Математические кривые сглаживания (Easing functions) */
    public static float easeInOutCubic(float x) {
        if (x <= 0f) return 0f;
        if (x >= 1f) return 1f;
        return x < 0.5f
                ? 4f * x * x * x
                : 1f - (float) Math.pow(-2f * x + 2f, 3) / 2f;
    }

    public static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        if (x <= 0f) return 0f;
        if (x >= 1f) return 1f;
        return 1f + c3 * (float) Math.pow(x - 1f, 3) + c1 * (float) Math.pow(x - 1f, 2);
    }

    public static float easeOutQuad(float x) {
        if (x <= 0f) return 0f;
        if (x >= 1f) return 1f;
        return 1f - (1f - x) * (1f - x);
    }

    public static float easeOutQuint(float x) {
        if (x <= 0f) return 0f;
        if (x >= 1f) return 1f;
        return 1f - (float) Math.pow(1f - x, 5);
    }

    public static float easeInOutSine(float x) {
        if (x <= 0f) return 0f;
        if (x >= 1f) return 1f;
        return -(float) (Math.cos(Math.PI * x) - 1.0) / 2.0f;
    }

    private static int rollCamTicks = 0;
    private static int rollCamMaxTicks = 0;
    private static boolean isRollCamAnimating = false;

    private static int wallClimbTurnTicks = 0;
    private static boolean isHighClimb = false;

    public static void tryQuickTurn(ClientPlayerEntity player) {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (!cfg.modEnabled || !cfg.wallClimbQuickTurnEnabled) return;

        float newYaw = player.getYaw() + 180.0f;
        player.setYaw(newYaw);

        lastYaw = newYaw;
        lastDirX = -lastDirX;
        lastDirZ = -lastDirZ;
        wallDirX = -wallDirX;
        wallDirZ = -wallDirZ;

        if (wallClimbActive) {
            wallClimbTurnTicks = ticks(cfg.wallClimbTurnHangSeconds);
        }
    }

    private static boolean inChain = false;
    private static int chainTicks = 0;
    private static double chainStartSpeed = 0.0;

    private static boolean coilPending = false;
    private static int coilTicks = 0;
    private static boolean coilUsed = false;
    private static double coilLift = 0.0;

    private static boolean wallRunActive = false;
    private static int wallRunTicks = 0;
    private static int wallRunCooldown = 0;
    private static int wallSide = 0;
    private static double wallDirX = 0.0;
    private static double wallDirZ = 0.0;
    private static double wallRunInitialSpeed = 0.0;
    private static int jumpBufferTicks = 0;

    private static boolean hasLastWall = false;
    private static int lastWallAxis = 0;
    private static int lastWallDir = 0;
    private static int lastWallCoord = 0;

    private static boolean climbActive = false;
    private static int climbTicks = 0;
    private static double climbTop = 0.0;
    private static double climbDirX = 0.0;
    private static double climbDirZ = 0.0;
    private static double climbExitSpeed = 0.0;
    private static int airTicks = 0;
    private static boolean grabUsed = false;

    private static boolean wallClimbActive = false;
    private static boolean wallClimbUsed = false;
    private static int wallClimbChain = 0;
    private static int wallClimbTicks = 0;
    private static double wallClimbStartY = 0.0;
    private static double wallClimbLastY = 0.0;
    private static double wcDirX = 0.0;
    private static double wcDirZ = 0.0;

    private static boolean vaultActive = false;
    private static int vaultTicks = 0;
    private static double vaultDirX = 0.0;
    private static double vaultDirZ = 0.0;
    private static double vaultSpeed = 0.0;
    private static boolean vaultWasOnGround = true;
    private static double vaultGroundY = 0.0;

    private static double lastDisp = 0.0;
    private static double lastDirX = 0.0;
    private static double lastDirZ = 1.0;

    private static boolean wasJump = false;
    private static boolean jumpPressed = false;

    private static int sidestepTicks = 0;
    private static int sidestepCooldown = 0;
    private static int boostTicks = 0;
    private static double sidestepDirX = 0.0;
    private static double sidestepDirZ = 0.0;
    private static double sidestepSpeed = 0.0;

    private static volatile boolean slideActive = false;
    private static volatile java.util.UUID slideOwner;
    private static int slideTicks = 0;
    private static double slideStartSpeed = 0.0;
    private static double slideDirX = 0.0;
    private static double slideDirZ = 0.0;

    public static void onPreTravel(ClientPlayerEntity player, Vec3d input) {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (!cfg.modEnabled) return;

        if (coilPending) {
            coilPending = false;
            Vec3d v = player.getVelocity();
            player.setVelocity(v.x, Math.max(v.y, coilLift), v.z);
        }

        if (coilTicks > 0) {
            coilTicks--;
        }

        if (waterSprinting && player.isTouchingWater()) {
            player.setSprinting(true);
        }
    }

    public static float camRoll(float td) {
        return MathHelper.lerp(td, prevRoll, roll);
    }

    public static float camPitch(float td) {
        return MathHelper.lerp(td, prevPitchOff, pitchOff);
    }

    public static float camOffsetY(float td) {
        return MathHelper.lerp(td, prevOffsetY, offsetY);
    }

    private record Frame(ClientPlayerEntity player, ParkSEdgeConfig cfg, boolean onGround,
                         boolean forward, boolean hasInput, boolean shift,
                         boolean left, boolean right, boolean back,
                         double disp, double dirX, double dirZ, float yaw, float turn) {
    }

    public static double hudKmh() {
        return currentKmh;
    }

    public static float momentum() {
        if (state == PlayerState.SHIFTING) {
            ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
            return (float) Math.min(1.0, cfg.shiftSpeedKmh / cfg.maxSpeedKmh);
        }
        return momentum;
    }

    public static PlayerState state() {
        return state;
    }

    public static boolean isInputLocked() {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (!cfg.modEnabled) return false;
        return slideActive || climbActive || wallClimbActive;
    }

    public static boolean isSlidingFor(java.util.UUID id) {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (!cfg.modEnabled || !cfg.slideEnabled) return false;
        return slideActive && id.equals(slideOwner);
    }

    public static boolean tryStartClimb(ClientPlayerEntity p) {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (!cfg.modEnabled || !cfg.climbEnabled) return false;
        if (!MinecraftClient.getInstance().options.forwardKey.isPressed()) return false;
        return startLedgeClimb(p, cfg, cfg.climbMinHeight, cfg.climbMaxHeight);
    }

    private static boolean tryStartAirGrab(ClientPlayerEntity p, ParkSEdgeConfig cfg) {
        if (!cfg.modEnabled || !cfg.grabEnabled) return false;
        return startLedgeClimb(p, cfg, cfg.grabMinHeight, cfg.grabMaxHeight);
    }

    private static boolean startLedgeClimb(ClientPlayerEntity p, ParkSEdgeConfig cfg, double minH, double maxH) {
        double rad = Math.toRadians(p.getYaw());
        return startLedgeClimb(p, cfg, minH, maxH, -Math.sin(rad), Math.cos(rad));
    }

    private static boolean startLedgeClimb(ClientPlayerEntity p, ParkSEdgeConfig cfg, double minH, double maxH,
                                           double dx, double dz) {
        if (!cfg.modEnabled || climbActive || vaultActive || wallRunActive || slideActive || sidestepTicks > 0)
            return false;

        double r = cfg.climbReach;
        var w = p.getWorld();
        net.minecraft.util.math.Box b = p.getBoundingBox();

        boolean hasWallAt1_7 = !w.isSpaceEmpty(p, b.offset(dx * r, 1.7, dz * r));
        if (!hasWallAt1_7) return false;

        double actualMinH = Math.max(1.8, minH);

        for (double h = actualMinH; h <= maxH + 1e-6; h += 0.1) {
            if (w.isSpaceEmpty(p, b.offset(dx * r, h, dz * r))) {
                if (!w.isSpaceEmpty(p, b.offset(0, h, 0))) return false;

                wallClimbActive = false;
                climbActive = true;
                climbTicks = 0;
                climbTop = p.getY() + h + 0.02;
                climbDirX = dx;
                climbDirZ = dz;

                isHighClimb = (h >= 2.0);

                climbExitSpeed = Math.min(Math.max(cfg.climbForward, lastDisp), cfg.maxSpeedKmh / KMH);
                return true;
            }
        }
        return false;
    }

    private static void tryStartVault(Frame f) {
        ParkSEdgeConfig cfg = f.cfg();
        ClientPlayerEntity p = f.player();
        if (!cfg.modEnabled || !cfg.vaultEnabled) return;

        double speed = f.disp();
        if (speed * KMH < cfg.vaultMinKmh) return;
        double dx = f.dirX(), dz = f.dirZ();
        var w = p.getWorld();
        double dy = p.getY() - vaultGroundY;
        net.minecraft.util.math.Box b = p.getBoundingBox().offset(0, -dy, 0);

        double d = -1.0;
        for (double t = 0.1; t <= cfg.vaultReach + 1e-6; t += 0.1) {
            if (!w.isSpaceEmpty(p, b.offset(dx * t, 0.2, dz * t))) {
                d = t;
                break;
            }
        }
        if (d < 0) return;

        double h = -1.0;
        for (double y = 0.4; y <= cfg.vaultMaxHeight + 1e-6; y += 0.05) {
            if (w.isSpaceEmpty(p, b.offset(dx * (d + 0.1), y, dz * (d + 0.1)))) {
                h = y;
                break;
            }
        }
        if (h < cfg.vaultMinHeight) return;
        if (!w.isSpaceEmpty(p, b.offset(0, h, 0))) return;

        double tFree = -1.0;
        for (double t = d + 0.1; t <= d + cfg.vaultMaxDepth + 1e-6; t += 0.1) {
            if (w.isSpaceEmpty(p, b.offset(dx * t, 0.2, dz * t))) {
                tFree = t;
                break;
            }
        }
        if (tFree < 0) return;

        double g = cfg.gravity / 400.0;
        double need = h + cfg.vaultClearance;
        int kObs = Math.max(1, (int) Math.ceil(d / Math.max(speed, 0.05)));
        int kEnd = (int) Math.ceil(tFree / Math.max(speed, 0.05)) + 1;
        double v0 = -1.0;

        for (double c = 0.2; c <= 0.9 + 1e-9; c += 0.01) {
            double y = dy, vy = c;
            boolean ok = true;
            for (int k = 1; k <= kEnd; k++) {
                y += vy;
                vy = (vy - g) * 0.98;
                if (k >= kObs && y < need) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                v0 = c;
                break;
            }
        }
        if (v0 < 0) v0 = 0.4;

        Vec3d v = p.getVelocity();
        p.setVelocity(v.x, v0, v.z);
        vaultActive = true;
        vaultTicks = 0;
        vaultDirX = dx;
        vaultDirZ = dz;
        vaultSpeed = Math.max(speed, 0.2);
    }

    public static boolean tryStartWallClimbFromJump(ClientPlayerEntity p) {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (!cfg.modEnabled || !cfg.wallClimbEnabled) return false;
        if (!MinecraftClient.getInstance().options.forwardKey.isPressed()) return false;
        return startWallClimb(p, cfg, lastDisp);
    }

    private static boolean startWallClimb(ClientPlayerEntity p, ParkSEdgeConfig cfg, double speed) {
        if (!cfg.modEnabled || !cfg.wallClimbEnabled || wallClimbUsed || wallClimbChain >= cfg.wallClimbMaxChain
                || wallClimbActive || climbActive || vaultActive
                || wallRunActive || slideActive || sidestepTicks > 0 || p.isTouchingWater()) return false;

        double rad = Math.toRadians(p.getYaw());
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);
        double r = cfg.climbReach;
        var w = p.getWorld();
        net.minecraft.util.math.Box b = p.getBoundingBox();

        int ax = 0, az = 0;
        if (Math.abs(dx) >= Math.abs(dz)) {
            ax = (int) Math.signum(dx);
        } else {
            az = (int) Math.signum(dz);
        }

        double dot = ax * dx + az * dz;
        if (dot < 0.70) return false;

        boolean hasWallInFront = !w.isSpaceEmpty(p, b.offset(ax * r, 0.5, az * r))
                || !w.isSpaceEmpty(p, b.offset(ax * r, 1.0, az * r));
        if (!hasWallInFront) return false;

        boolean standingAtWall = p.horizontalCollision || !w.isSpaceEmpty(p, b.offset(ax * 0.2, 0.2, az * 0.2));
        if (!standingAtWall && speed * KMH < cfg.wallClimbMinKmh) return false;

        wallClimbActive = true;
        wallClimbUsed = true;
        wallClimbChain++;
        wallClimbTicks = 0;
        wallClimbStartY = p.getY();
        wallClimbLastY = p.getY();
        wcDirX = ax;
        wcDirZ = az;
        return true;
    }

    public static boolean tryStartSidestep(ClientPlayerEntity player) {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (!cfg.modEnabled || !cfg.sidestepEnabled) return false;

        float side = player.input.movementSideways;
        if (side == 0 || slideActive) return false;
        if (sidestepTicks > 0) return true;

        double rad = Math.toRadians(player.getYaw());
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);
        sidestepDirX = side > 0 ? fz : -fz;
        sidestepDirZ = side > 0 ? -fx : fx;

        double capPerTick = (cfg.maxSpeedKmh + SPEED_MARGIN_KMH) / KMH;
        int minTicks = (int) Math.ceil(cfg.sidestepDistance / capPerTick);
        sidestepTicks = Math.max(Math.max(1, cfg.sidestepTicks), minTicks);
        sidestepSpeed = cfg.sidestepDistance / sidestepTicks;
        boostTicks = 0;

        Vec3d v = player.getVelocity();
        double targetX = v.x + sidestepDirX * sidestepSpeed;
        double targetZ = v.z + sidestepDirZ * sidestepSpeed;

        double maxCap = cfg.maxSpeedKmh / KMH;
        double hSpeed = Math.hypot(targetX, targetZ);
        if (hSpeed > maxCap) {
            targetX *= maxCap / hSpeed;
            targetZ *= maxCap / hSpeed;
        }

        player.setVelocity(targetX, Math.max(v.y, 0.1), targetZ);
        return true;
    }

    public static void limitSpeed(ClientPlayerEntity player, double prevX, double prevZ) {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (!cfg.modEnabled) return;

        if (player.isFallFlying() || player.getAbilities().flying) return;

        if (player.isTouchingWater() && !slideActive && vaultTicks == 0) {
            double waterCap = 11.5 / KMH;
            Vec3d v = player.getVelocity();
            if (Math.hypot(v.x, v.z) > waterCap) {
                double s = waterCap / Math.hypot(v.x, v.z);
                player.setVelocity(v.x * s, v.y, v.z * s);
            }
            return;
        }

        double dx = player.getX() - prevX;
        double dz = player.getZ() - prevZ;
        double d = Math.sqrt(dx * dx + dz * dz);

        double allowedMaxKmh = cfg.maxSpeedKmh + cfg.wallRunInitialBoostKmh + SPEED_MARGIN_KMH;
        double cap = Math.max(allowedMaxKmh / KMH, airSpeed + (SPEED_MARGIN_KMH / KMH));

        if (d > cap && d < 1.0) {
            double s = cap / d;
            player.setPosition(prevX + dx * s, player.getY(), prevZ + dz * s);
            Vec3d v = player.getVelocity();
            player.setVelocity(v.x * s, v.y, v.z * s);
        }
    }

    public static float fovCorrection() {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (!cfg.modEnabled) return 1.0f;

        double vanilla = Math.max(0.1, 1.0 + appliedBonus * 0.5);
        float curvedFov = easeOutQuad(MathHelper.clamp(fovMomentum, 0f, 1.5f));
        double ours = 1.0 + curvedFov * maxBonus(cfg) * 0.5 * cfg.fovStrength;

        return (float) (ours / vanilla);
    }

    public static void tick(ClientPlayerEntity player) {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        cfg.validatePostLoad();

        if (!cfg.modEnabled) {
            if (slideActive || climbActive || wallClimbActive || vaultActive || wallRunActive) {
                slideActive = false;
                climbActive = false;
                wallClimbActive = false;
                vaultActive = false;
                wallRunActive = false;
                player.setPose(EntityPose.STANDING);
            }

            clearAttributes(player);
            state = PlayerState.IDLE;
            momentum = 0f;
            fovMomentum = 0f;
            appliedBonus = 0.0;
            roll = pitchOff = offsetY = 0f;
            prevRoll = prevPitchOff = prevOffsetY = 0f;
            return;
        }

        Frame f = measure(player, cfg);
        if (f.forward() || !f.onGround()) stopping = false;
        lastDisp = f.disp();
        lastDirX = f.dirX();
        lastDirZ = f.dirZ();

        if (f.onGround()) {
            airBoostTicks = 0;
            lastShiftAirTick = -1000;
        }

        boolean hasDirectionalInput = f.forward() || f.back() || f.left() || f.right();
        Vec3d vel = player.getVelocity();
        double horizontalSpeed = Math.hypot(vel.x, vel.z);

        if (!hasDirectionalInput || horizontalSpeed < 0.02) {
            if (f.onGround()) {
                if (!f.forward() && momentum > 0.0f) {
                    tickStoppingSlide(player, f);
                } else {
                    momentum = 0.0f;
                    accelTicks = 0;
                }
            } else {
                momentum = 0.0f;
                accelTicks = 0;
                if (airBoostTicks == 0) airSpeed = 0.0;
            }
        }

        if (vaultActive) {
            vaultTicks++;
            if (vaultTicks > 40 || (f.onGround() && vaultTicks > 2)) {
                vaultActive = false;
                airSpeed = vaultSpeed;
            }
        }

        runTicks = f.forward() ? runTicks + 1 : 0;
        if (sidestepCooldown > 0 && sidestepTicks == 0) sidestepCooldown--;

        boolean shiftPressed = f.shift() && !wasShift;
        wasShift = f.shift();

        boolean sprintNow = MinecraftClient.getInstance().options.sprintKey.isPressed();
        if (!player.isTouchingWater()) {
            waterSprinting = false;
        } else {
            if (sprintNow && !wasSprintKey && cfg.waterSprintEnabled) {
                waterSprinting = true;
            }
            if (!f.forward() || f.disp() < 0.001) {
                waterSprinting = false;
            }
        }
        wasSprintKey = sprintNow;

        boolean jumpNow = MinecraftClient.getInstance().options.jumpKey.isPressed();
        jumpPressed = jumpNow && !wasJump;
        wasJump = jumpNow;

        if (jumpPressed && f.onGround() && !wallRunActive && !climbActive && !wallClimbActive) {
            jumpCamTicks = 0;
            isJumpCamAnimating = true;
        }

        if (jumpPressed && !f.onGround()) {
            jumpBufferTicks = 5;
        } else if (jumpBufferTicks > 0) {
            jumpBufferTicks--;
        }

        updateWallRun(f);

        if (ParkSEdgeClient.quickTurnKey != null && ParkSEdgeClient.quickTurnKey.wasPressed()) {
            tryQuickTurn(player);
        }

        boolean airborneNow = !f.onGround() && state != PlayerState.WALLRUN && state != PlayerState.WALLCLIMB;

        if (airborneNow && !wasAirborne) {
            fallStartY = player.getY();
            landingFall = 0.0;
        }

        if (airborneNow && player.getVelocity().y < -0.1 && cfg.rollEnabled) {
            if (shiftPressed || (f.shift() && player.age - lastRollSendTick >= 5)) {
                lastShiftAirTick = player.age;
                lastRollSendTick = player.age;
                NetworkManager.sendToServer(new MovementActionPayload(MovementAction.ROLL, true));
            }
        }

        if (!airborneNow && wasAirborne && f.onGround()) {
            double fallDistance = landingFall > 0 ? landingFall : (fallStartY - player.getY());
            landingFall = 0.0;

            if (fallDistance > 1.2) {
                landingTicks = 12;
                landingImpactForce = (float) MathHelper.clamp((fallDistance - 1.2) / 3.0, 0.3, 1.0);
            }

            if (fallDistance > 3.0) {
                boolean wantedRoll = f.shift() || (player.age - lastShiftAirTick <= 20);

                if (cfg.rollEnabled && wantedRoll) {
                    player.fallDistance = 0;
                    state = PlayerState.ROLLING;
                    rollTicks = 12;
                    momentum = Math.max(momentum, 0.75f);
                    accelTicks = (int) (momentum * cfg.accelSeconds * 20.0);

                    rollCamTicks = 0;
                    rollCamMaxTicks = Math.max(1, ticks(cfg.rollDurationSeconds));
                    isRollCamAnimating = true;
                } else {
                    state = PlayerState.HARD_LANDING;
                    hardLandingTicks = 15;
                    momentum = 0f;
                    accelTicks = 0;
                    airSpeed = 0.0;
                }
            }
        }
        wasAirborne = airborneNow;

        if (rollTicks > 0) {
            rollTicks--;
            rollProgress = 1.0f - ((float) rollTicks / 12.0f);
            if (rollTicks == 0) rollProgress = 0f;
        }

        if (hardLandingTicks > 0) {
            hardLandingTicks--;
            player.setVelocity(0, player.getVelocity().y, 0);
        }

        airTicks = f.onGround() ? 0 : airTicks + 1;
        if (f.onGround()) grabUsed = false;
        if (f.onGround() && !wallClimbActive && !climbActive) {
            wallClimbUsed = false;
            wallClimbChain = 0;
        }

        if ((jumpPressed || (jumpNow && player.horizontalCollision)) && !wallRunActive && !climbActive && !wallClimbActive && !vaultActive) {
            if (f.forward() && startWallClimb(player, cfg, f.disp())) {
            } else if (airTicks > 2 && !grabUsed && tryStartAirGrab(player, cfg)) {
                grabUsed = true;
            }
        }

        if (f.onGround()) coilUsed = false;
        if (cfg.coilEnabled && shiftPressed && !f.onGround() && !coilUsed
                && airTicks <= ticks(cfg.coilMaxAirTimeSeconds)
                && !wallRunActive && !wallClimbActive && !climbActive && !vaultActive) {
            coilLift = cfg.coilAirLiftPower;
            coilPending = true;
            coilTicks = 10;
            coilUsed = true;
            airSpeed = Math.max(airSpeed, f.disp());

            coilCamTicks = 0;
            isCoilCamAnimating = true;
        }

        if (slideActive && !f.onGround()) {
            slideActive = false;
            NetworkManager.sendToServer(new MovementActionPayload(MovementAction.SLIDE, false));
        }

        if (cfg.slideEnabled && shiftPressed && f.onGround() && !slideActive && !wallClimbActive && !climbActive && !vaultActive && sidestepTicks == 0
                && f.disp() * KMH >= cfg.slideMinStartKmh) {
            startSlide(f);
        }

        if (vaultWasOnGround && !f.onGround() && !vaultActive && f.forward()
                && player.getVelocity().y > 0.1
                && MinecraftClient.getInstance().options.jumpKey.isPressed()
                && !wallRunActive && !climbActive && !wallClimbActive && !slideActive && sidestepTicks == 0) {
            tryStartVault(f);
        }
        if (f.onGround()) vaultGroundY = player.getY();
        vaultWasOnGround = f.onGround();

        PlayerState next = resolveState(f);
        if (next != state) {
            if (next == PlayerState.AIRBORNE
                    && state != PlayerState.CLIMBING
                    && state != PlayerState.HIGH_CLIMB
                    && state != PlayerState.WALLCLIMB
                    && state != PlayerState.VAULTING
                    && state != PlayerState.WALLRUN) {
                onTakeoff(f);
            }
            state = next;
        }
        updateChain(f);

        if (player.horizontalCollision && coilTicks == 0 && !wallRunActive && !climbActive && !wallClimbActive && !vaultActive) {
            onWallHit(cfg);
        } else {
            switch (state) {
                case IDLE -> {
                    if (!f.forward() && f.onGround() && momentum > 0f) {
                        tickStoppingSlide(player, f);
                    } else {
                        momentum = Math.max(0f, momentum - DECEL);
                        if (momentum == 0f) accelTicks = 0;
                    }
                }
                case RUNNING -> tickRunning(f);
                case AIRBORNE -> holdAirSpeed(player, f.hasInput());
                case COILING -> holdAirSpeed(player, f.hasInput());
                case WALLRUN -> tickWallRun(f);
                case CLIMBING, HIGH_CLIMB -> tickClimb(f);
                case WALLCLIMB -> tickWallClimb(f);
                case VAULTING -> tickVault(f);
                case SIDESTEP -> tickSidestep(f);
                case SHIFTING -> {
                    momentum = 0f;
                    accelTicks = 0;
                }
                case SLIDING -> tickSliding(f);
            }
        }
        applyBoost(f);
        updateFov(f);
        applyAttributes(f);
        updateCamera(f);
    }

    private static void updateCamera(Frame f) {
        ParkSEdgeConfig cfg = f.cfg();

        prevRoll = roll;
        prevPitchOff = pitchOff;
        prevOffsetY = offsetY;

        if (!cfg.cameraEnabled) {
            roll = pitchOff = offsetY = 0f;
            lastYaw = f.yaw();
            return;
        }

        float dYaw = MathHelper.wrapDegrees(f.yaw() - lastYaw);
        float maxAngle = (float) cfg.cameraMaxAngle;
        float mouseRoll = MathHelper.clamp(dYaw * (float) cfg.cameraRollStrength, -maxAngle, maxAngle);
        float mousePitch = Math.min(Math.abs(dYaw) * (float) cfg.cameraPitchStrength, maxAngle);

        if (isWallRollAnimating) {
            wallRollTicks++;
            float progress = MathHelper.clamp((float) wallRollTicks / wallRollMaxTicks, 0f, 1f);
            float eased = easeInOutCubic(progress);
            roll = wallRollStart + (wallRollTarget - wallRollStart) * eased;

            if (wallRollTicks >= wallRollMaxTicks) {
                isWallRollAnimating = false;
                roll = wallRollTarget;
            }
        } else if (state == PlayerState.WALLRUN) {
            roll = -wallSide * (float) cfg.wallRunRollAngle;
        } else if (state == PlayerState.SIDESTEP) {
            float sideProg = 1.0f - MathHelper.clamp((float) sidestepTicks / Math.max(1, cfg.sidestepTicks), 0f, 1f);
            float sideSign = (sidestepDirX * f.dirZ() - sidestepDirZ * f.dirX() > 0) ? 1.0f : -1.0f;
            roll = sideSign * 6.5f * (1.0f - easeOutQuint(sideProg)) + mouseRoll;
        } else if (state == PlayerState.SLIDING) {
            roll = MathHelper.clamp(-dYaw * 0.6f, -8.0f, 8.0f);
        } else if (state == PlayerState.RUNNING && f.onGround() && f.disp() > 0.02) {
            stepTimer += (float) (f.disp() * 2.2 * cfg.runBobbingStrength);
            float runRoll = (float) Math.cos(stepTimer) * 1.2f * momentum;
            roll += (mouseRoll + runRoll - roll) * (float) cfg.cameraSmoothness;
        } else {
            stepTimer *= 0.85f;
            roll += (mouseRoll - roll) * (float) cfg.cameraSmoothness;
        }

        if (isRollCamAnimating) {
            rollCamTicks++;
            float progress = MathHelper.clamp((float) rollCamTicks / rollCamMaxTicks, 0f, 1f);

            float smoothRollPitch = (float) cfg.rollPitchAngle * (float) Math.sin(progress * Math.PI);
            pitchOff = smoothRollPitch + mousePitch;

            if (rollCamTicks >= rollCamMaxTicks) {
                isRollCamAnimating = false;
            }
        } else if (isJumpCamAnimating) {
            jumpCamTicks++;
            float t = MathHelper.clamp((float) jumpCamTicks / 6f, 0f, 1f);
            pitchOff = -3.5f * (1.0f - easeOutQuad(t)) + mousePitch;
            if (jumpCamTicks >= 6) isJumpCamAnimating = false;
        } else if (state == PlayerState.VAULTING) {
            float vProg = MathHelper.clamp((float) vaultTicks / 10f, 0f, 1f);
            pitchOff = -4.5f * 4.0f * vProg * (1.0f - vProg) + mousePitch;
        } else if (state == PlayerState.CLIMBING || state == PlayerState.HIGH_CLIMB) {
            float maxTicks = isHighClimb ? 20f : 12f;
            float progress = MathHelper.clamp((float) climbTicks / maxTicks, 0f, 1f);
            float peak = (progress < 0.4f) ? (progress / 0.4f) : (1.0f - (progress - 0.4f) / 0.6f);
            pitchOff = (float) cfg.cameraClimbPitch * easeInOutCubic(peak) + mousePitch;
        } else if (state == PlayerState.WALLCLIMB) {
            float wcPulse = (float) Math.sin(wallClimbTicks * 0.6f);
            pitchOff = -3.0f + easeInOutSine(Math.abs(wcPulse)) * 1.5f + mousePitch;
        } else if (state == PlayerState.SLIDING) {
            float actionPitch = (slideTicks < 5) ? -2.5f : 1.0f;
            pitchOff = actionPitch + mousePitch;
        } else if (state == PlayerState.RUNNING && f.onGround() && f.disp() > 0.02) {
            float runPitch = (float) Math.sin(stepTimer * 2.0f) * 0.8f * momentum;
            pitchOff += (mousePitch + runPitch - pitchOff) * (float) cfg.cameraSmoothness;
        } else {
            pitchOff += (mousePitch - pitchOff) * (float) cfg.cameraSmoothness;
        }

        if (isCoilCamAnimating) {
            coilCamTicks++;
            float t = MathHelper.clamp((float) coilCamTicks / 8f, 0f, 1f);
            float phase = (t < 0.5f) ? (t * 2f) : ((1f - t) * 2f);
            offsetY = 0.15f * easeOutBack(phase);
            if (coilCamTicks >= 8) isCoilCamAnimating = false;
        } else if (state == PlayerState.SLIDING) {
            float slideProgress = MathHelper.clamp((float) slideTicks / 8f, 0f, 1f);
            offsetY = -(float) cfg.slideCameraDrop * easeOutQuad(slideProgress);
        } else if (landingTicks > 0) {
            landingTicks--;
            float t = 1.0f - ((float) landingTicks / 12.0f);
            float spring = (float) (Math.sin(t * Math.PI) * Math.exp(-t * 2.0));
            offsetY = -spring * 0.09f * landingImpactForce * (float) cfg.landingImpactStrength;
        } else if (state == PlayerState.WALLCLIMB) {
            offsetY = easeInOutSine(Math.abs((float) Math.sin(wallClimbTicks * 0.6f))) * 0.04f;
        } else if (state == PlayerState.CLIMBING || state == PlayerState.HIGH_CLIMB) {
            float maxTicks = isHighClimb ? 20f : 12f;
            float progress = MathHelper.clamp((float) climbTicks / maxTicks, 0f, 1f);
            offsetY = (float) cfg.cameraClimbLift * easeInOutCubic(progress);
        } else if (state == PlayerState.VAULTING) {
            float vProg = MathHelper.clamp((float) vaultTicks / 10f, 0f, 1f);
            offsetY = (float) Math.sin(vProg * Math.PI) * 0.06f;
        } else if (state == PlayerState.RUNNING && f.onGround() && f.disp() > 0.02) {
            float runY = (float) -Math.abs(Math.sin(stepTimer * 2.0f)) * 0.03f * momentum;
            offsetY += (runY - offsetY) * 0.4f;
        } else {
            offsetY += (0f - offsetY) * 0.3f;
        }

        lastYaw = f.yaw();
    }

    private static PlayerState resolveState(Frame f) {
        if (sidestepTicks > 0) return PlayerState.SIDESTEP;
        if (climbActive) return isHighClimb ? PlayerState.HIGH_CLIMB : PlayerState.CLIMBING;
        if (wallClimbActive) return PlayerState.WALLCLIMB;
        if (vaultActive) return PlayerState.VAULTING;
        if (wallRunActive) return PlayerState.WALLRUN;
        if (coilTicks > 0) return PlayerState.COILING;
        if (rollTicks > 0) return PlayerState.ROLLING;
        if (!f.onGround()) return PlayerState.AIRBORNE;
        if (slideActive) return PlayerState.SLIDING;
        if (f.shift()) return PlayerState.SHIFTING;
        return f.forward() ? PlayerState.RUNNING : PlayerState.IDLE;
    }

    private static void tickRunning(Frame f) {
        ParkSEdgeConfig cfg = f.cfg();

        if (f.turn() > cfg.safeTurnDegrees && momentum > 0f) {
            momentum = Math.max(0f, momentum - (float) (f.turn() - cfg.safeTurnDegrees) * TURN_LOSS);
            accelTicks = (int) (momentum * cfg.accelSeconds * 20.0);
        }

        int totalAccelTicks = (int) (cfg.accelSeconds * 20.0);
        if (accelTicks < totalAccelTicks) {
            accelTicks++;
        }

        float t = (float) accelTicks / totalAccelTicks;
        momentum = t * t * (3.0f - 2.0f * t);
    }

    private static final double STOP_DISTANCE = 0.5;
    private static boolean stopping = false;
    private static int stopTick = 0, stopTotal = 1, stopStamp = -1;
    private static double stopV0 = 0, stopDirX = 0, stopDirZ = 1;
    private static float stopM0 = 0f;

    private static void tickStoppingSlide(ClientPlayerEntity player, Frame f) {
        if (stopStamp == player.age) return;
        stopStamp = player.age;

        if (!stopping) {
            stopping = true;
            stopTick = 0;
            stopM0 = momentum;
            stopV0 = f.disp() > 0.05 ? f.disp() : speedFor(momentum, f.cfg());
            stopDirX = f.dirX();
            stopDirZ = f.dirZ();
            stopTotal = Math.max(2, (int) Math.round(1.0 + 2.0 * STOP_DISTANCE / stopV0));
        }

        stopTick++;
        float t = Math.min(1f, (float) stopTick / stopTotal);
        float k = 1f - t * t * (3f - 2f * t);
        momentum = stopM0 * k;

        if (t < 1f) {
            double v = stopV0 * k;
            player.setVelocity(stopDirX * v, player.getVelocity().y, stopDirZ * v);
        } else {
            player.setVelocity(0, player.getVelocity().y, 0);
            momentum = 0f;
            accelTicks = 0;
            stopping = false;
        }
    }

    private static void tickSidestep(Frame f) {
        ClientPlayerEntity p = f.player();
        sidestepTicks--;

        ParkSEdgeConfig cfg = f.cfg();
        double maxCap = cfg.maxSpeedKmh / KMH;

        if (sidestepTicks > 0) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x + sidestepDirX * (sidestepSpeed * 0.25), v.y, v.z + sidestepDirZ * (sidestepSpeed * 0.25));

            double currentHoriz = Math.hypot(p.getVelocity().x, p.getVelocity().z);
            if (currentHoriz > maxCap) {
                double scale = maxCap / currentHoriz;
                p.setVelocity(p.getVelocity().x * scale, p.getVelocity().y, p.getVelocity().z * scale);
            }
        } else {
            sidestepCooldown = ticks(cfg.sidestepCooldownSeconds);
            boostTicks = ticks(cfg.sidestepBoostSeconds);

            float yaw = p.getYaw();
            double lookX = -Math.sin(Math.toRadians(yaw));
            double lookZ = Math.cos(Math.toRadians(yaw));

            double dot = lookX * sidestepDirX + lookZ * sidestepDirZ;

            if (f.forward()) {
                if (dot >= 0.65) {
                    momentum = 1.0f;
                } else if (dot >= 0.15) {
                    double accuracy = (dot - 0.15) / (0.65 - 0.15);
                    momentum = (float) (0.35 + accuracy * 0.65);
                } else {
                    momentum = 0.15f;
                }

                accelTicks = (int) (momentum * cfg.accelSeconds * 20.0);
            } else {
                p.setVelocity(0.0, p.getVelocity().y, 0.0);
                momentum = 0.0f;
                accelTicks = 0;
                airSpeed = 0.0;
            }
        }
    }

    private static void tickSliding(Frame f) {
        ParkSEdgeConfig cfg = f.cfg();
        ClientPlayerEntity p = f.player();
        slideTicks++;

        p.setPose(EntityPose.SWIMMING);

        double floor = cfg.slideEndKmh / KMH;
        double t = Math.min(1.0, slideTicks / (double) ticks(cfg.slideDecaySeconds));
        double ease = 1.0 - (1.0 - t) * (1.0 - t);
        double speed = floor + Math.max(0.0, slideStartSpeed - floor) * (1.0 - ease);
        momentum = Math.min(momentum, momentumFor(speed, cfg));

        boolean canStand = p.getWorld().isSpaceEmpty(p, p.getBoundingBox().stretch(0, 1.2, 0));
        boolean over = (!f.shift() || t >= 1.0) && canStand;

        double k = over ? GROUND_FRICTION : 1.0;
        Vec3d v = p.getVelocity();
        p.setVelocity(slideDirX * speed * k, v.y, slideDirZ * speed * k);

        if (over) {
            slideActive = false;
            NetworkManager.sendToServer(new MovementActionPayload(MovementAction.SLIDE, false));
        }
    }

    private static void startSlide(Frame f) {
        slideActive = true;
        slideOwner = f.player().getUuid();
        slideTicks = 0;
        slideStartSpeed = f.disp();
        slideDirX = f.dirX();
        slideDirZ = f.dirZ();

        NetworkManager.sendToServer(new MovementActionPayload(MovementAction.SLIDE, true));
    }

    private static void holdAirSpeed(ClientPlayerEntity player, boolean hasInput) {
        Vec3d v = player.getVelocity();
        double hSpeed = Math.hypot(v.x, v.z);
        if (hSpeed < 1.0E-4) return;

        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        double normalCap = cfg.maxSpeedKmh / KMH;

        if (airBoostTicks > 0) {
            airBoostTicks--;
            double totalDurationTicks = ticks(Math.max(1.0, cfg.wallBoostDecaySeconds));
            double decayProgress = 1.0 - ((double) airBoostTicks / totalDurationTicks);
            double easeDecay = decayProgress * decayProgress * (3.0 - 2.0 * decayProgress);
            airSpeed = MathHelper.lerp(easeDecay, airBoostStartSpeed, normalCap);
        } else if (airSpeed > normalCap) {
            airSpeed = Math.max(normalCap, airSpeed - (cfg.airDecayPerSecond / 20.0));
        }

        double target = Math.max(hSpeed, airSpeed);
        double s = target / hSpeed;
        player.setVelocity(v.x * s, v.y, v.z * s);
    }

    private static void onTakeoff(Frame f) {
        ParkSEdgeConfig cfg = f.cfg();
        airSpeed = f.disp();
        if (f.player().getVelocity().y <= 0) return;

        if (inChain && groundTicks <= CHAIN_TICKS) {
            double hopCap = cfg.hopCapKmh / KMH;
            float hopCapM = momentumFor(hopCap, cfg);
            if (momentum < hopCapM) {
                momentum = Math.min(hopCapM, momentum + HOP_GAIN);
                accelTicks = (int) (momentum * cfg.accelSeconds * 20.0);
                airSpeed = Math.max(airSpeed, speedFor(momentum, cfg));
            }
            double t = Math.min(1.0, chainTicks / (double) ticks(cfg.hopDecaySeconds));
            double ease = 1.0 - (1.0 - t) * (1.0 - t);
            double cap = hopCap + Math.max(0.0, chainStartSpeed - hopCap) * (1.0 - ease);
            airSpeed = Math.min(airSpeed, cap);
            momentum = Math.min(momentum, momentumFor(cap, cfg));
            accelTicks = (int) (momentum * cfg.accelSeconds * 20.0);
        } else {
            inChain = true;
            chainTicks = 0;
            chainStartSpeed = airSpeed;
        }
    }

    private static int detectWall(Frame f) {
        var p = f.player();
        var w = p.getWorld();
        net.minecraft.util.math.Box b = p.getBoundingBox();
        double reach = f.cfg().wallRunReach;
        for (int side = 1; side >= -1; side -= 2) {
            double sx = side * f.dirZ();
            double sz = side * -f.dirX();
            int ax = 0, az = 0;
            if (Math.abs(sx) >= Math.abs(sz)) ax = (int) Math.signum(sx);
            else az = (int) Math.signum(sz);
            if (ax == 0 && az == 0) continue;
            if (!w.isSpaceEmpty(p, b.offset(ax * reach, 0, az * reach))
                    && !w.isSpaceEmpty(p, b.offset(ax * reach, 1.0, az * reach))) {
                if (oppositeWallNear(f, ax, az)) continue;
                wallDirX = ax;
                wallDirZ = az;
                return side;
            }
        }
        return 0;
    }

    private static boolean oppositeWallNear(Frame f, int ax, int az) {
        double gap = f.cfg().wallRunOppositeGap;
        if (gap <= 0.0) return false;
        var p = f.player();
        var w = p.getWorld();
        net.minecraft.util.math.Box b = p.getBoundingBox();
        return !w.isSpaceEmpty(p, b.offset(-ax * gap, 0, -az * gap))
                || !w.isSpaceEmpty(p, b.offset(-ax * gap, 1.0, -az * gap));
    }

    private static boolean wallContact(Frame f) {
        var p = f.player();
        double r = f.cfg().wallRunReach;
        return !p.getWorld().isSpaceEmpty(p, p.getBoundingBox().offset(wallDirX * r, 0, wallDirZ * r));
    }

    private static double wallRunVy(ParkSEdgeConfig cfg, int t) {
        double u = Math.min(1.0, t / (double) Math.max(1, ticks(cfg.wallRunSeconds)));
        double p = cfg.wallRunPeak;
        if (u < p) return MathHelper.lerp(u / p, cfg.wallRunUpVy, 0.0);
        return MathHelper.lerp((u - p) / (1.0 - p), 0.0, cfg.wallRunEndVy);
    }

    private static void updateWallRun(Frame f) {
        ParkSEdgeConfig cfg = f.cfg();
        if (f.onGround()) {
            wallRunCooldown = 0;
            hasLastWall = false;
        } else if (wallRunCooldown > 0) {
            wallRunCooldown--;
        }

        if (wallRunActive) {
            boolean ok = cfg.wallRunEnabled && !f.onGround() && !f.player().isTouchingWater() && f.forward()
                    && wallContact(f)
                    && !oppositeWallNear(f, (int) wallDirX, (int) wallDirZ)
                    && Math.max(f.disp(), airSpeed) * KMH >= cfg.wallRunMinKmh * 0.7
                    && wallRunTicks < ticks(cfg.wallRunSeconds);
            if (!ok) {
                wallRunActive = false;
                wallRunCooldown = wallRunTicks >= ticks(cfg.wallRunSeconds) ? 100000 : 6;

                if (wallRollTarget != 0f) {
                    wallRollStart = roll;
                    wallRollTarget = 0f;
                    wallRollTicks = 0;
                    wallRollMaxTicks = Math.max(1, ticks(cfg.wallJumpRollDuration));
                    isWallRollAnimating = true;
                }
            }
            return;
        }

        double currentSpeedKmh = Math.max(f.disp(), airSpeed) * KMH;

        if (cfg.wallRunEnabled && !f.onGround() && !f.player().isTouchingWater() && wallRunCooldown == 0 && f.forward()
                && sidestepTicks == 0 && !slideActive && !wallClimbActive && !climbActive && !vaultActive
                && currentSpeedKmh >= cfg.wallRunMinKmh) {
            int side = detectWall(f);
            if (side != 0) {
                int currentAxis = (wallDirX != 0) ? 0 : 1;
                int currentDir = (currentAxis == 0) ? (int) wallDirX : (int) wallDirZ;
                double reach = cfg.wallRunReach;
                int currentCoord = (currentAxis == 0)
                        ? MathHelper.floor(f.player().getX() + wallDirX * reach)
                        : MathHelper.floor(f.player().getZ() + wallDirZ * reach);

                boolean isSameWall = hasLastWall && lastWallAxis == currentAxis && lastWallDir == currentDir
                        && Math.abs(currentCoord - lastWallCoord) <= 1;

                if (isSameWall && !cfg.wallRunChaining) {
                    return;
                }

                wallRunActive = true;
                wallRunTicks = 0;
                wallSide = side;

                hasLastWall = true;
                lastWallAxis = currentAxis;
                lastWallDir = currentDir;
                lastWallCoord = currentCoord;

                double baseTargetKmh = Math.max(f.disp() * KMH, cfg.maxSpeedKmh);
                wallRunInitialSpeed = (baseTargetKmh + cfg.wallRunInitialBoostKmh) / KMH;
                airSpeed = wallRunInitialSpeed;

                momentum = 1.0f;
                accelTicks = (int) (cfg.accelSeconds * 20.0);

                wallRollStart = roll;
                wallRollTarget = -wallSide * (float) cfg.wallRunRollAngle;
                wallRollTicks = 0;
                wallRollMaxTicks = Math.max(1, ticks(cfg.wallJumpRollDuration));
                isWallRollAnimating = true;

                if (jumpBufferTicks > 0) {
                    jumpBufferTicks = 0;
                    wallJump(f);
                }
            }
        }
    }

    private static void tickWallRun(Frame f) {
        ParkSEdgeConfig cfg = f.cfg();
        if (jumpPressed) {
            wallJump(f);
            return;
        }
        wallRunTicks++;

        double targetSpeed = cfg.maxSpeedKmh / KMH;
        double decayProgress = Math.min(1.0, (double) wallRunTicks / ticks(cfg.wallBoostDecaySeconds));
        double easeDecay = decayProgress * decayProgress * (3.0 - 2.0 * decayProgress);
        airSpeed = MathHelper.lerp(easeDecay, wallRunInitialSpeed, targetSpeed);

        double ux = f.dirX() * (1 - Math.abs(wallDirX));
        double uz = f.dirZ() * (1 - Math.abs(wallDirZ));
        double len = Math.hypot(ux, uz);
        if (len < 0.2) { ux = f.dirX(); uz = f.dirZ(); len = 1.0; }
        ux /= len;
        uz /= len;
        f.player().setVelocity(ux * airSpeed + wallDirX * cfg.wallRunStick,
                wallRunVy(cfg, wallRunTicks),
                uz * airSpeed + wallDirZ * cfg.wallRunStick);
    }

    private static void tickVault(Frame f) {
        var p = f.player();
        Vec3d v = p.getVelocity();
        p.setVelocity(vaultDirX * vaultSpeed, v.y, vaultDirZ * vaultSpeed);
    }

    private static void tickWallClimb(Frame f) {
        ParkSEdgeConfig cfg = f.cfg();
        var p = f.player();
        wallClimbTicks++;

        if (jumpPressed && wallClimbTicks > 1) {
            double rad = Math.toRadians(p.getYaw());
            double lookX = -Math.sin(rad);
            double lookZ = Math.cos(rad);
            boolean turned = wallClimbTurnTicks > 0;

            if (cfg.wallClimbQuickTurnEnabled && f.back()) {
                p.setVelocity(lookX * cfg.wallClimbKickPush * 1.3, cfg.wallClimbKickUp * 1.2, lookZ * cfg.wallClimbKickPush * 1.3);
                p.setYaw(p.getYaw() + 180.0f);

                wallClimbUsed = false;
                hasLastWall = false;
            } else {
                p.setVelocity(lookX * cfg.wallClimbKickPush, cfg.wallClimbKickUp, lookZ * cfg.wallClimbKickPush);
                if (turned) {
                    wallClimbUsed = false;
                    hasLastWall = false;
                }
            }

            airSpeed = cfg.wallClimbKickPush;
            wallClimbActive = false;
            wallClimbTurnTicks = 0;
            return;
        }

        if (wallClimbTurnTicks > 0) {
            wallClimbTurnTicks--;
            p.setVelocity(0, 0, 0);
            p.fallDistance = 0;
            NetworkManager.sendToServer(new MovementActionPayload(MovementAction.RESET_FALL, true));

            if (wallClimbTurnTicks == 0) {
                wallClimbActive = false;
            }
            return;
        }

        double r = cfg.climbReach;
        var w = p.getWorld();
        net.minecraft.util.math.Box b = p.getBoundingBox();

        boolean wallInFront = !w.isSpaceEmpty(p, b.offset(wcDirX * r, 0.2, wcDirZ * r));

        if (!wallInFront) {
            if (startLedgeClimb(p, cfg, 1.8, cfg.climbMaxHeight, wcDirX, wcDirZ)) {
                tickClimb(f);
            } else {
                wallClimbActive = false;
            }
            return;
        }

        double risen = p.getY() - wallClimbStartY;
        double limit = cfg.wallClimbMaxHeight;
        boolean stuck = wallClimbTicks > 3 && p.getY() - wallClimbLastY < 0.02;
        wallClimbLastY = p.getY();

        if (risen >= limit || wallClimbTicks > 60 || stuck) {
            wallClimbActive = false;
            return;
        }

        double vy = cfg.wallClimbSpeed * (1.0 - 0.5 * Math.min(1.0, risen / limit));
        vy = Math.min(vy, limit - risen + 0.05);
        p.setVelocity(wcDirX * 0.04, vy, wcDirZ * 0.04);
    }

    private static void tickClimb(Frame f) {
        ParkSEdgeConfig cfg = f.cfg();
        var p = f.player();
        climbTicks++;
        double remaining = climbTop - p.getY();
        if (remaining > 0.03 && climbTicks < 60) {
            double currentClimbSpeed = isHighClimb ? cfg.highClimbSpeed : cfg.climbSpeed;
            double vy = Math.min(currentClimbSpeed, remaining + 0.02);
            p.setVelocity(climbDirX * 0.04, vy, climbDirZ * 0.04);
        } else {
            p.setVelocity(climbDirX * climbExitSpeed, 0.0, climbDirZ * climbExitSpeed);
            airSpeed = climbExitSpeed;
            climbActive = false;
            isHighClimb = false;
        }
    }

    private static void wallJump(Frame f) {
        ParkSEdgeConfig cfg = f.cfg();
        ClientPlayerEntity p = f.player();

        double rad = Math.toRadians(p.getYaw());
        double lookX = -Math.sin(rad);
        double lookZ = Math.cos(rad);

        int windowTicks = ticks(cfg.wallBoostWindowSeconds);
        double finalSpeed = airSpeed;

        if (cfg.wallBoostEnabled && wallRunTicks <= windowTicks) {
            finalSpeed = wallRunInitialSpeed;
            momentum = 1.0f;
            accelTicks = (int) (cfg.accelSeconds * 20.0);

            double boostDuration = Math.max(1.0, cfg.wallBoostDecaySeconds);
            airBoostTicks = ticks(boostDuration);
            airBoostStartSpeed = finalSpeed;
        }

        double vx = lookX * finalSpeed * cfg.wallJumpKeep;
        double vz = lookZ * finalSpeed * cfg.wallJumpKeep;

        double h = Math.hypot(vx, vz);
        double cap = (cfg.maxSpeedKmh + cfg.wallRunInitialBoostKmh) / KMH;
        if (h > cap) {
            vx *= cap / h;
            vz *= cap / h;
            h = cap;
        }

        p.setVelocity(vx, cfg.wallJumpUp, vz);
        airSpeed = h;
        wallRunActive = false;
        wallRunCooldown = 6;
        jumpBufferTicks = 0;

        wallRollStart = roll;
        wallRollTarget = 0f;
        wallRollTicks = 0;
        wallRollMaxTicks = Math.max(1, ticks(cfg.wallJumpRollDuration));
        isWallRollAnimating = true;
    }

    private static void onWallHit(ParkSEdgeConfig cfg) {
        if (cfg.ticTacEnabled && !wasAirborne && jumpPressed) {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            if (p != null) {
                Vec3d vel = p.getVelocity();
                p.setVelocity(-vel.x * 0.8, cfg.wallClimbKickUp * 0.9, -vel.z * 0.8);
                airSpeed = Math.max(airSpeed, 0.3);
                return;
            }
        }

        momentum *= WALL_LOSS;
        accelTicks = (int) (momentum * cfg.accelSeconds * 20.0);
        airSpeed *= WALL_LOSS;
        airBoostTicks = 0;

        if (slideActive) {
            slideActive = false;
            NetworkManager.sendToServer(new MovementActionPayload(MovementAction.SLIDE, false));
        }

        boostTicks = 0;
        if (sidestepTicks > 0) {
            sidestepTicks = 0;
            sidestepCooldown = ticks(cfg.sidestepCooldownSeconds);
        }
    }

    private static void updateChain(Frame f) {
        groundTicks = f.onGround() ? groundTicks + 1 : 0;
        if (f.onGround() && groundTicks > CHAIN_TICKS) inChain = false;
        if (inChain) chainTicks++;
    }

    private static void applyBoost(Frame f) {
        if (boostTicks <= 0) return;
        boostTicks--;
        ClientPlayerEntity p = f.player();
        float fwd = p.input.movementForward;
        if (fwd <= 0 || !f.onGround()) return;

        float side = p.input.movementSideways;
        double rad = Math.toRadians(f.yaw());
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);
        double ix = fx * fwd + fz * side;
        double iz = fz * fwd - fx * side;
        double len = Math.sqrt(ix * ix + iz * iz);
        if (len < 1.0E-6) return;

        double cos = (ix * sidestepDirX + iz * sidestepDirZ) / len;
        if (cos < Math.cos(Math.toRadians(f.cfg().sidestepBoostAngle))) return;

        momentum = Math.min(1f, momentum + 0.3f);
        accelTicks = (int) (momentum * f.cfg().accelSeconds * 20.0);
        double kick = speedFor(momentum, f.cfg()) * GROUND_FRICTION;
        Vec3d v = p.getVelocity();
        p.setVelocity(ix / len * kick, v.y, iz / len * kick);
        boostTicks = 0;
    }

    private static Frame measure(ClientPlayerEntity p, ParkSEdgeConfig cfg) {
        float yaw = p.getYaw();
        double dx = p.getX() - lastX;
        double dz = p.getZ() - lastZ;
        double disp = Math.sqrt(dx * dx + dz * dz);
        if (disp > 1.0) disp = 0.0;
        lastX = p.getX();
        lastZ = p.getZ();
        currentKmh = disp * KMH;

        double dirX;
        double dirZ;
        if (disp > 0.05) {
            dirX = dx / disp;
            dirZ = dz / disp;
        } else {
            double rad = Math.toRadians(yaw);
            dirX = -Math.sin(rad);
            dirZ = Math.cos(rad);
        }

        float turn = Math.abs(MathHelper.wrapDegrees(yaw - lastYaw));

        float fwd = p.input.movementForward;
        float side = p.input.movementSideways;
        boolean shift = MinecraftClient.getInstance().options.sneakKey.isPressed();
        boolean left = p.input.movementSideways > 0;
        boolean right = p.input.movementSideways < 0;
        boolean back = p.input.movementForward < 0;

        return new Frame(p, cfg, p.isOnGround(), fwd > 0, fwd != 0 || side != 0, shift,
                left, right, back, disp, dirX, dirZ, yaw, turn);
    }

    private static void updateFov(Frame f) {
        ParkSEdgeConfig cfg = f.cfg();

        double currentSpeedKmh = f.disp() * KMH;
        float speedRatio = (float) MathHelper.clamp(currentSpeedKmh / cfg.maxSpeedKmh, 0.0, 1.5);

        float target = (f.hasInput() || !f.onGround()) ? Math.max(momentum, speedRatio) : 0f;

        if (state == PlayerState.WALLRUN) {
            target += (float) cfg.wallRunFovKick;
        } else if (state == PlayerState.SLIDING) {
            target += (float) cfg.slideFovKick;
        } else if (state == PlayerState.SIDESTEP) {
            float sideProg = 1.0f - MathHelper.clamp((float) sidestepTicks / Math.max(1, cfg.sidestepTicks), 0f, 1f);
            target += (float) cfg.sidestepFovKick * (1.0f - easeOutQuint(sideProg));
        } else if (isCoilCamAnimating) {
            target += (float) cfg.coilFovKick;
        }

        if (landingTicks > 0) {
            float landingFactor = (float) landingTicks / 12.0f;
            target -= (float) cfg.landingFovCompress * landingImpactForce * landingFactor;
        }

        target = MathHelper.clamp(target, 0f, 1.6f);

        float attackSpeed = (float) cfg.fovAttackSpeed;
        float releaseSpeed = (float) (1.0 / Math.max(1.0, cfg.fovReturnSeconds * 20.0));
        float lerpFactor = (target > fovMomentum) ? attackSpeed : Math.min(0.15f, releaseSpeed);

        fovMomentum += (target - fovMomentum) * lerpFactor;
    }

    private static void applyAttributes(Frame f) {
        ClientPlayerEntity p = f.player();
        ParkSEdgeConfig cfg = f.cfg();

        double shiftInputScale = 1.0;
        if (state == PlayerState.SHIFTING) {
            double len = Math.hypot(p.input.movementForward, p.input.movementSideways);
            if (len > 0.05) shiftInputScale = Math.min(1.0, len);
        }
        appliedBonus = (state == PlayerState.SHIFTING)
                ? cfg.shiftSpeedKmh / KMH / BASE_SPEED / shiftInputScale - 1.0
                : curve(momentum) * maxBonus(cfg);

        setModifier(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, MOMENTUM_ID,
                appliedBonus, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(p, EntityAttributes.GENERIC_GRAVITY, GRAVITY_ID,
                cfg.gravity / 400.0 - VANILLA_GRAVITY, EntityAttributeModifier.Operation.ADD_VALUE);
        setModifier(p, EntityAttributes.GENERIC_JUMP_STRENGTH, JUMP_ID,
                cfg.jumpMultiplier - 1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void clearAttributes(ClientPlayerEntity p) {
        removeModifier(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, MOMENTUM_ID);
        removeModifier(p, EntityAttributes.GENERIC_GRAVITY, GRAVITY_ID);
        removeModifier(p, EntityAttributes.GENERIC_JUMP_STRENGTH, JUMP_ID);
    }

    private static void setModifier(ClientPlayerEntity p, RegistryEntry<EntityAttribute> attribute,
                                    Identifier id, double amount, EntityAttributeModifier.Operation op) {
        EntityAttributeInstance inst = p.getAttributeInstance(attribute);
        if (inst == null) return;

        EntityAttributeModifier old = inst.getModifier(id);
        if (Math.abs(amount) < 1.0E-9) {
            if (old != null) inst.removeModifier(id);
            return;
        }
        if (old != null && Math.abs(old.value() - amount) < 1.0E-9) return;

        inst.removeModifier(id);
        inst.addTemporaryModifier(new EntityAttributeModifier(id, amount, op));
    }

    private static void removeModifier(ClientPlayerEntity p, RegistryEntry<EntityAttribute> attribute, Identifier id) {
        EntityAttributeInstance inst = p.getAttributeInstance(attribute);
        if (inst != null && inst.getModifier(id) != null) {
            inst.removeModifier(id);
        }
    }

    private static double maxBonus(ParkSEdgeConfig cfg) {
        return cfg.maxSpeedKmh / KMH / BASE_SPEED - 1.0;
    }

    private static float curve(float m) {
        return m;
    }

    private static double speedFor(float m, ParkSEdgeConfig cfg) {
        return BASE_SPEED * (1.0 + curve(m) * maxBonus(cfg));
    }

    private static float momentumFor(double speed, ParkSEdgeConfig cfg) {
        double c = MathHelper.clamp((speed / BASE_SPEED - 1.0) / maxBonus(cfg), 0.0, 1.0);
        return (float) c;
    }

    private static int ticks(double seconds) {
        return (int) Math.round(seconds * 20.0);
    }
}
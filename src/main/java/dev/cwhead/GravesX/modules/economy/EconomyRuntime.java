package dev.cwhead.GravesX.modules.economy;

/**
 * Runtime wrapper for ChargeConfig.
 */
public final class EconomyRuntime {
    private ChargeConfig cfg;

    public EconomyRuntime(ChargeConfig cfg) { this.cfg = cfg; }

    public ChargeConfig get() { return cfg; }

    public void set(ChargeConfig cfg) {
        this.cfg = cfg;
    }
}
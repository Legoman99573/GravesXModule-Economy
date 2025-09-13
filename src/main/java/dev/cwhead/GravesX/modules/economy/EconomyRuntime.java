package dev.cwhead.GravesX.modules.economy;

/**
 * Runtime accessor for the active {@link ChargeConfig}.
 * <p>
 * Designed for hot-reload: listeners and commands read the latest values
 * via {@link #get()} without re-registering. Visibility across threads is
 * ensured by a {@code volatile} reference.
 * </p>
 */
public final class EconomyRuntime {

    /**
     * Current configuration; {@code volatile} guarantees cross-thread visibility.
     */
    private volatile ChargeConfig config;

    /**
     * Creates a runtime holder with an initial configuration.
     *
     * @param initial initial {@link ChargeConfig}
     */
    public EconomyRuntime(ChargeConfig initial) {
        this.config = initial;
    }

    /**
     * Returns the current {@link ChargeConfig}.
     *
     * @return live configuration reference
     */
    public ChargeConfig get() {
        return config;
    }

    /**
     * Replaces the active {@link ChargeConfig}.
     *
     * @param next new configuration to expose
     */
    public void set(ChargeConfig next) {
        this.config = next;
    }
}
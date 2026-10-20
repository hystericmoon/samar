package app.samar.effects

/** The five effects from DESIGN_BRIEF.md. [available] flips on as each one is built. */
enum class Effect(val label: String, val available: Boolean) {
    Gaussian("Gaussian", available = true),
    Glass("Glass", available = true),
    Smear("Smear", available = true),
    Pixelate("Pixelate", available = true),
    Solid("Solid", available = true);

    /** Solid is always a full cover, so the intensity slider does not apply to it. */
    val usesIntensity get() = this != Solid
}

package dev.brahmkshatriya.echo.ui.components

import androidx.compose.ui.graphics.Color
import com.mikepenz.hypnoticcanvas.RuntimeEffect
import com.mikepenz.hypnoticcanvas.shaders.Shader

class WavyGrainyShader(
    private val colorA: Color,
    private val colorB: Color,
    private val colorC: Color,
    private val grainIntensity: Float = 0.1f,
) : Shader {

    override val name = ""
    override val authorName = ""
    override val authorUrl = ""
    override val credit = ""
    override val license = ""
    override val licenseUrl = ""

    private var runtimeEffect: RuntimeEffect? = null

    override fun applyUniforms(
        runtimeEffect: RuntimeEffect,
        time: Float,
        width: Float,
        height: Float,
    ) {
        this.runtimeEffect = runtimeEffect
        runtimeEffect.setFloatUniform("uTime", time)
        runtimeEffect.setFloatUniform("uResolution", width, height, 0f)
        applyColor(runtimeEffect, "uColorA", colorA)
        applyColor(runtimeEffect, "uColorB", colorB)
        applyColor(runtimeEffect, "uColorC", colorC)
        runtimeEffect.setFloatUniform("uGrainIntensity", grainIntensity)
    }

    private fun applyColor(
        runtimeEffect: RuntimeEffect, name: String, color: Color,
    ) = runtimeEffect.setFloatUniform(name, color.red, color.green, color.blue)

    fun changeColor(
        colorA: Color = this.colorA,
        colorB: Color = this.colorB,
        colorC: Color = this.colorC,
        grainIntensity: Float = this.grainIntensity,
    ) {
        val runtimeEffect = this.runtimeEffect ?: return
        applyColor(runtimeEffect, "uColorA", colorA)
        applyColor(runtimeEffect, "uColorB", colorB)
        applyColor(runtimeEffect, "uColorC", colorC)
        runtimeEffect.setFloatUniform("uGrainIntensity", grainIntensity)
    }

    override val sksl = """
uniform float uTime;
uniform vec3 uResolution;

uniform vec3 uColorA;
uniform vec3 uColorB;
uniform vec3 uColorC;
uniform float uGrainIntensity;

// ===== rotation =====
mat2 Rot(float a) {
    float s = sin(a);
    float c = cos(a);
    return mat2(c, -s, s, c);
}

// ===== hash =====
vec2 hash(vec2 p) {
    p = vec2(
        dot(p, vec2(2127.1, 81.17)),
        dot(p, vec2(1269.5, 283.37))
    );
    return fract(sin(p) * 43758.5453);
}

// ===== noise =====
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);

    float n = mix(
        mix(
            dot(-1.0 + 2.0 * hash(i + vec2(0.0, 0.0)), f),
            dot(-1.0 + 2.0 * hash(i + vec2(1.0, 0.0)), f - vec2(1.0, 0.0)),
            u.x
        ),
        mix(
            dot(-1.0 + 2.0 * hash(i + vec2(0.0, 1.0)), f - vec2(0.0, 1.0)),
            dot(-1.0 + 2.0 * hash(i + vec2(1.0, 1.0)), f - vec2(1.0, 1.0)),
            u.x
        ),
        u.y
    );

    return 0.5 + 0.5 * n;
}

// ===== film grain =====
float filmGrainNoise(vec2 uv) {
    return length(hash(uv));
}

// ===== main =====
vec4 main(vec2 fragCoord) {
    vec2 uv = fragCoord / uResolution.xy;
    float aspect = uResolution.x / uResolution.y;

    vec2 tuv = uv - 0.5;

    float degree = noise(vec2(uTime * 0.05, tuv.x * tuv.y));

    tuv.y /= aspect;
    tuv *= Rot(radians((degree - 0.5) * 720.0 + 180.0));
    tuv.y *= aspect;

    float frequency = 5.0;
    float amplitude = 30.0;
    float speed = uTime * 2.0;

    tuv.x += sin(tuv.y * frequency + speed) / amplitude;
    tuv.y += sin(tuv.x * frequency * 1.5 + speed) / (amplitude * 0.5);

    // ===== tri-color blend =====
    float gx = smoothstep(-0.3, 0.3, tuv.x);
    float gy = smoothstep(-0.4, 0.4, tuv.y);

    vec3 layer1 = mix(uColorA, uColorB, gx);
    vec3 color = mix(layer1, uColorC, gy);

    // ===== film grain =====
    color -= filmGrainNoise(uv) * uGrainIntensity;

    return vec4(color, 1.0);
}
"""
}

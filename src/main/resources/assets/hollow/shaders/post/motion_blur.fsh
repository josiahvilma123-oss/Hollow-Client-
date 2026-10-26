#version 330

// Exponential frame accumulation: blends the freshly rendered world with the previous
// accumulated frame. Runs before the HUD is drawn, so text and crosshair stay sharp.

uniform sampler2D InSampler;
uniform sampler2D PrevSampler;

layout(std140) uniform MotionBlurConfig {
    float BlendFactor;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 current = texture(InSampler, texCoord).rgb;
    vec3 previous = texture(PrevSampler, texCoord).rgb;
    fragColor = vec4(mix(current, previous, BlendFactor), 1.0);
}

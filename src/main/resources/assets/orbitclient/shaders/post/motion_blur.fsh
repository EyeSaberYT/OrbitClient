#version 330

uniform sampler2D InSampler;
uniform sampler2D PrevSampler;

layout(std140) uniform MotionBlurConfig {
    float Keep;
};

in vec2 texCoord;

out vec4 fragColor;

// Blends the new frame with the accumulated previous ones. The history starts transparent, so
// turning blur on never fades in from black.
void main() {
    vec4 current = texture(InSampler, texCoord);
    vec4 previous = texture(PrevSampler, texCoord);
    fragColor = vec4(mix(current.rgb, previous.rgb, Keep * previous.a), 1.0);
}
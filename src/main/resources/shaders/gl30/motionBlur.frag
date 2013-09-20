// $texture_layout: colors = 0
// $texture_layout: depths = 1

#version 330

in vec2 textureUV;
noperspective in vec3 viewRay;

layout(location = 0) out vec4 outputColor;

uniform sampler2D colors;
uniform sampler2D depths;
uniform vec2 projection;
uniform mat4 inverseViewMatrix;
uniform mat4 previousViewMatrix;
uniform mat4 previousProjectionMatrix;
uniform int sampleCount;
uniform float blurStrength;

float linearizeDepth(float depth) {
    return projection.y / (depth - projection.x);
}

void main() {
    vec4 color = texture(colors, textureUV);
    vec3 position = viewRay * linearizeDepth(texture(depths, textureUV).r);

    vec4 previous = previousProjectionMatrix * previousViewMatrix * inverseViewMatrix * vec4(position, 1);
    previous.xyz /= previous.w;
    previous.xy = previous.xy * 0.5 + 0.5;
    vec2 blurVector = (previous.xy - textureUV) * blurStrength;

    for (int i = 1; i < sampleCount; i++) {
        color += texture(colors, textureUV + blurVector * (float(i) / (sampleCount - 1) - 0.5));
    }

    outputColor = color / sampleCount;
}

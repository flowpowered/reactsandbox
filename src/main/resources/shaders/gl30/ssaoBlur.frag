#version 330

in vec2 textureUV;

layout(location = 0) out float outputOcclusion;

uniform sampler2D occlusion;
uniform int noiseSize;
uniform vec2 texelSize;

void main() {
    float blurred = 0;
    int halfNoiseSize = noiseSize / 2;
    for (int x = -halfNoiseSize; x < halfNoiseSize; x++) {
        for (int y = -halfNoiseSize; y < halfNoiseSize; y++) {
            blurred += texture(occlusion, textureUV + vec2(x * texelSize.x, y * texelSize.y)).r;
        }
    }
    outputOcclusion = blurred / (noiseSize * noiseSize);
}

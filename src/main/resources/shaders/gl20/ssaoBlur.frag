// $texture_layout: occlusions = 0

#version 120

varying vec2 textureUV;

uniform sampler2D occlusion;
uniform int noiseSize;
uniform vec2 texelSize;

void main() {
    float blurred = 0;
    int halfNoiseSize = noiseSize / 2;
    for (int x = -halfNoiseSize; x < halfNoiseSize; x++) {
        for (int y = -halfNoiseSize; y < halfNoiseSize; y++) {
            blurred += texture2D(occlusion, textureUV + vec2(x * texelSize.x, y * texelSize.y)).r;
        }
    }
    float occlusion = blurred / (noiseSize * noiseSize);

    gl_FragColor = vec4(occlusion, occlusion, occlusion, 1);
}

#version 120

const int MAX_KERNEL_SIZE = 32;

varying vec2 textureUV;
varying vec3 viewRay;
varying vec3 lightPositionView;

uniform sampler2D normals;
uniform sampler2D depths;
uniform sampler2DShadow lightDepths;
uniform mat4 inverseViewMatrix;
uniform mat4 lightViewMatrix;
uniform mat4 lightProjectionMatrix;
uniform vec2 projection;
uniform int kernelSize;
uniform vec2[MAX_KERNEL_SIZE] kernel;
uniform float bias;
uniform float radius;

float linearizeDepth(float depth) {
    return projection.y / (depth - projection.x);
}

vec2 rand(vec2 seed) {
    return vec2(fract(sin(dot(seed, vec2(12.9898, 78.233))) * 43758.5453), fract(cos(dot(seed, vec2(17.0391, 41.241))) * 29151.3212));
}

void main() {
    vec4 rawNormalView = texture2D(normals, textureUV);
    if (rawNormalView.a <= 0) {
        return;
    }
    vec3 normalView = normalize(rawNormalView.xyz * 2 - 1);

    vec3 positionView = viewRay * linearizeDepth(texture2D(depths, textureUV).r);

    vec3 lightDirection = normalize(lightPositionView - positionView);
    float normalDotLight = dot(normalView, lightDirection);

    vec4 positionLightClip = lightProjectionMatrix * lightViewMatrix * inverseViewMatrix * vec4(positionView, 1);
    positionLightClip.xyz = positionLightClip.xyz / positionLightClip.w * 0.5 + 0.5;

    float slopedBias = clamp(tan(acos(normalDotLight)) * bias, bias / 2, bias * 2);

    vec2 randomVector = normalize(rand(gl_FragCoord.xy) * 2 - 1);
    vec2 orthogonalVector = vec2(randomVector.y, -randomVector.x);
    mat2 basis = mat2(randomVector, orthogonalVector);

    float shadow;
    for (int i = 0; i < kernelSize; i++) {
        vec2 offsetPosition = positionLightClip.xy + basis * kernel[i] * radius;
        shadow += shadow2D(lightDepths, vec3(offsetPosition, positionLightClip.z - slopedBias)).r;
    }

    shadow /= kernelSize;

    gl_FragColor = vec4(shadow, shadow, shadow, 1);
}

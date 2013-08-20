#version 330

in vec2 textureUV;
noperspective in vec3 viewRay;
in vec3 lightPositionView;

layout(location = 0) out vec3 outputColor;

uniform sampler2D colors;
uniform sampler2D normals;
uniform sampler2D depths;
uniform sampler2D occlusion;
uniform mat4 projectionMatrix;
uniform float lightAttenuation;
uniform float diffuseIntensity;
uniform float specularIntensity;
uniform float ambientIntensity;

float linearizeDepth(in float depth) {
    return projectionMatrix[3][2] / (depth + projectionMatrix[2][2]);
}

void main() {
    outputColor = texture(colors, textureUV).rgb;

    vec4 rawNormalView = texture(normals, textureUV);
    if (rawNormalView.a <= 0) {
        return;
    }
    vec3 normalView = normalize(rawNormalView.xyz * 2 - 1);

    float depth = linearizeDepth(texture(depths, textureUV).r);
    float occlusion = texture(occlusion, textureUV).r;

    vec3 positionView = viewRay * depth;

    vec3 lightDifference = lightPositionView - positionView;
    float lightDistance = length(lightDifference);
    vec3 lightDirection = lightDifference / lightDistance;
    float distanceIntensity = 1 / (1 + lightAttenuation * lightDistance);
    float normalDotLight = max(0, dot(normalView, lightDirection));

    float diffuseTerm = diffuseIntensity * distanceIntensity * normalDotLight;

    float specularTerm;
    if (normalDotLight > 0) {
        specularTerm = specularIntensity * distanceIntensity * pow(max(0, dot(reflect(lightDirection, normalView), normalize(viewRay))), 20);
    } else {
        specularTerm = 0;
    }

    float ambientTerm = ambientIntensity;

    outputColor *= (diffuseTerm + specularTerm + ambientTerm) * occlusion;
}

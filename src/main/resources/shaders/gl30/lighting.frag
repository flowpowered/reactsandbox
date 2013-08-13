#version 330

const vec3 ZERO = vec3(0, 0, 0);

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
    vec3 color = texture(colors, textureUV).rgb;
    vec3 normalView = texture(normals, textureUV).xyz;

    if (dot(normalView, normalView) <= 0.9) {
        outputColor = color;
        return;
    }

    float depth = linearizeDepth(texture(depths, textureUV).r);
    vec3 occlusion = texture(occlusion, textureUV).rgb;

    vec3 positionView = viewRay * depth;

    vec3 lightDifference = lightPositionView - positionView;
    float lightDistance = length(lightDifference);
    vec3 lightDirection = lightDifference / lightDistance;
    float distanceIntensity = 1 / (1 + lightAttenuation * lightDistance);
    float normalDotLight = max(0, dot(normalView, lightDirection));

    float diffuseTerm = diffuseIntensity * distanceIntensity * normalDotLight;

    float specularTerm;
    if (normalDotLight > 0) {
        specularTerm = specularIntensity * distanceIntensity * pow(max(0, dot(reflect(lightDirection, normalView), normalize(positionView))), 20);
    } else {
        specularTerm = 0;
    }

    float ambientTerm = ambientIntensity;

    outputColor = color * (diffuseTerm + specularTerm + ambientTerm);
    //outputColor = occlusion;
}

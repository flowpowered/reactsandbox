#version 120

varying vec2 textureUV;
varying vec3 viewRay;
varying vec3 lightPositionView;

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
    vec4 color = texture2D(colors, textureUV);
    vec3 normalView = texture2D(normals, textureUV).xyz;

    if (dot(normalView, normalView) <= 0.9) {
        gl_FragColor = color;
        return;
    }

    float depth = linearizeDepth(texture2D(depths, textureUV).r);
    float occlusion = texture2D(occlusion, textureUV).r;

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

    gl_FragColor = color * (diffuseTerm + specularTerm + ambientTerm) * occlusion;
}

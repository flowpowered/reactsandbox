#version 120

varying vec2 textureUV;
varying vec3 viewRay;
varying vec3 lightPositionView;

uniform sampler2D colors;
uniform sampler2D normals;
uniform sampler2D depths;
uniform sampler2D materials;
uniform sampler2D occlusion;
uniform mat4 projectionMatrix;
uniform float lightAttenuation;

float linearizeDepth(in float depth) {
    return projectionMatrix[3][2] / (depth + projectionMatrix[2][2]);
}

void main() {
    gl_FragColor = texture2D(colors, textureUV);

    vec4 rawNormalView = texture2D(normals, textureUV);
    if (rawNormalView.a <= 0) {
        return;
    }
    vec3 normalView = normalize(rawNormalView.xyz * 2 - 1);

    float depth = linearizeDepth(texture2D(depths, textureUV).r);
    float occlusion = texture2D(occlusion, textureUV).r;

    vec3 positionView = viewRay * depth;

    vec3 lightDifference = lightPositionView - positionView;
    float lightDistance = length(lightDifference);
    vec3 lightDirection = lightDifference / lightDistance;
    float distanceIntensity = 1 / (1 + lightAttenuation * lightDistance);
    float normalDotLight = max(0, dot(normalView, lightDirection));

    vec3 material = texture2D(materials, textureUV).rgb;

    float diffuseTerm = material.x * distanceIntensity * normalDotLight;

    float specularTerm;
    if (normalDotLight > 0) {
        specularTerm = material.y * distanceIntensity * pow(max(0, dot(reflect(lightDirection, normalView), normalize(viewRay))), 20);
    } else {
        specularTerm = 0;
    }

    float ambientTerm = material.z;

    gl_FragColor.rgb *= (diffuseTerm + specularTerm + ambientTerm) * occlusion;
}

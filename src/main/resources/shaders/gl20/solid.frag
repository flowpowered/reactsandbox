#version 120

varying vec3 worldPosition;
varying vec3 worldNormal;
varying vec3 viewDirection;

uniform vec4 modelColor;
uniform vec3 lightPosition;
uniform float lightAttenuation;
uniform float diffuseIntensity;
uniform float specularIntensity;
uniform float ambientIntensity;

void main() {
    vec3 lightDifference = lightPosition - worldPosition;
    float lightDistance = length(lightDifference);
    vec3 lightDirection = lightDifference / lightDistance;
    float distanceIntensity = 1 / (1 + lightAttenuation * lightDistance);

    float diffuseTerm =
        diffuseIntensity *
        distanceIntensity *
        clamp(dot(worldNormal, lightDirection), 0, 1);

    float specularTerm =
        specularIntensity *
        distanceIntensity *
        pow(clamp(dot(reflect(-lightDirection, worldNormal), viewDirection), 0, 1), 20);

    float ambientTerm =
        ambientIntensity;

    vec4 color = modelColor;

    gl_FragColor = color * (diffuseTerm + specularTerm + ambientTerm);
}

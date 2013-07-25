#version 120

varying vec3 modelPosition;
varying vec3 modelNormal;
varying vec3 viewDirection;
varying vec2 textureUV;

uniform sampler2D diffuse;
uniform sampler2D specular;
uniform vec3 lightPosition;
uniform float lightAttenuation;
uniform float diffuseIntensity;
uniform float specularIntensity;
uniform float ambientIntensity;

void main() {
    vec3 lightDifference = lightPosition - modelPosition;
    float lightDistance = length(lightDifference);
    vec3 lightDirection = lightDifference / lightDistance;
    float distanceIntensity = 1 / (1 + lightAttenuation * lightDistance);

    float diffuseTerm =
        diffuseIntensity *
        distanceIntensity *
        clamp(dot(modelNormal, lightDirection), 0, 1);

    float specularTerm =
        texture2D(specular, textureUV).r *
        specularIntensity *
        distanceIntensity *
        pow(clamp(dot(reflect(-lightDirection, modelNormal), viewDirection), 0, 1), 20);

    float ambientTerm =
        ambientIntensity;

    vec4 color = texture2D(diffuse, textureUV);

    gl_FragColor = color * (diffuseTerm + specularTerm + ambientTerm);
}

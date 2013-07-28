#version 330

in vec3 worldPosition;
in vec3 worldNormal;
in vec3 viewDirection;
in vec2 textureUV;

out vec4 outputColor;

uniform sampler2D diffuse;
uniform sampler2D specular;
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
        texture(specular, textureUV).r *
        specularIntensity *
        distanceIntensity *
        pow(clamp(dot(reflect(-lightDirection, worldNormal), viewDirection), 0, 1), 20);

    float ambientTerm =
        ambientIntensity;

    vec4 color = texture(diffuse, textureUV);

    outputColor = color * (diffuseTerm + specularTerm + ambientTerm);
}

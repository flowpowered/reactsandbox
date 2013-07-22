#version 120

in vec3 modelPosition;
in vec3 modelNormal;
in vec3 viewDirection;

out vec4 outputColor;

uniform vec4 modelColor;
uniform vec3 lightPosition;
uniform float lightAttenuation;
uniform float diffuseIntensity;
uniform float specularIntensity;
uniform float ambientIntensity;
uniform vec3 targetPosition;
uniform float targetSize;
uniform vec4 targetColor;
uniform bool displayTarget;

void main() {
    vec4 color;
    vec3 targetDifference = targetPosition - modelPosition;
    if (displayTarget && dot(targetDifference, targetDifference) <= targetSize * targetSize) {
        color = targetColor;
    } else {
        color = modelColor;
    }

    vec3 lightDifference = lightPosition - modelPosition;
    float lightDistance = length(lightDifference);
    vec3 lightDirection = lightDifference / lightDistance;
    float distanceIntensity = 1 / (1 + lightAttenuation * lightDistance);

    vec4 diffuse = color * distanceIntensity * clamp(dot(modelNormal, lightDirection), 0, 1);
    vec4 specular = color * distanceIntensity * pow(clamp(dot(reflect(-lightDirection, modelNormal), viewDirection), 0, 1), 2);
    vec4 ambient = color;

    outputColor = diffuse * diffuseIntensity + specular * specularIntensity + ambient * ambientIntensity;
}

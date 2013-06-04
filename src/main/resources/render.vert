#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;

smooth out vec4 diffuse;
smooth out vec4 specular;
smooth out vec4 ambient;

uniform mat4 modelMatrix;
uniform mat4 cameraMatrix;
uniform mat4 projectionMatrix;
uniform vec4 modelColor;
uniform vec3 lightPosition;
uniform float lightAttenuation;

void main() {
    vec3 modelPosition = vec3(modelMatrix * vec4(position, 1));
    vec3 modelNormal = mat3(modelMatrix) * normal;

    gl_Position = projectionMatrix * cameraMatrix * vec4(modelPosition, 1);

    vec3 lightDifference = lightPosition - modelPosition;
    float lightDistance = length(lightDifference);
    vec3 lightDirection = lightDifference / lightDistance;
    float distanceIntensity = 1 / (1 + lightAttenuation * lightDistance);

    diffuse = modelColor * distanceIntensity *
        clamp(dot(modelNormal, lightDirection), 0, 1);

    specular = modelColor * distanceIntensity *
        pow(clamp(dot(
            reflect(-lightDirection, modelNormal),
            normalize(vec3(inverse(cameraMatrix) * vec4(0, 0, 0, 1)) - modelPosition)
        ), 0, 1), 2);

    ambient = modelColor;
}

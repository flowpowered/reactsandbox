#version 120

attribute vec3 position;
attribute vec3 normal;

varying vec3 worldPosition;
varying vec3 worldNormal;
varying vec3 viewDirection;

uniform mat4 modelMatrix;
uniform mat4 cameraMatrix;
uniform mat4 projectionMatrix;

void main() {
    worldPosition = vec3(modelMatrix * vec4(position, 1));
    worldNormal = mat3(modelMatrix) * normal;
    vec3 cameraPosition = -cameraMatrix[3].xyz * mat3(cameraMatrix);
    viewDirection = normalize(cameraPosition - worldPosition);

    if (dot(worldNormal, viewDirection) < 0) {
        worldNormal = -worldNormal;
    }

    gl_Position = projectionMatrix * cameraMatrix * vec4(worldPosition, 1);
}

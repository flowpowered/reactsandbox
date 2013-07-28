#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 textureCoords;

out vec3 worldPosition;
out vec3 worldNormal;
out vec3 viewDirection;
out vec2 textureUV;

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

    textureUV = textureCoords;

    gl_Position = projectionMatrix * cameraMatrix * vec4(worldPosition, 1);
}

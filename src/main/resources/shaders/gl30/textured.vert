#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 textureCoords;

out vec3 positionView;
out vec3 normalView;
out vec2 textureUV;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 normalMatrix;
uniform mat4 projectionMatrix;

void main() {
    positionView = (viewMatrix * modelMatrix * vec4(position, 1)).xyz;

    normalView = (normalMatrix * vec4(normal, 0)).xyz;

    textureUV = textureCoords;

    gl_Position = projectionMatrix * vec4(positionView, 1);
}

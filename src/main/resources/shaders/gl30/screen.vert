#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 textureCoords;

out vec2 textureUV;

uniform mat4 modelMatrix;
uniform mat4 cameraMatrix;
uniform mat4 projectionMatrix;

void main() {
    textureUV = textureCoords;

    gl_Position = projectionMatrix * cameraMatrix * modelMatrix * vec4(position, 1);
}

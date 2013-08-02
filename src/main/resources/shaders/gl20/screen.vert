#version 120

attribute vec3 position;
attribute vec2 textureCoords;

varying vec2 textureUV;

uniform mat4 modelMatrix;
uniform mat4 cameraMatrix;
uniform mat4 projectionMatrix;

void main() {
    textureUV = textureCoords;

    gl_Position = projectionMatrix * cameraMatrix * modelMatrix * vec4(position, 1);
}

#version 120

attribute vec3 position;

varying vec3 positionView;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

void main() {
    positionView = (viewMatrix * modelMatrix * vec4(position, 1)).xyz;

    gl_Position = projectionMatrix * vec4(positionView, 1);
}

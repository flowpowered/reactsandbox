#version 120

attribute vec3 position;
attribute vec3 normal;
attribute vec2 textureCoords;

varying vec3 positionView;
varying vec3 normalView;
varying vec2 textureUV;

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

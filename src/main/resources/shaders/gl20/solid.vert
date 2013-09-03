// $attrib_layout: position = 0
// $attrib_layout: normal = 1

#version 120

attribute vec3 position;
attribute vec3 normal;

varying vec3 positionView;
varying vec3 normalView;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 normalMatrix;
uniform mat4 projectionMatrix;

void main() {
    positionView = (viewMatrix * modelMatrix * vec4(position, 1)).xyz;

    normalView = (normalMatrix * vec4(normal, 0)).xyz;

    gl_Position = projectionMatrix * vec4(positionView, 1);
}

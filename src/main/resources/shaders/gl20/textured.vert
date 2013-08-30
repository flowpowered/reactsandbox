#version 120

attribute vec3 position;
attribute vec3 normal;
attribute vec2 textureCoords;
attribute vec4 tangent;

varying vec3 positionView;
varying vec3 normalView;
varying vec2 textureUV;
varying mat3 tangentMatrix;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 normalMatrix;
uniform mat4 projectionMatrix;

void main() {
    positionView = (viewMatrix * modelMatrix * vec4(position, 1)).xyz;

    textureUV = textureCoords;

    normalView = (normalMatrix * vec4(normal, 0)).xyz;
    vec3 tangentView = (normalMatrix * vec4(tangent.xyz, 0)).xyz;
    vec3 biTangentView = cross(normalView, tangentView) * tangent.w;
    tangentMatrix = mat3(tangentView, biTangentView, normalView);

    gl_Position = projectionMatrix * vec4(positionView, 1);
}

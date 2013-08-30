#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 textureCoords;
layout(location = 3) in vec4 tangent;

out vec3 positionView;
out vec3 normalView;
out vec2 textureUV;
out mat3 tangentMatrix;

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

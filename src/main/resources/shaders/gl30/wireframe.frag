#version 330

in vec3 positionView;

layout(location = 0) out vec4 outputColor;
layout(location = 1) out vec3 outputNormal;

uniform vec4 modelColor;

void main() {
    outputColor = modelColor;

    outputNormal = vec3(0, 0, 0);
}

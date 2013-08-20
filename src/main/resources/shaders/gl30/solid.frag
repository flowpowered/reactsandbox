#version 330

in vec3 positionView;
in vec3 normalView;

layout(location = 0) out vec4 outputColor;
layout(location = 1) out vec4 outputNormal;

uniform vec4 modelColor;

void main() {
    outputColor = modelColor;

    outputNormal = vec4((normalView + 1) / 2, 1);
}

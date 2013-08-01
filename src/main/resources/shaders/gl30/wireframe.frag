#version 330

layout(location = 0) out vec4 outputColor;

uniform vec4 modelColor;

void main() {
    outputColor = modelColor;
}

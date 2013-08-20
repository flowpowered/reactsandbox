#version 330

in vec3 positionView;
in vec3 normalView;
in vec2 textureUV;

layout(location = 0) out vec4 outputColor;
layout(location = 1) out vec4 outputNormal;

uniform sampler2D diffuse;

void main() {
    outputColor = texture(diffuse, textureUV);

    outputNormal = vec4((normalView + 1) / 2, 1);
}

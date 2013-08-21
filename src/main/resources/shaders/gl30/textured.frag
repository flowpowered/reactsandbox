#version 330

in vec3 positionView;
in vec2 textureUV;
in mat3 tangentMatrix;

layout(location = 0) out vec4 outputColor;
layout(location = 1) out vec4 outputNormal;

uniform sampler2D diffuse;
uniform sampler2D normals;

void main() {
    outputColor = texture(diffuse, textureUV);

    vec3 normalView = tangentMatrix * (texture(normals, textureUV).xyz * 2 - 1);
    outputNormal = vec4((normalView + 1) / 2, 1);
}

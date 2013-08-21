#version 120

varying vec3 positionView;
varying vec2 textureUV;
varying mat3 tangentMatrix;

uniform sampler2D diffuse;
uniform sampler2D normals;

void main() {
    gl_FragData[0] = texture2D(diffuse, textureUV);

    vec3 normalView = tangentMatrix * (texture2D(normals, textureUV).xyz * 2 - 1);
    gl_FragData[1] = vec4((normalView + 1) / 2, 1);
}

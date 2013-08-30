#version 120

varying vec3 positionView;
varying vec3 normalView;

uniform vec4 modelColor;
uniform float diffuseIntensity;
uniform float specularIntensity;
uniform float ambientIntensity;

void main() {
    gl_FragData[0] = modelColor;

    gl_FragData[1] = vec4((normalView + 1) / 2, 1);

    gl_FragData[2] = gl_FragData[1];

    gl_FragData[3] = vec4(diffuseIntensity, specularIntensity, ambientIntensity, 1);
}

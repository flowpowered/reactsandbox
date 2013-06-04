#version 330

smooth in vec4 diffuse;
smooth in vec4 specular;
smooth in vec4 ambient;

out vec4 outputColor;

uniform float diffuseIntensity;
uniform float specularIntensity;
uniform float ambientIntensity;

void main() {
    outputColor =
        diffuse * diffuseIntensity +
        specular * specularIntensity +
        ambient * ambientIntensity;
}

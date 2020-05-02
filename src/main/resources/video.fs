#version 330

in vec2 textureCoord;

out vec4 fragColor;

uniform sampler2D texImage;
uniform vec3 colorFilter;

void main()
{
	vec4 textureColor = texture(texImage, textureCoord);
	fragColor = textureColor * vec4(colorFilter, 1.0);
}

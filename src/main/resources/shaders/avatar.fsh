#version 330

in vec2 textureCoord;

out vec4 fragColor;

uniform sampler2D texImage;

void main()
{
    if((pow(textureCoord.x - 0.5, 2) + pow(textureCoord.y - 0.5, 2)) < 0.25)
    {
        vec4 textureColor = texture(texImage, textureCoord);
        fragColor = textureColor;
    }
    else
    {
        fragColor = vec4(0);
    }
}

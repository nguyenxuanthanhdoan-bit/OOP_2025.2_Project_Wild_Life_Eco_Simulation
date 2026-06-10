from PIL import Image
import os

files = ['media__1781063902555.png', 'media__1781063902556.png', 'media__1781063902564.png']
base_path = 'C:/Users/Admin/.gemini/antigravity/brain/74317300-a4da-45b1-bbf0-0bbca0939b8c/'
imgs = [Image.open(base_path + f) for f in files]

w = sum(i.width for i in imgs)
h = max(i.height for i in imgs)
out = Image.new('RGBA', (w, h))

x = 0
for i in imgs:
    out.paste(i, (x, 0))
    x += i.width

out.save('resources/assets/images/Structures/Lantern/lantern.png')
print('Saved lantern.png')
